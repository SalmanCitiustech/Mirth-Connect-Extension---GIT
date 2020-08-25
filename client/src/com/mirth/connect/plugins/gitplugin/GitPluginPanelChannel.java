package com.mirth.connect.plugins.gitplugin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXException;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.ui.AbstractChannelTabPanel;
import com.mirth.connect.client.ui.ChannelPanel;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthTextField;
import com.mirth.connect.model.Channel;
import com.mirth.connect.model.converters.ObjectXMLSerializer;

import net.miginfocom.swing.MigLayout;
import xmleditorkit.XMLEditorKit;

/**
 * @author SalmanK, RajshreeP
 *
 */
public class GitPluginPanelChannel extends AbstractChannelTabPanel {

	private Frame parent;
	private Properties properties;
	private DefaultTableModel model;
	private int count;
	private String commitMessage = "";
	private Object[][] data;
	private String[] columns;
	String gitLocalPath = "";
	private static Logger logger = Logger.getLogger(GitPluginPanelChannel.class);

	public GitPluginPanelChannel() {
		super();
		this.parent = PlatformUI.MIRTH_FRAME;
		this.properties = new Properties();
		initComponents();
		initLayout();
	}

	/*
	 * Load Method - Method Overridden from AbstractChannelTabPanel Load gets called
	 * every time Channel is loaded
	 */
	@Override
	public void load(Channel channel) {
		try {
			count = 0;
			properties = getGitProperties();
			gitLocalPath = properties.getProperty("git_local_path");
			resetOnLoad();
			setGitConfigPanelProperties(properties);
			addButtonEvent(channel);
		} catch (ClientException e) {
			logger.error(GitConstants.GIT_PLUGIN + e);
		}
	}

	@Override
	public void save(Channel channel) {
		int option;
		boolean isGitEnable = Boolean.parseBoolean(properties.getProperty("git_enable"));
		boolean isProdEnvironment = Boolean.parseBoolean(properties.getProperty("prod_environment"));
		if (isGitEnable) {
			if (!isProdEnvironment) {
				option = JOptionPane.showConfirmDialog(this, "Would you like to commit the changes to git repository?",
						"Warning", JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.YES_OPTION) {
					saveToGit(channel);
				}
			} else {
				saveToGit(channel);
			}
		}
	}

	private void saveToGit(Channel channel) {
		ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
		String channelXML = serializer.serialize(channel);
		String channelName = channel.getName();
		String channelId = channel.getId();
		do {
			commitMessage = showInputDialog(this, "Please enter a Commit Message for the channel.", null);
		} while (!checkCommitMessage(commitMessage));

		final String workingId = parent.startWorking("Saving Channel to Git...");
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			public Void doInBackground() {
				try {
					String str = setChannelXMLData(channelXML, channelName, channelId, commitMessage);
					parent.setSaveEnabled(false);
				} catch (Exception e) {
					logger.error("An error occurred while saving channel data to git for channel  " + channelName);
					parent.alertThrowable(parent, e, "An error occurred while attempting to save channel data to git");
				}
				return null;
			}

			@Override
			public void done() {
				parent.setSaveEnabled(false);
				parent.stopWorking(workingId);
			}
		};
		worker.execute();
	}

	public static String showInputDialog(Component parentComponent, Object message, Object initialSelectionValue)
			throws HeadlessException {
		return showInputDialog(parentComponent, message, "Input", JOptionPane.QUESTION_MESSAGE, null, null,
				initialSelectionValue);
	}

	/**
	 * Shows a confirmation dialog with a text input. This is needed due to
	 * https://bugs.openjdk.java.net/browse/JDK-8208743.
	 */
	public static String showInputDialog(Component parentComponent, Object message, String title, int messageType,
			Icon icon, Object[] selectionValues, Object initialSelectionValue) throws HeadlessException {
		JOptionPane optionPane = new JOptionPane(message, messageType, JOptionPane.OK_CANCEL_OPTION);
		optionPane.setWantsInput(true);
		optionPane.setSelectionValues(selectionValues);
		optionPane.setInitialSelectionValue(initialSelectionValue);
		optionPane.setComponentOrientation(
				(parentComponent == null ? JOptionPane.getRootFrame() : parentComponent).getComponentOrientation());

		JDialog dialog = optionPane.createDialog(parentComponent, title);

		optionPane.selectInitialValue();
		dialog.setVisible(true);
		dialog.dispose();

		Object value = optionPane.getInputValue();

		if (value == JOptionPane.UNINITIALIZED_VALUE) {
			return null;
		}
		return (String) value;
	}

	/* validation on Commit message */
	private boolean checkCommitMessage(String commitMessage) {
		if (commitMessage.equals("")) {
			parent.alertWarning(this, "Commit Message cannot be empty.");
			return false;
		}
		return true;
	}

	/* Create or Modify the channel xml onto the Git Repository. */
	private String setChannelXMLData(String channelXMLData, String channelName, String channelId, String commitMessage)
			throws IOException, InterruptedException {
		GitClientUtil.cloneAndAddFile(channelXMLData, channelName, channelId, properties, commitMessage);
		return "xml changes commited to git";
	}

	/* This Method is called on Load to reset the cache */
	private void resetOnLoad() {
		gitconfigPanel.removeAll();
		versionHistoryPanel.setVisible(false);
		versionHistoryPanel.removeAll();
		if (Boolean.valueOf(properties.getProperty("git_enable"))) {
			versionHistory.setEnabled(true);
		} else {
			versionHistory.setEnabled(false);
		}
		versionHistory.removeAll();
		versionHistory.revalidate();
		versionHistory.repaint();
		hide.setEnabled(false);
		viewFilePanel.setVisible(false);
		viewFilePanel.removeAll();
		scrollpane.removeAll();
		inputpanel.removeAll();
		panel.removeAll();
		table.removeAll();
	}

	private void setGitConfigPanelProperties(Properties properties) {
		gitEnabledLabel = new JLabel("Git Enable: ");
		gitEnabledTextLabel = new JLabel(properties.getProperty("git_enable"));

		gitURLLabel = new JLabel("Git URL: ");
		gitURLTextLabel = new JLabel(properties.getProperty("git_url"));

		gitLocalPathLabel = new JLabel("Git Local Path: ");
		gitLocalPathTextLabel = new JLabel(properties.getProperty("git_local_path"));

		gitconfigPanel.setLayout(
				new MigLayout("insets 8, novisualpadding, hidemode 3, fill", "[]12[][grow]", "[][][][][][][][][grow]"));
		gitconfigPanel.add(gitEnabledLabel, "newline, right");
		gitconfigPanel.add(gitEnabledTextLabel, "wrap");
		gitconfigPanel.add(gitURLLabel, "newline, right");
		gitconfigPanel.add(gitURLTextLabel, "wrap");
		gitconfigPanel.add(gitLocalPathLabel, "newline, right");
		gitconfigPanel.add(gitLocalPathTextLabel, "wrap");
		gitconfigPanel.add(versionHistory, "newline, right");
		gitconfigPanel.add(hide, "wrap");
	}

	/* This Method fetches the properties from gitConfig.properties file */
	private Properties getGitProperties() throws ClientException {

		String configFileName = GitClientUtil.getGitConfigFileName();
		if (!(null == configFileName)) {
			try {
				properties = new Properties();
				properties = GitClientUtil.readGitProperties(GitClientUtil.getGitConfigFileName());
				if (properties.isEmpty()) {
					return properties = setDefaultValues();
				}
			} catch (Exception e) {
				logger.error(e);
				throw new ClientException("Unable to find Git settings file to load Channel History. :" + e);
			}
		} else {
			properties = setDefaultValues();
			logger.info(" Git Configuration file not found.");
		}
		return properties;
	}

	private List<CommitDetails> getFileHistory(Channel channel) {
		gitLocalPath = properties.getProperty("git_local_path");

		List<CommitDetails> history = null;
		try {
			String directory = gitLocalPath + "\\ChannelRepository";
			File file = new File(directory);
			if (!file.isDirectory()) {
				GitClientUtil.createDirectoryAndClone(properties);
			}
			File fXmlFile = new File(directory.toString());
			String[] pathnames = fXmlFile.list();

			for (String pathname : pathnames) {
				XmlElementMapper elementMapper = new XmlElementMapper();
				if (pathname.substring(pathname.length() - 4).equals(".xml")) {
					String[] array = pathname.split(".xml");
					if (array[0].equalsIgnoreCase(channel.getId())) {
						history = GitUtility.getFileCommitHistory(directory, pathname);
					}
				}
			}
			if (history == null) {
				logger.info("File commit history does not exist for channel : " + channel.getId());
			}
		} catch (Exception e) {
			parent.alertThrowable(parent, e, "An error occurred while loading file Commit History");

		}

		return history;
	}

	private String getVersionFileData(String commitId, String channelId) {
		gitLocalPath = properties.getProperty("git_local_path");
		String versionData = null;
		File repoDir = new File(gitLocalPath + "\\ChannelRepository\\.git");
		String filepath = channelId + ".xml";
		versionData = GitUtility.readVersionFile(commitId, repoDir, filepath);
		return versionData;
	}

	private List<String> getCompareFileData(List<String> commitIds, String channelId) {

		String compareData = null;
		if (commitIds.isEmpty()) {
			return null;
		}
		List<String> compareDataList = new ArrayList<String>();
		gitLocalPath = properties.getProperty("git_local_path");
		File repoDir = new File(gitLocalPath + "\\ChannelRepository\\.git");
		String filepath = channelId + ".xml";
		for (String commitId : commitIds) {
			compareData = GitUtility.readVersionFile(commitId, repoDir, filepath);
			compareDataList.add(compareData);
		}

		return compareDataList;

	}

	private void addButtonEvent(Channel channel) {
		String channelId = channel.getId();
		versionHistory.removeAll();
		versionHistory.addActionListener(new ActionListener() {
			boolean isDefaultCompareViewSelected = Boolean
					.valueOf((String) properties.get("git_Default_Compare_Enabled"));

			@Override
			public void actionPerformed(ActionEvent e) {
				count++;
				if (count == 1) {
					table.removeAll();
					scrollpane.removeAll();
					inputpanel.removeAll();
					panel.removeAll();
					versionHistoryPanel.setVisible(true);
					versionHistory.setEnabled(false);
					hide.setEnabled(true);

					revertButton = new JButton("Revert");
					compareButton = new JButton("Compare");
					compareButton.setEnabled(false);
					revertButton.setEnabled(false);
					panel = new JPanel();

					ArrayList<CommitDetails> commitList = (ArrayList<CommitDetails>) getFileHistory(channel);
					if (commitList != null && !commitList.isEmpty()) {
						columns = new String[] { "Select", "Commit Id", "Commit By", "Commit Message", "Commit Time" };

						ArrayList<ArrayList<Object>> outerList = new ArrayList<ArrayList<Object>>();
						ArrayList<Object> innerList = new ArrayList<>();
						for (CommitDetails CommitDetails : commitList) {
							innerList = new ArrayList<Object>();
							innerList.add(false);
							innerList.add(CommitDetails.getCommitName());
							innerList.add(CommitDetails.getCommitAuthor());
							innerList.add(CommitDetails.getCommitMessage());
							innerList.add(CommitDetails.getCommitTime());
							outerList.add(innerList);
						}
						data = new Object[outerList.size()][];

						int i = 0;
						for (ArrayList<Object> nestedList : outerList) {
							data[i++] = nestedList.toArray(new Object[nestedList.size()]);
						}

						model = new DefaultTableModel(data, columns) {
							@Override
							public Class getColumnClass(int column) {
								switch (column) {
								case 0:
									return Boolean.class;
								case 1:
									return String.class;
								case 2:
									return String.class;
								case 3:
									return String.class;
								default:
									return String.class;
								}
							}

							boolean[] canEdit = new boolean[] { true, false, false, false, false };

							public boolean isCellEditable(int rowIndex, int columnIndex) {
								return canEdit[columnIndex];
							}
						};
						table = new JTable(model);
						table.setPreferredScrollableViewportSize(new Dimension(800, 200));
						table.setFocusable(false);
						table.setRowHeight(23);
						TableColumnModel columnModel = table.getColumnModel();
						columnModel.getColumn(0).setPreferredWidth(55);
						columnModel.getColumn(0).setMaxWidth(55);
						columnModel.getColumn(1).setPreferredWidth(250);
						columnModel.getColumn(1).setMaxWidth(280);
						columnModel.getColumn(2).setPreferredWidth(120);
						columnModel.getColumn(2).setMaxWidth(150);
						columnModel.getColumn(3).setPreferredWidth(200);
						columnModel.getColumn(3).setMaxWidth(230);
						scrollpane = new JScrollPane(table);
						scrollpane.setBackground(UIConstants.BACKGROUND_COLOR);
						scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
						scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
						inputpanel = new JPanel();
						inputpanel.setLayout(new FlowLayout());
						inputpanel.add(compareButton);
						inputpanel.add(revertButton);
						panel = new JPanel();
						panel.setLayout(new MigLayout("novisualpadding, insets 0"));
						panel.add(scrollpane, "wrap");
						panel.add(inputpanel, "wrap");

						versionHistoryPanel
								.setLayout(new MigLayout("hidemode 3, novisualpadding, insets 8", "12[right][left]"));
						versionHistoryPanel.add(panel, "wrap");
					} else {
						MirthTextField text = new MirthTextField();
						text.setText(" No Version History Found ");
						text.setBorder(BorderFactory.createLineBorder(Color.white));
						versionHistoryPanel
								.setLayout(new MigLayout("hidemode 3, novisualpadding, insets 8", "12[right][left]"));
						versionHistoryPanel.add(text, "wrap");
					}
					table.addMouseListener(new MouseAdapter() {
						public void mouseClicked(MouseEvent me) {
							if (me.getClickCount() == 2) {
								viewFilePanel.setVisible(false);
								viewFilePanel.removeAll();
								JTable target = (JTable) me.getSource();
								int row = target.rowAtPoint(me.getPoint());
								int column = target.columnAtPoint(me.getPoint());
								String commitId = target.getModel().getValueAt(row, 1) + "";
								String data = getVersionFileData(commitId, channelId);
								// if default view selected then open default
								// editor else open external editor
								if (isDefaultCompareViewSelected) {
									createFileViewPannel(data);
								} else {
									try {
										openFileEditor(commitId, data);
									} catch (IOException | ClientException e) {
										logger.error(GitConstants.GIT_PLUGIN + "Error in opening file editor : " + e);
									}
								}
							}

							if (table != null) {
								int count = 0;
								for (int i = 0; i < table.getRowCount(); i++) {
									Boolean isChecked = (Boolean) table.getValueAt(i, 0);
									if (isChecked) {
										count++;
									}
								}
								if (count != 2) {
									compareButton.setEnabled(false);
									revertButton.setEnabled(false);
								}
								if (count == 1) {
									revertButton.setEnabled(true);
									compareButton.setEnabled(false);
								}
								if (count == 2) {
									revertButton.setEnabled(false);
									compareButton.setEnabled(true);
								}
							}
						}
					});

					compareButton.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							viewFilePanel.setVisible(false);
							viewFilePanel.removeAll();
							String selectedCommitId = null;
							List<String> commitIds = new ArrayList<>();

							for (int i = 0; i < table.getRowCount(); i++) {
								Boolean isChecked = Boolean.valueOf(table.getValueAt(i, 0).toString());
								if (isChecked) {
									if (commitIds.size() >= 2) {
										break;
									}
									selectedCommitId = table.getValueAt(i, 1) + "";
									commitIds.add(selectedCommitId);
								}
							}

							List<String> versionData = getCompareFileData(commitIds, channelId);
							if (isDefaultCompareViewSelected) {

								createComparePannel(versionData.get(0), versionData.get(1));

							} else {
								try {
									String comparatorPath = (String) properties.get("compare_tool_path");
									openCompareTool(commitIds, versionData, comparatorPath);
								} catch (IOException e1) {
									logger.error(GitConstants.GIT_PLUGIN + "Error in opening compare tool : " + e1);

								} catch (ClientException e1) {
									// TODO Auto-generated catch block
									logger.error(GitConstants.GIT_PLUGIN + "Error in opening compare tool : " + e1);
								}
							}
						}
					});

					revertButton.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							List<String> commitIds = new ArrayList<>();
							ChannelPanel ChannelPanel = new ChannelPanel();
							ChannelPanel.setSaveEnabled(false);
							String channelId = channel.getId();
							String selectedCommitId = null;

							for (int i = 0; i < table.getRowCount(); i++) {
								Boolean isChecked = Boolean.valueOf((boolean) table.getValueAt(i, 0));
								if (isChecked) {
									if (commitIds.size() >= 1) {
										break;
									}
									selectedCommitId = table.getValueAt(i, 1) + "";
									commitIds.add(selectedCommitId);
								}
							}
							String data = getVersionFileData(selectedCommitId, channelId);
							String newChannelName = getTagValue(data, "name");

							ChannelPanel.importChannel(data, true);
							if (newChannelName.equals(channel.getName())) {

								parent.setSaveEnabled(true);
							}
						}
					});
				}
			}
		});
	}

	public static String getTagValue(String xml, String tagName) {
		return xml.split("<" + tagName + ">")[1].split("</" + tagName + ">")[0];
	}

	private Properties setDefaultValues() {
		properties = new Properties();
		properties.setProperty("git_enable", "NA");
		properties.setProperty("git_url", "NA");
		properties.setProperty("git_local_path", "NA");
		properties.setProperty("compare_tool_path", "NA");
		properties.setProperty("git_config_file_Path", "NA");
		properties.setProperty("git_Default_Compare_Enabled", "NA");
		properties.setProperty("file_editor_path", "NA");

		return properties;
	}

	private void createFileViewPannel(String data) {
		JTextArea ta = new JTextArea();
		ta.setText(data);
		viewFilePanel.setVisible(true);
		viewFilePanel.setOpaque(true);
		JEditorPane editorPane = new JEditorPane();
		editorPane.setEditable(false);
		editorPane.setOpaque(true);
		editorPane.setEditorKit(new XMLEditorKit());
		editorPane.setText(ta.getText());
		JScrollPane scroller = new JScrollPane(editorPane);
		scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setMaximumSize(new Dimension(1200, 500));
		viewFilePanel.setLayout(new MigLayout("hidemode 3, novisualpadding, insets 0", "12[right][left]"));
		viewFilePanel.add(scroller, "wrap");
	}

	private void createComparePannel(String file1, String file2) {
		HashMap<String, List<String>> highlights1 = null;
		HashMap<String, List<String>> highlights2 = null;
		viewFilePanel.setVisible(true);
		viewFilePanel.setLayout(new MigLayout("hidemode 3, novisualpadding, insets 0", "12[right][left]"));
		JPanel innerPanel = new JPanel();
		innerPanel.setLayout(new GridLayout(1, 1));
		Border border = BorderFactory.createLineBorder(Color.BLACK);

		viewFilePanel.setOpaque(false);
		textArea1 = new JTextArea(30, 60);
		textArea1
				.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		textArea1.setText(file1);
		textArea1.setWrapStyleWord(true);
		textArea1.setEditable(false);
		textArea1.setFont(Font.getFont(Font.SANS_SERIF));

		textArea2 = new JTextArea(30, 60);
		textArea2.setText(file2);
		textArea2.setEditable(false);
		textArea2
				.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		textArea2.setWrapStyleWord(true);
		textArea2.setEditable(false);
		textArea2.setFont(Font.getFont(Font.SANS_SERIF));

		try {
			InputStream sourceStream = IOUtils.toInputStream(file1);
			InputStream targetStream = IOUtils.toInputStream(file2);
			// using BufferedReader for improved performance
			BufferedReader source = new BufferedReader(new InputStreamReader(sourceStream));
			BufferedReader target = new BufferedReader(new InputStreamReader(targetStream));

			// configuring XMLUnit to ignore white spaces
			XMLUnit.setIgnoreWhitespace(true);

			// comparing two XML using XMLUnit in Java
			List<org.custommonkey.xmlunit.Difference> differences = GitClientUtil.compareXML(source, target);
			List<HashMap<String, List<String>>> highlightList = GitClientUtil.getFileDifferences(differences);

			highlights1 = highlightList.get(0);
			highlights2 = highlightList.get(1);

			GitClientUtil.highlightDifferences(highlights1, textArea1);
			GitClientUtil.highlightDifferences(highlights2, textArea2);

		} catch (SAXException | IOException e) {
			logger.error(GitConstants.GIT_PLUGIN + "Error while highlighting the differences in the files : " + e);
		}

		innerPanel.add(textArea1);
		innerPanel.add(textArea2);

		JScrollPane scroller = new JScrollPane(innerPanel);
		scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroller.setMaximumSize(new Dimension(1400, 500));
		viewFilePanel.add(scroller, "wrap");
	}

	private void openFileEditor(String commitId, String fileData) throws IOException, ClientException {

		String editorPath = String.valueOf(properties.get("file_editor_path"));

		// create a directory to save the given 2 versions of channel for
		// comparison
		String filePath = String.valueOf(properties.get("git_config_file_Path")) + "ComparisonFiles\\";
		File directory = new File(filePath);
		if (!directory.exists()) {
			directory.mkdir();
		}

		try {

			// create and write file
			String fileName = filePath + commitId + ".xml";
			File file = new File(String.valueOf(fileName));
			if (!file.exists()) {
				String data = fileData;
				FileWriter fileWriter = new FileWriter(fileName);
				fileWriter.write(data);
				fileWriter.close();
				Files.write(Paths.get(fileName), data.getBytes(), StandardOpenOption.CREATE);
			}

			// create a process to run the editor tool with given files
			String[] commands = { editorPath, fileName };
			ProcessBuilder builder = new ProcessBuilder(commands);
			Process pr = builder.start();

		} catch (IOException e) {
			logger.error(GitConstants.GIT_PLUGIN + "Error while starting a editor tool : " + e);
			throw new IOException(e);
		}
	}

	private void openCompareTool(List<String> commitIds, List<String> fileData, String comparatorPath)
			throws IOException, ClientException {
		List<String> fileNames = new ArrayList<String>();

		// create a directory to save the given 2 versions of channel for
		// comparison
		String filePath = GitClientUtil.getGitConfigFilePath() + "\\ComparisonFiles\\";
		File directory = new File(filePath);
		if (!directory.exists()) {
			directory.mkdir();
		}

		try {
			for (int i = 0; i < commitIds.size(); i++) {
				// create and write file
				String fileName = filePath + commitIds.get(i) + ".xml";
				File file = new File(String.valueOf(fileName));
				fileNames.add(fileName);
				if (!file.exists()) {
					String data = fileData.get(i);
					FileWriter fileWriter = new FileWriter(fileName);
					fileWriter.write(data);
					fileWriter.close();
					Files.write(Paths.get(fileName), data.getBytes(), StandardOpenOption.CREATE);
				}
			}

			// create a process to run the Comparator tool with given files
			String[] commands = { comparatorPath, fileNames.get(0), fileNames.get(1) };
			ProcessBuilder builder = new ProcessBuilder(commands);
			Process pr = builder.start();

		} catch (IOException e) {
			logger.error(GitConstants.GIT_PLUGIN + "Error while starting a comparator tool : " + e);
			throw new IOException(e);
		}
	}

	private void initComponents() {
		setBackground(UIConstants.BACKGROUND_COLOR);

		gitconfigPanel = new JPanel();
		gitconfigPanel.setBackground(UIConstants.BACKGROUND_COLOR);
		gitconfigPanel.setBorder(BorderFactory.createTitledBorder("Git Configuration"));

		versionHistoryPanel = new JPanel();
		versionHistoryPanel.setBackground(UIConstants.BACKGROUND_COLOR);
		versionHistoryPanel.setBorder(BorderFactory.createTitledBorder("Version History"));
		versionHistoryPanel.setVisible(false);

		viewFilePanel = new JPanel();
		viewFilePanel.setBackground(UIConstants.BACKGROUND_COLOR);
		viewFilePanel.setLayout(new BoxLayout(viewFilePanel, BoxLayout.Y_AXIS));
		viewFilePanel.setVisible(false);

		scrollpane = new JScrollPane();
		inputpanel = new JPanel();
		panel = new JPanel();
		table = new JTable();

		hide.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				count = 0;
				hide.setEnabled(false);
				compareButton.removeAll();
				revertButton.removeAll();
				versionHistory.setEnabled(true);
				versionHistoryPanel.setVisible(false);
				viewFilePanel.setVisible(false);
				versionHistoryPanel.removeAll();
				viewFilePanel.removeAll();
				scrollpane.removeAll();
				inputpanel.removeAll();
				panel.removeAll();
				table.removeAll();
			}
		});
	}

	private void initLayout() {
		setLayout(new MigLayout("hidemode 3, novisualpadding, insets 12", "[grow]"));
		add(gitconfigPanel, "grow, sx, wrap");
		add(versionHistoryPanel, "grow, sx, wrap");
		add(viewFilePanel, "grow, sx, wrap");
	}

	private JPanel gitconfigPanel;
	private JLabel gitURLLabel;
	private JLabel gitURLTextLabel;
	private JLabel gitEnabledLabel;
	private JLabel gitEnabledTextLabel;
	private JLabel gitLocalPathLabel;
	private JLabel gitLocalPathTextLabel;
	private JPanel versionHistoryPanel;
	private JButton versionHistory = new JButton("Version History");
	private JTable table;
	private JScrollPane scrollpane;

	private JButton hide = new JButton("Hide");
	private JPanel inputpanel;

	private JButton revertButton;
	private JButton deleteButton;
	private JButton compareButton;

	private JPanel viewFilePanel;
	private JPanel panel;
	private JTextArea textArea1;
	private JTextArea textArea2;

}
