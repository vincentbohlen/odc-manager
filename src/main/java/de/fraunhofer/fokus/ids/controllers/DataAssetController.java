package de.fraunhofer.fokus.ids.controllers;

import de.fraunhofer.fokus.ids.models.DataAssetDescription;
import de.fraunhofer.fokus.ids.persistence.enums.DataAssetStatus;
import de.fraunhofer.fokus.ids.persistence.managers.DataAssetManager;
import de.fraunhofer.fokus.ids.persistence.managers.JobManager;
import de.fraunhofer.fokus.ids.services.JobService;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
/**
 * @author Vincent Bohlen, vincent.bohlen@fokus.fraunhofer.de
 */
public class DataAssetController {

	private Logger LOGGER = LoggerFactory.getLogger(DataAssetController.class.getName());
	private DataAssetManager dataAssetManager;
	private JobManager jobManager;
	private JobService jobService;

	public DataAssetController(Vertx vertx) {
		dataAssetManager = new DataAssetManager(vertx);
		jobManager = new JobManager(vertx);
		jobService = new JobService(vertx);
	}

	public void counts(Handler<AsyncResult<JsonObject>> resultHandler) {

		Future<Long> count = Future.future();
		dataAssetManager.count( reply -> {
			if (reply.succeeded()) {
				count.complete(reply.result());
			}
			else {
				LOGGER.error("Count could not be queried.\n\n"+reply.cause());
				count.fail(reply.cause());
			}
		});

		Future<Long> countPublished = Future.future();
		dataAssetManager.countPublished(reply -> {
			if (reply.succeeded()) {
				countPublished.complete(reply.result());
			}
			else {
				LOGGER.error("Published count could not be queried.\n\n"+reply.cause());
				countPublished.fail(reply.cause());
			}
		});

		CompositeFuture.all(count, countPublished).setHandler(ar -> {
			if(ar.succeeded()) {
				JsonObject jO = new JsonObject();
				jO.put("dacount", count.result());
				jO.put("publishedcount", countPublished.result());
				resultHandler.handle(Future.succeededFuture(jO));
			}
			else {
				LOGGER.error("Composite Future failed.\n\n"+ar.cause());
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}

	public void add(DataAssetDescription dataAssetDescription, Handler<AsyncResult<JsonObject>> resultHandler) {

		if (dataAssetDescription.getData().isEmpty()) {
			JsonObject jO = new JsonObject();
			jO.put("status", "error");
			jO.put("text", "Bitte geben Sie eine Resource-ID ein!");
			resultHandler.handle(Future.succeededFuture(jO));
		} else {
			jobManager.add(new JsonObject(Json.encode(dataAssetDescription)), job -> {
				if (job.succeeded()) {
					jobService.process(reply -> {});
					JsonObject jO = new JsonObject();
					jO.put("status", "success");
					jO.put("text", "Job wurde erstellt!");
					resultHandler.handle(Future.succeededFuture(jO));
				} else {
					LOGGER.error("Der Job konnte nicht erstellt werden!\n\n"+job.cause());
					JsonObject jO = new JsonObject();
					jO.put("status", "error");
					jO.put("text", "Der Job konnte nicht erstellt werden!");
					resultHandler.handle(Future.succeededFuture(jO));
				}
			});
		}
	}

	public void publish(Long id, Handler<AsyncResult<JsonObject>> resultHandler) {
		dataAssetManager.changeStatus(DataAssetStatus.PUBLISHED, id, reply -> {
			JsonObject jO = new JsonObject();
			if (reply.succeeded()) {
				jO.put("success", "Data Asset " + id + " wurde veröffentlicht.");
				resultHandler.handle(Future.succeededFuture(jO));
			}
			else {
				LOGGER.error(reply.cause());
				resultHandler.handle(Future.failedFuture(reply.cause()));
			}
		});
	}

	public void unPublish(Long id, Handler<AsyncResult<JsonObject>> resultHandler) {
		dataAssetManager.changeStatus(DataAssetStatus.APPROVED, id, reply -> {
			JsonObject jO = new JsonObject();
			if (reply.succeeded()) {
				jO.put("success", "Data Asset " + id + " wurde zurückgehalten.");
				resultHandler.handle(Future.succeededFuture(jO));

			}
			else {
				LOGGER.error(reply.cause());
				resultHandler.handle(Future.failedFuture(reply.cause()));


			}
		});
	}

	public void delete(Long id, Handler<AsyncResult<JsonObject>> resultHandler) {
		//TODO DELETE ADD ADAPTER
		dataAssetManager.delete(id, reply -> {
			if (reply.succeeded()) {
				JsonObject jO = new JsonObject();
				jO.put("status", "success");
				jO.put("text", "Data Asset " + id + " wurde gelöscht.");
				resultHandler.handle(Future.succeededFuture(jO));
			} else {
				LOGGER.error("Delete Future could not be completed.\n\n" + reply.cause());
				resultHandler.handle(Future.failedFuture(reply.cause()));
			}
		});
	}

	public void index(Handler<AsyncResult<JsonArray>> resultHandler) {
		dataAssetManager.findAll(reply -> {
			if (reply.succeeded()) {
				resultHandler.handle(Future.succeededFuture(reply.result()));

			}
			else {
				LOGGER.error("FindAll Future could not be completed.\n\n" + reply.cause());
				resultHandler.handle(Future.failedFuture(reply.cause()));
			}
		});
	}

	public void resource(Message<Object> receivedMessage) {
		//TODO Get REsource from Adapter
	}

}
