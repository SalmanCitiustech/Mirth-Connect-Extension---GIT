package com.mirth.connect.plugins.gitplugin;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.apache.log4j.Logger;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.server.api.MirthServlet;

public class GitPluginChannelServlet extends MirthServlet implements GitPluginChannelServletInterface {

	private static Logger logger = Logger.getLogger(GitPluginChannelServlet.class);

	public GitPluginChannelServlet(@Context HttpServletRequest request, @Context SecurityContext sc) {
		super(request, sc, PLUGIN_POINT);
	}

	@Override
	public String getGitPlugin() throws ClientException {
		return "Git Plugin called";
	}

}
