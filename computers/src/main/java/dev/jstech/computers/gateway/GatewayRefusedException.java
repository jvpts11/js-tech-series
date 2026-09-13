/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

/**
 * A request the Gateway would not carry out, and why: a permission the host has not granted, a name the
 * network does not know, a Mainframe that is not there. The ComputerCraft side turns it into the error its
 * program sees; nothing about it is a fault in the mod.
 */
public final class GatewayRefusedException extends Exception {

    public GatewayRefusedException(final String message) {
        super(message);
    }
}
