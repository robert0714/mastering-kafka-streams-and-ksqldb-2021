package com.magicalpipelines;

import java.util.function.BiFunction;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.HostInfo; 
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.magicalpipelines.model.Player;
import com.magicalpipelines.model.Product;
import com.magicalpipelines.model.ScoreEvent;
import com.magicalpipelines.model.join.Enriched;
import com.magicalpipelines.model.join.ScoreWithPlayer;
import com.magicalpipelines.serialization.json.JsonSerdes;

import lombok.extern.slf4j.Slf4j; 

@SpringBootApplication
@Slf4j
public class VideoGameLeaderboardSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideoGameLeaderboardSpringApplication.class, args);
	}

	@Bean
	public HostInfo getHostInfo(@Autowired final  KafkaBinderConfigurationProperties propertes) {
		final String data = propertes.getConfiguration().get(StreamsConfig.APPLICATION_SERVER_CONFIG);		
		return HostInfo.buildFromEndpoint(data);
	}
	//For spring-cloud-stream to auto-register Serializer/Deserializer;
	/***
	 * https://docs.spring.io/spring-cloud-stream-binder-kafka/docs/3.1.6/reference/html/spring-cloud-stream-binder-kafka.html#_record_serialization_and_deserialization
	 * **/
	@Bean
    public Serde<HighScores> highScoresJsonSerde() {
        return JsonSerdes.HighScores();
    }
	/**
	 * refer: https://piotrminkowski.com/2021/11/11/kafka-streams-with-spring-cloud-stream/<br/>
	 * https://github.com/piomin/sample-spring-cloud-stream-kafka
	 * **/
	@Bean
	public BiFunction<KStream<String, ScoreEvent>, KTable<String, Player>, KStream<String, ScoreWithPlayer>> withPlayeers() {
		return (scoreEvents, players) -> scoreEvents
				// now marked for re-partitioning
				.selectKey((k, v) -> v.getPlayerId().toString())
				 // join scoreEvents -> players
				.join(players,	Topology.scorePlayerJoiner, Topology.playerJoinParams)
				.peek((k, v) -> log.info("Done -> {}", v));
	}
	
	 
	@Bean
	public BiFunction<KStream<String, ScoreWithPlayer>, GlobalKTable<String, Product>  ,KStream<String, HighScores> >  jointProducts()  {
		 	
		return (withPlayers ,products)->{	
						
			KStream<String, Enriched> withProducts = 
				withPlayers
					.join( products, Topology.keyMapper, Topology.productJoiner)
					.peek((k, v) -> log.info("withProducts -> {}", v));
						
			
			/** Group the enriched product stream */
		    KGroupedStream<String, Enriched> grouped = 
		    withProducts		    
		    .groupBy(
		            (key, value) -> value.getProductId().toString(),
		            Grouped.with(Serdes.String(), JsonSerdes.Enriched()));		      
		    // alternatively, use the following if you want to name the grouped repartition topic:
		    // Grouped.with("grouped-enriched", Serdes.String(), JsonSerdes.Enriched()))

		     

		    /** Perform the aggregation, and materialize the underlying state store for querying */
		    KTable<String, HighScores> highScores =
		        grouped.aggregate(
		        		Topology.highScoresInitializer,
		        		Topology.highScoresAdder,
		            Materialized.<String, HighScores, KeyValueStore<Bytes, byte[]>>
		                // give the state store an explicit name to make it available for interactive
		                // queries
		                as("leader-boards")
		                .withKeySerde(Serdes.String())
		                .withValueSerde(JsonSerdes.HighScores()));
		    
		     KStream<String, HighScores> stream = highScores
		    		 .toStream()
		    		 .peek((k, v) -> log.info("high-scores -> {}", v));
			 return stream ;
		};
	}
  }
