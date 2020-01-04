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

import com.blazebit.actor.spi.ActorContextBuilderProvider;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

/**
 * A closable context in which actors run.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public interface ActorContext {

    /**
     * Returns a registered service that is an instance of the given service class.
     *
     * @param serviceClass The service class
     * @param <T> The service type
     * @return The service or <code>null</code>
     */
    <T> T getService(Class<T> serviceClass);

    /**
     * Returns the property value registered for the given property key.
     *
     * @param property The property key
     * @return The value or <code>null</code>
     */
    Object getProperty(String property);

    /**
     * Returns the actor manager.
     *
     * @return the actor manager
     */
    ActorManager getActorManager();

    /**
     * Stops the actor context.
     * After this method finished no further actors are scheduled but there may still be running actors.
     */
    void stop();

    /**
     * Stops the actor context and waits up to the given amount of time for currently running actors to finish.
     * After this method finished no further actors are scheduled.
     *
     * @param timeout The maximum time to wait
     * @param unit The time unit of the timeout argument
     * @throws InterruptedException if interrupted while waiting
     */
    void stop(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Returns a builder for an actor context created via the {@link #defaultBuilderProvider()}.
     *
     * @return a builder for an actor context
     */
    static ActorContextBuilder builder() {
        return defaultBuilderProvider().createBuilder();
    }

    /**
     * Returns an empty builder for an actor context created via the {@link #defaultBuilderProvider()}.
     *
     * @return an empty builder for an actor context
     */
    static ActorContextBuilder emptyBuilder() {
        return defaultBuilderProvider().createEmptyBuilder();
    }

    /**
     * Returns the first {@linkplain ActorContextBuilderProvider} that is found.
     *
     * @return The first {@linkplain ActorContextBuilderProvider} that is found
     */
    static ActorContextBuilderProvider defaultBuilderProvider() {
        Iterator<ActorContextBuilderProvider> iterator = ServiceLoader.load(ActorContextBuilderProvider.class).iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }

        throw new IllegalStateException("No ActorContextBuilderProvider found on the class path. Please check if a valid implementation is on the class path.");
    }

}
