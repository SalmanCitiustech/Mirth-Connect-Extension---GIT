package com.mirth.connect.plugins.gitplugin;

import java.util.Properties;
import org.apache.log4j.Logger;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.server.controllers.ControllerFactory;

public class DefaultGitPluginChannelController extends GitPluginChannelController {

	private static final ConfigurationController configurationController = ControllerFactory.getFactory()
			.createConfigurationController();

	protected static final String GIT_PLUGIN_KEY = "<Git Plugin>";
	private Logger logger = Logger.getLogger(this.getClass());
	
	@Override
	public void init(Properties properties) throws GitConfigException {
		// TODO Auto-generated method stub
	}

	@Override
	public void update(Properties properties) throws GitConfigException {
		// TODO Auto-generated method stub
	}
	
}
