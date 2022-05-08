package com.magicalpipelines;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.KStream; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication; 
import org.springframework.context.annotation.Bean;
 
import com.magicalpipelines.language.LanguageClient;
import com.magicalpipelines.model.EntitySentiment;
import com.magicalpipelines.serialization.Tweet;

import io.confluent.kafka.streams.serdes.avro.ReflectionAvroSerde;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Produced; 
 
/*****
 * refer: <br/>
 * https://stackoverflow.com/questions/68450799/how-to-do-this-topology-in-spring-cloud-kafka-streams-in-function-style
 * *****/
@SpringBootApplication
@Slf4j
public class CryptoSentimentSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoSentimentSpringApplication.class, args);
	}
	@Autowired
	private LanguageClient languageClient;
	
	private static final List<String> currencies = Arrays.asList("bitcoin", "ethereum");
	
	@Bean
	public Function<KStream<byte[], Tweet>, KStream<byte[], EntitySentiment>> handle()  {
		// match all tweets that specify English as the source language
		final Predicate<byte[], Tweet> englishTweets = (key, tweet) -> tweet.getLang().equals("en");

		// match all other tweets
		final Predicate<byte[], Tweet> nonEnglishTweets = (key, tweet) -> !tweet.getLang().equals("en");	    
	     
	   return stream -> {
		   
		       stream.print(Printed.<byte[], Tweet>toSysOut().withLabel("tweets-stream"));
		       
		       // filter out retweets
			    KStream<byte[], Tweet> filtered =
			    		stream.filterNot(
			            (key, tweet) -> {
			              return tweet.isRetweet();
			            });
		       
		       
			    // branch based on tweet language
			    KStream<byte[], Tweet>[] branches = filtered.branch(englishTweets, nonEnglishTweets);
             
			 // English tweets
			    KStream<byte[], Tweet> englishStream = branches[0];
			    englishStream.print(Printed.<byte[], Tweet>toSysOut().withLabel("tweets-english"));

			    // non-English tweets
			    KStream<byte[], Tweet> nonEnglishStream = branches[1];
			    nonEnglishStream.print(Printed.<byte[], Tweet>toSysOut().withLabel("tweets-non-english"));

			    // for non-English tweets, translate the tweet text first.
			    KStream<byte[], Tweet> translatedStream =
			        nonEnglishStream.mapValues(
			            (tweet) -> {
			              return languageClient.translate(tweet, "en");
			            });

			    // merge the two streams
			    KStream<byte[], Tweet> merged = englishStream.merge(translatedStream);

			    // enrich with sentiment and salience scores
			    // note: the EntitySentiment class is auto-generated from the schema
			    // definition in src/main/avro/entity_sentiment.avsc
			    final KStream<byte[], EntitySentiment> enriched =
			        merged.flatMapValues(
			            (tweet) -> {
			              // perform entity-level sentiment analysis
			              List<EntitySentiment> results = languageClient.getEntitySentiment(tweet);

			              // remove all entity results that don't match a currency
			              results.removeIf(
			                  entitySentiment -> !currencies.contains(entitySentiment.getEntity()));

			              return results;
			            });
			    enriched.to(
			            "crypto-sentiment",
			            Produced.with(
			                Serdes.ByteArray(),
			                // registryless Avro Serde
			                dataEntitySentimentAvroSerde()));
			    return enriched;
         };
	  }
	@Bean
	public Serde<EntitySentiment> dataEntitySentimentAvroSerde() {
		boolean isKeySerde = false;
//		final Map<String, String> serdeConfig = Collections.singletonMap(io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081/");
//		
//		final Serde<EntitySentiment> reflectionAvroSerde = new ReflectionAvroSerde<>();		
//		reflectionAvroSerde.configure(serdeConfig, isKeySerde);
//		return reflectionAvroSerde;
		
		
//		return com.mitchseymour.kafka.serialization.avro.AvroSerdes.get(EntitySentiment.class);

		return com.magicalpipelines.serialization.avro.AvroSerdes.EntitySentiment("http://localhost:8081/", isKeySerde);
	}
//		@Bean
//		public Consumer<KStream<byte[], EntitySentiment>> shippedConsumer() {
//			return input -> input.foreach((key, value) -> log.debug("THIS IS THE END! key: {} value: {}", key, value));
//		}
	
  }
