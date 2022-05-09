package com.magicalpipelines;

import java.util.function.BiFunction;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.ValueJoiner;
import org.apache.kafka.streams.state.HostInfo; 
import org.apache.kafka.streams.state.KeyValueStore;  
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
	public HostInfo getHostInfo() {
		return new  HostInfo("localhost",8080);
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
		// join params for scoreEvents -> players join
		final Joined<String, ScoreEvent, Player> playerJoinParams = Joined.with(Serdes.String(),
				JsonSerdes.ScoreEvent(), JsonSerdes.Player());

		// join scoreEvents -> players
		final ValueJoiner<ScoreEvent, Player, ScoreWithPlayer> scorePlayerJoiner = (score,
				player) -> new ScoreWithPlayer(score, player);

		return (scoreEvents, players) -> scoreEvents
				// now marked for re-partitioning
				.selectKey((k, v) -> v.getPlayerId().toString())
				 // join scoreEvents -> players
				.join(players,	scorePlayerJoiner, playerJoinParams)
				.peek((k, v) -> log.info("Done -> {}", v));
	}
	
	// create the global product table
	@Bean
	public BiFunction<KStream<String, ScoreWithPlayer>, GlobalKTable<String, Product>  ,KStream<String, HighScores> >  jointProducts()  {
		final KeyValueMapper<String, ScoreWithPlayer, String> keyMapper = Topology.keyMapper;
		final ValueJoiner<ScoreWithPlayer, Product, Enriched> productJoiner =Topology.productJoiner;
		
		return (withPlayers ,products)->{		
			withPlayers.print(Printed.<String, ScoreWithPlayer>toSysOut().withLabel("withPlayers"));
						
			KStream<String, Enriched> withProducts = withPlayers.join(products, keyMapper, productJoiner);
			
			withProducts.print(Printed.<String, Enriched>toSysOut().withLabel("with-products"));
			
			
			/** Group the enriched product stream */
		    final KGroupedStream<String, Enriched> grouped = 
		    withProducts.groupBy(
		            (key, value) -> value.getProductId().toString(),
		            Grouped.with(Serdes.String(), JsonSerdes.Enriched()));
		    
		    
		 // alternatively, use the following if you want to name the grouped repartition topic:
		    // Grouped.with("grouped-enriched", Serdes.String(), JsonSerdes.Enriched()))

		    /** The initial value of our aggregation will be a new HighScores instances */
		    Initializer<HighScores> highScoresInitializer = HighScores::new;

		    /** The logic for aggregating high scores is implemented in the HighScores.add method */
		    Aggregator<String, Enriched, HighScores> highScoresAdder =
		        (key, value, aggregate) -> aggregate.add(value);

		    /** Perform the aggregation, and materialize the underlying state store for querying */
		    KTable<String, HighScores> highScores =
		        grouped.aggregate(
		            highScoresInitializer,
		            highScoresAdder,
		            Materialized.<String, HighScores, KeyValueStore<Bytes, byte[]>>
		                // give the state store an explicit name to make it available for interactive
		                // queries
		                as("leader-boards")
		                .withKeySerde(Serdes.String())
		                .withValueSerde(JsonSerdes.HighScores()));
		     final   KStream<String, HighScores> stream = highScores.toStream() ;
		     
		     stream.print(Printed.<String, HighScores>toSysOut().withLabel("high-scores"));
			 return stream ;
		};
	}
  }
