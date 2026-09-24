/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.advancement.ProgramTravels;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.PackageService;
import dev.jstech.computers.machine.ProgramService;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliMachine;
import dev.jstech.computers.program.cli.PosixPath;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.program.install.LiveTurn;
import dev.jstech.computers.program.job.JobWhen;
import dev.jstech.computers.program.job.MachineJobs;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Backs the Command Prompt's {@link ICliComputer} facade with a real computer and its network. Every command the shell
 * runs ultimately calls one of these methods on the server; effecting verbs route through the same Mainframe operation
 * dispatch the graphical terminal uses, so the CLI is a true alternative interface, not a parallel code path.
 *
 * <p>Laid out in layers, one per part of the machine a command reaches: the machine itself ({@link ServerCliShell}),
 * its files ({@link ServerCliFiles}), its network and the work it asks of it ({@link ServerCliNetwork}), and here its
 * software, its settings, its jobs and the programs it runs.
 */
@TextHolder
public final class ServerCliComputer extends ServerCliNetwork {

    /** The word the program runner is typed as, which is also what its complaints open with. */
    private static final String SIGMA = "sigma";

    private static final TextKey FORGOTTEN =TextKey.of("jsc.cli.variable.forgotten", "%s is forgotten");
    private static final TextKey CANNOT_RUN =
            TextKey.of("jsc.cli.sigma.cannot_run", "this machine cannot run programs");

    public ServerCliComputer(final IComputerTerminalHost host, final ServerLevel level) {
        this(host, level, null);
    }

    /** The same, for a shell somebody is typing at, which is what the words about the player need. */
    public ServerCliComputer(final IComputerTerminalHost host, final ServerLevel level,
                             @Nullable final ServerPlayer typist) {
        super(host, level, typist);
    }

    /** The shell family of the OS installed on {@code host} (DOS when it has no OS or is not a computer). */
    public static ShellFamily shellFamilyOf(final Object host) {
        if (host instanceof IOsHost computer) {
            final OsDef os = computer.installedOs();
            final KernelDef kernel = os == null ? null : OsRegistry.getKernel(os.kernelId());
            if (kernel != null) {
                return kernel.shellFamily();
            }
        }
        return ShellFamily.DOS;
    }

    /** The build every package the Mirror serves is currently at: the mod's own version. */
    public static String modVersion() {
        return PackageService.modVersion();
    }

    @Override
    public List<ProgramInfo> programs() {
        return installs().programs();
    }

    @Override
    public OpResult install(final String programId) {
        return installs().install(programId);
    }

    @Override
    public List<ProgramInfo> programsOnMedia() {
        final List<ProgramInfo> out = new ArrayList<>();
        for (final ProgramSpec spec : installs().onMedia()) {
            out.add(new ProgramInfo(spec.commandName(), spec.id().toString()));
        }
        return out;
    }

    @Override
    public boolean hasProgram(final ResourceLocation id) {
        return installs().has(id);
    }

    @Override
    public LiveInstallState liveInstall() {
        return installs().live();
    }

    @Override
    public LiveTurn liveRun(final String line) {
        // The live medium's reboot is a real one: the shell closes, the POST replays, the new system boots.
        return installs().liveRun(line, this::requestReboot);
    }

    @Override
    public PackageManagerKind packageManager() {
        return packages().manager();
    }

    @Override
    public boolean mirrorReachable() {
        return packages().mirror().reachable();
    }

    @Override
    public OpResult mirrorControl(final String action) {
        return packages().mirror().control(action);
    }

    @Override
    public List<String> drainNotices() {
        return packages().notices();
    }

    @Override
    public List<PackageInfo> packagesAvailable() {
        return packages().available();
    }

    @Override
    public OpResult publishPackage(final String path) {
        return packages().mirror().publish(path);
    }

    @Override
    public OpResult unpublishPackage(final String name) {
        return packages().mirror().unpublish(name);
    }

    @Override
    public Installing packageInstall(final String name, final boolean ask) {
        return packages().install(name, ask);
    }

    @Override
    public OpResult packageUpdate() {
        return packages().update();
    }

    @Override
    public OpResult packageRemove(final String name) {
        return packages().remove(name);
    }

    @Override
    public Installing portsnap(final List<String> commands) {
        return ports().snapshot(commands);
    }

    @Override
    public Installing makePort(final List<String> targets) {
        return ports().make(currentLocation(), PosixPath.render(tree(), currentLocation()), targets);
    }

    @Override
    public List<String> configSummary() {
        return config().summary();
    }

    @Override
    public OpResult setConfig(final String key, final String value) {
        return config().set(key, value);
    }

    @Override
    public List<ShareInfo> shares() {
        return config().shares();
    }

    @Override
    public List<String> favourites() {
        return host.console() == null ? List.of() : host.console().settings().favourites();
    }

    @Override
    public Map<String, String> shellVariables() {
        return host.console() == null ? Map.of() : host.console().settings().variables();
    }

    @Override
    public OpResult setShellVariable(final String name, final String value) {
        if (host.console() == null) {
            return OpResult.fail(ICliMachine.MachineWords.NO_NAMES);
        }
        host.console().settings().setVariable(name, value);
        hostBlock.setChanged();
        final String upper = name.toUpperCase(Locale.ROOT);
        return value == null || value.isEmpty()
                ? OpResult.ok(FORGOTTEN.with(upper))
                : OpResult.ok(Text.literal(upper + "=" + value));
    }

    @Override
    public List<MachineJobs.Job> jobs() {
        return host.console() == null ? List.of() : host.console().jobs().all();
    }

    @Override
    public MachineJobs.Job addJob(final String line, final JobWhen when) {
        if (host.console() == null) {
            return null;
        }
        final MachineJobs.Job job = host.console().jobs().add(line, when);
        hostBlock.setChanged();
        return job;
    }

    @Override
    public boolean stopJob(final int id) {
        if (host.console() == null || !host.console().jobs().remove(id)) {
            return false;
        }
        hostBlock.setChanged();
        return true;
    }

    /**
     * The machine's programs when one of them has the terminal, or null when the prompt is free.
     *
     * <p>A machine has one prompt, so it has at most one program in front of it; whoever is at the
     * keyboard is typing at that program until it returns.
     */
    @Nullable
    public MachinePrograms foreground() {
        final ProgramService running = sigma();
        return running == null ? null : running.foreground();
    }

    @Override
    public OpResult startSigma(final String path, final int heapMb) {
        return this.startSigma(path, heapMb, List.of());
    }

    @Override
    public OpResult startSigma(final String path, final int heapMb, final List<String> arguments) {
        final ProgramService running = sigma();
        final OpResult started = running == null ? OpResult.fail(CliTexts.SAID_BY.with(SIGMA, CANNOT_RUN))
                : running.startAtTerminal(path, heapMb, arguments);
        if (started.ok()) {
            report(JscEvents.SIGMA_RUN, "");
            if (typist != null) {
                ProgramTravels.ran(typist, FsPaths.fileName(path), hostBlock.getBlockPos().asLong());
            }
        }
        return started;
    }

    @Override
    public OpResult stopSigma(final int id) {
        final ProgramService running = sigma();
        return running == null ? OpResult.fail(CliTexts.SAID_BY.with(SIGMA, CANNOT_RUN)) : running.stop(id);
    }

    @Override
    public List<SigmaProcess> sigmaProcesses() {
        final ProgramService running = sigma();
        return running == null ? List.of() : running.processes();
    }
}
