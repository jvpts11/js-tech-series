/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.IMainframeService;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.menu.CommandPromptMenu;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.CommandOutputPayload;
import dev.jstech.computers.operation.payload.DesktopShellOutputPayload;
import dev.jstech.computers.operation.payload.SetupProgressPayload;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.os.SoftwareHouse;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Installing a program as something a machine does over time, rather than a flag that flips.
 *
 * <p>Every way of starting an install ends up here: the disc's setup program, This PC's button, the
 * prompt's {@code install}, the package manager. The job is held by the machine's console, ticked by
 * the machine, and told to every window looking at it. On Frames that is a Setup window on the desktop
 * and a bar at the prompt; on Linux it is the terminal alone, drawn the way the package manager that
 * was asked draws it, since no Linux ever put up a Setup window for {@code apt}.
 */
@TextHolder
public final class SetupRunner {

    /** How often a running job is told to the windows and the prompt, in ticks. */
    private static final int PUSH_EVERY = 10;

    /** Where a program comes from, as the Setup window words it. */
    private static final TextKey FROM_MIRROR = TextKey.of("jsc.install.setup_runner.from_mirror", "the Mirror");
    private static final TextKey FROM_FLOPPY = TextKey.of("jsc.install.setup_runner.from_floppy", "floppy");
    private static final TextKey FROM_CD = TextKey.of("jsc.install.setup_runner.from_cd", "CD");
    private static final TextKey FROM_DVD = TextKey.of("jsc.install.setup_runner.from_dvd", "DVD");
    private static final TextKey FROM_USB = TextKey.of("jsc.install.setup_runner.from_usb", "USB drive");

    private static final TextKey STILL_SETTING_UP = TextKey.of("jsc.install.setup_runner.still_setting_up",
            "This computer is still setting up %s.");
    private static final TextKey WAS_CANCELLED = TextKey.of("jsc.install.setup_runner.was_cancelled",
            "Setup was cancelled.");
    private static final TextKey CANCELLED_NOTHING_REMOVED =
            TextKey.of("jsc.install.setup_runner.cancelled_nothing_removed", "Setup cancelled. Nothing was removed.");
    private static final TextKey CANCELLED_NOTHING_INSTALLED = TextKey.of(
            "jsc.install.setup_runner.cancelled_nothing_installed", "Setup cancelled. Nothing was installed.");

    /*
     * What the package managers print as the bar fills and once it is full. Their own words are translated, as
     * those tools translate them; package names, versions, sizes, rates and times are data.
     */
    private static final TextKey APT_PROGRESS = TextKey.of("jsc.install.setup_runner.apt_progress",
            "Progress: [%s%%] [%s]");
    private static final TextKey PCKMGR_REMOVING = TextKey.of("jsc.install.setup_runner.pckmgr_removing",
            "Removing %s %s  [%s]  %s%%");
    private static final TextKey PCKMGR_DOWNLOADING = TextKey.of("jsc.install.setup_runner.pckmgr_downloading",
            "Downloading %s %s  [%s]  %s%%");
    private static final TextKey APT_REMOVING = TextKey.of("jsc.install.setup_runner.apt_removing",
            "Removing %s (%s) ...");
    private static final TextKey APT_TRIGGERS = TextKey.of("jsc.install.setup_runner.apt_triggers",
            "Processing triggers for %s ...");
    private static final TextKey APT_FETCHED = TextKey.of("jsc.install.setup_runner.apt_fetched",
            "Fetched %s MB in %ss (%s MB/s)");
    private static final TextKey APT_SELECTING = TextKey.of("jsc.install.setup_runner.apt_selecting",
            "Selecting previously unselected package %s.");
    private static final TextKey APT_UNPACKING = TextKey.of("jsc.install.setup_runner.apt_unpacking",
            "Unpacking %s (%s) ...");
    private static final TextKey APT_SETTING_UP = TextKey.of("jsc.install.setup_runner.apt_setting_up",
            "Setting up %s (%s) ...");
    private static final TextKey DNF_RUNNING = TextKey.of("jsc.install.setup_runner.dnf_running",
            "Running transaction");
    private static final TextKey DNF_ERASING = TextKey.of("jsc.install.setup_runner.dnf_erasing",
            "  Erasing          : %s   1/1");
    private static final TextKey DNF_INSTALLING = TextKey.of("jsc.install.setup_runner.dnf_installing",
            "  Installing       : %s   1/1");
    private static final TextKey DNF_REMOVED = TextKey.of("jsc.install.setup_runner.dnf_removed", "Removed:");
    private static final TextKey DNF_INSTALLED = TextKey.of("jsc.install.setup_runner.dnf_installed", "Installed:");
    private static final TextKey DNF_COMPLETE = TextKey.of("jsc.install.setup_runner.dnf_complete", "Complete!");
    private static final TextKey PACMAN_REMOVING = TextKey.of("jsc.install.setup_runner.pacman_removing",
            "(1/1) removing %s");
    private static final TextKey PACMAN_KEYS = TextKey.of("jsc.install.setup_runner.pacman_keys",
            "(1/1) checking keys in keyring");
    private static final TextKey PACMAN_INTEGRITY = TextKey.of("jsc.install.setup_runner.pacman_integrity",
            "(1/1) checking package integrity");
    private static final TextKey PACMAN_INSTALLING = TextKey.of("jsc.install.setup_runner.pacman_installing",
            "(1/1) installing %s");
    private static final TextKey VERSION_REMOVED = TextKey.of("jsc.install.setup_runner.version_removed",
            "%s %s removed.");
    private static final TextKey VERSION_INSTALLED = TextKey.of("jsc.install.setup_runner.version_installed",
            "%s %s installed.");
    private static final TextKey REMOVED = TextKey.of("jsc.install.setup_runner.removed", "%s removed.");
    private static final TextKey INSTALLED = TextKey.of("jsc.install.setup_runner.installed", "%s installed.");

    private SetupRunner() {
    }

    /**
     * Starts installing (or removing) {@code spec} on {@code host}, asked for the plain way: a disc's
     * setup program, or the Frames package manager when there is no disc.
     */
    public static Optional<Text> begin(final IOsHost host, final ServerLevel level, final BlockPos pos,
                                       final ProgramSpec spec, @Nullable final MediaFormat medium,
                                       final boolean removing) {
        return begin(host, level, pos, spec, medium, removing,
                medium == null ? PackageManagerKind.PCKMGR : PackageManagerKind.NONE);
    }

    /**
     * Starts installing (or removing) {@code spec} on {@code host}, or says why it cannot.
     *
     * @param medium  the disc it comes from, or null for the network
     * @param manager the package manager that was asked, or {@link PackageManagerKind#NONE} for a setup
     *                program or the install verb
     * @return the refusal, which was also shown at the machine's windows, or empty when the job began
     */
    public static Optional<Text> begin(final IOsHost host, final ServerLevel level, final BlockPos pos,
                                       final ProgramSpec spec, @Nullable final MediaFormat medium,
                                       final boolean removing, final PackageManagerKind manager) {
        final ComputerConsoleState console = host.console();
        if (console == null) {
            return Optional.of(SetupGate.CANNOT_HOLD.text());
        }
        if (console.setup() != null) {
            return Optional.of(STILL_SETTING_UP.with(console.setup().name()));
        }
        final Optional<Text> refusal = SetupGate.refusal(host, spec, removing, true);
        if (refusal.isPresent()) {
            pushWindow(host, level, pos, refused(pos, spec, removing, refusal.get()));
            return refusal;
        }
        final Text source = medium == null ? FROM_MIRROR.text() : sourceName(medium);
        // A floppy is a floppy, but a newer machine unpacks what it carries that much faster.
        final int factor = SetupTiming.eraFactor(host.displayEra());
        final int ticks = medium == null ? SetupTiming.networkTicks(spec.minDiskMb(), removing, factor)
                : SetupTiming.ticks(spec.minDiskMb(), medium, removing, factor);
        final SetupJob job = new SetupJob(spec.id().toString(), spec.displayName(),
                spec.houseOr(SoftwareHouse.MIDSOFT).name(), spec.minDiskMb(), source, removing, ticks, manager,
                spec.commandName());
        console.beginSetup(job);
        host.setChanged();
        pushWindow(host, level, pos, progress(pos, job, SetupProgressPayload.STATE_RUNNING, Text.EMPTY));
        // Whoever asked has already said what began; the bar follows on the next tick.
        return Optional.empty();
    }

    /** Stops the job, leaving the machine as it was. */
    public static void cancel(final IOsHost host, final ServerLevel level, final BlockPos pos) {
        final ComputerConsoleState console = host.console();
        final SetupJob job = console == null ? null : console.setup();
        if (job == null) {
            return;
        }
        console.clearSetup();
        host.setChanged();
        pushWindow(host, level, pos, progress(pos, job, SetupProgressPayload.STATE_CANCELLED, WAS_CANCELLED.text()));
        promptLines(level, pos, List.of(line((job.removing() ? CANCELLED_NOTHING_REMOVED
                : CANCELLED_NOTHING_INSTALLED).text(), CliStyle.ERROR)), false);
    }

    /** One tick of whatever the machine is setting up, if anything. */
    public static void tick(final IOsHost host, final ServerLevel level, final BlockPos pos) {
        final ComputerConsoleState console = host.console();
        final SetupJob job = console == null ? null : console.setup();
        if (job == null) {
            return;
        }
        final boolean last = job.tick();
        if (last) {
            console.clearSetup();
            finish(host, console, job);
            host.setChanged();
            pushWindow(host, level, pos, progress(pos, job, SetupProgressPayload.STATE_DONE, Text.EMPTY));
            // The bar reaches its end where it stands, then the lines that say what was done follow it.
            promptLines(level, pos, List.of(line(bar(job), CliStyle.PLAIN)), !job.drawBar());
            promptLines(level, pos, finished(job), false);
            return;
        }
        if (job.ticksLeft() % PUSH_EVERY == 0) {
            pushWindow(host, level, pos, progress(pos, job, SetupProgressPayload.STATE_RUNNING, Text.EMPTY));
            // The first drawing takes a line of its own; every one after it grows over that line.
            final boolean first = job.drawBar();
            promptLines(level, pos, List.of(line(bar(job), CliStyle.PLAIN)), !first);
        }
    }

    /**
     * The install itself, on the last tick.
     *
     * <p>The Mainframe's services live on the Mainframe as well as in its console; they used to be
     * switched on by each install path separately, which is how a Mirror installed from its disc once
     * ended up listed but not serving.
     */
    private static void finish(final IOsHost host, final ComputerConsoleState console, final SetupJob job) {
        final String id = job.programId();
        final ResourceLocation program = ResourceLocation.parse(id);
        final String path = program.getPath();
        final IMainframeService service = host instanceof MainframeBlockEntity mainframe
                ? mainframe.service(program) : null;
        if (job.removing()) {
            console.uninstall(id);
            if (service != null) {
                service.uninstall();
            }
            /*
             * Whatever the service was keeping goes with it. A machine that kept a conversation nobody can
             * reach any more, and went on paying for it in disk space, would be keeping a ghost.
             */
            host.serviceUninstalled(program);
            return;
        }
        if (service != null) {
            service.install();
        }
        console.install(id);
        console.setInstalledVersion(id, ProgramVersions.of(id));
        JscEvents.awardHost(host, JscEvents.PROGRAM_INSTALLED, path);
        if (fromMirror(job)) {
            JscEvents.awardHost(host, JscEvents.MIRROR_INSTALL, path);
        }
    }

    /**
     * Whether the job came over the network. Asked of the sentence's key rather than of its words, so a job saved
     * by a build that worded it differently is still recognised.
     */
    private static boolean fromMirror(final SetupJob job) {
        return job.source() instanceof Text.Translated said && said.key().key().equals(FROM_MIRROR.key());
    }

    private static Text sourceName(final MediaFormat medium) {
        return switch (medium) {
            case FLOPPY -> FROM_FLOPPY.text();
            case CD -> FROM_CD.text();
            case DVD -> FROM_DVD.text();
            case USB -> FROM_USB.text();
        };
    }

    /* What the prompt prints, in the dialect of whatever was asked */

    /**
     * The bar as the thing that was asked draws it.
     *
     * <p>{@code apt} keeps a percentage in front of a long bar, {@code dnf} counts megabytes against a
     * rate, {@code pacman} does both on one line, and a setup program from a disc draws a plain bar.
     * Whichever it is, the line is the one line, redrawn.
     */
    static Text bar(final SetupJob job) {
        final int pct = job.permille() / 10;
        final String pkg = job.packageName();
        final String ver = ProgramVersions.of(job.programId());
        return switch (job.manager()) {
            case APT -> APT_PROGRESS.with(pad3(pct), cells(job.permille(), 40, '#', '.'));
            // Nothing but names and figures, which those tools print the same in every language.
            case DNF -> Text.literal(pkg + "-" + ver + "  " + rate(job) + " MB/s | " + doneMb(job) + " MB  "
                    + clock(job));
            case PACMAN -> Text.literal(" " + pkg + "-" + ver + "  " + job.sizeMb() + ".0 MiB  " + rate(job)
                    + " MiB/s " + clock(job) + " [" + cells(job.permille(), 20, '#', '-') + "] " + pad3(pct) + "%");
            case PCKMGR -> (job.removing() ? PCKMGR_REMOVING : PCKMGR_DOWNLOADING)
                    .with(pkg, ver, cells(job.permille(), 20, '#', '.'), pct);
            default -> Text.literal("[" + cells(job.permille(), 20, '#', '.') + "]  " + pct + "%");
        };
    }

    /** What follows the bar once it is full: the manager's own closing lines. */
    static List<WireLine> finished(final SetupJob job) {
        final String pkg = job.packageName();
        final String ver = ProgramVersions.of(job.programId());
        final List<WireLine> out = new ArrayList<>();
        switch (job.manager()) {
            case APT -> {
                if (job.removing()) {
                    out.add(line(APT_REMOVING.with(pkg, ver), CliStyle.PLAIN));
                    out.add(line(APT_TRIGGERS.with(pkg), CliStyle.OK));
                } else {
                    out.add(line(APT_FETCHED.with(job.sizeMb(), seconds(job), rate(job)), CliStyle.PLAIN));
                    out.add(line(APT_SELECTING.with(pkg), CliStyle.PLAIN));
                    out.add(line(APT_UNPACKING.with(pkg, ver), CliStyle.PLAIN));
                    out.add(line(APT_SETTING_UP.with(pkg, ver), CliStyle.OK));
                }
            }
            case DNF -> {
                out.add(line(DNF_RUNNING.text(), CliStyle.PLAIN));
                out.add(line((job.removing() ? DNF_ERASING : DNF_INSTALLING).with(pkg + "-" + ver), CliStyle.PLAIN));
                out.add(line((job.removing() ? DNF_REMOVED : DNF_INSTALLED).text(), CliStyle.PLAIN));
                out.add(line(Text.literal("  " + pkg + "-" + ver), CliStyle.PLAIN));
                out.add(line(DNF_COMPLETE.text(), CliStyle.OK));
            }
            case PACMAN -> {
                if (job.removing()) {
                    out.add(line(PACMAN_REMOVING.with(pkg), CliStyle.OK));
                } else {
                    out.add(line(PACMAN_KEYS.text(), CliStyle.PLAIN));
                    out.add(line(PACMAN_INTEGRITY.text(), CliStyle.PLAIN));
                    out.add(line(PACMAN_INSTALLING.with(pkg), CliStyle.OK));
                }
            }
            case PCKMGR -> out.add(line((job.removing() ? VERSION_REMOVED : VERSION_INSTALLED).with(job.name(), ver),
                    CliStyle.OK));
            default -> out.add(line((job.removing() ? REMOVED : INSTALLED).with(job.name()), CliStyle.OK));
        }
        return out;
    }

    private static String cells(final int permille, final int width, final char full, final char empty) {
        final int filled = permille * width / 1000;
        return String.valueOf(full).repeat(filled) + String.valueOf(empty).repeat(width - filled);
    }

    private static String pad3(final int pct) {
        return pct < 10 ? "  " + pct : pct < 100 ? " " + pct : String.valueOf(pct);
    }

    /** Megabytes a second, from the job's own length: what it carries over how long it takes. */
    private static String rate(final SetupJob job) {
        final double seconds = job.ticksTotal() / (double) SetupTiming.TICKS_PER_SECOND;
        return String.format(Locale.ROOT, "%.1f", Math.max(0.1, job.sizeMb() / Math.max(0.05, seconds)));
    }

    private static String doneMb(final SetupJob job) {
        return String.format(Locale.ROOT, "%.1f", job.sizeMb() * job.permille() / 1000.0);
    }

    private static int seconds(final SetupJob job) {
        return Math.max(1, job.ticksTotal() / SetupTiming.TICKS_PER_SECOND);
    }

    private static String clock(final SetupJob job) {
        final int gone = job.ticksDone() / SetupTiming.TICKS_PER_SECOND;
        return String.format(Locale.ROOT, "%02d:%02d", gone / 60, gone % 60);
    }

    private static WireLine line(final Text text, final CliStyle style) {
        return new WireLine(text, style.id());
    }

    /* Who gets told */

    private static SetupProgressPayload progress(final BlockPos pos, final SetupJob job, final int state,
                                                 final Text message) {
        return new SetupProgressPayload(pos, job.programId(), job.name(), job.house(), job.sizeMb(), job.source(),
                job.permille(), job.phase(), state, message, job.removing());
    }

    private static SetupProgressPayload refused(final BlockPos pos, final ProgramSpec spec, final boolean removing,
                                                final Text message) {
        return new SetupProgressPayload(pos, spec.id().toString(), spec.displayName(),
                spec.houseOr(SoftwareHouse.MIDSOFT).name(), spec.minDiskMb(), Text.EMPTY, 0, Text.EMPTY,
                SetupProgressPayload.STATE_REFUSED, message, removing);
    }

    /**
     * Every player at a Frames desktop of that machine gets the job's state.
     *
     * <p>Only Frames: a Linux desktop installs from its terminal and never put up a Setup window for a
     * package manager, so on Linux the terminal is the whole of it.
     */
    private static void pushWindow(final IOsHost host, final ServerLevel level, final BlockPos pos,
                                   final SetupProgressPayload payload) {
        final OsDef os = host.installedOs();
        if (os == null || os.platform() != Platform.FRAMES) {
            return;
        }
        for (final ServerPlayer player : level.players()) {
            if (player.containerMenu instanceof DesktopMenu desk && pos.equals(desk.hostPos())) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    /**
     * Every prompt and every terminal window on that machine gets the lines, over the last line printed
     * when {@code replaceLast} says so, which is how a bar grows in place.
     */
    private static void promptLines(final ServerLevel level, final BlockPos pos,
                                    final List<WireLine> lines, final boolean replaceLast) {
        for (final ServerPlayer player : level.players()) {
            if (player.containerMenu instanceof CommandPromptMenu prompt && pos.equals(prompt.hostPos())) {
                PacketDistributor.sendToPlayer(player, new CommandOutputPayload(false, "", lines, "", "", replaceLast));
            } else if (player.containerMenu instanceof DesktopMenu desk && pos.equals(desk.hostPos())) {
                PacketDistributor.sendToPlayer(player, DesktopShellOutputPayload.informational(lines, replaceLast));
            }
        }
    }
}
