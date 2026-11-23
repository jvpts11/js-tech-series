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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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

    /* Whether this thread is having NeoForge write a file, so the reading it tells of at once is the Core's own. */
    private static final ThreadLocal<Boolean> SAVING = ThreadLocal.withInitial(() -> false);

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
            if (event.getConfig().getSpec() == spec.spec() && !SAVING.get()) {
                retake(file, spec);
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
                KeptConfigFiles.writeWhole(path, file.write());
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
        return new Spec(builder.build(), version, values, new Writes());
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
        if (file.read(read(file, spec)).rewrite()) {
            save(file, spec);
        }
    }

    /*
     * Takes what NeoForge read again, unless it is what the Core itself wrote a moment ago. NeoForge reads a file again
     * on a thread of its own whenever it changes on disk, the Core's own writing too, and a reading that began before
     * a later writing brings back the values that writing replaced: a setting changed twice in quick succession went
     * back to the first. Such a reading is let pass and the values now are written again; an edit made to the file is
     * taken as before.
     */
    private static void retake(final ConfigFile file, final Spec spec) {
        final Map<String, String> read = Writes.compared(read(file, spec));
        if (!spec.writes().wrote(read)) {
            take(file, spec);
        } else if (!spec.writes().last(read)) {
            save(file, spec);
        }
    }

    /* The file's values as NeoForge holds them now, in the shape the file's settings read them in. */
    private static Map<String, Object> read(final ConfigFile file, final Spec spec) {
        final Map<String, Object> plain = new LinkedHashMap<>();
        plain.put(ConfigFile.VERSION_KEY, spec.version().get());
        for (final ConfigKey<?> key : file.keys()) {
            ConfigTree.put(plain, key.path(), TomlConfigFormat.plainValue(spec.values().get(key.dottedPath()).get()));
        }
        return plain;
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
        spec.writes().add(Writes.compared(read(file, spec)));
        // NeoForge tells the file's settings of its own writing at once, with the values just put in.
        SAVING.set(true);
        try {
            spec.spec().save();
        } finally {
            SAVING.set(false);
        }
    }

    private static <T> Object plainNow(final ConfigFile file, final ConfigKey<T> key) {
        return key.plain(file.get(key));
    }

    /**
     * NeoForge's settings for one file, with the value of its version and of each of its settings, and what the Core
     * wrote to it lately.
     */
    private record Spec(ModConfigSpec spec, ModConfigSpec.ConfigValue<Integer> version,
                        Map<String, ModConfigSpec.ConfigValue<?>> values, Writes writes) {
    }

    /* The values the Core wrote to one file in the last moments, newest last, to know them when NeoForge reads one. */
    private static final class Writes {

        private final Deque<Written> recent = new ArrayDeque<>();

        /* How long a writing is remembered, and how many at most. */
        private static final long REMEMBERED_NANOS = 10_000_000_000L;
        private static final int MOST = 16;

        /* Values to compare by what they say, so a number read back as another kind of number is the same value. */
        static Map<String, String> compared(final Map<String, Object> plain) {
            final Map<String, String> out = new LinkedHashMap<>();
            plain.forEach((name, value) -> out.put(name, String.valueOf(value)));
            return out;
        }

        synchronized void add(final Map<String, String> values) {
            this.recent.addLast(new Written(values, System.nanoTime()));
            while (this.recent.size() > MOST) {
                this.recent.removeFirst();
            }
        }

        /* Whether the Core wrote {@code values} in the last moments. */
        synchronized boolean wrote(final Map<String, String> values) {
            final long now = System.nanoTime();
            for (final Written written : this.recent) {
                if (now - written.at() <= REMEMBERED_NANOS && written.values().equals(values)) {
                    return true;
                }
            }
            return false;
        }

        /* Whether {@code values} are the last the Core wrote. */
        synchronized boolean last(final Map<String, String> values) {
            return !this.recent.isEmpty() && this.recent.peekLast().values().equals(values);
        }
    }

    private record Written(Map<String, String> values, long at) {
    }
}
