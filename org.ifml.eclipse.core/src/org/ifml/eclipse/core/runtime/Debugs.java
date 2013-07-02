package org.ifml.eclipse.core.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.Nullable;

import org.eclipse.core.runtime.Platform;
import org.ifml.base.Strings2;
import org.ifml.eclipse.osgi.Bundles;
import org.osgi.framework.Bundle;

import com.google.common.base.CaseFormat;
import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;

/**
 * Provides utility methods for debugging.
 */
public final class Debugs {

    private Debugs() {
    }

    /**
     * Prints on the debug output the message obtained by substituting each {@code %s} in {@code template} with an argument, as
     * described in {@link Strings2#simpleFormat(String, Object...)}.
     * 
     * @param <T>
     *            the type parameter.
     * @param debug
     *            the debug facility.
     * @param template
     *            a non-null string containing 0 or more {@code %s} placeholders.
     * @param args
     *            the arguments to be substituted into the message template
     */
    public static <T extends Enum<T> & IDebug> void debug(T debug, String template, @Nullable Object... args) {
        if (debug.isEnabled()) {
            System.out.println(debug.name() + ": " + Strings2.simpleFormat(template, args));
        }
    }

    /**
     * Returns {@code true} if a debug entry point is enabled.
     * 
     * @param <T>
     *            the type parameter.
     * @param en
     *            the debug entry point.
     * @return {@code true} if the debug is enabled.
     */
    public static <T extends Enum<T> & IDebug> boolean isDebugEnabled(T en) {
        String pluginId = Bundles.getDeclaringBundle(en.getClass()).getSymbolicName();
        String optionPath = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, en.name());
        String option = pluginId + "/debug/" + optionPath;
        Bundle bundle = Bundles.getDeclaringBundle(en.getClass());
        Properties props = new Properties();
        InputSupplier<InputStream> supplier = Bundles.newEntryInputStreamSupplier(bundle, "/.options");
        InputStream in = null;
        try {
            in = supplier.getInput();
            props.load(in);
            if (!props.containsKey(option)) {
                Logs.logWarning(null, "Property not found in .options file: " + option, bundle);
            } else if (!"false".equals(props.getProperty(option))) {
                Logs.logWarning(null, "Invalid property value in .options file: " + option + ". Expecting false", bundle);
            }
        } catch (IllegalArgumentException e) { // .options not found
            return false;
        } catch (IOException e) {
            Logs.logWarning(e, "Unable to load .options file from bundle " + bundle.getSymbolicName(), bundle);
        } finally {
            Closeables.closeQuietly(in);
        }
        return "true".equals(Platform.getDebugOption(option));
    }
}
