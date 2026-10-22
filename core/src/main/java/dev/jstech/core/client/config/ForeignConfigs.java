/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mojang.serialization.Codec;
import dev.jstech.core.config.ConfigApply;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The settings of the mods that keep theirs NeoForge's way rather than the Core's, read from their own declarations so
 * the Core's settings screen shows them beside its own: each of a mod's files that is loaded becomes a file the screen
 * knows, each setting a setting of its kind (on or off, a whole number, a number, a word, a word of a few, a list of
 * words), held to the range the mod gives it and marked when it takes hold only on a restart, named as the mod's
 * language names it and described as the mod comments it. What the screen changes is written into the mod's own
 * setting and its file saved by the mod's own means. A kind of setting the screen has no control for is left out.
 */
final class ForeignConfigs {

    /* The files made of other mods' settings, held no longer than the screen that shows them. */
    private static final Set<ConfigFile> MADE = Collections.newSetFromMap(new WeakHashMap<>());

    private ForeignConfigs() {
    }

    /** Whether {@code file} is made of another mod's settings, which that mod keeps. */
    static boolean isForeign(final ConfigFile file) {
        return MADE.contains(file);
    }

    /**
     * Every mod but {@code except} with settings of NeoForge's own, each with its files read now, by display name. A
     * mod that keeps its settings the Core's way has its own screen of them, and is left out, though the Core hands
     * its files to NeoForge too.
     */
    static List<Mod> all(final String except) {
        final Map<String, List<ModConfig>> byMod = new TreeMap<>();
        for (final ModConfig.Type type : ModConfig.Type.values()) {
            for (final ModConfig config : ModConfigs.getConfigSet(type)) {
                if (!config.getModId().equals(except) && ConfigFiles.of(config.getModId()).isEmpty()
                        && config.getSpec() instanceof ModConfigSpec && config.getLoadedConfig() != null) {
                    byMod.computeIfAbsent(config.getModId(), id -> new ArrayList<>()).add(config);
                }
            }
        }
        final List<Mod> mods = new ArrayList<>();
        byMod.forEach((modId, configs) -> {
            final List<File> files = new ArrayList<>();
            for (final ModConfig config : configs) {
                file(config).ifPresent(files::add);
            }
            if (!files.isEmpty()) {
                mods.add(new Mod(modId, ModList.get().getModContainerById(modId).map(container -> container
                        .getModInfo().getDisplayName()).orElse(modId), List.copyOf(files)));
            }
        });
        mods.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return mods;
    }

    /* A mod's file as the screen knows it, or none when it holds no setting the screen can show. */
    private static Optional<File> file(final ModConfig config) {
        final ModConfigSpec spec = (ModConfigSpec) config.getSpec();
        final List<Setting> settings = new ArrayList<>();
        gather(spec, spec.getValues(), settings);
        if (settings.isEmpty()) {
            return Optional.empty();
        }
        final String name = config.getFileName().replace('\\', '/');
        final ConfigFile.Builder builder = ConfigFile.builder(stem(name), sideOf(config.getType()), ConfigFormats.TOML);
        for (final Setting setting : settings) {
            setting.declare(builder);
        }
        final ConfigFile file = builder.build();
        final Map<String, Object> plain = new LinkedHashMap<>();
        plain.put(ConfigFile.VERSION_KEY, file.version());
        final Map<ConfigKey<?>, Setting> byKey = new IdentityHashMap<>();
        for (final Setting setting : settings) {
            setting.into(plain);
            byKey.put(setting.key(), setting);
        }
        file.read(plain);
        file.onSave(spec::save);
        MADE.add(file);
        return Optional.of(new File(file, name, Collections.unmodifiableMap(byKey)));
    }

    /* Every value of {@code values}, a section's, and of the sections in it, in the order the mod declares them. */
    private static void gather(final ModConfigSpec spec, final UnmodifiableConfig values,
                               final List<Setting> into) {
        for (final Object value : values.valueMap().values()) {
            if (value instanceof ModConfigSpec.ConfigValue<?> setting) {
                Setting.of(spec, setting).ifPresent(into::add);
            } else if (value instanceof UnmodifiableConfig section) {
                gather(spec, section, into);
            }
        }
    }

    private static ConfigSide sideOf(final ModConfig.Type type) {
        return switch (type) {
            case SERVER -> ConfigSide.SERVER;
            case CLIENT -> ConfigSide.CLIENT;
            default -> ConfigSide.COMMON;
        };
    }

    /** A file's name with neither its folder nor its extension: {@code mekanism/general.toml} is {@code general}. */
    static String stem(final String file) {
        final String name = file.substring(file.lastIndexOf('/') + 1);
        final int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /* A name written in camel case or with underscores, as words: "transporterSpeed" is "Transporter speed". */
    static String words(final String name) {
        final String spaced = name.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT)
                .strip();
        return spaced.isEmpty() ? name : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /**
     * A mod with settings of NeoForge's own.
     *
     * @param modId its id
     * @param name  what it calls itself
     * @param files its files that are loaded, each as the screen knows it
     */
    record Mod(String modId, String name, List<File> files) {

        /** How many settings its files of {@code side} hold. */
        int count(final ConfigSide side) {
            return this.files.stream().filter(file -> file.file().side() == side)
                    .mapToInt(file -> file.settings().size()).sum();
        }
    }

    /**
     * One of a mod's files.
     *
     * @param file     as the screen knows it
     * @param name     its name, with its folder, as the mod writes it
     * @param settings each setting it holds, by the screen's key for it
     */
    record File(ConfigFile file, String name, Map<ConfigKey<?>, Setting> settings) {
    }

    /**
     * One of a mod's settings: the screen's key for it, what it is called and what its comment says, and how a value
     * goes back into the mod's own setting.
     */
    static final class Setting {

        private final ConfigKey<Object> key;
        private final String title;
        private final String comment;
        private final Consumer<Object> back;
        private final Object value;

        @SuppressWarnings("unchecked")
        private <T> Setting(final ConfigKey<T> key, final String title, final String comment, final Object value,
                            final Consumer<T> back) {
            this.key = (ConfigKey<Object>) key;
            this.title = title;
            this.comment = comment;
            this.value = value;
            this.back = (Consumer<Object>) back;
        }

        ConfigKey<?> key() {
            return this.key;
        }

        /** What it is called, as the mod's language names it, or its name as words. */
        String title() {
            return this.title;
        }

        /** What its comment says, or empty. */
        String comment() {
            return this.comment;
        }

        private void declare(final ConfigFile.Builder builder) {
            builder.key(this.key, this.back);
        }

        private void into(final Map<String, Object> plain) {
            Map<String, Object> at = plain;
            final List<String> path = this.key.path();
            for (int i = 0; i < path.size() - 1; i++) {
                at = section(at, path.get(i));
            }
            at.put(path.get(path.size() - 1), this.key.plain(this.value));
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> section(final Map<String, Object> in, final String name) {
            return (Map<String, Object>) in.computeIfAbsent(name, ignored -> new LinkedHashMap<String, Object>());
        }

        /* The screen's setting for one of the mod's, or none for a kind it has no control for. */
        @SuppressWarnings({"unchecked", "rawtypes"})
        static Optional<Setting> of(final ModConfigSpec spec, final ModConfigSpec.ConfigValue<?> setting) {
            final List<String> path = setting.getPath();
            if (path.isEmpty() || path.stream().anyMatch(part -> part.isBlank() || part.contains("."))
                    || path.size() == 1 && path.get(0).equals(ConfigFile.VERSION_KEY)) {
                return Optional.empty();
            }
            final ModConfigSpec.ValueSpec declared = setting.getSpec();
            final String dotted = String.join(".", path);
            final String title = declared.getTranslationKey() != null && I18n.exists(declared.getTranslationKey())
                    ? I18n.get(declared.getTranslationKey()) : words(path.get(path.size() - 1));
            final String comment = declared.getComment() == null ? "" : declared.getComment().strip();
            final Object current = setting.getRaw();
            final Object fallback = setting.getDefault();
            final ModConfigSpec.Range<?> range = declared.getRange();
            ConfigKey<?> key;
            final Consumer<Object> back;
            switch (fallback) {
                case Boolean on -> {
                    key = ConfigKey.flag(dotted, on);
                    back = value -> ((ModConfigSpec.ConfigValue<Boolean>) setting).set((Boolean) value);
                }
                case Integer whole -> {
                    ConfigKey<Integer> made = ConfigKey.whole(dotted, whole);
                    if (range != null) {
                        made = made.range((Integer) range.getMin(), (Integer) range.getMax());
                    }
                    key = made;
                    back = value -> ((ModConfigSpec.ConfigValue<Integer>) setting).set((Integer) value);
                }
                case Long whole -> {
                    ConfigKey<Long> made = ConfigKey.wholeLong(dotted, whole);
                    if (range != null) {
                        made = made.range((Long) range.getMin(), (Long) range.getMax());
                    }
                    key = made;
                    back = value -> ((ModConfigSpec.ConfigValue<Long>) setting).set((Long) value);
                }
                case Double number -> {
                    ConfigKey<Double> made = ConfigKey.number(dotted, number);
                    if (range != null) {
                        made = made.range((Double) range.getMin(), (Double) range.getMax());
                    }
                    key = made;
                    back = value -> ((ModConfigSpec.ConfigValue<Double>) setting).set((Double) value);
                }
                case Enum<?> word -> {
                    final Class<? extends Enum> kind = word.getDeclaringClass();
                    final List<String> words = new ArrayList<>();
                    for (final Object constant : kind.getEnumConstants()) {
                        words.add(((Enum<?>) constant).name());
                    }
                    key = ConfigKey.text(dotted, word.name()).allowing(words.toArray(String[]::new));
                    back = value -> {
                        for (final Object constant : kind.getEnumConstants()) {
                            if (((Enum<?>) constant).name().equals(value)) {
                                ((ModConfigSpec.ConfigValue) setting).set(constant);
                            }
                        }
                    };
                }
                case String text -> {
                    key = ConfigKey.text(dotted, text);
                    back = value -> ((ModConfigSpec.ConfigValue<String>) setting).set((String) value);
                }
                case List<?> list when list.stream().allMatch(String.class::isInstance)
                        && ((List<?>) current).stream().allMatch(String.class::isInstance) -> {
                    key = ConfigKey.of(dotted, Codec.STRING.listOf(), (List<String>) list);
                    back = value -> ((ModConfigSpec.ConfigValue) setting).set(new ArrayList<>((List<?>) value));
                }
                default -> {
                    return Optional.empty();
                }
            }
            // A word of a few is shown as its name; the mod keeps it as its own kind.
            final Object shown = current instanceof Enum<?> word ? word.name() : current;
            final ConfigKey<?> named = key.named(title);
            return Optional.of(new Setting((ConfigKey<Object>) (declared.restartType()
                    == ModConfigSpec.RestartType.NONE ? named : named.appliesOn(ConfigApply.RESTART)), title,
                    comment, shown, back));
        }
    }
}
