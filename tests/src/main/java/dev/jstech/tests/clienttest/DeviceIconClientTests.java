/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.devices.DeviceMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Every row of a Device Manager and of KDE's Info Center wears an icon in the look of the desktop it is drawn on: each
 * part of the board, each kind of port, and each device that can stand on a port, found among the game's resources
 * for every desktop look, the period ones included.
 */
public final class DeviceIconClientTests {

    /** The parts and the ports the windows draw, by the kind their icons are drawn by. */
    private static final List<String> PARTS_AND_PORTS = List.of("motherboard", "cpu", "ram", "disk", "ssd", "gpu",
            "sound_card", "network", "crafting_card", "video_port", "audio_jack", "serial_port", "parallel_port",
            "usb_port", "usb3_port");

    /** Every block that can stand on a port, by its id, which the icon is found from. */
    private static final List<String> DEVICES = List.of("vintage_monitor", "legacy_monitor", "monitor",
            "legacy_speaker", "transition_speaker", "speaker", "advanced_speaker", "floppy_drive", "cd_drive",
            "dvd_drive", "blu_ray_drive", "dock_station", "network_gateway", "vintage_hub", "legacy_hub",
            "transition_hub", "standard_hub", "advanced_hub", "vintage_redstone_interface",
            "legacy_redstone_interface", "transition_redstone_interface", "standard_redstone_interface",
            "advanced_redstone_interface", "vintage_pattern_encoder", "legacy_pattern_encoder",
            "transition_pattern_encoder", "pattern_encoder", "advanced_pattern_encoder");

    /** Every look a desktop wears, each of which has its own drawing of each icon. */
    private static final List<String> LOOKS = List.of("frames_95", "frames_xp", "frames_7", "frames_10", "frames_11",
            "kde_plasma", "kde_plasma_legacy", "gnome", "gnome_legacy", "cinnamon", "cde");

    private DeviceIconClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void everyPartPortAndDevice_hasItsIconInEveryLook(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final List<String> kinds = new ArrayList<>(PARTS_AND_PORTS);
            for (final String device : DEVICES) {
                kinds.add(DeviceMap.iconOf(device));
            }
            final List<String> missing = new ArrayList<>();
            for (final String kind : kinds) {
                for (final String look : LOOKS) {
                    final ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                            "textures/gui/device/" + kind + "/" + look + ".png");
                    if (Minecraft.getInstance().getResourceManager().getResource(icon).isEmpty()) {
                        missing.add(kind + "/" + look);
                    }
                }
            }
            ctx.assertTrue(missing.isEmpty(), "icons missing: " + missing);
        });
    }
}
