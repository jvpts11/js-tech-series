/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Objects;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import org.jetbrains.annotations.Nullable;

/**
 * The permission nodes of J's Computers' actions, declared through the Core's, so a server's permission mod (LuckPerms,
 * FTB Ranks) grants them by player, dimension and claim, and {@code /jstech dump permissions} lists them with the
 * Core's. Without such a mod the operator levels below apply. The computers themselves have no owner: who may use one
 * is whoever stands at it.
 */
@TextHolder
public final class ComputersPermissions {

    public static final TextKey RELOAD_CATALOG_TEXT = TextKey.of("jsc.permission.reload_catalog",
            "Read Soundfoundry's music catalogue again (/soundfoundry catalog reload)");
    public static final TextKey CONFIGURE_GATEWAYS_TEXT = TextKey.of("jsc.permission.configure_gateways",
            "Act on a Network Gateway from a computer: what it lets ComputerCraft do, its name, its buffer");

    private static @Nullable PermissionNode<Boolean> reloadCatalog;
    private static @Nullable PermissionNode<Boolean> configureGateways;

    private ComputersPermissions() {
    }

    /** Declares the nodes, once, from the mod's constructor, before the game gathers every mod's. */
    public static void declare() {
        // The level of the game's own reload.
        reloadCatalog = CorePermissions.declare(JsComputers.MODID, "soundfoundry.catalog.reload",
                CorePermissions.OPERATORS, RELOAD_CATALOG_TEXT);
        // Every player, unless a server says otherwise.
        configureGateways = CorePermissions.declare(JsComputers.MODID, "gateway.configure", 0,
                CONFIGURE_GATEWAYS_TEXT);
    }

    /** Reading Soundfoundry's music catalogue again. */
    public static PermissionNode<Boolean> reloadCatalog() {
        return Objects.requireNonNull(reloadCatalog, "the permissions of J's Computers are not declared yet");
    }

    /** Changing a Network Gateway, from the Gateway Manager or the gateway command. */
    public static PermissionNode<Boolean> configureGateways() {
        return Objects.requireNonNull(configureGateways, "the permissions of J's Computers are not declared yet");
    }
}
