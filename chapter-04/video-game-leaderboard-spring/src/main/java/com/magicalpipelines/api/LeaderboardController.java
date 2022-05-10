package com.magicalpipelines.api;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.magicalpipelines.HighScores;
import com.magicalpipelines.model.join.Enriched;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leaderboard")
@Slf4j
@ConditionalOnProperty(prefix = "api", name = "mode", havingValue = "SERVLET")
public class LeaderboardController {
	@Autowired
    private KafkaComponent kafkaComponent;
    
    @Autowired
    private   HostInfo hostInfo; 
     
	 /** Local key-value store query: all entries */
    @GetMapping
    @ResponseBody
    public Map<String, List<Enriched>> getAll() { 
    	Map<String, List<Enriched>> leaderboard = new HashMap<>();		
		try (KeyValueIterator<String, HighScores> range = getStore().all()) {
			range.forEachRemaining((next) -> {
				String game = next.key;
				HighScores highScores = next.value;
				leaderboard.put(game, highScores.toList());
			});
		}
        return leaderboard;
    }
    
    /** Local key-value store query: approximate number of entries */
	@GetMapping("/count")
	@ResponseBody
	public long getCount() {
		long count = kafkaComponent.getStore().approximateNumEntries();

	    for (StreamsMetadata metadata : getStream().allMetadataForStore("leader-boards")) {
	      if (!hostInfo.equals(metadata.hostInfo())) {
	        continue;
	      }
	      count += fetchCountFromRemoteInstance(metadata.hostInfo().host(), metadata.hostInfo().port());
	    }
		return count;
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

	/** Local key-value store query: approximate number of entries */
	@GetMapping("/count/local")
	@ResponseBody
	public String getCountLocal() {
	    long count = 0L;
	    try {
	      count = getStore().approximateNumEntries();
	    } catch (Exception e) {
	      log.error("Could not get local leaderboard count", e);
	    } finally {
	      return String.valueOf(count);
	    }
	}
	
	/** Local key-value store query: range scan (inclusive) */
	@GetMapping("/{from}/{to}")
	@ResponseBody
	public Map<String, List<Enriched>> getRange(@PathVariable("from") String from, @PathVariable("to") String to) {
		Map<String, List<Enriched>> leaderboard = new HashMap<>();

		KeyValueIterator<String, HighScores> range = getStore().range(from, to);
		while (range.hasNext()) {
			KeyValue<String, HighScores> next = range.next();
			String game = next.key;
			HighScores highScores = next.value;
			leaderboard.put(game, highScores.toList());
		}
		// close the iterator to avoid memory leaks!
		range.close();
		// return a JSON response
		return leaderboard;
	}
	




	@GetMapping("/{key}")
    @ResponseBody
    public ResponseEntity<?>  getKey(@PathVariable("key") String productId) {
    	 // find out which host has the key
        KeyQueryMetadata metadata =
        		getStream() .queryMetadataForKey("leader-boards", productId, Serdes.String().serializer());

        // the local instance has this key
        if (hostInfo.equals(metadata.activeHost())) {
          log.info("Querying local store for key");
          HighScores highScores = getStore().get(productId);

          if (highScores == null) {
            // game was not found 
            return ResponseEntity.notFound().build();
          }

          // game was found, so return the high scores 
          return ResponseEntity.ok(highScores.toList());
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
			
			return  ResponseEntity.ok(result);
		} catch (RestClientException e) { 
			e.printStackTrace();
			return  ResponseEntity.internalServerError().build();
		}
    }



	private KafkaStreams getStream() {
		return kafkaComponent.getStream();
	}
    private ReadOnlyKeyValueStore<String, HighScores> getStore() {
		return kafkaComponent.getStore();
	}
}
