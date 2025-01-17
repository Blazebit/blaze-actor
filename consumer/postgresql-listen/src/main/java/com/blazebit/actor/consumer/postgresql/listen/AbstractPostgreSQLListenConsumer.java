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

package com.blazebit.actor.consumer.postgresql.listen;

import com.blazebit.actor.spi.Consumer;
import com.blazebit.actor.spi.ConsumerListener;
import org.postgresql.PGConnection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A consumer that makes use of PostgreSQL LISTEN/NOTIFY via the standard JDBC driver.
 *
 * @param <T> The message type
 * @author Christian Beikov
 * @since 1.0.0
 */
public abstract class AbstractPostgreSQLListenConsumer<T> implements Consumer<T>, Runnable {

    private static final Logger LOG = Logger.getLogger(AbstractPgjdbcNgListenConsumer.class.getName());
    private final List<ConsumerListener<T>> listeners = new CopyOnWriteArrayList<>();
    private final DataSource dataSource;
    private final String channelName;
    private final long pollingMillis;

    /**
     * Creates a consumer that makes use of PostgreSQL LISTEN/NOTIFY via the standard JDBC driver.
     *
     * @param dataSource The datasource
     * @param channelName The LISTEN/NOTIFY channel name
     * @param pollingMillis The milliseconds to between polling requests
     */
    public AbstractPostgreSQLListenConsumer(DataSource dataSource, String channelName, long pollingMillis) {
        this.dataSource = dataSource;
        this.channelName = channelName;
        this.pollingMillis = pollingMillis;
    }

    @Override
    public void registerListener(ConsumerListener<T> listener) {
        listeners.add(listener);
    }

    /**
     * Converts the String payloads as received via PostgreSQL to the desired message type.
     *
     * @param payloads The String payloads
     * @return the desired message type
     */
    protected abstract List<T> convertPayload(List<String> payloads);

    /**
     * Returns whether the consumer is done and should stop.
     *
     * @return  whether the consumer is done and should stop
     */
    protected boolean isDone() {
        return Thread.currentThread().isInterrupted();
    }

    @Override
    public void run() {
        List<String> payloadList = new ArrayList<>();
        List<String> unmodifiableList = Collections.unmodifiableList(payloadList);
        while (!isDone()) {
            try (Connection connection = dataSource.getConnection()) {
                PGConnection pgConnection = connection.unwrap(PGConnection.class);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("LISTEN " + channelName);
                }

                while (!isDone()) {
                    try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT 1")) {
                        // Dummy query to receive pending notifications
                    }

                    org.postgresql.PGNotification[] notifications = pgConnection.getNotifications();
                    if (notifications != null) {
                        for (int i = 0; i < notifications.length; i++) {
                            payloadList.add(notifications[i].getParameter());
                        }

                        try {
                            List<T> messages = Collections.unmodifiableList(convertPayload(unmodifiableList));
                            payloadList.clear();
                            try {
                                listeners.forEach(l -> l.consume(messages));
                            } catch (Throwable t) {
                                LOG.log(Level.SEVERE, "Error in ConsumerListener", t);
                            }
                        } finally {
                            payloadList.clear();
                        }
                    }

                    // wait a while before checking again for new notifications
                    Thread.sleep(pollingMillis);
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
