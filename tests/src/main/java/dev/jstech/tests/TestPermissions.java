/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.team.CorePermissions;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * A permission of the test mod held only inside a claim of the test world, as a permission mod granting a node by the
 * claim's context would: its rule reads the contexts the Core hands it, so a test sees them arrive.
 */
public final class TestPermissions {

    /** Held inside a claim, and nowhere else. */
    public static final PermissionNode<Boolean> IN_A_CLAIM = new PermissionNode<>(JsTests.MODID, "in_a_claim",
            PermissionTypes.BOOLEAN, (player, id, contexts) -> inAClaim(contexts), CorePermissions.DIMENSION,
            CorePermissions.CLAIM);

    private TestPermissions() {
    }

    /** Hands the game the node as it gathers them, from the test mod's constructor. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(PermissionGatherEvent.Nodes.class, event -> event.addNodes(IN_A_CLAIM));
    }

    private static boolean inAClaim(final PermissionDynamicContext<?>... contexts) {
        for (final PermissionDynamicContext<?> context : contexts) {
            if (context.getDynamic() == CorePermissions.CLAIM
                    && !CorePermissions.NO_CLAIM.equals(context.getSerializedValue())) {
                return true;
            }
        }
        return false;
    }
}
