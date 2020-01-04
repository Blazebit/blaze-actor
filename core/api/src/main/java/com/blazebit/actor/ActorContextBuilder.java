/*
 * Copyright 2018 - 2020 Blazebit.
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

package com.blazebit.actor;

import com.blazebit.actor.spi.ActorManagerFactory;
import com.blazebit.actor.spi.ClusterStateManager;
import com.blazebit.actor.spi.Consumer;
import com.blazebit.actor.spi.ConsumerListenerFactory;
import com.blazebit.actor.spi.SchedulerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * A builder for an {@link ActorContext}.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public interface ActorContextBuilder {

    /**
     * Returns the context created from the configurations done through the builder.
     *
     * @return the context
     */
    public ActorContext createContext();

    /**
     * Returns the actor manager factory.
     *
     * @return the actor manager factory
     */
    public ActorManagerFactory getActorManagerFactory();

    /**
     * Sets the given actor manager factory.
     *
     * @param jobManagerFactory The actor manager factory
     * @return this for chaining
     */
    public ActorContextBuilder withActorManagerFactory(ActorManagerFactory jobManagerFactory);

    /**
     * Returns the scheduler factory.
     *
     * @return the scheduler factory
     */
    public SchedulerFactory getSchedulerFactory();

    /**
     * Sets the given scheduler factory.
     *
     * @param schedulerFactory The scheduler factory
     * @return this for chaining
     */
    public ActorContextBuilder withSchedulerFactory(SchedulerFactory schedulerFactory);

    /**
     * Returns the consumer listener factory.
     *
     * @return the consumer listener factory
     */
    public ConsumerListenerFactory getConsumerListenerFactory();

    /**
     * Sets the given consumer listener factory.
     *
     * @param consumerListenerFactory The consumer listener factory
     * @return this for chaining
     */
    public ActorContextBuilder withConsumerListenerFactory(ConsumerListenerFactory consumerListenerFactory);

    /**
     * Returns the cluster state manager.
     *
     * @return the cluster state manager
     */
    public ClusterStateManager getClusterStateManager();

    /**
     * Sets the given cluster state manager.
     *
     * @param clusterStateManager The cluster state manager
     * @return this for chaining
     */
    public ActorContextBuilder withClusterStateManager(ClusterStateManager clusterStateManager);

    /**
     * Returns the {@link ConsumingActor} that is registered for the given consumer.
     *
     * @param consumer The consumer for which to return the consuming actor
     * @return The {@link ConsumingActor} or <code>null</code>
     */
    public ConsumingActor<?> getConsumer(Consumer<?> consumer);

    /**
     * Registers the given {@link ConsumingActor} for the given {@link Consumer}.
     *
     * @param consumer The consumer for which to register
     * @param consumingActor The consuming actor to register
     * @param <X> The message type that is consumed
     * @return this for chaining
     */
    public <X> ActorContextBuilder withConsumer(Consumer<X> consumer, ConsumingActor<X> consumingActor);

    /**
     * Registers all consumer as if they were registered individually via {@link #withConsumer(Consumer, ConsumingActor)}.
     *
     * @param consumers The consumers to register
     * @return this for chaining
     */
    public ActorContextBuilder withConsumers(Map<Consumer<?>, ConsumingActor<?>> consumers);

    /**
     * Like {@link #withInitialActor(String, ScheduledActor, long)} but with an initial delay of 0 milliseconds.
     *
     * @param name The name under which the actor should be registered
     * @param actor The actor to register
     * @return this for chaining
     */
    public ActorContextBuilder withInitialActor(String name, ScheduledActor actor);

    /**
     * Register the given {@link ScheduledActor} under the given name, to delay the start for the given amount of
     * milliseconds after the start of the actor context.
     *
     * @param name The name under which the actor should be registered
     * @param actor The actor to register
     * @param initialDelayMillis The delay in milliseconds
     * @return this for chaining
     */
    public ActorContextBuilder withInitialActor(String name, ScheduledActor actor, long initialDelayMillis);

    /**
     * Returns the property value for the given property key or <code>null</code>.
     *
     * @param property The property key
     * @return the property value or <code>null</code>
     */
    public Object getProperty(String property);

    /**
     * Sets the property with the given key to the given value.
     *
     * @param property The property key
     * @param value The property value
     * @return this for chaining
     */
    public ActorContextBuilder withProperty(String property, Object value);

    /**
     * Registers all properties as if they were registered individually via  {@link #withProperty(String, Object)}.
     *
     * @param properties The properties to register
     * @return this for chaining
     */
    public ActorContextBuilder withProperties(Map<String, Object> properties);

    /**
     * Returns the registered services.
     *
     * @return the registered services
     */
    public Collection<Object> getServices();

    /**
     * Registers the given service for the given service class.
     *
     * @param serviceClass The service class for which to register the service for
     * @param service The service to register
     * @param <X> The service class type
     * @return this for chaining
     */
    public <X> ActorContextBuilder withService(Class<X> serviceClass, X service);
}
