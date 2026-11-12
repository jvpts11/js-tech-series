/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.os.player.SystemRun;
import dev.jstech.computers.os.player.SystemScreen;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The calls of {@code System.OS} the machine answers (computers/docs/design/player-systems.md), bound to what it does
 * for the operating system a player wrote that runs on it.
 *
 * <p>What draws the system's pages and starts its desktop answers only to the system itself, the machine's process 1;
 * its screen, its services and its input answer to it and to what it started; what it reads of the machine, any
 * program may ask on any machine.
 */
@TextHolder
final class SystemCalls {

    private static final String STRING = "string";
    private static final String INT = "int";
    private static final String STRINGS = "List<string>";
    /* The class of device the text screen is, as a system's drivers name it. */
    private static final String TEXT_SCREEN = "text";

    private static final TextKey ONLY_A_SYSTEM = TextKey.of("jsc.service.system.only_a_system",
            "%s answers only on a machine running a system a player wrote");
    private static final TextKey ONLY_THE_SYSTEM = TextKey.of("jsc.service.system.only_the_system",
            "%s answers only to the system itself, process 1");
    private static final TextKey NOT_A_COLOUR = TextKey.of("jsc.service.system.not_a_colour",
            "%s or %s is not a colour: name one of the sixteen, or write it #RRGGBB");
    private static final TextKey NOT_A_PAGE = TextKey.of("jsc.service.system.not_a_page",
            "%s is not a page: blue, red, console or own");
    private static final TextKey NO_DESKTOP = TextKey.of("jsc.service.system.no_desktop",
            "there is no desktop called %s");
    private static final TextKey NO_SUCH_PROCESS = TextKey.of("jsc.service.system.no_such_process",
            "there is no process %s here");
    private static final TextKey TOO_MANY_SERVICES = TextKey.of("jsc.service.system.too_many_services",
            "a system registers at most %s services");
    private static final TextKey SERVICE_TOO_LONG = TextKey.of("jsc.service.system.service_too_long",
            "a service's name is at most %s letters and its program's path at most %s");
    /* The services a system may register, kept with the machine, and how long their names and programs may be. */
    private static final int MOST_SERVICES = 64;
    private static final int MOST_SERVICE_NAME = 64;
    private static final int MOST_SERVICE_PROGRAM = 256;

    private SystemCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        boot(bindings);
        panic(bindings);
        screen(bindings);
        services(bindings);
        desktop(bindings);
        processes(bindings);
        machine(bindings);
    }

    /* What the screen shows while the system comes up. */
    private static void boot(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        os(bindings, "Boot", "Line", (system, call, target, arguments, line) -> {
            ownRun(system, call, "Boot.Line", line).line(text(arguments, 0));
            return null;
        }, STRING);
        os(bindings, "Boot", "Logo", (system, call, target, arguments, line) -> {
            ownRun(system, call, "Boot.Logo", line).logo(text(arguments, 0));
            return null;
        }, STRING);
        os(bindings, "Boot", "Progress", (system, call, target, arguments, line) -> {
            ownRun(system, call, "Boot.Progress", line).progress(number(arguments, 0));
            return null;
        }, INT);
        os(bindings, "Boot", "Done", (system, call, target, arguments, line) -> {
            ownRun(system, call, "Boot.Done", line).finish();
            return null;
        });
    }

    /* The system's panic: going down on its page, and the look and the reboot of that page. */
    private static void panic(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        os(bindings, "Panic", "Halt", (system, call, target, arguments, line) -> {
            ownRun(system, call, "Panic.Halt", line);
            system.halt(text(arguments, 0));
            return null;
        }, STRING);
        os(bindings, "Panic", "Page", (system, call, target, arguments, line) -> {
            final SystemRun run = ownRun(system, call, "Panic.Page", line);
            final String page = text(arguments, 0).toLowerCase(Locale.ROOT);
            if (!SystemRun.PAGES.contains(page)) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_PAGE.with(page));
            }
            run.page(page);
            return null;
        }, STRING);
        os(bindings, "Panic", "Colours", (system, call, target, arguments, line) -> {
            final SystemRun run = ownRun(system, call, "Panic.Colours", line);
            final String ground = text(arguments, 0);
            final String writing = text(arguments, 1);
            if (SystemScreen.colour(ground) < 0 || SystemScreen.colour(writing) < 0) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_COLOUR.with(ground, writing));
            }
            run.colours(ground, writing);
            return null;
        }, STRING, STRING);
        os(bindings, "Panic", "RebootAfter", (system, call, target, arguments, line) -> {
            ownRun(system, call, "Panic.RebootAfter", line).rebootAfter(number(arguments, 0));
            return null;
        }, INT);
    }

    /*
     * The monitor's text screen, for a system with a shell of its own. A system with no text driver has no screen, so
     * what it writes goes nowhere.
     */
    private static void screen(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        os(bindings, "Screen", "Write", (system, call, target, arguments, line) -> {
            final SystemRun run = screenRun(system, "Screen.Write", line);
            if (run != null) {
                run.screen().write(text(arguments, 0));
                run.screenChanged();
            }
            return null;
        }, STRING);
        os(bindings, "Screen", "WriteLine", (system, call, target, arguments, line) -> {
            final SystemRun run = screenRun(system, "Screen.WriteLine", line);
            if (run != null) {
                run.screen().writeLine(text(arguments, 0));
                run.screenChanged();
            }
            return null;
        }, STRING);
        os(bindings, "Screen", "Clear", (system, call, target, arguments, line) -> {
            final SystemRun run = screenRun(system, "Screen.Clear", line);
            if (run != null) {
                run.screen().clear();
                run.screenChanged();
            }
            return null;
        });
        os(bindings, "Screen", "Cursor", (system, call, target, arguments, line) -> {
            final SystemRun run = screenRun(system, "Screen.Cursor", line);
            if (run != null) {
                run.screen().cursor(number(arguments, 0), number(arguments, 1));
                run.screenChanged();
            }
            return null;
        }, INT, INT);
        os(bindings, "Screen", "Colours", (system, call, target, arguments, line) -> {
            final SystemRun run = anyRun(system, "Screen.Colours", line);
            if (!run.screen().colours(text(arguments, 0), text(arguments, 1))) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_COLOUR.with(text(arguments, 0),
                        text(arguments, 1)));
            }
            return null;
        }, STRING, STRING);
        os(bindings, "Screen", "Columns", (system, call, target, arguments, line) -> SystemScreen.COLUMNS);
        os(bindings, "Screen", "Rows", (system, call, target, arguments, line) -> SystemScreen.ROWS);
        os(bindings, "Input", "Focus", (system, call, target, arguments, line) -> {
            anyRun(system, "Input.Focus", line).focus(pidOf(arguments.length > 0 ? arguments[0] : null));
            return null;
        }, "Process");
    }

    /* Programs run as services, by name. */
    private static void services(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        os(bindings, "Services", "Register", (system, call, target, arguments, line) -> {
            final Map<String, String> registered = anyRun(system, "Services.Register", line).registered();
            final String name = text(arguments, 0);
            final String program = text(arguments, 1);
            // Kept with the machine and saved with it, so what a system registers is bounded as what it saves.
            if (name.length() > MOST_SERVICE_NAME || program.length() > MOST_SERVICE_PROGRAM) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line,
                        SERVICE_TOO_LONG.with(MOST_SERVICE_NAME, MOST_SERVICE_PROGRAM));
            }
            if (!registered.containsKey(name) && registered.size() >= MOST_SERVICES) {
                throw new Halt(Halt.Reason.REFUSED, line, TOO_MANY_SERVICES.with(MOST_SERVICES));
            }
            registered.put(name, program);
            return null;
        }, STRING, STRING);
        os(bindings, "Services", "Start", (system, call, target, arguments, line) -> {
            anyRun(system, "Services.Start", line);
            return system.startService(text(arguments, 0));
        }, STRING);
        os(bindings, "Services", "Stop", (system, call, target, arguments, line) -> {
            anyRun(system, "Services.Stop", line);
            return system.stopService(text(arguments, 0));
        }, STRING);
        os(bindings, "Services", "Status", (system, call, target, arguments, line) -> {
            anyRun(system, "Services.Status", line);
            return system.serviceStatus(text(arguments, 0));
        }, STRING);
    }

    /* The desktop the system starts. */
    private static void desktop(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        os(bindings, "Desktop", "Start", (system, call, target, arguments, line) -> {
            ownRun(system, call, "Desktop.Start", line);
            final ResourceLocation desktop = system.desktopNamed(text(arguments, 0));
            if (desktop == null) {
                throw new Halt(Halt.Reason.REFUSED, line, NO_DESKTOP.with(text(arguments, 0)));
            }
            final Text refused = system.startDesktop(desktop);
            if (refused != null) {
                throw new Halt(Halt.Reason.REFUSED, line, refused);
            }
            return null;
        }, STRING);
        os(bindings, "Desktop", "Stop", (system, call, target, arguments, line) -> {
            ownRun(system, call, "Desktop.Stop", line);
            system.startDesktop(null);
            return null;
        });
        os(bindings, "Desktop", "Current", (system, call, target, arguments, line) -> {
            final SystemRun run = system.run();
            return run == null || run.desktop() == null ? "" : run.desktop().getPath();
        });
    }

    /* The processes: started as children of whoever asks, stopped, listed, and given a priority. */
    private static void processes(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        final MachineCalls.IServiceFunction<ProgramService> start = (programs, call, target, arguments, line) -> {
            final String path = text(arguments, 0);
            final IProgramParent parent =
                    call.callerId() > 0 ? new IProgramParent.Local(call.callerId()) : IProgramParent.NONE;
            final ProgramLauncher.Launch launch = programs.start(path, strings(arguments, 1), parent,
                    ProgramPriority.MEDIUM);
            if (!launch.ok()) {
                throw new Halt(Halt.Reason.CANNOT_START, line, ProgramCalls.refusal(path, "", launch));
            }
            return ProgramCalls.handle(launch.id(), launch.name(), "");
        };
        MachineCalls.bind(bindings, MachineServices::programs, "Processes", "Start", start, STRING);
        MachineCalls.bind(bindings, MachineServices::programs, "Processes", "Start", start, STRING, STRINGS);
        MachineCalls.bind(bindings, MachineServices::programs, "Processes", "Stop",
                (programs, call, target, arguments, line) -> {
                    if (!programs.kill(number(arguments, 0), "")) {
                        throw new Halt(Halt.Reason.REFUSED, line, NO_SUCH_PROCESS.with(number(arguments, 0)));
                    }
                    return null;
                }, INT, STRING);
        MachineCalls.bind(bindings, MachineServices::computer, "Processes", "List",
                (info, call, target, arguments, line) -> {
                    final Values.ListValue all = new Values.ListValue();
                    for (final ComputerInfoService.Running one : info.running()) {
                        final Values.Obj made = new Values.Obj("ProcessInfo");
                        made.set("Id", one.id());
                        made.set("Name", one.name());
                        made.set("State", one.state());
                        made.set("HeldBytes", one.heldBytes());
                        all.items().add(made);
                    }
                    return all;
                });
        os(bindings, "Processes", "SetPriority", (system, call, target, arguments, line) -> {
            if (!system.setPriority(number(arguments, 0), text(arguments, 1))) {
                throw new Halt(Halt.Reason.REFUSED, line, NO_SUCH_PROCESS.with(number(arguments, 0)));
            }
            return null;
        }, INT, STRING);
    }

    /* What a program reads of the machine: its disk, its memory and its devices. */
    private static void machine(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        os(bindings, "Disk", "FreeMb", (system, call, target, arguments, line) -> system.diskFreeMb());
        os(bindings, "Disk", "SizeMb", (system, call, target, arguments, line) -> system.diskSizeMb());
        os(bindings, "Disk", "Mounted", (system, call, target, arguments, line) -> list(system.mounted()));
        MachineCalls.bind(bindings, MachineServices::computer, "Memory", "TotalMb",
                (info, call, target, arguments, line) -> info.ramMb());
        MachineCalls.bind(bindings, MachineServices::computer, "Memory", "FreeMb",
                (info, call, target, arguments, line) -> info.freeRamMb());
        os(bindings, "Memory", "HeldBy", (system, call, target, arguments, line) ->
                system.heldBy(number(arguments, 0)), INT);
        os(bindings, "Memory", "Ledger", (system, call, target, arguments, line) -> list(system.ledger()));
        os(bindings, "Drivers", "Devices", (system, call, target, arguments, line) -> {
            final Values.ListValue devices = new Values.ListValue();
            for (final PlayerSystemService.Device device : system.devices()) {
                final Values.Obj made = new Values.Obj("Device");
                made.set("Name", device.name());
                made.set("Class", device.deviceClass());
                devices.items().add(made);
            }
            return devices;
        });
        os(bindings, "Drivers", "Has", (system, call, target, arguments, line) -> system.has(text(arguments, 0)),
                STRING);
    }

    private static void os(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String owner,
                           final String name, final MachineCalls.IServiceFunction<PlayerSystemService> function,
                           final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::system, owner, name, function, parameters);
    }

    /* The system's run, for a call that answers only to the system itself. */
    private static SystemRun ownRun(final PlayerSystemService system, final IWorldCall call, final String asked,
                                    final int line) {
        final SystemRun run = anyRun(system, asked, line);
        if (call.callerId() != MachinePrograms.SYSTEM_PID) {
            throw new Halt(Halt.Reason.REFUSED, line, ONLY_THE_SYSTEM.with(asked));
        }
        return run;
    }

    /* The system's run, for a call that answers on a machine running a player's system. */
    private static SystemRun anyRun(final PlayerSystemService system, final String asked, final int line) {
        final SystemRun run = system.run();
        if (run == null) {
            throw new Halt(Halt.Reason.REFUSED, line, ONLY_A_SYSTEM.with(asked));
        }
        return run;
    }

    /* The run of a system, or null when it drives no text screen and what it writes there goes nowhere. */
    @Nullable
    private static SystemRun screenRun(final PlayerSystemService system, final String asked, final int line) {
        final SystemRun run = anyRun(system, asked, line);
        return system.drives(TEXT_SCREEN) ? run : null;
    }

    private static Values.ListValue list(final List<String> lines) {
        final Values.ListValue out = new Values.ListValue();
        out.items().addAll(lines);
        return out;
    }

    private static int pidOf(final Object handle) {
        return handle instanceof Values.Obj process && process.get("Id") instanceof Number id ? id.intValue()
                : MachinePrograms.SYSTEM_PID;
    }

    private static String text(final Object[] arguments, final int at) {
        return arguments.length <= at ? "" : String.valueOf(arguments[at]);
    }

    private static int number(final Object[] arguments, final int at) {
        return arguments.length > at && arguments[at] instanceof Number value ? value.intValue() : 0;
    }

    private static List<String> strings(final Object[] arguments, final int at) {
        if (arguments.length <= at || !(arguments[at] instanceof Values.ListValue list)) {
            return List.of();
        }
        return list.items().stream().map(String::valueOf).toList();
    }
}
