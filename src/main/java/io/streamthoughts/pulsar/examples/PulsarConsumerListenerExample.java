/*
 * Copyright 2020 StreamThoughts.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.streamthoughts.pulsar.examples;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.MessageListener;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;

import java.util.ArrayList;
import java.util.List;

public class PulsarConsumerListenerExample {

    public static void main(String[] args) throws PulsarClientException {

        if (args.length != 3) {
            System.err.println("Missing arguments: pulsarServiceUrl, topic, subscriptionName, ");
            System.exit(-1);
        }

        var serviceUrl =  args[0];
        var topic =  args[1];
        var subscription =  args[2];
        var numListenerThread = Integer.parseInt(args[2]);

        PulsarClient client = PulsarClient
            .builder()
            .serviceUrl(serviceUrl)
            .listenerThreads(numListenerThread)
            .build();

        final List<Consumer<?>> consumers = new ArrayList<>();
        for (int i = 0; i < numListenerThread; i++) {
            consumers.add(createConsumerWithLister(client, topic, subscription, "C" + i));
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Consumer<?> consumer : consumers) {
                try {
                    consumer.close();
                } catch (PulsarClientException e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    private static Consumer<String> createConsumerWithLister(final PulsarClient client,
                                                             final String topic,
                                                             final String subscription,
                                                             final String consumerName) throws PulsarClientException {
        return client.newConsumer(Schema.STRING)
                .topic(topic)
                .consumerName(consumerName)
                .subscriptionName(subscription)
                .subscriptionMode(SubscriptionMode.Durable)
                .subscriptionType(SubscriptionType.Failover)
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .messageListener((MessageListener<String>) (consumer, msg) -> {
                    System.out.printf(
                        "[%s/%s]Message received: key=%s, value=%s, topic=%s, id=%s%n",
                        consumerName,
                        Thread.currentThread().getName(),
                        msg.getKey(),
                        msg.getValue(),
                        msg.getTopicName(),
                        msg.getMessageId().toString());
                    consumer.acknowledgeAsync(msg);
                })
                .subscribe();
    }
}