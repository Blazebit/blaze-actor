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
import com.blazebit.actor.ActorContextBuilder;
import com.blazebit.actor.ActorException;
import com.blazebit.actor.ActorManager;
import com.blazebit.actor.ConsumingActor;
import com.blazebit.actor.ScheduledActor;
import com.blazebit.actor.spi.ActorManagerFactory;
import com.blazebit.actor.spi.ClusterNodeInfo;
import com.blazebit.actor.spi.ClusterStateListener;
import com.blazebit.actor.spi.ClusterStateManager;
import com.blazebit.actor.spi.Consumer;
import com.blazebit.actor.spi.ConsumerListenerFactory;
import com.blazebit.actor.spi.Scheduler;
import com.blazebit.actor.spi.SchedulerFactory;
import com.blazebit.actor.spi.StateReturningEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Christian Beikov
 * @since 1.0.0
 */
public class ActorContextBuilderImpl implements ActorContextBuilder {

    private SchedulerFactory schedulerFactory;
    private ActorManagerFactory actorManagerFactory;
    private ConsumerListenerFactory consumerListenerFactory;
    private ClusterStateManager clusterStateManager;
    private final Map<String, ActorEntry> initialActors = new HashMap<>();
    private final Map<Consumer<?>, ConsumingActor<?>> consumers = new HashMap<>();
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<Class<?>, Object> serviceMap = new HashMap<>();
    private final AtomicBoolean built = new AtomicBoolean();

    protected void loadDefaults() {
        schedulerFactory = loadFirstServiceOrNone(SchedulerFactory.class);
        actorManagerFactory = loadFirstServiceOrNone(ActorManagerFactory.class);
        consumerListenerFactory = loadFirstServiceOrNone(ConsumerListenerFactory.class);
        clusterStateManager = new NoClusterStateManager();
    }

    protected static <X> X loadFirstServiceOrNone(Class<X> serviceClass) {
        Iterator<X> scheduleFactoryIterator = ServiceLoader.load(serviceClass).iterator();
        if (scheduleFactoryIterator.hasNext()) {
            X o = scheduleFactoryIterator.next();
            if (scheduleFactoryIterator.hasNext()) {
                return null;
            }
            return o;
        }
        return null;
    }

    protected static <X> List<X> loadServices(Class<X> serviceClass) {
        List<X> list = new ArrayList<>();
        for (X service : ServiceLoader.load(serviceClass)) {
            list.add(service);
        }
        return list;
    }

    private void checkCreateContext() {
        if (getActorManagerFactory() == null) {
            throw new ActorException("No actor manager factory given!");
        }
        if (getSchedulerFactory() == null) {
            throw new ActorException("No scheduler factory given!");
        }
        if (getConsumerListenerFactory() == null) {
            throw new ActorException("No consumer listener factory given!");
        }
        if (getClusterStateManager() == null) {
            throw new ActorException("No cluster state manager given!");
        }
    }

    public ActorContext createContext() {
        checkCreateContext();
        if (!built.compareAndSet(false, true)) {
            throw new IllegalStateException("ActorContext was already built!");
        }
        return new DefaultActorContext(
                getActorManagerFactory(),
                getSchedulerFactory(),
                getConsumers(),
                getInitialActors(),
                getConsumerListenerFactory(),
                getClusterStateManager(),
                properties,
                serviceMap
        );
    }

    public ActorManagerFactory getActorManagerFactory() {
        return actorManagerFactory;
    }

    public ActorContextBuilder withActorManagerFactory(ActorManagerFactory jobManagerFactory) {
        this.actorManagerFactory = jobManagerFactory;
        return this;
    }

    public SchedulerFactory getSchedulerFactory() {
        return schedulerFactory;
    }

    public ActorContextBuilder withSchedulerFactory(SchedulerFactory schedulerFactory) {
        this.schedulerFactory = schedulerFactory;
        return this;
    }

    public ConsumerListenerFactory getConsumerListenerFactory() {
        return consumerListenerFactory;
    }

    public ActorContextBuilder withConsumerListenerFactory(ConsumerListenerFactory consumerListenerFactory) {
        this.consumerListenerFactory = consumerListenerFactory;
        return this;
    }

    public ClusterStateManager getClusterStateManager() {
        return clusterStateManager;
    }

    public ActorContextBuilder withClusterStateManager(ClusterStateManager clusterStateManager) {
        this.clusterStateManager = clusterStateManager;
        return this;
    }

    protected Map<Consumer<?>, ConsumingActor<?>> getConsumers() {
        return consumers;
    }

    public ConsumingActor<?> getConsumer(Consumer<?> consumer) {
        return consumers.get(consumer);
    }

    public <X> ActorContextBuilder withConsumer(Consumer<X> consumer, ConsumingActor<X> consumingActor) {
        this.consumers.put(consumer, consumingActor);
        return this;
    }

    public ActorContextBuilder withConsumers(Map<Consumer<?>, ConsumingActor<?>> consumers) {
        this.consumers.putAll(consumers);
        return this;
    }

    protected Map<String, ActorEntry> getInitialActors() {
        return initialActors;
    }

    public ActorContextBuilder withInitialActor(String name, ScheduledActor actor) {
        this.initialActors.put(name, new ActorEntry(actor, -1L));
        return this;
    }

    public ActorContextBuilder withInitialActor(String name, ScheduledActor actor, long initialDelayMillis) {
        if (initialDelayMillis < 0) {
            throw new IllegalArgumentException("Illegal negative initial delay: " + initialDelayMillis);
        }
        this.initialActors.put(name, new ActorEntry(actor, initialDelayMillis));
        return this;
    }

    protected Map<String, Object> getProperties() {
        return properties;
    }

    public Object getProperty(String property) {
        return properties.get(property);
    }

    public ActorContextBuilder withProperty(String property, Object value) {
        this.properties.put(property, value);
        return this;
    }

    public ActorContextBuilder withProperties(Map<String, Object> properties) {
        this.properties.putAll(properties);
        return this;
    }

    protected Map<Class<?>, Object> getServiceMap() {
        return serviceMap;
    }

    public Collection<Object> getServices() {
        return serviceMap.values();
    }

    public <X> ActorContextBuilder withService(Class<X> serviceClass, X service) {
        this.serviceMap.put(serviceClass, service);
        return this;
    }

    /**
     * @author Christian Beikov
     * @since 1.0.0
     */
    protected static class DefaultActorContext implements ActorContext {

        private final ActorManager actorManager;
        private final CapturingSchedulerFactory schedulerFactory;
        private final ClusterStateManager clusterStateManager;
        private final Map<String, Object> properties;
        private final Map<Class<?>, Object> serviceMap;

        protected DefaultActorContext(ActorManagerFactory actorManagerFactory, SchedulerFactory schedulerFactory, Map<Consumer<?>, ConsumingActor<?>> consumers, Map<String, ActorEntry> initialActors,
                                      ConsumerListenerFactory consumerListenerFactory, ClusterStateManager clusterStateManager, Map<String, Object> properties, Map<Class<?>, Object> serviceMap) {
            this.properties = new HashMap<>(properties);
            this.serviceMap = new HashMap<>(serviceMap);
            this.schedulerFactory = new CapturingSchedulerFactory(schedulerFactory);
            this.clusterStateManager = clusterStateManager;

            Map<String, ScheduledActor> actors = new HashMap<>(initialActors.size());
            for (Map.Entry<String, ActorEntry> entry : initialActors.entrySet()) {
                actors.put(entry.getKey(), entry.getValue().actor);
            }

            this.actorManager = actorManagerFactory.createActorManager(this, actors);

            for (Map.Entry<String, ActorEntry> entry : initialActors.entrySet()) {
                long initialDelayMillis = entry.getValue().initialDelayMillis;
                if (initialDelayMillis != -1L) {
                    this.actorManager.rescheduleActor(entry.getKey(), initialDelayMillis);
                }
            }
            for (Map.Entry<Consumer<?>, ConsumingActor<?>> entry : consumers.entrySet()) {
                ((Consumer) entry.getKey()).registerListener(consumerListenerFactory.createConsumerListener(this, entry.getValue()));
            }
        }

        @Override
        public Object getProperty(String property) {
            return properties.get(property);
        }

        @Override
        public <T> T getService(Class<T> serviceClass) {
            if (serviceClass == SchedulerFactory.class) {
                return (T) schedulerFactory;
            }
            if (serviceClass == ClusterStateManager.class) {
                return (T) clusterStateManager;
            }
            return (T) serviceMap.get(serviceClass);
        }

        @Override
        public ActorManager getActorManager() {
            return actorManager;
        }

        @Override
        public void stop() {
            schedulerFactory.stop();
        }

        @Override
        public void stop(long timeout, TimeUnit unit) throws InterruptedException {
            schedulerFactory.stop(timeout, unit);
        }
    }

    /**
     * @author Christian Beikov
     * @since 1.0.0
     */
    private static class SimpleFuture<T> implements Future<T> {

        private final T result;

        public SimpleFuture(T result) {
            this.result = result;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return result;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return result;
        }
    }

    /**
     * @author Christian Beikov
     * @since 1.0.0
     */
    private static class CapturingSchedulerFactory implements SchedulerFactory {

        private final List<Scheduler> schedulers = new CopyOnWriteArrayList<>();
        private final SchedulerFactory delegate;
        private volatile boolean closed;

        public CapturingSchedulerFactory(SchedulerFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public Scheduler createScheduler(ActorContext actorContext, String actorName) {
            if (closed) {
                return null;
            }
            Scheduler scheduler = delegate.createScheduler(actorContext, actorName);
            schedulers.add(scheduler);
            return scheduler;
        }

        public void stop() {
            closed = true;
            for (Scheduler scheduler : schedulers) {
                if (scheduler.supportsStop()) {
                    scheduler.stop();
                }
            }
        }

        public void stop(long timeout, TimeUnit unit) throws InterruptedException {
            closed = true;
            // Snapshot it first to avoid concurrency in timing code
            Scheduler[] schedulers = this.schedulers.toArray(new Scheduler[this.schedulers.size()]);
            long millis = unit.toMillis(timeout);
            long started = System.currentTimeMillis();
            for (Scheduler scheduler : schedulers) {
                if (scheduler.supportsStop()) {
                    scheduler.stop(millis, TimeUnit.MILLISECONDS);
                    long next = System.currentTimeMillis();
                    millis -= next - started;
                    started = next;
                }
            }
        }
    }

    /**
     * @author Christian Beikov
     * @since 1.0.0
     */
    private static class NoClusterStateManager implements ClusterStateManager, ClusterNodeInfo {

        private final Map<Class<?>, List<java.util.function.Consumer<Serializable>>> listeners = new ConcurrentHashMap<>();

        @Override
        public ClusterNodeInfo getCurrentNodeInfo() {
            return this;
        }

        @Override
        public void registerListener(ClusterStateListener listener) {
            listener.onClusterStateChanged(this);
        }

        @Override
        public <T extends Serializable> void registerListener(Class<T> eventClass, java.util.function.Consumer<T> listener) {
            listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add((java.util.function.Consumer<Serializable>) listener);
        }

        @Override
        public void fireEventExcludeSelf(Serializable event, boolean await) {
            // Noop because there is no cluster
        }

        @Override
        public void fireEvent(Serializable event, boolean await) {
            java.util.function.Consumer<Class<?>> consumer = eventClass -> {
                List<java.util.function.Consumer<Serializable>> consumers = listeners.get(eventClass);
                if (consumers != null) {
                    consumers.forEach(c -> c.accept(event));
                }
            };
            Class<?> clazz = event.getClass();
            Set<Class<?>> visitedClasses = new HashSet<>();
            do {
                consumer.accept(clazz);
                visitInterfaces(consumer, clazz, visitedClasses);
                clazz = clazz.getSuperclass();
            } while (clazz != null);
        }

        @Override
        public <T> Map<ClusterNodeInfo, Future<T>> fireEvent(StateReturningEvent<T> event) {
            fireEvent((Serializable) event, false);
            return Collections.singletonMap(getCurrentNodeInfo(), new SimpleFuture<>(event.getResult()));
        }

        @Override
        public <T> Map<ClusterNodeInfo, Future<T>> fireEventExcludeSelf(StateReturningEvent<T> event) {
            // Noop because there is no cluster
            return null;
        }

        @Override
        public boolean isStandalone() {
            return true;
        }

        private void visitInterfaces(java.util.function.Consumer<Class<?>> consumer, Class<?> clazz, Set<Class<?>> visitedClasses) {
            Class<?>[] interfaces = clazz.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                Class<?> interfaceClass = interfaces[i];
                if (visitedClasses.add(interfaceClass)) {
                    consumer.accept(interfaceClass);
                    visitInterfaces(consumer, interfaceClass, visitedClasses);
                }
            }
        }

        @Override
        public boolean isCoordinator() {
            return true;
        }

        @Override
        public long getClusterVersion() {
            return 0L;
        }

        @Override
        public int getClusterPosition() {
            return 0;
        }

        @Override
        public int getClusterSize() {
            return 1;
        }
    }

    /**
     * @author Christian Beikov
     * @since 1.0.0
     */
    private static class ActorEntry {
        private final ScheduledActor actor;
        private final long initialDelayMillis;

        public ActorEntry(ScheduledActor actor, long initialDelayMillis) {
            this.actor = actor;
            this.initialDelayMillis = initialDelayMillis;
        }
    }
}
