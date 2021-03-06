package de.fraunhofer.fokus.ids.services;

import de.fraunhofer.fokus.ids.models.Constants;
import de.fraunhofer.fokus.ids.persistence.entities.DataAsset;
import de.fraunhofer.fokus.ids.persistence.managers.DataAssetManager;
import de.fraunhofer.fokus.ids.persistence.service.DatabaseService;
import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.util.PlainLiteral;
import io.vertx.core.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
/**
 * @author Vincent Bohlen, vincent.bohlen@fokus.fraunhofer.de
 */
public class IDSService {
	private final Logger LOGGER = LoggerFactory.getLogger(IDSService.class.getName());

	private String INFO_MODEL_VERSION = "2.0.0";
	private String[] SUPPORTED_INFO_MODEL_VERSIONS = {"2.0.0"};
	private DataAssetManager dataAssetManager;
	private DatabaseService databaseService;

	public IDSService(Vertx vertx){
		dataAssetManager = new DataAssetManager(vertx);
		databaseService = DatabaseService.createProxy(vertx, Constants.DATABASE_SERVICE);
	}

	public void getSelfDescriptionResponse(Handler<AsyncResult<SelfDescriptionResponse>> resultHandler) {
		getConfiguration(config -> {
			if(config.succeeded()){
				resultHandler.handle(Future.succeededFuture( buildSelfDescriptionResponse(config.result())));
			} else {
				LOGGER.error(config.cause());
				resultHandler.handle(Future.failedFuture(config.cause()));
			}
		});
	}

	private XMLGregorianCalendar getDate(){
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(new Date());
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
		} catch (DatatypeConfigurationException e) {
			LOGGER.error(e);
		}
		return null;
	}

	public void createRegistrationMessage(Handler<AsyncResult<ConnectorAvailableMessage>> resultHandler){

		getConfiguration(config -> {
			if(config.succeeded()){
				try {
					ConnectorAvailableMessage message = new ConnectorAvailableMessageBuilder()
							._issued_(getDate())
							._modelVersion_("2.0.0")
							._issuerConnector_(new URI(config.result().getString("url")))
							._securityToken_( new DynamicAttributeTokenBuilder()
									._tokenFormat_(TokenFormat.JWT)
									._tokenValue_(getJWT())
									.build())
							.build();
					resultHandler.handle(Future.succeededFuture(message));
				} catch (URISyntaxException e) {
					LOGGER.error(e);
					resultHandler.handle(Future.failedFuture(e));
				}
			} else {
				LOGGER.error(config.cause());
				resultHandler.handle(Future.failedFuture(config.cause()));
			}
		});
	}

	public void createUpdateMessage(Handler<AsyncResult<ConnectorUpdateMessage>> resultHandler){

		getConfiguration(config -> {
			if(config.succeeded()){
				try {
					ConnectorUpdateMessage message = new ConnectorUpdateMessageBuilder()
							._issued_(getDate())
							._modelVersion_("2.0.0")
							._issuerConnector_(new URI(config.result().getString("url")))
							._securityToken_( new DynamicAttributeTokenBuilder()
									._tokenFormat_(TokenFormat.JWT)
									._tokenValue_(getJWT())
									.build())
							.build();
					resultHandler.handle(Future.succeededFuture(message));
				} catch (URISyntaxException e) {
					LOGGER.error(e);
					resultHandler.handle(Future.failedFuture(e));
				}
			} else {
				LOGGER.error(config.cause());
				resultHandler.handle(Future.failedFuture(config.cause()));
			}
		});
	}

	public void createUnregistrationMessage(Handler<AsyncResult<ConnectorUnavailableMessage>> resultHandler){

		getConfiguration(config -> {
			if(config.succeeded()){
				try {
					ConnectorUnavailableMessage message = new ConnectorUnavailableMessageBuilder()
							._issued_(getDate())
							._modelVersion_("2.0.0")
							._issuerConnector_(new URI(config.result().getString("url")))
							._securityToken_( new DynamicAttributeTokenBuilder()
									._tokenFormat_(TokenFormat.JWT)
									._tokenValue_(getJWT())
									.build())
							.build();
					resultHandler.handle(Future.succeededFuture(message));
				} catch (URISyntaxException e) {
					LOGGER.error(e);
					resultHandler.handle(Future.failedFuture(e));
				}
			} else {
				LOGGER.error(config.cause());
				resultHandler.handle(Future.failedFuture(config.cause()));
			}
		});
	}

	public void getConnector(Handler<AsyncResult<Connector>> resultHandler) {

		getConfiguration(configReply -> {
			if(configReply.succeeded()) {
				buildBaseConnector(configReply.result(), reply -> {
					if (reply.succeeded()) {
						resultHandler.handle(Future.succeededFuture(reply.result()));
					} else {
						LOGGER.error(reply.cause());
						resultHandler.handle(Future.failedFuture(reply.cause()));
					}
				});
			}
			else{
				LOGGER.error("Configuration could not be retrieved.");
				resultHandler.handle(Future.failedFuture(configReply.cause()));
			}
		});
	}

	private SelfDescriptionResponse buildSelfDescriptionResponse(JsonObject config){

		try {
			return new SelfDescriptionResponseBuilder(new URI(config.getString("url") +"#SelfDescriptionResponse"))
					._issued_(getDate())
					._issuerConnector_(new URI(config.getString("url")))
					._modelVersion_(INFO_MODEL_VERSION)
					.build();
		} catch (URISyntaxException e) {
			LOGGER.error(e);
		}
		return null;
	}

	private void buildBaseConnector(JsonObject config, Handler<AsyncResult<Connector>> next){

		Future<Catalog> catalogFuture = buildCatalog(config);
		try {
			BaseConnectorBuilder connectorBuilder = new BaseConnectorBuilder((new URI(config.getString("url") + "#Connector")))
					._maintainer_(new URI(config.getString("maintainer")))
					._version_("0.0.1")
					._curator_(new URI(config.getString("curator")))
					._outboundModelVersion_(INFO_MODEL_VERSION)
					._inboundModelVersion_(new ArrayList<>(Arrays.asList(SUPPORTED_INFO_MODEL_VERSIONS)))

					//TODO Change dummy auth service (can be null)
					//				._authInfo_(new AuthInfoBuilder(new URL(this.connectorURL + "#AuthInfo"))
					//						._authService_(new URI(this.connectorURL + "#AuthService"))
					//						._authStandard_(AuthStandard.OAUTH2_JWT)
					//						.build())

					._securityProfile_(SecurityProfile.BASE_CONNECTOR_SECURITY_PROFILE)
					._title_(new ArrayList<>(Arrays.asList(new PlainLiteral(config.getString("title"), ""))))

					._hosts_(new ArrayList<>(Arrays.asList(new HostBuilder()
							._accessUrl_(new URI(config.getString("url")))
							._protocol_(Protocol.HTTP)
							.build())));

					//TODO fill with information
					//				._descriptions_(new ArrayList<PlainLiteral>(Arrays.asList(new PlainLiteral(""))))
					//				._lifecycleActivities_(null)
					//				._componentCertification_(null)
					//				._physicalLocation_(null);

			catalogFuture.setHandler( ac -> {
				if(ac.succeeded()) {
					connectorBuilder._catalog_(catalogFuture.result());
					next.handle(Future.succeededFuture(connectorBuilder.build()));
				}
				else{
					LOGGER.error(ac.cause());
					next.handle(Future.failedFuture(ac.cause()));
				}
			});

		} catch (Exception e) {
			LOGGER.error(e);
			next.handle(Future.failedFuture(e.getMessage()));
		}
	}

	private Future<Catalog> buildCatalog(JsonObject config) {

		Future<List<Resource>> offers = getOfferResources(config);
		Future<List<Resource>> requests = getRequestResources(config);
		Future<Catalog> catalog = Future.future();

		CompositeFuture.all(offers, requests).setHandler(cf -> {
			if(cf.succeeded()) {
				try {
					catalog.complete(new CatalogBuilder(new URI(config.getString("url") + "#Catalog"))
							._offer_(new ArrayList(offers.result()))
							._request_(new ArrayList(requests.result()))
							.build());
				} catch (Exception e) {
					LOGGER.error(e);
					catalog.fail(e.getMessage());
				}
			}
			else{
				LOGGER.error(cf.cause());
				catalog.fail(cf.cause());
			}
		});
		return catalog;
	}

	private Future<List<Resource>> getRequestResources(JsonObject config) {
		// TODO Auto-generated method stub
		return Future.succeededFuture(new ArrayList<>());
	}

	private Future<List<Resource>> getOfferResources(JsonObject config) {
		Future<List<Resource>> daFuture = Future.future();
		findPublished(daList -> createDataResources(config, daList, daFuture));
		return daFuture;
	}

	private void createDataResources( JsonObject config, AsyncResult<List<DataAsset>> daList, Future<List<Resource>> daFuture) {
		if(daList.succeeded()) {
			List<DataAsset> das = daList.result();
			ArrayList<Resource> offerResources = new ArrayList<>();
			for (DataAsset da : das) {
				try {
					DataResourceBuilder r = new DataResourceBuilder(new URI(config.getString("url") + "#DataResource"))
							//						//TODO: The regular period with which items are added to a collection.
							//						._accrualPeriodicity_(null)
							//						//TODO: Reference to a Digital Content (physically or logically) included, definition of part-whole hierarchies.
							//						._contentParts_(null)
							//						//TODO: Constraint that refines a (composite) Digital Content.
							//						._contentRefinements_(null)
							//						//TODO: Standards document defining the given Digital Content. The content is assumed to conform to that Standard.
							//						._contentStandard_(null)
							//						//TODO: Enumerated types of content expanding upon the Digital Content hierarchy.
							//						._contentType_(null)
							//						//TODO: Reference to a Contract Offer defining the authorized use of the Resource.
							//						._contractOffers_(null)
							//						//TODO: Default representation of the content.
							//						._defaultRepresentation_(null)
							//						//TODO: Natural language(s) used within the content
							//						._languages_(null)
							//						//TODO: Something that occurs over a period of time and acts upon or with entities.
							//						._lifecycleActivities_(null)
							//						//TODO: Representation of the content.
							//						._representations_(null)
							//						//TODO: Reference to the Interface defining Operations supported by the Resource.
							//						._resourceInterface_(null)
							//						//TODO: Reference to a Resource (physically or logically) included, definition of part-whole hierarchies.
							//						._resourceParts_(null)
							//						//TODO: Sample Resource instance.
							//						._samples_(null)
							//						//TODO: Named spatial entity covered by the Resource.
							//						._spatialCoverages_(null)
							//						//TODO: Reference to a well-known License regulating the general usage of the Resource.
							//						._standardLicense_(null)
							//						//TODO: Temporal period or instance covered by the content.
							//						._temporalCoverages_(null)
							//						//TODO: Abstract or concrete concept related to or referred by the content.
							//						._themes_(null)
							//						//TODO: (Equivalent) variant of given Resource, e.g. a translation.
							//						._variant_(null)

							._version_(da.getVersion())
							._resourceEndpoint_(getResourceEndpoint(config, da))
							._publisher_(getAgent(config, "Publisher", "", ""))
							._sovereign_(getAgent(config, "Sovereign", da.getOrganizationDescription(), da.getOrganizationTitle()));
					if (da.getDatasetTitle() != null) {
						r._title_(new ArrayList<>(Arrays.asList(new PlainLiteral(da.getDatasetTitle()))));
					}
					if (da.getDataSetDescription() != null) {
						r._description_(new ArrayList<>(Arrays.asList(new PlainLiteral(da.getDataSetDescription()))));
					}
					ArrayList<PlainLiteral> keywords = getKeyWords(da);
					if (keywords != null) {
						r._keyword_(getKeyWords(da));
					}
					if (da.getLicenseUrl() != null) {
						r._customLicense_(new URI(da.getLicenseUrl()));
					}

					offerResources.add(r.build());
				} catch (Exception e) {
					LOGGER.error( e);
				}
			}
			daFuture.complete(offerResources);
		}
	}

	private void findPublished(Handler<AsyncResult<List<DataAsset>>> next) {

		dataAssetManager.findPublished(reply -> {
			if(reply.succeeded()) {

				JsonArray array = new JsonArray(reply.result().toString());
				List<DataAsset> assets = new ArrayList<>();
				for(int i=0;i<array.size();i++){
					assets.add(Json.decodeValue(array.getJsonObject(i).toString(), DataAsset.class));
				}
				next.handle(Future.succeededFuture(assets));
			}
			else{
				LOGGER.error(reply.cause());
				next.handle(Future.succeededFuture(new ArrayList<>()));
			}
		});
	}

	private ArrayList<? extends Endpoint> getResourceEndpoint(JsonObject config, DataAsset da) {
		ArrayList<Endpoint> endpoints = new ArrayList<>();
		Endpoint e;
		try {
			e = new StaticEndpointBuilder(new URI(config.getString("url")+"#ResourceEndpoint"))
					._endpointArtifact_(new ArtifactBuilder(new URI(config.getString("url")+"#Artifact"))
							._creationDate_(getDate(da.getCreatedAt()))
							._fileName_(da.getId().toString())
							.build())
					._endpointHost_(new HostBuilder(new URI(config.getString("url")+"#Host"))
							._accessUrl_(new URI(config.getString("url")))
							._pathPrefix_("/")
							._protocol_(Protocol.HTTP)
							.build())
					._path_("/data/")
					.build();
			endpoints.add(e);
		} catch (Exception e1) {
			LOGGER.error(e1);
		}
		return endpoints;
	}

	private XMLGregorianCalendar getDate(Date createdAt) {
		try {
			DateFormat format = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSSSS");
			String date = format.format(createdAt);

			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(format.parse(date));

			return  DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return null;
	}

	private Agent getAgent(JsonObject config, String agentRole, String agentDescription , String agentTitle) {
		Agent agent = null;
		PlainLiteral desc = agentDescription != null ? new PlainLiteral(agentDescription) : null;
		PlainLiteral title = agentTitle != null ? new PlainLiteral(agentTitle) : null;
		try {
			agent = new AgentBuilder(new URI(config.getString("url")+"#"+agentRole))
					._description_(new ArrayList<>(Arrays.asList(desc)))
					._title_(new ArrayList<>(Arrays.asList(title)))
					.build();
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return agent;
	}

	private ArrayList<PlainLiteral> getKeyWords(DataAsset da) {
		ArrayList<PlainLiteral> keywords = new ArrayList<>();
		for (String tag : da.getTags()) {
			keywords.add(new PlainLiteral(tag));
		}
		return keywords.isEmpty()? null : keywords;
	}

	private void getConfiguration(Handler<AsyncResult<JsonObject>> resultHandler){

		databaseService.query("SELECT * FROM configuration", new JsonArray(), reply -> {
			if(reply.succeeded()){
				if(reply.result().size() > 0) {
					resultHandler.handle(Future.succeededFuture(reply.result().get(0)));
				}
				else{
					resultHandler.handle(Future.failedFuture("No config available."));
				}
			}
			else{
				resultHandler.handle(Future.failedFuture(reply.cause()));
			}
		});
	}

	private String getJWT(){
		//TODO: implement DAPS and return real token
		return "abcdefg12";
	}
}
