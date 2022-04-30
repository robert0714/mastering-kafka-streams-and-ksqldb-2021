package com.magicalpipelines;
 
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes; 
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;  
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.SessionStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.streams.test.TestRecord;
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
 * https://developer.confluent.io/learn-kafka/kafka-streams/stateful-operations/  <br/>
 * https://github.com/confluentinc/kafka-streams-examples/tree/7.0.0-post/src/test/java/io/confluent/examples/streams
 * **/
public class LeaderboardTopologyVersion2Test {
//	  @ClassRule
//	  public static final EmbeddedZookeeper CLUSTER = new EmbeddedSingleNodeKafkaCluster();
 
	
	private TopologyTestDriver testDriver;
	private TestInputTopic<String, ScoreEvent> inputTopicScoreEvent;
	 
	
	private TestInputTopic<String, Player> inputTopicPlayers;
	
	private TestInputTopic<String, Product> inputTopicProduct; 
	
	private TestOutputTopic<String, Enriched> outputTopicEnriched;
	private TestOutputTopic<String, HighScores> outputTopicHighScores;
	
	
	@BeforeEach
	protected void setUp() throws Exception {
		// build the topology with a dummy client
		Topology topology = LeaderboardTopologyVersion2.build();
 

		// set the required properties for running Kafka Streams
		Properties props = new Properties();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
		props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);  
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());  
		// create a test driver. we will use this to pipe data to our topology
	    testDriver = new TopologyTestDriver(topology, props);

	     
	    
	    
	    // build the topology
	    System.out.println("Starting Videogame Leaderboard");
	    
	    // create the test input topic: score events
	    this.inputTopicScoreEvent = testDriver.createInputTopic(
	    		"score-events", Serdes.String().serializer() ,JsonSerdes.ScoreEvent().serializer());
	    
	    // create the test input topic: players
	    this.inputTopicPlayers = testDriver.createInputTopic(
	    		"players", Serdes.String().serializer() ,JsonSerdes.Player().serializer());
	    
	    
	    // create the test input topic: products
	    this.inputTopicProduct = testDriver.createInputTopic(
	    		"products", Serdes.String().serializer() ,JsonSerdes.Product().serializer());
	    
	    
	    // create the test output topic: Enriched
	    this.outputTopicEnriched  = testDriver.createOutputTopic(
	    		"with-products",
	    		Serdes.String().deserializer() ,	    		
	    		JsonSerdes.Enriched().deserializer());
	    
	 // create the test output topic: Enriched
	    this.outputTopicHighScores  = testDriver.createOutputTopic(
	    		"high-scores",
	    		Serdes.String().deserializer() ,	    		
	    		JsonSerdes.HighScores().deserializer());
	}

	@AfterEach
	protected void tearDown() throws Exception {
		testDriver.close();
	}

	private String[] scoreEventData = {
			"{\"score\": 1000, \"product_id\": 1, \"player_id\": 1}",
			"{\"score\": 2000, \"product_id\": 1, \"player_id\": 2}",
			"{\"score\": 4000, \"product_id\": 1, \"player_id\": 3}",
			"{\"score\": 500, \"product_id\": 1, \"player_id\": 4}",
			"{\"score\": 800, \"product_id\": 6, \"player_id\": 1}",			
			"{\"score\": 2500, \"product_id\": 6, \"player_id\": 2}",
			"{\"score\": 9000.0, \"product_id\": 6, \"player_id\": 3}",
			"{\"score\": 1200.0, \"product_id\": 6, \"player_id\": 4}"
	};
	private String[] playerData = {
			"{\"id\": 1, \"name\": \"Elyse\"}",
			"{\"id\": 2, \"name\": \"Mitch\"}",
			"{\"id\": 3, \"name\": \"Isabelle\"}",
			"{\"id\": 4, \"name\": \"Sammy\"}" 
	};
	private String[] productData = {
			"{\"id\": 1, \"name\": \"Super Smash Bros\"}",
			"{\"id\": 6, \"name\": \"Mario Kart\"}" 
	};
	@Test
	public void videogame()   { 

		for(String data:scoreEventData ) {
			final ScoreEvent scoreEvent = JsonSerdes.ScoreEvent().deserializer().deserialize("score-events",
					data.getBytes());
			inputTopicScoreEvent.pipeInput("score-events", scoreEvent);
		}		
		
		for(String data:playerData ) {
			final Player player = JsonSerdes.Player().deserializer().deserialize("players",
					data.getBytes());
			inputTopicPlayers.pipeInput("players", player);
		}
		 
		for(String data:productData ) {
			final Product product =  JsonSerdes.Product().deserializer().deserialize("products",
					data.getBytes());
			inputTopicProduct.pipeInput("products", product);
		}

		testDriver.advanceWallClockTime(Duration.ofMillis(20L));
		 
		System.out.println("---------StateStores-------------");
		testDriver.getAllStateStores().forEach((name,stateStore) ->{
			System.out.println(name);
			System.out.println(stateStore.isOpen());
//			final	KeyValueStore<Object, Object> keyValueStore = testDriver.getKeyValueStore(name);
			final	SessionStore<Object, Object> sessionoStore = testDriver.getSessionStore(name);
//			final	KeyValueStore<Object, ValueAndTimestamp<Object>> timestampedKeyValueStore =  testDriver.getTimestampedKeyValueStore(name);
//			final	WindowStore<Object, ValueAndTimestamp<Object>> timestampedWindowStore = testDriver.getTimestampedWindowStore(name);
			final	WindowStore<Object, Object> windowStore = testDriver.getWindowStore(name);			
			System.out.println("----------------------");
		});
		 
		System.out.println("---------Topic name-------------");
		Set<String> topicNames = testDriver.producedTopicNames();
		System.out.println(topicNames.size());
		topicNames.forEach(name ->{
			System.out.println(name);
		}); 
		//"leader-boards" ,"high-scores"
		 KeyValueStore<Object, Object> astore = testDriver.getKeyValueStore("leader-boards");
		 assertThat(astore).isNull();
		 testDriver.advanceWallClockTime(Duration.ofMillis(20L));
		 
		 
		final  List<TestRecord<String, Enriched>> enrichedList = this.outputTopicEnriched.readRecordsToList();
		final List<TestRecord<String, HighScores>> highScoresList = this.outputTopicHighScores.readRecordsToList();
		 assertThat(enrichedList).isNotNull();
		 assertThat(highScoresList).isNotNull();
		 assertThat(enrichedList).isEmpty();
		 assertThat(highScoresList).isEmpty();
		  
		 //TODO we need method to test store for stateful operator to process  kafka stream
		 //TODO how to test join operator
		 //TODO how to test group operator
		 //TODO how to test aggregate operator
		 // too hard to test . https://github.com/a0x8o/kafka/blob/master/streams/src/test/java/org/apache/kafka/streams/integration/QueryableStateIntegrationTest.java
	}

}
