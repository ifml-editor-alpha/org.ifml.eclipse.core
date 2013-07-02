package org.ifml.eclipse.core;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The plug-in runtime class for the IFML Eclipse Core Library.
 */
public final class CommonCore extends Plugin {

    /** The shared instance. */
    private static CommonCore plugin;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static CommonCore getDefault() {
        return plugin;
    }

}
