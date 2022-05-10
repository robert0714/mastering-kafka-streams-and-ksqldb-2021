package com.magicalpipelines.api;

import static java.util.logging.Level.FINE;

import java.util.HashMap; 
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata; 
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.springframework.http.HttpStatus; 
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate; 
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.magicalpipelines.HighScores;
import com.magicalpipelines.model.join.Enriched;
 
import lombok.extern.slf4j.Slf4j; 
import reactor.core.publisher.Mono;

@Slf4j
public class LeaderboardHandler { 
    private final KafkaComponent kafkaComponent;
     
    private   final HostInfo hostInfo; 
     
	public LeaderboardHandler(final HostInfo hostInfo,  final KafkaComponent kafkaComponent) {
		this.hostInfo = hostInfo;
		this.kafkaComponent = kafkaComponent;
	}
	 
	public Mono<ServerResponse> getAll(final ServerRequest request) {
		Map<String, List<Enriched>> leaderboard = new HashMap<>();
				
		try (KeyValueIterator<String, HighScores> range = getStore().all()) {
			range.forEachRemaining((next) -> {
				String game = next.key;
				HighScores highScores = next.value;
				leaderboard.put(game, highScores.toList());
			});
		}		 
		return ServerResponse.ok().bodyValue(leaderboard).log(log.getName(), FINE) ;
	} 
	public Mono<ServerResponse>  getCount(final ServerRequest request){
		long count = kafkaComponent.getStore().approximateNumEntries();

	    for (StreamsMetadata metadata : getStream().allMetadataForStore("leader-boards")) {
	      if (!hostInfo.equals(metadata.hostInfo())) {
	        continue;
	      }
	      count += fetchCountFromRemoteInstance(metadata.hostInfo().host(), metadata.hostInfo().port());
	    }
		return ServerResponse.ok().bodyValue(Long.valueOf(count)).log(log.getName(), FINE) ;
	}
	long fetchCountFromRemoteInstance(String host, int port) {
		final RestTemplate client = new RestTemplate();

		final String url = String.format("http://%s:%d/leaderboard/count/local", host, port);

		try {
			return client.getForObject(url, Long.class);
		} catch (Exception e) {
			// log error
			log.error("Could not get leaderboard count", e);
			return 0L;
		}
	}

	public Mono<ServerResponse> getCountLocal(final ServerRequest request) {
		long count = 0L;
		try {
			count = getStore().approximateNumEntries();
		} catch (Exception e) {
			log.error("Could not get local leaderboard count", e);
		} finally {
			return ServerResponse.ok().bodyValue(Long.valueOf(count)).log(log.getName(), FINE);
		}
	}
	public Mono<ServerResponse> getRange(final ServerRequest request) {
		final String from = request.pathVariable("from");
		final String to = request.pathVariable("to");
		Map<String, List<Enriched>> leaderboard = new HashMap<>();
 
		try (KeyValueIterator<String, HighScores> range = getStore().range(from, to)) {
			range.forEachRemaining((next) -> {
				String game = next.key;
				HighScores highScores = next.value;
				leaderboard.put(game, highScores.toList());
			});
		}
		// return a JSON response
		return ServerResponse.ok().bodyValue(leaderboard).log(log.getName(), FINE) ;
	}
	public Mono<ServerResponse> getKey(final ServerRequest request) {
		final String productId = request.pathVariable("key");
		
		 // find out which host has the key
        KeyQueryMetadata metadata =
        		getStream() .queryMetadataForKey("leader-boards", productId, Serdes.String().serializer());

		
		 // the local instance has this key
        if (hostInfo.equals(metadata.activeHost())) {
          log.info("Querying local store for key");
          HighScores highScores = getStore().get(productId);

          if (highScores == null) {
            // game was not found 
            return ServerResponse.notFound().build();
          }

          // game was found, so return the high scores 
          return ServerResponse.ok().bodyValue(highScores.toList()).log(log.getName(), FINE) ;
        }

        // a remote instance has the key
        String remoteHost = metadata.activeHost().host();
        int remotePort = metadata.activeHost().port();
        String url =
            String.format(
                "http://%s:%d/leaderboard/%s",
                // params
                remoteHost, remotePort, productId);
        
        try {
        	log.info("Querying remote store for key");
			String result =  new RestTemplate().getForEntity(url, String.class).getBody();
			
			return  ServerResponse.ok().bodyValue(result).log(log.getName(), FINE) ;
		} catch (RestClientException e) { 
			e.printStackTrace();
			return  ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		} 
	}
	private KafkaStreams getStream() {
		return kafkaComponent.getStream();
	}
    private ReadOnlyKeyValueStore<String, HighScores> getStore() {
		return kafkaComponent.getStore();
	}
}
