package com.mirth.connect.plugins.gitplugin;

import static com.mirth.connect.plugins.gitplugin.GitPluginChannelServletInterface.PERMISSION_SAVE;
import static com.mirth.connect.plugins.gitplugin.GitPluginChannelServletInterface.PERMISSION_VIEW;
import static com.mirth.connect.plugins.gitplugin.GitPluginChannelServletInterface.PLUGIN_POINT;

import java.util.Properties;

import org.apache.log4j.Logger;

import com.mirth.connect.client.core.api.util.OperationUtil;
import com.mirth.connect.model.ExtensionPermission;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.ServicePlugin;

public class GitPluginProvider implements ServicePlugin {
	
	private GitPluginChannelController gitPluginController = GitPluginChannelController.getInstance();
	private ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
	private Logger logger = Logger.getLogger(this.getClass());

	@Override
	public String getPluginPointName() {
		return PLUGIN_POINT;
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(Properties properties) {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(Properties properties) {
		try {
			gitPluginController.update(properties);
		} catch (GitConfigException e) {
			logger.error("Failed to update properties.", e);
		}
	}

	@Override
	public Properties getDefaultProperties() {
		return new Properties();
	}

	@Override
	public ExtensionPermission[] getExtensionPermissions() {
		ExtensionPermission viewPermission = new ExtensionPermission(PLUGIN_POINT, PERMISSION_VIEW,
				"Displays the contents of the Git Plugin.",
				OperationUtil.getOperationNamesForPermission(PERMISSION_VIEW, GitPluginChannelServletInterface.class),
				new String[] {});
		ExtensionPermission savePermission = new ExtensionPermission(PLUGIN_POINT, PERMISSION_VIEW,
				"Save the contents of the Git Plugin to git repo.",
				OperationUtil.getOperationNamesForPermission(PERMISSION_SAVE, GitPluginChannelServletInterface.class),
				new String[] {});
		return new ExtensionPermission[] { viewPermission, savePermission };
	}
}
