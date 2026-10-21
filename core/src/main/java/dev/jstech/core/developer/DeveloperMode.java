/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.team.CorePermissions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * The key "Developer mode": whether the tools for whoever makes a mod or a modpack are on, the studio and every tool
 * of it, the inspector and the editor of screens among them. Off by default in a game a player plays, on in a
 * development run. It is the player's own setting; on a server, the server's own key has to be on too, and only an
 * operator ({@link CorePermissions#DEVELOPER_TOOLS}) uses the tools that read or change the world.
 */
public final class DeveloperMode {

    private static final String SECTION = "developer";

    /** The player's key. */
    public static final ConfigKey<Boolean> PLAYER_KEY = ConfigKey.flag(SECTION + ".mode", !FMLEnvironment.production)
            .comment("Whether the tools for whoever makes a mod or a modpack are on in this game: the studio, the "
                    + "inspector and editor of screens, and every tool of the studio. On a server, its own key has to "
                    + "be on too.")
            .named("Developer mode");
    /** The server's key. */
    public static final ConfigKey<Boolean> SERVER_KEY = ConfigKey.flag(SECTION + ".server",
                    !FMLEnvironment.production)
            .comment("Whether this world lets the players whose developer mode is on use the developer's tools, and "
                    + "its operators use those that read or change the world.")
            .named("Developer mode on this world");
    /**
     * The port the player's game's MCP server listens on, on this computer only. Where it listens is all a file says:
     * the server itself is turned on only from inside the game, for that run.
     */
    public static final ConfigKey<Integer> MCP_PORT = ConfigKey.whole(SECTION + ".mcp_port", 25599)
            .range(1024, 65535)
            .comment("The port the studio's MCP server listens on, on this computer only, once it is turned on "
                    + "in the game.")
            .named("MCP server's port");
    /** The port a dedicated server's MCP server listens on, on the server's computer only. */
    public static final ConfigKey<Integer> SERVER_MCP_PORT = ConfigKey.whole(SECTION + ".server_mcp_port", 25598)
            .range(1024, 65535)
            .comment("The port this server's MCP server listens on, on the server's computer only, once an "
                    + "operator turns it on in the game.")
            .named("MCP server's port on this server");
    /** The player's file. */
    public static final ConfigFile PLAYER_FILE = ConfigFile.builder("jstech-developer", ConfigSide.CLIENT,
                    ConfigFormats.TOML)
            .comment("The tools for whoever makes a mod or a modpack, on this game.")
            .sectionNamed(SECTION, "Developer mode")
            .key(PLAYER_KEY, value -> playerOn = value)
            .key(MCP_PORT, value -> mcpPort = value)
            .build();
    /** The world's file, which its players are sent. */
    public static final ConfigFile SERVER_FILE = ConfigFile.builder("jstech-developer-server", ConfigSide.SERVER,
                    ConfigFormats.TOML)
            .comment("Whether this world lets the developer's tools be used.")
            .sectionNamed(SECTION, "Developer mode")
            .key(SERVER_KEY, value -> serverOn = value)
            .key(SERVER_MCP_PORT, value -> serverMcpPort = value)
            .build();

    private static volatile boolean playerOn = !FMLEnvironment.production;
    private static volatile boolean serverOn = !FMLEnvironment.production;
    private static volatile int mcpPort = 25599;
    private static volatile int serverMcpPort = 25598;

    private DeveloperMode() {
    }

    /** Whether the developer's tools are on in this game: the player's key, and the world's too. */
    public static boolean on() {
        return playerOn && serverOn;
    }

    /** Whether the world lets the developer's tools be used, as its own key says. */
    public static boolean onServer() {
        return serverOn;
    }

    /** The port the player's game's MCP server listens on. */
    public static int mcpPort() {
        return mcpPort;
    }

    /** The port a dedicated server's MCP server listens on. */
    public static int serverMcpPort() {
        return serverMcpPort;
    }

    /**
     * Whether {@code player} may use the tools that read or change the world: the world's key is on, and they are
     * the player whose game runs it, or hold {@link CorePermissions#DEVELOPER_TOOLS}.
     */
    public static boolean mayUseWorldTools(final ServerPlayer player) {
        return serverOn && (player.server.isSingleplayerOwner(player.getGameProfile())
                || CorePermissions.has(player, CorePermissions.DEVELOPER_TOOLS));
    }
}
