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
import dev.jstech.core.grid.LineSurvey;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
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
 * The network probe, the handheld instrument of the series that reads a whole line: linked to a wire of any line, it
 * reads the part of the line the wire is in, its blocks by what they are to it (sources, consumers, stores), what
 * flowed each second of the last minute against what its narrowest cable carries, what was asked of it, and what its
 * cables lost. Linked to a part that turns, it reads the part's network of shafts the same way, in watts: its sources
 * and loads, the power it carries and what friction takes. Held up, its display shows it on four pages; OK shows the
 * chosen blocks in the world; HOLD, SHOW, LIGHT and POWER as on the clamp meter, its family.
 */
@TextHolder
public final class NetworkProbe {

    public static final TextKey TITLE = TextKey.of("jscore.probe.title", "NETWORK PROBE");
    public static final TextKey LINK_WORD = TextKey.of("jscore.probe.link", "LINK");
    public static final TextKey HOLD = TextKey.of("jscore.probe.hold", "HOLD");
    public static final TextKey SHOW = TextKey.of("jscore.probe.show", "SHOW");
    public static final TextKey LIGHT = TextKey.of("jscore.probe.light", "LIGHT");
    public static final TextKey POWER = TextKey.of("jscore.probe.power", "POWER");
    public static final TextKey OK = TextKey.of("jscore.probe.ok", "OK");
    public static final TextKey PAGE_SUMMARY = TextKey.of("jscore.probe.page.summary", "SUMMARY");
    public static final TextKey PAGE_MEMBERS = TextKey.of("jscore.probe.page.members", "MEMBERS");
    public static final TextKey PAGE_FLOW = TextKey.of("jscore.probe.page.flow", "FLOW");
    public static final TextKey PAGE_LOSSES = TextKey.of("jscore.probe.page.losses", "LOSSES");
    public static final TextKey CABLES = TextKey.of("jscore.probe.cables", "%s CABLES");
    public static final TextKey FLOW = TextKey.of("jscore.probe.flow", "FLOW");
    public static final TextKey LAST_MINUTE = TextKey.of("jscore.probe.last_minute", "the last 60 s");
    public static final TextKey NARROWEST = TextKey.of("jscore.probe.narrowest", "NARROWEST");
    public static final TextKey NARROWEST_AT = TextKey.of("jscore.probe.narrowest_at", "%s at %s");
    public static final TextKey OF = TextKey.of("jscore.probe.of", "%s of %s");
    public static final TextKey FULL = TextKey.of("jscore.probe.full", "FULL");
    public static final TextKey NO_LIMIT = TextKey.of("jscore.probe.no_limit", "No cable limits it");
    public static final TextKey WANT = TextKey.of("jscore.probe.want", "Consumers want %s: they run at %s");
    public static final TextKey LOSSES = TextKey.of("jscore.probe.losses", "LOSSES");
    public static final TextKey LOSS_LINE = TextKey.of("jscore.probe.loss_line", "%s, %s of the flow");
    public static final TextKey NO_LOSS = TextKey.of("jscore.probe.no_loss", "None: it loses nothing on the way");
    public static final TextKey MEMBERS = TextKey.of("jscore.probe.members", "MEMBERS");
    public static final TextKey COUNT_SOURCE = TextKey.of("jscore.probe.count.source", "sources");
    public static final TextKey COUNT_CONSUMER = TextKey.of("jscore.probe.count.consumer", "consumers");
    public static final TextKey COUNT_STORE = TextKey.of("jscore.probe.count.store", "storage");
    public static final TextKey GROUP_SOURCE = TextKey.of("jscore.probe.group.source", "SOURCES");
    public static final TextKey GROUP_CONSUMER = TextKey.of("jscore.probe.group.consumer", "CONSUMERS");
    public static final TextKey GROUP_STORE = TextKey.of("jscore.probe.group.store", "STORAGE");
    public static final TextKey GROUP_IDLE = TextKey.of("jscore.probe.group.idle", "IDLE");
    public static final TextKey GIVES = TextKey.of("jscore.probe.gives", "give %s");
    public static final TextKey WANT_GET = TextKey.of("jscore.probe.want_get", "want %s, get %s");
    public static final TextKey TAKE = TextKey.of("jscore.probe.take", "take %s");
    public static final TextKey NET = TextKey.of("jscore.probe.net", "net %s");
    public static final TextKey TIMES = TextKey.of("jscore.probe.times", "x%s");
    public static final TextKey OK_SHOWS = TextKey.of("jscore.probe.ok_shows", "OK: show it in the world");
    public static final TextKey NO_MEMBERS = TextKey.of("jscore.probe.no_members", "It plugs into nothing");
    public static final TextKey FLOW_MINUTE = TextKey.of("jscore.probe.flow_minute", "FLOW, THE LAST MINUTE");
    public static final TextKey NOW = TextKey.of("jscore.probe.now", "NOW %s");
    public static final TextKey MOST = TextKey.of("jscore.probe.most", "MAX %s");
    public static final TextKey LEAST = TextKey.of("jscore.probe.least", "MIN %s");
    public static final TextKey MEAN = TextKey.of("jscore.probe.mean", "MEAN %s");
    public static final TextKey MINUTE_AGO = TextKey.of("jscore.probe.minute_ago", "60 s ago");
    public static final TextKey NOW_WORD = TextKey.of("jscore.probe.now_word", "now");
    public static final TextKey LOST_ON_THE_WAY = TextKey.of("jscore.probe.lost_on_the_way", "Lost on the way: %s");
    public static final TextKey SHARE = TextKey.of("jscore.probe.share", "%s of what the sources sent");
    public static final TextKey DELIVERED = TextKey.of("jscore.probe.delivered", "Sent %s, delivered %s");
    public static final TextKey ALONG = TextKey.of("jscore.probe.along", "Along %s cables");
    public static final TextKey LOSES_NOTHING = TextKey.of("jscore.probe.loses_nothing",
            "Pipes and tubes lose nothing on the way: what leaves a block reaches another.");
    public static final TextKey RATE_FLUID = TextKey.of("jscore.probe.rate.fluid", "%s mB/t");
    public static final TextKey RATE_ITEMS = TextKey.of("jscore.probe.rate.items", "%s items/t");
    public static final TextKey RATE_WATTS = TextKey.of("jscore.probe.rate.watts", "%s %sW");
    public static final TextKey PARTS = TextKey.of("jscore.probe.parts", "%s PARTS");
    public static final TextKey SHAFTS = TextKey.of("jscore.probe.shafts", "Shafts and gears");
    public static final TextKey FRICTION = TextKey.of("jscore.probe.friction",
            "Friction takes it, at every bearing, gear and belt.");
    public static final TextKey CARRIES_NOTHING = TextKey.of("jscore.probe.carries_nothing",
            "This line carries nothing: it only joins");
    public static final TextKey NOT_LINKED = TextKey.of("jscore.probe.not_linked", "Use it on a line to link it");
    public static final TextKey LOST = TextKey.of("jscore.probe.lost", "Nothing to read where it was linked");
    public static final TextKey LINKED_TO = TextKey.of("jscore.probe.linked_to", "Linked to %s");
    public static final TextKey HUD_MEMBERS = TextKey.of("jscore.probe.hud_members", "%s, %s blocks");

    /** What a probe linked to a network of shafts says it is linked to, in place of a cable. */
    public static final ResourceLocation MECHANICAL = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "mechanical");

    /** The wire the probe is linked to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ProbeLink>> LINK =
            CoreItems.CONTENT.component("probe_link", ProbeLink.CODEC, ProbeLink.STREAM_CODEC);
    /** How the probe is set. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ProbeState>> STATE =
            CoreItems.CONTENT.component("probe_state", ProbeState.CODEC, ProbeState.STREAM_CODEC);
    /** The network probe. */
    public static final ItemEntry<NetworkProbeItem> ITEM = CoreItems.CONTENT.item("network_probe",
                    NetworkProbeItem::new).named("Network Probe").look(IItemLook.HELD)
            .component(STATE, ProbeState.DEFAULT).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    private static final String NETWORK_VERSION = "1";

    private static volatile Consumer<InteractionHand> opener = hand -> {
    };

    static {
        // The probe reads any line, so the Core itself uses it.
        SharedUses.declare(JsCore.MODID, ITEM::get);
    }

    private NetworkProbe() {
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
    public static ProbeState state(final ItemStack stack) {
        return stack.getOrDefault(STATE.get(), ProbeState.DEFAULT);
    }

    /** The word a page's tab shows. */
    public static TextKey page(final ProbeState.Page page) {
        return switch (page) {
            case SUMMARY -> PAGE_SUMMARY;
            case MEMBERS -> PAGE_MEMBERS;
            case FLOW -> PAGE_FLOW;
            case LOSSES -> PAGE_LOSSES;
        };
    }

    /** The heading of the blocks of {@code role} on the members' page. */
    public static TextKey group(final LineSurvey.Role role) {
        return switch (role) {
            case SOURCE -> GROUP_SOURCE;
            case CONSUMER -> GROUP_CONSUMER;
            case STORE -> GROUP_STORE;
            case IDLE -> GROUP_IDLE;
        };
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, NetworkProbe::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                event -> ProbeSessions.forget(event.getEntity().getUUID()));
    }

    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
                .playToClient(ProbeReading.TYPE, ProbeReading.STREAM_CODEC, NetworkProbe::onReading)
                .playToServer(ProbeStatePayload.TYPE, ProbeStatePayload.STREAM_CODEC, NetworkProbe::onState);
    }

    /* Runs on a player's game: a reading is only ever sent to the player holding the probe. */
    private static void onReading(final ProbeReading reading, final IPayloadContext context) {
        context.enqueueWork(() -> ProbeView.accept(reading));
    }

    /* Runs on the server: a player set the probe in their hand from its keys. */
    private static void onState(final ProbeStatePayload payload, final IPayloadContext context) {
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
