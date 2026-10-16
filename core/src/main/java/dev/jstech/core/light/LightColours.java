/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The colour of light, as data maps a mod or a data pack fills, sent to every player: the colour each block that gives
 * light gives it in ({@code data/<namespace>/data_maps/block/light_colours.json}), the light an item gives while it is
 * held or lies on the ground ({@code item/held_lights.json}), and the light a kind of mob gives
 * ({@code entity_type/entity_lights.json}). A block the map says nothing of gives white light, which tints nothing,
 * as the game's own warm light; a held block that gives light gives its own level in its colour.
 *
 * <pre>{@code
 * { "values": { "minecraft:redstone_torch": "#FF3018", "mymod:alarm_lamp": "#FF2010" } }
 * { "values": { "mymod:flashlight": { "level": 13, "colour": "#FFF4E0" } } }
 * }</pre>
 */
public final class LightColours {

    /** A colour as a file writes it: {@code "#RRGGBB"}. */
    public static final Codec<Integer> COLOUR = Codec.STRING.comapFlatMap(LightColours::read, LightColours::write);
    /** Each block's colour of light. */
    public static final DataMapType<Block, Integer> BLOCKS = DataMapType.builder(id("light_colours"),
            Registries.BLOCK, COLOUR).synced(COLOUR, false).build();
    /** The light an item gives held or on the ground. */
    public static final DataMapType<Item, Glow> HELD = DataMapType.builder(id("held_lights"), Registries.ITEM,
            Glow.CODEC).synced(Glow.CODEC, false).build();
    /** The light a kind of mob gives. */
    public static final DataMapType<EntityType<?>, Glow> ENTITIES = DataMapType.builder(id("entity_lights"),
            Registries.ENTITY_TYPE, Glow.CODEC).synced(Glow.CODEC, false).build();
    /** The light of whatever burns: a fire's level and colour. */
    public static final Glow BURNING = new Glow(LightMath.BRIGHTEST, 0xFFA040);

    private static final int HEX = 16;
    private static final int DIGITS = 6;

    private LightColours() {
    }

    /** Hands the mod's bus the data maps, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> {
            event.register(BLOCKS);
            event.register(HELD);
            event.register(ENTITIES);
        });
    }

    /** The colour of the light {@code state} gives: the map's, or white. */
    public static int of(final BlockState state) {
        final Integer colour = state.getBlockHolder().getData(BLOCKS);
        return colour == null ? LightMath.WHITE : colour;
    }

    /** The light {@code stack} gives held or on the ground: the map's, else its block's own light; none for none. */
    public static @Nullable Glow held(final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        final Glow mapped = stack.getItemHolder().getData(HELD);
        if (mapped != null) {
            return mapped;
        }
        if (stack.getItem() instanceof BlockItem item) {
            final BlockState state = item.getBlock().defaultBlockState();
            final int level = state.getLightEmission();
            return level > 0 ? new Glow(level, of(state)) : null;
        }
        return null;
    }

    /** The light {@code entity} gives of itself: a fire's while it burns, else its kind's; none for none. */
    public static @Nullable Glow of(final Entity entity) {
        if (entity.isOnFire()) {
            return BURNING;
        }
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).getData(ENTITIES);
    }

    /** A colour written {@code #RRGGBB}. */
    public static String write(final int colour) {
        return String.format(Locale.ROOT, "#%06X", colour & LightMath.WHITE);
    }

    private static DataResult<Integer> read(final String text) {
        final String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.length() != DIGITS) {
            return DataResult.error(() -> "a colour of light is written #RRGGBB, not " + text);
        }
        try {
            return DataResult.success(Integer.parseInt(digits, HEX));
        } catch (final NumberFormatException e) {
            return DataResult.error(() -> "a colour of light is written #RRGGBB, not " + text);
        }
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    /**
     * Light something gives of itself.
     *
     * @param level  its level, from 1 to 15
     * @param colour its colour, {@code 0xRRGGBB}
     */
    public record Glow(int level, int colour) {

        /** As a file writes it: {@code {"level": 13, "colour": "#FFF4E0"}}, the colour white if left out. */
        public static final Codec<Glow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, LightMath.BRIGHTEST).fieldOf("level").forGetter(Glow::level),
                COLOUR.optionalFieldOf("colour", LightMath.WHITE).forGetter(Glow::colour))
                .apply(instance, Glow::new));
    }
}
