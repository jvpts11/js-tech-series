/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.ColorContrast;
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

class TerminalLooksTest {

    /** A role of a palette's file and its colour: {@code "ground": "#FF000000"}. */
    private static final Pattern ROLE = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"#([0-9A-Fa-f]{8})\"");

    /*
     * Every terminal a manual's session can be printed in, read from its palette as its mod writes it: what the system
     * says, the prompt and what is typed read on the console's ground as body text reads on a page.
     */
    @Test
    void everyTerminalLook_readsOnItsGround() throws IOException {
        final Path root = Path.of("").toAbsolutePath().getParent();
        final List<Path> looks = new ArrayList<>();
        try (Stream<Path> projects = Files.list(root)) {
            for (final Path assets : projects.map(project -> project.resolve("src/generated/resources/assets"))
                    .filter(Files::isDirectory).toList()) {
                try (Stream<Path> files = Files.walk(assets, 5)) {
                    files.filter(file -> file.toString().replace('\\', '/').matches(".*/palettes/guide/terminal.*"))
                            .filter(Files::isRegularFile).forEach(looks::add);
                }
            }
        }
        assertTrue(looks.size() >= 5, () -> "the Core's own and the four of J's Computers: " + looks);
        for (final Path look : looks) {
            final Map<String, Integer> roles = roles(look);
            for (final String role : List.of("text", "prompt", "typed")) {
                final double ratio = ColorContrast.ratio(roles.get(role), roles.get("ground"));
                assertTrue(ratio >= 4.5, () -> look + ": " + role + " reads at " + ratio + " to 1");
            }
        }
    }

    /** The colours of a palette's file, by role. */
    private static Map<String, Integer> roles(final Path file) throws IOException {
        final Map<String, Integer> roles = new HashMap<>();
        final Matcher matcher = ROLE.matcher(Files.readString(file));
        while (matcher.find()) {
            roles.put(matcher.group(1), (int) Long.parseLong(matcher.group(2), 16));
        }
        return roles;
    }
}
