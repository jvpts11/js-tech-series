/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.jstechseries.hazard.HazardDevice;
import dev.jstech.core.jstechseries.hazard.HazardDeviceItem;
import dev.jstech.core.jstechseries.hazard.HazardDeviceSettings;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.jstechseries.hazard.SeriesPollution;
import dev.jstech.core.jstechseries.hazard.WitheredGrass;
import dev.jstech.core.text.GameText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The hazard devices on the player's game: each on the HUD while carried (the Geiger counter in the hotbar or the off
 * hand, the dosimeter anywhere on them, the map in a hand), in the corner and at the size the player chose; held up
 * when used; the Geiger counter's clicks, faster as the rate climbs, each lighting a lamp and, when its sound is on,
 * raising its cue and naming itself in the subtitles; the body's screen on its key; and withered grass drawn brown.
 */
public final class HazardDevicesClient {

    /** The HUD elements of the three devices. */
    public static final ResourceLocation GEIGER_HUD = id("geiger_counter");
    public static final ResourceLocation DOSIMETER_HUD = id("pocket_dosimeter");
    public static final ResourceLocation MAP_HUD = id("hazard_map");
    /* How many clicks a second the counter makes for each unit of its rate, and the most it makes. */
    private static final double CLICKS_PER_UNIT = 10;
    private static final double MOST_CLICKS = 60;
    private static final int TICKS_A_SECOND = 20;
    /* How long a click keeps its lamp lit, in ticks. */
    private static final int LIT_TICKS = 3;
    /* How far a click is heard, so its subtitle stays on while the player stands with the counter. */
    private static final float HEARD = 16;
    private static final int[] LIT_UNTIL = new int[DeviceArt.CLICK_LAMPS];
    private static int ticks;
    private static int nextLamp;
    private static int clicks;

    private HazardDevicesClient() {
    }

    /** Hands the buses what this answers and says what holds a device up, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        HazardDevices.useOpener(HazardDevicesClient::open);
        HudElements.declare(GEIGER_HUD, () -> corner(HazardDevice.GEIGER_COUNTER),
                new DeviceHud(HazardDevice.GEIGER_COUNTER));
        HudElements.declare(DOSIMETER_HUD, () -> corner(HazardDevice.POCKET_DOSIMETER),
                new DeviceHud(HazardDevice.POCKET_DOSIMETER));
        HudElements.declare(MAP_HUD, () -> corner(HazardDevice.HAZARD_MAP), new DeviceHud(HazardDevice.HAZARD_MAP));
        KeyActionsClient.onPress(HazardDevices.BODY_SCREEN, () -> Minecraft.getInstance().setScreen(new BodyScreen()));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> DeviceHud.forget());
        modBus.addListener(RegisterColorHandlersEvent.Block.class, event -> event.register(
                (state, level, pos, tint) -> 0xFF << 24 | WitheredGrass.COLOUR, WitheredGrass.BLOCK.get()));
        modBus.addListener(RegisterColorHandlersEvent.Item.class, event -> event.register(
                (stack, tint) -> 0xFF << 24 | WitheredGrass.COLOUR, WitheredGrass.BLOCK.item()));
        // Food grown or caught on fouled ground says what it carries.
        NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, event -> {
            final double metals = SeriesPollution.metalsIn(event.getItemStack());
            if (metals > 0) {
                event.getToolTip().add(GameText.component(SeriesPollution.CONTAMINATED_FOOD.with(
                        DeviceReadings.figure(metals))).withStyle(ChatFormatting.RED));
            }
        });
        SmogClient.listen();
    }

    /**
     * The device the player carries where it shows: the Geiger counter in the hotbar or the off hand, the dosimeter
     * anywhere on them, the map in a hand; empty when they carry none there.
     */
    public static ItemStack carried(final Player player, final HazardDevice device) {
        final Inventory inventory = player.getInventory();
        if (device == HazardDevice.HAZARD_MAP) {
            for (final InteractionHand hand : InteractionHand.values()) {
                if (is(player.getItemInHand(hand), device)) {
                    return player.getItemInHand(hand);
                }
            }
            return ItemStack.EMPTY;
        }
        if (is(player.getOffhandItem(), device)) {
            return player.getOffhandItem();
        }
        final int slots = device == HazardDevice.GEIGER_COUNTER ? Inventory.getSelectionSize()
                : inventory.items.size();
        for (int slot = 0; slot < slots; slot++) {
            if (is(inventory.getItem(slot), device)) {
                return inventory.getItem(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Which of the Geiger counter's lamps are lit now, a bit each, the first lowest. */
    public static int litLamps() {
        int lit = 0;
        for (int k = 0; k < LIT_UNTIL.length; k++) {
            if (LIT_UNTIL[k] > ticks) {
                lit |= 1 << k;
            }
        }
        return lit;
    }

    /** How many times the counter clicked on this game since it started; for a test to watch. */
    public static int clicks() {
        return clicks;
    }

    /** The corner {@code device} shows in, as the player chose. */
    public static HudStack.Corner corner(final HazardDevice device) {
        return switch (HazardDeviceSettings.place(device)) {
            case HazardDeviceSettings.TOP_LEFT -> HudStack.Corner.TOP_LEFT;
            case HazardDeviceSettings.TOP_RIGHT -> HudStack.Corner.TOP_RIGHT;
            case HazardDeviceSettings.BOTTOM_RIGHT -> HudStack.Corner.BOTTOM_RIGHT;
            default -> HudStack.Corner.BOTTOM_LEFT;
        };
    }

    /* Holds up the device in {@code hand}, or opens the whole map. */
    private static void open(final HazardDevice device, final InteractionHand hand) {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(device == HazardDevice.HAZARD_MAP ? new HazardMapScreen(hand)
                : new HeldDeviceScreen(device, hand));
    }

    private static boolean is(final ItemStack stack, final HazardDevice device) {
        return stack.getItem() instanceof HazardDeviceItem item && item.device() == device;
    }

    /* A tick of the Geiger counter: as many clicks as its rate makes, at random, each lighting the next lamp. */
    private static void tick() {
        ticks++;
        final Minecraft minecraft = Minecraft.getInstance();
        final Player player = minecraft.player;
        if (player == null || minecraft.isPaused()) {
            return;
        }
        final ItemStack geiger = carried(player, HazardDevice.GEIGER_COUNTER);
        if (geiger.isEmpty()) {
            return;
        }
        final double rate = DeviceReadings.radiation().map(DeviceReadings::rateHere).orElse(0.0);
        final double expected = Math.min(MOST_CLICKS, rate * CLICKS_PER_UNIT) / TICKS_A_SECOND;
        int now = (int) expected;
        if (player.getRandom().nextDouble() < expected - now) {
            now++;
        }
        for (int i = 0; i < now; i++) {
            click(minecraft, player, HazardDevices.state(geiger).sound());
        }
    }

    private static void click(final Minecraft minecraft, final Player player, final boolean sound) {
        clicks++;
        LIT_UNTIL[nextLamp] = ticks + LIT_TICKS;
        nextLamp = (nextLamp + 1) % LIT_UNTIL.length;
        if (!sound) {
            return;
        }
        AudioEngine.playCue(HazardDevices.CLICK, SoundContext.EMPTY, player.getX(), player.getY(), player.getZ(), 1,
                1);
        // The cue may be bound to no sound yet; the subtitle names the click all the same.
        if (minecraft.options.showSubtitles().get()) {
            final ResourceLocation cue = HazardDevices.CLICK.id();
            minecraft.gui.subtitleOverlay.onPlaySound(new SimpleSoundInstance(cue, SoundSource.PLAYERS, 1, 1,
                    SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR, player.getX(),
                    player.getY(), player.getZ(), false), new WeighedSoundEvents(cue, HazardDevices.CLICKS.key()),
                    HEARD);
        }
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
