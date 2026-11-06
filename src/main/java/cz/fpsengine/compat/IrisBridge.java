package cz.fpsengine.compat;

import java.lang.reflect.Method;

/** Reads Iris' public v0 API via reflection, so FPS Engine has no compile-time dependency on Iris. */
public final class IrisBridge {
    private static Object api;
    private static Method inUse;
    private static boolean resolved;

    private IrisBridge() {}

    public static boolean installed() {
        return CompatReport.isLoaded("iris");
    }

    /** @return TRUE/FALSE, or null if Iris is missing or its API could not be read. */
    public static synchronized Boolean shaderPackInUse() {
        if (!installed()) return null;
        try {
            if (!resolved) {
                resolved = true;
                Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = c.getMethod("getInstance").invoke(null);
                inUse = c.getMethod("isShaderPackInUse");
            }
            return inUse == null ? null : (Boolean) inUse.invoke(api);
        } catch (Throwable t) {
            inUse = null;
            return null;
        }
    }
}
