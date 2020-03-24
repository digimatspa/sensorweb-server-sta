/*
 * Copyright (C) 2018-2020 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */

package org.n52.sta;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Test filtering using spatial operators. For now only checks if no error is thrown.
 *
 * @author <a href="mailto:j.speckamp@52north.org">Jan Speckamp</a>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ITFilterBySpatial extends ConformanceTests implements TestUtil {

    ITFilterBySpatial(@Value("${server.rootUrl}") String rootUrl) throws IOException {
        super(rootUrl);

        postEntity(EntityType.LOCATION, "{\n"
                + "  \"name\": \"52N HQ\",\n"
                + "  \"description\": \"test location at 52N52E\",\n"
                + "  \"encodingType\": \"application/vnd.geo+json\",\n"
                + "  \"location\": { \"type\": \"Point\", \"coordinates\": [52, 52] }\n"
                + "}");
    }

    @Test
    public void testFilterSTequalsDoesNotThrowError() throws IOException {
        getCollection(EntityType.OBSERVATION,
                      URLEncoder.encode("$filter=st_equals(location, geography'POINT (30 10)')"));
    }

    @Test
    public void testFilterSTequals() throws IOException {
        JsonNode response = getCollection(EntityType.LOCATION,
                                          URLEncoder.encode("$filter=st_equals(location, geography'POINT (30 10)')"));
        assertEmptyResponse(response);

        response = getCollection(EntityType.LOCATION,
                                 URLEncoder.encode("$filter=not st_equals(location, geography'POINT (30 10)')"));
        assertSingleResponse(response);

        response = getCollection(EntityType.LOCATION,
                                 URLEncoder.encode("$filter=st_equals(location, geography'POINT (52 52)')"));
        assertEmptyResponse(response);

        response = getCollection(EntityType.LOCATION,
                                 URLEncoder.encode("$filter=not st_equals(location, geography'POINT (52 52)')"));
        assertSingleResponse(response);

    }

    @Test
    public void testFilterSTdisjoint() throws IOException {
        JsonNode response = getCollection(EntityType.LOCATION,
                                          URLEncoder.encode("$filter=st_disjoint(location, geography'POINT (30 10)')"));
        assertEmptyResponse(response);

        response = getCollection(EntityType.LOCATION,
                                 URLEncoder.encode("$filter=not st_disjoint(location, geography'POINT (30 10)')"));
        assertSingleResponse(response);

        response = getCollection(EntityType.LOCATION,
                                 URLEncoder.encode("$filter=st_disjoint(location, geography'POINT (52 52)')"));
        assertSingleResponse(response);

        response = getCollection(EntityType.LOCATION,
                                 URLEncoder.encode("$filter=not st_disjoint(location, geography'POINT (52 52)')"));
        assertEmptyResponse(response);

    }

    private void assertEmptyResponse(JsonNode response) {
        Assertions.assertTrue(
                0 == response.get("@iot.count").asDouble(),
                "Entity count is not zero although it should be"
        );
        Assertions.assertTrue(
                response.get("value").isEmpty(),
                "Entity is returned although it shouldn't"
        );
    }

    private void assertSingleResponse(JsonNode response) {
        Assertions.assertTrue(
                1 == response.get("@iot.count").asDouble(),
                "Entity count is not one although it should be"
        );
        Assertions.assertFalse(
                response.get("value").isEmpty(),
                "Entity is not returned although it should be"
        );

    }

}