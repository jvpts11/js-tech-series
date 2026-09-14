/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The virtual machine runs a program with nothing of the game around it: no Minecraft, no NeoForge, no ComputerCraft,
 * no network library, none of the machines that host it and none of the language that compiles for it. These read
 * every source under {@code vm} and hold that line: an import comes from the JDK, from the Core's stable ids or from
 * the vm itself, and no other package of the series or of the game is named anywhere in the text.
 *
 * <p>The whole text is read, comments and strings included: a class named in a string can still be loaded by
 * reflection, and a comment that has to name the machine or the language describes something the vm should not know.
 */
class VmPurityTest {

    /** What an import may name: the JDK, the Core's stable ids and the vm. */
    private static final List<String> IMPORTABLE = List.of("java.", "dev.jstech.core.id.", "dev.jstech.computers.vm.");

    /** The packages of the series the text may name, each with everything under it. */
    private static final List<String> NAMEABLE = List.of("dev.jstech.core.id", "dev.jstech.computers.vm");

    private static final Pattern IMPORT =
            Pattern.compile("(?m)^[ \\t]*import\\s+(?:static\\s+)?([\\w.]+?)(?:\\.\\*)?\\s*;");
    private static final Pattern HEADER_LINE = Pattern.compile("(?m)^[ \\t]*(?:package|import)\\s[^\\n]*");
    private static final Pattern GAME_PACKAGE =
            Pattern.compile("\\b(?:net\\.minecraft|net\\.neoforged|com\\.mojang|dan200|io\\.netty)");
    private static final Pattern SERIES_PACKAGE = Pattern.compile("\\bdev\\.jstech(?:\\.\\w+)*");

    /** A source that reaches out of the vm once per form, with how many findings each rule owes it. */
    private static final String FORBIDDEN = """
            package dev.jstech.computers.vm.sample;

            import static net.minecraft.util.Mth.clamp;

            import dev.jstech.computers.sigma.SigmaError;
            import dev.jstech.core.network.NetworkUuid;
            import it.unimi.dsi.fastutil.ints.IntList;

            final class Sample {
                Object host() {
                    return dev.jstech.computers.machine.MachineHost.class;
                }

                Object nearby() {
                    return dev.jstech.computers.vmware.Guest.class;
                }

                Object loaded() throws ReflectiveOperationException {
                    return Class.forName("net.neoforged.fml.ModList");
                }

                // Written the way io.netty would, and kept where dev.jstech.core.idle keeps it.
            }
            """;

    /** A source that stays inside the vm while coming close to every form the rules forbid. */
    private static final String ALLOWED = """
            package dev.jstech.computers.vm.sample;

            import static java.lang.Math.max;

            import dev.jstech.computers.vm.listing.Opcode;
            import dev.jstech.core.id.IStableName;
            import java.util.List;

            /** Runs a {@link dev.jstech.computers.vm.program.Process} with no Minecraft, NeoForge or ComputerCraft around it. */
            final class Sample {
                IStableName kind;

                String names() {
                    return "dev.jstech.core.id and dev.jstech.computers.vm.";
                }

                int count(final List<Opcode> code) {
                    return max(code.size(), 1); // a mojang or netty word alone names no package
                }
            }
            """;

    @Test
    void vmSources_areFound() {
        for (final Path expected : List.of(Path.of("program", "Process.java"), Path.of("listing", "AsmReader.java"))) {
            assertTrue(VmSources.ALL.containsKey(expected.toString()),
                    () -> "the sources read are " + VmSources.ALL.keySet());
        }
    }

    @Test
    void imports_comeFromTheJdkTheStableIdsOrTheVm() {
        assertNone("imports from outside the vm", outsideImports(VmSources.ALL));
    }

    @Test
    void text_namesNoPackageOutsideTheVm() {
        assertNone("packages named from outside the vm", outsideNames(VmSources.ALL));
    }

    @Test
    void rules_findEveryReachOutOfTheVm() {
        final Map<String, String> sample = Map.of("sample/Sample.java", FORBIDDEN);
        assertEquals(4, outsideImports(sample).size(), () -> "imports: " + outsideImports(sample));
        assertEquals(5, outsideNames(sample).size(), () -> "names: " + outsideNames(sample));
    }

    @Test
    void rules_letThroughWhatTheVmMayUse() {
        final Map<String, String> sample = Map.of("sample/Sample.java", ALLOWED);
        assertNone("imports from outside the vm", outsideImports(sample));
        assertNone("packages named from outside the vm", outsideNames(sample));
    }

    private static List<String> outsideImports(final Map<String, String> sources) {
        final List<String> found = new ArrayList<>();
        for (final Map.Entry<String, String> file : sources.entrySet()) {
            final Matcher imported = IMPORT.matcher(file.getValue());
            while (imported.find()) {
                final String name = imported.group(1);
                if (IMPORTABLE.stream().noneMatch(name::startsWith)) {
                    found.add(where(file.getKey(), file.getValue(), imported.start()) + " " + name);
                }
            }
        }
        return found;
    }

    private static List<String> outsideNames(final Map<String, String> sources) {
        final List<String> found = new ArrayList<>();
        for (final Map.Entry<String, String> file : sources.entrySet()) {
            // Package and import lines belong to the import rule; blanking them keeps every other line where it was.
            final String text = HEADER_LINE.matcher(file.getValue()).replaceAll("");
            final Matcher game = GAME_PACKAGE.matcher(text);
            while (game.find()) {
                found.add(where(file.getKey(), text, game.start()) + " " + game.group());
            }
            final Matcher series = SERIES_PACKAGE.matcher(text);
            while (series.find()) {
                final String name = series.group();
                if (NAMEABLE.stream().noneMatch(root -> name.equals(root) || name.startsWith(root + "."))) {
                    found.add(where(file.getKey(), text, series.start()) + " " + name);
                }
            }
        }
        return found;
    }

    private static String where(final String label, final String text, final int at) {
        int line = 1;
        for (int i = 0; i < at; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return label + ":" + line;
    }

    private static void assertNone(final String what, final List<String> found) {
        assertTrue(found.isEmpty(), () -> found.size() + " " + what + ":\n" + String.join("\n", found));
    }

    /** Every source under the vm package, by its path inside the package, read once for all the rules. */
    private static final class VmSources {

        static final Map<String, String> ALL = load();

        private static Map<String, String> load() {
            final Path vm = Path.of("src", "main", "java", "dev", "jstech", "computers", "vm");
            final Map<String, String> texts = new LinkedHashMap<>();
            try (Stream<Path> walk = Files.walk(vm)) {
                for (final Path file : walk.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                    texts.put(vm.relativize(file).toString(), Files.readString(file, StandardCharsets.UTF_8));
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
            return texts;
        }
    }
}
