/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
/**
 * Calls a screen makes to the server, typed and with no payload written by hand. A mod declares each call once, on
 * both sides, with the kind of menu it is made from, how what it sends is written, and what the server does with it;
 * the screen then calls {@code send}, and the server handles it only while the player still has a menu of that kind
 * open and in reach, the menu the server opened being the proof of what the player is using. Every call travels in
 * one payload the Core registers.
 */
package dev.jstech.core.call;
