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
import com.blazebit.actor.ActorManager;
import com.blazebit.actor.ScheduledActor;
import com.blazebit.actor.spi.ActorManagerFactory;
import com.blazebit.apt.service.ServiceProvider;

import java.util.Map;

/**
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
@ServiceProvider(ActorManagerFactory.class)
public class ActorManagerFactoryImpl implements ActorManagerFactory {

    @Override
    public ActorManager createActorManager(ActorContext actorContext, Map<String, ScheduledActor> initialActors) {
        return new ActorManagerImpl(actorContext, initialActors);
    }
}
