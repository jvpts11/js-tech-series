/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.workshop;

import dev.jstech.computers.workshop.Workshop;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.text.TextCodecs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: the Workshop as the computer holds it, for its window: which personal-use cards are in, what
 * every slot holds, what the grid makes and how many more the inventory has for, the furnace's pace and progress, the
 * three enchanting offers, what the anvil card makes and what it asks, the player's level, and the line the last
 * action left.
 *
 * @param cards          the cards in the computer, as a mask of their bits
 * @param slots          every Workshop slot, in the Workshop's order
 * @param craftResult    what the grid makes now
 * @param craftMore      how many more times the inventory could fill the grid again
 * @param furnaceSpeed   how many times a furnace's pace the Furnace Card works at here
 * @param ticksPerItem   the ticks one item of the furnace's input takes, 0 with nothing that smelts
 * @param progressTicks  how far the item in the furnace is
 * @param smeltsInto     what the furnace's input becomes
 * @param offers         the three enchanting offers: the level each needs (0 for none), its clue, its levels
 * @param anvilResult    what the anvil card makes now
 * @param anvilLevels    what an anvil would ask for it
 * @param anvilCost      what the card asks
 * @param anvilName      the name the card gives the item
 * @param level          the player's experience level
 * @param progress       how far the player is into the next level, from 0 to 1
 * @param status         what the last action did, or nothing
 */
public record WorkshopStatePayload(int cards, List<ItemStack> slots, ItemStack craftResult, int craftMore,
                                   int furnaceSpeed, int ticksPerItem, int progressTicks, ItemStack smeltsInto,
                                   List<Offer> offers, ItemStack anvilResult, int anvilLevels, int anvilCost,
                                   String anvilName, int level, float progress, Text status)
        implements CustomPacketPayload {

    private static final int MAX_NAME = Workshop.MAX_NAME;
    private static final int MAX_OFFERS = 3;

    public static final CustomPacketPayload.Type<WorkshopStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "workshop_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorkshopStatePayload> STREAM_CODEC =
            StreamCodec.of((buf, state) -> state.write(buf), WorkshopStatePayload::read);

    public WorkshopStatePayload {
        // write() sends the count and read() takes at most SLOTS, so more would leave the stream misaligned.
        slots = List.copyOf(slots.size() > Workshop.SLOTS ? slots.subList(0, Workshop.SLOTS) : slots);
        offers = List.copyOf(offers);
        anvilName = anvilName == null ? "" : anvilName;
        status = status == null ? Text.EMPTY : status;
    }

    /** One enchanting offer: the level it needs (0 when there is none), the enchantment it hints, its levels. */
    public record Offer(int required, Component clue, int levels) {
    }

    /** The item in Workshop slot {@code slot}, or an empty stack. */
    public ItemStack slot(final int slot) {
        return slot >= 0 && slot < slots.size() ? slots.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public CustomPacketPayload.Type<WorkshopStatePayload> type() {
        return TYPE;
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(cards);
        buf.writeVarInt(slots.size());
        for (final ItemStack stack : slots) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, craftResult);
        buf.writeVarInt(craftMore);
        buf.writeVarInt(furnaceSpeed);
        buf.writeVarInt(ticksPerItem);
        buf.writeVarInt(progressTicks);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, smeltsInto);
        buf.writeVarInt(Math.min(MAX_OFFERS, offers.size()));
        for (final Offer offer : offers.subList(0, Math.min(MAX_OFFERS, offers.size()))) {
            buf.writeVarInt(offer.required());
            ComponentSerialization.STREAM_CODEC.encode(buf, offer.clue());
            buf.writeVarInt(offer.levels());
        }
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, anvilResult);
        buf.writeVarInt(anvilLevels);
        buf.writeVarInt(anvilCost);
        buf.writeUtf(TextBounds.clip(anvilName, MAX_NAME), MAX_NAME);
        buf.writeVarInt(level);
        buf.writeFloat(progress);
        TextCodecs.STREAM_CODEC.encode(buf, status);
    }

    private static WorkshopStatePayload read(final RegistryFriendlyByteBuf buf) {
        final int cards = buf.readVarInt();
        final int count = Math.min(buf.readVarInt(), Workshop.SLOTS);
        final List<ItemStack> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            slots.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        final ItemStack craftResult = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        final int craftMore = buf.readVarInt();
        final int furnaceSpeed = buf.readVarInt();
        final int ticksPerItem = buf.readVarInt();
        final int progressTicks = buf.readVarInt();
        final ItemStack smeltsInto = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        final int offerCount = Math.min(buf.readVarInt(), MAX_OFFERS);
        final List<Offer> offers = new ArrayList<>(offerCount);
        for (int i = 0; i < offerCount; i++) {
            offers.add(new Offer(buf.readVarInt(), ComponentSerialization.STREAM_CODEC.decode(buf), buf.readVarInt()));
        }
        final ItemStack anvilResult = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        final int anvilLevels = buf.readVarInt();
        final int anvilCost = buf.readVarInt();
        final String anvilName = buf.readUtf(MAX_NAME);
        final int level = buf.readVarInt();
        final float progress = buf.readFloat();
        final Text status = TextCodecs.STREAM_CODEC.decode(buf);
        return new WorkshopStatePayload(cards, slots, craftResult, craftMore, furnaceSpeed, ticksPerItem,
                progressTicks, smeltsInto, offers, anvilResult, anvilLevels, anvilCost, anvilName, level, progress,
                status);
    }
}
