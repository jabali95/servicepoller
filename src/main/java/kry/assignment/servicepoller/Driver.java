package kry.assignment.servicepoller;

import io.vertx.core.Vertx;

public class Driver {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
