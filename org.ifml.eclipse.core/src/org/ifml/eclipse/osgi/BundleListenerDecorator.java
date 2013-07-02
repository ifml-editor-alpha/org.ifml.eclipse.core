package org.ifml.eclipse.osgi;

import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.SynchronousBundleListener;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * A {@link BundleListener} decorator providing any combination of these features: synchronous or asynchronous listening, optional
 * filtering by specific bundles, optional filtering by specific event types. Usage example:
 * 
 * <pre>
 * IBundleListener aBundleListener = ...; // basic listener
 *  
 * new BundleListenerConfigurator(aBundleContext)
 *     .synchronous()
 *     .bundles(aBundle, anotherBundle)
 *     .eventTypes(BundleEvent.STARTING, BundleEvent.STOPPING)
 *     .decorateAndListen(aBundleListener);}
 * </pre>
 * 
 */
public final class BundleListenerDecorator {

    private static final int ASYNC_EVENT_TYPES = BundleEvent.INSTALLED | BundleEvent.RESOLVED | BundleEvent.STARTED
            | BundleEvent.STOPPED | BundleEvent.UNINSTALLED | BundleEvent.UNRESOLVED | BundleEvent.UPDATED;

    private static final int ALL_EVENT_TYPES = ASYNC_EVENT_TYPES | BundleEvent.LAZY_ACTIVATION | BundleEvent.STARTING
            | BundleEvent.STOPPING;

    private final BundleContext context;

    private boolean synchronous;

    private final Set<String> bundleSymbolicNames = Sets.newHashSet();

    private int eventTypes;

    /**
     * <p>
     * Constructs a new builder.
     * 
     * <p>
     * The {@code context} object is used both to define where the listener will be attached and which specific bundle will be
     * listened. Invoking {@code allBundles()} and {@code bundle()} will redefined the set of bundles to be listened.
     * 
     * @param context
     *            the bundle's execution context where the listener will be added; it also defines the initial bundle to be listened.
     */
    public BundleListenerDecorator(BundleContext context) {
        this.context = context;
        bundles(context.getBundle());
    }

    /**
     * Marks the build listener as synchronous, in order to create a {@link BundleListener}
     * 
     * @return this builder.
     */
    public BundleListenerDecorator synchronous() {
        synchronous = true;
        return this;
    }

    /**
     * Marks the build listener as asynchronous, in order to create a {@link BundleListener}
     * 
     * @return this builder.
     */
    public BundleListenerDecorator asynchronous() {
        synchronous = false;
        return this;
    }

    /**
     * Clears the set of bundles, meaning that the listener will listen to events coming from any bundle.
     * 
     * @return this builder.
     */
    public BundleListenerDecorator allBundles() {
        bundleSymbolicNames.clear();
        return this;
    }

    /**
     * Adds new bundles to the set of bundles being listened.
     * 
     * @param bundle
     *            the bundle to add.
     * @param bundles
     *            additional bundles.
     * @return this builder.
     */
    public BundleListenerDecorator bundles(Bundle bundle, Bundle... bundles) {
        Preconditions.checkNotNull(bundle);
        bundleSymbolicNames.add(bundle.getSymbolicName());
        for (Bundle bundle2 : bundles) {
            bundleSymbolicNames.add(bundle2.getSymbolicName());
        }
        return this;
    }

    /**
     * Adds new event types to the set of event types being listened.
     * 
     * @param eventType
     *            the event type to add.
     * @param eventTypes
     *            additional event types.
     * @return this builder.
     */
    public BundleListenerDecorator eventTypes(int eventType, int... eventTypes) {
        this.eventTypes |= eventType;
        for (int eventType2 : eventTypes) {
            this.eventTypes |= eventType2;
        }
        return this;
    }

    /**
     * Decorates the given listener and starts listening.
     * 
     * @param listener
     *            the delegate listener.
     * @return a new listener decorating {@code delegateListener}.
     */
    public BundleListener decorateAndListen(BundleListener listener) {
        Preconditions.checkNotNull(listener);
        if (synchronous) {
            Preconditions.checkState((ALL_EVENT_TYPES | eventTypes) == ALL_EVENT_TYPES);
        } else {
            Preconditions.checkState((ASYNC_EVENT_TYPES | eventTypes) == ASYNC_EVENT_TYPES);
        }
        BundleListener decoratorListener = synchronous ? new SynchronousListener(listener, this) : new AsynchronousListener(listener,
                this);
        context.addBundleListener(decoratorListener);
        return decoratorListener;
    }

    private static class AsynchronousListener implements BundleListener {

        private final Set<String> symbolicNames;

        private final int eventTypes;

        private final BundleListener listener;

        public AsynchronousListener(BundleListener delegateListener, BundleListenerDecorator builder) {
            this.symbolicNames = ImmutableSet.copyOf(builder.bundleSymbolicNames);
            this.eventTypes = builder.eventTypes;
            this.listener = delegateListener;
        }

        public void bundleChanged(BundleEvent event) {
            if (symbolicNames.isEmpty() || symbolicNames.contains(event.getBundle().getSymbolicName())) {
                if ((eventTypes == 0) || ((eventTypes & event.getType()) != 0)) {
                    listener.bundleChanged(event);
                }
            }
        }

    }

    private static final class SynchronousListener extends AsynchronousListener implements SynchronousBundleListener {

        public SynchronousListener(BundleListener delegateListener, BundleListenerDecorator builder) {
            super(delegateListener, builder);
        }

    }

}
