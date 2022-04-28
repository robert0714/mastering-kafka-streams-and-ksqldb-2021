package com.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProcessorApiExampleTest {
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

    Topology topology = new Topology();
    topology.addSource("UserSource", "users");
    topology.addProcessor("SayHello", SayHelloProcessor::new, "UserSource");

    // create a test driver. we will use this to pipe data to our topology
    testDriver = Utils.createTestDriverAndTestTopic(topology);

    // create the test input topic
    inputTopic =
        testDriver.createInputTopic(
            "users", Serdes.Void().serializer(), Serdes.String().serializer());
  }

  @Test
  public void testMain() {
    // stimulate to use topic named "users"
    inputTopic.pipeInput("Robert");
    assertThat(outContent.toString())
        .isEqualTo("(Processor API) Hello, Robert" + System.lineSeparator());
  }

  @AfterEach
  public void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }
}
