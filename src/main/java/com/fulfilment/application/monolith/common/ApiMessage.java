package com.fulfilment.application.monolith.common;

/**
 * Simple JSON response used by delete/archive APIs.
 */
public class ApiMessage {

  public String message;

  public ApiMessage() {}

  public ApiMessage(String message) {
    this.message = message;
  }
}
