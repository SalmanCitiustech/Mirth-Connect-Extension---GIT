package com.mirth.connect.plugins.gitplugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.treewalk.TreeWalk;
/*
 * This class provides all utility methods to perform GIT operations.
 * 
 */
public class GitUtility {
	private static Logger logger = Logger.getLogger(GitUtility.class);
	
	/*
	 * Method to fetch the list of commit history for the file from Git.
	 * @param path This is local path xml File.
	 * @param fileNameThis name of file for which commit history needs to be fetched from Git.
	 * @returns  List<CommitDetails> This is list of Commit details of each version of file.
	 * @exception NoHeadException,JGitInternalException,IOException Excetions while 
	 * 
	 */
	public static List<CommitDetails> getFileCommitHistory(String path, String fileName) throws NoHeadException,JGitInternalException,IOException
	{
		List<CommitDetails> commits = new ArrayList<CommitDetails>();
		
			File file = new File(path);
			Git git = Git.init().setDirectory(file).call();

			git = Git.open(file);
			//Retrieve commit logs and prepare the list
			Iterable<RevCommit> log = git.log().addPath(fileName).call();
			for (RevCommit revCommit : log) {
				PersonIdent PersonIdent = revCommit.getAuthorIdent();
				CommitDetails commitDetails = new CommitDetails();
				commitDetails.setCommitId(revCommit.getId());
				String dateAsText = new SimpleDateFormat(GitConstants.DTFORMAT_YYYYMMDD_HHMMSS)
                        .format(new Date(revCommit.getCommitTime() * 1000L));
                commitDetails.setCommitTime(dateAsText);
				commitDetails.setCommitAuthor(PersonIdent.getName());
				commitDetails.setCommitMessage(revCommit.getFullMessage());
				commitDetails.setCommitName(revCommit.getName());
				commits.add(commitDetails);
			}
		
		return commits;
	}
	
	/*
	 * Method to fetch file of the specific revision 
	 * @param commitId This is revision  of which details should be fetched
	 * @param repoDir This name of file for which commit history needs to be fetched from Git.
	 * @returns  List<CommitDetails> This is list of Commit details of each version of file.
	 * @exception NoHeadException,JGitInternalException,IOException Excetions while 
	 * @param commitMessage This is user Message to add while committing file to Git.
	 */
	public static String readVersionFile(String commitId, File repoDir, String filepath) {
		ObjectId lastCommitId;
		String versionData = null;
		Repository repo = null;
		try {
			repo = new FileRepository(repoDir);
			lastCommitId = repo.resolve(commitId);

			RevWalk revWalk = new RevWalk(repo);
			RevCommit commit = revWalk.parseCommit(lastCommitId);
			TreeWalk walk = TreeWalk.forPath(repo, filepath, commit.getTree());
			if (walk != null) {
				byte[] bytes = repo.open(walk.getObjectId(0)).getBytes();
				versionData = new String(bytes, StandardCharsets.UTF_8);
			} else {
				logger.error(GitConstants.GIT_PLUGIN+GitConstants.NOFILEPATH + GitConstants.SEP_COLON + filepath);
				throw new IllegalArgumentException(GitConstants.NOFILEPATH );
			}

		} catch (IOException e) {
			logger.error(GitConstants.GIT_PLUGIN+e);
		}
		return versionData;
	}

}
