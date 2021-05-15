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

package com.blazebit.actor.impl;

import com.blazebit.actor.ActorContextBuilder;
import com.blazebit.actor.spi.ActorContextBuilderProvider;
import com.blazebit.apt.service.ServiceProvider;

/**
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
@ServiceProvider(ActorContextBuilderProvider.class)
public class ActorContextBuilderProviderImpl implements ActorContextBuilderProvider {
    @Override
    public ActorContextBuilder createBuilder() {
        ActorContextBuilderImpl builder = new ActorContextBuilderImpl();
        builder.loadDefaults();
        return builder;
    }

    @Override
    public ActorContextBuilder createEmptyBuilder() {
        return new ActorContextBuilderImpl();
    }
}
