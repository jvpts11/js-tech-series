/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * An installation being set up and carried out: which page it is on, what has been answered, and how far the
 * copy may run before the next question.
 *
 * <p>The installer belongs to the machine, not to the monitor in front of it. Walking away leaves the copy
 * going and the answers where they were; coming back shows the page it had reached. Quitting is only possible
 * while nothing has been written, which is the promise every real installer makes and the reason the pages that
 * ask come before the pages that work.
 *
 * <p>The copy never runs past a question that has not been answered. The steps belong to the pages, so an
 * installer that asks something halfway through carries on to the end of that page's step and waits there: the
 * one that does this shows the step it is on while the dialog is open, which is what it really is doing.
 *
 * <p>Pure logic. It counts steps and answers questions about them; who ticks it, who writes the system and who
 * draws it are the machine's business.
 */
@TextHolder
public final class InstallerFlow {

    /** No disk chosen, and the answer a page gives when the machine has none at all. */
    public static final int NO_DISK = -1;

    /** No desktop: the system comes up at its terminal, which is an answer and not a missing one. */
    public static final int NO_DESKTOP = -1;

    /**
     * How long a computer's name may be.
     *
     * <p>It used to be the fifteen letters the machines of the first age allowed, which is period-correct and
     * no fun at all: a player naming a machine is naming it for themselves, and the name they want rarely
     * fits in fifteen. It is a length that has an end to it rather than a length anybody runs into.
     */
    public static final int MOST_NAME_LETTERS = 256;

    /** More disks than any machine wires up, so a list that crosses the wire has an end to it. */
    public static final int MOST_DISKS = 16;

    /** The same for the desktops a Mirror can offer. */
    public static final int MOST_DESKTOPS = 16;

    /*
     * OpenBSD's answers that no other installer asks, one bit each (computers/docs/design/operating-systems.md): the
     * three sets a player may leave out, whether X is expected, and whether xenodm starts the desktop at boot. None
     * set is what the real installer offers by default: every set, X expected, xenodm off.
     */
    /** The {@code bsd.rd} set left out. */
    public static final int NO_BSD_RD = 1;
    /** The {@code comp} set left out, and the building of ports with it. */
    public static final int NO_COMP = 2;
    /** The {@code man} set left out, and the manual pages with it. */
    public static final int NO_MAN = 4;
    /** The X Window System not expected, so xenodm is not asked about. */
    public static final int NO_X = 8;
    /** xenodm starting the desktop at boot. */
    public static final int XENODM = 16;

    /** The step a desktop from the Mirror adds to the copy, with the Mirror named when one answers. */
    private static final TextKey INSTALLING_DESKTOP =
            TextKey.of("jsc.install.installer_flow.installing_desktop", "Installing %s");
    private static final TextKey INSTALLING_FROM_MIRROR =
            TextKey.of("jsc.install.installer_flow.installing_from_mirror", "Installing %s from the Mirror on %s");

    private final InstallerStyle style;
    private final String systemId;
    private final String systemName;
    private final int footprintMb;
    /*
     * What the machine copies at, leaving the disk out: the medium it reads from and the processor that
     * unpacks it. The disk is the one part of the rate the player is still choosing on the page in front of
     * them, so it is multiplied in when they choose, and the copy really does get shorter when they pick a
     * faster one. Zero for an installation restored from a machine that only wrote down how long it had left.
     */
    private final double baseRate;
    private int copyTicks;
    /* Read again after the disk step changes a table, so the page lists what the disk really holds. */
    private List<Disk> disks;
    private final List<Desktop> desktops;
    private final String mirrorHost;
    private final List<InstallerStyle.Stage> stages;

    private List<Step> steps;
    private int stageIndex;
    private int targetSlot = NO_DISK;
    private int desktopIndex = NO_DESKTOP;
    private int eraseSlot = NO_DISK;
    private int erasePrompt = NO_DISK;
    private String computerName;
    /** Whether the optional parts of the system (bsdinstall's ports) go with the copy; ticked by default. */
    private boolean portsSelected = true;
    /** Whether a Mirror that answers is actually used, which the Mirror page lets the player turn down. */
    private boolean useMirror;
    /** Whether the installed system's own scheduler starts at boot; on by default, as the real system ships. */
    private boolean cronEnabled = true;
    /** Whether the installed system answers other computers on the network; off until the Services page ticks it. */
    private boolean sshdEnabled;
    /** OpenBSD's own answers, the bits above. */
    private int options;
    /** Where on its disk the player put the system, what they left to the installer at its default. */
    private Placement placement = Placement.NONE;
    /** What the installer knows of the machine: its file system, its swap, the table it writes. */
    private InstallPlan.Facts facts = InstallPlan.Facts.PLAIN;

    private InstallerFlow(final InstallerStyle style, final String systemId, final String systemName,
                          final int footprintMb, final int copyTicks, final double baseRate,
                          final List<Disk> disks, final String suggestedName, final List<Desktop> desktops,
                          final String mirrorHost) {
        this.style = style;
        this.systemId = systemId;
        this.systemName = systemName;
        this.footprintMb = Math.max(0, footprintMb);
        this.baseRate = Math.max(0, baseRate);
        this.copyTicks = Math.max(1, copyTicks);
        this.disks = List.copyOf(disks);
        this.desktops = List.copyOf(desktops);
        this.mirrorHost = mirrorHost == null ? "" : mirrorHost;
        this.stages = style.stages();
        this.computerName = trimName(suggestedName);
        this.useMirror = !this.mirrorHost.isEmpty();
        for (final Disk disk : this.disks) {
            if (this.targetSlot == NO_DISK && this.roomOn(disk)) {
                this.targetSlot = disk.slot();
                this.timeTheCopy(disk);
            }
        }
        this.rebuildSteps();
    }

    /**
     * Works out how long the copy onto that disk takes, and leaves it as the time this installation quotes.
     *
     * <p>Does nothing for an installation that came off the wire or out of a save, which carries the time it
     * was given rather than the parts to work it out from: only the machine that started it knows those.
     */
    private void timeTheCopy(final Disk disk) {
        if (this.baseRate > 0) {
            this.copyTicks = Math.max(1,
                    SetupTiming.ticks(this.footprintMb, this.baseRate * disk.speed(), false));
        }
    }

    /**
     * An installation about to be set up, with the first disk that has room already chosen and the machine's
     * own name already in the name field, the way an installer suggests both.
     *
     * @param baseRate   what the machine copies at with the disk left out: the medium it reads from and the
     *                   processor that unpacks it. The disk is multiplied in when the player chooses one, so
     *                   choosing a faster disk really does shorten the copy
     * @param mirrorHost the Mirror answering this machine, empty when none does
     */
    public static InstallerFlow beginning(final InstallerStyle style, final String systemId,
                                          final String systemName, final int footprintMb, final double baseRate,
                                          final List<Disk> disks, final String suggestedName,
                                          final List<Desktop> desktops, final String mirrorHost) {
        return new InstallerFlow(style, systemId, systemName, footprintMb, 1, baseRate, disks, suggestedName,
                desktops, mirrorHost);
    }

    /**
     * An installation whose copy has already been timed, which carries that time rather than working one out.
     *
     * <p>This is what comes off the wire and out of a save: the machine that started it worked the time out
     * from its own parts, and everything downstream of that is told the answer rather than asked to guess it
     * again from disks that may have been swapped underneath.
     *
     * @param copyTicks how long the copy of the system itself takes, already decided
     */
    public static InstallerFlow quoted(final InstallerStyle style, final String systemId, final String systemName,
                                       final int footprintMb, final int copyTicks, final List<Disk> disks,
                                       final String suggestedName, final List<Desktop> desktops,
                                       final String mirrorHost) {
        return new InstallerFlow(style, systemId, systemName, footprintMb, copyTicks, 0, disks, suggestedName,
                desktops, mirrorHost);
    }

    /**
     * An installation put back where it was: the same machine read again, with the answers it had been given.
     *
     * <p>Used both by a machine coming back after a reload and by a monitor being opened on one, so what the
     * player sees and what the machine holds are built the same way from the same facts.
     */
    public static InstallerFlow restored(final InstallerStyle style, final String systemId, final String systemName,
                                         final int footprintMb, final int copyTicks, final List<Disk> disks,
                                         final List<Desktop> desktops, final String mirrorHost,
                                         final int stageIndex, final int targetSlot, final String computerName,
                                         final String desktopId, final int eraseSlot) {
        return restored(style, systemId, systemName, footprintMb, copyTicks, disks, desktops, mirrorHost, stageIndex,
                targetSlot, computerName, desktopId, eraseSlot, true, !mirrorHost.isEmpty(), true, false, 0);
    }

    /**
     * The same, also carrying the answers bsdinstall's own pages add: whether the ports component is going with
     * the copy, whether the Mirror that answered is actually being used, and the two services page; and OpenBSD's
     * own, its {@code options}.
     */
    public static InstallerFlow restored(final InstallerStyle style, final String systemId, final String systemName,
                                         final int footprintMb, final int copyTicks, final List<Disk> disks,
                                         final List<Desktop> desktops, final String mirrorHost,
                                         final int stageIndex, final int targetSlot, final String computerName,
                                         final String desktopId, final int eraseSlot, final boolean portsSelected,
                                         final boolean useMirror, final boolean cronEnabled,
                                         final boolean sshdEnabled, final int options) {
        /*
         * A restored installation keeps the time it was quoted rather than working one out again: the copy is
         * already under way at that speed, and a disk swapped underneath it must not make the bar jump.
         */
        final InstallerFlow flow = quoted(style, systemId, systemName, footprintMb, copyTicks, disks,
                computerName, desktops, mirrorHost);
        flow.restoreErase(eraseSlot);
        flow.select(targetSlot);
        flow.cronEnabled = cronEnabled;
        flow.sshdEnabled = sshdEnabled;
        flow.portsSelected = portsSelected;
        flow.options = options;
        flow.useMirror = useMirror && flow.mirrorAnswers();
        flow.chooseDesktop(indexOfDesktop(flow, desktopId));
        flow.goToStage(stageIndex);
        return flow;
    }

    public InstallerStyle style() {
        return this.style;
    }

    /** The system being installed, as the registry names it. */
    public String systemId() {
        return this.systemId;
    }

    public String systemName() {
        return this.systemName;
    }

    /** What the system takes on the disk, which is what every page that offers a disk has to say. */
    public int footprintMb() {
        return this.footprintMb;
    }

    /** How long the copy of the system itself takes, before anything the Mirror adds to it. */
    public int copyTicks() {
        return this.copyTicks;
    }

    public List<Disk> disks() {
        return this.disks;
    }

    /** The desktops a Mirror could serve with this system, empty when it serves none or none answers. */
    public List<Desktop> desktops() {
        return this.desktops;
    }

    /** The Mirror answering this machine, by the name its host goes by; empty when none answers. */
    public String mirrorHost() {
        return this.mirrorHost;
    }

    public boolean mirrorAnswers() {
        return !this.mirrorHost.isEmpty();
    }

    /** Whether the optional parts of the system (bsdinstall's ports) are going with the copy. */
    public boolean portsSelected() {
        return this.portsSelected;
    }

    /** Ticks or unticks the optional parts of the system; changes what the copy's own steps come to. */
    public void setPortsSelected(final boolean selected) {
        this.portsSelected = selected;
        this.rebuildSteps();
    }

    /** Whether a Mirror that answers is actually being used, as the Mirror page lets the player choose. */
    public boolean useMirror() {
        return this.useMirror;
    }

    /**
     * Whether the desktop page is one this installation actually shows: a Mirror has to answer, and the player
     * has to have kept it rather than choosing none on the Mirror page.
     */
    public boolean desktopStageActive() {
        return this.mirrorAnswers() && this.useMirror;
    }

    /**
     * Chooses whether the Mirror that answered is used at all; turning it down is choosing none, which drops
     * whatever desktop was chosen along with it, since none is offered without one.
     */
    public void setUseMirror(final boolean use) {
        this.useMirror = use && this.mirrorAnswers();
        if (!this.useMirror) {
            this.desktopIndex = NO_DESKTOP;
        }
        this.rebuildSteps();
    }

    /** Whether the installed system's own scheduler starts at boot. */
    public boolean cronEnabled() {
        return this.cronEnabled;
    }

    public void setCronEnabled(final boolean enabled) {
        this.cronEnabled = enabled;
    }

    /**
     * Whether the installed system answers other computers on the network, the same switch as {@code config remote}.
     */
    public boolean sshdEnabled() {
        return this.sshdEnabled;
    }

    public void setSshdEnabled(final boolean enabled) {
        this.sshdEnabled = enabled;
    }

    /** OpenBSD's own answers, as the bits {@link #NO_BSD_RD} to {@link #XENODM}. */
    public int options() {
        return this.options;
    }

    /** Whether that one of OpenBSD's answers is given. */
    public boolean has(final int option) {
        return (this.options & option) != 0;
    }

    /**
     * Gives OpenBSD's answers; a set left out drops its step from the copy. Leaving out the kernel or the base
     * system is no answer the game takes, so only the bits above are kept.
     */
    public void setOptions(final int given) {
        this.options = given & (NO_BSD_RD | NO_COMP | NO_MAN | NO_X | XENODM);
        if (this.has(NO_X)) {
            this.options &= ~XENODM;
        }
        this.rebuildSteps();
    }

    /** Where on its disk the player put the system, with what they left to the installer at its default. */
    public Placement placement() {
        return this.placement;
    }

    /** What the installer knows of the machine beyond its pages. */
    public InstallPlan.Facts facts() {
        return this.facts;
    }

    /** Tells the installer what it knows of the machine: the file system it formats with, its swap and its table. */
    public void setFacts(final InstallPlan.Facts known) {
        this.facts = known == null ? InstallPlan.Facts.PLAIN : known;
    }

    /**
     * Chooses the first disk with room for the system when the one chosen has none, as an installer opening suggests
     * one: asked again once the installer knows the machine, since what fits depends on it.
     */
    public void suggestDisk() {
        if (this.started() || this.roomOn(this.target())) {
            return;
        }
        for (final Disk disk : this.disks) {
            if (this.roomOn(disk)) {
                this.select(disk.slot());
                return;
            }
        }
    }

    /** Where the system goes as things stand, and what is made for it. */
    public InstallPlan.Plan plan() {
        return InstallPlan.of(this);
    }

    /** Puts the system on the partition or in the free space starting at {@code mb} of the chosen disk. */
    public void place(final long mb) {
        if (!this.started()) {
            this.placement = this.placement.at(mb);
        }
    }

    /** Formats the system's partition with that file system, {@link Placement#KEEP} or empty for the installer's. */
    public void formatAs(final String fileSystem) {
        if (!this.started()) {
            this.placement = this.placement.formattedAs(fileSystem);
        }
    }

    /** Puts the system beside the one on its disk, whose partition is left at {@code mb}. */
    public void shrinkTo(final long mb) {
        if (!this.started()) {
            this.placement = this.placement.shrinking(mb);
        }
    }

    /** Makes the disk in {@code slot} the second of a ZFS mirror, or none with {@link #NO_DISK}. */
    public void mirrorOn(final int slot) {
        if (!this.started()) {
            this.placement = this.placement.mirroredOn(slot == this.targetSlot ? NO_DISK : slot);
        }
    }

    /** Puts back where the player had put the system, as a reload or a monitor opened on the machine does. */
    public void restorePlacement(final Placement restored) {
        this.placement = restored == null ? Placement.NONE : restored;
    }

    /**
     * The disks as they now are, read again after the disk step changed one: the choices stay where they were, and a
     * partition or free space chosen that is gone is let go.
     */
    public void refreshDisks(final List<Disk> now) {
        this.disks = List.copyOf(now);
        final Disk target = this.target();
        if (target != null && this.placement.startMb() != Placement.ANYWHERE
                && target.spotAt(this.placement.startMb()) == null) {
            this.placement = this.placement.onAnotherDisk();
        }
    }

    /**
     * The partition of the system beside the new one that the installer shrinks, or null when the disk has none it
     * can shrink.
     */
    @Nullable
    public DiskSpot besideSpot() {
        final Disk target = this.target();
        return target == null ? null : InstallPlan.besideOf(this, target);
    }

    /**
     * Whether the system goes in beside one already on its disk rather than over it: the page with the divider is
     * shown only then (Linux Mint's).
     */
    public boolean alongside() {
        final Disk disk = this.target();
        return disk != null && disk.hasSystem() && disk.slot() != this.eraseSlot;
    }

    public int stageIndex() {
        return this.stageIndex;
    }

    public InstallerStyle.Stage stage() {
        return this.stages.get(this.stageIndex);
    }

    public InstallerPage page() {
        return this.stage().page();
    }

    public InstallerChrome chrome() {
        return this.stage().chrome();
    }

    public int targetSlot() {
        return this.targetSlot;
    }

    /** The disk the system is going on, or null while none has room or none is in the machine. */
    public Disk target() {
        return this.diskAt(this.targetSlot);
    }

    public String computerName() {
        return this.computerName;
    }

    public int desktopIndex() {
        return this.desktopIndex;
    }

    /** The desktop chosen to come with the system, or null for the terminal alone. */
    public Desktop desktop() {
        return this.desktopIndex >= 0 && this.desktopIndex < this.desktops.size()
                ? this.desktops.get(this.desktopIndex) : null;
    }

    /** The desktop chosen, as the registry names it, or empty for the terminal alone. */
    public String desktopId() {
        final Desktop chosen = this.desktop();
        return chosen == null ? "" : chosen.id();
    }

    /** Where that desktop sits in the list this machine was offered, or {@link #NO_DESKTOP} when it is not in it. */
    public static int indexOfDesktop(final InstallerFlow flow, final String id) {
        if (id == null || id.isEmpty()) {
            return NO_DESKTOP;
        }
        for (int i = 0; i < flow.desktops().size(); i++) {
            if (flow.desktops().get(i).id().equals(id)) {
                return i;
            }
        }
        return NO_DESKTOP;
    }

    /** The disk this installation erases first, or {@link #NO_DISK} when it erases nothing. */
    public int eraseSlot() {
        return this.eraseSlot;
    }

    /** The disk an erase has been offered for and not yet answered, or {@link #NO_DISK}. */
    public int erasePrompt() {
        return this.erasePrompt;
    }

    /** Every step of the work, in order, with the page each belongs to and the time it takes. */
    public List<Step> steps() {
        return this.steps;
    }

    /** How long the whole installation takes, the system and whatever the Mirror adds to it. */
    public int ticksTotal() {
        int total = 0;
        for (final Step step : this.steps) {
            total += step.ticks();
        }
        return total;
    }

    /**
     * How far the copy may run as things stand: to the end of the page it is on, and no further.
     *
     * <p>This is what keeps an installer from finishing behind a question nobody has answered.
     */
    public int ticksUnlocked() {
        int unlocked = 0;
        for (final Step step : this.steps) {
            if (step.stageIndex() <= this.stageIndex) {
                unlocked += step.ticks();
            }
        }
        return unlocked;
    }

    /** Whether any of the work has been done, which is the moment quitting stops being possible. */
    public boolean started() {
        return this.ticksUnlocked() > 0;
    }

    /** Whether the player can still walk out with nothing written. */
    public boolean quittable() {
        return !this.started();
    }

    /** The step the work is on after that many ticks, or the last one once it is all done. */
    public int stepAt(final int ticksDone) {
        int passed = 0;
        for (int i = 0; i < this.steps.size(); i++) {
            passed += this.steps.get(i).ticks();
            if (ticksDone < passed) {
                return i;
            }
        }
        return Math.max(0, this.steps.size() - 1);
    }

    /** How far through the whole installation that many ticks are, in thousandths. */
    public int permille(final int ticksDone) {
        final int total = this.ticksTotal();
        return total <= 0 ? 1000 : (int) Math.min(1000L, 1000L * Math.max(0, ticksDone) / total);
    }

    /** How far through its own step, in thousandths, so a bar can walk across one step at a time. */
    public int stepPermille(final int ticksDone) {
        if (this.steps.isEmpty()) {
            return 1000;
        }
        final int index = this.stepAt(ticksDone);
        int before = 0;
        for (int i = 0; i < index; i++) {
            before += this.steps.get(i).ticks();
        }
        final int own = this.steps.get(index).ticks();
        if (own <= 0) {
            return 1000;
        }
        return (int) Math.max(0L, Math.min(1000L, 1000L * (ticksDone - before) / own));
    }

    /** Whether that page still wants an answer, which is what an installer that lists them marks. */
    public boolean wants(final InstallerPage page) {
        return switch (page) {
            case DISK, SETTINGS -> this.target() == null || !this.roomOn(this.target());
            case NAME -> this.computerName.isBlank();
            default -> false;
        };
    }

    /** Whether the page it is on has what it needs to move on. */
    public boolean canContinue() {
        return switch (this.page()) {
            case DISK -> !this.wants(InstallerPage.DISK);
            // Beside another system, the room is what is free beside it.
            case ALONGSIDE, FORMAT, POOL -> !this.wants(InstallerPage.DISK);
            case SETTINGS -> !this.wants(InstallerPage.DISK) && !this.wants(InstallerPage.NAME);
            case NAME -> !this.wants(InstallerPage.NAME);
            case HUB -> !this.wants(InstallerPage.DISK) && !this.wants(InstallerPage.NAME);
            // Only the drive answers a page asking for the next disk, when that disk goes in.
            case COPY, DONE, INSERT -> false;
            // The Quince System's Easy Install is its disk page too: it installs onto the disk it shows.
            case WELCOME -> this.style != InstallerStyle.QUINCE_SYSTEM || !this.wants(InstallerPage.DISK);
            default -> true;
        };
    }

    /**
     * Which of the system's disks the installer is on, counting from one: the one being read on a page of the copy,
     * and the one wanted on a page asking for the next. A system on one medium is always on its first.
     */
    public int disk() {
        int disk = 1;
        for (int i = 0; i <= this.stageIndex && i < this.stages.size(); i++) {
            if (this.stages.get(i).page() == InstallerPage.INSERT) {
                disk++;
            }
        }
        return disk;
    }

    /** Moves on from a page asking for the next disk, once the drive holds it. */
    public void diskInserted() {
        if (this.page() == InstallerPage.INSERT) {
            this.goToStage(this.stageIndex + 1);
        }
    }

    /**
     * Whether the system can go on that disk: where the player put it on the chosen one, and wherever the installer
     * would put it on another, counting one that is about to be erased as empty.
     */
    public boolean roomOn(final Disk disk) {
        return disk != null && InstallPlan.on(this, disk).ok();
    }

    /** What that disk has free, counting one that is about to be erased as the whole of it. */
    public int freeOn(final Disk disk) {
        if (disk == null) {
            return 0;
        }
        return disk.slot() == this.eraseSlot ? disk.sizeMb() : disk.freeMb();
    }

    /** The disk in that slot, or null when the machine has none there. */
    public Disk diskAt(final int slot) {
        for (final Disk disk : this.disks) {
            if (disk.slot() == slot) {
                return disk;
            }
        }
        return null;
    }

    /** Moves to the next page, or back to the list of questions on the installer that keeps one. */
    public void next() {
        if (!this.canContinue()) {
            return;
        }
        if (this.page() == InstallerPage.HUB) {
            this.begin();
            return;
        }
        if (this.style.gathersQuestions()) {
            this.goTo(InstallerPage.HUB);
            return;
        }
        int target = this.stageIndex + 1;
        while (target < this.stages.size() - 1 && this.hiddenStage(target)) {
            target++;
        }
        this.goToStage(target);
    }

    /** Moves back a page, or to the list of questions on the installer that keeps one. */
    public void back() {
        if (this.started()) {
            return;
        }
        if (this.style.gathersQuestions() && this.page() != InstallerPage.HUB) {
            this.goTo(InstallerPage.HUB);
            return;
        }
        int target = this.stageIndex - 1;
        while (target > 0 && this.hiddenStage(target)) {
            target--;
        }
        this.goToStage(target);
    }

    /**
     * Whether that stage is one this installation is not showing right now: the desktop page of a style that asks
     * about a Mirror at all, when no Mirror is being used to serve one. A style with no Mirror page of its own
     * never hides its desktop page this way, so an installer that always offered one (off a network or on) keeps
     * offering it exactly where it always did. Every stage a style declares stays in its fixed list; this is what
     * lets one of them go unseen without the list itself changing under a page turned mid-installation.
     */
    private boolean hiddenStage(final int index) {
        final InstallerPage page = this.stages.get(index).page();
        // The divider is only there beside another system: a disk taken whole has nothing to divide.
        if (page == InstallerPage.ALONGSIDE) {
            return !this.alongside();
        }
        // The ZFS page is only there when the system is going on ZFS.
        if (page == InstallerPage.POOL) {
            return InstallPlan.formatWith(this) != FileSystemType.ZFS;
        }
        return page == InstallerPage.DESKTOP && this.hasMirrorStage() && !this.desktopStageActive();
    }

    /** Whether this style's own pages ask where packages and ports come from, which is what makes a Mirror optional. */
    private boolean hasMirrorStage() {
        for (final InstallerStyle.Stage stage : this.stages) {
            if (stage.page() == InstallerPage.MIRROR) {
                return true;
            }
        }
        return false;
    }

    /** Jumps to a page by name, which is how the list of questions reaches the one being answered. */
    public void goTo(final InstallerPage page) {
        for (int i = 0; i < this.stages.size(); i++) {
            if (this.stages.get(i).page() == page) {
                this.goToStage(i);
                return;
            }
        }
    }

    /** Puts the installer on that page by its place in the order; also how a saved one is restored. */
    public void goToStage(final int index) {
        this.stageIndex = Math.max(0, Math.min(index, this.stages.size() - 1));
        this.erasePrompt = NO_DISK;
    }

    /** Starts the work from the list of questions, once nothing on it is still wanted. */
    public void begin() {
        if (!this.canContinue()) {
            return;
        }
        for (int i = 0; i < this.stages.size(); i++) {
            if (!this.stages.get(i).steps().isEmpty()) {
                this.goToStage(i);
                return;
            }
        }
    }

    /**
     * Carries the work on past a page whose steps are done, and answers whether it moved.
     *
     * <p>A page that asks something stays put: that is the whole point of the steps belonging to the pages.
     */
    public boolean advance(final int ticksDone) {
        if (this.stage().asks() || this.stageIndex >= this.stages.size() - 1) {
            return false;
        }
        if (ticksDone < this.ticksUnlocked()) {
            return false;
        }
        this.goToStage(this.stageIndex + 1);
        return true;
    }

    /**
     * Chooses the disk the system goes on; a slot the machine has no disk in is ignored.
     *
     * <p>Choosing also times the copy again, because the disk is part of how fast it goes: picking the
     * solid-state one over the mechanical one is picking a shorter wait, and the page has to say so before
     * the copy starts rather than after it.
     */
    public void select(final int slot) {
        final Disk disk = this.diskAt(slot);
        if (disk != null) {
            // An erase was agreed to for the disk chosen then; choosing another before the copy starts takes it back.
            if (slot != this.eraseSlot && !this.started()) {
                this.eraseSlot = NO_DISK;
            }
            // Where it went on the disk chosen before means nothing on this one.
            if (slot != this.targetSlot && !this.started()) {
                this.placement = this.placement.onAnotherDisk();
            }
            this.targetSlot = slot;
            this.timeTheCopy(disk);
            // The steps are what the page shows and what unlocks it, so they share out the copy's new length.
            this.rebuildSteps();
        }
    }

    /** Names the computer, trimmed to what a name may be. */
    public void setComputerName(final String name) {
        this.computerName = trimName(name);
    }

    /** Chooses the desktop that comes with the system, or {@link #NO_DESKTOP} for the terminal alone. */
    public void chooseDesktop(final int index) {
        this.desktopIndex = index >= 0 && index < this.desktops.size() ? index : NO_DESKTOP;
        this.rebuildSteps();
    }

    /** Offers to erase a disk, which is the one destructive moment and so is always asked twice. */
    public void askErase(final int slot) {
        if (this.diskAt(slot) != null) {
            this.erasePrompt = slot;
        }
    }

    /** Takes the offer: the disk counts as empty from here, and the machine erases it before it copies. */
    public void confirmErase() {
        if (this.erasePrompt != NO_DISK) {
            this.eraseSlot = this.erasePrompt;
            this.targetSlot = this.erasePrompt;
            this.erasePrompt = NO_DISK;
            // Erasing a disk is choosing it, and a disk chosen is a copy timed for that disk.
            final Disk disk = this.diskAt(this.targetSlot);
            if (disk != null) {
                this.timeTheCopy(disk);
                this.rebuildSteps();
            }
        }
    }

    /**
     * Leaves the disk alone: undoes an offer not yet taken, and, as long as nothing has been written yet, an
     * erase already agreed to as well, since one questions asked twice is answered no differently from being
     * asked once inline, and neither ever destroys anything before the copy itself begins.
     */
    public void cancelErase() {
        this.erasePrompt = NO_DISK;
        if (!this.started()) {
            this.eraseSlot = NO_DISK;
            this.rebuildSteps();
        }
    }

    /** Restores an erase that was agreed to before the world went away. */
    public void restoreErase(final int slot) {
        this.eraseSlot = this.diskAt(slot) != null ? slot : NO_DISK;
    }

    private static String trimName(final String name) {
        if (name == null) {
            return "";
        }
        final String trimmed = name.strip();
        return trimmed.length() <= MOST_NAME_LETTERS ? trimmed : trimmed.substring(0, MOST_NAME_LETTERS);
    }

    /*
     * The steps of the system's own copy share its time evenly, and what the Mirror adds keeps its own: a
     * desktop comes over the network at the network's speed, which is not the speed of the disc in the drive.
     *
     * <p>Every step declared, kept or not, is weighed against the whole: a component left out (bsdinstall's
     * ports, when it was not ticked) simply drops its own even share off the total instead of being folded into
     * the ones that stay, which is what makes leaving it out really shorten the copy.
     */
    private void rebuildSteps() {
        final List<Step> built = new ArrayList<>();
        int declared = 0;
        for (final InstallerStyle.Stage stage : this.stages) {
            declared += stage.steps().size();
        }
        final int total = Math.max(1, declared);
        int position = 0;
        int lastWithSteps = 0;
        /*
         * bsdinstall's copy already runs before the Desktop page: a desktop chosen there adds a step of its own,
         * and that step has to be gated behind the Desktop page rather than behind whichever earlier stage last
         * had static steps of its own, or the fetch would finish before the player had chosen anything to fetch.
         */
        int afterDesktop = -1;
        for (int i = 0; i < this.stages.size(); i++) {
            if (this.stages.get(i).page() == InstallerPage.DESKTOP) {
                afterDesktop = i + 1;
            }
            for (final Text label : this.stages.get(i).steps()) {
                final int through = (int) ((long) this.copyTicks * (position + 1) / total);
                final int before = (int) ((long) this.copyTicks * position / total);
                position++;
                if (this.excluded(label)) {
                    continue;
                }
                built.add(new Step(label, i, through - before));
                lastWithSteps = i;
            }
        }
        final Desktop chosen = this.desktop();
        if (chosen != null) {
            final Text label = this.mirrorAnswers() ? INSTALLING_FROM_MIRROR.with(chosen.name(), this.mirrorHost)
                    : INSTALLING_DESKTOP.with(chosen.name());
            final int attachAt = afterDesktop < 0 ? lastWithSteps
                    : Math.max(lastWithSteps, Math.min(afterDesktop, this.stages.size() - 1));
            built.add(new Step(label, attachAt, chosen.ticks()));
        }
        this.steps = List.copyOf(built);
    }

    /**
     * Whether that static step is left out of the copy: bsdinstall's ports, when the component was not ticked, and
     * an OpenBSD set left out.
     */
    private boolean excluded(final Text label) {
        if (this.style == InstallerStyle.OPENBSD) {
            return this.has(NO_BSD_RD) && label.equals(InstallerStyle.SET_BSD_RD)
                    || this.has(NO_COMP) && label.equals(InstallerStyle.SET_COMP)
                    || this.has(NO_MAN) && label.equals(InstallerStyle.SET_MAN);
        }
        return this.style == InstallerStyle.BSD_INSTALL && !this.portsSelected
                && label.equals(InstallerStyle.STEP_BSD_PORTS.text());
    }

    /**
     * A disk the installer can offer, as a page that offers one has to describe it.
     *
     * @param slot   where it sits in the machine
     * @param label  the drive as it is written on it
     * @param sizeMb how big the whole disk is
     * @param freeMb what is free on it as it stands, in its partitions and outside them
     * @param holds  the system already on it, empty when it holds none
     * @param speed  its tier, where a mechanical disk is 1, which is what makes one disk a quicker install
     *               than another and so makes this page a choice about something
     * @param scheme how its table is written
     * @param spots  its partitions and the free space between them, in the order they lie
     */
    public record Disk(int slot, String label, int sizeMb, int freeMb, String holds, int speed,
                       PartitionScheme scheme, List<DiskSpot> spots) {

        public Disk {
            holds = holds == null ? "" : holds;
            speed = Math.max(1, speed);
            spots = List.copyOf(spots);
        }

        /** A disk nothing has partitioned: one FAT partition over it all, holding what it holds. */
        public Disk(final int slot, final String label, final int sizeMb, final int freeMb, final String holds,
                    final int speed) {
            this(slot, label, sizeMb, freeMb, holds, speed, PartitionScheme.MBR, List.of(new DiskSpot(1, 0L, sizeMb,
                    freeMb, FileSystemType.FAT32.serializedName(), holds, "", PartitionRole.DATA, true)));
        }

        /** Whether the disk carries a system, which is what makes choosing it a thing to confirm. */
        public boolean hasSystem() {
            return !this.holds.isEmpty();
        }

        /** Whether nothing on it would be lost to an erase: no system and nothing held in any partition. */
        public boolean holdsNothing() {
            for (final DiskSpot spot : this.spots) {
                if (spot.hasSystem() || spot.usedMb() > 0L) {
                    return false;
                }
            }
            return true;
        }

        /** The partition or free space starting at {@code mb}, or null when nothing starts there. */
        @Nullable
        public DiskSpot spotAt(final long mb) {
            for (final DiskSpot spot : this.spots) {
                if (spot.startMb() == mb) {
                    return spot;
                }
            }
            return null;
        }

        /** Its table as the tools see it. */
        public DiskLayout layout() {
            final List<PartitionEntry> partitions = new ArrayList<>();
            for (final DiskSpot spot : this.spots) {
                if (!spot.isFree()) {
                    partitions.add(spot.entry());
                }
            }
            return new DiskLayout(this.scheme, this.sizeMb, partitions);
        }
    }

    /**
     * A desktop a Mirror can serve along with the system.
     *
     * @param id     the package, as the registry names it
     * @param name   the desktop by the name a person reads
     * @param sizeMb what it takes on the disk
     * @param ticks  how long it takes to come over the network onto this machine
     */
    public record Desktop(String id, String name, int sizeMb, int ticks) {
    }

    /**
     * One step of the work.
     *
     * @param label      what the installer says it is doing, read in the player's language
     * @param stageIndex the page it belongs to, which is what stops the copy running past a question
     * @param ticks      how long it takes
     */
    public record Step(Text label, int stageIndex, int ticks) {
    }
}
