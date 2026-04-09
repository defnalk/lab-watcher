package com.labwatcher.engine;

/**
 * Native bindings to the C++ valengine library. Each method returns a JSON
 * string which the {@link EngineAdapter} deserializes into Java records.
 *
 * <p>Loading: the JVM must be started with {@code -Djava.library.path=...}
 * pointing at the directory containing {@code libvalengine.so} (Linux) or
 * {@code libvalengine.dylib} (macOS).
 */
public final class ValEngineJNI {
    private ValEngineJNI() {}

    static {
        try {
            System.loadLibrary("valengine");
        } catch (UnsatisfiedLinkError e) {
            // Allow tests that don't touch the native side to run.
            System.err.println("[lab-watcher] native library not loaded: " + e.getMessage());
        }
    }

    public static native String parseCsv(String filePath);
    public static native String loadSchema(String tomlPath);
    public static native String parseAndValidate(String filePath, String schemaPath);
}
