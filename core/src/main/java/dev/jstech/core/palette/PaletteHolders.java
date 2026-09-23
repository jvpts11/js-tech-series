/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.palette;

import java.lang.annotation.ElementType;
import java.util.stream.Stream;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;

/**
 * Loads the classes that declare palettes, so every palette is declared before it is written out or read back,
 * whether or not anything has drawn with it yet. A palette is declared the moment its class is loaded, and a class
 * nothing has used is not loaded on its own.
 *
 * <p>Only the generator and a client's resource reload call these, since a palette holder is a client class.
 */
public final class PaletteHolders {

    private PaletteHolders() {
    }

    /** Loads the palette holders of that mod. */
    public static void load(final String modid) {
        final IModFileInfo file = ModList.get().getModFileById(modid);
        if (file == null) {
            throw new IllegalStateException("no mod " + modid + " to read palettes from");
        }
        load(Stream.of(file.getFile().getScanResult()));
    }

    /** Loads the palette holders of every mod there is, addons included. */
    public static void loadAll() {
        load(ModList.get().getAllScanData().stream());
    }

    private static void load(final Stream<ModFileScanData> scans) {
        scans.flatMap(scan -> scan.getAnnotatedBy(PaletteHolder.class, ElementType.TYPE))
                .map(annotation -> annotation.clazz().getClassName())
                .sorted()
                .forEach(PaletteHolders::loadClass);
    }

    private static void loadClass(final String className) {
        try {
            Class.forName(className, true, PaletteHolders.class.getClassLoader());
        } catch (final ClassNotFoundException missing) {
            throw new IllegalStateException("the palettes of " + className + " could not be read", missing);
        }
    }
}
