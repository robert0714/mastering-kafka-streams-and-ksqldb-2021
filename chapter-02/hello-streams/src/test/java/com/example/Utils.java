package com.example;

import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;

public class Utils {

  /***
   *   set the required properties for running Kafka Streams
   * **/
  static Properties createPros() {
    Properties config = new Properties();
    config.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
    config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Void().getClass());
    config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    return config;
  }

  /****
   *  create a test driver. we will use this to pipe data to our topology
   * **/
  static TopologyTestDriver createTestDriverAndTestTopic(final Topology topology) {
    // set the required properties for running Kafka Streams
    Properties config = createPros();

    // create a test driver. we will use this to pipe data to our topology
    TopologyTestDriver testDriver = new TopologyTestDriver(topology, config);

    return testDriver;
  }
}
