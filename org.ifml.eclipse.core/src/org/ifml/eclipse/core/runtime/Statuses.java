package org.ifml.eclipse.core.runtime;

import javax.annotation.Nullable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * Provides utility methods for working with status objects.
 */
public final class Statuses {

    /**
     * A standard error status with no message.
     * 
     * @see Status#OK_STATUS
     * @see Status#CANCEL_STATUS
     */
    public static final IStatus ERROR_STATUS = getErrorStatus(null, "", null);

    private Statuses() {
    }

    /**
     * Returns an error status wrapping a specific exception.
     * 
     * @param e
     *            the Throwable to wrap.
     * @param message
     *            the error message.
     * @param bundle
     *            the relevant bundle.
     * @return the error status.
     */
    public static final IStatus getErrorStatus(@Nullable Throwable e, @Nullable String message, @Nullable Bundle bundle) {
        return getStatus(e, message, bundle, IStatus.ERROR);
    }

    /**
     * Returns a warning status wrapping a specific exception.
     * 
     * @param e
     *            the Throwable to wrap.
     * @param message
     *            the warning message.
     * @param bundle
     *            the relevant bundle.
     * @return the error status.
     */
    public static IStatus getWarningStatus(@Nullable Throwable e, @Nullable String message, @Nullable Bundle bundle) {
        return getStatus(e, message, bundle, IStatus.WARNING);
    }

    /**
     * Converts a boolean into a standard Ok or Cancel status.
     * 
     * @param b
     *            the boolean to convert.
     * @return the converted OK or Cancel status.
     */
    public static IStatus toOkCancelStatus(boolean b) {
        return b ? Status.OK_STATUS : Status.CANCEL_STATUS;
    }

    /**
     * Converts a boolean into a standard Ok or Error status.
     * 
     * @param b
     *            the boolean to convert.
     * @return the converted OK or Error status.
     */
    public static IStatus toOkErrorStatus(boolean b) {
        return b ? Status.OK_STATUS : ERROR_STATUS;
    }

    /**
     * Builds a status object.
     * 
     * @param e
     *            the Throwable to wrap.
     * @param message
     *            the status message.
     * @param bundle
     *            the relevant bundle.
     * @param severity
     *            the severity.
     * @return
     */
    private static final IStatus getStatus(@Nullable Throwable e, @Nullable String message, @Nullable Bundle bundle, int severity) {
        if (e != null) {
            if (message == null) {
                message = e.getMessage();
            }
            if (message == null) {
                message = e.toString();
            }
        }
        if (message == null) {
            message = "";
        }
        String pluginId = (bundle != null) ? bundle.getSymbolicName() : "unknown";
        return new Status(severity, pluginId, 1, message, e);
    }

}
