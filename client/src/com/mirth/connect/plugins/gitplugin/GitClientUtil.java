package com.mirth.connect.plugins.gitplugin;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import org.apache.log4j.Logger;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.xml.sax.SAXException;

public class GitClientUtil {

	private static Logger logger = Logger.getLogger(GitClientUtil.class);
	private static HashMap<String, List<String>> highlights1 = null;
	private static HashMap<String, List<String>> highlights2 = null;
	private static int indicator = 0;
	private static String gitConfigFileName;
	private static String gitConfigFilePath;

	/**
	 * Method to create new file and perform all operations to save file in Git
	 * repository.
	 * 
	 * @param channelXMLData This is xml File to be saved in Git in String format
	 * @param channelName
	 * @param channelId
	 * @param prop           This is properties for Git Plugin
	 * @param commitMessage  This is user Message to add while committing file to
	 *                       Git.
	 */
	public static void cloneAndAddFile(String channelXMLData, String channelName, String channelId, Properties prop,
			String commitMessage) throws IOException, InterruptedException {
		boolean isGitEnable = Boolean.parseBoolean(prop.getProperty("git_enable"));
		if (isGitEnable) {
			String originUrl = prop.getProperty("git_url");
			Path directory = Paths.get(prop.getProperty("git_local_path") + "\\ChannelRepository");
			String[] splitUrl = originUrl.split("//");
			boolean isDirExist = Files.isDirectory(directory);

			if (!isDirExist) {
				gitClone(directory, originUrl);
			}
			// pull files from existing Repository
			gitPull(directory);

			// Create or Replace file with new Data in given directory
			String fileName = XmlUtility.readXmlFileData(directory, channelName, channelId);
			FileWriter fileWriter = new FileWriter(fileName);
			fileWriter.write(channelXMLData);
			fileWriter.close();
			Files.write(Paths.get(fileName), channelXMLData.getBytes(), StandardOpenOption.CREATE);

			// Commit file and Push it in Git Repository
			gitStage(directory);
			gitCommit(directory, commitMessage);
			gitPush(directory);

			// add logs in Mirth log file for successful operations
			switch (indicator) {
			case 1:
				logger.info(GitConstants.GIT_PLUGIN + channelName + " : " + GitConstants.GIT_CLONE_PASS);
				break;
			case 2:
				logger.info(GitConstants.GIT_PLUGIN + channelName + GitConstants.GIT_PULL_PASS);
				break;
			case 3:
				logger.info(GitConstants.GIT_PLUGIN + channelName + GitConstants.GIT_COMMIT_PASS);
				break;
			case 4:
				logger.info(GitConstants.GIT_PLUGIN + channelName + GitConstants.GIT_PUSH_PASS);
				break;
			}
		}
	}

	public static void createDirectoryAndClone(Properties prop) throws IOException, InterruptedException {

		String originUrl = prop.getProperty("git_url");
		Path directory = Paths.get(prop.getProperty("git_local_path") + "\\ChannelRepository");
		boolean isDirExist = Files.isDirectory(directory);

		if (!isDirExist) {
			gitClone(directory, originUrl);
		}
		// pull files from existing Repository
		gitPull(directory);
	}

	public static void gitPull(Path directory) throws IOException, InterruptedException {
		indicator = 2;
		runCommand(directory, indicator, "git", "pull");
	}

	public static void gitInit(Path directory) throws IOException, InterruptedException {
		runCommand(directory, indicator, GitConstants.git, GitConstants.init);
	}

	public static void gitStage(Path directory) throws IOException, InterruptedException {
		runCommand(directory, indicator, GitConstants.git, GitConstants.add, "-A");
	}

	public static void gitCommit(Path directory, String message) throws IOException, InterruptedException {
		indicator = 3;
		runCommand(directory, indicator, GitConstants.git, GitConstants.commit, "-m", message);
	}

	public static void gitPush(Path directory) throws IOException, InterruptedException {
		indicator = 4;
		runCommand(directory, indicator, "git", "push");
	}

	public static void gitClone(Path directory, String originUrl) throws IOException, InterruptedException {
		indicator = 1;
		runCommand(directory.getParent(), indicator, GitConstants.git, GitConstants.clone, originUrl,
				directory.getFileName().toString());
	}

	/**
	 * Method to run the Specified git commands in the given directory
	 * 
	 * @param directory This is local path set as a Git directory.
	 * @param indicator This indicates the operation being performed.
	 * @param command   These are sequence of String representing a command to run.
	 * @exception IOException,InterruptedException Error in Starting a process and
	 *                                             running the commands.
	 */
	public static void runCommand(Path directory, int indicator, String... command)
			throws IOException, InterruptedException {

		// Ensure if the directory Exists, if not throw the exception on UI
		Objects.requireNonNull(directory, "directory");
		if (!Files.exists(directory)) {
			logger.error(GitConstants.GIT_PLUGIN + directory + GitConstants.SEP_COLON + GitConstants.NODIRECTORY);
			throw new RuntimeException(GitConstants.NODIRECTORY + " '" + directory + "'");

		}

		// Create and Start a process along with two threads to print the output
		// of
		// execution
		ProcessBuilder pb = new ProcessBuilder().command(command).directory(directory.toFile());
		Process p = pb.start();

		StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), GitConstants.ERROR);
		StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), GitConstants.OUTPUT);
		outputGobbler.start();
		errorGobbler.start();
		int exit = p.waitFor();
		errorGobbler.join();
		outputGobbler.join();

		// Log the error if any of the operation breaks and throw it on UI
		if (exit != 0) {
			switch (indicator) {
			case 1:
				logger.error(GitConstants.GIT_PLUGIN + GitConstants.GIT_CLONE_FAILED);
				break;
			case 2:
				logger.error(GitConstants.GIT_PLUGIN + GitConstants.GIT_PULL_FAILED);
				break;
			case 3:
				logger.error(GitConstants.GIT_PLUGIN + GitConstants.GIT_COMMIT_FAILED);
				break;
			case 4:
				logger.error(GitConstants.GIT_PLUGIN + GitConstants.GIT_PUSH_PASS);
				break;
			}
			throw new AssertionError(String.format("runCommand returned %d", exit));

		}
	}

	/**
	 * This Inner Thread class reads the output of executed process.
	 */
	private static class StreamGobbler extends Thread {

		private final InputStream is;
		private final String type;

		private StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

		@Override
		public void run() {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
				String line;
				while ((line = br.readLine()) != null) {
					System.out.println(type + "> " + line);
					if (this.type.equalsIgnoreCase(GitConstants.ERROR)
							&& (line.contains(GitConstants.FATAL) || line.contains(GitConstants.REMOTE))) {
						logger.error(GitConstants.GIT_PLUGIN + line);
					}
				}
			} catch (IOException ioe) {
				logger.error(GitConstants.GIT_PLUGIN + ioe);
			}
		}
	}

	/**
	 * Method to fetch file of the specific revision
	 * 
	 * @param commitId This is revision of which details should be fetched
	 * @param repoDir  This name of file for which commit history needs to be
	 *                 fetched from Git.
	 * @returns List<CommitDetails> This is list of Commit details of each version
	 *          of file.
	 * @exception NoHeadException,JGitInternalException,IOException Excetions while
	 * @param commitMessage This is user Message to add while committing file to
	 *                      Git.
	 */
	public static Properties readGitProperties(String fileName) throws Exception {
		FileInputStream fis = null;
		Properties prop = null;
		try {
			File file = new File(fileName);
			if (!file.exists()) {
				FileOutputStream fo = new FileOutputStream(file);
			}
			fis = new FileInputStream(file);
			prop = new Properties();
			prop.load(fis);
		} catch (Exception e) {
			throw new Exception(e);
		} finally {
			fis.close();
		}
		return prop;
	}

	/**
	 * Method to Compares the two xml files of the specific revision
	 * 
	 * @param source is of Reader which is contained in package java.io
	 * @returns List<Difference> This is list of difference in xml files.
	 * @exception SAXException, IOException
	 */
	public static List<Difference> compareXML(Reader source, Reader target) throws SAXException, IOException {
		// creating Diff instance to compare two XML files
		Diff xmlDiff = new Diff(source, target);

		// for getting detailed differences between two xml files
		DetailedDiff detailXmlDiff = new DetailedDiff(xmlDiff);

		return (List<Difference>) detailXmlDiff.getAllDifferences();
	}

	/**
	 * This method creates a hashmap of nodes and values that should be highlighted
	 * in each of the version file individually
	 * 
	 * @param differences
	 * 
	 *                    returns List<HashMap<String,List<String>>> List of HashMap
	 */
	public static List<HashMap<String, List<String>>> getFileDifferences(
			List<org.custommonkey.xmlunit.Difference> differences) {
		highlights1 = new HashMap<String, List<String>>();
		highlights2 = new HashMap<String, List<String>>();
		List<String> valueList1 = null;
		List<String> valueList2 = null;
		List<HashMap<String, List<String>>> highlightList = new ArrayList<>();
		for (org.custommonkey.xmlunit.Difference difference : differences) {
			String node = difference.toString().split("comparing ")[1].split("...>")[0];
			node = Character.isWhitespace(node.charAt(node.length() - 1)) ? node.substring(0, node.length() - 1) + ">"
					: node + ">";
			if (node.contains("at null to")) {
				node = node.substring("at null to ".length());
			}
			if (highlights1.containsKey(node) && highlights1.containsKey(node)) {
				valueList1 = highlights1.get(node);
				valueList1.add(difference.getControlNodeDetail().getValue());

				valueList2 = highlights2.get(node);
				valueList2.add(difference.getTestNodeDetail().getValue());

			} else {
				valueList1 = new ArrayList<String>();
				valueList1.add(difference.getControlNodeDetail().getValue());

				valueList2 = new ArrayList<String>();
				valueList2.add(difference.getTestNodeDetail().getValue());
			}
			highlights1.put(node, valueList1);
			highlights2.put(node, valueList2);

			highlightList.add(highlights1);
			highlightList.add(highlights2);
		}
		return highlightList;
	}

	/**
	 * This method Highlights the differences with Grey color
	 */
	public static void highlightDifferences(HashMap<String, List<String>> highlights, JTextArea editorPane) {

		for (Map.Entry<String, List<String>> entry : highlights.entrySet()) {

			for (String s : entry.getValue()) {

				Pattern pattern = Pattern.compile(s);
				Matcher matcher = pattern.matcher(editorPane.getText());
				Highlighter highlighter = editorPane.getHighlighter();
				HighlightPainter diffPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.GRAY);
				HighlightPainter addPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.CYAN);

				// Check all occurrences
				while (matcher.find()) {

					try {
						int start = matcher.start();
						int startoffset = editorPane.getLineStartOffset(editorPane.getLineOfOffset(start));
						int endoffset = editorPane.getLineEndOffset(editorPane.getLineOfOffset(start));
						String lineText = editorPane.getText(startoffset, endoffset - startoffset);

						if (lineText.contains(entry.getKey())) {
							String trimmedKey = entry.getKey().replace(" ", "");
							if (trimmedKey.equalsIgnoreCase("<" + s + ">")) {
								highlighter.addHighlight(start - 1, endoffset, addPainter);
							} else
								highlighter.addHighlight(start, matcher.end(), diffPainter);
						}
					} catch (BadLocationException e) {
						logger.error(
								GitConstants.GIT_PLUGIN + "Location Error while highlighting in file comparison :" + e);
					}

				}
			}

		}

	}

	public static boolean createConfigFile() {
		boolean isFileCreated = false;
		try {
			if (System.getProperty("os.name").contains("Windows")) {
				String username = System.getProperty("user.name");
				gitConfigFilePath = GitConstants.CONFIG_FILE_PATH_WINDOWS + GitConstants.SEP_PATH_DBS + username;

				boolean isUserDirExist = Files.isDirectory(Paths.get(gitConfigFilePath));
				if (isUserDirExist) {
					boolean isGitFileDirExist = Files
							.isDirectory(Paths.get(gitConfigFilePath + GitConstants.git_conf_local_folder));
					String configFileName = null;
					gitConfigFilePath = gitConfigFilePath + GitConstants.git_conf_local_folder;
					setGitConfigFilePath(gitConfigFilePath);
					if (!isGitFileDirExist) {
						boolean isDirCreated = new File(gitConfigFilePath).mkdir();
						if (isDirCreated) {
							configFileName = gitConfigFilePath + GitConstants.git_conf_file_name;
							setGitConfigFileName(configFileName);
							File configFile = new File(configFileName);
							isFileCreated = configFile.createNewFile();
						}
					} else {
						configFileName = gitConfigFilePath + GitConstants.git_conf_file_name;
						setGitConfigFileName(configFileName);
						isFileCreated = true;
					}
				}

			}
		} catch (IOException e) {
			logger.error(GitConstants.GIT_PLUGIN + e);
		}
		return isFileCreated;
	}

	public static void setGitConfigFileName(String gitConfigFile) {
		gitConfigFileName = gitConfigFile;
	}

	public static String getGitConfigFileName() {
		return gitConfigFileName;
	}
	
	public static void setGitConfigFilePath(String gitConfFilePath) {
		gitConfigFilePath = gitConfFilePath;
	}

	public static String getGitConfigFilePath() {
		return gitConfigFilePath;
	}
}
