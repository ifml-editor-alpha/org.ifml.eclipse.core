package org.ifml.eclipse.core.runtime;

import javax.annotation.Nullable;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Provides utility methods for working with adaptable objects
 */
public final class Adaptables {

    private Adaptables() {
    }

    /**
     * Returns an object which is an instance of the {@code adapter} class associated with the {@code adaptable} object. Returns
     * <code>null</code> if no such object can be found.
     * 
     * @param <T>
     *            the type parameter.
     * @param adaptable
     *            the adaptable object.
     * @param adapter
     *            the adapter class to look up.
     * @return a object castable to the given class, or <code>null</code> if this object does not have an adapter for the given class
     */
    public static <T> T getAdapter(@Nullable IAdaptable adaptable, @Nullable Class<T> adapter) {
        if ((adaptable == null) || (adapter == null)) {
            return null;
        }
        Object obj = adaptable.getAdapter(adapter);
        if (adapter.isInstance(obj)) {
            return adapter.cast(obj);
        } else {
            return null;
        }
    }

}
