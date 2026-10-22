/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.format.IConfigComments;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.log.LogSystem;
import dev.jstech.core.log.LogSystems;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.CrashReportCallables;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * What the Core writes down for whoever looks into a fault: its section of a crash report, with what each thread was
 * running as a section, the sections slowed or stalled and the state of the systems; and the diagnostic file,
 * {@code /jstech diagnostics}, which adds the mods, the settings, the timings, the network use and the log's levels,
 * for a player to attach to a bug report.
 */
public final class DiagnosticReports {

    /** The folder in the game's own the diagnostic files are written into. */
    public static final String FOLDER = "jstech-diagnostics";

    private static final Logger LOGGER = CoreLogs.DIAGNOSTICS.logger();
    private static final String CRASH_LABEL = "J's Core";
    private static final String CRASH_INDENT = "\n\t\t";
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);
    private static final int KINDS_SHOWN = 10;
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    private DiagnosticReports() {
    }

    /** Adds the Core's section to every crash report, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        CrashReportCallables.registerCrashCallable(CRASH_LABEL, DiagnosticReports::crashSection);
    }

    /**
     * The Core's section of a crash report: what each thread was running as a section, the sections slowed or
     * stalled, and, when the crash is on the server's thread, the state of the systems.
     */
    public static String crashSection() {
        final List<String> lines = new ArrayList<>(running());
        lines.addAll(paced());
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) {
            lines.addAll(health(server));
        }
        return lines.isEmpty() ? "nothing running" : String.join(CRASH_INDENT, lines);
    }

    /** The whole diagnostic file, as it is written. */
    public static String bundle(final MinecraftServer server) {
        final StringBuilder out = new StringBuilder();
        out.append("J's Tech Series diagnostics, written ").append(LocalDateTime.now()).append('\n');
        out.append("Minecraft ").append(SharedConstants.getCurrentVersion().getName())
                .append(", NeoForge ").append(version("neoforge"))
                .append(", Java ").append(System.getProperty("java.version"))
                .append(" (").append(System.getProperty("java.vendor")).append("), ")
                .append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.version"))
                .append(' ').append(System.getProperty("os.arch")).append('\n');
        final Runtime runtime = Runtime.getRuntime();
        out.append("Memory: ").append((runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MEGABYTE)
                .append(" MB used of ").append(runtime.maxMemory() / BYTES_PER_MEGABYTE).append(" MB; ")
                .append(server.isDedicatedServer() ? "a dedicated server" : "a single-player world")
                .append(" with ").append(server.getPlayerCount()).append(" players on\n");
        heading(out, "Mods");
        for (final IModInfo mod : ModList.get().getMods()) {
            out.append(mod.getModId()).append(' ').append(mod.getVersion()).append('\n');
        }
        heading(out, "Settings");
        for (final ConfigFile file : ConfigFiles.all()) {
            out.append("-- ").append(file.fileName()).append(", read on the ")
                    .append(file.side().name().toLowerCase(Locale.ROOT)).append(" --\n")
                    .append(new String(file.format().write(file.toPlain(), IConfigComments.NONE),
                            StandardCharsets.UTF_8).strip()).append('\n');
        }
        heading(out, "Timings");
        for (final Timings.Summary summary : Diagnostics.timings().summaries()) {
            out.append(summary.section()).append(": ").append(Diagnostics.millis(summary.averageNanos()))
                    .append(" ms on average, ").append(Diagnostics.millis(summary.maxNanos())).append(" ms at most\n");
        }
        heading(out, "Sections");
        for (final Section section : Sections.all()) {
            out.append(section).append(" of ").append(section.system()).append(": ")
                    .append(Diagnostics.millis(section.averageNanos())).append(" ms a tick");
            if (section.budgetNanos() > 0L) {
                out.append(", budget ").append(Diagnostics.millis(section.budgetNanos())).append(" ms, one tick in ")
                        .append(section.stride());
            }
            if (section.stalls() > 0) {
                out.append(", ").append(section.stalls()).append(" stalls, the longest ")
                        .append(Diagnostics.millis(section.longestStallNanos())).append(" ms");
            }
            out.append('\n');
        }
        lines(out, running());
        heading(out, "Health");
        lines(out, health(server));
        heading(out, "Network use");
        final BandwidthLedger everyone = Bandwidth.everyone();
        out.append(Bandwidth.rate(everyone.bytesPerSecond())).append(" over the last minute to ")
                .append(Bandwidth.countedPlayers()).append(" players counted: ")
                .append(Bandwidth.graph(everyone.lastMinute())).append('\n');
        final List<BandwidthLedger.Rate> kinds = everyone.kinds();
        for (final BandwidthLedger.Rate kind : kinds.subList(0, Math.min(KINDS_SHOWN, kinds.size()))) {
            out.append(kind.name()).append(": ").append(Bandwidth.rate(kind.bytesPerSecond())).append('\n');
        }
        heading(out, "Log levels");
        for (final LogSystem system : LogSystems.all()) {
            out.append(system.id()).append(": ").append(LogSystems.level(system).serializedName()).append('\n');
        }
        return out.toString();
    }

    /**
     * Writes the diagnostic file into {@value #FOLDER}, and beside it the same as a page a browser opens
     * ({@link DiagnosticPage}), and hands back where the file is.
     */
    public static Path write(final MinecraftServer server) throws IOException {
        final Path folder = FMLPaths.GAMEDIR.get().resolve(FOLDER);
        Files.createDirectories(folder);
        final String name = "diagnostics-" + FILE_TIME.format(LocalDateTime.now());
        final Path file = folder.resolve(name + ".txt");
        final String said = bundle(server);
        Files.writeString(file, said, StandardCharsets.UTF_8);
        Files.writeString(folder.resolve(name + ".html"), DiagnosticPage.html(name, said), StandardCharsets.UTF_8);
        LOGGER.info("Wrote the diagnostics to {}", file);
        return file;
    }

    /* What each thread runs as a section now. */
    private static List<String> running() {
        final List<String> out = new ArrayList<>();
        for (final Sections.Now now : Sections.runningNow()) {
            out.add("Running " + now.section() + " on " + now.thread() + " for " + Diagnostics.millis(now.nanos())
                    + " ms");
        }
        return out;
    }

    /* The sections slowed by their budget, and the ones that stalled. */
    private static List<String> paced() {
        final List<String> out = new ArrayList<>();
        for (final Section section : Sections.all()) {
            if (section.slowed()) {
                out.add("Slowed " + section + " to one tick in " + section.stride());
            }
            if (section.stalls() > 0) {
                out.add("Stalled " + section + " " + section.stalls() + " times");
            }
        }
        return out;
    }

    /* The state of the systems, in English, a system's id before each line and a warning marked. */
    private static List<String> health(final MinecraftServer server) {
        final List<String> out = new ArrayList<>();
        for (final Health.Part part : Health.read(server)) {
            for (final Health.Line line : part.lines()) {
                out.add("[" + part.system().id() + "] " + (line.warning() ? "WARNING " : "") + line.text().english());
            }
        }
        return out;
    }

    private static String version(final String modid) {
        return ModList.get().getModContainerById(modid)
                .map(container -> container.getModInfo().getVersion().toString()).orElse("?");
    }

    private static void heading(final StringBuilder out, final String title) {
        out.append("\n== ").append(title).append(" ==\n");
    }

    private static void lines(final StringBuilder out, final List<String> lines) {
        for (final String line : lines) {
            out.append(line).append('\n');
        }
    }
}
