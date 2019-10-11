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

package com.blazebit.actor.declarative;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Marks a class that implements {@link com.blazebit.actor.ScheduledActor} as declarative actor.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ActorConfig {

    /**
     * The name of the actor.
     *
     * @return the name of the actor
     */
    String name() default "";

    /**
     * The initial delay for running the actor.
     *
     * @return the initial delay
     */
    long initialDelay() default 0L;

    /**
     * The time unit for the initial delay.
     *
     * @return the time unit for the initial delay
     */
    TimeUnit unit() default TimeUnit.SECONDS;

}
