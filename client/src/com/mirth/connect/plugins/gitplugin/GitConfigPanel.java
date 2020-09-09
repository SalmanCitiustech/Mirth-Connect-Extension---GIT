package com.mirth.connect.plugins.gitplugin;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthRadioButton;
import com.mirth.connect.client.ui.components.MirthTextField;
import com.mirth.connect.model.converters.ObjectXMLSerializer;
import com.mirth.connect.plugins.SettingsPanelPlugin;

import net.miginfocom.swing.MigLayout;

public class GitConfigPanel extends AbstractSettingsPanel implements ActionListener {

	private static final long serialVersionUID = 3055102412930809343L;
	private SettingsPanelPlugin plugin = null;
	private ObjectXMLSerializer serializer = ObjectXMLSerializer.getInstance();
	private AtomicBoolean refreshing = new AtomicBoolean(false);
	private Frame parent;
	private static Logger logger = Logger.getLogger(GitConfigPanel.class);
	private String gitConfigFileName = null;

	private static Properties properties = null;

	public GitConfigPanel(String tabName, SettingsPanelPlugin plugin) {
		super(tabName);

		this.plugin = plugin;
		this.parent = PlatformUI.MIRTH_FRAME;
		boolean isFileCreated = GitClientUtil.createConfigFile();
		gitConfigFileName = GitClientUtil.getGitConfigFileName();
		initComponents();
		initLayout();
	}

	@Override
	public void doRefresh() {
		File gitConfigFile = new File(GitClientUtil.getGitConfigFileName());
		boolean exists = gitConfigFile.exists();
		if(!exists) {
			noEnabledRadio.setSelected(true);
			disableFields();
		}
		yesEnabledRadio.addActionListener(this);
		noEnabledRadio.addActionListener(this);
		prodEnvYesRadio.addActionListener(this);
		prodEnvNoRadio.addActionListener(this);
		setPropertiestoUI();
	}

	@Override
	public boolean doSave() {
		final String workingId = getFrame().startWorking("Saving " + getTabName() + " properties...");

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			public Void doInBackground() {

				boolean isValid = validateGitConfigInput();
				if (!isValid) {
					return null;
				}
				boolean isPathGiven = true;
				String warningMessage = "";
				if (!defaultCompareYesRadio.isSelected()) {
					if (comparatorDirPathText.getText().isEmpty() && fileEditorDirPathText.getText().isEmpty()) {
						isPathGiven = false;
						warningMessage = "Please provide Compare tool path and file editor path";
					} else if (comparatorDirPathText.getText().isEmpty()) {
						warningMessage = "Please provide Compare tool path.";
						isPathGiven = false;
					} else if (fileEditorDirPathText.getText().isEmpty()) {
						warningMessage = "Please provide file editor path.";
						isPathGiven = false;
					}

					if (!isPathGiven) {
						getFrame().alertWarning(getFrame(), warningMessage);
						setSaveEnabled(true);
						return null;
					}

				}

				if (isValid && isPathGiven) {
					try {
						setPropertiestoServer();

					} catch (Exception e) {
						getFrame().alertThrowable(getFrame(), e);
					}
					return null;
				}
				return null;
			}

			@Override
			public void done() {
				setSaveEnabled(false);
				getFrame().stopWorking(workingId);
			}
		};

		worker.execute();

		return true;

	}

	public Properties setPropertiestoServer() {

		try (OutputStream output = new FileOutputStream(GitClientUtil.getGitConfigFileName())) {
			properties = new Properties();

			String enabled = "true";
			if (noEnabledRadio.isSelected()) {
				enabled = "false";
			}

			String prodEnvSelectedValue = "true";
			if (prodEnvNoRadio.isSelected()) {
				prodEnvSelectedValue = "false";
			}

			properties.setProperty("git_enable", enabled);
			properties.setProperty("git_url", urlText.getText());
			properties.setProperty("git_local_path", gitLocalDirPathText.getText());
			properties.setProperty("compare_tool_path", comparatorDirPathText.getText());
			properties.setProperty("git_Default_Compare_Enabled", String.valueOf(defaultCompareYesRadio.isSelected()));
			properties.setProperty("file_editor_path", fileEditorDirPathText.getText());
			properties.setProperty("prod_environment", prodEnvSelectedValue);

			// save properties to project root folder
			properties.store(output, null);

		} catch (IOException io) {
			logger.error(GitConstants.GIT_PLUGIN + io);
		}

		return properties;
	}

	private void setPropertiestoUI() {
		try {
			properties = GitClientUtil.readGitProperties(gitConfigFileName);
			if (!properties.isEmpty()) {
				if (Boolean.parseBoolean(properties.getProperty("git_enable"))) {
					yesEnabledRadio.setSelected(true);
					enableFields();
				} else {
					noEnabledRadio.setSelected(true);
					disableFields();
				}
				urlText.setText(properties.getProperty("git_url"));
				gitLocalDirPathText.setText(properties.getProperty("git_local_path"));
				comparatorDirPathText.setText(properties.getProperty("compare_tool_path"));
				if (Boolean.parseBoolean(properties.getProperty("git_Default_Compare_Enabled"))) {
					defaultCompareYesRadio.setSelected(true);
					defaultCompareYesRadioSelected();
				} else {
					defaultCompareNoRadio.setSelected(true);
					defaultComapreNoRadioSelected();
					fileEditorDirPathText.setText(properties.getProperty("file_editor_path"));
				}

				boolean isProdEnvSelected = Boolean.parseBoolean(properties.getProperty("prod_environment"));
				if (isProdEnvSelected) {
					prodEnvYesRadio.setSelected(true);
					prodEnvSelectedInfo.setVisible(true);
					prodEnvSelectedInfoLabel.setVisible(true);
				} else {
					prodEnvNoRadio.setSelected(true);
					prodEnvSelectedInfo.setVisible(false);
					prodEnvSelectedInfoLabel.setVisible(false);
				}
			} else {
				noEnabledRadio.setSelected(true);
				urlText.setText("");
				gitLocalDirPathText.setText("");
				comparatorDirPathText.setText("");
				defaultCompareYesRadio.setSelected(true);
				fileEditorDirPathText.setText("");
				prodEnvYesRadio.setSelected(true);
			}
		} catch (Exception e) {
			logger.error(GitConstants.GIT_PLUGIN + "Error in reading Git Configuration File");
		}
	}

	public boolean validateGitConfigInput() {

		boolean isValidInput = true;
		String warningMessage = null;
		ValidationUtil verifier = new ValidationUtil();
		boolean isvalidUrl = verifier.verify(urlText);
		if (!isvalidUrl) {
			warningMessage = "Please provide valid GitHub url.";
		}
		if (!isvalidUrl) {
			getFrame().alertWarning(getFrame(), warningMessage);
			isValidInput = false;
		}
		return isValidInput;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == noEnabledRadio) {
			disableFields();
		} else if (e.getSource() == yesEnabledRadio) {
			enableFields();
			setSaveEnabled(true);
		} else if (e.getSource() == browseDirButton) {
			JFileChooser j = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
			j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			// invoke the showsSaveDialog function to show the save dialog
			int r = j.showOpenDialog(null);

			if (r == JFileChooser.APPROVE_OPTION) {
				gitLocalDirPathText.setText(j.getSelectedFile().getAbsolutePath());
				setSaveEnabled(true);
			}

		} else if (e.getSource() == browseCompareToolButton) {
			JFileChooser j = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
			int r = j.showOpenDialog(null);
			j.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if (r == JFileChooser.APPROVE_OPTION) {
				String comparatorPath = j.getSelectedFile().getAbsolutePath();

				if (StringUtils.substringAfterLast(comparatorPath, ".").equalsIgnoreCase("exe")) {
					comparatorDirPathText.setText(comparatorPath);
					setSaveEnabled(true);
				} else {
					parent.alertWarning(this, "Please provide appropriate executable file for file comparison.");
				}

			}
		} else if (e.getSource() == fileEditorButton) {
			JFileChooser j = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
			j.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int r = j.showOpenDialog(null);

			if (r == JFileChooser.APPROVE_OPTION) {
				fileEditorDirPathText.setText(j.getSelectedFile().getAbsolutePath());
				setSaveEnabled(true);
			}

		} else if (e.getSource() == prodEnvYesRadio) {
			prodEnvSelectedInfo.setVisible(true);
			prodEnvSelectedInfoLabel.setVisible(true);
			prodEnvSelectedInfo.setText(GitConstants.prodEnvMessage);
		} else if (e.getSource() == prodEnvNoRadio) {
			prodEnvSelectedInfo.setVisible(false);
			prodEnvSelectedInfoLabel.setVisible(false);
		}

	}

	private void enableFields() {
		urlText.setEnabled(true);
		gitLocalDirPathText.setEnabled(true);
		defaultCompareYesRadio.setEnabled(true);
		defaultCompareNoRadio.setEnabled(true);
		browseGitConfigDirButton.setEnabled(true);
		browseDirButton.setEnabled(true);
		prodEnvYesRadio.setEnabled(true);
		prodEnvNoRadio.setEnabled(true);
		prodEnvSelectedInfo.setEnabled(true);
		prodEnvSelectedInfoLabel.setEnabled(true);
	}

	private void disableFields() {
		urlText.setEnabled(false);
		gitLocalDirPathText.setEnabled(false);
		comparatorDirPathText.setEnabled(false);
		comparatorDirPathText.setEnabled(false);
		fileEditorDirPathText.setEnabled(false);
		defaultCompareYesRadio.setEnabled(false);
		defaultCompareNoRadio.setEnabled(false);
		browseCompareToolButton.setEnabled(false);
		browseGitConfigDirButton.setEnabled(false);
		browseDirButton.setEnabled(false);
		fileEditorButton.setEnabled(false);
		prodEnvYesRadio.setEnabled(false);
		prodEnvNoRadio.setEnabled(false);
		prodEnvSelectedInfo.setEnabled(false);
		prodEnvSelectedInfoLabel.setEnabled(false);
	}

	private void initComponents() {
		setBackground(UIConstants.BACKGROUND_COLOR);

		gitConfigPanel = new JPanel();
		gitConfigPanel.setBackground(UIConstants.BACKGROUND_COLOR);
		gitConfigPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)), "Git Configurations",
				TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Tahoma", 1, 11)));

		enabledLabel = new JLabel(GitConstants.enableLabel);

		yesEnabledRadio = new MirthRadioButton(GitConstants.yesLabel);
		yesEnabledRadio.setFocusable(false);
		yesEnabledRadio.setBackground(Color.white);

		noEnabledRadio = new MirthRadioButton(GitConstants.noLabel);
		noEnabledRadio.setFocusable(false);
		noEnabledRadio.setBackground(Color.white);
		noEnabledRadio.setSelected(true);

		enablegrp = new ButtonGroup();
		enablegrp.add(yesEnabledRadio);
		enablegrp.add(noEnabledRadio);

		urlLabel = new JLabel(GitConstants.url_Label);
		urlText = new MirthTextField();
		urlText.setName("urlText");
		urlText.setSize(new Dimension(250, 20));

		gitLocalDirLabel = new JLabel(GitConstants.git_Local_Dir_Label);
		gitLocalDirPathText = new MirthTextField();
		gitLocalDirPathText.setName("gitLocalDirPathText");
		gitLocalDirPathText.setToolTipText("Local directory path on Client's system used as Git repository");
		gitLocalDirPathText.setEditable(false);
		browseDirButton.setText(GitConstants.browseLabel);
		browseDirButton.addActionListener(this);

		comparatorDirLabel = new JLabel(GitConstants.comparator_Dir_Label);
		comparatorDirPathText = new MirthTextField();
		comparatorDirPathText.setName("comparatorDirPathText");
		comparatorDirPathText
				.setToolTipText("File Path which specifies the location of the compare tool executable file.");
		comparatorDirPathText.setEditable(false);
		browseCompareToolButton.setText(GitConstants.browseLabel);
		browseCompareToolButton.addActionListener(this);

		browseGitConfigDirButton.setText(GitConstants.browseLabel);
		browseGitConfigDirButton.addActionListener(this);

		fileEditorDirLabel = new JLabel(GitConstants.file_editor_Dir_Label);
		fileEditorDirPathText = new MirthTextField();
		fileEditorDirPathText.setName("fileEditorDirPathText");
		fileEditorDirPathText
				.setToolTipText("File Path which specifies the location of the editor tool executable file.");
		fileEditorDirPathText.setEditable(false);
		fileEditorButton.setText(GitConstants.browseLabel);
		fileEditorButton.addActionListener(this);

		defaultCompareButtonGroup = new ButtonGroup();
		defaultCompareYesRadio = new MirthRadioButton(GitConstants.default_Compare_Yes_Radio);
		defaultCompareYesRadio.setBackground(getBackground());
		defaultCompareYesRadio.setSelected(true);
		defaultCompareYesRadio.setToolTipText("Allows to setup compare tool to compare two versions of a channel.");
		defaultCompareYesRadio.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				defaultCompareYesRadioSelected();
			}
		});
		defaultCompareButtonGroup.add(defaultCompareYesRadio);

		defaultCompareNoRadio = new MirthRadioButton(GitConstants.default_Compare_No_Radio);
		defaultCompareNoRadio.setBackground(getBackground());
		defaultCompareNoRadio.setToolTipText("Uses default comapare window to compare two versions of channel.");
		defaultCompareNoRadio.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				defaultComapreNoRadioSelected();
			}
		});
		defaultCompareButtonGroup.add(defaultCompareNoRadio);

		isDefaultCompareEnabled = new JCheckBox();
		DefaultCompareLabel = new JLabel(GitConstants.default_Compare_Label);

		isDefaultCompareEnabled.setRolloverEnabled(false);
		isDefaultCompareEnabled.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				setSaveEnabled(true);
				validateGitConfigInput();
			}
		});

		if (defaultCompareYesRadio.isSelected()) {
			comparatorDirPathText.setEnabled(false);
			comparatorDirPathText.setEditable(false);
			browseCompareToolButton.setEnabled(false);
		}

		prodEnvLabel = new JLabel(GitConstants.prodEnvLabel);

		prodEnvYesRadio = new MirthRadioButton(GitConstants.yesLabel);
		prodEnvYesRadio.setFocusable(false);
		prodEnvYesRadio.setBackground(Color.white);
		prodEnvYesRadio.setSelected(true);

		prodEnvNoRadio = new MirthRadioButton(GitConstants.noLabel);
		prodEnvNoRadio.setFocusable(false);
		prodEnvNoRadio.setBackground(Color.white);

		envButtonGroup = new ButtonGroup();
		envButtonGroup.add(prodEnvYesRadio);
		envButtonGroup.add(prodEnvNoRadio);

		prodEnvSelectedInfoLabel = new JLabel("Info: ");
		prodEnvSelectedInfoLabel.setVisible(true);

		prodEnvSelectedInfo = new JLabel();
		prodEnvSelectedInfo.setVisible(true);
		prodEnvSelectedInfo.setText(GitConstants.prodEnvMessage);

		if (noEnabledRadio.isSelected()) {
			disableFields();
		}
	}

	private void defaultComapreNoRadioSelected() {
		comparatorDirPathText.setEnabled(true);
		comparatorDirPathText.setEditable(false);
		browseCompareToolButton.setEnabled(true);
		fileEditorDirPathText.setEnabled(true);
		fileEditorDirPathText.setEditable(false);
		fileEditorButton.setEnabled(true);

	}

	private void defaultCompareYesRadioSelected() {
		comparatorDirPathText.setEnabled(false);
		browseCompareToolButton.setEnabled(false);
		fileEditorDirPathText.setEnabled(false);
		fileEditorButton.setEnabled(false);
	}

	private void initLayout() {
		setSaveEnabled(false);
		setLayout(new MigLayout("hidemode 3, novisualpadding,fill, insets 12", "[]12[][grow]"));
		gitConfigPanel.setLayout(new MigLayout("insets 12, novisualpadding, hidemode 3, fill, gap 6", "[]12[][grow]",
				"[][][][][][][][][][][grow]"));
		gitConfigPanel.add(enabledLabel, " right");
		gitConfigPanel.add(yesEnabledRadio, "split 2");
		gitConfigPanel.add(noEnabledRadio);
		enablegrp.add(yesEnabledRadio);
		enablegrp.add(noEnabledRadio);

		gitConfigPanel.add(urlLabel, "newline, right");
		gitConfigPanel.add(urlText, "w 225!, span");

		gitConfigPanel.add(gitLocalDirLabel, "newline , right");
		gitConfigPanel.add(gitLocalDirPathText, "w 225!");
		gitConfigPanel.add(browseDirButton);

		gitConfigPanel.add(DefaultCompareLabel, "newline , right");
		gitConfigPanel.add(defaultCompareYesRadio, "split 2");
		gitConfigPanel.add(defaultCompareNoRadio);

		gitConfigPanel.add(comparatorDirLabel, "newline, right");
		gitConfigPanel.add(comparatorDirPathText, "w 225!");
		gitConfigPanel.add(browseCompareToolButton);

		gitConfigPanel.add(fileEditorDirLabel, "newline, right");
		gitConfigPanel.add(fileEditorDirPathText, "w 225!");
		gitConfigPanel.add(fileEditorButton);

		gitConfigPanel.add(prodEnvLabel, "newline, right");
		gitConfigPanel.add(prodEnvYesRadio, "split 2");
		gitConfigPanel.add(prodEnvNoRadio, "wrap");
		envButtonGroup.add(prodEnvYesRadio);
		envButtonGroup.add(prodEnvNoRadio);

		gitConfigPanel.add(prodEnvSelectedInfoLabel, "newline, right");
		gitConfigPanel.add(prodEnvSelectedInfo);

		add(gitConfigPanel, " grow, wrap");
	}

	private JPanel gitConfigPanel;
	private JLabel enabledLabel;
	private MirthRadioButton yesEnabledRadio;
	private MirthRadioButton noEnabledRadio;
	private ButtonGroup enablegrp;
	private JLabel urlLabel;
	private MirthTextField urlText;
	private JLabel gitLocalDirLabel;
	private MirthTextField gitLocalDirPathText;
	private JButton browseDirButton = new javax.swing.JButton();
	private JLabel comparatorDirLabel;
	private MirthTextField comparatorDirPathText;
	private JButton browseCompareToolButton = new javax.swing.JButton();
	private JButton browseGitConfigDirButton = new javax.swing.JButton();
	private JCheckBox isDefaultCompareEnabled;
	private JLabel DefaultCompareLabel;
	private ButtonGroup defaultCompareButtonGroup;
	private MirthRadioButton defaultCompareYesRadio;
	private MirthRadioButton defaultCompareNoRadio;

	private JLabel fileEditorDirLabel;
	private MirthTextField fileEditorDirPathText;
	private JButton fileEditorButton = new javax.swing.JButton();

	private MirthRadioButton prodEnvYesRadio;
	private MirthRadioButton prodEnvNoRadio;
	private JLabel prodEnvSelectedInfo;
	private JLabel prodEnvSelectedInfoLabel;
	private ButtonGroup envButtonGroup;
	private JLabel prodEnvLabel;
}
