package org.ifml.eclipse.core.runtime;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.Nullable;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * Provides utility methods for working with Eclipse logs.
 */
public final class Logs {

    private Logs() {
    }

    /**
     * Logs an error.
     * 
     * @param e
     *            the Throwable to log.
     * @param message
     *            the error message.
     * @param bundle
     *            the relevant bundle.
     */
    public static void logError(@Nullable Throwable e, @Nullable String message, @Nullable Bundle bundle) {
        if (e instanceof InvocationTargetException) {
            e = ((InvocationTargetException) e).getTargetException();
        }
        IStatus status = Statuses.getErrorStatus(e, message, bundle);
        ResourcesPlugin.getPlugin().getLog().log(status);
    }

    /**
     * Logs a warning.
     * 
     * @param e
     *            the Throwable to log.
     * @param message
     *            the warning message.
     * @param bundle
     *            the relevant bundle.
     */
    public static void logWarning(@Nullable Throwable e, @Nullable String message, @Nullable Bundle bundle) {
        if (e instanceof InvocationTargetException) {
            e = ((InvocationTargetException) e).getTargetException();
        }
        IStatus status = Statuses.getWarningStatus(e, message, bundle);
        ResourcesPlugin.getPlugin().getLog().log(status);
    }

    /**
     * Removes the Eclipse workspace log file, if exists.
     * 
     * @throws IOException
     *             if an I/O error occurs.
     */
    public static void deleteLogFile() throws IOException {
        File logFile = Platform.getLogFileLocation().toFile();
        if (logFile.exists() && logFile.isFile()) {
            if (!logFile.delete()) {
                throw new IOException(String.format("Failed to delete %s", logFile));
            }
        }
    }

}
