/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.sta.service.processor;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.n52.sta.data.service.AbstractSensorThingsEntityService;
import org.n52.sta.service.deserializer.SensorThingsDeserializer;
import org.n52.sta.service.handler.AbstractEntityRequestHandler;
import org.n52.sta.service.handler.crud.AbstractEntityCrudRequestHandler;
import org.n52.sta.service.handler.crud.EntityCrudRequestHandlerRepository;
import org.n52.sta.service.query.QueryOptions;
import org.n52.sta.service.query.QueryOptionsHandler;
import org.n52.sta.service.query.URIQueryOptions;
import org.n52.sta.service.response.EntityResponse;
import org.n52.sta.service.serializer.SensorThingsSerializer;
import org.n52.sta.utils.EntityAnnotator;
import org.n52.sta.utils.UriResourceNavigationResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
@Component
public class SensorThingsEntityProcessor implements EntityProcessor {

    @Autowired
    AbstractEntityRequestHandler requestHandler;
    
    @Autowired
    EntityCrudRequestHandlerRepository crudRequestHandlerReportitory;

    @Autowired
    QueryOptionsHandler queryOptionsHandler;

    @Autowired
    EntityAnnotator entityAnnotator;
    
    @Autowired
    private UriResourceNavigationResolver navigationResolver;


    private OData odata;
    private ServiceMetadata serviceMetadata;
    private ODataSerializer serializer;
    private ODataDeserializer deserializer;

    @Override
    public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        QueryOptions queryOptions = new URIQueryOptions(uriInfo, request.getRawBaseUri());
        EntityResponse entityResponse = requestHandler.handleEntityRequest(uriInfo.getUriResourceParts(), queryOptions);

        InputStream serializedContent = createResponseContent(serviceMetadata, entityResponse, queryOptions);

        // configure the response object: set the body, headers and status code
        response.setContent(serializedContent);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.JSON_NO_METADATA.toContentTypeString());
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        EntityResponse entityResponse = new EntityResponse();
        DeserializerResult deserializeRequestBody = deserializeRequestBody(request, uriInfo);
        if (deserializeRequestBody.getEntity() != null) {
            Entity entity = getCrudEntityHanlder(uriInfo).handleCreateEntityRequest(deserializeRequestBody.getEntity());
        } else if (deserializeRequestBody.getEntityCollection() != null) {
            EntityCollection entityCollection = getCrudEntityHanlder(uriInfo).handleCreateEntityRequest(deserializeRequestBody.getEntityCollection());
        }
        

        // configure the response object: set the body, headers and status code
        response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.JSON_NO_METADATA.toContentTypeString());
        
//        throw new ODataApplicationException("Not supported.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        EntityResponse entityResponse = getCrudEntityHanlder(uriInfo).handleUpdateEntityRequest(deserializeRequestBody(request, uriInfo), request.getMethod());
        

        // configure the response object: set the body, headers and status code
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.JSON_NO_METADATA.toContentTypeString());
        
        throw new ODataApplicationException("Not supported.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {
        EntityResponse entityResponse = getCrudEntityHanlder(uriInfo).handleDeleteEntityRequest(deserializeRequestBody(request, uriInfo));
        
        // configure the response object: set the body, headers and status code
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.JSON_NO_METADATA.toContentTypeString());
        
        throw new ODataApplicationException("Not supported.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    @Override
    public void init(OData odata, ServiceMetadata sm) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
        this.serializer = new SensorThingsSerializer(ContentType.JSON_NO_METADATA);
        this.deserializer = new SensorThingsDeserializer(ContentType.JSON_NO_METADATA);
        this.queryOptionsHandler.setUriHelper(odata.createUriHelper());
    }

    private InputStream createResponseContent(ServiceMetadata serviceMetadata, EntityResponse response, QueryOptions queryOptions) throws SerializerException {
        EdmEntityType edmEntityType = response.getEntitySet().getEntityType();

        // annotate the entity
        entityAnnotator.annotateEntity(response.getEntity(),
                response.getEntitySet().getEntityType(), queryOptions.getBaseURI());

        ContextURL.Builder contextUrlBuilder = ContextURL.with()
                .entitySet(response.getEntitySet())
                .suffix(ContextURL.Suffix.ENTITY);
        contextUrlBuilder.selectList(queryOptionsHandler.getSelectListFromSelectOption(
                edmEntityType, queryOptions.getExpandOption(), queryOptions.getSelectOption()));
        ContextURL contextUrl = contextUrlBuilder.build();

        EntitySerializerOptions opts = EntitySerializerOptions.with()
                .contextURL(contextUrl)
                .select(queryOptions.getSelectOption())
                .expand(queryOptions.getExpandOption())
                .build();

        SerializerResult serializerResult = serializer.entity(serviceMetadata, response.getEntitySet().getEntityType(), response.getEntity(), opts);
        InputStream serializedContent = serializerResult.getContent();

        return serializedContent;
    }

    private DeserializerResult deserializeRequestBody(ODataRequest request, UriInfo uriInfo)
            throws DeserializerException, ODataApplicationException {
        return deserializer.entity(request.getBody(), navigationResolver
                .resolveRootUriResource(uriInfo.getUriResourceParts().get(0)).getEntityType());
    }

    private AbstractEntityCrudRequestHandler getCrudEntityHanlder(UriInfo uriInfo) throws ODataApplicationException {
        return getCrudEntityHanlder(navigationResolver.resolveRootUriResource(uriInfo.getUriResourceParts().get(0)));
    }
    
    private AbstractEntityCrudRequestHandler getCrudEntityHanlder(UriResourceEntitySet uriResourceEntitySet) {
        return getUriResourceEntitySet(uriResourceEntitySet.getEntityType().getName());
    }

    private AbstractEntityCrudRequestHandler getUriResourceEntitySet(String type) {
        return crudRequestHandlerReportitory.getEntityCrudRequestHandler(type);
    }
}
