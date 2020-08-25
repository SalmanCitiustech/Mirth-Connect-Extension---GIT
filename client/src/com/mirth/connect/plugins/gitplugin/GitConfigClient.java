package com.mirth.connect.plugins.gitplugin;

import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.plugins.SettingsPanelPlugin;

public class GitConfigClient extends SettingsPanelPlugin {

	private AbstractSettingsPanel settingsPanel = null;
	
	public GitConfigClient(String name) {
		super(name);
		
		settingsPanel = new GitConfigPanel("Git Configuration", this);
		// TODO Auto-generated constructor stub
	}

	@Override
	public AbstractSettingsPanel getSettingsPanel() {
		return settingsPanel;
	}

	@Override
	public String getPluginPointName() {
		return "Git Configuration";
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
	public void reset() {
		// TODO Auto-generated method stub
		
	}

}
