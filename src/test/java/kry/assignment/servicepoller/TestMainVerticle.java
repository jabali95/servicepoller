package kry.assignment.servicepoller;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  @Order(1)
  @DisplayName("insert a new service with invalid url")
  void insertServiceWithInvalidUrl(Vertx vertx, VertxTestContext testContext) {
    JsonObject params = new JsonObject().put("name", "Kry").put("url", "kry.se");
    WebClient.create(vertx)
      .post(8080, "::1", "/api/services")
      .sendJsonObject(params, handler -> testContext.verify(() -> {
        assertEquals(400, handler.result().statusCode());
        String body = handler.result().bodyAsString();
        assertEquals("Invalid url: kry.se", body);
        testContext.completeNow();
      }));
  }


  @Test
  @Order(2)
  @DisplayName("insert a new service with invalid url")
  void insertServiceWithValidUrl(Vertx vertx, VertxTestContext testContext) {
    JsonObject params = new JsonObject().put("name", "Kry").put("url", "https://kry.se/");
    WebClient.create(vertx)
      .post(8080, "::1", "/api/services")
      .sendJsonObject(params, handler -> testContext.verify(() -> {
        assertEquals(200, handler.result().statusCode());
        String body = handler.result().bodyAsString();
        assertEquals("Inserted", body);
        testContext.completeNow();
      }));
  }

  @Test
  @Order(3)
  @DisplayName("update a service")
  void updateService(Vertx vertx, VertxTestContext testContext) {
    JsonObject params = new JsonObject().put("url", "https://kry.se/").put("name", "Kryy").put("id", 1);
    WebClient.create(vertx)
      .put(8080, "::1", "/api/services/:id")
      .sendJsonObject(params, handler -> testContext.verify(() -> {
        String body = handler.result().bodyAsString();
        assertEquals("Updated", body);
        testContext.completeNow();
      }));
  }

  @Test
  @Order(4)
  @DisplayName("delete a service")
  void deleteService(Vertx vertx, VertxTestContext testContext) {
    JsonObject params = new JsonObject().put("id", 1);
    WebClient.create(vertx)
      .delete(8080, "::1", "/api/services/:id")
      .sendJsonObject(params, handler -> testContext.verify(() -> {
        String body = handler.result().bodyAsString();
        assertEquals("Deleted", body);
        testContext.completeNow();
      }));
  }
}
