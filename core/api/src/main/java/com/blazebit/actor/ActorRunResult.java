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
package com.blazebit.actor;

/**
 * The run result of an actor.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public final class ActorRunResult {

    private static final ActorRunResult SUSPEND = new ActorRunResult(-1);
    private static final ActorRunResult DONE = new ActorRunResult(-2);

    private final long delayMillis;

    private ActorRunResult(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    /**
     * Returns the run result representing that an actor is done with it's execution.
     *
     * @return the run result
     */
    public static ActorRunResult done() {
        return DONE;
    }

    /**
     * Returns the run result representing that an actor is done with it's execution and can be suspended.
     *
     * @return the run result
     */
    public static ActorRunResult suspend() {
        return SUSPEND;
    }

    /**
     * Returns the run result representing that an actor should be rescheduled in the given amount of milliseconds.
     *
     * @param millis The amount of milliseconds to wait until an actor should be rescheduled again
     * @return the run result
     */
    public static ActorRunResult rescheduleIn(long millis) {
        return new ActorRunResult(millis < 0 ? 0 : millis);
    }

    /**
     * Returns whether the run result represents the done state.
     *
     * @return whether the run result represents the done state
     */
    public boolean isDone() {
        return this == DONE;
    }

    /**
     * Returns whether the run result represents the suspend state.
     *
     * @return whether the run result represents the suspend state
     */
    public boolean isSuspend() {
        return this == SUSPEND;
    }

    /**
     * Returns whether the run result represents the reschedule state.
     *
     * @return whether the run result represents the reschedule state
     */
    public boolean isReschedule() {
        return delayMillis > -1;
    }

    /**
     * Returns the delay in milliseconds after which a reschedule should happen or -1 if none should happen.
     *
     * @return the delay in milliseconds after which a reschedule should happen
     */
    public long getDelayMillis() {
        return delayMillis > -1 ? delayMillis : -1;
    }

}
