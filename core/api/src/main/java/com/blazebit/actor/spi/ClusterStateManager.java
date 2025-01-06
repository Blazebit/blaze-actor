/*
 * Copyright 2018 - 2025 Blazebit.
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

package com.blazebit.actor.spi;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * A manager for the state of a cluster.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public interface ClusterStateManager {

    /**
     * Returns the cluster node information of the cluster node in which this JVM is running.
     *
     * @return the cluster node information
     */
    public ClusterNodeInfo getCurrentNodeInfo();

    /**
     * Registers a cluster state listener.
     *
     * @param listener The listener
     */
    public void registerListener(ClusterStateListener listener);

    /**
     * Registers an event listener for the given event class listening to cluster events.
     *
     * @param eventClass The event class
     * @param listener   The listener
     * @param <T>        The event type
     */
    public <T extends Serializable> void registerListener(Class<T> eventClass, java.util.function.Consumer<T> listener);

    /**
     * Fires the given event throughout the whole cluster.
     *
     * @param event The event to fire
     * @param await Whether to await all results
     */
    public void fireEvent(Serializable event, boolean await);

    /**
     * Fires the given event throughout the whole cluster except in the current JVM.
     *
     * @param event The event to fire
     * @param await Whether to await all results
     */
    public void fireEventExcludeSelf(Serializable event, boolean await);

    /**
     * Fires the given event throughout the whole cluster.
     *
     * @param event The event to fire
     * @param <T> The result type of the event
     * @return The return values of the event
     */
    public <T> Map<ClusterNodeInfo, Future<T>> fireEvent(StateReturningEvent<T> event);

    /**
     * Fires the given event throughout the whole cluster except in the current JVM.
     *
     * @param event The event to fire
     * @param <T> The result type of the event
     * @return The return values of the event
     */
    public <T> Map<ClusterNodeInfo, Future<T>> fireEventExcludeSelf(StateReturningEvent<T> event);

    /**
     * Returns the lock service for the cluster.
     *
     * @return The lock service
     */
    public LockService getLockService();

    /**
     * Returns <code>true</code> if this is a standalone instance without real clustering support, otherwise <code>false</code>.
     *
     * @return whether this is standalone instance without real clustering support
     */
    public boolean isStandalone();

}
