/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.Diagnostic;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmReader;
import dev.jstech.computers.cannon.run.Halt;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Loaded;
import dev.jstech.computers.cannon.run.Process;
import dev.jstech.computers.cannon.run.Snapshot;
import dev.jstech.computers.cannon.run.Values;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What a Cannon program sees of the machine's Gateways: which there are, which one it chose, what is on
 * the other side, and what a ComputerCraft computer says to it.
 */
class HostGatewayTest {

    private static final long ROOM = 256L * 1024;
    private static final int PLENTY = 1_000_000;

    /** A machine with two Gateways, which writes down every question it was asked. */
    private static final class Machine implements IHost {

        private final List<String> asked = new ArrayList<>();
        /** Whether this machine has Gateways at all; one without answers for none. */
        private final boolean has;

        Machine() {
            this(true);
        }

        Machine(final boolean has) {
            this.has = has;
        }

        @Override
        public long tick() {
            return 0;
        }

        @Override
        public long dayTime() {
            return 0;
        }

        @Override
        public long day() {
            return 0;
        }

        @Override
        public boolean provides(final String owner) {
            return this.has && "Gateway".equals(owner);
        }

        @Override
        public Reply call(final String owner, final String member, final List<Object> arguments,
                          final String caller, final int line) {
            this.asked.add(member + arguments);
            final String chosen = arguments.isEmpty() ? "" : String.valueOf(arguments.getFirst());
            return switch (member) {
                case "Online" -> Reply.of(true, 1);
                case "Current" -> Reply.of(chosen.isEmpty() ? "north" : chosen, 1);
                case "Names" -> {
                    final Values.ListValue names = new Values.ListValue();
                    names.items().addAll(List.of("north", "west"));
                    yield Reply.of(names, 1);
                }
                case "Select" -> Reply.of("west".equals(chosen), 1);
                case "Computers" -> {
                    final Values.Obj one = new Values.Obj("CcComputer");
                    one.set("Id", 3L);
                    one.set("Name", "computer_3");
                    one.set("Label", "turtle bay");
                    one.set("Online", true);
                    final Values.ListValue all = new Values.ListValue();
                    all.items().add(one);
                    yield Reply.of(all, 1);
                }
                case "Peripherals" -> {
                    final Values.Obj one = new Values.Obj("CcPeripheral");
                    one.set("Name", "monitor_0");
                    one.set("Type", "monitor");
                    final Values.ListValue methods = new Values.ListValue();
                    methods.items().addAll(List.of("write", "setCursorPos"));
                    one.set("Methods", methods);
                    final Values.ListValue all = new Values.ListValue();
                    all.items().add(one);
                    yield Reply.of(all, 1);
                }
                case "Call" -> Reply.of("done", 1);
                case "TurnOn", "Shutdown", "Reboot", "Send" -> Reply.of(true, 1);
                default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "Gateway has no " + member);
            };
        }
    }

    private static final String PRELUDE =
            "using System.*; using System.IO.*; using System.Collections.*; using System.Network.*; namespace Tests; ";

    private static Loaded load(final String source) {
        final CannonCompiler.Result built =
                CannonCompiler.compile(List.of(new SourceFile("Bridge.can", PRELUDE + source)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final DiagnosticBag bag = new DiagnosticBag("Bridge.asm");
        final AsmProgram program = new AsmReader(built.assembly(), bag).read();
        assertFalse(bag.hasErrors(), () -> String.join("\n",
                bag.sorted().stream().map(Diagnostic::format).toList()) + "\n" + built.assembly());
        return Loaded.of(program);
    }

    private static Process start(final Loaded program, final Machine machine) {
        final Process process = new Process(program, ROOM, machine);
        process.begin(process.create(program.entryPoint()), "OnInit");
        process.step(PLENTY);
        return process;
    }

    /** A program that asks its Gateway everything there is to ask, and listens for what comes back. */
    private static final String BRIDGE = """
            class Bridge : IScript {
                public void OnInit() {
                    Console.PrintLine("online " + Gateway.Online);
                    Console.PrintLine("first " + Gateway.Current);
                    Console.PrintLine("names " + Gateway.Names().Count);
                    Console.PrintLine("picked " + Gateway.Select("west"));
                    Console.PrintLine("now " + Gateway.Current);
                    foreach (CcComputer one in Gateway.Computers()) {
                        Console.PrintLine("computer " + one.Id + " " + one.Label + " " + one.Online);
                    }
                    foreach (CcPeripheral device in Gateway.Peripherals()) {
                        Console.PrintLine("device " + device.Name + " " + device.Type + " " + device.Methods.Count);
                    }
                    Console.PrintLine("called " + Gateway.Call("monitor_0", "setCursorPos", 1, 2));
                    Console.PrintLine("power " + Gateway.TurnOn(3) + Gateway.Shutdown(3) + Gateway.Reboot(3));
                    Console.PrintLine("sent " + Gateway.Send(3, "hello"));
                    Gateway.OnMessage(Heard);
                }
                void Heard(GatewayMessage said) {
                    Console.PrintLine("heard " + said.From + " " + said.Text + " " + said.Tick);
                }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

    @Test
    void gateway_answersEverythingAProgramAsksOfIt() {
        final Machine machine = new Machine();
        final Process process = start(load(BRIDGE), machine);
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        assertEquals(List.of("online true", "first north", "names 2", "picked true", "now west",
                "computer 3 turtle bay true", "device monitor_0 monitor 2", "called done",
                "power truetruetrue", "sent true"), process.console());
    }

    @Test
    void select_makesEveryCallAfterwardsNameThatGateway() {
        final Machine machine = new Machine();
        start(load(BRIDGE), machine);
        assertTrue(machine.asked.contains("Select[west, west]"), machine.asked.toString());
        assertTrue(machine.asked.contains("Current[]"), "the first question is about whichever comes first");
        assertTrue(machine.asked.contains("Current[west]"), "and afterwards about the one it chose");
        assertTrue(machine.asked.contains("Call[west, monitor_0, setCursorPos, 1, 2]"),
                "a call carries the chosen Gateway and everything the method takes");
    }

    @Test
    void onMessage_handsTheProgramWhatAComputerSaid() {
        final Machine machine = new Machine();
        final Process process = start(load(BRIDGE), machine);
        assertTrue(process.deliverGatewayMessage(3, "hello", 7), "the program is listening");
        process.step(PLENTY);
        assertEquals("heard 3 hello 7", process.console().getLast());
    }

    @Test
    void save_keepsTheChosenGatewayAndTheListening() {
        final Machine machine = new Machine();
        final Loaded program = load(BRIDGE);
        Process process = start(program, machine);
        final Snapshot shot = process.save();
        process = Process.restore(program, shot, machine);
        assertEquals("west", process.gatewayName());
        assertTrue(process.deliverGatewayMessage(5, "again", 9), "it is still listening after the save");
        process.step(PLENTY);
        assertEquals("heard 5 again 9", process.console().getLast());
    }

    @Test
    void gateway_saysSoOnAMachineThatHasNone() {
        final Process process = start(load("""
                class Bridge : IScript {
                    public void OnInit() { Console.PrintLine("online " + Gateway.Online); }
                    public void OnTick() { }
                    public void OnDestroy() { }
                }
                """), new Machine(false));
        assertEquals(Process.State.HALTED, process.state());
        assertTrue(process.message().contains("Gateway"), process.message());
    }
}
