package org.ifml.eclipse.core.runtime;

import javax.annotation.Nullable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * Provides utility methods for progress monitors.
 */
public final class ProgressMonitors {

    /** A default value (100) for a progress monitor total work. */
    public static final int DEFAULT_TOTAL_WORK = 100;

    private ProgressMonitors() {
    }

    /**
     * Returns a non-null progress monitor from the {@code monitor} argument.
     * 
     * @param monitor
     *            the input progress monitor (possibly null).
     * @return the input {@code monitor} if not null, or a suitable non-null monitor.
     */
    public static IProgressMonitor monitorFor(@Nullable IProgressMonitor monitor) {
        return (monitor != null) ? monitor : new NullProgressMonitor();
    }

    /**
     * Returns a sub-progress monitor from the {@code monitor} argument.
     * 
     * @param monitor
     *            the parent progress monitor (possibly null).
     * @param ticks
     *            the number of work ticks allocated from the parent monitor.
     * @return a suitable sub-progress monitor.
     */
    public static IProgressMonitor subMonitorFor(IProgressMonitor monitor, int ticks) {
        if (monitor == null) {
            return new NullProgressMonitor();
        }
        if (monitor instanceof NullProgressMonitor) {
            return monitor;
        }
        return new SubProgressMonitor(monitor, ticks);
    }

    /**
     * Returns a progress monitor only supporting cancelation. The default implementations of the other methods do nothing.
     * 
     * @return a new empty progress monitor.
     */
    public static IProgressMonitor emptyMonitor() {
        return new NullProgressMonitor();
    }

}
