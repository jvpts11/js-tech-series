/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.advancement.Acting;
import dev.jstech.computers.advancement.HardwareMilestones;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.audio.MachineVoices;
import dev.jstech.computers.audio.MusicDownloads;
import dev.jstech.computers.audio.MusicPlayer;
import dev.jstech.computers.audio.ProgramCue;
import dev.jstech.computers.audio.ProgramSounds;
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.block.IEraChassisBlock;
import dev.jstech.computers.client.audio.MachineSoundSources;
import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.MachinePorts;
import dev.jstech.computers.hardware.SoundCardSpec;
import dev.jstech.computers.hardware.WorkshopCardSpec;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.IExpansionCardItem;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.PsuItem;
import dev.jstech.computers.item.RamItem;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.MachineServices;
import dev.jstech.computers.machine.NetworkReadService;
import dev.jstech.computers.monitor.VideoMemory;
import dev.jstech.computers.operation.payload.OpenSystemBootPayload;
import dev.jstech.computers.operation.payload.ScreenSessions;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.VramLedger;
import dev.jstech.computers.os.boot.BootIdentity;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootRunner;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.computers.os.boot.BootSplash;
import dev.jstech.computers.os.boot.BootTiming;
import dev.jstech.computers.os.boot.SystemWelcome;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.job.MachineJobs;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.audio.IAudioHost;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.audio.StereoSide;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.text.Text;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.peripheral.IPeripheralOwnerSupport;
import dev.jstech.core.peripheral.PeripheralPorts;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Shared base for every computer that is a BLOCK (Personal Computer, Mainframe, Crafting Computer, and
 * future ones such as Subframe / Supercomputer / AI Server).
 *
 * <p>What a computer is made of is held in parts, each owning one matter of it: the hardware installed and
 * what it adds up to, the power, where it stands on the data network, what hangs off its peripheral cables,
 * the system it boots and the session it runs it in, the console that rides on its disk, the programs it is
 * running, the players watching it, and what it sends them.
 *
 * <p>This class is where those parts are put together. It holds them, answers to the names the rest of the
 * mod has always called, decides what THIS kind of computer accepts in a slot, which is the one thing each
 * kind settles for itself, and saves each part in turn.
 */
public abstract class AbstractComputerBlockEntity extends SyncedBlockEntity
        implements IPeripheralOwnerSupport, IOsHost, IWatchedConsole {

    /** The parts installed and what they add up to; it is built with the layout, so the constructor sets it. */
    private final ComputerHardware hardware;
    /** Whether it is on, whether it comes up by itself, and whether the next look at it shows the self-test. */
    private final ComputerPower power = new ComputerPower(this::setChanged, this::endSession);
    /** Where this computer stands on the data network: its node, its network, and the cable it reads. */
    private final NetworkAttachment attachment = new NetworkAttachment(this);
    /** What is on the far end of its peripheral cables. */
    private final PeripheralEndpoints peripherals = new PeripheralEndpoints();
    /** The system it boots and the session it runs: disks, desktop, windows, installing and formatting. */
    private final OsSession session = new OsSession(this);
    /** The console it keeps, which rides on the system disk rather than on the machine. */
    private final DiskConsole diskConsole = new DiskConsole(this);
    /** The programs it is running and what they reach through it. */
    private final ProgramHost host = new ProgramHost(this);
    /** The program tick, made once rather than on every tick it is run inside its operator's scope. */
    private final Runnable sigmaTick = this::tickSigma;
    /** The players with this computer's console on screen. */
    private final Viewers viewers = new Viewers(this.worldPosition);
    /** What it sends them: the windows its programs have open, and what those programs print. */
    private final ClientReplication replication = new ClientReplication(this);
    /** What moves the tool in front of its terminal along, and sends what that tool prints. */
    private final TerminalFeed terminalFeed = TerminalFeed.of(this);
    /** What it sounds like as a machine: its power button, its start-up, its hard drive. */
    private final ComputerSounds sounds = new ComputerSounds(this);
    /** What its system's sound plays through, and where it comes out. */
    private final ComputerAudioHost audio = new ComputerAudioHost(this);
    /** The voices of its sound hardware and the sounds holding them; before the music, which takes them. */
    private final MachineVoices voices = new MachineVoices(this);
    private final MusicPlayer music = new MusicPlayer(this);
    /** The songs Soundfoundry is fetching over the network. */
    private final MusicDownloads musicDownloads = new MusicDownloads(this);
    /** What the programs running on it play: their tunes, their beeps and their recordings. */
    private final ProgramSounds programSounds = new ProgramSounds(this);

    /** The name a player gave this computer: the machine's own, and no part's; the players who see it are sent it. */
    private final ValueField<String> computerName;

    /*
     * The recipe drafts the Pattern Studio edits, kept out of the session deliberately: a power cut ends a
     * session and closes its windows, while a draft half laid out is still there afterwards, for whoever
     * sits down next.
     */
    private final PatternWorkbench studio =
            new PatternWorkbench();
    /*
     * The video memory the machine has, and what its lit monitors hold of it, sent to the players who see the machine
     * so its desktop can weigh a graphics window against what is left before opening it.
     */
    private final LongField vramTotalKb = fields().longInteger("VramTotalKb", 0L).toClient();
    private final LongField vramMonitorsKb = fields().longInteger("VramMonitorsKb", 0L).toClient();

    /*
     * Each part is saved in turn, in the order declared here: the hardware first, because the console rides on the
     * system disk and has to be pushed onto it before the disk stacks are written.
     */
    protected AbstractComputerBlockEntity(final BlockEntityType<?> type, final BlockPos pos,
                                          final BlockState state, final ComputerHardwareLayout layout) {
        super(type, pos, state);
        this.hardware = new ComputerHardware(this, layout);
        fields().part("Hardware", IFieldPart.of((tag, registries) -> {
            diskConsole.flush();
            hardware.save(tag, registries, hardwareNbtKey());
        }, (tag, registries) -> {
            hardware.load(tag, registries, hardwareNbtKey());
            hardware.markDirty();
            partsChanged();
        })).save();
        fields().part("Power", IFieldPart.of((tag, registries) -> power.save(tag),
                (tag, registries) -> power.load(tag))).save();
        fields().part("Session", IFieldPart.of((tag, registries) -> session.save(tag),
                (tag, registries) -> session.load(tag))).save();
        this.computerName = fields().value("ComputerName", Codec.STRING, "").save().toClient();
        fields().part("Attachment", new AttachmentPart()).save().toClient();
        fields().part("Peripherals", IFieldPart.of((tag, registries) -> peripherals.save(tag),
                (tag, registries) -> peripherals.load(tag))).save();
        fields().part("Studio", IFieldPart.of(this::saveStudio, this::loadStudio)).save();
        fields().part("Programs", IFieldPart.of((tag, registries) -> host.save(tag),
                (tag, registries) -> host.load(tag))).save();
        // The console now rides on the disk; a machine saved before that still carries its own, read once here.
        fields().part("LegacyConsole", IFieldPart.of((tag, registries) -> { },
                (tag, registries) -> diskConsole.loadLegacy(tag))).save();
        fields().part("Sounds", new SoundsPart()).save().toClient();
    }

    /* The parts installed have changed: the build is worked out again and the power reconsidered. */
    void hardwareChanged() {
        power.hardwareChanged(buildValid());
        setChanged();
        HardwareMilestones.report(this);
        partsChanged();
    }

    /** The parts in the machine have changed or been read from the save: a machine that shows them updates its look. */
    protected void partsChanged() {
    }

    // Hardware assembly

    protected abstract Set<FormFactor> acceptedFormFactors();

    /**
     * The hardware era a board must belong to for this computer to accept it, or {@code null} when the
     * computer takes a board of any era (the default). A non-null value gates both slot insertion and the
     * computed build: a board whose era differs is neither installable nor counted. Used by the era-specific
     * Personal Computers to keep, for example, a Legacy board out of a Standard machine even though both are
     * ATX.
     */
    @Nullable
    protected HardwareEra requiredBoardEra() {
        return null;
    }

    /**
     * Whether {@code stack} is a motherboard this computer accepts: it must match an accepted form factor
     * and, when {@link #requiredBoardEra()} is set, also match that era.
     */
    protected boolean isAcceptedBoard(final ItemStack stack) {
        if (!MotherboardItem.fits(stack, acceptedFormFactors())) {
            return false;
        }
        final HardwareEra required = requiredBoardEra();
        return required == null
                || (stack.getItem() instanceof MotherboardItem board && board.spec().era() == required);
    }

    /**
     * Whether {@code stack} is a processor this machine's board can seat: the socket has to match, and
     * so does the hardware generation. A chip that physically cannot go in the socket should not go in
     * the slot either, since letting it in only to refuse to boot tells the player nothing about why.
     */
    protected boolean isValidCpu(final ItemStack stack) {
        if (!(stack.getItem() instanceof CpuItem cpu)) {
            return false;
        }
        final ItemStack boardStack = getHardware().getStackInSlot(layout().motherboardSlot());
        if (!(boardStack.getItem() instanceof MotherboardItem board)) {
            return true; // no board yet: allow pre-staging, as the expansion slots do
        }
        return cpu.spec().socket().equals(board.spec().socket())
                && cpu.spec().era() == board.spec().era();
    }

    /**
     * Whether {@code stack} is memory this machine's board takes: the board lists the RAM generations
     * its slots are keyed for, and the module must belong to the same hardware generation.
     */
    protected boolean isValidRam(final ItemStack stack) {
        if (!(stack.getItem() instanceof RamItem ram)) {
            return false;
        }
        final ItemStack boardStack = getHardware().getStackInSlot(layout().motherboardSlot());
        if (!(boardStack.getItem() instanceof MotherboardItem board)) {
            return true; // no board yet: allow pre-staging
        }
        return board.spec().acceptedRam().contains(ram.spec().generation())
                && ram.spec().era() == board.spec().era();
    }

    protected boolean isValidPcieCard(final ItemStack stack) {
        if (!(stack.getItem() instanceof IExpansionCardItem card)) {
            return false;
        }
        if (card.cardSpec() instanceof WorkshopCardSpec && !takesWorkshopCards()) {
            return false;
        }
        final ItemStack boardStack = getHardware().getStackInSlot(layout().motherboardSlot());
        if (!(boardStack.getItem() instanceof MotherboardItem motherboard)) {
            /*
             * No board yet, so accept the card so it can be pre-staged; the slot stays inoperative
             * until a board arrives.
             */
            return true;
        }
        if (card.cardSpec() instanceof SoundCardSpec sound && sound.era() != motherboard.spec().era()) {
            // A sound card sits only on a board of its own age; no Standard card exists, the boards have it built in.
            return false;
        }
        return card.cardSpec().fits(motherboard.spec().pcieGeneration());
    }

    /** Whether this computer seats the personal-use cards, which only a Personal Computer has a program for. */
    protected boolean takesWorkshopCards() {
        return false;
    }

    public boolean isValidForSlot(final int slot, final ItemStack stack) {
        if (slot == layout().motherboardSlot()) {
            return isAcceptedBoard(stack);
        }
        if (slot == layout().psuSlot()) {
            return stack.getItem() instanceof PsuItem;
        }
        if (layout().isCpu(slot)) {
            return isValidCpu(stack);
        }
        if (layout().isRam(slot)) {
            return isValidRam(stack);
        }
        if (layout().isPcie(slot)) {
            return isValidPcieCard(stack) && !(isSoundCard(stack) && anotherSoundCardSeated(slot));
        }
        if (layout().isDisk(slot)) {
            return stack.getItem() instanceof DiskItem;
        }
        return false;
    }

    public ItemStackHandler getHardware() {
        return hardware.handler();
    }

    @Override
    public List<ItemStack> hardwareStacks() {
        final ItemStackHandler slots = hardware.handler();
        final List<ItemStack> out = new ArrayList<>(slots.getSlots());
        for (int i = 0; i < slots.getSlots(); i++) {
            if (!slots.getStackInSlot(i).isEmpty()) {
                out.add(slots.getStackInSlot(i));
            }
        }
        return out;
    }

    static boolean isSoundCard(final ItemStack stack) {
        return stack.getItem() instanceof IExpansionCardItem card && card.cardSpec() instanceof SoundCardSpec;
    }

    /* A machine plays its sound through one card: a second one would take a slot and power and play nothing. */
    private boolean anotherSoundCardSeated(final int slot) {
        final ItemStackHandler slots = getHardware();
        for (int i = 0; i < slots.getSlots(); i++) {
            if (i != slot && layout().isPcie(i) && isSoundCard(slots.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    /** Where this computer's slots are: which one takes the board, which ones take disks, and how many. */
    ComputerHardwareLayout layout() {
        return hardware.layout();
    }

    protected void markBuildDirty() {
        hardware.markDirty();
    }

    @Override
    @Nullable
    public ComputerBuild currentBuild() {
        return hardware.current();
    }

    /** Every core of every processor seated in this machine, which a machine can really be asked. */
    @Override
    public int cpuCores() {
        final ComputerBuild build = currentBuild();
        if (build == null) {
            return 1;
        }
        int cores = 0;
        for (final CpuSpec cpu : build.cpus()) {
            cores += cpu.cores();
        }
        return Math.max(1, cores);
    }

    public boolean buildValid() {
        return hardware.valid();
    }

    public boolean isRunning() {
        return buildValid() && power.on();
    }

    public boolean isManualOn() {
        return power.on();
    }

    public boolean isAutoStart() {
        return power.autoStart();
    }

    public void togglePower() {
        final boolean wasOn = isRunning();
        if (level instanceof ServerLevel server) {
            sounds.pressed(server);
        }
        power.toggle();
        if (wasOn && !isRunning()) {
            showShutdown(false);
        }
        testedInvalidBuild();
    }

    @Override
    public void setPowered(final boolean on) {
        final boolean wasOn = isRunning();
        final boolean wasSwitchedOn = power.on();
        power.setPowered(on);
        if (wasOn && !on) {
            showShutdown(false);
        }
        if (!wasSwitchedOn) {
            testedInvalidBuild();
        }
    }

    /** The machine used its disk: a turning hard drive is heard seeking for a moment. */
    @Override
    public void diskWorked(final ServerLevel level) {
        sounds.diskWorked(level);
    }

    /** Whether the machine worked its disk a moment ago, on the server. */
    boolean diskBusy() {
        return level != null && sounds.diskBusy(level.getGameTime());
    }

    /**
     * What else the machine keeps sounding while it does it, over its disk, heard from its block: nothing for most
     * machines. Read on the client, from what the server sent it.
     */
    public List<LoopRequest> machineLoops() {
        return List.of();
    }

    /**
     * Starts the machine over: the system says goodbye first, and the self-test begins when it has finished.
     *
     * <p>A restart is not a power cut. The system that is running closes its programs and shows what it shows
     * while it does, exactly as it does on the way to being switched off, and only then does the machine test
     * itself again. A machine with nothing running, or with a system of an age that had no such screen, starts
     * over at once, which is also what those machines did.
     */
    @Override
    public void restart() {
        if (!(level instanceof ServerLevel server) || !isRunning()) {
            setNeedsPost(true);
            return;
        }
        final int ticks = showShutdown(true);
        if (ticks <= 0) {
            setNeedsPost(true);
            return;
        }
        power.beginDown(server.getGameTime(), ticks);
    }

    /**
     * Shuts the machine down from inside its system: the system says goodbye in front of whoever is watching, and
     * the power goes when it has finished. A machine with nothing running, or with a system of an age that had no
     * such screen, goes dark at once.
     */
    @Override
    public void shutDown() {
        if (!(level instanceof ServerLevel server) || !isRunning()) {
            setPowered(false);
            return;
        }
        final int ticks = showShutdown(false);
        if (ticks <= 0) {
            power.setPowered(false);
            return;
        }
        power.beginDown(server.getGameTime(), ticks, true);
    }

    @Override
    public boolean poweringOff() {
        return power.poweringOff();
    }

    @Override
    public void finishShutdown() {
        power.setPowered(false);
    }

    /**
     * Puts the system's own goodbye in front of whoever is watching, and answers how long it runs for.
     *
     * <p>Only the two ways a machine really stops: switched off, or started over. A machine that went dark
     * because its parts no longer make a computer lost its power rather than being shut down, and nothing says
     * goodbye when the plug comes out. The systems of the earliest ages have nothing to show either, so their
     * monitors simply go dark where they stand, which is what a length of zero means here.
     *
     * @param restarting the machine is coming straight back up, which every system words differently
     */
    private int showShutdown(final boolean restarting) {
        if (!(level instanceof ServerLevel server)) {
            return 0;
        }
        final BootSequence sequence = BootLines.shutdownFor(this, restarting);
        if (sequence.isEmpty()) {
            return 0;
        }
        final int ticks = BootTiming.shutdownTicks(
                BootRunner.bootLength(this));
        // A system that says goodbye on its screen says it out loud too.
        systemSound(server, SystemSound.SHUTDOWN);
        ScreenSessions.eachWatcher(server, worldPosition, (player, monitor) -> {
            PacketDistributor.sendToPlayer(player,
                    new OpenSystemBootPayload(
                            worldPosition, monitor, ticks, ticks, sequence, !restarting,
                            installedOs() == null ? BootSplash.PLAIN
                                    : BootSplash.of(installedOs().platform(), installedOs().familyRank()),
                            /* A machine on its way down shows no desktop coming up, so it names none. */
                            new BootIdentity("", installedOs() == null ? "" : installedOs().displayName(),
                                    Installers.hostName(this), "")));
            // The words and the screen that shows them, as everywhere else: one without the other shows nothing.
            MonitorBlock.openSession(player, server, monitor, worldPosition, this,
                    MonitorSessionMenu.Phase.SYSTEM_BOOT);
        });
        return ticks;
    }

    public void toggleAutoStart() {
        power.toggleAutoStart(buildValid());
    }

    /* Switched on with parts that do not make a computer: the board tests itself all the same and beeps its failure. */
    private void testedInvalidBuild() {
        if (power.on() && !buildValid() && level instanceof ServerLevel server) {
            sounds.postFailed(server);
        }
    }

    private void endSession() {
        session.drop();
        /*
         * The terminal's lines belong to the run of the machine that printed them, and this is where a run
         * ends: a restart, a power cut, a cold start. Counting the run up is what lets a terminal opened
         * afterwards start clean instead of coming up showing somebody else's installation.
         */
        if (console() != null) {
            console().newSession();
        }
    }

    /** Whether the machine owes a power-on self-test or is in the middle of one. */
    @Override
    public boolean needsPost() {
        return power.needsPost();
    }

    /** Whether the machine is standing at the end of a self-test that found nothing to boot. */
    @Override
    public boolean haltedAtPost() {
        return power.halted();
    }

    @Override
    public void resumeFromHalt() {
        power.resume();
    }

    /** The ticks the self-test still has to run, for a monitor opened while it is under way. */
    @Override
    public int postRemaining() {
        return level == null ? 0 : power.postRemaining(level.getGameTime());
    }

    @Override
    public boolean keepsInstalls() {
        return true;
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    /** The system being copied onto a disk right now, or nothing. */
    @Override
    @Nullable
    public OsInstallJob installing() {
        return session.installing();
    }

    /** Starts, replaces or ends the copy this machine is doing. */
    public void setInstalling(@Nullable final OsInstallJob job) {
        session.setInstalling(job);
    }

    @Override
    public SystemWelcome systemWelcome() {
        return session.welcome();
    }

    @Override
    public void setSystemWelcome(final SystemWelcome welcome) {
        session.setWelcome(welcome);
    }

    /** The installer this machine is in: the page it is on and what has been answered so far. */
    @Nullable
    public InstallerFlow installer() {
        return session.installer();
    }

    /** Puts the machine in an installer, or takes it out of one. */
    public void setInstaller(@Nullable final InstallerFlow flow) {
        session.setInstaller(flow);
    }

    /** Whether that system could go on that disk, asked before a copy starts rather than after it ends. */
    public boolean canTakeOs(final ResourceLocation osId, final int preferredSlot) {
        return session.canTakeOs(osId, preferredSlot);
    }

    /** Boots that system on that disk for this boot only, leaving what the disk boots by default alone. */
    public void setBootOnce(final int slot, @Nullable final ResourceLocation osId) {
        session.setBootOnce(slot, osId);
    }

    /**
     * Carries this machine up: the self-test, the wait at a boot manager, and the system coming up.
     *
     * <p>The machine keeps these times rather than the screen doing it. Closing the monitor halfway through no
     * longer stops a machine coming up, opening it again shows how far it has got, and a machine nobody is
     * looking at boots all the same, which is what a machine does.
     */
    protected void tickBootPhases(final ServerLevel level) {
        BootRunner.tick(this, power.phases(), level, worldPosition);
    }

    /** Whether the machine is stopped at its boot menu. */
    @Override
    public boolean atBootMenu() {
        return power.atMenu();
    }

    /** The ticks left before the menu boots its first entry by itself, or zero once a key has stopped it. */
    @Override
    public int menuRemaining() {
        return level == null ? 0 : power.menuRemaining(level.getGameTime());
    }

    /** A key was pressed at the menu: the machine waits there for a choice. */
    @Override
    public void holdBootMenu() {
        power.holdMenu();
    }

    /** Leaves the menu and brings the chosen system up. */
    @Override
    public void leaveBootMenu() {
        power.endMenu();
        power.beginBoot();
    }

    @Override
    public void restartFromBootMenu() {
        power.endMenu();
        setNeedsPost(true);
    }

    /** Whether the system is coming up on this machine right now. */
    @Override
    public boolean booting() {
        return power.booting();
    }

    /** The ticks the system still needs, for a monitor opened while it comes up. */
    @Override
    public int bootRemaining() {
        return level == null ? 0 : power.bootRemaining(level.getGameTime());
    }

    /** How long the coming-up under way takes in all, for the bar on the screen watching it. */
    @Override
    public int bootTotal() {
        return power.bootTotal();
    }

    /** Whether the machine is closing its system down on its way to starting over. */
    @Override
    public boolean goingDown() {
        return power.goingDown();
    }

    /** How long that closing-down takes in all, and how much of it a monitor opened now would join. */
    @Override
    public int downTotal() {
        return power.downTotal();
    }

    @Override
    public int downRemaining() {
        return level == null ? 0 : power.downRemaining(level.getGameTime());
    }

    /** What this machine's system shows while it comes up. */
    @Override
    public BootSequence bootSequence() {
        return BootLines.forMachine(this, level instanceof ServerLevel server ? server : null);
    }

    /** Whether a drive this machine reaches holds something it could boot instead of one of its own disks. */
    @Override
    public boolean hasBootableMedium() {
        if (level == null) {
            return false;
        }
        for (final long endpoint : enabledEndpoints()) {
            if (Loaded.blockEntity(level, BlockPos.of(endpoint))
                    instanceof MediaReaderBlockEntity reader
                    && reader.insertedKind() == MediaKind.OS_INSTALL
                    && reader.insertedPayload() != null) {
                return true;
            }
        }
        return false;
    }

    public void setNeedsPost(final boolean value) {
        power.setNeedsPost(value);
    }

    @Override
    public int pendingInstallSlot() {
        return session.pendingInstallSlot();
    }

    @Override
    public void setPendingInstallSlot(final int slot) {
        session.setPendingInstallSlot(slot);
    }

    @Override
    @Nullable
    public ResourceLocation bootedDesktopId() {
        return session.bootedDesktopId();
    }

    @Override
    public void setBootedDesktopId(@Nullable final ResourceLocation id) {
        session.setBootedDesktopId(id);
    }

    @Override
    public PatternWorkbench studio() {
        return studio;
    }

    @Override
    public List<OpenWindow> openWindows() {
        return session.openWindows();
    }

    @Override
    public void setOpenWindows(final List<OpenWindow> windows) {
        session.setOpenWindows(windows);
    }

    @Override
    public int desktopWorkspace() {
        return session.desktopWorkspace();
    }

    @Override
    public void setDesktopWorkspace(final int workspace) {
        session.setDesktopWorkspace(workspace);
    }

    public long capacity() {
        return hardware.capacity();
    }

    public long ramBuffer() {
        return hardware.ramBuffer();
    }

    public int boardCpuSlots() {
        return hardware.boardCpuSlots();
    }

    public int boardRamSlots() {
        return hardware.boardRamSlots();
    }

    public int boardPcieSlots() {
        return hardware.boardPcieSlots();
    }

    public int boardDiskSlots() {
        return hardware.boardDiskSlots();
    }

    /** The hardware era of the installed motherboard, or {@code null} when no board is present. */
    @Nullable
    public HardwareEra installedEra() {
        return hardware.installedEra();
    }

    /**
     * The hardware era the GUI should wear. A per-era chassis (a Vintage or Legacy computer block) fixes its era
     * regardless of what is installed, so its assembly GUI shows the right era skin even when empty; every other
     * computer takes its look from the installed board, falling back to {@code null} (the STANDARD skin) when bare.
     */
    @Nullable
    public HardwareEra displayEra() {
        return getBlockState().getBlock() instanceof IEraChassisBlock chassis
                ? chassis.chassisEra()
                : installedEra();
    }

    public int installedCpus() {
        return hardware.installedCpus();
    }

    public int installedRam() {
        return hardware.installedRam();
    }

    public int installedGpus() {
        return hardware.installedGpus();
    }

    /** The best (max) CPU clock in MHz across installed CPUs, or 0 when there is no valid build. */
    public int maxCpuMhz() {
        return hardware.maxCpuMhz();
    }

    /** The usable VRAM in MB across installed GPUs, or 0 when there is no valid build. */
    public int totalVramMb() {
        return hardware.totalVramMb();
    }

    /**
     * Free space on the system disk in real MB, for the program-install disk-footprint gate: the free
     * mB-equivalent weight ({@link #systemDiskFreeWeight()}) at what an item costs on that disk's era.
     */
    public long systemDiskFreeMb() {
        return session.systemDiskFreeMb();
    }

    public int installedDisks() {
        return hardware.installedDisks();
    }

    /*
     * Host-facing slot-count names (alias the board-derived counts), so subclasses that implement
     * IComputerTerminalHost inherit these without boilerplate.
     */
    public int cpuSlots() {
        return boardCpuSlots();
    }

    public int ramSlots() {
        return boardRamSlots();
    }

    public int gpuSlots() {
        return boardPcieSlots();
    }

    public int diskSlots() {
        return boardDiskSlots();
    }

    // Identity & name

    public NodeUuid nodeUuid() {
        return attachment.node();
    }

    @Nullable
    public NetworkUuid networkUuid() {
        return attachment.network();
    }

    @Override
    public boolean networkAttached() {
        return attachment.attached();
    }

    /**
     * What the network this machine is on is holding, and how much room it has for more.
     *
     * <p>Asked of the machine that orchestrates that network, because the index of what is where is kept
     * there and nowhere else. Every machine used to answer nothing at all, the orchestrator excepted, so a
     * terminal on a personal computer said the network held nothing while showing what it held.
     *
     * <p>No {@code @Override} because the terminal host interface is implemented by the machines below this
     * class rather than by this one; a method inherited from here answers it for each of them.
     */
    public long networkStorageUsed() {
        final MainframeBlockEntity orchestrator = networkOrchestrator();
        return orchestrator == null ? 0L : orchestrator.networkStorageUsed();
    }

    public long networkStorageTotal() {
        if (networkUuid() == null || !(level instanceof ServerLevel serverLevel)) {
            return 0L;
        }
        // In items as the racks registered them: what a megabyte holds differs by era, an item does not.
        return NetworkSystem.get(serverLevel).totalStorageItemsOf(networkUuid());
    }

    /** The machine that orchestrates this one's network, or null when it is on none or none is running. */
    @Nullable
    private MainframeBlockEntity networkOrchestrator() {
        if (networkUuid() == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return NetworkSystem.get(serverLevel).mainframePositionOf(networkUuid())
                .map(pos -> Loaded.blockEntity(serverLevel, BlockPos.of(pos))
                        instanceof MainframeBlockEntity mainframe ? mainframe : null)
                .orElse(null);
    }

    /** Where this computer stands on the data network, for a Mainframe, which owns its network itself. */
    protected NetworkAttachment attachment() {
        return attachment;
    }

    /** The data cables this computer is wired to the network by: for most, the one cable at its side. */
    public Set<Long> networkCables(final ServerLevel level) {
        final long cable = attachment.cable(level);
        return cable == NetworkAttachment.NO_CABLE ? Set.of() : Set.of(cable);
    }

    /**
     * The slowest cable between this computer and that one, which is as fast as data can go between them, or
     * empty when no cable joins them.
     */
    public Optional<DataLink> slowestCableTo(final ServerLevel level, final AbstractComputerBlockEntity other) {
        return NetworkSystem.get(level).connectivity()
                .slowestBetween(networkCables(level), other.networkCables(level));
    }

    public String customName() {
        return computerName.get();
    }

    public void setCustomName(final String name) {
        final String trimmed = name.strip();
        computerName.set(trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed);
    }

    /*
     * OS installation (shared across all computer block entities)
     * The OS lives on the system disk's SYSTEM_OS component so it travels with the disk.
     */

    /**
     * Returns the first installed disk stack whose {@code SYSTEM_OS} component points to a
     * registered OS, or {@link ItemStack#EMPTY} when no bootable disk is present.
     *
     * <p>The system disk is defined as the first disk slot (lowest index) holding a
     * {@link DiskItem} with a {@code SYSTEM_OS} component that maps to a known {@link OsDef}.
     */
    public ItemStack systemDisk() {
        return session.systemDisk();
    }

    /** The disk slot index the firmware boots first, or {@code -1} for "the first disk with a system". */
    public int bootDiskSlot() {
        return session.bootDiskSlot();
    }

    /** Sets the preferred boot disk slot ({@code -1} = automatic) and marks the computer dirty. */
    public void setBootDiskSlot(final int slot) {
        session.setBootDiskSlot(slot);
    }

    /**
     * Returns {@code true} when a bootable system disk is present in the hardware inventory.
     */
    /**
     * The desktop environment this computer boots into: the OS's bundled one (the Frames editions), else the
     * first desktop-environment package installed on it (a Linux distribution after {@code apt install gnome}),
     * else null (a TTY-only or network OS).
     */
    public ResourceLocation installedDesktopId() {
        return session.installedDesktopId();
    }

    /** The operating space this computer draws with, or null when it comes up at its prompt and nothing else. */
    @Override
    public ResourceLocation installedSpaceId() {
        return session.installedSpaceId();
    }

    public boolean hasOs() {
        return session.hasOs();
    }

    /**
     * Returns the stacks in this computer's disk slots, in slot order. Entries may be empty or hold
     * non-disk items; callers filter as needed (used by the "This PC" disk listing).
     */
    public List<ItemStack> diskStacks() {
        return session.diskStacks();
    }

    /** The disk stack in the given 0-based disk slot (for renaming a specific installed disk), or EMPTY. */
    public ItemStack diskInSlot(final int slot) {
        return session.diskInSlot(slot);
    }

    /**
     * Returns the registry key of the OS on the system disk, or {@code null} when no bootable
     * disk is installed.
     */
    @Nullable
    public ResourceLocation installedOsId() {
        return session.installedOsId();
    }

    /**
     * Returns the {@link OsDef} for the OS on the system disk, or {@code null} when no bootable
     * disk is installed or the registry entry is absent.
     */
    @Nullable
    public OsDef installedOs() {
        return session.installedOs();
    }

    /**
     * Returns the disk footprint of the installed OS in item-equivalents, or zero when no OS is
     * present. This is subtracted from the usable storage capacity so the OS competes for disk
     * space alongside stored data.
     */
    public long reservedByOs() {
        return session.reservedByOs();
    }

    /**
     * Free weight in mB-equivalents available on the system disk for user files: the disk capacity
     * minus the stored items, the existing files, and the installed OS footprint. Returns 0 when no
     * system disk is present.
     */
    public long systemDiskFreeWeight() {
        return session.systemDiskFreeWeight();
    }

    /**
     * Installs the OS identified by {@code osId} onto a disk in this computer's hardware inventory.
     *
     * <p>The method scans disk slots in order and picks the first {@link DiskItem} slot (preferring
     * one that already carries a {@code SYSTEM_OS} over a plain data disk, so re-installing the
     * same OS is idempotent). The OS footprint in mB-equivalents must fit within the chosen disk's
     * free weight ({@code capacity − stored items' weight − FILESYSTEM.usedWeight}).
     *
     * <p>Returns {@code false} without making any change when:
     * <ul>
     *   <li>the id is unknown to the registry,</li>
     *   <li>no disk is installed in the hardware inventory, or</li>
     *   <li>the OS footprint does not fit the chosen disk's free space.</li>
     * </ul>
     *
     * Returns {@code true} on success; the component is written to the disk stack in the slot,
     * the change is persisted, and clients are notified.
     */
    public boolean installOs(final ResourceLocation osId) {
        return installOs(osId, -1);
    }

    /** Moves whatever is running in front of this computer's terminal along, and tells whoever is watching. */
    public void tickTerminal(final ServerLevel level) {
        terminalFeed.tick(level);
    }

    /**
     * Every player with this computer's console on screen: its terminal menus, or its open desktop.
     *
     * <p>The list is the machine's own, kept on the server thread: read it, do not keep it.
     */
    public List<ServerPlayer> consoleViewers(
            final ServerLevel level) {
        return viewers.at(level);
    }

    /**
     * The windows this machine owes that player, counted as sent from here on: everything its programs have
     * open that the player has not been given already.
     *
     * <p>Somebody who has just opened the desktop is owed all of them, which is how a second person at the
     * same machine is shown what the first is already looking at.
     */
    public List<UiWindowPayload> takeWindowsOwed(
            final ServerPlayer viewer) {
        return replication.takeOwed(viewer);
    }

    @Override
    public void consoleOpenedBy(final ServerPlayer viewer) {
        this.viewers.opened(viewer);
    }

    @Override
    public void consoleClosedBy(final ServerPlayer viewer) {
        this.viewers.closed(viewer);
    }

    /**
     * Whether this computer still has something to run: the installed OS on a disk, or a live-install
     * session whose medium is still in a linked drive. A live session whose medium was pulled out is
     * dropped here (the machine "crashed"), so the next boot lands on the firmware instead of a ghost
     * installer shell.
     */
    public boolean validateOsSession() {
        return session.validateOsSession();
    }

    @Override
    public boolean settleLiveInstall() {
        return session.settleLiveInstall();
    }

    /**
     * Formats disk slot {@code slot}: erases the installed system, every file, the item storage and the
     * privacy split on it, leaving a blank disk. The boot-order pointer is cleared when it pointed here.
     * Returns whether a disk was actually formatted.
     */
    public boolean formatDisk(final int slot) {
        return session.formatDisk(slot);
    }

    /**
     * A disk carrying a system was just formatted: everything the software layer remembered lived on it,
     * so the console's history, session location and installed-program set go with it. Subclasses hosting
     * software services (the Mainframe) extend this to switch those off too.
     */
    protected void onSystemErased() {
        final ComputerConsoleState console = console();
        if (console != null) {
            console.wipeSoftware();
        }
    }

    /**
     * The disk slot the firmware installs onto by default: the first disk without a system (so a second OS
     * lands beside the first for dual boot), else the first disk; {@code -1} when no disk is installed.
     */
    public int defaultInstallSlot() {
        return session.defaultInstallSlot();
    }

    /**
     * Installs {@code osId} onto disk slot {@code preferredSlot}, or ({@code -1}) onto the default target:
     * a slot already carrying this OS (an idempotent re-install), else the first disk without a system, else
     * the first disk. Returns false when the OS is unknown, no disk is present, or the footprint does not fit.
     */
    public boolean installOs(final ResourceLocation osId, final int preferredSlot) {
        return session.installOs(osId, preferredSlot);
    }

    /**
     * Removes the OS from the system disk. A no-op when no bootable disk is installed.
     */
    public void uninstallOs() {
        session.uninstallOs();
    }

    /*
     * Peripheral ownership: the links and the standard owner methods come from IPeripheralOwnerSupport; only how
     * many ports of each kind the machine has depends on its hardware.
     */

    @Override
    public PeripheralPorts peripheralPorts() {
        return peripherals.ports();
    }

    @Override
    public void markPeripheralChange() {
        setChanged();
    }

    /** Its board's device ports, its graphics cards' video outputs and its audio output; none without a build. */
    @Override
    public int ports(final PortKind kind) {
        final ComputerBuild build = currentBuild();
        return build == null ? 0 : MachinePorts.of(build, kind);
    }

    /*
     * Network participation (default: a passive client that reads its network from a cable).
     * The Mainframe overrides this entirely (it owns and orchestrates a network).
     */

    protected abstract void registerNode(NetworkSystem system, NetworkUuid network);

    protected abstract void unregisterNode(NetworkSystem system, NetworkUuid network);

    protected void tickNode(final ServerLevel level) {
        tickTerminal(level);
        tickBootPhases(level);
        tickSounds(level);
        OsInstallRunner.tick(this, level, worldPosition);
        // What the machine's programs and jobs ask the network for is the doing of whoever works it.
        Acting.asOperatorOf(this, sigmaTick);
        SetupRunner.tick(this, level, worldPosition);
        attachment.tick(level);
        final VramLedger video = VideoMemory.of(level, this).ledger();
        vramTotalKb.set(video.totalKb());
        vramMonitorsKb.set(video.usedKb(VramLedger.Kind.MONITOR));
    }

    /** The video memory this machine has, in kilobytes, as its players were last told. */
    public long vramTotalKb() {
        return vramTotalKb.get();
    }

    /** What its lit monitors hold of its video memory, in kilobytes, as its players were last told. */
    public long vramMonitorsKb() {
        return vramMonitorsKb.get();
    }

    public void onBroken(final ServerLevel level) {
        attachment.leave(level);
    }

    /**
     * Hears the machine come on and go off, keeps its hard drive turning in between, and keeps Soundfoundry going:
     * the song it plays and the songs it is fetching.
     */
    protected void tickSounds(final ServerLevel level) {
        sounds.tick(level);
        music.tick(level);
        musicDownloads.tick(level);
        programSounds.tick(level);
        // A machine that is off plays nothing, so whatever held its voices has stopped.
        if (!isRunning()) {
            voices.silence(level);
        }
    }

    /** Soundfoundry playing on this machine. */
    public MusicPlayer musicPlayer() {
        return music;
    }

    /** The voices of its sound hardware, and the sounds holding them. */
    public MachineVoices voices() {
        return voices;
    }

    /** What the programs running on it play. */
    public ProgramSounds programSounds() {
        return programSounds;
    }

    /** The songs Soundfoundry is fetching over this machine's network. */
    public MusicDownloads musicDownloads() {
        return musicDownloads;
    }

    /** What its system's sound plays through and where it comes out. */
    public IAudioHost audioHost() {
        return audio;
    }

    @Override
    public void systemSound(final ServerLevel level, final SystemSound sound) {
        audio.play(level, sound);
    }

    @Override
    public void programSound(final ServerLevel level, final ProgramCue sound) {
        audio.play(level, sound);
    }

    @Override
    public void bell(final ServerLevel level) {
        audio.bell(level);
    }

    /** How many speakers are linked to it. */
    public int speakerCount() {
        return audio.speakerCount();
    }

    /** Which side of a stereo recording the speaker at {@code speaker} plays for it. */
    public StereoSide speakerSide(final BlockPos speaker) {
        return audio.sideOf(speaker);
    }

    /** The speakers linked to it, in a fixed order. */
    public List<SpeakerBlockEntity> linkedSpeakers() {
        return audio.linkedSpeakers();
    }

    /** Whether a subwoofer stands against one of its Transition satellites, which then play the bass too. */
    public boolean hasSubwoofer() {
        return audio.hasSubwoofer();
    }

    /** Whether its speakers play, by the output its system chose and what is linked. */
    public boolean speakersPlay() {
        return audio.speakersPlay();
    }

    /** Whether its monitors play, by the output its system chose and what is linked. */
    public boolean monitorsPlay() {
        return audio.monitorsPlay();
    }

    /** What plays its system's sound, named for a screen: its sound card, the board's own sound, or the case's. */
    public Text soundHardwareLabel() {
        return audio.hardwareLabel();
    }

    /** Whether its system's sound plays recordings, rather than only the beeps of the speaker in its case. */
    public boolean playsRecordings() {
        return audio.audioDevice().samples();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            MachineSoundSources.track(sounds);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        /*
         * The block's onRemove only fires on destruction; a plain chunk unload removes the block entity
         * without it, so without this the node would stay registered in the still-loaded per-level
         * network as a phantom. onBroken is idempotent, so the destruction path running both is safe.
         */
        if (level instanceof ServerLevel serverLevel) {
            onBroken(serverLevel);
            music.removed(serverLevel);
            voices.silence(serverLevel);
        } else if (level != null && level.isClientSide()) {
            MachineSoundSources.untrack(sounds);
        }
    }

    /** The Σ# programs this machine is running. */
    public MachinePrograms programs() {
        return host.programs();
    }

    /** What the Σ# programs on this machine reach through it. */
    public MachineServices services() {
        return host.services();
    }

    /** The data network as what runs on this machine reads it. */
    @Override
    public NetworkReadService networkService() {
        return host.services().network();
    }

    /**
     * How much of that the network holds, for the programs watching it.
     *
     * <p>Off a network, everything reads as none: a watch on a machine with no cable simply never goes
     * off, which is the truthful answer and not an error.
     */
    public long networkStock(final String item) {
        return host.networkStock(item);
    }

    /** The prompt this machine's shell would show, for giving it back when a program lets go. */
    String shellPrompt() {
        return host.shellPrompt();
    }

    /** The same prompt a run at a time, in the colours the machine's shell gives it. */
    CliLine shellPromptLine() {
        return host.shellPromptLine();
    }

    /**
     * How many instructions this machine's processors are worth in one tick.
     *
     * <p>A machine with no build is worth nothing, which is the honest answer for one whose parts have
     * been taken out from under a running program.
     */
    public int sigmaCredits() {
        return host.credits();
    }

    /**
     * Runs whatever scripts the machine has, or stops them all if it is no longer up.
     *
     * <p>A computer that has been switched off is not running programs, so they are told so and given
     * their chance to say goodbye rather than being left frozen for whenever it comes back on.
     */
    protected void tickSigma() {
        /*
         * The machine's own work comes first and on its own: a computer with no program running still has the
         * jobs it was left with, and the program host has nothing to do on such a machine and says so.
         */
        if (level instanceof ServerLevel world && isRunning()) {
            tickJobs(world);
        }
        if (!host.tick() || !(level instanceof ServerLevel server)) {
            return;
        }
        replication.pushOutput(server);
        replication.pushWindows(server);
        host.hearGateways();
    }

    /**
     * Runs the work this machine was left with: a line put in the background, and a line whose hour has come.
     *
     * <p>Run here, on the machine's own tick, and not by whoever typed it: that is what a job is. Nobody has
     * to be at the keyboard, or in the world at all, and what a job prints goes nowhere unless a terminal is
     * looking, exactly as at a real one.
     */
    private void tickJobs(final ServerLevel server) {
        final ComputerConsoleState console = console();
        /*
         * Only a machine that is a terminal in its own right runs jobs, which is every computer a player
         * types at. A rack's bays keep theirs on the unit that owns the terminal, not on the rack.
         */
        if (console == null || console.jobs().isEmpty() || !(this instanceof IComputerTerminalHost terminal)) {
            return;
        }
        final List<MachineJobs.Job> due = console.jobs().due(server.getDayTime(), console.settings().cronEnabled());
        if (due.isEmpty()) {
            return;
        }
        final ServerCliComputer computer = new ServerCliComputer(terminal, server);
        // A schedule kept by UNIX on the oldest machines there are: whoever set it up earns it when it runs.
        final boolean clockwork = displayEra() == HardwareEra.VINTAGE && installedOs() != null
                && installedOs().platform() == Platform.UNIX;
        for (final MachineJobs.Job job : due) {
            CliCommands.shellFor(computer, TermBuffer.MONITOR_COLUMNS).run(job.line(), computer);
            if (clockwork && !job.when().once()) {
                JscEvents.awardOperator(this, JscEvents.CLOCKWORK);
            }
        }
        setChanged();
    }

    /**
     * Whether the program on another machine that started one of this machine's programs is still there
     * to read what it left.
     *
     * <p>It is only asked about a program that has finished and was started from elsewhere, so an ordinary
     * tick never looks. A machine whose chunk is not loaded is not known to be gone: its programs come back
     * with it, so what was started for them is kept until it can be asked.
     */

    /**
     * Tells the program on another machine that started one of this machine's programs that the program has ended, so
     * a wait on it runs again at once. A machine that is not loaded is not told: its programs look again when they
     * come back.
     */

    /**
     * Hands the programs on this machine whatever the ComputerCraft computers said through its Gateways.
     *
     * <p>A message waits on the Gateway until this tick and no longer: whoever is listening hears it now,
     * and a machine where no program listens simply lets it go.
     */
    /** Says a Gateway linked to this machine has something waiting for its programs. */
    public void gatewayMailWaits() {
        host.gatewayMailWaits();
    }

    /*
     * Provided here (no @Override: this base does not itself declare IComputerTerminalHost) so the
     * computer subclasses that ARE hosts inherit it and satisfy the interface's console() method.
     */
    public ComputerConsoleState console() {
        return diskConsole.state();
    }

    @Override
    public void setChanged() {
        /*
         * Every mutation of installed software ends in setChanged, so this is the one place that
         * guarantees the disk is current before the player can pull it out. Without it, installing a
         * program and immediately removing the drive would lose the install: the in-memory state is
         * discarded when the slot changes, and the world may not have saved in between.
         */
        diskConsole.flush();
        super.setChanged();
    }

    // Persistence: each part is declared in the constructor, and the players are sent what each part says

    /**
     * The NBT key the hardware {@link ItemStackHandler} is stored under. Overridable so a subclass with
     * pre-existing saved worlds (the Mainframe, which historically persisted under {@code "Inventory"})
     * can keep its key and load every existing component without data migration.
     */
    protected String hardwareNbtKey() {
        return "Hardware";
    }

    private void saveStudio(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag studioTag = new CompoundTag();
        studio.save(studioTag, registries);
        tag.put("Studio", studioTag);
    }

    private void loadStudio(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (tag.contains("Studio")) {
            studio.load(tag.getCompound("Studio"), registries);
        }
    }

    /** Where the machine stands on the network: saved whole, and whether it is on one sent to the players. */
    private final class AttachmentPart implements IFieldPart {

        @Override
        public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
            attachment.save(tag);
        }

        @Override
        public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
            attachment.load(tag);
        }

        @Override
        public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
            attachment.saveForClient(tag);
        }

        @Override
        public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
            attachment.loadFromClient(tag);
        }
    }

    /**
     * What the machine sounds like: nothing of it is saved, and a machine read back from a save listens afresh for
     * whether it runs; the players are sent whether its disk is turning and seeking, which they hear.
     */
    private final class SoundsPart implements IFieldPart {

        @Override
        public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        }

        @Override
        public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
            sounds.loaded();
        }

        @Override
        public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
            sounds.saveForClient(tag);
        }

        @Override
        public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
            sounds.loadFromClient(tag);
        }
    }
}
