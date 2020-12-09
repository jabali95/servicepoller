package kry.assignment.servicepoller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle {

  private static final String SQL_CREATE = "CREATE TABLE IF NOT EXISTS services (" +
    " id INTEGER PRIMARY KEY AUTOINCREMENT , " +
    "name VARCHAR(255) NOT NULL , " +
    "url VARCHAR(255) NOT NULL , " +
    "modified TIMESTAMP NOT NULL , " +
    "created TIMESTAMP NOT NULL)";

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> promise) throws Exception {

    JsonObject config = new JsonObject().put("url", "jdbc:sqlite:dataBase.db").
      put("driver_class", "org.sqlite.JDBC")
      .put("max_pool_size", 30);
    JDBCClient client = JDBCClient.createShared(vertx, config);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    // Serve static resources
    router.route("/*").handler(StaticHandler.create());

    WebClient webClient = WebClient.create(vertx);
    ServicesPoller poller = new ServicesPoller(webClient, client, router);

    prepareDatabase(client).onComplete(ar -> {
      if (ar.succeeded()) {
        vertx.setPeriodic(60000, timer -> poller.queryGetServices());
        poller.setGetServicesRouter();
        poller.setPostServiceRouter();
        poller.setDeleteServiceRouter();
        poller.setGetServiceRouter();
        poller.setUpdateServiceRouter();
        vertx
          .createHttpServer()
          .requestHandler(router)
          .listen(8080, r -> {
            if (r.succeeded()) {
              promise.complete();
            } else {
              promise.fail(r.cause());
            }
          });
      } else {
        promise.fail(ar.cause());
      }
    });
  }

  private Future<Void> prepareDatabase(JDBCClient client) {
    Promise<Void> promise = Promise.promise();
    client.query(SQL_CREATE, handler -> {
      if (handler.failed()) {
        LOGGER.error("Database preparation error", handler.cause());
        promise.fail(handler.cause());
      } else {
        promise.complete();
      }
    });
    return promise.future();
  }
}
