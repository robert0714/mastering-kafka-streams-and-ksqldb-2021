package com.magicalpipelines.api;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

import com.magicalpipelines.model.CombinedVitals;
 

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
				context.getBean("&stream-builder-process", StreamsBuilderFactoryBean.class);
		
		KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
		return  kafkaStreams ;
    }

    protected ReadOnlyWindowStore<String, Long> getBpmStore() {
    	int tmp =(int)(Math.random()*10000);
    	
		if ( (tmp % 2) == 0)
			return getBpmStoreV1();
		else
			return getBpmStoreV2();
	}
    
	protected ReadOnlyWindowStore<String, Long> getBpmStoreV1() {
		return interactiveQueryService.getQueryableStore(
				// state store name
				"pulse-counts",
				// state store type
				QueryableStoreTypes.windowStore());
	}

	protected ReadOnlyWindowStore<String, Long> getBpmStoreV2() {
		return getStream().store(StoreQueryParameters.fromNameAndType(
				// state store name
				"pulse-counts",
				// state store type
				QueryableStoreTypes.windowStore()));
	}
	protected ReadOnlyKeyValueStore<String, CombinedVitals> getAlertsStore() {
    	int tmp =(int)(Math.random()*10000);
    	
		if ( (tmp % 2) == 0)
			return getAlertsStoreV1();
		else
			return getAlertsStoreV2();
    }

	private ReadOnlyKeyValueStore<String, CombinedVitals> getAlertsStoreV1() {
		return interactiveQueryService.getQueryableStore(
				// state store name
				"alerts",
				// state store type
				QueryableStoreTypes.keyValueStore());
	}
	
	private ReadOnlyKeyValueStore<String, CombinedVitals> getAlertsStoreV2() {
		return getStream().store(StoreQueryParameters.fromNameAndType(
				// state store name
				"alerts",
				// state store type
				QueryableStoreTypes.keyValueStore()));
	}
}
