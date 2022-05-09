package com.magicalpipelines;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
 
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicalpipelines.model.Player;
import com.magicalpipelines.model.Product;
import com.magicalpipelines.model.ScoreEvent;
import com.magicalpipelines.serialization.json.JsonSerdes;

@SpringBootTest 
@DirtiesContext
@EmbeddedKafka(partitions = 5, brokerProperties = { "listeners=PLAINTEXT://localhost:29092", "port=29092" }, topics = {  "score-events","players","products","with-players" ,"high-scores"  })
class VideoGameLeaderboardSpringApplicationTests {
	private ObjectMapper om ;
	@BeforeEach
	protected void setUp() throws Exception { 
		this.om = new ObjectMapper();
	}
	@Autowired
	private EmbeddedKafkaBroker embeddedKafkaBroker;
	 
//	@Test
//	void contextLoads() {
//		 
//	}
	@Test
	public void testSendReceive() throws Exception {
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafkaBroker.getBrokersAsString());
		senderProps.put("key.serializer", ByteArraySerializer.class);
		senderProps.put("value.serializer", ByteArraySerializer.class);
		
		final DefaultKafkaProducerFactory<byte[], byte[]> factory
		= new DefaultKafkaProducerFactory<>(senderProps);
		
		KafkaTemplate<byte[], byte[]> template = new KafkaTemplate<>(factory, true);
		
		
		
		for(String data:scoreEventData ) {
			final ScoreEvent scoreEvent = JsonSerdes.ScoreEvent().deserializer().deserialize("score-events",
					data.getBytes()); 
			template.setDefaultTopic("score-events");
			template.sendDefault(this.om.writeValueAsBytes(scoreEvent));
		}		
		
		for(String data:playerData ) {
			final Player player = JsonSerdes.Player().deserializer().deserialize("players",
					data.getBytes()); 
			template.setDefaultTopic("players");
			template.sendDefault(this.om.writeValueAsBytes(player));
		}
		 
		for(String data:productData ) {
			final Product product =  JsonSerdes.Product().deserializer().deserialize("products",
					data.getBytes()); 
			template.setDefaultTopic("products");
			template.sendDefault(this.om.writeValueAsBytes(product));
		}
		 
		
		 
		
		factory.destroy();
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
}
