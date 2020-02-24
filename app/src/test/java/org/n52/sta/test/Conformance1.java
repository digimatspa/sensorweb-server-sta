/*
 * Copyright 2015-2020 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.n52.sta.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.n52.janmayen.http.HTTPMethods;
import org.n52.shetland.ogc.sta.StaConstants;
import org.n52.sta.serdes.model.DatastreamEntityDefinition;
import org.n52.sta.serdes.model.FeatureOfInterestEntityDefinition;
import org.n52.sta.serdes.model.HistoricalLocationEntityDefinition;
import org.n52.sta.serdes.model.LocationEntityDefinition;
import org.n52.sta.serdes.model.ObservationEntityDefinition;
import org.n52.sta.serdes.model.ObservedPropertyEntityDefinition;
import org.n52.sta.serdes.model.STAEntityDefinition;
import org.n52.sta.serdes.model.SensorEntityDefinition;
import org.n52.sta.serdes.model.ThingEntityDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:j.speckamp@52north.org">Jan Speckamp</a>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class Conformance1 extends ConformanceTests implements TestUtil {

    /**
     * The variable that defines to which recursive level the resource path
     * should be tested
     */
    private final int resourcePathLevel = 4;

    public Conformance1(@Value("${server.rootUrl}") String rootUrl) throws IOException {
        super(rootUrl);

        // Create required test harness
        // Requires POST with deep insert to work.
        postEntity(EntityType.THING, "{ \"description\": \"thing 1\", \"name\": \"thing name 1\", \"properties\": { " +
                "\"reference\": \"first\" }, \"Locations\": [ { \"description\": \"location 1\", \"name\": \"location" +
                " name 1\", \"location\": { \"type\": \"Point\", \"coordinates\": [ -117.05, 51.05 ] }, " +
                "\"encodingType\": \"application/vnd.geo+json\" } ], \"Datastreams\": [ { \"unitOfMeasurement\": { " +
                "\"name\": \"Lumen\", \"symbol\": \"lm\", \"definition\": \"http://www.qudt.org/qudt/owl/1.0" +
                ".0/unit/Instances.html/Lumen\" }, \"description\": \"datastream 1\", \"name\": \"datastream name " +
                "1\", \"observationType\": \"http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement\", " +
                "\"ObservedProperty\": { \"name\": \"Luminous Flux\", \"definition\": \"http://www.qudt" +
                ".org/qudt/owl/1.0.0/quantity/Instances.html/LuminousFlux\", \"description\": \"observedProperty 1\" " +
                "}, \"Sensor\": { \"description\": \"sensor 1\", \"name\": \"sensor name 1\", \"encodingType\": " +
                "\"application/pdf\", \"metadata\": \"Light flux sensor\" }, \"Observations\":[ { \"phenomenonTime\":" +
                " \"2015-03-03T00:00:00Z\", \"result\": 3 }, { \"phenomenonTime\": \"2015-03-04T00:00:00Z\", " +
                "\"result\": 4 } ] }, { \"unitOfMeasurement\": { \"name\": \"Centigrade\", \"symbol\": \"C\", " +
                "\"definition\": \"http://www.qudt.org/qudt/owl/1.0.0/unit/Instances.html/Lumen\" }, \"description\":" +
                " \"datastream 2\", \"name\": \"datastream name 2\", \"observationType\": \"http://www.opengis" +
                ".net/def/observationType/OGC-OM/2.0/OM_Measurement\", \"ObservedProperty\": { \"name\": " +
                "\"Tempretaure\", \"definition\": \"http://www.qudt.org/qudt/owl/1.0.0/quantity/Instances" +
                ".html/Tempreture\", \"description\": \"observedProperty 2\" }, \"Sensor\": { \"description\": " +
                "\"sensor 2\", \"name\": \"sensor name 2\", \"encodingType\": \"application/pdf\", \"metadata\": " +
                "\"Tempreture sensor\" }, \"Observations\":[ { \"phenomenonTime\": \"2015-03-05T00:00:00Z\", " +
                "\"result\": 5 }, { \"phenomenonTime\": \"2015-03-06T00:00:00Z\", \"result\": 6 } ] } ] }");
    }

    /**
     * This method is testing GET entities. It should return 200. Then the
     * response entities are tested for control information, mandatory
     * properties, and mandatory related entities.
     */
    @Test
    public void readEntitiesAndCheckResponse() throws IOException {
        JsonNode collection;
        collection = getCollection(EntityType.THING);
        checkEntitiesAllAspectsForResponse(new ThingEntityDefinition(), collection);
        collection = getCollection(EntityType.LOCATION);
        checkEntitiesAllAspectsForResponse(new LocationEntityDefinition(), collection);
        collection = getCollection(EntityType.HISTORICAL_LOCATION);
        checkEntitiesAllAspectsForResponse(new HistoricalLocationEntityDefinition(), collection);
        collection = getCollection(EntityType.DATASTREAM);
        checkEntitiesAllAspectsForResponse(new DatastreamEntityDefinition(), collection);
        collection = getCollection(EntityType.SENSOR);
        checkEntitiesAllAspectsForResponse(new SensorEntityDefinition(), collection);
        collection = getCollection(EntityType.OBSERVATION);
        checkEntitiesAllAspectsForResponse(new ObservationEntityDefinition(), collection);
        collection = getCollection(EntityType.OBSERVED_PROPERTY);
        checkEntitiesAllAspectsForResponse(new ObservedPropertyEntityDefinition(), collection);
        collection = getCollection(EntityType.FEATURE_OF_INTEREST);
        checkEntitiesAllAspectsForResponse(new FeatureOfInterestEntityDefinition(), collection);
    }

    /**
     * This method is testing GET when requesting a nonexistent entity. The
     * response should be 404.
     */
    @Test
    public void readNonexistentEntity() throws IOException {
        getNonExistentEntity(EntityType.THING);
        getNonExistentEntity(EntityType.LOCATION);
        getNonExistentEntity(EntityType.HISTORICAL_LOCATION);
        getNonExistentEntity(EntityType.DATASTREAM);
        getNonExistentEntity(EntityType.SENSOR);
        getNonExistentEntity(EntityType.OBSERVATION);
        getNonExistentEntity(EntityType.OBSERVED_PROPERTY);
        getNonExistentEntity(EntityType.FEATURE_OF_INTEREST);
    }

    /**
     * This method is testing GET for a specific entity with its id. It checks
     * the control information, mandatory properties and mandatory related
     * entities for the response entity.
     */
    @Test
    public void readEntityAndCheckResponse() throws IOException {
        Set<JsonNode> response;
        response = readEntityWithEntityType(EntityType.THING);
        checkEntityAllAspectsForResponse(new ThingEntityDefinition(), response);
        response = readEntityWithEntityType(EntityType.LOCATION);
        checkEntityAllAspectsForResponse(new LocationEntityDefinition(), response);
        response = readEntityWithEntityType(EntityType.HISTORICAL_LOCATION);
        checkEntityAllAspectsForResponse(new HistoricalLocationEntityDefinition(), response);
        response = readEntityWithEntityType(EntityType.DATASTREAM);
        checkEntityAllAspectsForResponse(new DatastreamEntityDefinition(), response);
        response = readEntityWithEntityType(EntityType.SENSOR);
        checkEntityAllAspectsForResponse(new SensorEntityDefinition(), response);
        response = readEntityWithEntityType(EntityType.OBSERVATION);
        checkEntityAllAspectsForResponse(new ObservationEntityDefinition(), response);
        response = readEntityWithEntityType(EntityType.OBSERVED_PROPERTY);
        checkEntityAllAspectsForResponse(new ObservedPropertyEntityDefinition(), response);
        response = readEntityWithEntityType(EntityType.FEATURE_OF_INTEREST);
        checkEntityAllAspectsForResponse(new FeatureOfInterestEntityDefinition(), response);
    }

    /**
     * This method is testing GET for a property of an entity.
     */
    @Test
    public void readPropertyOfEntityAndCheckResponse() throws IOException {
        readPropertyOfEntityWithEntityType(EntityType.THING, new ThingEntityDefinition());
        readPropertyOfEntityWithEntityType(EntityType.LOCATION, new LocationEntityDefinition());
        readPropertyOfEntityWithEntityType(EntityType.HISTORICAL_LOCATION, new HistoricalLocationEntityDefinition());
        readPropertyOfEntityWithEntityType(EntityType.DATASTREAM, new DatastreamEntityDefinition());
        readPropertyOfEntityWithEntityType(EntityType.OBSERVED_PROPERTY, new ObservedPropertyEntityDefinition());
        readPropertyOfEntityWithEntityType(EntityType.SENSOR, new SensorEntityDefinition());
        readPropertyOfEntityWithEntityType(EntityType.OBSERVATION, new ObservationEntityDefinition());
        readPropertyOfEntityWithEntityType(EntityType.FEATURE_OF_INTEREST, new FeatureOfInterestEntityDefinition());
    }

    /**
     * This helper method is testing property and property/$value for single
     * entity of a given entity type
     *
     * @param entityType Entity type from EntityType enum list
     */
    private void readPropertyOfEntityWithEntityType(EntityType entityType, STAEntityDefinition definition)
            throws IOException {
        JsonNode collection = getCollection(entityType);
        Assertions.assertNotNull(collection.get("value"),
                                 "Could not get collection for EntityType: " + entityType.name());
        for (JsonNode entity : collection.get("value")) {
            Assertions.assertNotNull(entity.get("@iot.id"),
                                     "Could not read @iot.id from entity:" + entity.toPrettyString());
            for (String mandatoryProp : definition.getEntityPropsMandatory()) {
                checkGetPropertyOfEntity(entityType, entity.get("@iot.id").asText(), mandatoryProp);
                checkGetPropertyValueOfEntity(entityType, entity.get("@iot.id").asText(), mandatoryProp);
            }
        }
    }

    /**
     * This helper method sending GET request for requesting a property and
     * check the response is 200.
     *
     * @param entityType Entity type from EntityType enum list
     * @param id         The id of the entity
     * @param property   The property to get requested
     */
    private void checkGetPropertyOfEntity(EntityType entityType, String id, String property) throws IOException {
        JsonNode entity = getEntity(entityType, id);
        Assertions.assertNotNull(entity.get(property),
                                 "Reading property \"" + property + "\"of \"" + entityType + "\" fails.");
        Assertions.assertEquals(1,
                                entity.size(),
                                "The response for getting property " + property + " of Entity " + entityType +
                                        " returns more properties!");
    }

    /**
     * This helper method sending GET request for requesting a property $value
     * and check the response is 200.
     *
     * @param entityType Entity type from EntityType enum list
     * @param id         The id of the entity
     * @param property   The property to get requested
     */
    private void checkGetPropertyValueOfEntity(EntityType entityType, String id, String property) throws IOException {

        JsonNode entity = getEntity(entityType, id);
        Assertions.assertNotNull(entity.get(property),
                                 "Reading property \"" + property + "\"of \"" + entityType + "\" fails.");
        Assertions.assertEquals(1,
                                entity.size(),
                                "The response for getting property " + property + " of a " + entityType +
                                        " returns more properties!");
        if (!property.equals("location") && !property.equals("feature") && !property.equals("unitOfMeasurement")) {
            Assertions.assertEquals(-1,
                                    entity.toString().indexOf("{"),
                                    "Reading property value of \"" + property + "\"of \"" + entityType + "\" fails.");
        } else {
            Assertions.assertEquals(0,
                                    entity.toString().indexOf("{"),
                                    "Reading property value of \"" + property + "\"of \"" + entityType + "\" fails.");
        }
    }

    /**
     * This method is testing the resource paths based on specification to the
     * specified level.
     */
    @Test
    public void checkResourcePaths() {
        readRelatedEntityOfEntityWithEntityType(EntityType.THING);
        readRelatedEntityOfEntityWithEntityType(EntityType.LOCATION);
        readRelatedEntityOfEntityWithEntityType(EntityType.HISTORICAL_LOCATION);
        readRelatedEntityOfEntityWithEntityType(EntityType.DATASTREAM);
        readRelatedEntityOfEntityWithEntityType(EntityType.OBSERVED_PROPERTY);
        readRelatedEntityOfEntityWithEntityType(EntityType.SENSOR);
        readRelatedEntityOfEntityWithEntityType(EntityType.OBSERVATION);
        readRelatedEntityOfEntityWithEntityType(EntityType.FEATURE_OF_INTEREST);
    }

    /**
     * This helper method is the start point for testing resource path. It adds
     * the entity type to be tested to resource path chain and call the other
     * method to test the chain.
     *
     * @param entityType Entity type from EntityType enum list
     */
    private void readRelatedEntityOfEntityWithEntityType(EntityType entityType) {
        List<String> entityTypes = new ArrayList<>();
        switch (entityType) {
        case THING:
            entityTypes.add("Things");
            break;
        case LOCATION:
            entityTypes.add("Locations");
            break;
        case HISTORICAL_LOCATION:
            entityTypes.add("HistoricalLocations");
            break;
        case DATASTREAM:
            entityTypes.add("Datastreams");
            break;
        case SENSOR:
            entityTypes.add("Sensors");
            break;
        case OBSERVATION:
            entityTypes.add("Observations");
            break;
        case OBSERVED_PROPERTY:
            entityTypes.add("ObservedProperties");
            break;
        case FEATURE_OF_INTEREST:
            entityTypes.add("FeaturesOfInterest");
            break;
        default:
            Assertions.fail("Entity type is not recognized in SensorThings API : " + entityType);
        }
        readRelatedEntity(entityTypes, new ArrayList<>());
    }

    /**
     * This helper method is testing the chain to the specified level. It
     * confirms that the response is 200.
     *
     * @param entityTypes List of entity type from EntityType enum list for the
     *                    chain
     * @param ids         List of ids for teh chain
     */
    private void readRelatedEntity(List<String> entityTypes, List<String> ids) {
//        if (entityTypes.size() > resourcePathLevel) {
//            return;
//        }
//        try {
//
//            getEntity()
//            String urlString = ServiceURLBuilder.buildURLString(rootUri, entityTypes, ids, null);
//            Map<String, Object> responseMap = HTTPMethods.doGet(urlString);
//            Assert.assertEquals(responseMap.get("response-code"),
//                                200,
//                                "Reading relation of the entity failed: " + entityTypes.toString());
//            String response = responseMap.get("response").toString();
//            if (!entityTypes.get(entityTypes.size() - 1).toLowerCase().equals("featuresofinterest") &&
//                    !entityTypes.get(entityTypes.size() - 1).endsWith("s")) {
//                return;
//            }
//            Long id = new JSONObject(response.toString()).getJSONArray("value")
//                                                         .getJSONObject(0)
//                                                         .getLong(ControlInformation.ID);
//
//            //check $ref
//            urlString = ServiceURLBuilder.buildURLString(rootUri, entityTypes, ids, "$ref");
//            responseMap = HTTPMethods.doGet(urlString);
//            Assert.assertEquals(responseMap.get("response-code"),
//                                200,
//                                "Reading relation of the entity failed: " + entityTypes.toString());
//            response = responseMap.get("response").toString();
//            checkAssociationLinks(response, entityTypes, ids);
//
//            if (entityTypes.size() == resourcePathLevel) {
//                return;
//            }
//            ids.add(id);
//            for (String relation : EntityRelations.getRelationsListFor(entityTypes.get(entityTypes.size() - 1))) {
//                entityTypes.add(relation);
//                readRelatedEntity(entityTypes, ids);
//                entityTypes.remove(entityTypes.size() - 1);
//            }
//            ids.remove(ids.size() - 1);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            Assert.fail("An Exception occurred during testing!:\n" + e.getMessage());
//        }

    }

    /**
     * This method is checking the response for the request of Association Link.
     * It confirms that it contains a list of selfLinks.
     *
     * @param response    The response for GET association link request
     * @param entityTypes List of entity type from EntityType enum list for the
     *                    chain
     * @param ids         List of ids for teh chain
     */
    private void checkAssociationLinks(String response, List<String> entityTypes, List<Long> ids) {

        //        try {
        //            Assert.assertTrue(response.indexOf("value") != -1,
        //                              "The GET entities Association Link response does not match SensorThings API :
        //                              missing " +
        //                                      "\"value\" in response.: " +
        //                                      entityTypes.toString() + ids.toString());
        //            JSONArray value = new JSONObject(response.toString()).getJSONArray("value");
        //            int count = 0;
        //            for (int i = 0; i < value.length() && count < 2; i++) {
        //                count++;
        //                JSONObject obj = value.getJSONObject(i);
        //                try {
        //                    Assert.assertNotNull(obj.get(ControlInformation.SELF_LINK),
        //                                         "The Association Link does not contain self-links.: " +
        //                                                 entityTypes.toString() + ids.toString());
        //                } catch (JSONException e) {
        //                    Assert.fail("The Association Link does not contain self-links.: " + entityTypes
        //                    .toString() +
        //                                        ids.toString());
        //                }
        //                Assert.assertEquals(obj.length(), 1,
        //                                    "The Association Link contains properties other than self-link.: " +
        //                                            entityTypes.toString() + ids.toString());
        //            }
        //        } catch (JSONException e) {
        //            e.printStackTrace();
        //            Assert.fail("An Exception occurred during testing!:\n" + e.getMessage());
        //        }
    }

    /**
     * This method is reading a specific entity and return it as a string.
     *
     * @param entityType Entity type from EntityType enum list
     * @return The entity response as a string
     */
    private Set<JsonNode> readEntityWithEntityType(EntityType entityType) throws IOException {
        HashSet<JsonNode> responses = new HashSet<>();
        JsonNode collection = getCollection(entityType);
        Assertions.assertNotNull(collection.get("value"),
                                 "Could not get collection for EntityType: " + entityType.name());
        for (JsonNode entity : collection.get("value")) {
            Assertions.assertNotNull(entity.get("@iot.id"),
                                     "Could not read @iot.id from entity:" + entity.toPrettyString());
            responses.add(getEntity(entityType, entity.get("@iot.id").asText()));
        }
        return responses;
    }

    /**
     * This method is testing the root URL of the service under test.
     */
    @Test()
    public void checkServiceRootUri() throws IOException {
        JsonNode response = getRootResponse();
        ArrayNode entities = (ArrayNode) response.get("value");

        Map<String, Boolean> addedLinks = new HashMap<>();
        addedLinks.put("Things", false);
        addedLinks.put("Locations", false);
        addedLinks.put("HistoricalLocations", false);
        addedLinks.put("Datastreams", false);
        addedLinks.put("Sensors", false);
        addedLinks.put("Observations", false);
        addedLinks.put("ObservedProperties", false);
        addedLinks.put("FeaturesOfInterest", false);
        for (int i = 0; i < entities.size(); i++) {
            JsonNode entity = entities.get(i);
            Assertions.assertNotNull(entity.get("name"));
            Assertions.assertNotNull(entity.get("url"));
            String name = entity.get("name").asText();
            String nameUrl = entity.get("url").asText();
            switch (name) {
            case "Things":
                Assertions.assertEquals(rootUrl + "Things",
                                        nameUrl,
                                        "The URL for Things in Service Root URI is not compliant to SensorThings " +
                                                "API.");
                addedLinks.remove("Things");
                addedLinks.put(name, true);
                break;
            case "Locations":
                Assertions.assertEquals(rootUrl + "Locations",
                                        nameUrl,
                                        "The URL for Locations in Service Root URI is not compliant to " +
                                                "SensorThings " +
                                                "API.");
                addedLinks.remove("Locations");
                addedLinks.put(name, true);
                break;
            case "HistoricalLocations":
                Assertions.assertEquals(rootUrl + "HistoricalLocations",
                                        nameUrl,
                                        "The URL for HistoricalLocations in Service Root URI is not compliant to " +
                                                "SensorThings API.");
                addedLinks.remove("HistoricalLocations");
                addedLinks.put(name, true);
                break;
            case "Datastreams":
                Assertions.assertEquals(rootUrl + "Datastreams",
                                        nameUrl,
                                        "The URL for Datastreams in Service Root URI is not compliant to " +
                                                "SensorThings" +
                                                " API.");
                addedLinks.remove("Datastreams");
                addedLinks.put(name, true);
                break;
            case "Sensors":
                Assertions.assertEquals(rootUrl + "Sensors",
                                        nameUrl,
                                        "The URL for Sensors in Service Root URI is not compliant to SensorThings" +
                                                " API" +
                                                ".");
                addedLinks.remove("Sensors");
                addedLinks.put(name, true);
                break;
            case "Observations":
                Assertions.assertEquals(rootUrl + "Observations",
                                        nameUrl,
                                        "The URL for Observations in Service Root URI is not compliant to " +
                                                "SensorThings API.");
                addedLinks.remove("Observations");
                addedLinks.put(name, true);
                break;
            case "ObservedProperties":
                Assertions.assertEquals(rootUrl + "ObservedProperties",
                                        nameUrl,
                                        "The URL for ObservedProperties in Service Root URI is not compliant to " +
                                                "SensorThings API.");
                addedLinks.remove("ObservedProperties");
                addedLinks.put(name, true);
                break;
            case "FeaturesOfInterest":
                Assertions.assertEquals(rootUrl + "FeaturesOfInterest",
                                        nameUrl,
                                        "The URL for FeaturesOfInterest in Service Root URI is not compliant to " +
                                                "SensorThings API.");
                addedLinks.remove("FeaturesOfInterest");
                addedLinks.put(name, true);
                break;
            case "MultiDatastreams":
                Assertions.assertEquals(rootUrl + "/MultiDatastreams",
                                        nameUrl,
                                        "The URL for MultiDatastreams in Service Root URI is not compliant to " +
                                                "SensorThings API.");
                break;
            default:
                Assertions.fail(
                        "There is a component in Service Root URI response that is not in SensorThings API : " +
                                name);
                break;
            }
        }
        for (String key : addedLinks.keySet()) {
            Assertions.assertTrue(addedLinks.get(key), "The Service Root URI response does not contain " + key);
        }
    }

    /**
     * This helper method is the start point for checking the response for a
     * collection in all aspects.
     *
     * @param entityType Entity type from EntityType enum list
     * @param entity     The response of the GET request to be checked
     */
    private void checkEntitiesAllAspectsForResponse(STAEntityDefinition entityType, JsonNode entity) {
        checkEntitiesControlInformation(entity);
        checkEntitiesProperties(entityType.getEntityPropsMandatory(), entity);
        checkEntitiesProperties(annotateNavigationProperties(entityType.getNavPropsMandatory()), entity);
    }

    private Set<String> annotateNavigationProperties(Set<String> properties) {
        HashSet<String> annotated = new HashSet<>();
        for (String property : properties) {
            annotated.add(property + "@iot.navigationLink");
        }
        return annotated;
    }

    /**
     * This helper method is the start point for checking the response for a
     * specific entity in all aspects.
     *
     * @param entityType Entity type from EntityType enum list
     * @param entity     The response of the GET request to be checked
     */
    private void checkEntityAllAspectsForResponse(STAEntityDefinition entityType, JsonNode entity) {
        checkEntityControlInformation(entity);
        checkEntityProperties(entityType.getEntityPropsMandatory(), entity);
        checkEntityProperties(annotateNavigationProperties(entityType.getNavPropsMandatory()), entity);
    }

    /**
     * This helper method is the start point for checking the response for a
     * specific entity in all aspects.
     *
     * @param entityType Entity type from EntityType enum list
     * @param entitySet  The response of the GET request to be checked
     */
    private void checkEntityAllAspectsForResponse(STAEntityDefinition entityType, Set<JsonNode> entitySet) {
        for (JsonNode entity : entitySet) {
            checkEntityControlInformation(entity);
            checkEntityProperties(entityType.getEntityPropsMandatory(), entity);
            checkEntityProperties(annotateNavigationProperties(entityType.getNavPropsMandatory()), entity);
        }
    }

    /**
     * This helper method is checking the control information of the response
     * for a collection
     *
     * @param response The response of the GET request to be checked
     */
    private void checkEntitiesControlInformation(JsonNode response) {
        JsonNode entities = response.get("value");
        for (int i = 0; i < entities.size(); i++) {
            JsonNode entity = entities.get(i);
            checkEntityControlInformation(entity);
        }
    }

    /**
     * This helper method is checking the control information of the response
     * for a specific entity
     *
     * @param entity The entity to be checked
     */
    private void checkEntityControlInformation(JsonNode entity) {
        Assertions.assertTrue(entity.has(StaConstants.AT_IOT_ID),
                              "The entity does not have mandatory control information :" + StaConstants.AT_IOT_ID);
        Assertions.assertNotNull(entity.get(StaConstants.AT_IOT_ID),
                                 "The entity does not have mandatory control information :" + StaConstants.AT_IOT_ID);

        Assertions.assertTrue(entity.has(StaConstants.AT_IOT_SELFLINK),
                              "The entity does not have mandatory control information :" +
                                      StaConstants.AT_IOT_SELFLINK);
    }

    /**
     * This helper method is checking the mandatory properties of the response
     * for a collection
     *
     * @param mandatoryProperties List of mandatory properties
     * @param response            The response of the GET request to be checked
     */
    private void checkEntitiesProperties(Set<String> mandatoryProperties, JsonNode response) {
        JsonNode entities = response.get("value");
        for (int i = 0; i < entities.size(); i++) {
            JsonNode entity = entities.get(i);
            checkEntityProperties(mandatoryProperties, entity);
        }

    }

    /**
     * This helper method is checking the mandatory properties of the response
     * for a specific entity
     *
     * @param mandatoryProperties List of mandatory properties
     * @param response            The response of the GET request to be checked
     */
    private void checkEntityProperties(Set<String> mandatoryProperties, JsonNode response) {
        for (String property : mandatoryProperties) {
            Assertions.assertTrue(response.has(property),
                                  "Entity: "
                                          + response.toPrettyString()
                                          + "does not have mandatory property:"
                                          + property);
            Assertions.assertNotNull(response.get(property),
                                     "Entity: "
                                             + response.toPrettyString()
                                             + "does not have mandatory property:"
                                             + property);

        }

    }

}