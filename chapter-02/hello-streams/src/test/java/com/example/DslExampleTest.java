package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.kstream.KStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DslExampleTest {
  private TopologyTestDriver testDriver;
  private TestInputTopic<Void, String> inputTopic;

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;

  @BeforeEach
  protected void setUp() throws Exception {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));

    // the builder is used to construct the topology
    StreamsBuilder builder = new StreamsBuilder();

    // read from the source topic, "users"
    KStream<Void, String> stream = builder.stream("users");

    // for each record that appears in the source topic,
    // print the value (as SayHelloProcessor)
    stream.foreach(
        (key, value) -> {
          System.out.println("(DSL) Hello, " + value);
        });

    // the builder is used to construct the topology
    Topology topology = builder.build();

    // create a test driver. we will use this to pipe data to our topology
    testDriver = Utils.createTestDriverAndTestTopic(topology);

    // create the test input topic
    inputTopic =
        testDriver.createInputTopic(
            "users", Serdes.Void().serializer(), Serdes.String().serializer());
  }

  @Test
  public void testMain() {
    // stimulate to  use topic named "users"
    inputTopic.pipeInput("Robert");
    assertThat(outContent.toString()).isEqualTo("(DSL) Hello, Robert" + System.lineSeparator());
  }

  @AfterEach
  public void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }
}
