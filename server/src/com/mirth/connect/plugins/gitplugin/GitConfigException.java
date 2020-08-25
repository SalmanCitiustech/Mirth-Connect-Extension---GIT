package com.mirth.connect.plugins.gitplugin;

public class GitConfigException extends Exception {
    public GitConfigException(String message) {
        super(message);
    }

    public GitConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitConfigException(Throwable cause) {
        super(cause);
    }
}
