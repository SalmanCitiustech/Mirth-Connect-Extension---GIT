package com.mirth.connect.plugins.gitplugin;

import com.mirth.connect.client.ui.AbstractChannelTabPanel;
import com.mirth.connect.plugins.ChannelTabPlugin;

public class GitPluginClientChannel extends ChannelTabPlugin {

	AbstractChannelTabPanel channelTabPanel = null;

	public GitPluginClientChannel(String pluginName) {
		super(pluginName);
		channelTabPanel = new GitPluginPanelChannel();
	}

	@Override
	public AbstractChannelTabPanel getChannelTabPanel() {
		return channelTabPanel;
	}

	@Override
	public String getPluginPointName() {
		return "Git Plugin";
	}

}
