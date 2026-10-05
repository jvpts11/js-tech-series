/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.team;

import dev.jstech.core.JsCore;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * The permissions the Core asks the game's permission system about, so a server's permission mod can grant or deny
 * them: by default an operator holds them and nobody else does.
 */
public final class CorePermissions {

    /** Using what another player owns, whatever its access says. */
    public static final PermissionNode<Boolean> PASS_OWNERS = new PermissionNode<>(JsCore.MODID, "ownership.bypass",
            PermissionTypes.BOOLEAN, (player, id, context) -> player != null && player.hasPermissions(2));

    private CorePermissions() {
    }

    /** Hands the game the Core's permissions, on the game's event bus. */
    public static void onGatherNodes(final PermissionGatherEvent.Nodes event) {
        event.addNodes(PASS_OWNERS);
    }

    /** Whether {@code player} may use what others own. */
    public static boolean mayPassOwners(final ServerPlayer player) {
        return PermissionAPI.getPermission(player, PASS_OWNERS);
    }
}
