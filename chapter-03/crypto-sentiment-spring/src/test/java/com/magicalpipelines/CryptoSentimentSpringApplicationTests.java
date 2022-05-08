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
import com.magicalpipelines.serialization.Tweet;
import com.magicalpipelines.serialization.json.TweetSerdes; 

@SpringBootTest 
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:29092", "port=29092" }, topics = {  "tweets" })
class CryptoSentimentSpringApplicationTests {
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
		final TweetSerdes serds = new TweetSerdes(); 
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafkaBroker.getBrokersAsString());
		senderProps.put("key.serializer", ByteArraySerializer.class);
		senderProps.put("value.serializer", serds.serializer().getClass());
		
		final DefaultKafkaProducerFactory<byte[], Tweet> factory
		= new DefaultKafkaProducerFactory<>(senderProps);
		
		KafkaTemplate<byte[],Tweet> template = new KafkaTemplate<>(factory, true);
		template.setDefaultTopic("tweets");
		Thread.sleep(1_000L);
		
		
		for (String unit: tweetData) {
			final Tweet tweet = serds.deserializer().deserialize("tweets", unit.getBytes());
			template.sendDefault(tweet);
		}
		
	} 
	private String[] tweetData = {
			"{\"CreatedAt\":1577933872630,\"Id\":10005,\"Text\":\"Bitcoin has a lot of promise. I'm not too sure about #ethereum\",\"Lang\":\"en\",\"Retweet\":false,\"Source\":\"\",\"User\":{\"Id\":\"14377870\",\"Name\":\"MagicalPipelines\",\"Description\":\"Learn something magical today.\",\"ScreenName\":\"MagicalPipelines\",\"URL\":\"http://www.magicalpipelines.com\",\"FollowersCount\":\"248247\",\"FriendsCount\":\"16417\"}}\n"
			,
			"{\"CreatedAt\":1577933871912,\"Id\":10006,\"Text\":\"RT Bitcoin has a lot of promise. I'm not too sure about #ethereum\",\"Lang\":\"en\",\"Retweet\":true,\"Source\":\"\",\"User\":{\"Id\":\"14377871\",\"Name\":\"MagicalPipelines\",\"Description\":\"\",\"ScreenName\":\"Mitch\",\"URL\":\"http://blog.mitchseymour.com/\",\"FollowersCount\":\"120\",\"FriendsCount\":\"120\"}}\n"
			 
	};
}
