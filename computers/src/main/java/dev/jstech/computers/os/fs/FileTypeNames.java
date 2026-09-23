/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What each kind of file the machines know is called in a listing's Type column and in a New menu. The kinds a
 * language owns are named by that language instead, so they have no name here.
 */
@TextHolder
final class FileTypeNames {

    static final TextKey IQL = TextKey.of("jsc.file_type.iql", "IQL script");
    static final TextKey TXT = TextKey.of("jsc.file_type.txt", "Text");
    static final TextKey LOG = TextKey.of("jsc.file_type.log", "Log");
    static final TextKey CFG = TextKey.of("jsc.file_type.cfg", "Configuration");
    static final TextKey CSV = TextKey.of("jsc.file_type.csv", "Table");
    static final TextKey CMD = TextKey.of("jsc.file_type.cmd", "Shell script");
    static final TextKey CRAFT = TextKey.of("jsc.file_type.craft", "Craft pattern");
    static final TextKey DAT = TextKey.of("jsc.file_type.dat", "Stored item");
    static final TextKey EXE = TextKey.of("jsc.file_type.exe", "Installer");
    static final TextKey SH = TextKey.of("jsc.file_type.sh", "Install script");
    static final TextKey PKG = TextKey.of("jsc.file_type.pkg", "Package manifest");
    static final TextKey INF = TextKey.of("jsc.file_type.inf", "Setup information");
    static final TextKey BIN = TextKey.of("jsc.file_type.bin", "Installer data");
    static final TextKey CPK = TextKey.of("jsc.file_type.cpk", "Program package");
    static final TextKey SLN = TextKey.of("jsc.file_type.sln", "Solution");
    static final TextKey SGSPROJ = TextKey.of("jsc.file_type.sgsproj", "Σ# project");
    static final TextKey SGPROJ = TextKey.of("jsc.file_type.sgproj", "Σ project");

    private FileTypeNames() {
    }
}
