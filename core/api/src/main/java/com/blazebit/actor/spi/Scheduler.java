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
package com.blazebit.actor.spi;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * An abstraction for task scheduling.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public interface Scheduler {

    /**
     * Like {@link #schedule(Callable, long)} but schedules the task without a delay.
     *
     * @param task The task to schedule
     * @param <T> The return type of the task
     * @return A future to access the result or cancel the task
     */
    default <T> Future<T> submit(Callable<T> task) {
        return schedule(task, 0L);
    }

    /**
     * Schedules a task for execution with the given delay in milliseconds.
     *
     * @param task The task to schedule
     * @param delayMillis The delay in milliseconds
     * @param <T> The return type of the task
     * @return A future to access the result or cancel the task
     */
    <T> Future<T> schedule(Callable<T> task, long delayMillis);

    /**
     * Returns whether the scheduler supports being stopped.
     * Managed schedulers which are controlled by a runtime might not support stopping.
     *
     * @return whether the scheduler supports being stopped
     */
    boolean supportsStop();

    /**
     * Stops the scheduler.
     * After this method finished no further tasks are scheduled but there may still be running tasks.
     * @throws UnsupportedOperationException when supportStop returns false
     */
    void stop();

    /**
     * Stops the scheduler and waits up to the given amount of time for currently running tasks to finish.
     * After this method finished no further tasks are scheduled.
     *
     * @param timeout The maximum time to wait
     * @param unit The time unit of the timeout argument
     * @throws UnsupportedOperationException when supportStop returns false
     * @throws InterruptedException if interrupted while waiting
     */
    void stop(long timeout, TimeUnit unit) throws InterruptedException;
}
