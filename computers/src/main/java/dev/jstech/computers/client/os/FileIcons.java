/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.machine.MachineListing;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.core.JsCore;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The small icons every program draws beside a file: a folder, a page marked by what it holds, a cartridge for
 * what cannot be opened.
 *
 * <p>They live in one place so the explorer, the studios' trees and a picker all show the same thing for the same
 * file, and a new kind of file gets its icon once. Each kind is a picture per desktop look at
 * {@code textures/gui/file/<kind>/<skin>.png}; a look without its own falls back the way {@link SkinSprites} says,
 * down to the plain page.
 */
public final class FileIcons {

    /** How big the pictures are drawn, which is the size they are made at. */
    public static final int SIZE = 16;

    /** What an icon stands for. Its name, in lower case, is the folder its pictures are in. */
    public enum Kind { UP, FOLDER, HOME, IQL, DOC, DAT, EXE, PKG, INF, BIN, CFG, LOG, CRAFT, SOURCE, PROGRAM,
        BUNDLE, IMAGE }

    private FileIcons() {
    }

    /**
     * The icon for a file with that extension: the machine's listings get the program page, and a language's own files
     * the source and program pages.
     */
    public static Kind kindOf(final String ext) {
        final String lower = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        if (MachineListing.claims(lower)) {
            return Kind.PROGRAM;
        }
        final var language = JsCore.languages().byExtension(lower);
        if (language != null) {
            return language.sourceExtensions().contains(lower) ? Kind.SOURCE : Kind.PROGRAM;
        }
        // Every kind of file is named here, so a new one cannot be added without its icon being chosen.
        return switch (FileType.of(lower)) {
            case IQL -> Kind.IQL;
            case DAT -> Kind.DAT;
            case EXE, SH -> Kind.EXE;
            case PKG -> Kind.PKG;
            case INF, INI -> Kind.INF;
            case BIN, SYS, FON -> Kind.BIN;
            case CFG -> Kind.CFG;
            case LOG -> Kind.LOG;
            case CRAFT -> Kind.CRAFT;
            // An archive is a thing with other things inside it, which is what the parcel already stands for.
            case CPK, SLN, SGSPROJ, SGPROJ, ARK -> Kind.BUNDLE;
            case PIX -> Kind.IMAGE;
            case TXT, CSV, CMD, SGS, SG, ASM, OTHER -> Kind.DOC;
        };
    }

    /** The icon for a path, by its extension, or a folder's when {@code directory}. */
    public static Kind kindOfPath(final String path, final boolean directory) {
        if (directory) {
            return Kind.FOLDER;
        }
        final int dot = path.lastIndexOf('.');
        return kindOf(dot >= 0 && dot < path.length() - 1 ? path.substring(dot + 1) : "");
    }

    /**
     * Draws the icon with its top-left at ({@code x}, {@code y}), {@link #SIZE} square, in the look of that
     * desktop.
     *
     * @param skin the desktop's icon set, a Frames edition or a desktop's id
     */
    public static void draw(final GuiGraphics g, final int x, final int y, final Kind type, final String skin) {
        SkinSprites.draw(g, SkinSprites.find("file", type.name().toLowerCase(Locale.ROOT), "doc", skin),
                x, y, SIZE, SIZE, SIZE);
    }
}
