/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.input.KeyAction;
import dev.jstech.core.input.KeyActions;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lwjgl.glfw.GLFW;

/**
 * The hazard devices, handheld things of the series' shared content that look like the real equipment, a yellow case
 * with a black face: the Geiger counter, which shows the radiation's rate here on a needle over a scale of tens and
 * clicks faster as it climbs; the pocket dosimeter, which shows the dose taken and its band; and the hazard map, which
 * shows the land around with a hazard on it, chunk by chunk. Carried, each shows on the HUD where the player put it and
 * at the size they gave it; used, the first two are held up full size and the map opens whole. The body's screen,
 * opened by a key of its own (B until the player changes it), shows the dose, the contamination, the lungs and the
 * dissonance on one worn panel.
 */
@TextHolder
public final class HazardDevices {

    public static final TextKey GEIGER = TextKey.of("jscore.hazard_device.geiger", "GEIGER  x%s");
    public static final TextKey DOSE = TextKey.of("jscore.hazard_device.dose", "DOSE");
    public static final TextKey RANGE = TextKey.of("jscore.hazard_device.range", "RANGE");
    public static final TextKey SOUND = TextKey.of("jscore.hazard_device.sound", "SOUND");
    public static final TextKey LIGHT = TextKey.of("jscore.hazard_device.light", "LIGHT");
    public static final TextKey HAZARDS = TextKey.of("jscore.hazard_device.hazards", "HAZARDS");
    public static final TextKey OPEN = TextKey.of("jscore.hazard_device.open", "OPEN");
    public static final TextKey HERE = TextKey.of("jscore.hazard_device.here", "here %s");
    public static final TextKey NOTHING_MAPPED = TextKey.of("jscore.hazard_device.nothing_mapped",
            "No hazard is mapped");
    public static final TextKey OFF = TextKey.of("jscore.hazard_device.off", "OFF");
    /** A rate in a hazard's unit a second, for a hazard whose rate has no unit of its own. */
    public static final TextKey PER_SECOND = TextKey.of("jscore.hazard_device.per_second", "%s/s");
    public static final TextKey CLICKS = TextKey.of("subtitles.jscore.geiger_counter.click", "Geiger counter clicks");
    public static final TextKey BODY_KEY = TextKey.of("key.jscore.body_screen", "Body screen");

    /** The series' ionizing radiation, which the Geiger counter and the dosimeter read: declared with the radiation. */
    public static final ResourceLocation RADIATION = id("ionizing_radiation");
    /** The radioactive dust carried on the body, which keeps dosing until it is washed off. */
    public static final ResourceLocation CONTAMINATION = id("radioactive_contamination");
    /** How far the laws of physics round the body are from the normal ones. */
    public static final ResourceLocation DISSONANCE = id("dissonance");
    /** How a device is set. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DeviceState>> STATE =
            CoreItems.CONTENT.component("hazard_device", DeviceState.CODEC, DeviceState.STREAM_CODEC);
    public static final ItemEntry<HazardDeviceItem> GEIGER_COUNTER = CoreItems.CONTENT.item("geiger_counter",
                    properties -> new HazardDeviceItem(HazardDevice.GEIGER_COUNTER, properties))
            .named("Geiger Counter").look(IItemLook.HELD).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final ItemEntry<HazardDeviceItem> POCKET_DOSIMETER = CoreItems.CONTENT.item("pocket_dosimeter",
                    properties -> new HazardDeviceItem(HazardDevice.POCKET_DOSIMETER, properties))
            .named("Pocket Dosimeter").look(IItemLook.HELD).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final ItemEntry<HazardDeviceItem> HAZARD_MAP = CoreItems.CONTENT.item("hazard_map",
                    properties -> new HazardDeviceItem(HazardDevice.HAZARD_MAP, properties))
            .named("Hazard Map").look(IItemLook.FLAT).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    /** The click of the Geiger counter, at the player: a cue bound to nothing until the series' sounds come. */
    public static final SoundCue CLICK = CoreItems.CONTENT.cue("hazard/geiger_click").world().range(16).register();
    /** Opens the body's screen. B, until the player changes it. */
    public static final KeyAction BODY_SCREEN = KeyActions.declare(KeyAction.builder(id("body_screen"), BODY_KEY)
            .key(GLFW.GLFW_KEY_B));
    private static final String NETWORK_VERSION = "1";

    private static volatile BiConsumer<HazardDevice, InteractionHand> opener = (device, hand) -> {
    };

    static {
        // The Core's hazards are measured with them, so the Core itself uses them.
        SharedUses.declare(JsCore.MODID, GEIGER_COUNTER::get);
        SharedUses.declare(JsCore.MODID, POCKET_DOSIMETER::get);
        SharedUses.declare(JsCore.MODID, HAZARD_MAP::get);
    }

    private HazardDevices() {
    }

    /** Declares the devices, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(NETWORK_VERSION)
                .playToServer(DeviceStatePayload.TYPE, DeviceStatePayload.STREAM_CODEC, HazardDevices::onState));
    }

    /** Holds up {@code device} in {@code hand} on this game's screen, or opens the whole map; on a server, nothing. */
    public static void open(final HazardDevice device, final InteractionHand hand) {
        opener.accept(device, hand);
    }

    /** Says what holds a device up: the player's game does, once, as it starts. */
    public static void useOpener(final BiConsumer<HazardDevice, InteractionHand> what) {
        opener = Objects.requireNonNull(what, "what");
    }

    /** How the device in {@code stack} is set. */
    public static DeviceState state(final ItemStack stack) {
        return stack.getOrDefault(STATE.get(), DeviceState.DEFAULT);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    /* Runs on the server: a player set the device in their hand from its buttons. */
    private static void onState(final DeviceStatePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Player player = context.player();
            final ItemStack held = player.getItemInHand(payload.mainHand() ? InteractionHand.MAIN_HAND
                    : InteractionHand.OFF_HAND);
            if (held.getItem() instanceof HazardDeviceItem) {
                held.set(STATE.get(), payload.state());
            }
        });
    }
}
