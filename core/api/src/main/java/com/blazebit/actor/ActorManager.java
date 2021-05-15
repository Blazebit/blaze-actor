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
package com.blazebit.actor;

/**
 * A manager for registering, rescheduling and removing actors.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public interface ActorManager {

    /**
     * Like {@link #registerActor(String, ScheduledActor, long)} but with an initial delay of 0.
     *
     * @param name The name of the actor
     * @param actor The actor
     * @throws ActorException when an actor is already registered for the given name
     */
    public default void registerActor(String name, ScheduledActor actor) {
        registerActor(name, actor, 0);
    }

    /**
     * Registers the given actor under the given name and starts it after the given initial delay in milliseconds passed.
     *
     * @param name The name of the actor
     * @param actor The actor
     * @param initialDelayMillis The delay in milliseconds after which to start the actor
     * @throws ActorException when an actor is already registered for the given name
     */
    public void registerActor(String name, ScheduledActor actor, long initialDelayMillis);

    /**
     * Registers the given actor under the given name in a suspended state.
     *
     * @param name The name of the actor
     * @param actor The actor
     * @throws ActorException when an actor is already registered for the given name
     */
    public void registerSuspendedActor(String name, ScheduledActor actor);

    /**
     * Triggers a reschedule of the actor with the given name to happen after the given delay in milliseconds passed.
     * When an actor is still running, at most one rescheduling request is queued up.
     * Multiple calls to this method may lead to rescheduling only once.
     *
     * @param name The name of the actor to reschedule
     * @param delayMillis The delay in milliseconds
     * @throws ActorException when no actor with the given name exists
     */
    public void rescheduleActor(String name, long delayMillis);

    /**
     * Removes the actor with the given name.
     * A removed actor is not scheduled again unless it is registered again.
     *
     * @param name The name of the actor to remove
     */
    public void removeActor(String name);

}
