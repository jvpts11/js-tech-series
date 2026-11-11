/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How a system keeps a program's own settings in a file, the way the real one did, so a player reading the file with
 * the system's own tools finds what they expect (computers/docs/design/sigma.md). Each reads the settings of one
 * program out of the file's text and writes them back into it; only the registry is shared by every program, so only
 * it keeps what the other programs wrote.
 *
 * <p>A setting's name is a plain word and its value one line, which every one of these holds as written.
 */
public enum SettingsFormat {

    /** Frames' registry: one file for every program, each program's settings under its own key, as regedit exports. */
    REGISTRY,
    /** An INI file of the program's own, as MC-DOS programs kept beside themselves. */
    INI,
    /** A file of the program's own with a setting a line, as Unix programs keep in ~/.config and services in /etc. */
    PLAIN,
    /** A property list of the program's own, as Quince OS X keeps in the Library's Preferences. */
    PLIST;

    /** The longest name a setting may have. */
    public static final int NAME_MOST = 64;
    /** The longest value a setting may hold. */
    public static final int VALUE_MOST = 1024;

    private static final String REGISTRY_HEADER = "REGEDIT4";
    private static final String REGISTRY_KEY = "[HKEY_LOCAL_MACHINE\\SOFTWARE\\%s]";
    private static final String INI_SECTION = "[Settings]";
    private static final String PLIST_HEAD =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<plist version=\"1.0\">\n<dict>\n";
    private static final String PLIST_TAIL = "</dict>\n</plist>\n";
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern PLIST_ENTRY = Pattern.compile("<key>(.*?)</key>\\s*<string>(.*?)</string>");
    /* A value of the registry as regedit writes it: "name"="value", a backslash before a quote or a backslash. */
    private static final Pattern REGISTRY_ENTRY =
            Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"=\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** Whether {@code name} may name a setting: letters, digits, dots, dashes and underscores, and not too long. */
    public static boolean validName(final String name) {
        return name.length() <= NAME_MOST && NAME.matcher(name).matches();
    }

    /** Whether {@code value} may be a setting's value: one line, and not too long. */
    public static boolean validValue(final String value) {
        return value.length() <= VALUE_MOST && value.indexOf('\n') < 0 && value.indexOf('\r') < 0;
    }

    /** The settings of {@code program} in the file whose text is {@code text}, in the order they are written. */
    public Map<String, String> read(final String text, final String program) {
        final Map<String, String> settings = new LinkedHashMap<>();
        switch (this) {
            case REGISTRY -> {
                final List<String> ours = sections(text).getOrDefault(registryKey(program), List.of());
                for (final String line : ours) {
                    final Matcher entry = REGISTRY_ENTRY.matcher(line.trim());
                    if (entry.matches()) {
                        settings.put(unescapeRegistry(entry.group(1)), unescapeRegistry(entry.group(2)));
                    }
                }
            }
            case INI, PLAIN -> {
                for (final String line : text.split("\n")) {
                    final String trimmed = line.trim();
                    final int equals = trimmed.indexOf('=');
                    if (equals > 0 && !trimmed.startsWith("[") && !trimmed.startsWith(";")
                            && !trimmed.startsWith("#")) {
                        settings.put(trimmed.substring(0, equals).trim(), trimmed.substring(equals + 1).trim());
                    }
                }
            }
            case PLIST -> {
                final Matcher entry = PLIST_ENTRY.matcher(text);
                while (entry.find()) {
                    settings.put(unescapeXml(entry.group(1)), unescapeXml(entry.group(2)));
                }
            }
        }
        return settings;
    }

    /**
     * The text of the file once {@code program}'s settings are {@code settings}: the registry keeps every other
     * program's key as it was; the files of one program are written whole.
     */
    public String write(final String text, final String program, final Map<String, String> settings) {
        final StringBuilder out = new StringBuilder();
        switch (this) {
            case REGISTRY -> {
                final Map<String, List<String>> sections = sections(text);
                final List<String> ours = new ArrayList<>();
                settings.forEach((name, value) -> ours.add("\"" + escapeRegistry(name) + "\"=\""
                        + escapeRegistry(value) + "\""));
                sections.remove(registryKey(program));
                if (!ours.isEmpty()) {
                    sections.put(registryKey(program), ours);
                }
                out.append(REGISTRY_HEADER).append('\n');
                sections.forEach((key, lines) -> {
                    out.append('\n').append(key).append('\n');
                    lines.forEach(line -> out.append(line).append('\n'));
                });
            }
            case INI -> {
                out.append(INI_SECTION).append('\n');
                settings.forEach((name, value) -> out.append(name).append('=').append(value).append('\n'));
            }
            case PLAIN -> settings.forEach((name, value) -> out.append(name).append('=').append(value).append('\n'));
            case PLIST -> {
                out.append(PLIST_HEAD);
                settings.forEach((name, value) -> out.append("\t<key>").append(escapeXml(name)).append("</key>\n")
                        .append("\t<string>").append(escapeXml(value)).append("</string>\n"));
                out.append(PLIST_TAIL);
            }
        }
        return out.toString();
    }

    /* The key a program's settings sit under in the registry. */
    private static String registryKey(final String program) {
        return String.format(REGISTRY_KEY, program);
    }

    /* The registry's keys, each with the lines under it, in the order they are written; the header is left out. */
    private static Map<String, List<String>> sections(final String text) {
        final Map<String, List<String>> sections = new LinkedHashMap<>();
        List<String> current = null;
        for (final String line : text.split("\n")) {
            final String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                current = sections.computeIfAbsent(trimmed, key -> new ArrayList<>());
            } else if (current != null && !trimmed.isEmpty()) {
                current.add(trimmed);
            }
        }
        return sections;
    }

    private static String escapeRegistry(final String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unescapeRegistry(final String text) {
        final StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                i++;
                out.append(text.charAt(i));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String escapeXml(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String unescapeXml(final String text) {
        return text.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }
}
