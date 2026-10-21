/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.registry.CoreItems;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A shaped recipe that a setting turns on and off: it matches only while the setting it names is on, so a server
 * that takes an item out of survival does it by a setting, with no data pack, and the recipe comes back the moment
 * the setting is turned on again. It is written as a shaped recipe is, with the settings file and the setting:
 *
 * <pre>{@code
 * {"type": "jscore:crafting_shaped_switch", "file": "jstech-balance",
 *  "setting": "moving_structures.physics_tool_recipe", "pattern": [...], "key": {...}, "result": {...}}
 * }</pre>
 *
 * A setting that is not there, or is not a switch, keeps the recipe on.
 */
public final class SwitchedShapedRecipe extends ShapedRecipe {

    private final String file;
    private final String setting;

    public static final MapCodec<SwitchedShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("file").forGetter(SwitchedShapedRecipe::file),
            Codec.STRING.fieldOf("setting").forGetter(SwitchedShapedRecipe::setting),
            ShapedRecipe.Serializer.CODEC.forGetter(recipe -> recipe)).apply(instance, SwitchedShapedRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchedShapedRecipe> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SwitchedShapedRecipe::file, ByteBufCodecs.STRING_UTF8,
                    SwitchedShapedRecipe::setting, ShapedRecipe.Serializer.STREAM_CODEC, recipe -> recipe,
                    SwitchedShapedRecipe::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SwitchedShapedRecipe>> SERIALIZER =
            CoreItems.CONTENT.recipeSerializer("crafting_shaped_switch", () -> new RecipeSerializer<>() {
                @Override
                public MapCodec<SwitchedShapedRecipe> codec() {
                    return CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, SwitchedShapedRecipe> streamCodec() {
                    return STREAM_CODEC;
                }
            });

    public SwitchedShapedRecipe(final String file, final String setting, final ShapedRecipe shaped) {
        super(shaped.getGroup(), shaped.category(), shaped.pattern, shaped.getResultItem(RegistryAccess.EMPTY),
                shaped.showNotification());
        this.file = file;
        this.setting = setting;
    }

    /** Declares the recipe's serializer, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** The settings file the setting is in. */
    public String file() {
        return file;
    }

    /** The setting that turns the recipe on and off, by its dotted path in its file. */
    public String setting() {
        return setting;
    }

    /** Whether the setting turns the recipe on now. */
    public boolean on() {
        final Optional<ConfigFile> found = ConfigFiles.byName(file);
        if (found.isEmpty()) {
            return true;
        }
        final Optional<ConfigKey<?>> key = found.get().key(setting);
        return key.isEmpty() || !(found.get().get(key.get()) instanceof Boolean value) || value;
    }

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        return on() && super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }
}
