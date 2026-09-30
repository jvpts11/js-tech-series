/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.machine.SigmaLanguage;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaVersions;
import dev.jstech.core.language.IProgrammingLanguage;

/**
 * The version of a language the open desktop's machine can build, which is what its installed compiler knows, and
 * what an editor builds and offers with from it: the editors follow the compiler, as the machine's own prompt does.
 */
final class InstalledCompilers {

    private InstalledCompilers() {
    }

    /** The version of {@code level} the machine's installed compiler knows. */
    static int version(final LanguageLevel level) {
        return SigmaVersions.ofPackage(ActiveDesktop.installedVersion(level.compilerPackage()));
    }

    /**
     * The version a build of {@code level} is held to: the one its project names when it names one, never above
     * what the compiler knows, and the compiler's own when the project names none.
     */
    static int held(final LanguageLevel level, final int projectVersion) {
        final int ceiling = version(level);
        return projectVersion > 0 ? Math.min(projectVersion, ceiling) : ceiling;
    }

    /**
     * What to build with in {@code language}: the instruction set, empty for the language's own choice, and for a
     * language with versions the one the build is held to; another language builds at its newest.
     */
    static IProgrammingLanguage.CompileOptions options(final IProgrammingLanguage language, final String isa,
                                                       final int projectVersion) {
        final int version = language instanceof SigmaLanguage sigma
                ? held(sigma.level(), projectVersion) : IProgrammingLanguage.CompileOptions.NEWEST;
        return new IProgrammingLanguage.CompileOptions(isa, version);
    }
}
