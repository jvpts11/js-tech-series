/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingsFormatTest {

    @Test
    void write_eachFormatReadsBackWhatItWrote() {
        final Map<String, String> settings = settings("speed", "fast", "path", "C:\\logs \"daily\" <a&b>");
        for (final SettingsFormat format : SettingsFormat.values()) {
            final String text = format.write("", "Logger", settings);
            assertEquals(settings, format.read(text, "Logger"), () -> format + " wrote " + text);
        }
    }

    @Test
    void registry_keepsWhatOtherProgramsWrote() {
        final String first = SettingsFormat.REGISTRY.write("", "Logger", settings("speed", "fast"));
        final String both = SettingsFormat.REGISTRY.write(first, "Gate", settings("open", "yes"));
        final String changed = SettingsFormat.REGISTRY.write(both, "Logger", settings("speed", "slow"));

        assertEquals(settings("speed", "slow"), SettingsFormat.REGISTRY.read(changed, "Logger"));
        assertEquals(settings("open", "yes"), SettingsFormat.REGISTRY.read(changed, "Gate"));
        assertTrue(changed.startsWith("REGEDIT4\n"), changed);
        assertTrue(changed.contains("[HKEY_LOCAL_MACHINE\\SOFTWARE\\Gate]\n\"open\"=\"yes\"\n"), changed);
    }

    @Test
    void registry_dropsTheKeyOfAProgramWithNoSettingsLeft() {
        final String first = SettingsFormat.REGISTRY.write("", "Logger", settings("speed", "fast"));
        final String emptied = SettingsFormat.REGISTRY.write(first, "Logger", settings());

        assertFalse(emptied.contains("Logger"), emptied);
    }

    @Test
    void ini_writesTheSectionAndASettingALine() {
        assertEquals("[Settings]\nspeed=fast\nsize=4\n",
                SettingsFormat.INI.write("", "LOGGER", settings("speed", "fast", "size", "4")));
    }

    @Test
    void plain_readsPastCommentsAndBlankLines() {
        assertEquals(settings("speed", "fast", "size", "4"),
                SettingsFormat.PLAIN.read("# written by hand\n\nspeed = fast\nsize=4\n", "logger"));
    }

    @Test
    void plist_writesAPropertyListOfStrings() {
        final String text = SettingsFormat.PLIST.write("", "Logger", settings("speed", "fast"));

        assertTrue(text.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<plist version=\"1.0\">\n<dict>\n"),
                text);
        assertTrue(text.contains("\t<key>speed</key>\n\t<string>fast</string>\n"), text);
    }

    @Test
    void validName_takesAPlainWordOnly() {
        assertTrue(SettingsFormat.validName("last.run-2_a"));
        assertFalse(SettingsFormat.validName(""));
        assertFalse(SettingsFormat.validName("two words"));
        assertFalse(SettingsFormat.validName("a=b"));
        assertFalse(SettingsFormat.validName("x".repeat(SettingsFormat.NAME_MOST + 1)));
    }

    @Test
    void validValue_takesOneLineOnly() {
        assertTrue(SettingsFormat.validValue("anything = at all"));
        assertTrue(SettingsFormat.validValue(""));
        assertFalse(SettingsFormat.validValue("two\nlines"));
        assertFalse(SettingsFormat.validValue("x".repeat(SettingsFormat.VALUE_MOST + 1)));
    }

    private static Map<String, String> settings(final String... pairs) {
        final Map<String, String> made = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            made.put(pairs[i], pairs[i + 1]);
        }
        return made;
    }
}
