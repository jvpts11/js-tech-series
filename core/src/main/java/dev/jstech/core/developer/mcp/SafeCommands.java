/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import dev.jstech.core.text.Text;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The game's commands an agent may run, in a test world: those that set up and look at the world a test needs (time,
 * weather, blocks, items, creatures, effects, game rules, the world's data), never those that give or take operator
 * rights, ban, kick, stop the server or change its whitelist, nor those that run other commands in their turn, which
 * would carry any command past the list.
 */
public final class SafeCommands {

    /** The commands let through, by their first word. */
    public static final Set<String> ALLOWED = Set.of("time", "weather", "give", "clear", "setblock", "fill", "clone",
            "summon", "kill", "tp", "teleport", "gamemode", "effect", "enchant", "experience", "xp", "difficulty",
            "gamerule", "item", "particle", "playsound", "stopsound", "spreadplayers", "setworldspawn", "spawnpoint",
            "tick", "worldborder", "locate", "data", "scoreboard", "tag", "title", "reload", "fillbiome", "place",
            "ride", "damage", "attribute", "loot", "seed", "list", "forceload", "save-all");
    /** The commands never let through, whatever list a later version keeps. */
    public static final Set<String> NEVER = Set.of("op", "deop", "ban", "ban-ip", "banlist", "pardon", "pardon-ip",
            "kick", "stop", "whitelist", "publish", "transfer", "save-off", "save-on", "perf", "jfr", "debug",
            "execute", "function", "schedule", "return", "msg", "tell", "w", "me", "teammsg", "tm", "trigger",
            "jstech", "neoforge", "forge", "config");
    /** The longest command taken. */
    public static final int LONGEST = 2000;

    private SafeCommands() {
    }

    /** Why {@code command} is not run, or none when it may be; a slash before it is left out. */
    public static Optional<Text> refusal(final String command) {
        final String plain = command.strip().startsWith("/") ? command.strip().substring(1) : command.strip();
        if (plain.isEmpty() || plain.length() > LONGEST || plain.indexOf('\n') >= 0 || plain.indexOf('\r') >= 0) {
            return Optional.of(McpWords.COMMAND_EMPTY.text());
        }
        final int space = plain.indexOf(' ');
        String root = (space < 0 ? plain : plain.substring(0, space)).toLowerCase(Locale.ROOT);
        if (root.startsWith("minecraft:")) {
            root = root.substring("minecraft:".length());
        }
        if (NEVER.contains(root) || root.contains(":")) {
            return Optional.of(McpWords.COMMAND_NEVER.with(root));
        }
        if (!ALLOWED.contains(root)) {
            return Optional.of(McpWords.COMMAND_NOT_LISTED.with(root));
        }
        return Optional.empty();
    }
}
