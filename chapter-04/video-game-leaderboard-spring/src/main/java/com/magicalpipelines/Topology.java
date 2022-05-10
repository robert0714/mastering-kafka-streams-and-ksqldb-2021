package com.magicalpipelines;

 
import org.apache.kafka.common.serialization.Serdes; 
import org.apache.kafka.streams.kstream.Aggregator; 
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.Joined; 
import org.apache.kafka.streams.kstream.KeyValueMapper; 
import org.apache.kafka.streams.kstream.ValueJoiner; 
  
import com.magicalpipelines.model.Player;
import com.magicalpipelines.model.Product;
import com.magicalpipelines.model.ScoreEvent;
import com.magicalpipelines.model.join.Enriched;
import com.magicalpipelines.model.join.ScoreWithPlayer;
import com.magicalpipelines.serialization.json.JsonSerdes;
 

public interface Topology {
    /**
     * map score-with-player records to products
     *
     * <p>Regarding the KeyValueMapper param types: - String is the key type for the score events
     * stream - ScoreWithPlayer is the value type for the score events stream - String is the lookup
     * key type
     */
	final KeyValueMapper<String, ScoreWithPlayer, String> keyMapper =
        (leftKey, scoreWithPlayer) -> {
          return String.valueOf(scoreWithPlayer.getScoreEvent().getProductId());
        };
        
	/***
	 * join the withPlayers stream to the product global ktable <br/> <br/>
	 * This join needs to combine a ScoreWithPlayer (from the output of the first join) with a Product (from the products GlobalKTable) <br/> <br/>
	 * A ValueJoiner, expressed as a lambda, that   for the KStream-GlobalKTable <br/> <br/>
	 ***/
    final ValueJoiner<ScoreWithPlayer, Product, Enriched> productJoiner =
            (scoreWithPlayer, product) -> new Enriched(scoreWithPlayer, product);
            
            
	/***
	 * KStream to KTable Join (players Join) <br/> <br/>
	 * join params for scoreEvents -> players join
	 * ***/   
    final Joined<String, ScoreEvent, Player> playerJoinParams = 
    		Joined.with(
    			Serdes.String(),
    			JsonSerdes.ScoreEvent(),
    			JsonSerdes.Player());
    /***
	 * join scoreEvents -> players <br/> <br/>
	 * ValueJoiner combines a ScoreEvent (from the score-events KStream) and a Player (from the players KTable) into a ScoreWithPlayer instance <br/> <br/>
	 *  (score,player) -> new ScoreWithPlayer(score, player)  , a static method ->  ScoreWithPlayer::new 
	 * ***/ 
    final ValueJoiner<ScoreEvent, Player, ScoreWithPlayer> scorePlayerJoiner
//              =  		(score,player) -> new ScoreWithPlayer(score, player);
                =   ScoreWithPlayer::new ;
    
    
  
    /** 
     * The initial value of our aggregation will be a new HighScores instances <br/><br/> 
     * It tell Kafka Streams how to initialize our new data class. Initializing a class is simple; we just need to instantiate it
     *  */
    final  Initializer<HighScores> highScoresInitializer = HighScores::new;

    /** 
     * The logic for aggregating high scores is implemented in the HighScores.add method <br/>
     * This is accomplished using the Aggregator interface, which, like Initializer, is a functional interface that can be implemented using a lambda. The implementing function needs to accept three parameters:<br/>
     * (1)The record key<br/>
     * (2)The record value <br/>
     * (3)The current aggregate value
     *  */
    final Aggregator<String, Enriched, HighScores> highScoresAdder =
        (key, value, aggregate) -> aggregate.add(value);
}
