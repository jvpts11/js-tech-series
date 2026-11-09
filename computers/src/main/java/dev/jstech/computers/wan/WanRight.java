/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wan;

/**
 * What one network may do on another it is federated with, each granted by the port of the network acted on: see its
 * items (a SELECT that only reads), take items from it, send items into it, and have items crafted there.
 */
public enum WanRight {
    SEE,
    TAKE,
    SEND,
    CRAFT
}
