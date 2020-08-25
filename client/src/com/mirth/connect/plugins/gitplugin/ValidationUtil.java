package com.mirth.connect.plugins.gitplugin;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

public class ValidationUtil extends InputVerifier {

	@Override
	public boolean verify(JComponent component) {
		boolean isValid = true;

		if (component.getName().equalsIgnoreCase("urlText")) {
			String[] url = ((JTextField) component).getText().split("//", 0);
			if (!(url.length == 2) || !url[0].equalsIgnoreCase("https:")
					|| !url[1].substring(0, url[1].indexOf("/")).equalsIgnoreCase("github.com")) {
				isValid = false;
			}
		}

		return isValid;
	}

}
