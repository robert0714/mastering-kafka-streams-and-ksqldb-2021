package com.magicalpipelines.serialization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tweet {
	
  @JsonProperty("CreatedAt")
  private Long createdAt;

  @JsonProperty("Id")
  private Long id;

  @JsonProperty("Lang")
  private String lang;

  @JsonProperty("Retweet")
  private Boolean retweet;

  @JsonProperty("Text")
  private String text;

  public Long getCreatedAt() {
    return this.createdAt;
  }

  public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
  }

  public Long getId() {
    return this.id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getLang() {
    return this.lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }

  public Boolean isRetweet() {
    return this.retweet;
  }

  public Boolean getRetweet() {
    return this.retweet;
  }

  public void setRetweet(Boolean retweet) {
    this.retweet = retweet;
  }

  public String getText() {
    return this.text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
