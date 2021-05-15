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

/**
 * A message consumer that notifies its consumer listeners.
 *
 * @param <T> The message type
 * @author Christian Beikov
 * @since 1.0.0
 */
public interface Consumer<T> {

    /**
     * Registers a consumer listener on the consumer.
     *
     * @param listener The listener to register
     */
    public void registerListener(ConsumerListener<T> listener);

}