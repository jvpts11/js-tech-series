/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.os.player.PlayerDesktop;
import dev.jstech.computers.sigma.DesktopFacts;
import dev.jstech.computers.sigma.Diagnostic;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.sigma.SystemFacts;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code sigma build --os} and {@code sigma build --desktop} (computers/docs/design/player-systems.md): an operating
 * system a player wrote is compiled from its folder, its services each written out at their own class, kept for the
 * world, and its installer written onto a medium of its era in one of the machine's drives; a desktop's look is read
 * from its folder, and its programs and pictures gathered with it.
 */
@TextHolder
final class SystemBuild {

    /* The sources compiled, by name. */
    private static final TextKey COMPILING = TextKey.of("jsc.cli.system_build.compiling", "Compiling %s... ok");
    private static final TextKey NO_SOURCES = TextKey.of("jsc.cli.system_build.no_sources",
            "%s holds no Σ# sources (.sgs)");
    private static final TextKey NO_SYSTEM = TextKey.of("jsc.cli.system_build.no_system",
            "no class in %s implements ISystem");
    /* The system, its author, its era, its instruction set, its memory and its kernel. */
    private static final TextKey SYSTEM_LINE = TextKey.of("jsc.cli.system_build.system_line",
            "%s by %s: %s, %s, %s MB, on the %s kernel");
    private static final TextKey OWN_KERNEL_LINE = TextKey.of("jsc.cli.system_build.own_kernel_line",
            "%s by %s: %s, %s, %s MB, on a kernel of its own");
    /* The medium and the drive it is in, and the system's size. */
    private static final TextKey WRITING = TextKey.of("jsc.cli.system_build.writing",
            "Writing the installer to %s... done (%s MB)");
    private static final TextKey NO_MEDIUM = TextKey.of("jsc.cli.system_build.no_medium",
            "No blank medium that can be written is in a drive: the system is kept, and sgpack can publish it to"
                    + " the Mirror.");
    private static final TextKey NO_DESKTOP = TextKey.of("jsc.cli.system_build.no_desktop",
            "no folder here declares a desktop called %s");
    private static final TextKey NOT_BUILT = TextKey.of("jsc.cli.system_build.not_built",
            "%s is not built: build it into %s first");
    private static final TextKey NOT_THERE = TextKey.of("jsc.cli.system_build.not_there", "%s is not there");
    /* The desktop, how many programs it brings, and its size. */
    private static final TextKey DESKTOP_LINE = TextKey.of("jsc.cli.system_build.desktop_line",
            "%s: the look, %s programs, the wallpaper... ok (%s MB)");
    private static final TextKey DESKTOP_ONE_LINE = TextKey.of("jsc.cli.system_build.desktop_one_line",
            "%s: the look, 1 program, the wallpaper... ok (%s MB)");
    /* The shape of every error count, which sgsc already says. */
    private static final TextKey ONE_ERROR = TextKey.of("jsc.cli.system_build.one_error", "1 error");
    private static final TextKey ERRORS = TextKey.of("jsc.cli.system_build.errors", "%s errors");

    /** The extension of a source this build compiles. */
    private static final String SOURCE = ".sgs";
    /** The extension of a listing, the build's own output and a desktop's programs. */
    private static final String LISTING = ".asm";
    /** The listing a system runs from, inside its folder. */
    private static final String SYSTEM_LISTING = "system.asm";

    private SystemBuild() {
    }

    /** {@code sigma build --os <folder>}. */
    static void os(final CliContext ctx, final String verb, final String folder) {
        final List<SourceFile> sources = sources(ctx, folder);
        if (sources.isEmpty()) {
            ctx.out().error(CliTexts.SAID_BY.with(verb, NO_SOURCES.with(folder)));
            return;
        }
        final SigmaCompiler.Result first = SigmaCompiler.compile(sources);
        if (!reported(ctx, verb, first)) {
            return;
        }
        final SystemFacts facts = first.system();
        if (facts == null) {
            ctx.out().error(CliTexts.SAID_BY.with(verb, NO_SYSTEM.with(folder)));
            return;
        }
        // Built for the instruction set it says it is for, and each of its services the same way.
        final SigmaCompiler.Result system = SigmaCompiler.compile(sources, facts.isaId());
        if (!reported(ctx, verb, system)) {
            return;
        }
        final Map<String, String> listings = new LinkedHashMap<>();
        listings.put(SYSTEM_LISTING, system.assembly());
        for (final String service : facts.services()) {
            final SigmaCompiler.Result built = SigmaCompiler.compileStartingAt(sources, facts.isaId(), service);
            if (!reported(ctx, verb, built)) {
                return;
            }
            listings.put(service.toLowerCase(Locale.ROOT) + LISTING, built.assembly());
        }
        for (final Map.Entry<String, String> listing : listings.entrySet()) {
            final ICliComputer.FsResult written = ctx.computer().writeFile(join(folder, listing.getKey()),
                    listing.getValue());
            if (!written.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(verb, written.message()));
                return;
            }
        }
        ctx.out().ok(COMPILING.with(namesOf(sources)));
        ctx.out().ok(facts.kernel().own()
                ? OWN_KERNEL_LINE.with(facts.label(), facts.author(), eraText(facts.era()), facts.isaWritten(),
                facts.memoryMb())
                : SYSTEM_LINE.with(facts.label(), facts.author(), eraText(facts.era()), facts.isaWritten(),
                facts.memoryMb(), facts.kernel().base()));
        final ICliSystems.Kept kept = ctx.computer().keepSystem(facts, folder, filesOf(ctx, folder));
        if (!kept.kept()) {
            ctx.out().error(CliTexts.SAID_BY.with(verb, kept.failure()));
            return;
        }
        if (kept.medium().isEmpty()) {
            ctx.out().dim(NO_MEDIUM.text());
        } else {
            ctx.out().ok(WRITING.with(kept.medium(), kept.sizeMb()));
        }
    }

    /**
     * {@code sigma build --desktop <name>}: the folder of that name, or the one here whose sources declare a desktop
     * of that name, as a system's folder holds its desktop beside it.
     */
    static void desktop(final CliContext ctx, final String verb, final String name) {
        DesktopFacts facts = null;
        String folder = null;
        final List<String> candidates = new ArrayList<>();
        candidates.add(name);
        for (final String entry : ctx.computer().fileNames()) {
            if (entry.endsWith("/") && !entry.equals(name + "/")) {
                candidates.add(entry.substring(0, entry.length() - 1));
            }
        }
        for (final String candidate : candidates) {
            final List<SourceFile> sources = sources(ctx, candidate);
            final DesktopFacts found = sources.isEmpty() ? null : SigmaCompiler.declared(sources).desktop();
            if (found != null && (candidate.equals(name) || found.name().equals(name))) {
                facts = found;
                folder = candidate;
                break;
            }
        }
        if (facts == null) {
            ctx.out().error(CliTexts.SAID_BY.with(verb, NO_DESKTOP.with(name)));
            return;
        }
        final Map<String, String> files = new LinkedHashMap<>();
        for (final String program : facts.programs()) {
            final String path = join(folder, program + LISTING);
            final ICliComputer.FsResult read = ctx.computer().readFile(path);
            if (!read.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(verb, NOT_BUILT.with(program, path)));
                return;
            }
            files.put(program + PlayerDesktop.LISTING, read.message().english());
            // A program with no picture among the desktop's icons is listed in its menu with a plain mark.
            final ICliComputer.FsResult picture = ctx.computer().readFile(join(facts.icons(),
                    program + PlayerDesktop.ICON));
            if (picture.ok()) {
                files.put(program + PlayerDesktop.ICON, picture.message().english());
            }
        }
        final ICliComputer.FsResult menu = ctx.computer().readFile(join(facts.icons(), PlayerDesktop.MENU_ICON));
        if (menu.ok()) {
            files.put(PlayerDesktop.MENU_ICON, menu.message().english());
        }
        final ICliComputer.FsResult wallpaper = ctx.computer().readFile(facts.wallpaper());
        if (!wallpaper.ok()) {
            ctx.out().error(CliTexts.SAID_BY.with(verb, NOT_THERE.with(facts.wallpaper())));
            return;
        }
        files.put(PlayerDesktop.WALLPAPER, wallpaper.message().english());
        final ICliSystems.Kept kept = ctx.computer().keepDesktop(facts, folder, files);
        if (!kept.kept()) {
            ctx.out().error(CliTexts.SAID_BY.with(verb, kept.failure()));
            return;
        }
        final int programs = facts.programs().size();
        ctx.out().ok(programs == 1 ? DESKTOP_ONE_LINE.with(facts.label(), kept.sizeMb())
                : DESKTOP_LINE.with(facts.label(), programs, kept.sizeMb()));
    }

    /* Every source in the folder, read; empty when it holds none or is not there. */
    private static List<SourceFile> sources(final CliContext ctx, final String folder) {
        final List<SourceFile> out = new ArrayList<>();
        for (final String entry : ctx.computer().fileNames(folder)) {
            if (!entry.endsWith(SOURCE)) {
                continue;
            }
            final ICliComputer.FsResult read = ctx.computer().readFile(join(folder, entry));
            if (read.ok()) {
                out.add(new SourceFile(entry, read.message().english()));
            }
        }
        return out;
    }

    /* Every file in the folder and the folders inside it, by its path inside the folder. */
    private static Map<String, String> filesOf(final CliContext ctx, final String folder) {
        final Map<String, String> files = new LinkedHashMap<>();
        gather(ctx, folder, "", files);
        return files;
    }

    private static void gather(final CliContext ctx, final String folder, final String inside,
                               final Map<String, String> files) {
        for (final String entry : ctx.computer().fileNames(join(folder, inside))) {
            if (entry.endsWith("/")) {
                gather(ctx, folder, inside + entry, files);
                continue;
            }
            final ICliComputer.FsResult read = ctx.computer().readFile(join(folder, inside + entry));
            if (read.ok()) {
                files.put(inside + entry, read.message().english());
            }
        }
    }

    /* Prints what the compiler said; answers whether it built. */
    private static boolean reported(final CliContext ctx, final String verb, final SigmaCompiler.Result result) {
        for (final Diagnostic diagnostic : result.diagnostics()) {
            if (diagnostic.isError()) {
                ctx.out().error(diagnostic.text());
            } else {
                ctx.out().dim(diagnostic.text());
            }
        }
        if (result.ok()) {
            return true;
        }
        ctx.computer().report(JscEvents.SIGMA_COMPILE_ERROR, "");
        final long errors = result.diagnostics().stream().filter(Diagnostic::isError).count();
        ctx.out().error(CliTexts.SAID_BY.with(verb, errors == 1 ? ONE_ERROR.text() : ERRORS.with(errors)));
        return false;
    }

    private static String namesOf(final List<SourceFile> sources) {
        final List<String> names = new ArrayList<>();
        for (final SourceFile source : sources) {
            names.add(source.name());
        }
        return String.join(", ", names);
    }

    private static Text eraText(final String named) {
        final Era era = switch (named) {
            case "Vintage" -> HardwareEra.VINTAGE;
            case "Transition" -> HardwareEra.TRANSITION;
            case "Standard" -> HardwareEra.STANDARD;
            case "Advanced" -> HardwareEra.ADVANCED;
            default -> HardwareEra.LEGACY;
        };
        return era.text();
    }

    private static String join(final String folder, final String name) {
        if (folder.isEmpty()) {
            return name;
        }
        return folder.endsWith("/") ? folder + name : folder + "/" + name;
    }
}
