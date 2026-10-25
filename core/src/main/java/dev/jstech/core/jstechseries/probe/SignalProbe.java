/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The signal probe, the series' handheld instrument for redstone and signals by frequency, of the network probe's
 * family: linked to a block it reads the redstone on it, now and its peak since the link; wherever it is held it hears
 * the frequencies there, each with whose it is, where it comes from, how well it arrives and the value it carries,
 * clashes marked, and the frequencies of the player's team not heard there, so a player finds where a receiver or a
 * relay will hear well. OK on the radio's page points out where the chosen one comes from.
 */
@TextHolder
public final class SignalProbe {

    public static final TextKey TITLE = TextKey.of("jscore.signal_probe.title", "SIGNAL PROBE");
    public static final TextKey PAGE_HERE = TextKey.of("jscore.signal_probe.page.here", "HERE");
    public static final TextKey PAGE_RADIO = TextKey.of("jscore.signal_probe.page.radio", "RADIO");
    public static final TextKey REDSTONE = TextKey.of("jscore.signal_probe.redstone", "REDSTONE");
    public static final TextKey RADIO = TextKey.of("jscore.signal_probe.radio", "RADIO");
    public static final TextKey HEARD_COUNT = TextKey.of("jscore.signal_probe.heard_count", "%s HEARD");
    public static final TextKey ON_THE_WIRE = TextKey.of("jscore.signal_probe.on_the_wire", "ON THE WIRE");
    public static final TextKey OF_FULL = TextKey.of("jscore.signal_probe.of_full", "of %s");
    public static final TextKey PEAK = TextKey.of("jscore.signal_probe.peak", "Peak %s since the click");
    public static final TextKey NOT_LINKED = TextKey.of("jscore.signal_probe.not_linked",
            "Use it on a wire to read it");
    public static final TextKey LOST_WIRE = TextKey.of("jscore.signal_probe.lost_wire",
            "The block it reads is out of reach");
    public static final TextKey HEARD_HERE = TextKey.of("jscore.signal_probe.heard_here", "HEARD HERE");
    public static final TextKey FREQ = TextKey.of("jscore.signal_probe.freq", "FREQ");
    public static final TextKey WHOSE = TextKey.of("jscore.signal_probe.whose", "WHOSE");
    public static final TextKey FROM = TextKey.of("jscore.signal_probe.from", "FROM");
    public static final TextKey SENDS = TextKey.of("jscore.signal_probe.sends", "SENDS");
    public static final TextKey CLASH = TextKey.of("jscore.signal_probe.clash", "clash");
    public static final TextKey LOST = TextKey.of("jscore.signal_probe.lost", "lost");
    public static final TextKey METRES = TextKey.of("jscore.signal_probe.metres", "%s m");
    public static final TextKey RELAYED = TextKey.of("jscore.signal_probe.relayed", "%s, relay");
    public static final TextKey NOTHING_HEARD = TextKey.of("jscore.signal_probe.nothing_heard",
            "No frequency is heard here");
    public static final TextKey FREQUENCY = TextKey.of("jscore.signal_probe.frequency", "FREQUENCY %s");
    public static final TextKey OWNER = TextKey.of("jscore.signal_probe.owner", "Owner: %s");
    public static final TextKey NOBODY = TextKey.of("jscore.signal_probe.nobody", "nobody");
    public static final TextKey SENT_FROM = TextKey.of("jscore.signal_probe.sent_from", "From %s, at %s");
    public static final TextKey RECEPTION = TextKey.of("jscore.signal_probe.reception", "Arrives with %s of %s bars");
    public static final TextKey NOT_HEARD = TextKey.of("jscore.signal_probe.not_heard", "Not heard here: out of reach");
    public static final TextKey CARRIES = TextKey.of("jscore.signal_probe.carries", "Carries %s");
    public static final TextKey CLASHES = TextKey.of("jscore.signal_probe.clashes", "Clash: its receivers are silent");
    public static final TextKey LINKED_TO = TextKey.of("jscore.signal_probe.linked_to", "Reading %s at %s");
    public static final TextKey HUD_WIRE = TextKey.of("jscore.signal_probe.hud_wire", "Redstone %s");
    public static final TextKey HUD_HEARD = TextKey.of("jscore.signal_probe.hud_heard", "%s frequencies heard");

    /** The block the probe reads. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> LINK =
            CoreItems.CONTENT.component("signal_link", GlobalPos.CODEC, GlobalPos.STREAM_CODEC);
    /** How the probe is set. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SignalProbeState>> STATE =
            CoreItems.CONTENT.component("signal_probe_state", SignalProbeState.CODEC, SignalProbeState.STREAM_CODEC);
    /** The signal probe. */
    public static final ItemEntry<SignalProbeItem> ITEM = CoreItems.CONTENT.item("signal_probe",
                    SignalProbeItem::new).named("Signal Probe").look(IItemLook.HELD)
            .component(STATE, SignalProbeState.DEFAULT).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    private static final String NETWORK_VERSION = "1";

    private static volatile Consumer<InteractionHand> opener = hand -> {
    };

    static {
        // The probe reads any redstone and hears every radio built on the Core's kit, so the Core itself uses it.
        SharedUses.declare(JsCore.MODID, ITEM::get);
    }

    private SignalProbe() {
    }

    /** Declares the probe, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Holds up the probe in {@code hand} on this game's screen; on a dedicated server, nothing. */
    public static void open(final InteractionHand hand) {
        opener.accept(hand);
    }

    /** Says what holds the probe up: the player's game does, once, as it starts. */
    public static void useOpener(final Consumer<InteractionHand> what) {
        opener = Objects.requireNonNull(what, "what");
    }

    /** How the probe in {@code stack} is set. */
    public static SignalProbeState state(final ItemStack stack) {
        return stack.getOrDefault(STATE.get(), SignalProbeState.DEFAULT);
    }

    /** The word a page's tab shows. */
    public static TextKey page(final SignalProbeState.Page page) {
        return switch (page) {
            case HERE -> PAGE_HERE;
            case RADIO -> PAGE_RADIO;
        };
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, SignalProbe::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                event -> SignalSessions.forget(event.getEntity().getUUID()));
    }

    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
                .playToClient(SignalReading.TYPE, SignalReading.STREAM_CODEC, SignalProbe::onReading)
                .playToServer(SignalProbeStatePayload.TYPE, SignalProbeStatePayload.STREAM_CODEC,
                        SignalProbe::onState);
    }

    /* Runs on a player's game: a reading is only ever sent to the player holding the probe. */
    private static void onReading(final SignalReading reading, final IPayloadContext context) {
        context.enqueueWork(() -> SignalView.accept(reading));
    }

    /* Runs on the server: a player set the probe in their hand from its keys. */
    private static void onState(final SignalProbeStatePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Player player = context.player();
            final ItemStack held = player.getItemInHand(payload.mainHand() ? InteractionHand.MAIN_HAND
                    : InteractionHand.OFF_HAND);
            if (held.is(ITEM.get())) {
                held.set(STATE.get(), payload.state());
            }
        });
    }
}
