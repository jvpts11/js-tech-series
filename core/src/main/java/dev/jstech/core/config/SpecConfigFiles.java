/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.JsCore;
import dev.jstech.core.config.format.ConfigFormatException;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.config.format.TomlConfigFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The TOML settings files, handed to NeoForge as its own: built into a {@link ModConfigSpec} from the file's settings,
 * so NeoForge writes them with their comments, shows them in its settings screen, keeps a world's copy in step with
 * the players and reads a file again when somebody edits it; each reading is then taken by the file's settings.
 *
 * <p>NeoForge opens a file before any mod can see it and drops what its settings do not name, so a file's upgrade
 * steps run on it on disk first, in the game's config folder, while the mod is being built. A world's own copy of a
 * world's settings, which NeoForge reads from beside the world when there is one, is opened before that: its values
 * still take the steps that change a value, but a renamed setting there keeps only the default.
 */
final class SpecConfigFiles {

    private SpecConfigFiles() {
    }

    static void register(final ConfigFile file, final IEventBus modEventBus, final ModContainer modContainer) {
        upgradeOnDisk(file, FMLPaths.CONFIGDIR.get().resolve(file.fileName()));
        final Spec spec = build(file, modContainer.getModId());
        modContainer.registerConfig(typeOf(file.side()), spec.spec(), file.fileName());
        modEventBus.addListener(ModConfigEvent.Loading.class, event -> {
            if (event.getConfig().getSpec() == spec.spec()) {
                take(file, spec);
            }
        });
        modEventBus.addListener(ModConfigEvent.Reloading.class, event -> {
            if (event.getConfig().getSpec() == spec.spec()) {
                take(file, spec);
            }
        });
        modEventBus.addListener(ModConfigEvent.Unloading.class, event -> {
            if (event.getConfig().getSpec() == spec.spec()) {
                file.reset();
            }
        });
        file.onSave(() -> save(file, spec));
    }

    private static ModConfig.Type typeOf(final ConfigSide side) {
        return switch (side) {
            case CLIENT -> ModConfig.Type.CLIENT;
            case COMMON -> ModConfig.Type.COMMON;
            case SERVER -> ModConfig.Type.SERVER;
        };
    }

    /** An older file in the config folder, upgraded and written back before NeoForge opens it. */
    private static void upgradeOnDisk(final ConfigFile file, final Path path) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            final Map<String, Object> plain = ConfigFormats.TOML.read(Files.readAllBytes(path));
            final Object found = plain.get(ConfigFile.VERSION_KEY);
            final int version = found instanceof Number number ? number.intValue() : 0;
            if (version < file.version()) {
                file.read(plain);
                Files.write(path, file.write());
                file.reset();
            }
        } catch (final ConfigFormatException | IOException e) {
            // NeoForge says the same when it opens the file, and puts it right its own way.
            JsCore.LOGGER.warn("{} could not be read to upgrade it ({})", path, e.getMessage());
        }
    }

    /**
     * The file's settings as NeoForge's: the version first, then each section and setting with its comment, and each
     * under the key its name is translated by on NeoForge's settings screen ({@link ConfigTexts}).
     */
    private static Spec build(final ConfigFile file, final String modId) {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        final List<String> top = new ArrayList<>(file.comment());
        if (!top.isEmpty()) {
            top.add("");
        }
        top.addAll(file.commentAt(List.of(ConfigFile.VERSION_KEY)));
        builder.comment(top.toArray(String[]::new));
        builder.translation(ConfigTexts.VERSION.key());
        final ModConfigSpec.ConfigValue<Integer> version =
                builder.defineInRange(ConfigFile.VERSION_KEY, file.version(), 0, Integer.MAX_VALUE);
        final Map<String, ModConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
        List<String> open = List.of();
        for (final ConfigKey<?> key : file.keys()) {
            final List<String> section = key.path().subList(0, key.path().size() - 1);
            int shared = 0;
            while (shared < open.size() && shared < section.size() && open.get(shared).equals(section.get(shared))) {
                shared++;
            }
            for (int i = open.size(); i > shared; i--) {
                builder.pop();
            }
            for (int i = shared; i < section.size(); i++) {
                final String dotted = String.join(".", section.subList(0, i + 1));
                final List<String> lines = file.sections().getOrDefault(dotted, List.of());
                if (!lines.isEmpty()) {
                    builder.comment(lines.toArray(String[]::new));
                }
                builder.translation(ConfigTexts.key(modId, dotted));
                builder.push(section.get(i));
            }
            open = section;
            if (!key.comment().isEmpty()) {
                builder.comment(key.comment().toArray(String[]::new));
            }
            builder.translation(ConfigTexts.key(modId, key.dottedPath()));
            values.put(key.dottedPath(), define(builder, key));
        }
        for (int i = 0; i < open.size(); i++) {
            builder.pop();
        }
        return new Spec(builder.build(), version, values);
    }

    /**
     * One setting as NeoForge's: a number in its range (NeoForge pulls a value outside it to the nearer end, as the
     * Core does), a word in its list, a plain value as itself, and anything else as what its codec writes, which the
     * setting's own reading checks.
     */
    private static ModConfigSpec.ConfigValue<?> define(final ModConfigSpec.Builder builder, final ConfigKey<?> key) {
        final Object defaultValue = key.defaultValue();
        if (key.range().isPresent()) {
            final ConfigKeyRange<?> range = key.range().get();
            if (defaultValue instanceof Integer value) {
                return builder.defineInRange(key.name(), value, (Integer) range.min(), (Integer) range.max());
            }
            if (defaultValue instanceof Long value) {
                return builder.defineInRange(key.name(), value, (Long) range.min(), (Long) range.max());
            }
            if (defaultValue instanceof Double value) {
                return builder.defineInRange(key.name(), value, (Double) range.min(), (Double) range.max());
            }
        }
        if (key.allowed().isPresent()) {
            /*
             * A copy that can be asked about null: NeoForge asks the list whether it holds a value it has not read
             * yet, and a list made by List.of throws on that question instead of answering no.
             */
            return builder.defineInList(key.name(), (String) defaultValue, new ArrayList<>(key.allowed().get()));
        }
        if (defaultValue instanceof Boolean value) {
            return builder.define(key.name(), value.booleanValue());
        }
        if (defaultValue instanceof Number || defaultValue instanceof String) {
            return builder.define(key.name(), defaultValue);
        }
        final Object written = TomlConfigFormat.tomlValue(plainDefault(key));
        return builder.define(List.of(key.name()), () -> written,
                raw -> raw != null && key.read(TomlConfigFormat.plainValue(raw)).result().isPresent());
    }

    private static <T> Object plainDefault(final ConfigKey<T> key) {
        return key.plain(key.defaultValue());
    }

    /** Takes what NeoForge read; when that needed putting right (an older copy, a value refused), writes it back. */
    private static void take(final ConfigFile file, final Spec spec) {
        final Map<String, Object> plain = new LinkedHashMap<>();
        plain.put(ConfigFile.VERSION_KEY, spec.version().get());
        for (final ConfigKey<?> key : file.keys()) {
            ConfigTree.put(plain, key.path(), TomlConfigFormat.plainValue(spec.values().get(key.dottedPath()).get()));
        }
        if (file.read(plain).rewrite()) {
            save(file, spec);
        }
    }

    /** Puts the file's values into NeoForge's settings and has NeoForge write the file. */
    @SuppressWarnings("unchecked")
    private static void save(final ConfigFile file, final Spec spec) {
        if (file.newer() || !spec.spec().isLoaded()) {
            return;
        }
        spec.version().set(file.version());
        for (final ConfigKey<?> key : file.keys()) {
            final ModConfigSpec.ConfigValue<Object> value =
                    (ModConfigSpec.ConfigValue<Object>) spec.values().get(key.dottedPath());
            value.set(TomlConfigFormat.tomlValue(plainNow(file, key)));
        }
        spec.spec().save();
    }

    private static <T> Object plainNow(final ConfigFile file, final ConfigKey<T> key) {
        return key.plain(file.get(key));
    }

    /** NeoForge's settings for one file, with the value of its version and of each of its settings. */
    private record Spec(ModConfigSpec spec, ModConfigSpec.ConfigValue<Integer> version,
                        Map<String, ModConfigSpec.ConfigValue<?>> values) {
    }
}
