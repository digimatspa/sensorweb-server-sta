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
package org.n52.sta.mqtt.core.subscription;

import org.n52.series.db.beans.HibernateRelations;
import org.n52.shetland.oasis.odata.query.option.QueryOptions;
import org.n52.svalbard.odata.QueryOptionsFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
public abstract class AbstractMqttSubscription {


    protected QueryOptionsFactory queryOptionsFactory = new QueryOptionsFactory();

    protected Matcher matcher;

    protected String sourceEntityType;

    protected String sourceId;

    protected String wantedEntityType;

    private final String topic;

    public AbstractMqttSubscription(String topic, Matcher mt) {
        this.topic = topic;
        this.matcher = mt;
    }

    /**
     * Returns the topic given entity should be posted to. null if the entity
     * does not match this subscription.
     *
     * @param rawObject       Entity to be posted
     * @param entityType      Type of Entity
     * @param relatedEntities Map with EntityType-ID pairs for the related
     *                        entities
     * @param differenceMap   differenceMap names of properties that have changed.
     *                        if null all properties have changed (new entity)
     * @return Topic to be posted to. May be null if Entity does not match this
     * subscription.
     */
    public String checkSubscription(Object rawObject,
                                    String entityType,
                                    Map<String, Set<String>> relatedEntities,
                                    Set<String> differenceMap) {
        return matches(rawObject, entityType, relatedEntities, differenceMap) ? topic : null;
    }

    public String getTopic() {
        return topic;
    }

    public String getEntityType() {
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AbstractMqttSubscription
                && ((AbstractMqttSubscription) other).getTopic().equals(this.topic);
    }

    /**
     * Returns the selectOption extracted from the Topic.
     *
     * @return SelectOption if present, else null
     */
    public QueryOptions getQueryOptions() {
        return null;
    }

    public boolean matches(Object entity,
                           String realEntityType,
                           Map<String, Set<String>> collections,
                           Set<String> differenceMap) {
        return matches((HibernateRelations.HasIdentifier) entity, realEntityType, collections, differenceMap);
    }

    protected abstract boolean matches(HibernateRelations.HasIdentifier entity,
                                       String realEntityType,
                                       Map<String, Set<String>> collections,
                                       Set<String> differenceMap);

    @Override
    public String toString() {
        return new StringBuilder()
                .append("New ")
                .append(this.getClass().getSimpleName())
                .append("[")
                .append("topic=")
                .append(topic)
                .append(",")
                .append("sourceEntityType=")
                .append(sourceEntityType)
                .append(",")
                .append("sourceId=")
                .append(sourceId)
                .append(",")
                .append("wantedEntityType=")
                .append(wantedEntityType)
                .append("]")
                .toString();
    }
}
