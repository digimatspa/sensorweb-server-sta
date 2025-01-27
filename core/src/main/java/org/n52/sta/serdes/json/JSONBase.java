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

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.joda.time.DateTime;
import org.n52.series.db.beans.parameter.ParameterEntity;
import org.n52.series.db.beans.parameter.ParameterFactory;
import org.n52.shetland.ogc.gml.time.Time;
import org.n52.shetland.ogc.gml.time.TimeInstant;
import org.n52.shetland.ogc.gml.time.TimePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

@SuppressWarnings("VisibilityModifier")
public class JSONBase {

    public enum EntityType {
        FULL,
        PATCH,
        REFERENCE
    }


    abstract static class JSONwithId<T> {

        private static Logger LOGGER = LoggerFactory.getLogger(JSONwithId.class);

        @JsonProperty("@iot.id")
        public String identifier = UUID.randomUUID().toString();

        /**
         * Backreference to parent Entity used during nested deserialization
         * Used for linking to parent Entities
         */
        @JsonBackReference
        public Object backReference;

        /**
         * Holds type of the referenced Collection in the URL.
         * E.g. when POSTing on Datastreams(52)/Observations this will contain "Datastreams"
         */
        public String referencedFromType;

        /**
         * Holds id of the referenced Collection in the URL.
         * E.g. when POSTing on Datastreams(52)/Observations this will contain "52"
         */
        public String referencedFromID;

        /**
         * Deals with linking to parent Objects during deep insert
         * Used for dealing with nested inserts
         */
        protected T self;

        /**
         * Whether an Id was autogenerated or not.
         */
        protected boolean generatedId = true;

        public void setIdentifier(String rawIdentifier) {
            generatedId = false;
            identifier = rawIdentifier;
        }

        /**
         * Parses referencedFromType and referencedFromID into the specific JSON Entities.
         * Must be called before any validation is performed on the presence of related Entities!
         */
        protected void parseReferencedFrom() {
        }

        /**
         * Returns a reference to the result of this classes toEntity() method
         *
         * @return reference to created database entity
         */
        public T getEntity() {
            Assert.notNull(self, "Trying to get Entity prior to creation!");
            return this.self;
        }

        /**
         * Creates and validates the Database Entity to conform to invariants defined in standard.
         * What is validated is dictated by given type parameter
         *
         * @param type type of the entity
         * @return created Entity
         */
        public abstract T toEntity(EntityType type);

        /**
         * Used when multiple Entity Types are allowed.
         *
         * @param type1 first type to check
         * @param type2 second type to check
         * @return created entity
         */
        public T toEntity(EntityType type1, EntityType type2) {
            Exception ex = null;
            Exception secondEx = null;
            try {
                return toEntity(type1);
            } catch (IllegalStateException | IllegalArgumentException e) {
                // We have errored out on type 1
                ex = e;
            }

            try {
                return toEntity(type2);
            } catch (IllegalStateException | IllegalArgumentException e) {
                // We have errored out on type 2
                secondEx = e;
            }
            // We have errored out on both types so return error message
            throw new IllegalStateException(ex.getMessage() + secondEx.getMessage());
        }

        @SuppressWarnings("unchecked")
        protected HashSet<ParameterEntity<?>> convertParameters(JsonNode parameters,
                                                                ParameterFactory.EntityType entityType) {
            // parameters
            if (parameters != null) {
                HashSet<ParameterEntity<?>> parameterEntities = new HashSet<>();
                // Check that structure is correct
                Iterator<String> it = parameters.fieldNames();
                while (it.hasNext()) {
                    String key = it.next();
                    JsonNode value = parameters.get(key);

                    ParameterEntity parameterEntity;
                    switch (value.getNodeType()) {
                        case ARRAY:
                            // fallthru
                        case MISSING:
                            // fallthru
                        case NULL:
                            // fallthru
                        case OBJECT:
                            // fallthru
                        case POJO:
                            parameterEntity = ParameterFactory.from(entityType, ParameterFactory.ValueType.JSON);
                            parameterEntity.setValue(value.asText());
                            break;
                        case BINARY:
                            // fallthru
                        case BOOLEAN:
                            parameterEntity = ParameterFactory.from(entityType, ParameterFactory.ValueType.BOOLEAN);
                            parameterEntity.setValue(value.asBoolean());
                            break;
                        case NUMBER:
                            parameterEntity = ParameterFactory.from(entityType, ParameterFactory.ValueType.QUANTITY);
                            parameterEntity.setValue(BigDecimal.valueOf(value.asDouble()));
                            break;
                        case STRING:
                            parameterEntity = ParameterFactory.from(entityType, ParameterFactory.ValueType.TEXT);
                            parameterEntity.setValue(value.asText());
                            break;
                        default:
                            throw new RuntimeException("Could not identify value type of parameters!");
                    }
                    parameterEntity.setName(key);
                    parameterEntities.add(parameterEntity);
                }
                return parameterEntities;
            } else {
                return null;
            }
        }
    }


    @SuppressFBWarnings("UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    abstract static class JSONwithIdNameDescription<T> extends JSONwithId<T> {

        public String name;
        public String description;
    }


    @SuppressFBWarnings("UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    abstract static class JSONwithIdNameDescriptionTime<T> extends JSONwithIdTime<T> {

        public String name;
        public String description;
    }


    abstract static class JSONwithIdTime<T> extends JSONwithId<T> {

        protected Time createTime(DateTime time) {
            return new TimeInstant(time);
        }

        /**
         * Create {@link Time} from {@link DateTime}s
         *
         * @param start Start {@link DateTime}
         * @param end   End {@link DateTime}
         * @return Resulting {@link Time}
         */
        protected Time createTime(DateTime start, DateTime end) {
            if (start.equals(end)) {
                return createTime(start);
            } else {
                return new TimePeriod(start, end);
            }
        }

        protected Time parseTime(String input) {
            if (input.contains("/")) {
                String[] split = input.split("/");
                return createTime(DateTime.parse(split[0]),
                                  DateTime.parse(split[1]));
            } else {
                return new TimeInstant(DateTime.parse(input));
            }
        }

    }

}
