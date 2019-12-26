/*
 * Copyright 2018 - 2019 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.actor.impl;

import com.blazebit.actor.ActorContext;
import com.blazebit.actor.ActorException;
import com.blazebit.actor.ActorManager;
import com.blazebit.actor.ActorRunResult;
import com.blazebit.actor.ScheduledActor;
import com.blazebit.actor.spi.ClusterStateManager;
import com.blazebit.actor.spi.Scheduler;
import com.blazebit.actor.spi.SchedulerFactory;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public class ActorManagerImpl implements ActorManager {

    private static final Logger LOG = Logger.getLogger(ActorManagerImpl.class.getName());

    private final ActorContext actorContext;
    private final SchedulerFactory schedulerFactory;
    private final ClusterStateManager clusterStateManager;
    private final ConcurrentMap<String, ActorEntry> registeredActors = new ConcurrentHashMap<>();

    public ActorManagerImpl(ActorContext actorContext, Map<String, ScheduledActor> initialActors) {
        this.actorContext = actorContext;
        this.schedulerFactory = actorContext.getService(SchedulerFactory.class);
        this.clusterStateManager = actorContext.getService(ClusterStateManager.class);

        for (Map.Entry<String, ScheduledActor> entry : initialActors.entrySet()) {
            getOrRegisterActor(entry.getKey(), entry.getValue());
        }

        this.clusterStateManager.registerListener(ActorRescheduleEvent.class, e -> {
            // Reschedule without delay since this is a cluster event
            if (!rescheduleActorLocally(e.getActorName(), 0)) {
                // No need to queue events as the cluster event should only be fired after registering all actors
                if (LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("Dropping rescheduling event because actor is not registered: " + e.getActorName());
                }
            }
        });
    }

    @Override
    public void registerSuspendedActor(String name, ScheduledActor actor) {
        getOrRegisterActor(name, actor);
    }

    @Override
    public void registerActor(String name, ScheduledActor actor, long initialDelayMillis) {
        getOrRegisterActor(name, actor).reschedule(initialDelayMillis);
    }

    private ActorEntry getOrRegisterActor(String name, ScheduledActor actor) {
        return registeredActors.compute(name, (n, actorEntry) -> {
            if (actorEntry != null) {
                if (!actorEntry.actor.equals(actor)) {
                    throw new ActorException("An actor is already scheduled for the name '" + name + "'. Can't schedule: " + actor);
                }

                return actorEntry;
            } else {
                Scheduler scheduler = schedulerFactory.createScheduler(actorContext, name);
                return new ActorEntry(name, actor, scheduler, registeredActors);
            }
        });
    }

    @Override
    public void rescheduleActor(String name, long delayMillis) {
        if (!rescheduleActorLocally(name, delayMillis)) {
            throw new ActorException("No actor with the name '" + name + "' is registered!");
        }
        clusterStateManager.fireEventExcludeSelf(new ActorRescheduleEvent(name), false);
    }

    private boolean rescheduleActorLocally(String name, long delayMillis) {
        if (delayMillis < 0) {
            throw new ActorException("Invalid negative delay!");
        }
        ActorEntry actorEntry = registeredActors.get(name);
        if (actorEntry == null) {
            return false;
        }
        actorEntry.reschedule(delayMillis);
        return true;
    }

    @Override
    public void removeActor(String name) {
        ActorEntry actorEntry = registeredActors.get(name);
        if (actorEntry == null) {
            return;
        }

        actorEntry.cancel();
    }

    private static class ActorEntry implements Callable<Void> {

        private final String name;
        private final ScheduledActor actor;
        private final Scheduler scheduler;
        private final ConcurrentMap<String, ActorEntry> registeredActors;
        private final Lock running = new ReentrantLock();
        private final AtomicBoolean reRun = new AtomicBoolean();
        private volatile boolean cancelled;

        public ActorEntry(String name, ScheduledActor actor, Scheduler scheduler, ConcurrentMap<String, ActorEntry> registeredActors) {
            this.name = name;
            this.actor = actor;
            this.scheduler = scheduler;
            this.registeredActors = registeredActors;
        }

        public void reschedule(long delayMillis) {
            if (!cancelled) {
                // If this should be scheduled immediately, we try to signal a re-run to an existing thread
                if (delayMillis == 0L && reRun.compareAndSet(false, true)) {
                    // If we can acquire the lock, no thread is running this
                    if (running.tryLock()) {
                        // So we unlock and reset the re-run flag
                        running.unlock();
                        reRun.set(false);
                        // And simply schedule a run
                        scheduler.schedule(this, delayMillis);
                    }
                    // If we weren't able to acquire a lock, the running thread will take care of the re-run
                } else {
                    scheduler.schedule(this, delayMillis);
                }
            }
        }

        public void cancel() {
            this.cancelled = true;
            registeredActors.remove(name, this);
        }

        @Override
        public Void call() {
            // If we set this to true...
            if (reRun.compareAndSet(false, true)) {
                // And the actor is already running...
                if (!running.tryLock()) {
                    // Then we are done, as the running actor will re-run
                    return null;
                }
                // Otherwise we just run the actor as we now have the lock
            } else {
                // If re-run was set by another thread, it will be handled by that thread or a currently running one
                return null;
            }

            // We have the lock and are about to run, so this is just like a re-run
            reRun.set(false);
            while (!cancelled) {
                try {
                    ActorRunResult runResult = actor.work();
                    if (runResult.isReschedule()) {
                        if (runResult.getDelayMillis() == 0L) {
                            reRun.set(true);
                        } else {
                            reschedule(runResult.getDelayMillis());
                        }
                    } else if (runResult.isDone()) {
                        cancel();
                        break;
                    }
                } catch (Throwable t) {
                    LOG.log(Level.SEVERE, "Error during actor execution", t);
                    cancel();
                    break;
                } finally {
                    running.unlock();
                }
                // If no immediate re-run is scheduled we are done
                if (!reRun.compareAndSet(true, false)) {
                    break;
                }
                // If this thread should handle the re-run, try locking again
                if (!running.tryLock()) {
                    // If another thread was faster, because e.g. we have been suspended, we are done
                    break;
                }
            }
            return null;
        }
    }
}
