/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.log.CoreLogs;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.slf4j.Logger;

/**
 * A condition any data file can carry: the data loads only while a setting has a value, so a modpack turns a recipe,
 * a loot table or anything else NeoForge loads with conditions on and off from a settings file, with no data pack of
 * its own. It names the file, the setting by its dotted path, and the value it asks for, a switch being on when no
 * value is given:
 *
 * <pre>{@code
 * "neoforge:conditions": [
 *   {"type": "jscore:setting", "file": "jstech-balance", "setting": "moving_structures.physics_tool_recipe"},
 *   {"type": "jscore:setting", "file": "jscomputers-server", "setting": "storage.mode", "value": "simple"}
 * ]
 * }</pre>
 *
 * A world's setting is read as the world's data loads, from the file beside the world, so the condition holds from the
 * world's first load; a change takes hold at the next {@code /reload}. A file or a setting that is not there, or a
 * player's own file, which no world's data may hang on, fails the condition, and the log says so once.
 *
 * @param file    the settings file, by its name without the extension
 * @param setting the setting, by its dotted path in the file
 * @param value   the value it asks for: a switch's {@code true} or {@code false}, a number, a word, or a list
 */
public record SettingCondition(String file, String setting, JsonElement value) implements ICondition {

    /** How it is written in a data file. */
    public static final MapCodec<SettingCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("file").forGetter(SettingCondition::file),
            Codec.STRING.fieldOf("setting").forGetter(SettingCondition::setting),
            ExtraCodecs.JSON.optionalFieldOf("value", new JsonPrimitive(true)).forGetter(SettingCondition::value)
    ).apply(instance, SettingCondition::new));

    private static final Logger LOGGER = CoreLogs.CONFIG.logger();
    /** What the log said already, so it says each once. */
    private static final Set<String> TOLD = ConcurrentHashMap.newKeySet();

    public SettingCondition {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(setting, "setting");
        Objects.requireNonNull(value, "value");
    }

    /** The condition that the switch {@code key} of {@code file} is on, for a mod's data generation. */
    public static SettingCondition on(final ConfigFile file, final ConfigKey<Boolean> key) {
        return new SettingCondition(file.name(), key.dottedPath(), new JsonPrimitive(true));
    }

    @Override
    public boolean test(final IContext context) {
        final Optional<ConfigFile> found = ConfigFiles.byName(this.file);
        if (found.isEmpty()) {
            if (firstTime()) {
                LOGGER.warn("A data file's condition on {} fails: no settings file {} is installed", this.setting,
                        this.file);
            }
            return false;
        }
        if (found.get().side() == ConfigSide.CLIENT) {
            if (firstTime()) {
                LOGGER.warn("A data file's condition on {} fails: {} is a player's own settings, which no world's "
                        + "data may hang on", this.setting, this.file);
            }
            return false;
        }
        final Optional<ConfigKey<?>> key = found.get().key(this.setting);
        if (key.isEmpty()) {
            if (firstTime()) {
                LOGGER.warn("A data file's condition fails: {} has no setting {}", this.file, this.setting);
            }
            return false;
        }
        return matches(plainOf(key.get(), found.get()), this.value);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return DataConditions.SETTING.get();
    }

    private static <T> Object plainOf(final ConfigKey<T> key, final ConfigFile file) {
        return key.plain(ConfigFiles.forData(file).get(key));
    }

    /** Whether a setting's plain value is the value asked for: words alike in any case, numbers by their value. */
    private static boolean matches(final Object plain, final JsonElement wanted) {
        if (wanted instanceof JsonArray list) {
            if (!(plain instanceof List<?> values) || values.size() != list.size()) {
                return false;
            }
            for (int i = 0; i < list.size(); i++) {
                if (!matches(values.get(i), list.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (!(wanted instanceof JsonPrimitive primitive)) {
            return false;
        }
        if (primitive.isBoolean()) {
            return plain instanceof Boolean flag && flag == primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            return plain instanceof Number number && Double.compare(number.doubleValue(), primitive.getAsDouble()) == 0;
        }
        return plain instanceof String word && word.equalsIgnoreCase(primitive.getAsString());
    }

    /** Whether the log has not yet said why this condition fails. */
    private boolean firstTime() {
        return TOLD.add(this.file + "/" + this.setting);
    }
}
