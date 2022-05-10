package com.magicalpipelines.api;

import static java.util.logging.Level.FINE;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap; 
import java.util.List;
import java.util.Map;

import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

 
import lombok.extern.slf4j.Slf4j; 
import reactor.core.publisher.Mono;

@Slf4j
public class PatientMonitoringHandler { 
    private final KafkaComponent kafkaComponent;
     
	public PatientMonitoringHandler(  final KafkaComponent kafkaComponent) {		
		this.kafkaComponent = kafkaComponent;
	}	
	 
	public Mono<ServerResponse> getAll(final ServerRequest request) {
		Map<String, Long> bpm = new HashMap<>();
				
		try (KeyValueIterator<Windowed<String>, Long> range = getBpmStore().all()) {
			range.forEachRemaining((next) -> {
				Windowed<String> key = next.key;
				Long value = next.value;
				bpm.put(key.toString(), value);
			});
		}		 
		return ServerResponse.ok().bodyValue(bpm).log(log.getName(), FINE) ;
	}
	
	public Mono<ServerResponse> getAllInRange(final ServerRequest request) {
		final String from = request.pathVariable("from");
		final String to = request.pathVariable("to");
		Instant fromTime = Instant.ofEpochMilli(Long.valueOf(from));
		Instant toTime = Instant.ofEpochMilli(Long.valueOf(to));
		List<Map<String, Object>> bpms = new ArrayList<>();		
		try (KeyValueIterator<Windowed<String>, Long> range = getBpmStore().fetchAll(fromTime, toTime)) {
			range.forEachRemaining((next) -> {
				Map<String, Object> bpm = new HashMap<>(); 
				String key = next.key.key();
				Window window = next.key.window();
				Long start = window.start();
				Long end = window.end();
				Long count = next.value;
				bpm.put("key", key);
				bpm.put("start", Instant.ofEpochMilli(start).toString());
				bpm.put("end", Instant.ofEpochMilli(end).toString());
				bpm.put("count", count);
				bpms.add(bpm);
			});
		}		 
		return ServerResponse.ok().bodyValue(bpms).log(log.getName(), FINE) ;
	}
	
	public Mono<ServerResponse> getRange(final ServerRequest request) {
		final String key = request.pathVariable("key");
		final String from = request.pathVariable("from");
		final String to = request.pathVariable("to");
		
		Instant fromTime = Instant.ofEpochMilli(Long.valueOf(from));
		Instant toTime = Instant.ofEpochMilli(Long.valueOf(to));
		List<Map<String, Object>> bpms = new ArrayList<>();		
		
		try (WindowStoreIterator<Long> range = getBpmStore().fetch(key, fromTime, toTime)) {
			range.forEachRemaining((next) -> {
			      Map<String, Object> bpm = new HashMap<>();
			      Long timestamp = next.key;
			      Long count = next.value;
			      bpm.put("timestamp", Instant.ofEpochMilli(timestamp).toString());
			      bpm.put("count", count);
			      bpms.add(bpm);
			});
		}		 
		return ServerResponse.ok().bodyValue(bpms).log(log.getName(), FINE) ;
	}
	
	
	private ReadOnlyWindowStore<String, Long> getBpmStore() {
		return this.kafkaComponent.getBpmStore();
	}
}
