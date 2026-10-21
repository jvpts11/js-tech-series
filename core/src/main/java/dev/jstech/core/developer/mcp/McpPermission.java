/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

/**
 * What the player lets a client of the MCP server do when it pairs: read the game (its state, its screens, pictures of
 * the window, the logs), or read it and act in it (move, click, place, run the allowed commands, write files in the
 * mod's and the pack's folders). A tool needs one of them.
 */
public enum McpPermission {

    READ,
    ACT;

    /** Whether a client given this may use a tool that needs {@code needed}: acting lets a client read too. */
    public boolean allows(final McpPermission needed) {
        return this == ACT || needed == READ;
    }
}
