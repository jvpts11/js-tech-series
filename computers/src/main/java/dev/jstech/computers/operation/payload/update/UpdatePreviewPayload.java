/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.update;

import dev.jstech.computers.operation.payload.workshop.WorkshopStatePayload;
import dev.jstech.computers.workshop.UpdateAction;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: what the Update window shows for an item, every tab's at once, and what came of the last ask.
 *
 * @param item           the item as the network holds it
 * @param cards          the asking computer's personal-use cards, as a mask of their bits; none on anything but a
 *                       Personal Computer
 * @param accepts        the actions the item takes, as a mask of {@code 1 << id}
 * @param computer       the asking computer's name
 * @param held           how many of the item the network holds
 * @param where          the servers that hold it, the most first, at most a few
 * @param offers         the Enchanting Card's three offers for the player who asked
 * @param level          that player's experience level
 * @param worn           the most worn of the item's kind the network holds, the one a repair takes
 * @param material       what mends it, as the network holds it, or an empty stack for nothing
 * @param materialHeld   how much of the material the network holds
 * @param materialUsed   how much of it the repair takes
 * @param repaired       what the anvil would make of the worn item, its material and the name
 * @param anvilLevels    what an anvil would ask for it
 * @param cardLevels     what the Anvil Card asks
 * @param name           the name the anvil would give
 * @param smeltsInto     what one of the item smelts into, or an empty stack
 * @param furnaceSpeed   how many times a furnace's pace the Furnace Card works at
 * @param workshopDone   how many of the Workshop's own smelting are done, while it smelts
 * @param workshopTotal  how many it smelts in all, zero while it smelts nothing
 * @param networkBusy    whether another UPDATE has the card's furnace now
 * @param status         what came of the last ask, or nothing
 * @param sent           whether the last ask started an UPDATE, which closes the window
 */
public record UpdatePreviewPayload(ItemStack item, int cards, int accepts, String computer, long held,
                                   List<String> where, List<WorkshopStatePayload.Offer> offers, int level,
                                   ItemStack worn, ItemStack material, long materialHeld, int materialUsed,
                                   ItemStack repaired, int anvilLevels, int cardLevels, String name,
                                   ItemStack smeltsInto, int furnaceSpeed, int workshopDone, int workshopTotal,
                                   boolean networkBusy, Text status, boolean sent) implements CustomPacketPayload {

    private static final int MAX_TEXT = 64;
    private static final int MAX_WHERE = 4;
    private static final int MAX_OFFERS = 3;

    public static final CustomPacketPayload.Type<UpdatePreviewPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "update_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePreviewPayload> STREAM_CODEC =
            StreamCodec.of((buf, preview) -> preview.write(buf), UpdatePreviewPayload::read);

    public UpdatePreviewPayload {
        computer = clip(computer);
        where = where.stream().limit(MAX_WHERE).map(UpdatePreviewPayload::clip).toList();
        offers = List.copyOf(offers.subList(0, Math.min(MAX_OFFERS, offers.size())));
        name = clip(name);
        status = status == null ? Text.EMPTY : status;
    }

    /** Whether the item takes {@code action}. */
    public boolean takes(final UpdateAction action) {
        return (accepts & 1 << action.id()) != 0;
    }

    @Override
    public CustomPacketPayload.Type<UpdatePreviewPayload> type() {
        return TYPE;
    }

    private static String clip(final String text) {
        return text == null ? "" : text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT);
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, item);
        buf.writeVarInt(cards);
        buf.writeVarInt(accepts);
        ByteBufCodecs.stringUtf8(MAX_TEXT).encode(buf, computer);
        buf.writeVarLong(held);
        buf.writeVarInt(where.size());
        for (final String server : where) {
            ByteBufCodecs.stringUtf8(MAX_TEXT).encode(buf, server);
        }
        buf.writeVarInt(offers.size());
        for (final WorkshopStatePayload.Offer offer : offers) {
            buf.writeVarInt(offer.required());
            ComponentSerialization.STREAM_CODEC.encode(buf, offer.clue());
            buf.writeVarInt(offer.levels());
        }
        buf.writeVarInt(level);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, worn);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, material);
        buf.writeVarLong(materialHeld);
        buf.writeVarInt(materialUsed);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, repaired);
        buf.writeVarInt(anvilLevels);
        buf.writeVarInt(cardLevels);
        ByteBufCodecs.stringUtf8(MAX_TEXT).encode(buf, name);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, smeltsInto);
        buf.writeVarInt(furnaceSpeed);
        buf.writeVarInt(workshopDone);
        buf.writeVarInt(workshopTotal);
        buf.writeBoolean(networkBusy);
        TextCodecs.STREAM_CODEC.encode(buf, status);
        buf.writeBoolean(sent);
    }

    private static UpdatePreviewPayload read(final RegistryFriendlyByteBuf buf) {
        final ItemStack item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        final int cards = buf.readVarInt();
        final int accepts = buf.readVarInt();
        final String computer = ByteBufCodecs.stringUtf8(MAX_TEXT).decode(buf);
        final long held = buf.readVarLong();
        final int whereCount = Math.min(buf.readVarInt(), MAX_WHERE);
        final List<String> where = new ArrayList<>(whereCount);
        for (int i = 0; i < whereCount; i++) {
            where.add(ByteBufCodecs.stringUtf8(MAX_TEXT).decode(buf));
        }
        final int offerCount = Math.min(buf.readVarInt(), MAX_OFFERS);
        final List<WorkshopStatePayload.Offer> offers = new ArrayList<>(offerCount);
        for (int i = 0; i < offerCount; i++) {
            offers.add(new WorkshopStatePayload.Offer(buf.readVarInt(), ComponentSerialization.STREAM_CODEC.decode(buf),
                    buf.readVarInt()));
        }
        final int level = buf.readVarInt();
        final ItemStack worn = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        final ItemStack material = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        final long materialHeld = buf.readVarLong();
        final int materialUsed = buf.readVarInt();
        final ItemStack repaired = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        final int anvilLevels = buf.readVarInt();
        final int cardLevels = buf.readVarInt();
        final String name = ByteBufCodecs.stringUtf8(MAX_TEXT).decode(buf);
        final ItemStack smeltsInto = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        final int furnaceSpeed = buf.readVarInt();
        final int workshopDone = buf.readVarInt();
        final int workshopTotal = buf.readVarInt();
        final boolean networkBusy = buf.readBoolean();
        final Text status = TextCodecs.STREAM_CODEC.decode(buf);
        final boolean sent = buf.readBoolean();
        return new UpdatePreviewPayload(item, cards, accepts, computer, held, where, offers, level, worn, material,
                materialHeld, materialUsed, repaired, anvilLevels, cardLevels, name, smeltsInto, furnaceSpeed,
                workshopDone, workshopTotal, networkBusy, status, sent);
    }
}
