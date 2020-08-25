package com.mirth.connect.plugins.gitplugin;

public class GitConfigModel {

	private boolean enable;
	private String url;
	private String gitLocalFolderPath;
	private String compareToolPath;
	private String gitConfigFilePath;
	private boolean isDefaultCompareEnabled;
	private String fileEditorPath;

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getGitLocalFolderPath() {
		return gitLocalFolderPath;
	}

	public void setGitLocalFolderPath(String gitLocalFolderPath) {
		this.gitLocalFolderPath = gitLocalFolderPath;
	}
	
	public String getCompareToolPath() {
		return compareToolPath;
	}

	public void setCompareToolPath(String compareToolPath) {
		this.compareToolPath = compareToolPath;
	}
	
	public String getGitConfigFilePath() {
		return gitConfigFilePath;
	}

	public void setGitConfigFilePath(String gitConfigFilePath) {
		this.gitConfigFilePath = gitConfigFilePath;
	}
	

	public boolean isDefaultCompareEnabled() {
		return isDefaultCompareEnabled;
	}

	public void setDefaultCompareEnabled(boolean isDefaultCompareEnabled) {
		this.isDefaultCompareEnabled = isDefaultCompareEnabled;
	}

	public String getFileEditorPath() {
		return fileEditorPath;
	}

	public void setFileEditorPath(String fileEditorPath) {
		this.fileEditorPath = fileEditorPath;
	}

	@Override
	public String toString() {
		return "GitConfigModel [enable=" + enable + ", url=" + url + ", gitLocalFolderPath=" + gitLocalFolderPath
				+ ", compareToolPath=" + compareToolPath + ", gitConfigFilePath=" + gitConfigFilePath
				+ ", isDefaultCompareEnabled=" + isDefaultCompareEnabled + "]";
	}

	
}
