package com.magicalpipelines.model;

import java.io.Serializable;

public class Player  {
  private Long id;
  private String name;

  public Long getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  @Override
  public String toString() {
    return "{" + " id='" + getId() + "'" + ", name='" + getName() + "'" + "}";
  }
}
