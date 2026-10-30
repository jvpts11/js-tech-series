/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.LineGroup;
import dev.jstech.core.grid.LineGroups;
import dev.jstech.space.telemetry.AntennaSize;
import dev.jstech.space.telemetry.TelemetryLine;
import dev.jstech.space.telemetry.TelemetryRole;
import java.util.Optional;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * The devices the tests' telemetry line joins, in the stead of J's Space's own until they come: copper, iron and gold
 * blocks are a small, a medium and a large antenna, a target block a receiver, an observer a decoder, a jukebox a data
 * recorder and a cartography table a ground station. Only the test mod gives them the capabilities, and only in
 * development.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestTelemetryDevices {

    /* A device takes its place on a group line, hearing of nothing. */
    private static final LineGroups.IGroupMember ON_THE_LINE = new LineGroups.IGroupMember() {
        @Override
        public void joined(final LineGroup group) {
            // Its ground system is read from the line when it is asked for.
        }

        @Override
        public void left(final GridKind kind) {
            // As above.
        }
    };

    private TestTelemetryDevices() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        device(event, Blocks.COPPER_BLOCK, TelemetryRole.ANTENNA, Optional.of(AntennaSize.SMALL));
        device(event, Blocks.IRON_BLOCK, TelemetryRole.ANTENNA, Optional.of(AntennaSize.MEDIUM));
        device(event, Blocks.GOLD_BLOCK, TelemetryRole.ANTENNA, Optional.of(AntennaSize.LARGE));
        device(event, Blocks.TARGET, TelemetryRole.RECEIVER, Optional.empty());
        device(event, Blocks.OBSERVER, TelemetryRole.DECODER, Optional.empty());
        device(event, Blocks.JUKEBOX, TelemetryRole.RECORDER, Optional.empty());
        device(event, Blocks.CARTOGRAPHY_TABLE, TelemetryRole.GROUND_STATION, Optional.empty());
    }

    /* {@code block} is a device of {@code role}, of {@code size} if an antenna, on the telemetry line. */
    private static void device(final RegisterCapabilitiesEvent event, final Block block, final TelemetryRole role,
                               final Optional<AntennaSize> size) {
        event.registerBlock(LineGroups.MEMBER, (level, pos, state, entity, side) -> ON_THE_LINE, block);
        event.registerBlock(TelemetryLine.MEMBER, (level, pos, state, entity, side) -> new Device(role, size), block);
    }

    /* A device: what it does, and how big it is. */
    private record Device(TelemetryRole role, Optional<AntennaSize> size) implements TelemetryLine.IMember {
    }
}
