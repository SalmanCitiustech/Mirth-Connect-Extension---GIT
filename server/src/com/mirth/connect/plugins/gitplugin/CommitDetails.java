package com.mirth.connect.plugins.gitplugin;

import java.io.Serializable;

import org.eclipse.jgit.lib.ObjectId;

public class CommitDetails implements Serializable{
	
	private ObjectId commitId;
	private String commitTime;
	private String commitBy;
	private String commitAuthor;
	private String commitMessage;
	private String commitName;
		
	public ObjectId getCommitId() {
		return commitId;
	}
	public void setCommitId(ObjectId commitId) {
		this.commitId = commitId;
	}
	public String getCommitTime() {
		return commitTime;
	}
	public void setCommitTime(String commitTime) {
		this.commitTime = commitTime;
	}
	public String getCommitBy() {
		return commitBy;
	}
	public void setCommitBy(String commitBy) {
		this.commitBy = commitBy;
	}
	public String getCommitAuthor() {
		return commitAuthor;
	}
	public void setCommitAuthor(String commitAuthor) {
		this.commitAuthor = commitAuthor;
	}
	public String getCommitMessage() {
		return commitMessage;
	}
	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}
	public String getCommitName() {
		return commitName;
	}
	public void setCommitName(String commitName) {
		this.commitName = commitName;
	}
		
}
