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
package org.n52.sta.serdes.json;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.sta.data.service.ServiceUtils;
import org.springframework.util.Assert;

@SuppressWarnings("VisibilityModifier")
public class JSONFeatureOfInterest extends JSONBase.JSONwithIdNameDescription<FeatureEntity>
        implements AbstractJSONEntity {

    // JSON Properties. Matched by Annotation or variable name
    public String encodingType;
    public JsonNode feature;
    @JsonManagedReference
    public JSONObservation[] Observations;

    private final String ENCODINGTYPE_GEOJSON = "application/vnd.geo+json";
    private final GeometryFactory factory =
            new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);


    public JSONFeatureOfInterest() {
        self = new FeatureEntity();
    }

    public FeatureEntity toEntity(boolean validate) {
        if (!generatedId && name == null && validate) {
            Assert.isNull(name, INVALID_REFERENCED_ENTITY);
            Assert.isNull(description, INVALID_REFERENCED_ENTITY);
            Assert.isNull(encodingType, INVALID_REFERENCED_ENTITY);
            Assert.isNull(feature, INVALID_REFERENCED_ENTITY);
            Assert.isNull(Observations, INVALID_REFERENCED_ENTITY);

            self.setIdentifier(identifier);
            return self;
        } else {
            if (validate) {
                Assert.notNull(name, INVALID_INLINE_ENTITY + "name");
                Assert.notNull(description, INVALID_INLINE_ENTITY + "description");
                Assert.notNull(feature, INVALID_INLINE_ENTITY + "feature");
                Assert.state(encodingType.equals(ENCODINGTYPE_GEOJSON),
                        "Invalid encodingType supplied. Only GeoJSON (application/vnd.geo+json) is supported!");
            }

            self.setIdentifier(identifier);
            self.setName(name);
            self.setDescription(description);

            if (feature != null) {
                GeoJsonReader reader = new GeoJsonReader(factory);
                try {
                    self.setGeometry(reader.read(feature.toString()));
                } catch (ParseException e) {
                    Assert.notNull(null, "Could not parse feature to GeoJSON. Error was:" + e.getMessage());
                }
                self.setFeatureType(ServiceUtils.createFeatureType(self.getGeometry()));
            }

            //TODO: handle nested observations
            // if (backReference != null) {
            //      TODO: link feature to observations?
            //      throw new NotImplementedException();
            // }

            return self;
        }
    }
}
