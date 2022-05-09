package com.magicalpipelines.model;
 
import com.fasterxml.jackson.annotation.JsonProperty;

public class ScoreEvent {
  @JsonProperty( "player_id")
  private Long playerId;
  @JsonProperty( "product_id")
  private Long productId;
  private Double score;

  public Long getPlayerId() {
    return this.playerId;
  }

  public void setPlayerId(Long playerId) {
    this.playerId = playerId;
  }

  public Long getProductId() {
    return this.productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public Double getScore() {
    return this.score;
  }

  public void setScore(Double score) {
    this.score = score;
  }
  @Override
  public String toString() {
    return "{" + " player_id='" + getPlayerId() + "'" + ", productId='" + getProductId() + "'" + ", score='" + getScore() + "'"+"}";
  }
}
