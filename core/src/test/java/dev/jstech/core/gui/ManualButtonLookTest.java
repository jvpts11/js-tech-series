/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ManualButtonLookTest {

    private static final int BEIGE = 0xFFE8E0C8;
    private static final int NAVY = 0xFF2E3D5C;
    private static final int INK = 0xFF23252A;
    private static final int LABEL = 0xFFF4F1E6;
    /** A role of a palette's file and its colour: {@code "ink": "#FF23252A"}. */
    private static final Pattern ROLE = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"#([0-9A-Fa-f]{8})\"");

    @Test
    void markFor_takesTheInkOnBeigeAndTheLabelOnNavy() {
        assertEquals(INK, ManualButtonLook.markFor(BEIGE, INK, LABEL));
        assertEquals(LABEL, ManualButtonLook.markFor(NAVY, INK, LABEL));
    }

    @Test
    void of_lightensAndFramesAButtonUnderThePointer() {
        final ManualButtonLook rest = ManualButtonLook.of(NAVY, INK, LABEL, ManualButtonLook.State.NORMAL);
        final ManualButtonLook hover = ManualButtonLook.of(NAVY, INK, LABEL, ManualButtonLook.State.HOVER);

        assertFalse(rest.framed());
        assertTrue(hover.framed());
        assertTrue(ColorContrast.luminance(hover.fill()) > ColorContrast.luminance(rest.fill()));
        assertEquals(rest.mark(), hover.mark(), "the mark keeps its colour under the pointer");
    }

    @Test
    void of_sinksThePressedMarkAndTurnsItsBevelOver() {
        final ManualButtonLook pressed = ManualButtonLook.of(BEIGE, INK, LABEL, ManualButtonLook.State.PRESSED);

        assertEquals(1, pressed.drop());
        assertTrue(ColorContrast.luminance(pressed.dark()) > ColorContrast.luminance(pressed.light()),
                "the lit edge is now at the bottom and right");
    }

    @Test
    void of_fadesTheMarkOfAButtonThatLeadsNowhere() {
        final ManualButtonLook off = ManualButtonLook.of(BEIGE, INK, LABEL, ManualButtonLook.State.DISABLED);

        assertNotEquals(INK, off.mark());
        assertTrue(ColorContrast.ratio(off.mark(), off.fill()) < ColorContrast.ratio(INK, BEIGE));
    }

    /*
     * Every manual style of the series, read from its palette as its mod writes it: a button's mark at rest and
     * pressed reads on its face as body text reads on a page.
     */
    @Test
    void everyManualStyle_keepsItsButtonMarksReadable() throws IOException {
        final Path root = Path.of("").toAbsolutePath().getParent();
        final List<Path> palettes = new ArrayList<>();
        try (Stream<Path> projects = Files.list(root)) {
            for (final Path assets : projects.map(project -> project.resolve("src/generated/resources/assets"))
                    .filter(Files::isDirectory).toList()) {
                try (Stream<Path> files = Files.walk(assets, 4)) {
                    files.filter(file -> file.toString().replace('\\', '/').matches(".*/palettes/guide/[a-z_]+\\.json"))
                            .filter(file -> roles(file).containsKey("coverEdge")).forEach(palettes::add);
                }
            }
        }
        assertTrue(palettes.size() >= 3, () -> "the binder, the Guide to Operations and the drawings: " + palettes);
        for (final Path palette : palettes) {
            final Map<String, Integer> roles = roles(palette);
            for (final ManualButtonLook.State state : List.of(ManualButtonLook.State.NORMAL,
                    ManualButtonLook.State.PRESSED)) {
                final ManualButtonLook look = ManualButtonLook.of(roles.get("coverEdge"), roles.get("ink"),
                        roles.get("label"), state);
                final double ratio = ColorContrast.ratio(look.mark(), look.fill());
                assertTrue(ratio >= ManualButtonLook.LEAST_CONTRAST, () -> palette.getFileName() + " " + state
                        + ": the mark reads at " + ratio + " to 1");
            }
        }
    }

    /** The colours of a palette's file, by role. */
    private static Map<String, Integer> roles(final Path file) {
        final Map<String, Integer> roles = new HashMap<>();
        try {
            final Matcher matcher = ROLE.matcher(Files.readString(file));
            while (matcher.find()) {
                roles.put(matcher.group(1), (int) Long.parseLong(matcher.group(2), 16));
            }
        } catch (final IOException e) {
            throw new IllegalStateException(file + " could not be read", e);
        }
        return roles;
    }
}
