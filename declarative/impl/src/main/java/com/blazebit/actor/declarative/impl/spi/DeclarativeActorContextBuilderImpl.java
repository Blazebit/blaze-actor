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

package com.blazebit.actor.declarative.impl.spi;

import com.blazebit.actor.ActorContext;
import com.blazebit.actor.ActorContextBuilder;
import com.blazebit.actor.ScheduledActor;
import com.blazebit.actor.declarative.ActorConfig;
import com.blazebit.actor.declarative.DeclarativeActorContextBuilder;
import com.blazebit.annotation.AnnotationUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Christian Beikov
 * @since 1.0.0
 */
public class DeclarativeActorContextBuilderImpl implements DeclarativeActorContextBuilder {

    private final Map<String, ActorEntry> initialActors = new HashMap<>();

    @Override
    public DeclarativeActorContextBuilder addActor(ScheduledActor actor) {
        Class<? extends ScheduledActor> actorClass = actor.getClass();
        ActorConfig actorConfig = AnnotationUtils.findAnnotation(actorClass, ActorConfig.class);
        if (actorConfig == null) {
            throw new IllegalArgumentException("The given actor class has no ActorConfig annotation: " + actorClass.getName());
        }
        String name = actorConfig.name();
        if (name.isEmpty()) {
            name = actorClass.getName();
        }

        ActorEntry old;
        if (actorConfig.initialDelay() < 0L) {
            old = initialActors.put(name, new ActorEntry(actor, -1L));
        } else {
            old = initialActors.put(name, new ActorEntry(actor, actorConfig.unit().toMillis(actorConfig.initialDelay())));
        }

        if (old != null) {
            throw new IllegalArgumentException("Can't register the actor with class '" + actorClass.getName() + "' under the name '" + name + "' as there already is an actor registered under this name: " + old.actor.getClass().getName());
        }
        return this;
    }

    @Override
    public void apply(ActorContextBuilder builder) {
        for (Map.Entry<String, ActorEntry> entry : initialActors.entrySet()) {
            ActorEntry value = entry.getValue();
            if (value.initialDelayMillis == -1) {
                builder.withInitialActor(entry.getKey(), value.actor);
            } else {
                builder.withInitialActor(entry.getKey(), value.actor, value.initialDelayMillis);
            }
        }
    }

    @Override
    public ActorContextBuilder createBuilder() {
        ActorContextBuilder builder = ActorContext.builder();
        apply(builder);
        return builder;
    }

    private static class ActorEntry {
        private final ScheduledActor actor;
        private final long initialDelayMillis;

        public ActorEntry(ScheduledActor actor, long initialDelayMillis) {
            this.actor = actor;
            this.initialDelayMillis = initialDelayMillis;
        }
    }
}
