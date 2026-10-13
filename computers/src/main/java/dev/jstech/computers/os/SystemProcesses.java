/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

/**
 * The processes a Unix system starts for itself before any program a player runs, and the numbers it gives them.
 *
 * <p>The first process is the system's init, number 1 on every Unix there has been: systemd on the distributions that
 * use it, and plain init on Gentoo, which comes up under OpenRC, on FreeBSD and on UNIX. The services come after it,
 * each at a number of its own that does not move while the machine runs, whatever else is installed or stopped, the way
 * a service started at boot keeps the number it started under. The programs a player starts are numbered after all of
 * them.
 */
public final class SystemProcesses {

    /** The number of the first process. */
    public static final int INIT_PID = 1;
    /** Where the first process's file is, on every Unix. */
    public static final String INIT_FILE = "/sbin/init";

    /** Where the services are numbered from: past the kernel's own threads, as on a machine that has just come up. */
    private static final int FIRST_SERVICE_PID = 300;
    /** How far apart two services' numbers are, for the helpers each starts of its own. */
    private static final int SERVICE_STEP = 11;
    private static final String SERVICE_DIR = "/usr/sbin/";

    private SystemProcesses() {
    }

    /** The number of the service at {@code index} among every program that runs in the background. */
    public static int servicePid(final int index) {
        return FIRST_SERVICE_PID + Math.max(0, index) * SERVICE_STEP;
    }

    /** The place among the programs that run in the background of the service numbered {@code pid}, or -1. */
    public static int serviceIndex(final int pid) {
        final int past = pid - FIRST_SERVICE_PID;
        return past >= 0 && past % SERVICE_STEP == 0 ? past / SERVICE_STEP : -1;
    }

    /** What the first process is called on a {@code platform} system installing with {@code packages}. */
    public static String initName(final Platform platform, final PackageManagerKind packages) {
        return platform == Platform.LINUX && packages != PackageManagerKind.EMERGE ? "systemd" : "init";
    }

    /** Where a service run by {@code command} is installed. */
    public static String serviceFile(final String command) {
        return SERVICE_DIR + command;
    }
}
