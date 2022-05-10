package com.magicalpipelines.api;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
 
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse; 
 
@Configuration
@ConditionalOnProperty(prefix = "api", name = "mode", havingValue = "REACTIVE")
public class RoutesConfig {
	private static final String MAIN_REQUEST_MAPPING = "/bpm";
	
	
	/** Local key-value store query: all entries */
	private static final String ALL= "/all";
	
 	
	/** Local key-value store query: range scan (inclusive) */
	private static final String RANGE_SCAN= "/range/{from}/{to}";
	
	/** Local key-value store query: range scan (inclusive) */
	private static final String RANGE_KEY_SCAN= "/range/{key}/{from}/{to}";
	
	 
	
	@Bean
	public RouterFunction<ServerResponse> routes(final PatientMonitoringHandler handler) {
		return route().path(MAIN_REQUEST_MAPPING,
				builder -> builder.GET(ALL, handler::getAll)						 
						.GET(RANGE_SCAN, handler::getAllInRange)
						.GET(RANGE_KEY_SCAN, handler::getRange))
				.build();		 
	}
	@Bean
	public PatientMonitoringHandler handler(final KafkaComponent kafkaComponent) {
		return new PatientMonitoringHandler( kafkaComponent);
	}
}
