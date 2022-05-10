package com.magicalpipelines.api;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.apache.kafka.streams.state.HostInfo; 
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse; 
 
@Configuration
@ConditionalOnProperty(prefix = "api", name = "mode", havingValue = "REACTIVE")
public class LeaderboardRoutes {
	private static final String MAIN_REQUEST_MAPPING = "/leaderboard";
	
	
	/** Local key-value store query: all entries */
	private static final String ALL= "";
	
	/** Local key-value store query: approximate number of entries */
	private static final String COUNT= "/count";
	
	/** Local key-value store query: approximate number of entries */
	private static final String COUNT_LOCAL= "/count/local";
	
	/** Local key-value store query: range scan (inclusive) */
	private static final String RANGE_SCAN= "/{from}/{to}";
	
	/** Local key-value store query: point-lookup / single-key lookup */
	private static final String KEY= "/{key}";
	
	
	@Bean
	public RouterFunction<ServerResponse> routes(final LeaderboardHandler handler) {
		return route().path(MAIN_REQUEST_MAPPING,
				builder -> builder.GET(ALL, handler::getAll)
						.GET(COUNT, handler::getCount)
						.GET(COUNT_LOCAL, handler::getCountLocal)
						.GET(RANGE_SCAN, handler::getRange)
						.GET(KEY, handler::getKey))
				.build();		 
	}
	@Bean
	public LeaderboardHandler handler(final HostInfo hostInfo,  final KafkaComponent kafkaComponent) {
		return new LeaderboardHandler(hostInfo, kafkaComponent);
	}
}
