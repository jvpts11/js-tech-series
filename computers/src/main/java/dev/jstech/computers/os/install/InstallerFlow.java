/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

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
public final class InstallerFlow {

    /** No disk chosen, and the answer a page gives when the machine has none at all. */
    public static final int NO_DISK = -1;

    /** No desktop: the system comes up at its terminal, which is an answer and not a missing one. */
    public static final int NO_DESKTOP = -1;

    /** How long a computer's name may be, the length the machines of the first age allowed. */
    public static final int MOST_NAME_LETTERS = 15;

    private final InstallerStyle style;
    private final String systemName;
    private final int footprintMb;
    private final int copyTicks;
    private final List<Disk> disks;
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

    private InstallerFlow(final InstallerStyle style, final String systemName, final int footprintMb,
                          final int copyTicks, final List<Disk> disks, final String suggestedName,
                          final List<Desktop> desktops, final String mirrorHost) {
        this.style = style;
        this.systemName = systemName;
        this.footprintMb = Math.max(0, footprintMb);
        this.copyTicks = Math.max(1, copyTicks);
        this.disks = List.copyOf(disks);
        this.desktops = List.copyOf(desktops);
        this.mirrorHost = mirrorHost == null ? "" : mirrorHost;
        this.stages = style.stages();
        this.computerName = trimName(suggestedName);
        for (final Disk disk : this.disks) {
            if (this.targetSlot == NO_DISK && this.roomOn(disk)) {
                this.targetSlot = disk.slot();
            }
        }
        this.rebuildSteps();
    }

    /**
     * An installation about to be set up, with the first disk that has room already chosen and the machine's
     * own name already in the name field, the way an installer suggests both.
     *
     * @param copyTicks  how long the copy of the system itself takes
     * @param mirrorHost the Mirror answering this machine, empty when none does
     */
    public static InstallerFlow beginning(final InstallerStyle style, final String systemName,
                                          final int footprintMb, final int copyTicks, final List<Disk> disks,
                                          final String suggestedName, final List<Desktop> desktops,
                                          final String mirrorHost) {
        return new InstallerFlow(style, systemName, footprintMb, copyTicks, disks, suggestedName, desktops,
                mirrorHost);
    }

    public InstallerStyle style() {
        return this.style;
    }

    public String systemName() {
        return this.systemName;
    }

    /** What the system takes on the disk, which is what every page that offers a disk has to say. */
    public int footprintMb() {
        return this.footprintMb;
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
            case SETTINGS -> !this.wants(InstallerPage.DISK) && !this.wants(InstallerPage.NAME);
            case NAME -> !this.wants(InstallerPage.NAME);
            case HUB -> !this.wants(InstallerPage.DISK) && !this.wants(InstallerPage.NAME);
            case COPY, DONE -> false;
            default -> true;
        };
    }

    /** Whether that disk has room for the system, counting one that is about to be erased as empty. */
    public boolean roomOn(final Disk disk) {
        return disk != null && this.freeOn(disk) >= this.footprintMb;
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
        this.goToStage(this.stageIndex + 1);
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
        this.goToStage(this.stageIndex - 1);
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

    /** Chooses the disk the system goes on; a slot the machine has no disk in is ignored. */
    public void select(final int slot) {
        if (this.diskAt(slot) != null) {
            this.targetSlot = slot;
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
        }
    }

    /** Leaves the disk alone. */
    public void cancelErase() {
        this.erasePrompt = NO_DISK;
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
     */
    private void rebuildSteps() {
        final List<Step> built = new ArrayList<>();
        int own = 0;
        for (final InstallerStyle.Stage stage : this.stages) {
            own += stage.steps().size();
        }
        final int total = Math.max(1, own);
        int written = 0;
        int lastWithSteps = 0;
        for (int i = 0; i < this.stages.size(); i++) {
            for (final String label : this.stages.get(i).steps()) {
                final int through = (int) ((long) this.copyTicks * (written + 1) / total);
                final int before = (int) ((long) this.copyTicks * written / total);
                built.add(new Step(label, i, through - before));
                written++;
                lastWithSteps = i;
            }
        }
        final Desktop chosen = this.desktop();
        if (chosen != null) {
            final String where = this.mirrorAnswers() ? " from the Mirror on " + this.mirrorHost : "";
            built.add(new Step("Installing " + chosen.name() + where, lastWithSteps, chosen.ticks()));
        }
        this.steps = List.copyOf(built);
    }

    /**
     * A disk the installer can offer, as a page that offers one has to describe it.
     *
     * @param slot   where it sits in the machine
     * @param label  the drive as it is written on it
     * @param sizeMb how big the whole disk is
     * @param freeMb what is free on it as it stands
     * @param holds  the system already on it, empty when it holds none
     */
    public record Disk(int slot, String label, int sizeMb, int freeMb, String holds) {

        public Disk {
            holds = holds == null ? "" : holds;
        }

        /** Whether the disk carries a system, which is what makes choosing it a thing to confirm. */
        public boolean hasSystem() {
            return !this.holds.isEmpty();
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
     * @param label      what the installer says it is doing
     * @param stageIndex the page it belongs to, which is what stops the copy running past a question
     * @param ticks      how long it takes
     */
    public record Step(String label, int stageIndex, int ticks) {
    }
}
