/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.energy.SurgeArresters;
import dev.jstech.core.overhead.IWireAnchors;
import dev.jstech.core.overhead.OverheadLines;
import dev.jstech.core.overhead.WireAnchor;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * The insulators the tests hang overhead wires from: the tests' energy source and sink, the cut copper blocks, each
 * have a connector on top; an end rod is a pole with a relay at its tip; mud bricks are a wall with a feedthrough, an
 * insulator on its north face and one on its south face; and an iron block is a surge arrester with a connector on
 * top. Only the test mod gives these blocks insulators, and only in development.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestAnchors {

    /** A connector on top of a block, holding four wires. */
    public static final IWireAnchors CONNECTOR = () -> List.of(new WireAnchor(new Vec3(0.5, 1.0, 0.5),
            WireAnchor.Role.CONNECTOR, Direction.UP, 4));
    /** A relay at the tip of a pole, holding four wires. */
    public static final IWireAnchors RELAY = () -> List.of(new WireAnchor(new Vec3(0.5, 0.9, 0.5),
            WireAnchor.Role.RELAY, Direction.UP, 4));
    /** A surge arrester that takes every surge. */
    public static final SurgeArresters.ISurgeArrester ARRESTER = new SurgeArresters.ISurgeArrester() {
    };
    /** A feedthrough through a wall: one insulator on its north face, one on its south. */
    public static final IWireAnchors FEEDTHROUGH = () -> List.of(
            new WireAnchor(new Vec3(0.5, 0.5, 0.0), WireAnchor.Role.FEEDTHROUGH, Direction.NORTH, 1),
            new WireAnchor(new Vec3(0.5, 0.5, 1.0), WireAnchor.Role.FEEDTHROUGH, Direction.SOUTH, 1));

    private TestAnchors() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlock(OverheadLines.ANCHORS, (level, pos, state, entity, context) -> CONNECTOR,
                Blocks.CUT_COPPER, Blocks.EXPOSED_CUT_COPPER);
        event.registerBlock(OverheadLines.ANCHORS, (level, pos, state, entity, context) -> RELAY, Blocks.END_ROD);
        event.registerBlock(OverheadLines.ANCHORS, (level, pos, state, entity, context) -> FEEDTHROUGH,
                Blocks.MUD_BRICKS);
        // An iron block is a surge arrester, a line hung to the connector on top of it.
        event.registerBlock(OverheadLines.ANCHORS, (level, pos, state, entity, context) -> CONNECTOR,
                Blocks.IRON_BLOCK);
        event.registerBlock(SurgeArresters.ARRESTER, (level, pos, state, entity, side) -> ARRESTER,
                Blocks.IRON_BLOCK);
    }
}
