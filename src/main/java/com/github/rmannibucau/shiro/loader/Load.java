package com.github.rmannibucau.shiro.loader;

public final class Load {
    private Load() {
        // no-op
    }

    public static Class<?> load(final String name, final Class<?> def) {
        try {
            return Load.class.getClassLoader().loadClass(name);
        } catch (final ClassNotFoundException | NoClassDefFoundError e) {
            return def;
        }
    }
}
