package org.ifml.eclipse.core.runtime;

import javax.annotation.Nullable;

import org.ifml.base.Strings2;

/**
 * The common interface to be implemented in order to provide an entry point for the Eclipse debugging facilities.
 * <p>
 * To implement this interface:
 * <ul>
 * <li>Create a {@code enum} which implements this interface
 * <li>Add as many enum constants as required (e.g. BASE, EVENT_FILTER, etc...)
 * <li>Store into a private boolean field {@code enabled} the result of invoking {@link Debugs#isDebugEnabled}.
 * <li>Implement {@link #isEnabled()} just returning the private field.
 * <li>Implement {@link #debug(String, Object...)} just invoking {@link Debugs#debug}
 * <li>Create a file named {@code .options} at the root of the plug-in and fill it with one property for each enum constant, using the
 * following syntax:
 * <ul>
 * <li>{@code <plugin-id>/debug/enumConstantInLowerCamelCase}=false.
 * </ul>
 * <li>For instance:
 * <ul>
 * <li>e.g. {@code org.ifml.foo.bar/debug/base=false}
 * <li>e.g. {@code org.ifml.foo.bar/debug/eventFilter=false}
 * </ul>
 * <li>Enable the debug in your own launch configuration using the {@code Tracing} tab
 * </ul>
 */
public interface IDebug {

    /**
     * Returns {@code true} if this debug facility is enabled.
     * 
     * @return the enabled state.
     */
    boolean isEnabled();

    /**
     * Prints on the debug output the message obtained by substituting each {@code %s} in {@code template} with an argument, as
     * described in {@link Strings2#simpleFormat(String, Object...)}.
     * 
     * @param template
     *            a non-null string containing 0 or more {@code %s} placeholders.
     * @param args
     *            the arguments to be substituted into the message template
     */
    void debug(String template, @Nullable Object... args);

}
