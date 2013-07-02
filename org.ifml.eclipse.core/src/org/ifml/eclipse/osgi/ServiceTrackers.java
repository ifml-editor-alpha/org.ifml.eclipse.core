package org.ifml.eclipse.osgi;

import org.ifml.base.ImmutablePair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.util.tracker.ServiceTracker;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Provides utility methods for OSGi service trackers.
 */
public final class ServiceTrackers {

    private static LoadingCache<ImmutablePair<BundleContext, ?>, ServiceTracker> TRACKERS = CacheBuilder.newBuilder().build(
            new CacheLoader<ImmutablePair<BundleContext, ?>, ServiceTracker>() {
                @Override
                public ServiceTracker load(ImmutablePair<BundleContext, ?> input) {
                    BundleContext bundleContext = input.first;
                    Class<?> cl = (Class<?>) input.second;
                    ServiceTracker tracker = new ServiceTracker(bundleContext, cl.getName(), null);
                    new BundleListenerDecorator(bundleContext).synchronous().eventTypes(BundleEvent.STOPPING)
                            .decorateAndListen(new TrackerCloser(tracker));
                    tracker.open();
                    return tracker;
                }
            });

    private ServiceTrackers() {
    }

    /**
     * Returns a service object. If not yet available, a suitable service tracker is created and opened. This method also ensures that
     * the service tracker will be closed when the associated bundle context will stop.
     * 
     * @param <T>
     *            the type parameter.
     * @param context
     *            the {@link BundleContext} against which the tracking is done.
     * @param cl
     *            the class of the services being tracked.
     * @return the service object.
     */
    public static <T> T getService(BundleContext context, Class<T> cl) {
        ServiceTracker tracker = TRACKERS.getUnchecked(ImmutablePair.of(context, cl));
        return cl.cast(tracker.getService());
    }

    private static final class TrackerCloser implements BundleListener {

        private final ServiceTracker tracker;

        public TrackerCloser(ServiceTracker tracker) {
            this.tracker = tracker;
        }

        @Override
        public void bundleChanged(BundleEvent event) {
            tracker.close();
        }
    }

}
