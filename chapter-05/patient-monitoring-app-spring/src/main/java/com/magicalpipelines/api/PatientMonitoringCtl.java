package com.magicalpipelines.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Window;
import org.apache.kafka.streams.kstream.Windowed; 
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

 

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/bpm")
@Slf4j
//@ConditionalOnProperty(prefix = "api", name = "mode", havingValue = "SERVLET")
public class PatientMonitoringCtl {
	@Autowired
    private KafkaComponent kafkaComponent;
     
      
    
	@GetMapping("/all")
	@ResponseBody
	public Map<String, Long> getAll() {
		Map<String, Long> bpm = new HashMap<>();
		KeyValueIterator<Windowed<String>, Long> range = getBpmStore().all();
		while (range.hasNext()) {
			KeyValue<Windowed<String>, Long> next = range.next();
			Windowed<String> key = next.key;
			Long value = next.value;
			bpm.put(key.toString(), value);
		}
		// close the iterator to avoid memory leaks
		range.close();
		// return a JSON response
		return bpm;
	}
	@GetMapping("/range/{from}/{to}")
    @ResponseBody
	public List<Map<String, Object>> getAllInRange(@PathVariable("from") String from, @PathVariable("to") String to) {
		List<Map<String, Object>> bpms = new ArrayList<>();

		Instant fromTime = Instant.ofEpochMilli(Long.valueOf(from));
		Instant toTime = Instant.ofEpochMilli(Long.valueOf(to));

		KeyValueIterator<Windowed<String>, Long> range = getBpmStore().fetchAll(fromTime, toTime);
		while (range.hasNext()) {
			Map<String, Object> bpm = new HashMap<>();
			KeyValue<Windowed<String>, Long> next = range.next();
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
		}
		// close the iterator to avoid memory leaks
		range.close();
		// return a JSON response
		return bpms;
	}
	@GetMapping("/range/{key}/{from}/{to}")
    @ResponseBody
	public List<Map<String, Object>> getRange(
			@PathVariable("key") String key, 
			@PathVariable("from") String from,
			@PathVariable("to") String to) {
		
		List<Map<String, Object>> bpms = new ArrayList<>();

		Instant fromTime = Instant.ofEpochMilli(Long.valueOf(from));
		Instant toTime = Instant.ofEpochMilli(Long.valueOf(to));

		WindowStoreIterator<Long> range = getBpmStore().fetch(key, fromTime, toTime);
	    while (range.hasNext()) {
	      Map<String, Object> bpm = new HashMap<>();
	      KeyValue<Long, Long> next = range.next();
	      Long timestamp = next.key;
	      Long count = next.value;
	      bpm.put("timestamp", Instant.ofEpochMilli(timestamp).toString());
	      bpm.put("count", count);
	      bpms.add(bpm);
	    }
	    // close the iterator to avoid memory leaks
	    range.close(); 
		// return a JSON response
		return bpms;
	}
	private ReadOnlyWindowStore<String, Long> getBpmStore() {
		return this.kafkaComponent.getBpmStore();
	}
}
