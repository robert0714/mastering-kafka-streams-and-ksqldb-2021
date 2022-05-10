package com.magicalpipelines;

import java.time.Duration;
import java.util.function.BiFunction;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue; 
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.ValueJoiner;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.Suppressed.BufferConfig;
import org.apache.kafka.streams.processor.TimestampExtractor; 
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.StreamsBuilderFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.KafkaStreamsCustomizer;

import com.magicalpipelines.model.BodyTemp;
import com.magicalpipelines.model.CombinedVitals;
import com.magicalpipelines.model.Pulse;
import com.magicalpipelines.serialization.json.JsonSerdes;

import lombok.extern.slf4j.Slf4j; 

@SpringBootApplication
@Slf4j
public class PatientMonitoringTopologySpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(PatientMonitoringTopologySpringApplication.class, args);
	}
	 
	//For spring-cloud-stream to auto-register Serializer/Deserializer;
	/***
	 * https://docs.spring.io/spring-cloud-stream-binder-kafka/docs/3.1.6/reference/html/spring-cloud-stream-binder-kafka.html#_record_serialization_and_deserialization
	 * **/
	@Bean
    public Serde<Pulse> pulseJsonSerde() {
        return JsonSerdes.Pulse();
    }
	@Bean
    public Serde<BodyTemp> bodyTempJsonSerde() {
        return JsonSerdes.BodyTemp();
    }
	@Bean
    public Serde<CombinedVitals> combinedVitalsJsonSerde() {
        return JsonSerdes.CombinedVitals();
    }
	/**
	 * https://docs.spring.io/spring-cloud-stream-binder-kafka/docs/3.1.6/reference/html/spring-cloud-stream-binder-kafka.html#_timestamp_extractor
	 * **/
	@Bean
	public TimestampExtractor timestampExtractor() {
	    return new VitalTimestampExtractor();
	}
	@Bean
	public StreamsBuilderFactoryBeanCustomizer streamsBuilderFactoryBeanCustomizer() {
	    return factoryBean -> {
	        factoryBean.setKafkaStreamsCustomizer(new KafkaStreamsCustomizer() {
	            @Override
	            public void customize(KafkaStreams kafkaStreams) { 
	                kafkaStreams.setGlobalStateRestoreListener(new MyRestoreListener());
	            }
	        });
	    };
	}
	@Bean
	public BiFunction<KStream<String, Pulse>,KStream<String, BodyTemp>, KStream<String, CombinedVitals>> process() {
	    // turn pulse into a rate (bpm)
	    final TimeWindows tumblingWindow =
	        TimeWindows.of(Duration.ofSeconds(60)).grace(Duration.ofSeconds(5));    
	    
		return (pulseEvents, tempEvents) -> {
		    /*!
		     * Examples of other windows (not needed for the tutorial) are commented
		     * out below
		     *
		     * TimeWindows hoppingWindow =
		     *     TimeWindows.of(Duration.ofSeconds(5)).advanceBy(Duration.ofSeconds(4));
		     *
		     * SessionWindows sessionWindow = SessionWindows.with(Duration.ofSeconds(5));
		     *
		     * JoinWindows joinWindow = JoinWindows.of(Duration.ofSeconds(5));
		     *
		     * SlidingWindows slidingWindow =
		     *     SlidingWindows.withTimeDifferenceAndGrace(Duration.ofSeconds(5), Duration.ofSeconds(0));
		     */
			
			
			final KTable<Windowed<String>, Long> pulseCounts =
				pulseEvents
				// 2
				.groupByKey()
				// 3.1 - windowed aggregation
				.windowedBy(tumblingWindow)
				// 3.2 - windowed aggregation
				.count(Materialized.as("pulse-counts"))
				// 4
				.suppress(Suppressed.untilWindowCloses(BufferConfig.unbounded().shutDownWhenFull()));
			
			// 5.1
		    // filter for any pulse that exceeds our threshold
			final KStream<String, Long> highPulse =
		        pulseCounts
		            .toStream()
		            // this peek operator is not included in the book, but was added
		            // to this code example so you could view some additional information
		            // when running the application locally :)
		            .peek(
		                (key, value) -> {
		                  String id = new String(key.key());
		                  Long start = key.window().start();
		                  Long end = key.window().end();
		                  log.info(
		                      "Patient {} had a heart rate of {} between {} and {}", id, value, start, end);
		                })
		            // 5.1
		            .filter((key, value) -> value >= 100)
		            // 6
		            .map(
		                (windowedKey, value) -> {
		                  return KeyValue.pair(windowedKey.key(), value);
		                });
		    // 5.2
		    // filter for any temperature reading that exceeds our threshold
			final KStream<String, BodyTemp> highTemp =
		        tempEvents.filter(
		            (key, value) ->
		                value != null && value.getTemperature() != null && value.getTemperature() > 100.4);
			
			// looking for step 6? it's chained right after 5.1

		    // 7
		    StreamJoined<String, Long, BodyTemp> joinParams =
		        StreamJoined.with(Serdes.String(), Serdes.Long(), JsonSerdes.BodyTemp());

		    JoinWindows joinWindows =
		        JoinWindows
		            // timestamps must be 1 minute apart
		            .of(Duration.ofSeconds(60))
		            // tolerate late arriving data for up to 10 seconds
		            .grace(Duration.ofSeconds(10));

		    ValueJoiner<Long, BodyTemp, CombinedVitals> valueJoiner =
		        (pulseRate, bodyTemp) -> new CombinedVitals(pulseRate.intValue(), bodyTemp);

		    KStream<String, CombinedVitals> vitalsJoined =
		        highPulse.join(highTemp, valueJoiner, joinWindows, joinParams);
		    
		    // debug only
		    pulseCounts
		        .toStream()
		        .print(Printed.<Windowed<String>, Long>toSysOut().withLabel("pulse-counts"));
		    highPulse.print(Printed.<String, Long>toSysOut().withLabel("high-pulse"));
		    highTemp.print(Printed.<String, BodyTemp>toSysOut().withLabel("high-temp"));
		    vitalsJoined.print(Printed.<String, CombinedVitals>toSysOut().withLabel("vitals-joined"));
			return vitalsJoined;
		};

	}
  }
