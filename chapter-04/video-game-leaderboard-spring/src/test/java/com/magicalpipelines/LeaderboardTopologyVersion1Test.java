package com.magicalpipelines;

import java.util.Properties; 
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.magicalpipelines.model.Player;
import com.magicalpipelines.model.Product;
import com.magicalpipelines.model.ScoreEvent;
import com.magicalpipelines.model.join.Enriched;
import com.magicalpipelines.serialization.json.JsonSerdes;
/**
 * https://kafka.apache.org/30/documentation/streams/developer-guide/testing.html <br/>
 * https://kafka.apache.org/27/documentation/streams/developer-guide/testing.html <br/>
 * https://www.confluent.io/blog/testing-kafka-streams/  <br/>
 * https://developer.confluent.io/learn-kafka/kafka-streams/stateful-operations/   <br/>
 * https://kafka.apache.org/27/documentation/streams/developer-guide/dsl-api.html#join-co-partitioning-requirements
 * **/
public class LeaderboardTopologyVersion1Test {
	private TopologyTestDriver testDriver;
	private TestInputTopic<byte[], ScoreEvent> inputTopicScoreEvent;
	 
	
	private TestInputTopic<String, Player> inputTopicPlayers;
	
	private TestInputTopic<String, Product> inputTopicProduct;
 
	@BeforeEach
	protected void setUp() throws Exception {
		// build the topology with a dummy client
		Topology topology = LeaderboardTopologyVersion1.build();
 

		// set the required properties for running Kafka Streams
		Properties props = new Properties();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
		props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);  
		// create a test driver. we will use this to pipe data to our topology
	    testDriver = new TopologyTestDriver(topology, props);

	    // build the topology
	    System.out.println("Starting Videogame Leaderboard");
	    
	    // create the test input topic: score events
	    this.inputTopicScoreEvent = testDriver.createInputTopic(
	    		"score-events", Serdes.ByteArray().serializer() ,JsonSerdes.ScoreEvent().serializer());
	    
	    // create the test input topic: players
	    this.inputTopicPlayers = testDriver.createInputTopic(
	    		"score-events", Serdes.String().serializer() ,JsonSerdes.Player().serializer());
	    
	    
	    // create the test input topic: products
	    this.inputTopicProduct = testDriver.createInputTopic(
	    		"score-events", Serdes.String().serializer() ,JsonSerdes.Product().serializer());
	}

	@AfterEach
	protected void tearDown() throws Exception {
		testDriver.close();
	}

	@Test
	public void testBuild() {
		ScoreEvent scoreEvent= new  ScoreEvent() ;
		scoreEvent.setPlayerId(2L);
		scoreEvent.setProductId(6L);
		scoreEvent.setScore(422D);
		inputTopicScoreEvent.pipeInput( new byte[] {},scoreEvent);
		
		final Player player = JsonSerdes.Player().deserializer().deserialize("players",
				"{\"id\": 1, \"name\": \"Elyse\"}".getBytes());
		
		
		Product product = new Product();
		product.setId(1L);
		product.setName("Super Smash Bros");
				
		inputTopicProduct.pipeInput("",product);
		inputTopicPlayers.pipeInput("", player);
		
		 
	}

}
