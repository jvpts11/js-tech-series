/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.vm.program.IHost;
import dev.jstech.computers.vm.program.IWorldFunction;
import dev.jstech.computers.vm.system.MemberId;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * What the programs on a machine reach through it, kept with the machine so a call finds it ready. It is the host
 * those programs run on: their clock is the machine's world, and their calls to the world are answered with what it
 * keeps.
 *
 * <p>Each thing is kept with what it was made from and made again only when that has changed, which costs less to
 * check than to make. Nothing has to tell it the machine changed: a machine read out of a save and then placed in a
 * world, or put in another one, is noticed the next time a program asks. The clock asks the machine for its world
 * every time for the same reason.
 */
public final class MachineServices implements IHost {

    /** The length of a Minecraft day in ticks. */
    static final long DAY = 24_000L;

    private static final Logger LOGGER = LogUtils.getLogger();

    private final AbstractComputerBlockEntity machine;

    /**
     * The world the shell and the services over it were made for; null until one is first asked for, and while the
     * machine is in none.
     */
    @Nullable
    private Level shellLevel;

    @Nullable
    private ServerCliComputer shell;

    @Nullable
    private FileService files;

    @Nullable
    private ComputerInfoService computer;

    @Nullable
    private NetworkReadService network;

    @Nullable
    private MainframeStatsService mainframe;

    @Nullable
    private OperationsService operations;

    @Nullable
    private IqlService iql;

    @Nullable
    private PackageService packages;

    @Nullable
    private InstallService installs;

    @Nullable
    private ProgramService programs;

    @Nullable
    private RemoteComputerService remotes;

    @Nullable
    private GatewayBridgeService gateways;

    public MachineServices(final AbstractComputerBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public long tick() {
        final Level level = this.machine.getLevel();
        return level == null ? 0 : level.getGameTime();
    }

    @Override
    public long dayTime() {
        final Level level = this.machine.getLevel();
        return level == null ? 0 : level.getDayTime() % DAY;
    }

    @Override
    public long day() {
        final Level level = this.machine.getLevel();
        return level == null ? 0 : level.getDayTime() / DAY;
    }

    /**
     * The calls the machine answers with one of these services, bound to them; a call on a service the machine cannot
     * reach stops the program when it is made.
     */
    @Override
    public IWorldFunction bind(final MemberId id) {
        final MachineCalls.Binding<?> binding = MachineCalls.find(id);
        return binding == null ? null : binding.on(this);
    }

    /** Whether the machine boots to a desktop; one in no world, or no person could sit at, has none to open on. */
    @Override
    public boolean hasDesktop() {
        final ComputerInfoService info = this.computer();
        return info != null && info.hasDesktop();
    }

    /** Whether a program this machine, or a computer of its network, lists under that number is still going. */
    @Override
    public boolean programRunning(final int program, final String host) {
        final ProgramService running = this.programs();
        return running != null && running.running(program, host);
    }

    @Override
    public void fault(final String process, final int line, final RuntimeException cause) {
        LOGGER.error("Σ# program '{}' on the machine at {} failed inside the runtime at instruction {}",
                process, this.machine.getBlockPos(), line, cause);
    }

    @Override
    public void programEnded(final int program) {
        this.machine.programs().ended(program);
    }

    /**
     * The machine as its shell sees it, which is how its programs reach its drives and its network.
     *
     * <p>The shell holds nothing of the world but the world itself, so one serves every call while the machine stays
     * there. What a command sets on a shell, a reboot asked for or the terminal window it speaks for, is never set on
     * this one: programs run no commands on their own machine, and the prompt makes a shell of its own.
     *
     * @return null when the machine is not one a person could sit at, or is in no world yet
     */
    @Nullable
    public ServerCliComputer shell() {
        this.follow();
        return this.shell;
    }

    /**
     * The machine's drives, reached through the shell's own door.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public FileService files() {
        this.follow();
        return this.files;
    }

    /**
     * What the machine is and what it holds, as what runs on it reads it.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public ComputerInfoService computer() {
        this.follow();
        return this.computer;
    }

    /**
     * The data network the machine is on, as what runs on it reads it.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public NetworkReadService network() {
        this.follow();
        return this.network;
    }

    /**
     * The Mainframe of the machine's network, and what it remembers of the work it has done.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public MainframeStatsService mainframe() {
        this.follow();
        return this.mainframe;
    }

    /**
     * Asking the machine's network to move and make things.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public OperationsService operations() {
        this.follow();
        return this.operations;
    }

    /**
     * The network's own language, run on its Mainframe.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public IqlService iql() {
        this.follow();
        return this.iql;
    }

    /**
     * The packages the machine installs over its network's Mirror, and the Mirror itself.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public PackageService packages() {
        this.follow();
        return this.packages;
    }

    /**
     * What is installed on the machine, and the installing itself.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public InstallService installs() {
        this.follow();
        return this.installs;
    }

    /**
     * The programs on the machine, and what another program started on a computer of its network.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public ProgramService programs() {
        this.follow();
        return this.programs;
    }

    /**
     * The other computers of the machine's network.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public RemoteComputerService remotes() {
        this.follow();
        return this.remotes;
    }

    /**
     * The Gateways linked to the machine, through which its programs reach the ComputerCraft side.
     *
     * @return null when the machine has no shell
     */
    @Nullable
    public GatewayBridgeService gateways() {
        this.follow();
        return this.gateways;
    }

    /** Makes the shell and what goes through it again when the machine is in another world than before. */
    private void follow() {
        final Level level = this.machine.getLevel();
        if (level == this.shellLevel) {
            return;
        }
        this.shellLevel = level;
        if (this.machine instanceof IComputerTerminalHost terminal && level instanceof ServerLevel server) {
            this.shell = new ServerCliComputer(terminal, server);
            this.files = new FileService(this.machine, server,
                    new NetworkPathResolver(server, this.shell::machinesNamed, this.shell::networkShares),
                    this.shell::currentLocation);
            this.computer = new ComputerInfoService(this.machine, this.shell);
            // Operations first: the network hands their rows back when a query asks for them.
            this.operations = new OperationsService(terminal, server, this.shell);
            this.network = new NetworkReadService(terminal, server, this.shell, this.operations);
            this.mainframe = new MainframeStatsService(terminal, server);
            this.iql = new IqlService(terminal, server, this.files, this.operations, this.network);
            // The packages come after the language: the Mirror's list of services carries the engine's row.
            this.packages = new PackageService(terminal, server, this.files, this.iql);
            this.installs = new InstallService(terminal, server, this.packages);
            this.programs = new ProgramService(this.machine, terminal, server, this.shell);
            this.remotes = new RemoteComputerService(this.shell);
            this.gateways = new GatewayBridgeService(this.machine, server);
        } else {
            this.shell = null;
            this.files = null;
            this.computer = null;
            this.network = null;
            this.mainframe = null;
            this.operations = null;
            this.iql = null;
            this.packages = null;
            this.installs = null;
            this.programs = null;
            this.remotes = null;
            this.gateways = null;
        }
    }
}
