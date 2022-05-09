package com.magicalpipelines;

import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;

public interface KStreamProcessor {

		@Input("score-events")
		KStream<?, ?> scoreEvents();

		@Input("players")
		KTable<?, ?> players();

		@Input("products")
		GlobalKTable<?, ?> products();

		@Output("high-scores")
		KStream<?, ?> highScores();
	}