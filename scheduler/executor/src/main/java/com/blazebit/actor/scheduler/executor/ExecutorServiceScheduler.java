/*
 * Copyright 2018 - 2021 Blazebit.
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
package com.blazebit.actor.scheduler.executor;

import com.blazebit.actor.ActorContext;
import com.blazebit.actor.ActorException;
import com.blazebit.actor.spi.Scheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A scheduler based on the {@link ScheduledExecutorService} API.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public class ExecutorServiceScheduler implements Scheduler {

    public static final String EXECUTOR_SERVICE_PROPERTY = "actor.scheduler.executor.executor_service";

    private static final Logger LOG = Logger.getLogger(ExecutorServiceScheduler.class.getName());
    private static final Class<?> MANAGED_EXECUTOR_SERVICE_CLASS;

    static {
        Class<?> managedExecutorServiceClass = null;
        try {
            managedExecutorServiceClass = Class.forName("javax.enterprise.concurrent.ManagedExecutorService");
        } catch (ClassNotFoundException e) {
            LOG.warning("Couldn't find javax.enterprise.concurrent.ManagedExecutorService class, disabling shutdown detection");
        }
        MANAGED_EXECUTOR_SERVICE_CLASS = managedExecutorServiceClass;
    }

    private final String name;
    private final ScheduledExecutorService executorService;

    /**
     * Creates a scheduler based on a single threaded executor.
     */
    public ExecutorServiceScheduler() {
        this(Executors.newSingleThreadScheduledExecutor());
    }

    /**
     * Creates a scheduler using the given actor name for resolving an executor service.
     *
     * @param actorContext The actor context
     * @param name The actor name
     */
    public ExecutorServiceScheduler(ActorContext actorContext, String name) {
        this(name, getExecutorService(actorContext, name));
    }

    /**
     * Creates a scheduler with the given executor service.
     *
     * @param executorService The executor service
     */
    public ExecutorServiceScheduler(ScheduledExecutorService executorService) {
        this(null, executorService);
    }

    /**
     * Creates a scheduler with the given name and executor service.
     *
     * @param name The actor name
     * @param executorService The executor service
     */
    public ExecutorServiceScheduler(String name, ScheduledExecutorService executorService) {
        if (executorService == null) {
            throw new ActorException("No executor service given");
        }
        this.name = name;
        this.executorService = executorService;
    }

    private static ScheduledExecutorService getExecutorService(ActorContext actorContext, String name) {
        Object executorService = null;
        if (name != null) {
            executorService = actorContext.getProperty(EXECUTOR_SERVICE_PROPERTY + "." + name);
        }
        if (executorService == null) {
            executorService = actorContext.getProperty(EXECUTOR_SERVICE_PROPERTY);
        } else if (executorService instanceof ScheduledExecutorService) {
            return (ScheduledExecutorService) executorService;
        } else {
            throw new ActorException("The object given via the property '" + EXECUTOR_SERVICE_PROPERTY + "." + name + "' should be a ScheduledExecutorService but isn't: " + executorService);
        }
        if (executorService == null) {
            return actorContext.getService(ScheduledExecutorService.class);
        } else if (executorService instanceof ScheduledExecutorService) {
            return (ScheduledExecutorService) executorService;
        } else {
            throw new ActorException("The object given via the property '" + EXECUTOR_SERVICE_PROPERTY + "' should be a ScheduledExecutorService but isn't: " + executorService);
        }
    }

    @Override
    public <T> Future<T> schedule(Callable<T> task, long delayMillis) {
        return executorService.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean supportsStop() {
        if (MANAGED_EXECUTOR_SERVICE_CLASS != null && MANAGED_EXECUTOR_SERVICE_CLASS.isAssignableFrom(executorService.getClass())) {
            return false;
        }
        return true;
    }

    @Override
    public void stop() {
        if (!supportsStop()) {
            throw new UnsupportedOperationException();
        }
        executorService.shutdown();
    }

    @Override
    public void stop(long timeout, TimeUnit unit) throws InterruptedException {
        if (!supportsStop()) {
            throw new UnsupportedOperationException();
        }
        executorService.shutdown();
        executorService.awaitTermination(timeout, unit);
    }
}
