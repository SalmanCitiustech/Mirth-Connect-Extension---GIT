package com.mirth.connect.plugins.gitplugin;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.core.Operation.ExecuteType;
import com.mirth.connect.client.core.api.BaseServletInterface;
import com.mirth.connect.client.core.api.MirthOperation;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/extensions/gitplugin")
@Api("Extension Services")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public interface GitPluginChannelServletInterface extends BaseServletInterface {

	public static final String PLUGIN_POINT = "Git Plugin";
	public static final String PERMISSION_VIEW = "View Git Plugin";
	public static final String PERMISSION_SAVE = "Save Channel XML";

	@GET
	@Path("/")
	@ApiOperation("Retrieves git plugin entry.")
	@MirthOperation(name = "getGitPlugin", display = "Git Plugin", permission = PERMISSION_VIEW, type = ExecuteType.ASYNC)
	public String getGitPlugin() throws ClientException;
	// @formatter:on

}
