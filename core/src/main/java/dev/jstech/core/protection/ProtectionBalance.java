/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.protection;

/**
 * What the server's settings say of protection, set each time the world's settings are read: whether teammates may
 * hurt each other, whether the audit log is kept and how much of it, and how many robots one player may have.
 */
public final class ProtectionBalance {

    /** How many entries the audit log keeps, the oldest dropped first, unless the settings say otherwise. */
    public static final int DEFAULT_AUDIT_ENTRIES = 10_000;
    /** How many robots one player may have at once, unless the settings say otherwise. */
    public static final int DEFAULT_ROBOTS_PER_PLAYER = 16;

    private static volatile boolean teamDamage;
    private static volatile boolean audit;
    private static volatile int auditEntries = DEFAULT_AUDIT_ENTRIES;
    private static volatile int robotsPerPlayer = DEFAULT_ROBOTS_PER_PLAYER;

    private ProtectionBalance() {
    }

    /** How many robots one player may have at once. */
    public static int robotsPerPlayer() {
        return robotsPerPlayer;
    }

    public static void setRobotsPerPlayer(final int value) {
        robotsPerPlayer = value;
    }

    /** Whether players on one team may hurt each other. */
    public static boolean teamDamage() {
        return teamDamage;
    }

    public static void setTeamDamage(final boolean value) {
        teamDamage = value;
    }

    /** Whether who used, changed or broke what others own is written down. */
    public static boolean audit() {
        return audit;
    }

    public static void setAudit(final boolean value) {
        audit = value;
    }

    /** How many entries the audit log keeps. */
    public static int auditEntries() {
        return auditEntries;
    }

    public static void setAuditEntries(final int value) {
        auditEntries = value;
    }
}
