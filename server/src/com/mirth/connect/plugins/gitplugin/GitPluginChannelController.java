package com.mirth.connect.plugins.gitplugin;

import java.util.Properties;

import com.mirth.connect.server.ExtensionLoader;

public abstract class GitPluginChannelController {

	private static GitPluginChannelController instance = null;

	public static GitPluginChannelController getInstance() {
		synchronized (DefaultGitPluginChannelController.class) {
			if (instance == null) {
				instance = ExtensionLoader.getInstance().getControllerInstance(GitPluginChannelController.class);

				if (instance == null) {
					instance = new DefaultGitPluginChannelController();
				}
			}

			return instance;
		}
	}
	
	public abstract void init(Properties properties) throws GitConfigException;

    public abstract void update(Properties properties) throws GitConfigException;
}
