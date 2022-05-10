package com.magicalpipelines.api;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

import com.magicalpipelines.HighScores;

@Component
public class KafkaComponent {
	@Autowired
    private InteractiveQueryService interactiveQueryService;
    @Autowired
    private ApplicationContext context;
    protected KafkaStreams getStream() {    	
    	String[] clazzNames = context.getBeanNamesForType( StreamsBuilderFactoryBean.class);
		for(String name:clazzNames) {
			System.out.println(name);
		}
		final StreamsBuilderFactoryBean streamsBuilderFactoryBean = 
				context.getBean("&stream-builder-jointProducts", StreamsBuilderFactoryBean.class);
		
		KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
		return  kafkaStreams ;
    }

    protected ReadOnlyKeyValueStore<String, HighScores> getStore() {
    	int tmp =(int)(Math.random()*10000);
    	
		if ( (tmp % 2) == 0)
			return getStoreV1();
		else
			return getStoreV2();
	}

	protected ReadOnlyKeyValueStore<String, HighScores> getStoreV1() {
		return interactiveQueryService.getQueryableStore(
				// state store name
				"leader-boards",
				// state store type
				QueryableStoreTypes.keyValueStore());
	}

	protected ReadOnlyKeyValueStore<String, HighScores> getStoreV2() {

		return getStream().store(StoreQueryParameters.fromNameAndType(
				// state store name
				"leader-boards",
				// state store type
				QueryableStoreTypes.keyValueStore()));
	}
}
