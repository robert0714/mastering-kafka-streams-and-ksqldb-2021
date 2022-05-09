package com.magicalpipelines;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.ValueJoiner;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerde;
 
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicalpipelines.model.Player;
import com.magicalpipelines.model.Product;
import com.magicalpipelines.model.join.Enriched;
import com.magicalpipelines.model.join.ScoreWithPlayer;
 

public interface Topology {
    /**
     * map score-with-player records to products
     *
     * <p>Regarding the KeyValueMapper param types: - String is the key type for the score events
     * stream - ScoreWithPlayer is the value type for the score events stream - String is the lookup
     * key type
     */
    KeyValueMapper<String, ScoreWithPlayer, String> keyMapper =
        (leftKey, scoreWithPlayer) -> {
          return String.valueOf(scoreWithPlayer.getScoreEvent().getProductId());
        };
        
        
    // join the withPlayers stream to the product global ktable
    ValueJoiner<ScoreWithPlayer, Product, Enriched> productJoiner =
            (scoreWithPlayer, product) -> new Enriched(scoreWithPlayer, product);
}
