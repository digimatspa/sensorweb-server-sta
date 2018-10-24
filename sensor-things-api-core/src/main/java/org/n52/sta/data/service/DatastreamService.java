/*
 * Copyright (C) 2012-2018 52°North Initiative for Geospatial Open Source
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
package org.n52.sta.data.service;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.ODataApplicationException;
import org.n52.series.db.FormatRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FormatEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.UnitEntity;
import org.n52.series.db.beans.dataset.Dataset;
import org.n52.series.db.beans.sta.DatastreamEntity;
import org.n52.series.db.beans.sta.StaDataEntity;
import org.n52.series.db.beans.sta.ThingEntity;
import org.n52.sta.data.query.DatastreamQuerySpecifications;
import org.n52.sta.data.repositories.DatastreamRepository;
import org.n52.sta.data.repositories.UnitRepository;
import org.n52.sta.data.service.EntityServiceRepository.EntityTypes;
import org.n52.sta.mapping.DatastreamMapper;
import org.n52.sta.service.query.QueryOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.querydsl.core.types.dsl.BooleanExpression;

/**
 * @author <a href="mailto:j.speckamp@52north.org">Jan Speckamp</a>
 *
 */
@Component
public class DatastreamService extends AbstractSensorThingsEntityService<DatastreamRepository, DatastreamEntity> {

    private DatastreamMapper mapper;
    
    @Autowired
    private UnitRepository unitRepository;
    
    @Autowired
    private FormatRepository formatRepository;
    
    private final static DatastreamQuerySpecifications dQS = new DatastreamQuerySpecifications();

    public DatastreamService(DatastreamRepository repository, DatastreamMapper mapper) {
        super(repository);
        this.mapper = mapper;
    }

    @Override
    public EntityTypes getType() {
        return EntityTypes.Datastream;
    }

    @Override
    public EntityCollection getEntityCollection(QueryOptions queryOptions) {
        EntityCollection retEntitySet = new EntityCollection();
        getRepository().findAll(createPageableRequest(queryOptions)).forEach(t -> retEntitySet.getEntities().add(mapper.createEntity(t)));
        return retEntitySet;
    }

    @Override
    public Entity getEntity(Long id) {
        Optional<DatastreamEntity> entity = getRepository().findOne(byId(id));
        return entity.isPresent() ? mapper.createEntity(entity.get()) : null;
    }

    @Override
    public EntityCollection getRelatedEntityCollection(Long sourceId, EdmEntityType sourceEntityType, QueryOptions queryOptions) {
        Iterable<DatastreamEntity> datastreams = getRepository().findAll(getFilter(sourceId, sourceEntityType), createPageableRequest(queryOptions));

        EntityCollection retEntitySet = new EntityCollection();
        datastreams.forEach(t -> retEntitySet.getEntities().add(mapper.createEntity(t)));
        return retEntitySet;
    }
    
    @Override
    public long getRelatedEntityCollectionCount(Long sourceId, EdmEntityType sourceEntityType) {
        return getRepository().count(getFilter(sourceId, sourceEntityType));
    }
    
    @Override
    public boolean existsEntity(Long id) {
        return getRepository().exists(byId(id));
    }

    @Override
    public boolean existsRelatedEntity(Long sourceId, EdmEntityType sourceEntityType) {
        return this.existsRelatedEntity(sourceId, sourceEntityType, null);
    }

    @Override
    public boolean existsRelatedEntity(Long sourceId, EdmEntityType sourceEntityType, Long targetId) {
        BooleanExpression filter = getFilter(sourceId, sourceEntityType);
        if (filter == null) {
            return false;
        }
        if (targetId != null) {
            filter = filter.and(dQS.withId(targetId));
        }
        return getRepository().exists(filter);
    }

    @Override
    public OptionalLong getIdForRelatedEntity(Long sourceId, EdmEntityType sourceEntityType) {
        return this.getIdForRelatedEntity(sourceId, sourceEntityType, null);
    }

    @Override
    public OptionalLong getIdForRelatedEntity(Long sourceId, EdmEntityType sourceEntityType, Long targetId) {
        Optional<DatastreamEntity> thing = this.getRelatedEntityRaw(sourceId, sourceEntityType, targetId);
        if (thing.isPresent()) {
            return OptionalLong.of(thing.get().getId());
        } else {
            return OptionalLong.empty();
        }
    }

    @Override
    public Entity getRelatedEntity(Long sourceId, EdmEntityType sourceEntityType) {
        return this.getRelatedEntity(sourceId, sourceEntityType, null);
    }

    @Override
    public Entity getRelatedEntity(Long sourceId, EdmEntityType sourceEntityType, Long targetId) {
        Optional<DatastreamEntity> thing = this.getRelatedEntityRaw(sourceId, sourceEntityType, targetId);
        if (thing.isPresent()) {
            return mapper.createEntity(thing.get());
        } else {
            return null;
        }
    }
    
    @Override
    protected String checkPropertyForSorting(String property) {
        switch (property) {
        case "phenomenonTime":
            return DatastreamEntity.PROPERTY_SAMPLING_TIME_START;
        case "resultTime":
            return DatastreamEntity.PROPERTY_RESULT_TIME_START;
        default:
            return super.checkPropertyForSorting(property);
        }
    }

    /**
     * Retrieves Datastream Entity with Relation to sourceEntity from Database.
     * Returns empty if Entity is not found or Entities are not related.
     * 
     * @param sourceId Id of the Source Entity
     * @param sourceEntityType Type of the Source Entity
     * @param targetId Id of the Entity to be retrieved
     * @return Optional<DatastreamEntity> Requested Entity
     */
    private Optional<DatastreamEntity> getRelatedEntityRaw(Long sourceId, EdmEntityType sourceEntityType, Long targetId) {
        BooleanExpression filter = getFilter(sourceId, sourceEntityType);
        if (filter == null) {
            return Optional.empty();
        }

        if (targetId != null) {
            filter = filter.and(dQS.withId(targetId));
        }
        return getRepository().findOne(filter);
    }

    /**
     * Creates BooleanExpression to Filter Queries depending on source Entity Type
     * 
     * @param sourceId ID of Source Entity
     * @param sourceEntityType Type of Source Entity
     * @return BooleanExpression Filter
     */
    private BooleanExpression getFilter(Long sourceId, EdmEntityType sourceEntityType) {
        BooleanExpression filter;
        switch(sourceEntityType.getFullQualifiedName().getFullQualifiedNameAsString()) {
        case "iot.Thing": {
            filter = dQS.withThing(sourceId);
            break;
        }
        case "iot.Sensor": {
            filter = dQS.withSensor(sourceId);
            break;
        }
        case "iot.ObservedProperty": {
            filter = dQS.withObservedProperty(sourceId);
            break;
        }
        case "iot.Observation": {
            filter = dQS.withObservation(sourceId);
            break;
        }
        default: return null;
        }
        return filter;
    }

    /**
     * Constructs SQL Expression to request Entity by ID.
     * 
     * @param id id of the requested entity
     * @return BooleanExpression evaluating to true if Entity is found and valid
     */
    private BooleanExpression byId(Long id) {
        return dQS.withId(id);
    }

    @Override
    public DatastreamEntity create(DatastreamEntity datastream) throws ODataApplicationException {
        if (datastream.getId() != null && !datastream.isSetName()) {
            return getRepository().findOne(dQS.withId(datastream.getId())).get();
        }
        if (getRepository().exists(dQS.withName(datastream.getName()))) {
            Optional<DatastreamEntity> optional = getRepository().findOne(dQS.withName(datastream.getName()));
            return optional.isPresent() ? optional.get() : null;
        }
        checkObservationType(datastream);
        checkUnit(datastream);
        datastream.setObservableProperty(getObservedPropertyService().create(datastream.getObservableProperty()));
        datastream.setProcedure(getSensorService().create(datastream.getProcedure()));
        datastream.setThing(getThingService().create(datastream.getThing()));
        datastream = getRepository().save(datastream);
        processObservation(datastream);
        return datastream = getRepository().save(datastream);
    }

    @Override
    public DatastreamEntity update(DatastreamEntity entity, HttpMethod method) throws ODataApplicationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DatastreamEntity delete(DatastreamEntity entity) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private void checkUnit(DatastreamEntity datastream) {
        UnitEntity unit;
        if (!unitRepository.existsBySymbol(datastream.getUnit().getSymbol())) {
            unit = unitRepository.save(datastream.getUnit());
        } else {
            unit = unitRepository.findBySymbol(datastream.getUnit().getSymbol());
        }
        datastream.setUnit(unit);
    }

    private void checkObservationType(DatastreamEntity datastream) {
        FormatEntity format;
        if (!formatRepository.existsByFormat(datastream.getObservationType().getFormat())) {
            format = formatRepository.save(datastream.getObservationType());
        } else {
            format = formatRepository.findByFormat(datastream.getObservationType().getFormat());
        }
        datastream.setObservationType(format);
    }
    
    private void processObservation(DatastreamEntity datastream) throws ODataApplicationException {
        Set<DatasetEntity> datasets = new LinkedHashSet<>();
        for (StaDataEntity observation : datastream.getObservations()) {
            DataEntity<?> data = getObservationService().create(observation);
            if (data != null) {
                datasets.add(data.getDataset());
            }
        }
        datastream.setDatasets(datasets);
    }

    private AbstractSensorThingsEntityService<?, ThingEntity> getThingService() {
        return (AbstractSensorThingsEntityService<?, ThingEntity>) getEntityService(EntityTypes.Thing);
    }
    
    private AbstractSensorThingsEntityService<?, ProcedureEntity> getSensorService() {
        return (AbstractSensorThingsEntityService<?, ProcedureEntity>) getEntityService(EntityTypes.Sensor);
    }
    
    private AbstractSensorThingsEntityService<?, PhenomenonEntity> getObservedPropertyService() {
        return (AbstractSensorThingsEntityService<?, PhenomenonEntity>) getEntityService(EntityTypes.ObservedProperty);
    }
    
    private AbstractSensorThingsEntityService<?, DataEntity<?>> getObservationService() {
        return (AbstractSensorThingsEntityService<?, DataEntity<?>>) getEntityService(EntityTypes.Observation);
    }
}
