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

package com.blazebit.actor.consumer.memory;

import com.blazebit.actor.spi.Consumer;
import com.blazebit.actor.spi.ConsumerListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @param <T> The message type
 * @author Christian Beikov
 * @since 1.0.0
 */
public class BlockingDequeConsumer<T> implements Consumer<T>, Runnable {

    private static final Logger LOG = Logger.getLogger(BlockingDequeConsumer.class.getName());
    private final List<ConsumerListener<T>> listeners = new CopyOnWriteArrayList<>();
    private final BlockingDeque<T> payloadQueue;

    /**
     * Creates a new blocking deque consumer that consumes from the given queue.
     *
     * @param payloadQueue The queue to consume from
     */
    public BlockingDequeConsumer(BlockingDeque<T> payloadQueue) {
        this.payloadQueue = payloadQueue;
    }

    @Override
    public void registerListener(ConsumerListener<T> listener) {
        listeners.add(listener);
    }

    protected boolean isDone() {
        return Thread.currentThread().isInterrupted();
    }

    @Override
    public void run() {
        List<T> messages = new ArrayList<>();
        List<T> unmodifiableList = Collections.unmodifiableList(messages);
        while (!isDone()) {
            try {
                // Blocked wait
                T first = payloadQueue.takeFirst();
                messages.add(first);
                // When we found one element, drain the rest that is available
                payloadQueue.drainTo(messages);
                try {
                    try {
                        listeners.forEach(l -> l.consume(unmodifiableList));
                    } catch (Throwable t) {
                        LOG.log(Level.SEVERE, "Error in ConsumerListener", t);
                    }
                } finally {
                    messages.clear();
                }
            } catch (InterruptedException e) {
                // Stop the loop when we were interrupted
                break;
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Error in Consumer", t);
            }
        }
    }
}
