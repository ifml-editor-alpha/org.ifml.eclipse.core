package org.ifml.eclipse.osgi;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.ifml.base.Disposable;
import org.ifml.eclipse.core.CommonCore;
import org.ifml.eclipse.core.runtime.Logs;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.packageadmin.PackageAdmin;

import com.google.common.base.Preconditions;
import com.google.common.io.InputSupplier;

/**
 * Provides utility methods for OSGi bundles.
 */
public final class Bundles {

    private Bundles() {
    }

    /**
     * Returns a factory that will supply instances of {@link InputStream} that read from an entry in the bundle.
     * <p>
     * The returned supplier will throw an {@link IllegalArgumentException} in its {@link InputSupplier#getInput() getInput()} method
     * if no entry could be found.
     * 
     * @param bundle
     *            the bundle to read from.
     * @param entryPath
     *            the path name of the entry.
     * @return the factory.
     * @throws NullPointerException
     *             if the bundle is null.
     */

    public static InputSupplier<InputStream> newEntryInputStreamSupplier(final Bundle bundle, final String entryPath) {
        checkNotNull(bundle);
        return new InputSupplier<InputStream>() {
            @Override
            public InputStream getInput() throws IOException {
                URL url = bundle.getEntry(entryPath);
                checkArgument(url != null, "entry %s not found in bundle %s", entryPath, bundle.getSymbolicName());
                return url.openStream();
            }

        };
    }

    /**
     * Ensures a bundle with a specific identifier exists.
     * 
     * @param symbolicName
     *            the symbolic name.
     * @throws IllegalArgumentException
     *             if a plug-in with the specified symbolic name does not exist.
     */
    public static void checkBundle(String symbolicName) {
        checkArgument(Platform.getBundle(symbolicName) != null, "bundle %s not found", symbolicName);
    }

    /**
     * Returns the bundle declaring a specified class.
     * 
     * @param cl
     *            the class.
     * @return the bundle, or {@code null} if the class was not loaded by a bundle class loader.
     */
    public static Bundle getDeclaringBundle(Class<?> cl) {
        PackageAdmin packageAdmin = ServiceTrackers.getService(CommonCore.getDefault().getBundle().getBundleContext(),
                PackageAdmin.class);
        return packageAdmin.getBundle(cl);
    }

    /**
     * Disposes of a {@link Disposable} when a bundle is closed.
     * 
     * @param bundleContext
     *            a bundle context.
     * @param disposable
     *            a disposable.
     */
    public static void disposeOnClose(BundleContext bundleContext, Disposable disposable) {
        Preconditions.checkNotNull(bundleContext);
        Preconditions.checkNotNull(disposable);
        new BundleListenerDecorator(bundleContext).synchronous().eventTypes(BundleEvent.STOPPING)
                .decorateAndListen(new Disposer(disposable));
    }

    /**
     * Returns the installation directory of a bundle.
     * <p>
     * 
     * @param bundle
     *            a bundle.
     * @return the base directory of {@code bundle}.
     * @throws IOException
     *             if this method fails.
     */
    // TODO: verify the behaviour in case of JAR-based plug-in
    public static File getDirectory(Bundle bundle) throws IOException {
        URL url = FileLocator.toFileURL(bundle.getEntry("/"));
        return new File(url.getFile());
    }

    private static final class Disposer implements BundleListener {

        private final Disposable disposable;

        public Disposer(Disposable disposable) {
            this.disposable = disposable;
        }

        @Override
        public void bundleChanged(BundleEvent event) {
            try {
                disposable.dispose();
            } catch (Throwable e) {
                Logs.logError(e, "Unable to dispose " + disposable, null);
            }
        }

    }

}
