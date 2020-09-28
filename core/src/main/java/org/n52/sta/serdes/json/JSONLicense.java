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
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.n52.series.db.beans.AbstractDatasetEntity;
import org.n52.series.db.beans.sta.LicenseEntity;
import org.n52.shetland.ogc.sta.StaConstants;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:j.speckamp@52north.org">Jan Speckamp</a>
 */
@SuppressWarnings("VisibilityModifier")
@SuppressFBWarnings({"NM_FIELD_NAMING_CONVENTION", "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD"})
public class JSONLicense extends JSONBase.JSONwithId<LicenseEntity>
    implements AbstractJSONEntity {

    public String name;
    public String definition;
    public String logo;

    @JsonManagedReference
    @JsonProperty(StaConstants.DATASTREAMS)
    public JSONDatastream[] datastreams;

    public JSONLicense() {
        self = new LicenseEntity();
    }

    @Override public LicenseEntity toEntity(JSONBase.EntityType type) {
        switch (type) {
            case FULL:
                parseReferencedFrom();
                Assert.notNull(name, INVALID_INLINE_ENTITY_MISSING + "name");
                Assert.notNull(definition, INVALID_INLINE_ENTITY_MISSING + "definition");

                return createEntity();
            case PATCH:
                parseReferencedFrom();
                return createEntity();
            case REFERENCE:
                Assert.isNull(name, INVALID_REFERENCED_ENTITY);
                Assert.isNull(definition, INVALID_REFERENCED_ENTITY);
                Assert.isNull(logo, INVALID_REFERENCED_ENTITY);
                self.setStaIdentifier(identifier);
                return self;
            default:
                return null;
        }
    }

    private LicenseEntity createEntity() {
        self.setStaIdentifier(identifier);
        self.setName(name);
        self.setDefinition(definition);

        if (logo != null) {
            self.setLogo(logo);
        }

        Set<AbstractDatasetEntity> related = new HashSet<>();
        if (datastreams != null) {
            for (JSONDatastream datastream : datastreams) {
                related.add(datastream.toEntity(JSONBase.EntityType.FULL, JSONBase.EntityType.REFERENCE));
            }
            self.setDatasets(related);
        } else if (backReference instanceof JSONDatastream) {
            related.add(((JSONDatastream) backReference).getEntity());
            self.setDatasets(related);
        }

        return self;
    }
}
