/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TOML, read and written with NightConfig, the library NeoForge brings and writes its own settings with. Comments go
 * above the value or the table they describe, and the file's own comment above everything.
 */
public final class TomlConfigFormat implements IConfigFormat {

    TomlConfigFormat() {
    }

    @Override
    public String extension() {
        return "toml";
    }

    @Override
    public boolean keepsComments() {
        return true;
    }

    @Override
    public Map<String, Object> read(final byte[] file) throws ConfigFormatException {
        final CommentedConfig config = CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());
        try {
            TomlFormat.instance().createParser().parse(new StringReader(new String(file, StandardCharsets.UTF_8)),
                    config, ParsingMode.REPLACE);
        } catch (final ParsingException e) {
            throw new ConfigFormatException("not TOML: " + e.getMessage(), e);
        }
        return plain(config);
    }

    @Override
    public byte[] write(final Map<String, Object> values, final IConfigComments comments) {
        final CommentedConfig config = CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());
        fill(config, values, List.of(), comments);
        final TomlWriter writer = TomlFormat.instance().createWriter();
        final StringBuilder out = new StringBuilder();
        final List<String> header = comments.at(List.of());
        for (final String line : header) {
            out.append(line.isEmpty() ? "#" : "# " + line).append('\n');
        }
        if (!header.isEmpty()) {
            out.append('\n');
        }
        out.append(writer.writeToString(config));
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * A value as NightConfig holds it (a table, an array, a number...) in plain values; NeoForge's settings hand theirs
     * over this way.
     */
    public static Object plainValue(final Object value) {
        if (value instanceof UnmodifiableConfig table) {
            return plain(table);
        }
        if (value instanceof List<?> list) {
            final List<Object> out = new ArrayList<>(list.size());
            for (final Object element : list) {
                out.add(plainValue(element));
            }
            return out;
        }
        return PlainValues.value(value);
    }

    /** A plain value as NightConfig holds it in a TOML file: a map becomes a table, in the order its keys have. */
    public static Object tomlValue(final Object value) {
        return tomlValue(CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance()), value, List.of(),
                IConfigComments.NONE);
    }

    /** A table in plain values, its tables as maps and its arrays of tables as lists of maps. */
    private static Map<String, Object> plain(final UnmodifiableConfig config) {
        final Map<String, Object> out = new LinkedHashMap<>();
        for (final UnmodifiableConfig.Entry entry : config.entrySet()) {
            final Object value = plainValue(entry.getRawValue());
            if (value != null) {
                out.put(entry.getKey(), value);
            }
        }
        return out;
    }

    private static void fill(final CommentedConfig config, final Map<String, Object> values, final List<String> at,
                             final IConfigComments comments) {
        for (final Map.Entry<String, Object> entry : values.entrySet()) {
            final List<String> path = new ArrayList<>(at);
            path.add(entry.getKey());
            final List<String> key = List.of(entry.getKey());
            config.set(key, tomlValue(config, entry.getValue(), path, comments));
            final List<String> lines = comments.at(path);
            if (!lines.isEmpty()) {
                config.setComment(key, " " + String.join("\n ", lines));
            }
        }
    }

    private static Object tomlValue(final CommentedConfig parent, final Object value, final List<String> path,
                                    final IConfigComments comments) {
        if (value instanceof Map<?, ?> map) {
            final CommentedConfig table = parent.createSubConfig();
            fill(table, PlainValues.map(map), path, comments);
            return table;
        }
        if (value instanceof List<?> list) {
            final List<Object> out = new ArrayList<>(list.size());
            for (final Object element : list) {
                out.add(tomlValue(parent, element, path, IConfigComments.NONE));
            }
            return out;
        }
        return value;
    }
}
