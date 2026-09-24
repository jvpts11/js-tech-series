/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.crafting.RecipeChoice;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the planner's answer for the Craft popup. The rows are what the request consumes (need
 * vs have, red when short), with whether it is fully feasible, the largest feasible amount for the PARTIAL
 * button, and a time estimate. When more than one recipe on the network makes the result, {@code options}
 * lists them all and {@code recipe} says which one the rows were planned with; {@code cover} says, per row
 * that is short, what the network would craft to cover it (or that nothing can).
 */
public record CraftPlanPayload(ItemStack result, long quantity, List<Row> rows,
                               boolean feasible, long maxFeasible, int estimateTicks,
                               int recipe, List<RecipeChoice> options, List<Text> cover, int stages)
        implements CustomPacketPayload {

    public static final int MAX_ROWS = 64;
    public static final int MAX_OPTIONS = 8;
    public static final int MAX_INPUTS = 12;
    public static final int MAX_MACHINES = 8;
    public static final int MAX_COVER = 6;
    public static final int MAX_TEXT = 96;

    private static final StreamCodec<ByteBuf, List<Text>> MACHINES_CODEC =
            TextCodecs.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_MACHINES));
    private static final StreamCodec<ByteBuf, List<Text>> COVER_CODEC =
            TextCodecs.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_COVER));

    /** A plan with no choice of recipe: what a result with a single recipe gets. */
    public CraftPlanPayload(final ItemStack result, final long quantity, final List<Row> rows,
                            final boolean feasible, final long maxFeasible, final int estimateTicks) {
        this(result, quantity, rows, feasible, maxFeasible, estimateTicks, 0, List.of(), List.of(), 1);
    }

    public record Row(ItemStack item, long need, long have) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC =
                StreamCodec.composite(
                        ItemStack.STREAM_CODEC, Row::item,
                        ByteBufCodecs.VAR_LONG, Row::need,
                        ByteBufCodecs.VAR_LONG, Row::have,
                        Row::new);

        public boolean satisfied() {
            return have >= need;
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeChoice.Input> INPUT_CODEC =
            StreamCodec.composite(
                    TextCodecs.STREAM_CODEC, RecipeChoice.Input::name,
                    ByteBufCodecs.VAR_LONG, RecipeChoice.Input::need,
                    ByteBufCodecs.VAR_LONG, RecipeChoice.Input::have,
                    ByteBufCodecs.BOOL, RecipeChoice.Input::craftable,
                    RecipeChoice.Input::new);

    /*
     * Written out by hand: a choice carries seven things and composite takes six pairs. The kind is cut to its
     * cap on the way out, never refused, because a cap on writeUtf drops the connection; lists are cut to theirs.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeChoice> CHOICE_CODEC =
            StreamCodec.of(CraftPlanPayload::encodeChoice, CraftPlanPayload::decodeChoice);

    private static void encodeChoice(final RegistryFriendlyByteBuf buf, final RecipeChoice choice) {
        TextCodecs.STREAM_CODEC.encode(buf, choice.label());
        buf.writeUtf(clip(choice.kind()), MAX_TEXT);
        MACHINES_CODEC.encode(buf, first(choice.machines(), MAX_MACHINES));
        buf.writeVarInt(choice.stages());
        buf.writeVarInt(choice.estimateTicks());
        INPUT_CODEC.apply(ByteBufCodecs.list(MAX_INPUTS)).encode(buf, first(choice.inputs(), MAX_INPUTS));
        buf.writeBoolean(choice.feasible());
    }

    private static RecipeChoice decodeChoice(final RegistryFriendlyByteBuf buf) {
        final Text label = TextCodecs.STREAM_CODEC.decode(buf);
        final String kind = buf.readUtf(MAX_TEXT);
        final List<Text> machines = MACHINES_CODEC.decode(buf);
        final int stages = buf.readVarInt();
        final int estimate = buf.readVarInt();
        final List<RecipeChoice.Input> inputs = INPUT_CODEC.apply(ByteBufCodecs.list(MAX_INPUTS)).decode(buf);
        final boolean feasible = buf.readBoolean();
        return new RecipeChoice(label, kind, machines, stages, estimate, inputs, feasible);
    }

    private static String clip(final String text) {
        final String s = text == null ? "" : text;
        return s.length() <= MAX_TEXT ? s : s.substring(0, MAX_TEXT);
    }

    private static <T> List<T> first(final List<T> items, final int max) {
        return items.size() <= max ? items : new ArrayList<>(items.subList(0, max));
    }

    public static final CustomPacketPayload.Type<CraftPlanPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "craft_plan"));

    // Ten things to carry, so the codec is written out rather than composed.
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPlanPayload> STREAM_CODEC =
            StreamCodec.of(CraftPlanPayload::encode, CraftPlanPayload::decode);

    private static void encode(final RegistryFriendlyByteBuf buf, final CraftPlanPayload p) {
        ItemStack.STREAM_CODEC.encode(buf, p.result);
        buf.writeVarLong(p.quantity);
        Row.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)).encode(buf, p.rows);
        buf.writeBoolean(p.feasible);
        buf.writeVarLong(p.maxFeasible);
        buf.writeVarInt(p.estimateTicks);
        buf.writeVarInt(p.recipe);
        CHOICE_CODEC.apply(ByteBufCodecs.list(MAX_OPTIONS)).encode(buf, p.options);
        COVER_CODEC.encode(buf, first(p.cover, MAX_COVER));
        buf.writeVarInt(p.stages);
    }

    private static CraftPlanPayload decode(final RegistryFriendlyByteBuf buf) {
        final ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
        final long quantity = buf.readVarLong();
        final List<Row> rows = Row.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)).decode(buf);
        final boolean feasible = buf.readBoolean();
        final long maxFeasible = buf.readVarLong();
        final int estimate = buf.readVarInt();
        final int recipe = buf.readVarInt();
        final List<RecipeChoice> options = CHOICE_CODEC.apply(ByteBufCodecs.list(MAX_OPTIONS)).decode(buf);
        final List<Text> cover = COVER_CODEC.decode(buf);
        final int stages = buf.readVarInt();
        return new CraftPlanPayload(result, quantity, rows, feasible, maxFeasible, estimate, recipe, options, cover, stages);
    }

    /** Whether the player has a choice of recipe to make: more than one option came back. */
    public boolean hasChoice() {
        return options.size() > 1;
    }

    /** The option the rows were planned with, or null when the plan carries no options. */
    public RecipeChoice chosen() {
        return recipe >= 0 && recipe < options.size() ? options.get(recipe) : null;
    }

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public CraftPlanPayload {
        rows = List.copyOf(rows);
        options = List.copyOf(options);
        cover = List.copyOf(cover);
    }

    @Override
    public CustomPacketPayload.Type<CraftPlanPayload> type() {
        return TYPE;
    }
}
