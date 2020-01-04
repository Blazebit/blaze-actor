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
package com.blazebit.actor.scheduler.spring;

import com.blazebit.actor.ActorContext;
import com.blazebit.actor.spi.Scheduler;
import com.blazebit.actor.spi.SchedulerFactory;
import com.blazebit.apt.service.ServiceProvider;

/**
 * A factory for Springs' {@link org.springframework.scheduling.TaskScheduler} based schedulers.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
@ServiceProvider(SchedulerFactory.class)
public class SpringTaskSchedulerFactory implements SchedulerFactory {

    @Override
    public Scheduler createScheduler(ActorContext actorContext, String name) {
        return new SpringTaskScheduler(actorContext, name);
    }
}
