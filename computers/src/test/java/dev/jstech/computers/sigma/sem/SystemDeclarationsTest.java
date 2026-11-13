/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.DesktopFacts;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.sigma.SystemFacts;
import java.util.List;
import org.junit.jupiter.api.Test;

class SystemDeclarationsTest {

    private static final String LANTERN = """
            using System.OS.*;
            namespace Lantern;

            class LanternOs : ISystem {
                public string Name => "Lantern OS";
                public string Version => "1.2";
                public string Author => "Alex";
                public Era Era => Era.Legacy;
                public Isa Isa => Isa.IA32;
                public int MemoryMb => 48;
                public Kernel Kernel => Kernel.Of("linux");

                public void OnBoot() {
                    Boot.Line("Lighting the lantern...");
                    Boot.Progress(60);
                    Boot.Done();
                }
                public void OnTick() { }
                public void OnInput(InputEvent e) { }
                public void OnShutdown() { }
            }
            """;

    @Test
    void compile_readsTheLanternsDeclarations() {
        final SigmaCompiler.Result result = compile(LANTERN);
        assertTrue(result.ok(), result.lines().toString());
        final SystemFacts system = result.system();
        assertNotNull(system);
        assertEquals("Lantern OS", system.name());
        assertEquals("1.2", system.version());
        assertEquals("Alex", system.author());
        assertEquals("Legacy", system.era());
        assertEquals("IA32", system.isa());
        assertEquals(48, system.memoryMb());
        assertEquals("linux", system.kernel().base());
        assertFalse(system.kernel().own());
    }

    @Test
    void compile_addsTheNumbersAWrittenPropertyStartsWithAsTheLanguageDoes() {
        final SigmaCompiler.Result result = compile(LANTERN.replace("Version => \"1.2\"", "Version => 1 + 2 + \".0\""));
        assertTrue(result.ok(), result.lines().toString());
        assertEquals("3.0", result.system().version());
    }

    @Test
    void compile_startsAProgramThatDeclaresASystemAtItAsASystem() {
        final SigmaCompiler.Result result = compile(LANTERN);
        assertTrue(result.assembly().contains("system"), result.assembly());
    }

    @Test
    void compile_readsAKernelOfItsOwnPolicyByPolicy() {
        final SigmaCompiler.Result result = compile(LANTERN.replace("Kernel.Of(\"linux\")",
                "Kernel.Own().Scheduler(\"cooperative\").FileSystem(\"flat\").Drivers(\"text, sound\")"
                        + ".MemoryRule(\"refuse\").Shell(\"own\")"));
        assertTrue(result.ok(), result.lines().toString());
        final SystemFacts.Kernel kernel = result.system().kernel();
        assertTrue(kernel.own());
        assertEquals("cooperative", kernel.scheduler());
        assertEquals("flat", kernel.fileSystem());
        assertEquals(List.of("text", "sound"), kernel.drivers());
        assertEquals("refuse", kernel.memoryRule());
        assertEquals("own", kernel.shell());
    }

    @Test
    void compile_refusesAPolicyOnOneOfTheModsKernels() {
        final SigmaCompiler.Result result = compile(LANTERN.replace("Kernel.Of(\"linux\")",
                "Kernel.Of(\"linux\").Scheduler(\"none\")"));
        assertFalse(result.ok());
        assertTrue(result.lines().toString().contains("S3065"), result.lines().toString());
    }

    @Test
    void compile_refusesASystemThatDoesNotSayItsName() {
        final SigmaCompiler.Result result = compile(LANTERN.replace("public string Name => \"Lantern OS\";", ""));
        assertFalse(result.ok());
        assertTrue(result.lines().toString().contains("S3062"), result.lines().toString());
    }

    @Test
    void compile_refusesAnArrowValueTheCompilerDoesNotKnow() {
        final SigmaCompiler.Result result = compile(LANTERN.replace("public int MemoryMb => 48;",
                "public int MemoryMb => Computer.RamMb;").replace("using System.OS.*;",
                "using System.OS.*;\nusing System.Machine.*;"));
        assertFalse(result.ok());
        assertTrue(result.lines().toString().contains("S3061"), result.lines().toString());
    }

    @Test
    void compile_refusesAnEraTheMachinesDoNotHave() {
        final SigmaCompiler.Result result = compile(LANTERN.replace("Era.Legacy", "Isa.IA32"));
        assertFalse(result.ok());
    }

    @Test
    void declared_readsADesktopWithNothingToRun() {
        final SigmaCompiler.Result result = SigmaCompiler.declared(List.of(new SourceFile("LanternDesktop.sgs", """
                using System.OS.*;
                namespace Lantern;

                class LanternDesktop : IDesktop {
                    public string Name => "lantern-desktop";
                    public string Version => "1.2";
                    public string Author => "Alex";
                    public string Menu => "Lantern";
                    public string PanelAt => "top";
                    public string Frames => "flat";
                    public string DesktopColour => "#241733";
                    public string PanelColour => "#2E1F44";
                    public string TextColour => "#F4E9D8";
                    public string AccentColour => "#FFD27A";
                    public string WindowColour => "#F6EEE3";
                    public string TitleColour => "#FFD27A";
                    public string Icons => "/lantern/icons";
                    public string Wallpaper => "/lantern/sky.pix";
                    public string Programs => "watchman, notes, files";
                }
                """)));
        final DesktopFacts desktop = result.desktop();
        assertNotNull(desktop, result.lines().toString());
        assertNull(result.system());
        assertEquals("lantern-desktop", desktop.name());
        assertEquals("top", desktop.panelAt());
        assertEquals("#FFD27A", desktop.colours().get("AccentColour"));
        assertEquals(List.of("watchman", "notes", "files"), desktop.programs());
    }

    private static SigmaCompiler.Result compile(final String source) {
        return SigmaCompiler.compile(List.of(new SourceFile("LanternOs.sgs", source)));
    }
}
