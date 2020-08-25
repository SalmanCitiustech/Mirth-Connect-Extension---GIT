package com.mirth.connect.plugins.gitplugin;

public class GitConstants {

	public static final String GIT_PLUGIN = "GIT PLUGIN : ";
	public static final String GIT_CONFIG = "GIT CONFIGURATION : ";

	// Git Error Message
	public static final String GIT_PUSH_PASS = " Git Push is Successful.";
	public static final String GIT_PUSH_FAIL = " Git Push failed.";
	public static final String GIT_COMMIT_PASS = " Git Commit successful.";
	public static final String GIT_COMMIT_FAILED = " Git Commit failed.";
	public static final String GIT_PULL_PASS = " Git Pull successful.";
	public static final String GIT_PULL_FAILED = " Git Pull failed.";
	public static final String GIT_CLONE_PASS = " Git Clone successful.";
	public static final String GIT_CLONE_FAILED = " Git Clone failed.";
	public static final String NODIRECTORY = " Directory does not exists.";

	// Error Level
	public static final String ERROR = "ERROR";
	public static final String FATAL = "FATAL";
	public static final String REMOTE = "REMOTE";

	// Date Format
	public static final String DTFORMAT_YYYYMMDD_HHMMSS = "yyyy-MM-dd HH:mm:ss";
	public static final String NOFILEPATH = "No File Path Found";

	// Other
	public static final String OUTPUT = "OUTPUT";

	// Git Commands

	public static final String git = "git";
	public static final String pull = "pull";
	public static final String push = "push";
	public static final String commit = "commit";
	public static final String init = "init";
	public static final String add = "add";
	public static final String clone = "clone";

	// Separators
	public static final String SEP_COLON = " : ";
	public static final String SEP_HYPHEN = " - ";
	public static final String SEP_PATH_DBS = "\\";

	// File format

	public static final String FILEFORMAT_XML = ".xml";

	// UI Labels
	public static final String default_Compare_Label = "Select editor: ";
	public static final String git_Config_File_Label = "Git Config file Path: ";
	public static final String comparator_Dir_Label = "Compare Editor Path: ";
	public static final String git_Local_Dir_Label = "Git Local Path: ";
	public static final String file_editor_Dir_Label = "File Editor Path: ";
	public static final String url_Label = "Git URL:";
	public static final String default_Compare_Yes_Radio = "Default ";
	public static final String default_Compare_No_Radio = "Custom ";
	public static final String browseLabel = "Browse...";
	public static final String enableLabel = "Enable: ";
	public static final String yesLabel = "Yes";
	public static final String noLabel = "No";
	public static final String prodEnvLabel = "Enable Mandatory Check In: ";
	public static final String prodEnvMessage = "All Channels will be commited to Git Repository";
	public static final String CONFIG_FILE_PATH_WINDOWS = "C:\\Users";
	public static final String NA = "NA";
	public static final String git_conf_local_folder = "\\MirthGitPluginConf";
	public static final String git_conf_file_name = "\\gitconfig.properties";
}
