/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.sta.utils;

import java.util.List;
import java.util.Locale;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.n52.sta.data.EntityServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Helper class to resolve URI resources
 *
 * @author <a href="mailto:s.drost@52north.org">Sebastian Drost</a>
 */
@Component
public class UriResourceNavigationResolver {

    @Autowired
    private EntityServiceRepository serviceRepository;

    /**
     * Resolves the root URI resource as UriResourceEntitySet
     *
     * @param uriResource the URI resource paths to resolve
     * @return the root UriResourceEntitySet
     * @throws ODataApplicationException
     */
    public UriResourceEntitySet resolveRootUriResource(UriResource uriResource) throws ODataApplicationException {

        if (!(uriResource instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Only EntitySet is supported",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
        return uriResourceEntitySet;
    }

    /**
     * Resolves complex URI resource Navigation paths to determine the target
     * {@link NavigationLink}
     *
     * @param navigationResourcePaths the URI resource paths to resolve
     * @return the target {@link NavigationLink}
     * @throws ODataApplicationException
     */
    public NavigationLink resolveUriResourceNavigationPaths(List<UriResource> navigationResourcePaths) throws ODataApplicationException {
        UriResourceEntitySet uriResourceEntitySet = resolveRootUriResource(navigationResourcePaths.get(0));
        EdmEntitySet targetEntitySet = uriResourceEntitySet.getEntitySet();

        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        Entity sourceEntity = serviceRepository.getEntityService(uriResourceEntitySet.getEntityType().getName()).getEntity(keyPredicates);
        EdmEntityType targetEntityType = null;

        if (sourceEntity == null) {
            throw new ODataApplicationException("Entity not found.",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
        }

        for (int navigationCount = 1; navigationCount < navigationResourcePaths.size() - 1; navigationCount++) {
            UriResource targetSegment = navigationResourcePaths.get(navigationCount);

            if (targetSegment instanceof UriResourceNavigation) {
                UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) targetSegment;
                EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                targetEntityType = edmNavigationProperty.getType();

                List<UriParameter> navKeyPredicates = uriResourceNavigation.getKeyPredicates();
                Entity targetEntity = null;

                // e.g. /Things(1)/Location
                if (navKeyPredicates.isEmpty()) {
                    targetEntity = serviceRepository.getEntityService(targetEntityType.getName()).getRelatedEntity(sourceEntity);

                } else { // e.g. /Things(1)/Locations(1)
                    targetEntity = serviceRepository.getEntityService(targetEntityType.getName()).getRelatedEntity(sourceEntity, navKeyPredicates);
                }
                if (targetEntity == null) {
                    throw new ODataApplicationException("Entity not found.",
                            HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
                }

                sourceEntity = targetEntity;
                targetEntitySet = getNavigationTargetEntitySet(targetEntitySet, edmNavigationProperty);
            }
        }
        UriResource lastSegment = navigationResourcePaths.get(navigationResourcePaths.size() - 1);
        if (lastSegment instanceof UriResourceNavigation) {
            EdmNavigationProperty edmNavigationProperty = ((UriResourceNavigation) lastSegment).getProperty();
            targetEntitySet = getNavigationTargetEntitySet(targetEntitySet, edmNavigationProperty);
        }
        NavigationLink link = new NavigationLink();
        link.setSourceEntity(sourceEntity);
        link.setTargetEntitySet(targetEntitySet);

        return link;
    }

    /**
     * Determines the target EdmEntitySet for a navigation property
     *
     * @param startEdmEntitySet the EdmEntitySet to start with
     * @param edmNavigationProperty the navigation property from one entity type
     * to another
     * @return the target EdmEntitySet for the navigation property
     * @throws ODataApplicationException
     */
    public static EdmEntitySet getNavigationTargetEntitySet(EdmEntitySet startEdmEntitySet,
            EdmNavigationProperty edmNavigationProperty)
            throws ODataApplicationException {

        EdmEntitySet navigationTargetEntitySet = null;

        String navPropName = edmNavigationProperty.getName();
        EdmBindingTarget edmBindingTarget = startEdmEntitySet.getRelatedBindingTarget(navPropName);
        if (edmBindingTarget == null) {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        if (edmBindingTarget instanceof EdmEntitySet) {
            navigationTargetEntitySet = (EdmEntitySet) edmBindingTarget;
        } else {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        return navigationTargetEntitySet;
    }

}