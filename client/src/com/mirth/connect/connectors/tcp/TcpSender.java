/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.connectors.tcp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.ui.ConnectorTypeDecoration;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.LoadedExtensions;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthFieldConstraints;
import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.client.ui.panels.connectors.ResponseHandler;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.model.Connector.Mode;
import com.mirth.connect.model.transmission.TransmissionModeProperties;
import com.mirth.connect.plugins.BasicModeClientProvider;
import com.mirth.connect.plugins.TransmissionModeClientProvider;
import com.mirth.connect.plugins.TransmissionModePlugin;
import com.mirth.connect.util.ConnectionTestResponse;

public class TcpSender extends ConnectorSettingsPanel implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass());
    private Frame parent;
    private TransmissionModeClientProvider defaultProvider;
    private TransmissionModeClientProvider transmissionModeProvider;
    private JComponent settingsPlaceHolder;
    private String selectedMode;
    private boolean modeLock = false;

    public TcpSender() {
        this.parent = PlatformUI.MIRTH_FRAME;
        initComponents();
        sendTimeoutField.setDocument(new MirthFieldConstraints(0, false, false, true));
        bufferSizeField.setDocument(new MirthFieldConstraints(0, false, false, true));
        responseTimeoutField.setDocument(new MirthFieldConstraints(0, false, false, true));

        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement("Basic TCP");
        selectedMode = "Basic TCP";

        for (String pluginPointName : LoadedExtensions.getInstance().getTransmissionModePlugins().keySet()) {
            model.addElement(pluginPointName);
            if (pluginPointName.equals("MLLP")) {
                defaultProvider = LoadedExtensions.getInstance().getTransmissionModePlugins().get(pluginPointName).createProvider();
            }
        }

        transmissionModeComboBox.setModel(model);

        settingsPlaceHolder = settingsPlaceHolderLabel;

        parent.setupCharsetEncodingForConnector(charsetEncodingCombobox);
    }

    @Override
    public String getConnectorName() {
        return new TcpDispatcherProperties().getName();
    }

    @Override
    public ConnectorProperties getProperties() {
        TcpDispatcherProperties properties = new TcpDispatcherProperties();

        if (transmissionModeProvider != null) {
            properties.setTransmissionModeProperties((TransmissionModeProperties) transmissionModeProvider.getProperties());
        }
        properties.setRemoteAddress(remoteAddressField.getText());
        properties.setRemotePort(remotePortField.getText());
        properties.setOverrideLocalBinding(overrideLocalBindingYesRadio.isSelected());
        properties.setLocalAddress(localAddressField.getText());
        properties.setLocalPort(localPortField.getText());
        properties.setSendTimeout(sendTimeoutField.getText());
        properties.setBufferSize(bufferSizeField.getText());
        properties.setKeepConnectionOpen(keepConnectionOpenYesRadio.isSelected());
        properties.setCheckRemoteHost(checkRemoteHostYesRadio.isSelected());
        properties.setResponseTimeout(responseTimeoutField.getText());
        properties.setIgnoreResponse(ignoreResponseCheckBox.isSelected());
        properties.setQueueOnResponseTimeout(queueOnResponseTimeoutYesRadio.isSelected());
        properties.setDataTypeBinary(dataTypeBinaryRadio.isSelected());
        properties.setCharsetEncoding(parent.getSelectedEncodingForConnector(charsetEncodingCombobox));
        properties.setTemplate(templateTextArea.getText());

        logger.debug("getProperties: properties=" + properties);

        return properties;
    }

    @Override
    public void setProperties(ConnectorProperties properties) {
        logger.debug("setProperties: properties=" + properties);
        TcpDispatcherProperties props = (TcpDispatcherProperties) properties;

        TransmissionModeProperties modeProps = props.getTransmissionModeProperties();
        String name = "Basic TCP";
        if (modeProps != null && LoadedExtensions.getInstance().getTransmissionModePlugins().containsKey(modeProps.getPluginPointName())) {
            name = modeProps.getPluginPointName();
        }

        modeLock = true;
        transmissionModeComboBox.setSelectedItem(name);
        transmissionModeComboBoxActionPerformed(null);
        modeLock = false;
        selectedMode = name;
        if (transmissionModeProvider != null) {
            transmissionModeProvider.setProperties(modeProps);
        }

        remoteAddressField.setText(props.getRemoteAddress());
        remotePortField.setText(props.getRemotePort());

        if (props.isOverrideLocalBinding()) {
            overrideLocalBindingYesRadio.setSelected(true);
            overrideLocalBindingYesRadioActionPerformed(null);
        } else {
            overrideLocalBindingNoRadio.setSelected(true);
            overrideLocalBindingNoRadioActionPerformed(null);
        }

        localAddressField.setText(props.getLocalAddress());
        localPortField.setText(props.getLocalPort());
        sendTimeoutField.setText(props.getSendTimeout());
        bufferSizeField.setText(props.getBufferSize());

        if (props.isKeepConnectionOpen()) {
            keepConnectionOpenYesRadio.setSelected(true);
            keepConnectionOpenYesRadioActionPerformed(null);
        } else {
            keepConnectionOpenNoRadio.setSelected(true);
            keepConnectionOpenNoRadioActionPerformed(null);
        }

        if (props.isCheckRemoteHost()) {
            checkRemoteHostYesRadio.setSelected(true);
        } else {
            checkRemoteHostNoRadio.setSelected(true);
        }

        responseTimeoutField.setText(String.valueOf(props.getResponseTimeout()));

        ignoreResponseCheckBox.setSelected(props.isIgnoreResponse());
        ignoreResponseCheckBoxActionPerformed(null);

        if (props.isQueueOnResponseTimeout()) {
            queueOnResponseTimeoutYesRadio.setSelected(true);
        } else {
            queueOnResponseTimeoutNoRadio.setSelected(true);
        }

        if (props.isDataTypeBinary()) {
            dataTypeBinaryRadio.setSelected(true);
            dataTypeBinaryRadioActionPerformed(null);
        } else {
            dataTypeASCIIRadio.setSelected(true);
            dataTypeASCIIRadioActionPerformed(null);
        }

        parent.setPreviousSelectedEncodingForConnector(charsetEncodingCombobox, props.getCharsetEncoding());

        templateTextArea.setText(props.getTemplate());
    }

    @Override
    public ConnectorProperties getDefaults() {
        TcpDispatcherProperties props = new TcpDispatcherProperties();
        if (defaultProvider != null) {
            props.setTransmissionModeProperties(defaultProvider.getDefaultProperties());
        }
        return props;
    }

    @Override
    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        logger.debug("checkProperties: properties=" + properties);
        TcpDispatcherProperties props = (TcpDispatcherProperties) properties;

        boolean valid = true;

        if (transmissionModeProvider != null) {
            if (!transmissionModeProvider.checkProperties(transmissionModeProvider.getProperties(), highlight)) {
                valid = false;
            }
        }
        if (props.getRemoteAddress().length() <= 3) {
            valid = false;
            if (highlight) {
                remoteAddressField.setBackground(UIConstants.INVALID_COLOR);
            }
        }
        if (props.getRemotePort().length() == 0) {
            valid = false;
            if (highlight) {
                remotePortField.setBackground(UIConstants.INVALID_COLOR);
            }
        }
        if (props.isOverrideLocalBinding()) {
            if (props.getLocalAddress().length() <= 3) {
                valid = false;
                if (highlight) {
                    localAddressField.setBackground(UIConstants.INVALID_COLOR);
                }
            }
            if (props.getLocalPort().length() == 0) {
                valid = false;
                if (highlight) {
                    localPortField.setBackground(UIConstants.INVALID_COLOR);
                }
            }
        }
        if (props.isKeepConnectionOpen()) {
            if (props.getSendTimeout().length() == 0) {
                valid = false;
                if (highlight) {
                    sendTimeoutField.setBackground(UIConstants.INVALID_COLOR);
                }
            }
        }
        if (props.getBufferSize().length() == 0) {
            valid = false;
            if (highlight) {
                bufferSizeField.setBackground(UIConstants.INVALID_COLOR);
            }
        }
        if (props.getResponseTimeout().length() == 0) {
            valid = false;
            if (highlight) {
                responseTimeoutField.setBackground(UIConstants.INVALID_COLOR);
            }
        }
        if (props.getTemplate().length() == 0) {
            valid = false;
            if (highlight) {
                templateTextArea.setBackground(UIConstants.INVALID_COLOR);
            }
        }

        return valid;
    }

    @Override
    public void resetInvalidProperties() {
        if (transmissionModeProvider != null) {
            transmissionModeProvider.resetInvalidProperties();
        }
        remoteAddressField.setBackground(null);
        decorateConnectorType();
        remotePortField.setBackground(null);
        localAddressField.setBackground(null);
        localPortField.setBackground(null);
        sendTimeoutField.setBackground(null);
        bufferSizeField.setBackground(null);
        responseTimeoutField.setBackground(null);
        templateTextArea.setBackground(null);
    }

    @Override
    public ConnectorTypeDecoration getConnectorTypeDecoration() {
        return new ConnectorTypeDecoration(Mode.DESTINATION);
    }

    @Override
    public void doLocalDecoration(ConnectorTypeDecoration connectorTypeDecoration) {
        if (connectorTypeDecoration != null) {
            remoteAddressField.setIcon(connectorTypeDecoration.getIcon());
            remoteAddressField.setAlternateToolTipText(connectorTypeDecoration.getIconToolTipText());
            remoteAddressField.setIconPopupMenuComponent(connectorTypeDecoration.getIconPopupComponent());
            remoteAddressField.setBackground(connectorTypeDecoration.getHighlightColor());
        }
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource().equals(transmissionModeProvider)) {
            if (evt.getActionCommand().equals(TransmissionModeClientProvider.CHANGE_SAMPLE_LABEL_COMMAND)) {
                sampleLabel.setText(transmissionModeProvider.getSampleLabel());
            } else if (evt.getActionCommand().equals(TransmissionModeClientProvider.CHANGE_SAMPLE_VALUE_COMMAND)) {
                sampleValue.setText(transmissionModeProvider.getSampleValue());
            }
        }
    }

    // @formatter:off
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        keepConnectionOpenGroup = new javax.swing.ButtonGroup();
        dataTypeButtonGroup = new javax.swing.ButtonGroup();
        overrideLocalBindingButtonGroup = new javax.swing.ButtonGroup();
        queueOnResponseTimeoutButtonGroup = new javax.swing.ButtonGroup();
        checkRemoteHostButtonGroup = new javax.swing.ButtonGroup();
        keepConnectionOpenLabel = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        sendTimeoutLabel = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        remotePortField = new com.mirth.connect.client.ui.components.MirthTextField();
        sendTimeoutField = new com.mirth.connect.client.ui.components.MirthTextField();
        bufferSizeField = new com.mirth.connect.client.ui.components.MirthTextField();
        keepConnectionOpenYesRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        keepConnectionOpenNoRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        remoteAddressField = new com.mirth.connect.client.ui.components.MirthIconTextField();
        responseTimeoutField = new com.mirth.connect.client.ui.components.MirthTextField();
        responseTimeoutLabel = new javax.swing.JLabel();
        charsetEncodingCombobox = new com.mirth.connect.client.ui.components.MirthComboBox();
        encodingLabel = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        templateTextArea = new com.mirth.connect.client.ui.components.MirthSyntaxTextArea();
        ignoreResponseCheckBox = new com.mirth.connect.client.ui.components.MirthCheckBox();
        dataTypeASCIIRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        dataTypeBinaryRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        dataTypeLabel = new javax.swing.JLabel();
        testConnection = new javax.swing.JButton();
        transmissionModeComboBox = new com.mirth.connect.client.ui.components.MirthComboBox();
        transmissionModeLabel = new javax.swing.JLabel();
        sampleLabel = new javax.swing.JLabel();
        sampleValue = new javax.swing.JLabel();
        localAddressLabel = new javax.swing.JLabel();
        localAddressField = new com.mirth.connect.client.ui.components.MirthTextField();
        localPortLabel = new javax.swing.JLabel();
        localPortField = new com.mirth.connect.client.ui.components.MirthTextField();
        keepConnectionOpenLabel1 = new javax.swing.JLabel();
        overrideLocalBindingYesRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        overrideLocalBindingNoRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        settingsPlaceHolderLabel = new javax.swing.JLabel();
        queueOnResponseTimeoutLabel = new javax.swing.JLabel();
        queueOnResponseTimeoutYesRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        queueOnResponseTimeoutNoRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        checkRemoteHostLabel = new javax.swing.JLabel();
        checkRemoteHostYesRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();
        checkRemoteHostNoRadio = new com.mirth.connect.client.ui.components.MirthRadioButton();

        setBackground(new java.awt.Color(255, 255, 255));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        keepConnectionOpenLabel.setText("Keep Connection Open:");

        jLabel15.setText("Buffer Size (bytes):");

        sendTimeoutLabel.setText("Send Timeout (ms):");

        jLabel17.setText("Remote Port:");

        jLabel18.setText("Remote Address:");

        remotePortField.setToolTipText("<html>The port on which to connect.</html>");

        sendTimeoutField.setToolTipText("<html>The number of milliseconds to keep the connection<br/>to the host open, if Keep Connection Open is enabled.<br/>If zero, the connection will be kept open indefinitely.</html>");

        bufferSizeField.setToolTipText("<html>The size, in bytes, of the buffer to be used to hold messages waiting to be sent. Generally, the default value is fine.<html>");

        keepConnectionOpenYesRadio.setBackground(new java.awt.Color(255, 255, 255));
        keepConnectionOpenYesRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        keepConnectionOpenGroup.add(keepConnectionOpenYesRadio);
        keepConnectionOpenYesRadio.setText("Yes");
        keepConnectionOpenYesRadio.setToolTipText("<html>Select Yes to keep the connection to the host open across multiple messages.<br>Select No to immediately the close the connection to the host after sending each message.</html>");
        keepConnectionOpenYesRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        keepConnectionOpenYesRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keepConnectionOpenYesRadioActionPerformed(evt);
            }
        });

        keepConnectionOpenNoRadio.setBackground(new java.awt.Color(255, 255, 255));
        keepConnectionOpenNoRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        keepConnectionOpenGroup.add(keepConnectionOpenNoRadio);
        keepConnectionOpenNoRadio.setText("No");
        keepConnectionOpenNoRadio.setToolTipText("<html>Select Yes to keep the connection to the host open across multiple messages.<br>Select No to immediately the close the connection to the host after sending each message.</html>");
        keepConnectionOpenNoRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        keepConnectionOpenNoRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keepConnectionOpenNoRadioActionPerformed(evt);
            }
        });

        remoteAddressField.setToolTipText("<html>The DNS domain name or IP address on which to connect.</html>");

        responseTimeoutField.setToolTipText("<html>The number of milliseconds the connector should wait whenever attempting to read from the remote socket.</html>");

        responseTimeoutLabel.setText("Response Timeout (ms):");

        charsetEncodingCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Default", "UTF-8", "ISO-8859-1", "UTF-16 (le)", "UTF-16 (be)", "UTF-16 (bom)", "US-ASCII" }));
        charsetEncodingCombobox.setToolTipText("<html>The character set encoding to use when converting the outbound message to a byte stream if Data Type Text is selected.<br>Select Default to use the default character set encoding for the JVM running the Mirth Connect server.</html>");
        charsetEncodingCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                charsetEncodingComboboxActionPerformed(evt);
            }
        });

        encodingLabel.setText("Encoding:");

        jLabel7.setText("Template:");

        templateTextArea.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        ignoreResponseCheckBox.setBackground(new java.awt.Color(255, 255, 255));
        ignoreResponseCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        ignoreResponseCheckBox.setText("Ignore Response");
        ignoreResponseCheckBox.setToolTipText("<html>If checked, the connector will not wait for a response after sending a message.<br>If unchecked, the connector will wait for a response from the host after each message is sent.</html>");
        ignoreResponseCheckBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        ignoreResponseCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ignoreResponseCheckBoxActionPerformed(evt);
            }
        });

        dataTypeASCIIRadio.setBackground(new java.awt.Color(255, 255, 255));
        dataTypeASCIIRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        dataTypeButtonGroup.add(dataTypeASCIIRadio);
        dataTypeASCIIRadio.setSelected(true);
        dataTypeASCIIRadio.setText("Text");
        dataTypeASCIIRadio.setToolTipText("<html>Select Binary if the outbound message is a Base64 string (will be decoded before it is sent out).<br/>Select Text if the outbound message is text (will be encoded with the specified character set encoding).</html>");
        dataTypeASCIIRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        dataTypeASCIIRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dataTypeASCIIRadioActionPerformed(evt);
            }
        });

        dataTypeBinaryRadio.setBackground(new java.awt.Color(255, 255, 255));
        dataTypeBinaryRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        dataTypeButtonGroup.add(dataTypeBinaryRadio);
        dataTypeBinaryRadio.setText("Binary");
        dataTypeBinaryRadio.setToolTipText("<html>Select Binary if the outbound message is a Base64 string (will be decoded before it is sent out).<br/>Select Text if the outbound message is text (will be encoded with the specified character set encoding).</html>");
        dataTypeBinaryRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        dataTypeBinaryRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dataTypeBinaryRadioActionPerformed(evt);
            }
        });

        dataTypeLabel.setText("Data Type:");

        testConnection.setText("Test Connection");
        testConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testConnectionActionPerformed(evt);
            }
        });

        transmissionModeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "MLLP" }));
        transmissionModeComboBox.setToolTipText("<html>Select the transmission mode to use for sending and receiving data.<br/></html>");
        transmissionModeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmissionModeComboBoxActionPerformed(evt);
            }
        });

        transmissionModeLabel.setText("Transmission Mode:");
        transmissionModeLabel.setToolTipText("<html>Select the transmission mode to use for sending and receiving data.<br/></html>");

        sampleLabel.setText("Sample Frame:");

        sampleValue.setForeground(new java.awt.Color(153, 153, 153));
        sampleValue.setText("<html><b>&lt;VT&gt;</b> <i>&lt;Message Data&gt;</i> <b>&lt;FS&gt;&lt;CR&gt;</b></html>");
        sampleValue.setEnabled(false);

        localAddressLabel.setText("Local Address:");

        localAddressField.setToolTipText("<html>The local address that the client socket will be bound to, if Override Local Binding is set to Yes.<br/></html>");

        localPortLabel.setText("Local Port:");

        localPortField.setToolTipText("<html>The local port that the client socket will be bound to, if Override Local Binding is set to Yes.<br/><br/>Note that if a specific (non-zero) local port is chosen, then after a socket is closed it's up to the<br/>underlying OS to release the port before the next socket creation, otherwise the bind attempt will fail.<br/></html>");

        keepConnectionOpenLabel1.setText("Override Local Binding:");

        overrideLocalBindingYesRadio.setBackground(new java.awt.Color(255, 255, 255));
        overrideLocalBindingYesRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        overrideLocalBindingButtonGroup.add(overrideLocalBindingYesRadio);
        overrideLocalBindingYesRadio.setText("Yes");
        overrideLocalBindingYesRadio.setToolTipText("<html>Select Yes to override the local address and port that the client socket will be bound to.<br/>Select No to use the default values of 0.0.0.0:0.<br/>A local port of zero (0) indicates that the OS should assign an ephemeral port automatically.<br/><br/>Note that if a specific (non-zero) local port is chosen, then after a socket is closed it's up to the<br/>underlying OS to release the port before the next socket creation, otherwise the bind attempt will fail.<br/></html>");
        overrideLocalBindingYesRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        overrideLocalBindingYesRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                overrideLocalBindingYesRadioActionPerformed(evt);
            }
        });

        overrideLocalBindingNoRadio.setBackground(new java.awt.Color(255, 255, 255));
        overrideLocalBindingNoRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        overrideLocalBindingButtonGroup.add(overrideLocalBindingNoRadio);
        overrideLocalBindingNoRadio.setText("No");
        overrideLocalBindingNoRadio.setToolTipText("<html>Select Yes to override the local address and port that the client socket will be bound to.<br/>Select No to use the default values of 0.0.0.0:0.<br/>A local port of zero (0) indicates that the OS should assign an ephemeral port automatically.<br/><br/>Note that if a specific (non-zero) local port is chosen, then after a socket is closed it's up to the<br/>underlying OS to release the port before the next socket creation, otherwise the bind attempt will fail.<br/></html>");
        overrideLocalBindingNoRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        overrideLocalBindingNoRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                overrideLocalBindingNoRadioActionPerformed(evt);
            }
        });

        settingsPlaceHolderLabel.setText("     ");

        queueOnResponseTimeoutLabel.setText("Queue on Response Timeout:");

        queueOnResponseTimeoutYesRadio.setBackground(new java.awt.Color(255, 255, 255));
        queueOnResponseTimeoutYesRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        queueOnResponseTimeoutButtonGroup.add(queueOnResponseTimeoutYesRadio);
        queueOnResponseTimeoutYesRadio.setText("Yes");
        queueOnResponseTimeoutYesRadio.setToolTipText("<html>If enabled, the message is queued when a timeout occurs while waiting for a response.<br/>Otherwise, the message is set to errored when a timeout occurs while waiting for a response.<br/>This setting has no effect unless queuing is enabled for the connector.</html>");
        queueOnResponseTimeoutYesRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));

        queueOnResponseTimeoutNoRadio.setBackground(new java.awt.Color(255, 255, 255));
        queueOnResponseTimeoutNoRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        queueOnResponseTimeoutButtonGroup.add(queueOnResponseTimeoutNoRadio);
        queueOnResponseTimeoutNoRadio.setText("No");
        queueOnResponseTimeoutNoRadio.setToolTipText("<html>If enabled, the message is queued when a timeout occurs while waiting for a response.<br/>Otherwise, the message is set to errored when a timeout occurs while waiting for a response.<br/>This setting has no effect unless queuing is enabled for the connector.</html>");
        queueOnResponseTimeoutNoRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));

        checkRemoteHostLabel.setText("Check Remote Host:");

        checkRemoteHostYesRadio.setBackground(new java.awt.Color(255, 255, 255));
        checkRemoteHostYesRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        checkRemoteHostButtonGroup.add(checkRemoteHostYesRadio);
        checkRemoteHostYesRadio.setText("Yes");
        checkRemoteHostYesRadio.setToolTipText("<html>Select Yes to check if the remote host has closed the connection before each message.<br>Select No to assume the remote host has not closed the connection.<br>Checking the remote host will decrease throughput but will prevent the message from<br>erroring if the remote side closed the connection and queueing is disabled.</html>");
        checkRemoteHostYesRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));

        checkRemoteHostNoRadio.setBackground(new java.awt.Color(255, 255, 255));
        checkRemoteHostNoRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        checkRemoteHostButtonGroup.add(checkRemoteHostNoRadio);
        checkRemoteHostNoRadio.setText("No");
        checkRemoteHostNoRadio.setToolTipText("<html>Select Yes to check if the remote host has closed the connection before each message.<br>Select No to assume the remote host has not closed the connection.<br>Checking the remote host will decrease throughput but will prevent the message from<br>erroring if the remote side closed the connection and queueing is disabled.</html>");
        checkRemoteHostNoRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(encodingLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(responseTimeoutLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(keepConnectionOpenLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(queueOnResponseTimeoutLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(dataTypeLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(keepConnectionOpenLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(sendTimeoutLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel18, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(checkRemoteHostLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(transmissionModeLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(sampleLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(localAddressLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(localPortLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleValue)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(transmissionModeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(settingsPlaceHolderLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(templateTextArea, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(remoteAddressField, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(testConnection))
                            .addComponent(remotePortField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(overrideLocalBindingYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(overrideLocalBindingNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(localAddressField, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(localPortField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(keepConnectionOpenYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(keepConnectionOpenNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(sendTimeoutField, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(responseTimeoutField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(bufferSizeField, javax.swing.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(ignoreResponseCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(queueOnResponseTimeoutYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(queueOnResponseTimeoutNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(dataTypeBinaryRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(dataTypeASCIIRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(charsetEncodingCombobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(checkRemoteHostYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkRemoteHostNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(transmissionModeLabel)
                    .addComponent(transmissionModeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(settingsPlaceHolderLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sampleLabel)
                    .addComponent(sampleValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(remoteAddressField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(testConnection))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(remotePortField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keepConnectionOpenLabel1)
                    .addComponent(overrideLocalBindingYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(overrideLocalBindingNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(localAddressLabel)
                    .addComponent(localAddressField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(localPortLabel)
                    .addComponent(localPortField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keepConnectionOpenLabel)
                    .addComponent(keepConnectionOpenYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(keepConnectionOpenNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkRemoteHostLabel)
                    .addComponent(checkRemoteHostYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkRemoteHostNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sendTimeoutLabel)
                    .addComponent(sendTimeoutField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(bufferSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(responseTimeoutLabel)
                    .addComponent(responseTimeoutField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ignoreResponseCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(queueOnResponseTimeoutLabel)
                    .addComponent(queueOnResponseTimeoutYesRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(queueOnResponseTimeoutNoRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dataTypeLabel)
                    .addComponent(dataTypeBinaryRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dataTypeASCIIRadio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(encodingLabel)
                    .addComponent(charsetEncodingCombobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(templateTextArea, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {settingsPlaceHolderLabel, transmissionModeComboBox});

    }// </editor-fold>//GEN-END:initComponents
    // @formatter:on

    private void dataTypeBinaryRadioActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_dataTypeBinaryRadioActionPerformed
    {//GEN-HEADEREND:event_dataTypeBinaryRadioActionPerformed
        encodingLabel.setEnabled(false);
        charsetEncodingCombobox.setEnabled(false);
        charsetEncodingCombobox.setSelectedIndex(0);
    }//GEN-LAST:event_dataTypeBinaryRadioActionPerformed

    private void dataTypeASCIIRadioActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_dataTypeASCIIRadioActionPerformed
    {//GEN-HEADEREND:event_dataTypeASCIIRadioActionPerformed
        encodingLabel.setEnabled(true);
        charsetEncodingCombobox.setEnabled(true);
    }//GEN-LAST:event_dataTypeASCIIRadioActionPerformed

    private void ignoreResponseCheckBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ignoreResponseCheckBoxActionPerformed
    {//GEN-HEADEREND:event_ignoreResponseCheckBoxActionPerformed
        boolean selected = ignoreResponseCheckBox.isSelected();
        queueOnResponseTimeoutLabel.setEnabled(!selected);
        queueOnResponseTimeoutYesRadio.setEnabled(!selected);
        queueOnResponseTimeoutNoRadio.setEnabled(!selected);
    }//GEN-LAST:event_ignoreResponseCheckBoxActionPerformed

    private void testConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testConnectionActionPerformed
        ResponseHandler handler = new ResponseHandler() {
            @Override
            public void handle(Object response) {
                ConnectionTestResponse connectionTestResponse = (ConnectionTestResponse) response;
                if (connectionTestResponse == null) {
                    parent.alertError(parent, "Failed to invoke service.");
                } else if (connectionTestResponse.getType().equals(ConnectionTestResponse.Type.SUCCESS)) {
                    parent.alertInformation(parent, connectionTestResponse.getMessage());
                } else {
                    parent.alertWarning(parent, connectionTestResponse.getMessage());
                }
            }
        };

        try {
            getServlet(TcpConnectorServletInterface.class, "Testing connection...", "Failed to test connection: ", handler).testConnection(getChannelId(), getChannelName(), (TcpDispatcherProperties) getFilledProperties());
        } catch (ClientException e) {
            // Should not happen
        }
    }//GEN-LAST:event_testConnectionActionPerformed

    private void keepConnectionOpenYesRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keepConnectionOpenYesRadioActionPerformed
        sendTimeoutLabel.setEnabled(true);
        sendTimeoutField.setEnabled(true);
        checkRemoteHostLabel.setEnabled(true);
        checkRemoteHostYesRadio.setEnabled(true);
        checkRemoteHostNoRadio.setEnabled(true);
    }//GEN-LAST:event_keepConnectionOpenYesRadioActionPerformed

    private void keepConnectionOpenNoRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keepConnectionOpenNoRadioActionPerformed
        sendTimeoutLabel.setEnabled(false);
        sendTimeoutField.setEnabled(false);
        checkRemoteHostLabel.setEnabled(false);
        checkRemoteHostYesRadio.setEnabled(false);
        checkRemoteHostNoRadio.setEnabled(false);
    }//GEN-LAST:event_keepConnectionOpenNoRadioActionPerformed

    private void transmissionModeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmissionModeComboBoxActionPerformed
        String name = (String) transmissionModeComboBox.getSelectedItem();

        if (!modeLock && transmissionModeProvider != null) {
            if (!transmissionModeProvider.getDefaultProperties().equals(transmissionModeProvider.getProperties())) {
                if (JOptionPane.showConfirmDialog(parent, "Are you sure you would like to change the transmission mode and lose all of the current transmission properties?", "Select an Option", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    modeLock = true;
                    transmissionModeComboBox.setSelectedItem(selectedMode);
                    modeLock = false;
                    return;
                }
            }
        }

        selectedMode = name;
        if (name.equals("Basic TCP")) {
            transmissionModeProvider = new BasicModeClientProvider();
        } else {
            for (TransmissionModePlugin plugin : LoadedExtensions.getInstance().getTransmissionModePlugins().values()) {
                if (plugin.getPluginPointName().equals(name)) {
                    transmissionModeProvider = plugin.createProvider();
                }
            }
        }

        if (transmissionModeProvider != null) {
            transmissionModeProvider.initialize(this);
            ((GroupLayout) getLayout()).replace(settingsPlaceHolder, transmissionModeProvider.getSettingsComponent());
            settingsPlaceHolder = transmissionModeProvider.getSettingsComponent();
        }
    }//GEN-LAST:event_transmissionModeComboBoxActionPerformed

    private void overrideLocalBindingYesRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_overrideLocalBindingYesRadioActionPerformed
        localAddressField.setEnabled(true);
        localAddressLabel.setEnabled(true);
        localPortField.setEnabled(true);
        localPortLabel.setEnabled(true);
    }//GEN-LAST:event_overrideLocalBindingYesRadioActionPerformed

    private void overrideLocalBindingNoRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_overrideLocalBindingNoRadioActionPerformed
        localAddressField.setEnabled(false);
        localAddressLabel.setEnabled(false);
        localPortField.setEnabled(false);
        localPortLabel.setEnabled(false);
    }//GEN-LAST:event_overrideLocalBindingNoRadioActionPerformed

    private void charsetEncodingComboboxActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_charsetEncodingComboboxActionPerformed
    }// GEN-LAST:event_charsetEncodingComboboxActionPerformed
     // Variables declaration - do not modify//GEN-BEGIN:variables

    private com.mirth.connect.client.ui.components.MirthTextField bufferSizeField;
    private com.mirth.connect.client.ui.components.MirthComboBox charsetEncodingCombobox;
    private javax.swing.ButtonGroup checkRemoteHostButtonGroup;
    private javax.swing.JLabel checkRemoteHostLabel;
    private com.mirth.connect.client.ui.components.MirthRadioButton checkRemoteHostNoRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton checkRemoteHostYesRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton dataTypeASCIIRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton dataTypeBinaryRadio;
    private javax.swing.ButtonGroup dataTypeButtonGroup;
    private javax.swing.JLabel dataTypeLabel;
    private javax.swing.JLabel encodingLabel;
    private com.mirth.connect.client.ui.components.MirthCheckBox ignoreResponseCheckBox;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel7;
    private javax.swing.ButtonGroup keepConnectionOpenGroup;
    private javax.swing.JLabel keepConnectionOpenLabel;
    private javax.swing.JLabel keepConnectionOpenLabel1;
    private com.mirth.connect.client.ui.components.MirthRadioButton keepConnectionOpenNoRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton keepConnectionOpenYesRadio;
    private com.mirth.connect.client.ui.components.MirthTextField localAddressField;
    private javax.swing.JLabel localAddressLabel;
    private com.mirth.connect.client.ui.components.MirthTextField localPortField;
    private javax.swing.JLabel localPortLabel;
    private javax.swing.ButtonGroup overrideLocalBindingButtonGroup;
    private com.mirth.connect.client.ui.components.MirthRadioButton overrideLocalBindingNoRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton overrideLocalBindingYesRadio;
    private javax.swing.ButtonGroup queueOnResponseTimeoutButtonGroup;
    private javax.swing.JLabel queueOnResponseTimeoutLabel;
    private com.mirth.connect.client.ui.components.MirthRadioButton queueOnResponseTimeoutNoRadio;
    private com.mirth.connect.client.ui.components.MirthRadioButton queueOnResponseTimeoutYesRadio;
    private com.mirth.connect.client.ui.components.MirthIconTextField remoteAddressField;
    private com.mirth.connect.client.ui.components.MirthTextField remotePortField;
    private com.mirth.connect.client.ui.components.MirthTextField responseTimeoutField;
    private javax.swing.JLabel responseTimeoutLabel;
    private javax.swing.JLabel sampleLabel;
    private javax.swing.JLabel sampleValue;
    private com.mirth.connect.client.ui.components.MirthTextField sendTimeoutField;
    private javax.swing.JLabel sendTimeoutLabel;
    private javax.swing.JLabel settingsPlaceHolderLabel;
    private com.mirth.connect.client.ui.components.MirthSyntaxTextArea templateTextArea;
    private javax.swing.JButton testConnection;
    private com.mirth.connect.client.ui.components.MirthComboBox transmissionModeComboBox;
    private javax.swing.JLabel transmissionModeLabel;
    // End of variables declaration//GEN-END:variables
}
