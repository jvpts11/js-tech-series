/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.pack.Manifest;
import dev.jstech.computers.sigma.pack.Packed;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Packages a player writes, and packages a player is about to install from someone else.
 *
 * <p>The second is why all of this is plain text: what matters most here is that a manifest says what
 * it means, that a bad one says exactly which line to fix, and that what comes out of a Mirror is the
 * same thing that went in.
 */
class PackedTest {

    private static Packed built() {
        final Map<String, String> files = new LinkedHashMap<>();
        files.put("stockwatch.asm", ".asm 1\n.start Watcher script\n");
        files.put("readme.txt", "Watches the iron.\n");
        final Manifest manifest = new Manifest("stockwatch", "1.2.0", "jvpts11", "stockwatch.asm",
                "bell", 2, List.of("stockwatch.asm", "readme.txt"), "Tells you when the iron runs low");
        return new Packed(manifest, files);
    }

    @Test
    void write_thenRead_givesBackWhatWentIn() {
        final Packed before = built();
        final Packed after = Packed.read(before.write());
        assertEquals(before.manifest(), after.manifest());
        assertEquals(before.files(), after.files());
        assertTrue(after.problems().isEmpty(), () -> String.join("\n", after.problems()));
    }

    @Test
    void write_thenRead_keepsALineThatLooksLikeTheStartOfAFile() {
        final Map<String, String> files = new LinkedHashMap<>();
        files.put("stockwatch.asm", ".asm 1\n.start Watcher script\n");
        files.put("readme.txt", "Watches the iron.\n--- how it works\n\\n is a new line\n---\n");
        final Manifest manifest = new Manifest("stockwatch", "1.2.0", "jvpts11", "stockwatch.asm",
                "bell", 2, List.of("stockwatch.asm", "readme.txt"), "Tells you when the iron runs low");
        final Packed before = new Packed(manifest, files);

        final Packed after = Packed.read(before.write());
        assertEquals(before.files(), after.files());
        assertTrue(after.problems().isEmpty(), () -> String.join("\n", after.problems()));
    }

    @Test
    void write_putsThePackageInOnePieceOfReadableText() {
        final String text = built().write();
        assertTrue(text.startsWith(".pkg 1\n"), text);
        assertTrue(text.contains("name: stockwatch"), text);
        assertTrue(text.contains("--- stockwatch.asm"), text);
        // Someone about to install this can read every line of what they are installing.
        assertTrue(text.contains(".start Watcher script"), text);
    }

    @Test
    void read_refusesTextThatIsNotAPackage() {
        assertNull(Packed.read("just some notes"));
        assertNull(Packed.read(""));
        assertNull(Packed.read(null));
    }

    @Test
    void fileName_saysWhatItIsAndWhichBuild() {
        assertEquals("stockwatch-1.2.0.cpk", built().fileName());
    }

    @Test
    void problems_namesTheLineToFixForEachThingWrong() {
        final Manifest bad = new Manifest("Stock Watch", "1.2", "jvpts11", "watch.sgs", "sparkle", 0,
                List.of("watch.sgs"), "");
        final List<String> found = bad.problems();
        assertTrue(found.stream().anyMatch(p -> p.startsWith("name:")), () -> found.toString());
        assertTrue(found.stream().anyMatch(p -> p.startsWith("version:")), () -> found.toString());
        assertTrue(found.stream().anyMatch(p -> p.startsWith("entry:")), () -> found.toString());
        assertTrue(found.stream().anyMatch(p -> p.startsWith("icon:")), () -> found.toString());
        assertTrue(found.stream().anyMatch(p -> p.startsWith("ram:")), () -> found.toString());
    }

    @Test
    void problems_catchesAPackageWhoseFilesAndManifestDisagree() {
        final Packed missing = new Packed(built().manifest(),
                Map.of("stockwatch.asm", ".asm 1\n.start Watcher script\n"));
        assertTrue(missing.problems().stream().anyMatch(p -> p.contains("readme.txt")),
                () -> missing.problems().toString());
        final Map<String, String> extra = new LinkedHashMap<>(built().files());
        extra.put("secret.asm", ".asm 1\n");
        assertTrue(new Packed(built().manifest(), extra).problems().stream()
                        .anyMatch(p -> p.contains("secret.asm")),
                "a file nobody declared is not quietly installed");
    }

    @Test
    void problems_refusesAFileNamedToClimbOutOfThePackageFolder() {
        final Manifest manifest = new Manifest("stockwatch", "1.2.0", "jvpts11", "stockwatch.asm",
                "bell", 2, List.of("stockwatch.asm", "../../AUTOEXEC.TXT"), "");
        final Map<String, String> files = new LinkedHashMap<>();
        files.put("stockwatch.asm", ".asm 1\n.start Watcher script\n");
        files.put("../../AUTOEXEC.TXT", "overwritten\n");
        final List<String> found = new Packed(manifest, files).problems();
        assertTrue(found.stream().anyMatch(p -> p.contains("AUTOEXEC.TXT")), () -> found.toString());
    }

    @Test
    void isSafeFileName_acceptsPlainNamesAndSubfoldersOnly() {
        assertTrue(Packed.isSafeFileName("stockwatch.asm"));
        assertTrue(Packed.isSafeFileName("lib/util.asm"));
        assertFalse(Packed.isSafeFileName(""));
        assertFalse(Packed.isSafeFileName(".."));
        assertFalse(Packed.isSafeFileName("a/../b"));
        assertFalse(Packed.isSafeFileName("/etc/x"));
        assertFalse(Packed.isSafeFileName("a//b"));
        assertFalse(Packed.isSafeFileName("..\\x"));
        assertFalse(Packed.isSafeFileName("C:x"));
    }

    @Test
    void read_passesOverALineFromALaterVersionRatherThanRefusingTheWhole() {
        final Manifest read = Manifest.read("""
                name: stockwatch
                version: 1.0.0
                entry: stockwatch.asm
                file: stockwatch.asm
                signature: something a later build writes
                """);
        assertEquals("stockwatch", read.name());
        assertTrue(read.problems().isEmpty(), () -> read.problems().toString());
    }

    @Test
    void read_ignoresCommentsAndBlankLines() {
        final Manifest read = Manifest.read("""
                # what this is
                name: stockwatch

                version: 1.0.0   
                entry: stockwatch.asm
                file: stockwatch.asm
                """);
        assertEquals("stockwatch", read.name());
        assertEquals("1.0.0", read.version());
    }

    @Test
    void fresh_startsAProjectThatIsAlreadyValid() {
        final Manifest made = Manifest.fresh("stockwatch", "jvpts11");
        assertTrue(made.problems().isEmpty(), () -> made.problems().toString());
        assertEquals("stockwatch.asm", made.entry());
        assertFalse(made.write().isBlank());
    }

    @Test
    void read_keepsAHashInsideAValue() {
        final Manifest before = new Manifest("stockwatch", "1.0.0", "Team #1", "stockwatch.asm", "bell", 1,
                List.of("stockwatch.asm"), "use # for notes");
        final Manifest after = Manifest.read(before.write());
        assertEquals("use # for notes", after.about());
        assertEquals("Team #1", after.house());
    }

    @Test
    void write_thenRead_givesBackAFileWithNoFinalNewlineAsItWas() {
        final Map<String, String> files = new LinkedHashMap<>();
        files.put("a.txt", "x");
        files.put("b.txt", "");
        files.put("c.txt", "y\n");
        final Manifest manifest = new Manifest("stockwatch", "1.0.0", "jvpts11", "a.txt", "bell", 1,
                List.of("a.txt", "b.txt", "c.txt"), "");
        final Packed after = Packed.read(new Packed(manifest, files).write());
        assertEquals(files, after.files());
    }

    @Test
    void read_refusesAHeadThatOnlyStartsLikeTheFormat() {
        assertNull(Packed.read(".pkg 10\nname: a\n"));
        assertNull(Packed.read(".pkg 1x\n"));
    }

    @Test
    void read_refusesAPackageThatNamesOneFileTwice() {
        final String text = built().write() + "--- readme.txt\nsecond body\n";
        assertNull(Packed.read(text));
    }
}
