/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.config.format.ConfigFormatException;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.config.format.IConfigComments;
import dev.jstech.core.config.format.IConfigFormat;
import dev.jstech.core.config.format.NbtConfigFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The presets a person keeps beside the game, and settings written out as text to share: a preset is a file of the
 * same format as the settings it is for, in {@code config/jstech-presets/<file>/}, holding the values of some or all of
 * them; text is the file's own format, or JSON for a file kept in a format that is not text.
 *
 * <p>A preset the mod ships with the file is {@link ConfigFile#presets()}; this is the ones people make.
 */
public final class ConfigPresets {

    /** The folder in the game's config folder the presets people make live in, a folder for each file. */
    public static final String FOLDER = "jstech-presets";

    /** A preset's name: letters, digits, spaces, dashes and underscores, as many as a file name takes comfortably. */
    private static final Pattern NAME = Pattern.compile("[\\p{L}\\p{N} _-]{1,48}");
    /** The formats an imported text is tried in, after the file's own. */
    private static final List<IConfigFormat> TEXT_FORMATS = List.of(ConfigFormats.JSON, ConfigFormats.TOML,
            ConfigFormats.JSON5, ConfigFormats.YAML);

    private ConfigPresets() {
    }

    /** Whether {@code name} may name a preset. */
    public static boolean validName(final String name) {
        return name != null && NAME.matcher(name).matches() && !name.isBlank();
    }

    /** The folder the presets made for {@code file} live in. */
    public static Path folder(final ConfigFile file) {
        return FMLPaths.CONFIGDIR.get().resolve(FOLDER).resolve(file.name());
    }

    /** The names of the presets made for {@code file}, in the order of their names. */
    public static List<String> saved(final ConfigFile file) {
        final Path folder = folder(file);
        final List<String> out = new ArrayList<>();
        if (!Files.isDirectory(folder)) {
            return out;
        }
        final String ending = "." + file.format().extension();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(folder, "*" + ending)) {
            for (final Path entry : entries) {
                final String fileName = entry.getFileName().toString();
                out.add(fileName.substring(0, fileName.length() - ending.length()));
            }
        } catch (final IOException e) {
            ConfigFiles.LOGGER.warn("the presets in {} could not be listed", folder, e);
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /**
     * Keeps {@code plain}, a file's values as it holds them, as the preset {@code name} of {@code file}, written over
     * one of the same name.
     *
     * @throws IOException when it cannot be written
     */
    public static void save(final ConfigFile file, final String name, final Map<String, Object> plain)
            throws IOException {
        if (!validName(name)) {
            throw new IOException("a preset cannot be named " + name);
        }
        KeptConfigFiles.writeWhole(pathOf(file, name), file.format().write(plain, file::commentAt));
    }

    /** The values the preset {@code name} of {@code file} holds, as a file holds them, or none when it has none. */
    public static Optional<Map<String, Object>> load(final ConfigFile file, final String name) {
        final Path path = pathOf(file, name);
        if (!validName(name) || !Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(file.format().read(Files.readAllBytes(path)));
        } catch (final IOException | ConfigFormatException e) {
            ConfigFiles.LOGGER.warn("the preset {} could not be read", path, e);
            return Optional.empty();
        }
    }

    /** Throws away the preset {@code name} of {@code file}; whether there was one. */
    public static boolean delete(final ConfigFile file, final String name) {
        try {
            return validName(name) && Files.deleteIfExists(pathOf(file, name));
        } catch (final IOException e) {
            ConfigFiles.LOGGER.warn("the preset {} of {} could not be thrown away", name, file.fileName(), e);
            return false;
        }
    }

    /**
     * The values of a whole file, or the preset {@code name} the mod ships with it, or one made for it: what picking
     * {@code name} takes, by the shipped ones first.
     */
    public static Optional<Map<String, Object>> find(final ConfigFile file, final String name) {
        final Optional<ConfigPreset> shipped = file.preset(name);
        return shipped.isPresent() ? Optional.of(shipped.get().plain()) : load(file, name);
    }

    /** {@code plain} written as text to share: in the file's own format, or JSON when that is not text. */
    public static String export(final ConfigFile file, final Map<String, Object> plain) {
        final IConfigFormat format = textFormat(file);
        final IConfigComments comments = format.keepsComments() ? file::commentAt : IConfigComments.NONE;
        return new String(format.write(plain, comments), StandardCharsets.UTF_8);
    }

    /**
     * Text shared from {@link #export}, read back into plain values: tried in the file's own format first, then in each
     * other text format, so a person who pastes JSON into a TOML file's settings is understood.
     *
     * @throws ConfigFormatException when no format reads it, saying why the file's own did not
     */
    public static Map<String, Object> importText(final ConfigFile file, final String text)
            throws ConfigFormatException {
        final byte[] bytes = text.strip().getBytes(StandardCharsets.UTF_8);
        final IConfigFormat own = textFormat(file);
        try {
            return own.read(bytes);
        } catch (final ConfigFormatException ownFailed) {
            for (final IConfigFormat other : TEXT_FORMATS) {
                if (other == own) {
                    continue;
                }
                try {
                    return other.read(bytes);
                } catch (final ConfigFormatException notThisOne) {
                    // Tried in the next.
                }
            }
            throw ownFailed;
        }
    }

    private static Path pathOf(final ConfigFile file, final String name) {
        return folder(file).resolve(name + "." + file.format().extension());
    }

    private static IConfigFormat textFormat(final ConfigFile file) {
        return file.format() == NbtConfigFormat.INSTANCE ? ConfigFormats.JSON : file.format();
    }
}
