package kry.assignment.servicepoller;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Designing the API
 */

public class ServicesPoller {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServicesPoller.class);

  private static final String SQL_INSERT = "INSERT INTO services (name, url, created, modified) values (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
  private static final String SQL_UPDATE = "UPDATE services SET  name = ?, url = ?, modified = CURRENT_TIMESTAMP  WHERE id = ?";
  private static final String SQL_SELECT_ALL = "SELECT * FROM services";
  private static final String SQL_SELECT_ONE = "SELECT * FROM services WHERE id = ?";
  private static final String SQL_DELETE = "DELETE FROM services WHERE id = ?";

  private final WebClient webClient;
  private final JDBCClient client;
  private final Router router;

  public ServicesPoller(WebClient webClient, JDBCClient client, Router router) {
    this.webClient = webClient;
    this.client = client;
    this.router = router;
  }

  /**
   * Create GET method with no parameters.
   * Will get all services from the data base and return the result as A JsonArray to the web client
   */
  protected void setGetServicesRouter() {
    router.get("/api/services").handler(ctx -> {
      // First get services from the data-base, then check the status for each service
      queryGetServices().compose(res -> appendStatusToServices(res)).onComplete(handler -> {
        List<JsonObject> result = handler.result();
        if (result == null)
          result = List.of();
        ctx.response()
          .putHeader("content-type", "application/json")
          .setStatusCode(200)
          .end(new JsonArray(result).encode());
      });
    });
  }

  protected Future<ResultSet> queryGetServices() {
    Promise<ResultSet> promise = Promise.promise();
    client.query(SQL_SELECT_ALL, handler -> {
      if (handler.failed()) {
        LOGGER.error("Could not get data", handler.cause());
        promise.fail(handler.cause());
      } else {
        promise.complete(handler.result());
      }
    });
    return promise.future();
  }

  /**
   * Append the services status
   *
   * @param res JsonArray result that contains all the services in the data-base
   * @return return a List of JsonObjects that have the status field after requesting the services.
   */
  private Future<List<JsonObject>> appendStatusToServices(ResultSet res) {
    Promise<List<JsonObject>> promise = Promise.promise();
    ArrayList<JsonObject> jsonObjects = new ArrayList<>();
    if (res.getRows().isEmpty())
      promise.complete();
    res.getRows().forEach(x -> {
      webClient.getAbs(x.getString("url"))
        .timeout(5000)
        .send(handler -> { // Set a timeout to protect the poller from misbehaving services
        if (handler.result().statusCode() == 200) { // only code 200 considered as :OK to status
          x.put("status", "OK");
        } else {
          x.put("status", "FAIL");
        }
        jsonObjects.add(x);
        if (res.getRows().size() == jsonObjects.size())
          promise.complete(jsonObjects);
      });
    });
    return promise.future();
  }

  /**
   * Create GET method with parameter ID.
   * Will get the requested service with the specified id if exist in the data-base.
   */
  protected void setGetServiceRouter() {
    router.get("/api/services/:id").handler(ctx -> {
      JsonArray param = new JsonArray().add(ctx.request().getParam("id"));
      queryGetService(param).onComplete(handler -> {
        List<JsonObject> rows = handler.result().getRows();
        JsonObject tmp = new JsonObject();
        if (!rows.isEmpty())
          tmp = rows.get(0);
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(tmp.encode());
      });
    });
  }

  private Future<ResultSet> queryGetService(JsonArray param) {
    Promise<ResultSet> promise = Promise.promise();
    client.queryWithParams(SQL_SELECT_ONE, param, handler -> {
      if (handler.failed()) {
        LOGGER.error("Could not get data", handler.cause());
        promise.fail(handler.cause());
      } else {
        promise.complete(handler.result());
      }
    });
    return promise.future();
  }

  /**
   * Create DELETE method with parameter ID.
   * Will delete a service specified by the ID if exist in the data-base.
   */
  protected void setDeleteServiceRouter() {
    router.delete("/api/services/:id").handler(ctx -> {
      JsonArray param = new JsonArray().add(ctx.getBodyAsJson().getInteger("id"));
      queryDeleteService(param).onComplete(handler -> {
        if (handler.succeeded())
          ctx.response()
            .putHeader("content-type", "text/plain")
            .end("Deleted");
        else
          ctx.response()
            .putHeader("content-type", "text/plain")
            .end("Not Found");
      });
    });
  }

  private Future<Void> queryDeleteService(JsonArray param) {
    Promise<Void> promise = Promise.promise();
    client.queryWithParams(SQL_DELETE, param, handler -> {
      if (handler.failed()) {
        LOGGER.error("Could not delete Data", handler.cause());
        promise.fail(handler.cause());
      } else {
        promise.complete();
      }
    });
    return promise.future();
  }

  /**
   * Create PUT method with parameter ID.
   * Will update a service specified by the ID if exist in the data-base.
   */
  protected void setUpdateServiceRouter() {
    router.put("/api/services/:id").handler(ctx -> {
      queryUpdateService(ctx);
    });
  }

  private void queryUpdateService(RoutingContext ctx) {
    JsonObject jsonBody = ctx.getBodyAsJson();
    JsonObject service;
    try {
      service = getServiceObject(jsonBody.getString("url"), jsonBody.getString("name"), jsonBody.getInteger("id"));
    } catch (MalformedURLException e) {
      ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "text/plain")
        .end("Invalid url: " + jsonBody.getString("url"));
      return;
    }
    update(service).onComplete(handler -> {
      if (handler.succeeded()) {
        ctx.response()
          .putHeader("content-type", "text/plain")
          .end("Updated");
      } else {
        LOGGER.error("Update failed", handler.cause());
        ctx.response()
          .setStatusCode(500)
          .putHeader("content-type", "text/plain")
          .end("Internal error");
      }
    });
  }

  private Future<Void> update(JsonObject service) {
    Promise<Void> promise = Promise.promise();
    client.queryWithParams(SQL_UPDATE, new JsonArray()
      .add(service.getString("name"))
      .add(service.getString("url"))
      .add(service.getInteger("id")), handler -> {
      if (handler.succeeded()) {
        promise.complete();
      } else {
        LOGGER.error("Could not update data", handler.cause());
        promise.fail(handler.cause());
      }
    });
    return promise.future();
  }


  /**
   * Create POST method.
   * Will insert a new service to the data-base.
   */
  protected void setPostServiceRouter() {
    router.post("/api/services").handler(ctx -> {
      queryInsertServices(ctx);
    });
  }

  private void queryInsertServices(RoutingContext ctx) {
    JsonObject jsonBody = ctx.getBodyAsJson();
    JsonObject service;
    try {
      service = getServiceObject(jsonBody.getString("url"), jsonBody.getString("name"), jsonBody.getInteger("id"));
    } catch (MalformedURLException e) {
      ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "text/plain")
        .end("Invalid url: " + jsonBody.getString("url"));
      return;
    }
    insert(service).onComplete(handler -> {
      if (handler.succeeded()) {
        ctx.response()
          .putHeader("content-type", "text/plain")
          .end("Inserted");
      } else {
        LOGGER.error("Insertion failed", handler.cause());
        ctx.response()
          .setStatusCode(500)
          .putHeader("content-type", "text/plain")
          .end("Internal error");
      }
    });
  }

  private Future<Void> insert(JsonObject service) {
    Promise<Void> promise = Promise.promise();
    client.queryWithParams(SQL_INSERT, new JsonArray()
      .add(service.getString("name"))
      .add(service.getString("url")), handler -> {
      if (handler.succeeded()) {
        promise.complete();
      } else {
        LOGGER.error("Could not insert data", handler.cause());
        promise.fail(handler.cause());
      }
    });
    return promise.future();
  }

  private JsonObject getServiceObject(String url, String name, Integer id) throws MalformedURLException {
    return new JsonObject()
      .put("id", id)
      .put("name", name)
      .put("url", new URL(url).toString());
  }
}
