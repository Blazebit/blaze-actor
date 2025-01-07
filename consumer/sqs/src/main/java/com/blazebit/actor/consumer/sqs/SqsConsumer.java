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

package com.blazebit.actor.consumer.sqs;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.blazebit.actor.spi.Consumer;
import com.blazebit.actor.spi.ConsumerListener;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.IllegalStateException;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.amazon.sqs.javamessaging.SQSConnection;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * A consumer for the AWS SQS API that makes use of the JMS wrapper.
 * ConsumerListeners shouldn't acknowledge, this will happen in SqsConsumer in a batched way.
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
public class SqsConsumer implements Consumer<Message>, Runnable {

    private static final Logger LOG = Logger.getLogger(SqsConsumer.class.getName());
    private final List<ConsumerListener<Message>> listeners = new CopyOnWriteArrayList<>();
    private final MessageConsumer messageConsumer;
    private volatile boolean closed;

    /**
     * Creates a consumer that makes use of JMS wrapper API for the AWS SQS API.
     *
     * @param messageConsumer The message consumer
     */
    public SqsConsumer(MessageConsumer messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void registerListener(ConsumerListener<Message> listener) {
        listeners.add(listener);
    }

    /**
     * Returns whether the consumer is done and should stop.
     *
     * @return  whether the consumer is done and should stop
     */
    protected boolean isDone() {
        return closed;
    }

    @Override
    public void run() {
        List<Message> messages = new ArrayList<>();
        List<Message> unmodifiableList = Collections.unmodifiableList(messages);
        while (!isDone()) {
            try {
                // Blocked wait
                Message msg = messageConsumer.receive();
                messages.add(msg);
                // When we found one element, drain the rest that is available
                // TODO: Maybe allow to configure a limit?
                while ((msg = messageConsumer.receiveNoWait()) != null) {
                    messages.add(msg);
                }
                try {
                    boolean error = true;
                    try {
                        listeners.forEach(l -> l.consume(unmodifiableList));
                        error = false;
                    } catch (Throwable t) {
                        LOG.log(Level.SEVERE, "Error in ConsumerListener", t);
                    } finally {
                        // We only acknowledge when no errors happened
                        if (!error) {
                            // We acknowledge the last message first so that the RANGE acknowledge mode can batch acknowledge messages
                            for (int i = messages.size() - 1; i >= 0; i--) {
                                messages.get(i).acknowledge();
                            }
                        }
                    }
                } finally {
                    messages.clear();
                }
            } catch (IllegalStateException e) {
                // This can only happen due to the message consumer being closed
                closed = true;
                // But make sure it's closed anyway
                LOG.log(Level.SEVERE, "Closing Consumer as the underlying MessageConsumer seems closed", e);
                try {
                    messageConsumer.close();
                } catch (Throwable ex) {
                    LOG.log(Level.SEVERE, "Error while closing MessageConsumer", ex);
                }
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Error in Consumer", t);
            }
        }
    }

    /**
     * A configuration for AWS SQS consumers.
     *
     * @author Christian Beikov
     * @since 1.0.0
     */
    public static class Config {

        private final String accessKey;
        private final String secretKey;
        private final String region;
        private final String queueName;
        private final int prefetchSize;

        private Config(String accessKey, String secretKey, String region, String queueName, int prefetchSize) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            this.region = region;
            this.queueName = queueName;
            this.prefetchSize = prefetchSize;
        }

        /**
         * Returns the configured AWS access key.
         *
         * @return the configured AWS access key
         */
        public String getAccessKey() {
            return accessKey;
        }

        /**
         * Returns the configured AWS secret key.
         *
         * @return the configured AWS secret key
         */
        public String getSecretKey() {
            return secretKey;
        }

        /**
         * Returns the configured AWS region.
         *
         * @return the configured AWS region
         */
        public String getRegion() {
            return region;
        }

        /**
         * Returns the configured queue name.
         *
         * @return the configured queue name
         */
        public String getQueueName() {
            return queueName;
        }

        /**
         * Creates the given amount of new Blaze-Actor {@link SqsConsumer} objects for partitioned consumption.
         *
         * @param connection The connection for which to create the consumers
         * @param consumers The amount of consumers to create
         * @return the new Blaze-Actor SqsConsumers
         */
        public List<SqsConsumer> createConsumers(SQSConnection connection, int consumers) {
            List<SqsConsumer> consumerList = new ArrayList<>(consumers);
            for (int i = 0; i < consumers; i++) {
                consumerList.add(createConsumer(connection));
            }
            return consumerList;
        }

        /**
         * Creates a new Blaze-Actor {@link SqsConsumer}.
         *
         * @param connection The connection for which to create the consumer
         * @return a new Blaze-Actor SqsConsumer
         */
        public SqsConsumer createConsumer(SQSConnection connection) {
            Session session = null;
            try {
                session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                return new SqsConsumer(session.createConsumer(session.createQueue(queueName)));
            } catch (Throwable e) {
                if (session != null) {
                    try {
                        session.close();
                    } catch (Throwable ex) {
                        e.addSuppressed(ex);
                    }
                }
                throw new RuntimeException("Couldn't create consumer", e);
            }
        }

        /**
         * Creates a new SQS JMS {@link Connection}.
         *
         * @return a new SQS JMS Connection
         */
        public SQSConnection createConnection() {
            SQSConnectionFactory connectionFactory = createConnectionFactory();
            SQSConnection connection = null;
            try {
                return connectionFactory.createConnection();
            } catch (Throwable e) {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (JMSException ex) {
                        e.addSuppressed(ex);
                    }
                }
                throw new RuntimeException("Couldn't create Connection", e);
            }
        }

        /**
         * Creates a new SQS JMS {@link ConnectionFactory}.
         *
         * @return a new SQS JMS ConnectionFactory
         */
        public SQSConnectionFactory createConnectionFactory() {
            AwsCredentials credentials;

            if (accessKey == null || secretKey == null) {
                throw new IllegalArgumentException("No AWS access key and secret key given for SQS queue!");
            } else {
                credentials = AwsBasicCredentials.create(accessKey, secretKey);
            }

            if (region == null) {
                throw new IllegalArgumentException("No AWS region given for SQS queue!");
            }

            ProviderConfiguration providerConfiguration = new ProviderConfiguration();
            providerConfiguration.withNumberOfMessagesToPrefetch(prefetchSize);
            SqsClient sqs = SqsClient.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.of(region))
                    .build();
            return new SQSConnectionFactory(providerConfiguration, sqs);
        }

        /**
         * Creates a new builder.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * A builder for a AWS SQS configuration.
         *
         * @author Christian Beikov
         * @since 1.0.0
         */
        public static class Builder {
            private static final int DEFAULT_PREFETCH_SIZE = 10;
            private String accessKey;
            private String secretKey;
            private String region;
            private String queueName;
            private int prefetchSize = DEFAULT_PREFETCH_SIZE;

            /**
             * Returns a new configuration.
             *
             * @return a new configuration
             */
            public Config build() {
                return new Config(accessKey, secretKey, region, queueName, prefetchSize);
            }

            /**
             * Sets the given AWS access key.
             *
             * @param accessKey The AWS access key
             * @return this for chaining
             */
            public Builder withAccessKey(String accessKey) {
                this.accessKey = accessKey;
                return this;
            }

            /**
             * Sets the given AWS secret key.
             *
             * @param secretKey The AWS secret key
             * @return this for chaining
             */
            public Builder withSecretKey(String secretKey) {
                this.secretKey = secretKey;
                return this;
            }

            /**
             * Sets the given AWS region.
             *
             * @param region The AWS region
             * @return this for chaining
             */
            public Builder withRegion(String region) {
                this.region = region;
                return this;
            }

            /**
             * Sets the given AWS SQS queue name.
             *
             * @param queueName The queue name
             * @return this for chaining
             */
            public Builder withQueueName(String queueName) {
                this.queueName = queueName;
                return this;
            }

            /**
             * Sets the given prefetch size.
             *
             * @param prefetchSize The prefetch size
             * @return this for chaining
             */
            public Builder withPrefetchSize(int prefetchSize) {
                this.prefetchSize = prefetchSize;
                return this;
            }
        }
    }
}
