/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.jstech.core.command.CommandRoot;
import dev.jstech.core.config.format.ConfigFormatException;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

/**
 * {@code /jstech config}, for the operators: {@code get <file> <setting>}, a setting's value; {@code set <file>
 * <setting> <value>}, changing it for every player at once; {@code reset <file> <setting>}, back to its default; and
 * {@code preset <file> <name>}, picking one of the file's presets. It reaches the files this side keeps: a world's, and
 * those of every game.
 */
@TextHolder
public final class ConfigCommands {

    /* A setting's path, its file, and its value. */
    public static final TextKey VALUE = TextKey.of("jscore.command.config.value", "%s of %s is %s");
    public static final TextKey SET = TextKey.of("jscore.command.config.set", "%s of %s is now %s");
    public static final TextKey SET_ON_RESTART = TextKey.of("jscore.command.config.set_on_restart",
            "%s of %s is now %s; it takes hold when the game starts again");
    public static final TextKey RESET = TextKey.of("jscore.command.config.reset", "%s of %s is back to %s");
    /* The preset's name, the file, and how many settings it changed. */
    public static final TextKey PRESET = TextKey.of("jscore.command.config.preset",
            "Picked the preset %s of %s: %s settings set");
    public static final TextKey NO_FILE = TextKey.of("jscore.command.config.no_file",
            "No settings file here is named %s");
    public static final TextKey NO_SETTING = TextKey.of("jscore.command.config.no_setting",
            "That file has no setting %s");
    public static final TextKey NO_PRESET = TextKey.of("jscore.command.config.no_preset",
            "That file has no preset %s");
    /* The value written, and why it was not taken. */
    public static final TextKey NOT_TAKEN = TextKey.of("jscore.command.config.not_taken",
            "%s cannot be that setting's value: %s");

    private static final String FILE = "file";
    private static final String SETTING = "setting";
    private static final String VALUE_ARGUMENT = "value";
    private static final String NAME = "name";
    /** A value written in a command is read as the one value of a JSON object around it. */
    private static final String WRAP_KEY = "value";
    private static final DynamicCommandExceptionType NO_SUCH_FILE =
            new DynamicCommandExceptionType(name -> GameText.component(NO_FILE.with(String.valueOf(name))));
    private static final DynamicCommandExceptionType NO_SUCH_SETTING =
            new DynamicCommandExceptionType(path -> GameText.component(NO_SETTING.with(String.valueOf(path))));
    private static final DynamicCommandExceptionType NO_SUCH_PRESET =
            new DynamicCommandExceptionType(name -> GameText.component(NO_PRESET.with(String.valueOf(name))));
    private static final DynamicCommandExceptionType REFUSED =
            new DynamicCommandExceptionType(text -> GameText.component((Text) text));
    private static final SuggestionProvider<CommandSourceStack> FILES = (context, builder) ->
            SharedSuggestionProvider.suggest(reachable().map(ConfigFile::name), builder);
    private static final SuggestionProvider<CommandSourceStack> SETTINGS = (context, builder) -> {
        final Optional<ConfigFile> file = ConfigFiles.byName(StringArgumentType.getString(context, FILE));
        return file.isEmpty() ? builder.buildFuture()
                : SharedSuggestionProvider.suggest(file.get().keys().stream().map(ConfigKey::dottedPath), builder);
    };
    private static final SuggestionProvider<CommandSourceStack> PRESETS = (context, builder) -> {
        final Optional<ConfigFile> file = ConfigFiles.byName(StringArgumentType.getString(context, FILE));
        if (file.isEmpty()) {
            return builder.buildFuture();
        }
        return SharedSuggestionProvider.suggest(Stream.concat(file.get().presets().stream().map(ConfigPreset::name),
                ConfigPresets.saved(file.get()).stream()), builder);
    };

    private ConfigCommands() {
    }

    /** Declares the command, from the Core's constructor. */
    public static void declare() {
        CommandRoot.CORE.declare("config", CorePermissions.OPERATORS, (node, context) -> node
                .then(Commands.literal("get").then(Commands.argument(FILE, StringArgumentType.word()).suggests(FILES)
                        .then(Commands.argument(SETTING, StringArgumentType.word()).suggests(SETTINGS)
                                .executes(ConfigCommands::get))))
                .then(Commands.literal("set").then(Commands.argument(FILE, StringArgumentType.word()).suggests(FILES)
                        .then(Commands.argument(SETTING, StringArgumentType.word()).suggests(SETTINGS)
                                .then(Commands.argument(VALUE_ARGUMENT, StringArgumentType.greedyString())
                                        .executes(ConfigCommands::set)))))
                .then(Commands.literal("reset").then(Commands.argument(FILE, StringArgumentType.word())
                        .suggests(FILES).then(Commands.argument(SETTING, StringArgumentType.word())
                                .suggests(SETTINGS).executes(ConfigCommands::reset))))
                .then(Commands.literal("preset").then(Commands.argument(FILE, StringArgumentType.word())
                        .suggests(FILES).then(Commands.argument(NAME, StringArgumentType.greedyString())
                                .suggests(PRESETS).executes(ConfigCommands::preset)))));
    }

    /**
     * What a person wrote as a setting's value, as a file writes it: read as JSON ({@code true}, {@code 12},
     * {@code ["a", "b"]}, {@code "a word"}), and as the words themselves when it is not JSON.
     */
    public static Object readValue(final String written) {
        final String wrapped = "{\"" + WRAP_KEY + "\": " + written + "}";
        try {
            final Object read = ConfigFormats.JSON.read(wrapped.getBytes(StandardCharsets.UTF_8)).get(WRAP_KEY);
            return read == null ? written : read;
        } catch (final ConfigFormatException notJson) {
            return written;
        }
    }

    /** A value as a file writes it, shown in a line: words as they are, lists and maps in brackets. */
    public static String show(final Object plain) {
        if (plain instanceof List<?> list) {
            return list.stream().map(ConfigCommands::show).collect(Collectors.joining(", ", "[", "]"));
        }
        if (plain instanceof Map<?, ?> map) {
            final List<String> entries = new ArrayList<>();
            map.forEach((name, value) -> entries.add(name + ": " + show(value)));
            return "{" + String.join(", ", entries) + "}";
        }
        return String.valueOf(plain);
    }

    private static int get(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ConfigFile file = file(context);
        final ConfigKey<?> key = key(context, file);
        final String shown = show(plainOf(file, key));
        context.getSource().sendSuccess(() -> GameText.component(VALUE.with(key.dottedPath(), file.fileName(),
                shown)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int set(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ConfigFile file = file(context);
        final ConfigKey<?> key = key(context, file);
        final String written = StringArgumentType.getString(context, VALUE_ARGUMENT);
        final Optional<String> problem = ConfigSync.stage(file, key, readValue(written));
        if (problem.isPresent()) {
            throw REFUSED.create(NOT_TAKEN.with(written, problem.get()));
        }
        file.save();
        final String shown = show(plainOf(file, key));
        final TextKey said = key.traits().applies() == ConfigApply.RESTART ? SET_ON_RESTART : SET;
        context.getSource().sendSuccess(() -> GameText.component(said.with(key.dottedPath(), file.fileName(),
                shown)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ConfigFile file = file(context);
        final ConfigKey<?> key = key(context, file);
        toDefault(file, key);
        final String shown = show(plainOf(file, key));
        context.getSource().sendSuccess(() -> GameText.component(RESET.with(key.dottedPath(), file.fileName(),
                shown)), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int preset(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ConfigFile file = file(context);
        final String name = StringArgumentType.getString(context, NAME);
        final Map<String, Object> values = ConfigPresets.find(file, name).orElseThrow(() -> NO_SUCH_PRESET.create(
                name));
        final ConfigDraft draft = new ConfigDraft(file);
        final int taken = draft.takePlain(values);
        draft.apply();
        context.getSource().sendSuccess(() -> GameText.component(PRESET.with(name, file.fileName(), taken)), true);
        return taken;
    }

    /* The files a command reaches here: a world's, and those of every game, as this side keeps them. */
    private static Stream<ConfigFile> reachable() {
        return ConfigFiles.all().stream().filter(file -> file.side() != ConfigSide.CLIENT);
    }

    private static ConfigFile file(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final String name = StringArgumentType.getString(context, FILE);
        return ConfigFiles.byName(name).filter(file -> file.side() != ConfigSide.CLIENT)
                .orElseThrow(() -> NO_SUCH_FILE.create(name));
    }

    private static ConfigKey<?> key(final CommandContext<CommandSourceStack> context, final ConfigFile file)
            throws CommandSyntaxException {
        final String path = StringArgumentType.getString(context, SETTING);
        return file.key(path).orElseThrow(() -> NO_SUCH_SETTING.create(path));
    }

    private static <T> Object plainOf(final ConfigFile file, final ConfigKey<T> key) {
        return key.plain(file.get(key));
    }

    private static <T> void toDefault(final ConfigFile file, final ConfigKey<T> key) {
        file.set(key, key.defaultValue());
    }
}
