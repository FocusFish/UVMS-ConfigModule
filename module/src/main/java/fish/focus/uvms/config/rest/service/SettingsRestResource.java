/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package fish.focus.uvms.config.rest.service;

import fish.focus.schema.config.types.v1.SettingType;
import fish.focus.schema.config.types.v1.SettingsCreateQuery;
import fish.focus.uvms.rest.security.RequiresFeature;
import fish.focus.uvms.rest.security.UnionVMSFeature;
import fish.focus.uvms.config.rest.mapper.ModuleStatusMapper;
import fish.focus.uvms.config.service.bean.ConfigServiceBean;
import fish.focus.uvms.config.service.dao.bean.ConfigDaoBean;
import fish.focus.uvms.config.service.entity.component.Setting;
import fish.focus.uvms.config.service.mapper.ConfigMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Stateless
@Path("/")
@RequiresFeature(UnionVMSFeature.viewConfiguration)
public class SettingsRestResource {

    final static Logger LOG = LoggerFactory.getLogger(SettingsRestResource.class);

    @Inject
    ConfigServiceBean serviceLayer;

    @Inject
    ConfigDaoBean configDaoBean;

    @Context
    private HttpServletRequest request;

    /**
     * @param query an object containing the setting and the name of the module
     * @return the created setting
     * @summary Creates a new setting.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @POST
    @Consumes(value = { MediaType.APPLICATION_JSON })
    @Produces(value = { MediaType.APPLICATION_JSON })
    @Path("/settings")
    public Response create(SettingsCreateQuery query) {
        LOG.info("Create setting invoked in rest layer:{}",query);
        try {
            SettingType setting = serviceLayer.create(query.getSetting(), query.getModuleName(), request.getRemoteUser());
            return Response.ok(setting).build();
        }
        catch (Exception e) {
            LOG.error("[ Error when creating setting:{} ] {} ",query, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * @param settingId setting ID
     * @return setting with given ID
     * @summary Returns the setting with the given ID.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @GET
    @Produces(value = { MediaType.APPLICATION_JSON })
    @Path("/settings/{id}")
    public Response getById(@PathParam(value = "id") final Long settingId) {
        LOG.info("Get setting by ID invoked in rest layer: {}",settingId);
        try {
            return Response.ok(serviceLayer.getById(settingId)).build();
        }
        catch (Exception e) {
            LOG.error("[ Error when getting setting by ID. {}] {} ",settingId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Returns either a list of settings for not-null module, or a map from
     * module name to list of settings if argument module is null.
     * 
     * @param moduleName name of a module
     * @return settings for one or more modules
     * @summary Returns a list of settings for a module.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @GET
    @Produces(value = { MediaType.APPLICATION_JSON })
    @Path("/settings")
    public Response getByModuleName(@QueryParam("moduleName") String moduleName) {
        LOG.info("Get settings invoked in rest layer:{}",moduleName);
        try {
            List<SettingType> settings = serviceLayer.getListIncludingGlobal(moduleName);
            if (settings == null) {
                return Response.status(400).entity("No module called " + moduleName + " exists.").build();
            }

            return Response.ok(settings).build();
        }
        catch (Exception ex) {
            LOG.error("[ Error when getting settings list. {} ] {} ",moduleName, ex);
            throw ex;
        }
    }

    /**
     * @param settingId the ID of the setting to be updated
     * @param setting the setting itself
     * @return the updated setting
     * @summary Updates an existing setting.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @PUT
    @Consumes(value = { MediaType.APPLICATION_JSON })
    @Produces(value = { MediaType.APPLICATION_JSON })
    @Path("/settings/{id}")
    public Response update(@PathParam(value = "id") Long settingId, final SettingType setting) {
        LOG.info("Update setting invoked in rest layer. {} {}",settingId,setting);
        try {
            return Response.ok(serviceLayer.update(setting, request.getRemoteUser())).build();
        }
        catch (Exception e) {
            LOG.error("[ Error when updating setting. {} {}] {} ",settingId,setting, e.getMessage(), e);
            throw e;
        }
    }

    @PUT
    @Consumes(value = { MediaType.APPLICATION_JSON })
    @Produces(value = { MediaType.APPLICATION_JSON })
    @Path("/settings/{module}/{key}/{value}")
    public Response update(@PathParam(value = "module") String module, @PathParam(value = "key") String key, @PathParam(value = "value") String value) {
        LOG.info("Updateing setting {} to {}.", key, value);
        try {
            Setting settingByKeyAndModule = configDaoBean.getSettingByKeyAndModule(key, module);
            settingByKeyAndModule.setValue(value);
            settingByKeyAndModule.setUpdatedBy(request.getRemoteUser());
            return Response.ok(ConfigMapper.toModel(settingByKeyAndModule)).build();
        }
        catch (Exception e) {
            LOG.error("[ Error when updating setting key {} to {}] {} ", key, value, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * @summary Deletes a setting.
     * @param id the ID of the setting to reset
     * @return the deleted setting
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @DELETE
    @Produces(value = { MediaType.APPLICATION_JSON })
    @Path("/settings/{id}")
    public Response delete(@PathParam(value = "id") Long id) {
        LOG.info("Delete setting invoked in rest layer. {}",id);
        try {
            return Response.ok(serviceLayer.reset(id, request.getRemoteUser())).build();
        }
        catch (Exception e) {
            LOG.error("[ Error when updating setting. {}] {} ",id, e.getMessage() , e);
            throw e;
        }
    }

    /**
     * @summary Returns a catalog of settings, grouped by module.
     * @return a mapping of modules to lists of their settings
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @GET
    @Produces(value = {MediaType.APPLICATION_JSON})
    @Path("/catalog")
    public Response catalog() {
        try {
            return Response.ok(serviceLayer.getCatalog()).build();
        }
        catch (Exception ex) {
            LOG.error("[ Error when getting catalog. ] {} ", ex.getMessage() , ex);
            throw ex;
        }
    }

    /**
     * @summary Returns an object of module statuses (online and last ping).
     * @return object of module statuses
     */
    @GET
    @Produces(value = {MediaType.APPLICATION_JSON})
    @Path("/pings")
    public Response getPings() {
        Map<String, Instant> timestamps = serviceLayer.getModuleTimestamps();
        return Response.ok(ModuleStatusMapper.mapToModuleStatus(timestamps)).build();
    }

    /**
     * @summary Returns all global settings.
     * @return global settings
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @GET
    @Produces(value = {MediaType.APPLICATION_JSON})
    @Path("/globals")
    public Response getGlobalSettings() {
        try {
            return Response.ok(serviceLayer.getGlobalSettings()).build();
        } catch (Exception e) {
            LOG.error("[ Error when getting global settings. ] {} ", e.getMessage(), e);
            throw e;
        }
    }

}