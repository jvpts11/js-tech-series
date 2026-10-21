/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

/**
 * Which game an MCP server runs in: the player's own, which sees the window and acts as the player, or a dedicated
 * server, which reads and changes its world for the operator who let a client in. Each has tools of its own.
 */
public enum McpSide {

    GAME,
    SERVER
}
