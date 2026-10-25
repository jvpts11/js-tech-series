/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

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
import net.minecraft.core.Direction;
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
 * The clamp meter, a handheld instrument of the network probe's family: clamped on a wire it reads the energy through
 * the line (how much, which way, since it was clamped, the line's ceiling, the highest and lowest); clamped on a block,
 * what goes in and out through each face. Held, its reading shows beside the crosshair; used in the air, it is held up
 * and its display gives the details, in the energy's own unit or in FE.
 */
@TextHolder
public final class ClampMeter {

    public static final TextKey TITLE = TextKey.of("jscore.meter.title", "CLAMP METER");
    public static final TextKey CLAMPED = TextKey.of("jscore.meter.clamped", "CLAMPED");
    public static final TextKey HOLD = TextKey.of("jscore.meter.hold", "HOLD");
    public static final TextKey MAX_MIN = TextKey.of("jscore.meter.max_min", "MAX MIN");
    public static final TextKey SHOW = TextKey.of("jscore.meter.show", "SHOW");
    public static final TextKey LIGHT = TextKey.of("jscore.meter.light", "LIGHT");
    public static final TextKey DIAL_OFF = TextKey.of("jscore.meter.dial.off", "OFF");
    public static final TextKey DIAL_OWN = TextKey.of("jscore.meter.dial.own", "JE");
    public static final TextKey DIAL_FE = TextKey.of("jscore.meter.dial.fe", "FE");
    public static final TextKey TOWARD_NORTH = TextKey.of("jscore.meter.toward.north", "to the north");
    public static final TextKey TOWARD_SOUTH = TextKey.of("jscore.meter.toward.south", "to the south");
    public static final TextKey TOWARD_EAST = TextKey.of("jscore.meter.toward.east", "to the east");
    public static final TextKey TOWARD_WEST = TextKey.of("jscore.meter.toward.west", "to the west");
    public static final TextKey TOWARD_UP = TextKey.of("jscore.meter.toward.up", "upward");
    public static final TextKey TOWARD_DOWN = TextKey.of("jscore.meter.toward.down", "downward");
    public static final TextKey FACE_NORTH = TextKey.of("jscore.meter.face.north", "NORTH");
    public static final TextKey FACE_SOUTH = TextKey.of("jscore.meter.face.south", "SOUTH");
    public static final TextKey FACE_EAST = TextKey.of("jscore.meter.face.east", "EAST");
    public static final TextKey FACE_WEST = TextKey.of("jscore.meter.face.west", "WEST");
    public static final TextKey FACE_UP = TextKey.of("jscore.meter.face.up", "UP");
    public static final TextKey FACE_DOWN = TextKey.of("jscore.meter.face.down", "DOWN");
    public static final TextKey EACH_SECOND = TextKey.of("jscore.meter.each_second", "%s each second");
    public static final TextKey SINCE = TextKey.of("jscore.meter.since", "Since clamped: %s in %s");
    public static final TextKey CEILING = TextKey.of("jscore.meter.ceiling", "Its ceiling is %s:");
    public static final TextKey NO_CEILING = TextKey.of("jscore.meter.no_ceiling", "It has no ceiling");
    public static final TextKey FULL = TextKey.of("jscore.meter.full", "FULL");
    public static final TextKey MOST_LEAST = TextKey.of("jscore.meter.most_least", "MAX %s   MIN %s");
    public static final TextKey STORED = TextKey.of("jscore.meter.stored", "Stored %s of %s, %s");
    public static final TextKey FILLING = TextKey.of("jscore.meter.filling", "filling");
    public static final TextKey EMPTYING = TextKey.of("jscore.meter.emptying", "emptying");
    public static final TextKey STEADY = TextKey.of("jscore.meter.steady", "steady");
    public static final TextKey IN = TextKey.of("jscore.meter.in", "in");
    public static final TextKey OUT = TextKey.of("jscore.meter.out", "out");
    public static final TextKey NO_PORT = TextKey.of("jscore.meter.no_port", "no port");
    public static final TextKey IDLE = TextKey.of("jscore.meter.idle", "idle");
    public static final TextKey NOT_CLAMPED = TextKey.of("jscore.meter.not_clamped", "Clamp it onto a line or a block");
    public static final TextKey LOST = TextKey.of("jscore.meter.lost", "Nothing to read where it was clamped");
    public static final TextKey CLAMPED_ONTO = TextKey.of("jscore.meter.clamped_onto", "Clamped onto %s");

    /** What the meter is clamped on. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MeterClamp>> CLAMP =
            CoreItems.CONTENT.component("meter_clamp", MeterClamp.CODEC, MeterClamp.STREAM_CODEC);
    /** How the meter is set. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MeterState>> STATE =
            CoreItems.CONTENT.component("meter_state", MeterState.CODEC, MeterState.STREAM_CODEC);
    /** The clamp meter. */
    public static final ItemEntry<ClampMeterItem> ITEM = CoreItems.CONTENT.item("clamp_meter", ClampMeterItem::new)
            .named("Clamp Meter").look(IItemLook.HELD).component(STATE, MeterState.DEFAULT)
            .tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    private static final String NETWORK_VERSION = "1";

    private static volatile Consumer<InteractionHand> opener = hand -> {
    };

    static {
        // The meter reads any energy, FE included, so the Core itself uses it.
        SharedUses.declare(JsCore.MODID, ITEM::get);
    }

    private ClampMeter() {
    }

    /** Declares the meter, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Holds up the meter in {@code hand} on this game's screen; on a dedicated server, nothing. */
    public static void open(final InteractionHand hand) {
        opener.accept(hand);
    }

    /** Says what holds the meter up: the player's game does, once, as it starts. */
    public static void useOpener(final Consumer<InteractionHand> what) {
        opener = Objects.requireNonNull(what, "what");
    }

    /** How the meter in {@code stack} is set. */
    public static MeterState state(final ItemStack stack) {
        return stack.getOrDefault(STATE.get(), MeterState.DEFAULT);
    }

    /** The words for energy running beyond {@code face}. */
    public static TextKey toward(final Direction face) {
        return switch (face) {
            case NORTH -> TOWARD_NORTH;
            case SOUTH -> TOWARD_SOUTH;
            case EAST -> TOWARD_EAST;
            case WEST -> TOWARD_WEST;
            case UP -> TOWARD_UP;
            case DOWN -> TOWARD_DOWN;
        };
    }

    /** The name of {@code face}, as the display prints it. */
    public static TextKey face(final Direction face) {
        return switch (face) {
            case NORTH -> FACE_NORTH;
            case SOUTH -> FACE_SOUTH;
            case EAST -> FACE_EAST;
            case WEST -> FACE_WEST;
            case UP -> FACE_UP;
            case DOWN -> FACE_DOWN;
        };
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, ClampMeter::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                event -> MeterSessions.forget(event.getEntity().getUUID()));
    }

    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
                .playToClient(MeterReading.TYPE, MeterReading.STREAM_CODEC, ClampMeter::onReading)
                .playToServer(MeterStatePayload.TYPE, MeterStatePayload.STREAM_CODEC, ClampMeter::onState);
    }

    /* Runs on a player's game: a reading is only ever sent to the player holding the meter. */
    private static void onReading(final MeterReading reading, final IPayloadContext context) {
        context.enqueueWork(() -> MeterView.accept(reading));
    }

    /* Runs on the server: a player set the meter in their hand from its display. */
    private static void onState(final MeterStatePayload payload, final IPayloadContext context) {
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
