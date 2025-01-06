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

import com.blazebit.actor.ActorContext;
import com.blazebit.actor.ConsumingActor;

/**
 * Interface implemented by the actor implementation provider.
 *
 * Implementations are instantiated via {@link java.util.ServiceLoader}.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public interface ConsumerListenerFactory {

    /**
     * Creates a managed consumer listener for the given actor context and consuming actor.
     *
     * @param context The actor context
     * @param consumingActor The consuming actor
     * @param <T> The message type
     * @return the consumer listener
     */
    <T> ConsumerListener<T> createConsumerListener(ActorContext context, ConsumingActor<T> consumingActor);
}
