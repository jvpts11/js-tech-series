/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.network.payload.IPayloadGate;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A sheet torn out of a manual: a copy of one of its pages as an item, the manual itself staying whole, to hang on a
 * wall in an item frame, where it fills the frame as a map does, or to hand to someone. The sheet keeps which manual
 * it came from and where its page stands in its entry, so it shows the page in the reader's language and as their
 * resource packs lay it out; its tooltip says where it was torn from.
 */
@TextHolder
public final class TornSheets {

    /** The longest a manual's id, a page's place and its printed number may be, as a sheet keeps them. */
    public static final int MOST_ID = 128;
    public static final int MOST_PLACE = 256;
    public static final int MOST_FOLIO = 32;
    /** Which page a sheet is a copy of. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Sheet>> SHEET =
            CoreItems.CONTENT.component("torn_sheet", Sheet.CODEC, Sheet.STREAM_CODEC);
    /** The sheet torn out. */
    public static final ItemEntry<Item> ITEM = CoreItems.CONTENT.item("torn_sheet", Item::new).named("Torn Sheet")
            .register();
    /** Its tooltip: the manual and the page it was torn from, and that it is a copy. */
    public static final TextKey TORN_FROM = TextKey.of("jscore.guide.torn_from", "From the %s, page %s");
    public static final TextKey A_COPY = TextKey.of("jscore.guide.torn_copy", "A copy: the manual keeps its page");
    private static final String NETWORK_VERSION = "1";

    private TornSheets() {
    }

    /** Declares the sheet and what it keeps, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        // Anyone in the world may copy a page, the manual's own words, as long as they are in it to hold the sheet.
        final IPayloadGate<TearPayload> playing = (player, payload) -> !player.isSpectator();
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> ClientPayloads.accept(
                event.registrar(NETWORK_VERSION), TearPayload.TYPE, TearPayload.STREAM_CODEC, playing,
                TornSheets::tear));
    }

    /** A sheet holding a copy of that page. */
    public static ItemStack of(final Sheet sheet) {
        final ItemStack stack = new ItemStack(ITEM.get());
        stack.set(SHEET.get(), sheet);
        return stack;
    }

    /* A page torn out of a manual on a player's screen: the copy goes into their inventory, or at their feet. */
    private static void tear(final TearPayload payload, final ServerPlayer player, final ServerLevel level) {
        final ItemStack sheet = of(payload.sheet());
        if (!player.getInventory().add(sheet)) {
            player.drop(sheet, false);
        }
    }

    /**
     * Which page a sheet is a copy of.
     *
     * @param manual  the manual's id
     * @param target  the entry, section or chapter the page belongs to, or the front matter
     * @param pagesIn how many pages into it the page is
     * @param folio   the page's number as the book printed it when it was torn, which the tooltip says
     */
    public record Sheet(String manual, String target, int pagesIn, String folio) {

        public static final Codec<Sheet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("manual").forGetter(Sheet::manual),
                Codec.STRING.fieldOf("target").forGetter(Sheet::target),
                Codec.INT.fieldOf("pages_in").forGetter(Sheet::pagesIn),
                Codec.STRING.optionalFieldOf("folio", "").forGetter(Sheet::folio)
        ).apply(instance, Sheet::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sheet> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(MOST_ID), Sheet::manual, ByteBufCodecs.stringUtf8(MOST_PLACE), Sheet::target,
                ByteBufCodecs.VAR_INT, Sheet::pagesIn, ByteBufCodecs.stringUtf8(MOST_FOLIO), Sheet::folio, Sheet::new);
    }
}
