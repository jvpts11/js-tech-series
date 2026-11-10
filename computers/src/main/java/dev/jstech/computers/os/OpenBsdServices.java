/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.api.JsComputersApi;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.ComputerSettings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * OpenBSD's daemons as its rc keeps them, secure by default: the base system's, the desktop's xenodm, and every
 * service a player installed, which comes switched off and runs only once {@code rcctl enable} switched it on
 * (computers/docs/design/operating-systems.md). A Mainframe's services are none of these: they are the network's.
 *
 * <p>cron and sshd are the machine's own switches, which every Unix system here keeps, so here starting one is
 * switching it on and stopping it is switching it off. pf, pflogd and syslogd are the base system's and always run:
 * pf is the network's firewall, which belongs to the network's security. xenodm brings the desktop up.
 */
public final class OpenBsdServices {

    /** The display manager's name, which is the desktop's start-up script. */
    public static final String XENODM = "xenodm";

    /** The base system's daemons that stay on whatever is asked of them. */
    private static final Set<String> ALWAYS_ON = Set.of("pf", "pflogd", "syslogd");

    private OpenBsdServices() {
    }

    /**
     * One daemon as {@code rcctl} sees it.
     *
     * @param enabled whether it starts when the system comes up
     * @param started whether it runs now
     * @param fixed   whether it is one of the base system's that always runs
     */
    public record Daemon(String name, boolean enabled, boolean started, boolean fixed) {
    }

    /** Every daemon on that machine, by name, as {@code rcctl ls} lists them; none off OpenBSD. */
    public static List<Daemon> of(final IOsHost machine) {
        final ComputerConsoleState console = machine.console();
        if (!onOpenBsd(machine) || console == null) {
            return List.of();
        }
        final ComputerSettings settings = console.settings();
        final List<Daemon> out = new ArrayList<>();
        out.add(new Daemon("cron", settings.cronEnabled(), settings.cronEnabled(), false));
        out.add(new Daemon("sshd", settings.remoteAllowed(), settings.remoteAllowed(), false));
        for (final String always : ALWAYS_ON) {
            out.add(new Daemon(always, true, true, true));
        }
        if (machine.installedDesktopId() != null) {
            out.add(new Daemon(XENODM, settings.xenodmEnabled(), machine.bootedDesktopId() != null, false));
        }
        for (final ProgramSpec service : installedServices(console)) {
            out.add(new Daemon(service.commandName(), settings.daemonEnabled(service.id().toString()),
                    machine.serviceRunning(service), false));
        }
        out.sort(Comparator.comparing(Daemon::name));
        return out;
    }

    /**
     * Switches that daemon on or off for when the system comes up.
     *
     * @return false for a daemon the machine has none of, or one of the base system's that always runs
     */
    public static boolean enable(final IOsHost machine, final String name, final boolean on) {
        final ComputerConsoleState console = machine.console();
        final Daemon daemon = find(machine, name);
        if (daemon == null || daemon.fixed() || console == null) {
            return false;
        }
        final ComputerSettings settings = console.settings();
        switch (name) {
            case "cron" -> settings.setCronEnabled(on);
            case "sshd" -> settings.setRemoteAllowed(on);
            case XENODM -> settings.setXenodmEnabled(on);
            default -> settings.setDaemonEnabled(serviceNamed(console, name).id().toString(), on);
        }
        return true;
    }

    /**
     * Starts or stops that daemon now, until the machine starts over.
     *
     * @return false for a daemon the machine has none of, or one of the base system's that always runs
     */
    public static boolean start(final IOsHost machine, final String name, final boolean on) {
        final ComputerConsoleState console = machine.console();
        final Daemon daemon = find(machine, name);
        if (daemon == null || daemon.fixed() || console == null) {
            return false;
        }
        switch (name) {
            case "cron" -> console.settings().setCronEnabled(on);
            case "sshd" -> console.settings().setRemoteAllowed(on);
            case XENODM -> machine.setBootedDesktopId(on ? machine.installedDesktopId() : null);
            default -> {
                final ProgramSpec service = serviceNamed(console, name);
                if (on) {
                    machine.startService(service.id());
                } else {
                    machine.endService(service.id());
                }
            }
        }
        return true;
    }

    /** The system has come up: every installed service {@code rcctl} has not switched on is left stopped. */
    public static void cameUp(final IOsHost machine) {
        final ComputerConsoleState console = machine.console();
        if (!onOpenBsd(machine) || console == null) {
            return;
        }
        for (final ProgramSpec service : installedServices(console)) {
            if (!console.settings().daemonEnabled(service.id().toString())) {
                machine.endService(service.id());
            }
        }
    }

    /** A service just installed comes switched off on OpenBSD, as it does at every start after. */
    public static void installed(final IOsHost machine, final ProgramSpec program) {
        final ComputerConsoleState console = machine.console();
        if (onOpenBsd(machine) && console != null && isDaemon(program)
                && !console.settings().daemonEnabled(program.id().toString())) {
            machine.endService(program.id());
        }
    }

    @Nullable
    private static Daemon find(final IOsHost machine, final String name) {
        for (final Daemon daemon : of(machine)) {
            if (daemon.name().equals(name)) {
                return daemon;
            }
        }
        return null;
    }

    /* The installed service that answers to that name; only asked once find() has said there is one. */
    private static ProgramSpec serviceNamed(final ComputerConsoleState console, final String name) {
        for (final ProgramSpec service : installedServices(console)) {
            if (service.commandName().equals(name)) {
                return service;
            }
        }
        throw new IllegalStateException("No installed service " + name);
    }

    private static List<ProgramSpec> installedServices(final ComputerConsoleState console) {
        final List<ProgramSpec> out = new ArrayList<>();
        for (final ProgramSpec spec : OsRegistry.programs()) {
            if (isDaemon(spec) && console.isInstalled(spec.id().toString())) {
                out.add(spec);
            }
        }
        return out;
    }

    /*
     * A program that runs in the background as a daemon of its own. A language's runtime is in the programs it runs
     * instead, and a Mainframe's services are the network's, started and stopped by hand where the network is kept.
     */
    private static boolean isDaemon(final ProgramSpec spec) {
        return spec.kind().runsInBackground() && spec.hostScope() != HostScope.MAINFRAME
                && JsComputersApi.languages().get(spec.id()) == null;
    }

    private static boolean onOpenBsd(final IOsHost machine) {
        final OsDef system = machine.installedOs();
        return system != null && system.platform() == Platform.OPENBSD;
    }
}
