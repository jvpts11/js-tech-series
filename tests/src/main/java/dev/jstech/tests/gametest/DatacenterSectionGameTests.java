/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.datacenter.DatacenterSection;
import dev.jstech.core.uuid.NodeUuid;
import dev.jstech.tests.JsTests;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A datacenter section is a snapshot: changing the collections it was built from afterwards does not change it, and
 * the order of its racks, which the router's rack budget walks, is kept.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DatacenterSectionGameTests {

    private DatacenterSectionGameTests() {
    }

    private static final String ARENA = "empty";

    @GameTest(template = ARENA)
    public static void constructor_copiesItsCollectionsAndKeepsRackOrder(final GameTestHelper helper) {
        final Set<Long> racks = new LinkedHashSet<>(List.of(30L, 10L, 20L));
        final List<NodeUuid> servers = new ArrayList<>(List.of(NodeUuid.random()));

        final DatacenterSection section = new DatacenterSection(Direction.NORTH, racks, servers, 0L);
        racks.add(99L);
        servers.clear();

        helper.assertTrue(section.rackCount() == 3, "a later change to the source set does not reach the section");
        helper.assertTrue(section.serverCount() == 1, "a later change to the source list does not reach the section");
        helper.assertTrue(List.copyOf(section.rackPositions()).equals(List.of(30L, 10L, 20L)),
                "the racks keep their order; got " + section.rackPositions());
        helper.succeed();
    }
}
