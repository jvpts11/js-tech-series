/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallerFlowTest {

    private static final InstallerFlow.Disk EMPTY_500 =
            new InstallerFlow.Disk(0, "Vaultis Swift SSD 500 GB", 512_000, 512_000, "", 4);

    private static final InstallerFlow.Disk FULL_WITH_UBUNTU =
            new InstallerFlow.Disk(1, "Vaultis Keep HDD 1 TB", 1_024_000, 512, "Ubuntu", 1);

    /** The mechanical disk of the pair, empty, so choosing between the two is choosing a speed. */
    private static final InstallerFlow.Disk EMPTY_SLOW =
            new InstallerFlow.Disk(1, "Vaultis Keep HDD 1 TB", 1_024_000, 1_024_000, "", 1);

    private static final InstallerFlow.Desktop GNOME =
            new InstallerFlow.Desktop("jsc:gnome", "GNOME", 192, 60);

    @Test
    void beginning_choosesTheFirstDiskWithRoomForTheSystem() {
        final InstallerFlow flow = frames11(List.of(FULL_WITH_UBUNTU, EMPTY_500));
        assertEquals(0, flow.targetSlot());
        assertNotNull(flow.target());
    }

    @Test
    void beginning_withNoDiskThatFits_choosesNoneAndWillNotMoveOn() {
        final InstallerFlow flow = frames11(List.of(FULL_WITH_UBUNTU));
        assertEquals(InstallerFlow.NO_DISK, flow.targetSlot());
        flow.next();
        assertEquals(InstallerPage.DISK, flow.page());
        assertFalse(flow.canContinue());
        assertTrue(flow.wants(InstallerPage.DISK));
    }

    @Test
    void beginning_suggestsTheMachinesOwnName() {
        assertEquals("STUDIO-11", frames11(List.of(EMPTY_500)).computerName());
    }

    @Test
    void setComputerName_keepsTheSpaceOffTheEndsAndTheNameItself() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        flow.setComputerName("  a-very-long-machine-name  ");
        assertEquals("a-very-long-machine-name", flow.computerName());
    }

    /** However long a name is typed, what the machine keeps has an end to it. */
    @Test
    void setComputerName_pastTheLimit_isCutToIt() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        flow.setComputerName("n".repeat(InstallerFlow.MOST_NAME_LETTERS + 50));
        assertEquals(InstallerFlow.MOST_NAME_LETTERS, flow.computerName().length());
    }

    @Test
    void setComputerName_blank_leavesThePageWantingAnAnswer() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        flow.setComputerName("   ");
        assertTrue(flow.wants(InstallerPage.NAME));
    }

    @Test
    void roomOn_aDiskAboutToBeErased_countsAsTheWholeDisk() {
        final InstallerFlow flow = frames11(List.of(FULL_WITH_UBUNTU));
        assertFalse(flow.roomOn(flow.diskAt(1)));
        flow.askErase(1);
        flow.confirmErase();
        assertTrue(flow.roomOn(flow.diskAt(1)));
        assertEquals(1, flow.targetSlot());
    }

    @Test
    void askErase_answeredNo_leavesTheDiskAsItWas() {
        final InstallerFlow flow = frames11(List.of(FULL_WITH_UBUNTU));
        flow.askErase(1);
        assertEquals(1, flow.erasePrompt());
        flow.cancelErase();
        assertEquals(InstallerFlow.NO_DISK, flow.erasePrompt());
        assertEquals(InstallerFlow.NO_DISK, flow.eraseSlot());
        assertFalse(flow.roomOn(flow.diskAt(1)));
    }

    @Test
    void askErase_aSlotWithNoDisk_isIgnored() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        flow.askErase(7);
        assertEquals(InstallerFlow.NO_DISK, flow.erasePrompt());
    }

    @Test
    void select_aSlotWithNoDisk_isIgnored() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        flow.select(9);
        assertEquals(0, flow.targetSlot());
    }

    @Test
    void steps_shareTheSystemsOwnTimeBetweenThem() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        assertEquals(4, flow.steps().size());
        assertEquals(100, flow.ticksTotal());
    }

    @Test
    void steps_aDesktopFromTheMirror_bringsItsOwnTimeOverAndAbove() {
        final InstallerFlow flow = ubuntu(List.of(EMPTY_500));
        final int withoutDesktop = flow.ticksTotal();
        flow.chooseDesktop(0);
        assertEquals(withoutDesktop + GNOME.ticks(), flow.ticksTotal());
        final InstallerFlow.Step last = flow.steps().get(flow.steps().size() - 1);
        assertEquals("Installing GNOME from the Mirror on CORE", last.label().english());
    }

    @Test
    void steps_noDesktop_addsNothingToTheWork() {
        final InstallerFlow flow = ubuntu(List.of(EMPTY_500));
        flow.chooseDesktop(0);
        flow.chooseDesktop(InstallerFlow.NO_DESKTOP);
        assertNull(flow.desktop());
        assertEquals(100, flow.ticksTotal());
    }

    @Test
    void ticksUnlocked_stopsAtTheQuestionTheInstallerHasNotAsked() {
        final InstallerFlow flow = framesXp(List.of(EMPTY_500));
        assertEquals(0, flow.ticksUnlocked());
        flow.next();
        assertEquals(25, flow.ticksUnlocked());
        flow.advance(25);
        assertEquals(InstallerPage.NAME, flow.page());
        assertEquals(50, flow.ticksUnlocked());
    }

    @Test
    void advance_aPageThatOnlyWorks_carriesOnByItself() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        flow.next();
        flow.next();
        flow.next();
        assertEquals(InstallerPage.COPY, flow.page());
        assertTrue(flow.advance(100));
        assertEquals(InstallerPage.DONE, flow.page());
    }

    @Test
    void advance_aPageThatAsks_waitsForTheAnswer() {
        final InstallerFlow flow = framesXp(List.of(EMPTY_500));
        flow.next();
        flow.advance(25);
        assertEquals(InstallerPage.NAME, flow.page());
        assertFalse(flow.advance(1000));
        assertEquals(InstallerPage.NAME, flow.page());
        flow.next();
        assertEquals(InstallerPage.COPY, flow.page());
    }

    @Test
    void advance_theLastPage_staysThere() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        flow.next();
        flow.next();
        flow.next();
        flow.advance(100);
        assertFalse(flow.advance(1000));
        assertEquals(InstallerPage.DONE, flow.page());
    }

    @Test
    void quittable_onlyWhileNothingHasBeenWritten() {
        final InstallerFlow flow = framesXp(List.of(EMPTY_500));
        assertTrue(flow.quittable());
        flow.next();
        assertFalse(flow.quittable());
        assertTrue(flow.started());
    }

    @Test
    void back_afterTheCopyHasStarted_goesNowhere() {
        final InstallerFlow flow = framesXp(List.of(EMPTY_500));
        flow.next();
        final int was = flow.stageIndex();
        flow.back();
        assertEquals(was, flow.stageIndex());
    }

    @Test
    void back_beforeAnythingIsWritten_returnsToTheQuestionBefore() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        flow.next();
        flow.next();
        assertEquals(InstallerPage.NAME, flow.page());
        flow.back();
        assertEquals(InstallerPage.DISK, flow.page());
    }

    @Test
    void next_theInstallerThatListsItsQuestions_returnsToTheListAfterEachAnswer() {
        final InstallerFlow flow = fedora(List.of(EMPTY_500));
        assertEquals(InstallerPage.HUB, flow.page());
        flow.goTo(InstallerPage.DISK);
        flow.next();
        assertEquals(InstallerPage.HUB, flow.page());
    }

    @Test
    void next_theInstallerThatListsItsQuestions_beginsOnceNothingIsStillWanted() {
        final InstallerFlow flow = fedora(List.of(EMPTY_500));
        flow.next();
        assertEquals(InstallerPage.COPY, flow.page());
        assertTrue(flow.started());
    }

    @Test
    void next_theInstallerThatListsItsQuestions_willNotBeginWithAnAnswerMissing() {
        final InstallerFlow flow = fedora(List.of(FULL_WITH_UBUNTU));
        flow.next();
        assertEquals(InstallerPage.HUB, flow.page());
        assertFalse(flow.started());
    }

    @Test
    void stepAt_namesTheStepTheWorkIsOn() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        assertEquals("Copying files", flow.steps().get(flow.stepAt(0)).label().english());
        assertEquals("Creating the system folders", flow.steps().get(flow.stepAt(25)).label().english());
        assertEquals("Setting up the boot entry", flow.steps().get(flow.stepAt(99)).label().english());
        assertEquals("Setting up the boot entry", flow.steps().get(flow.stepAt(1000)).label().english());
    }

    @Test
    void permille_runsOverTheWholeInstallationAndTheStepOverItsOwn() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        assertEquals(0, flow.permille(0));
        assertEquals(500, flow.permille(50));
        assertEquals(1000, flow.permille(200));
        assertEquals(0, flow.stepPermille(25));
        assertEquals(480, flow.stepPermille(37));
    }

    @Test
    void mirrorAnswers_saysWhetherThereIsOneToServeADesktop() {
        assertTrue(ubuntu(List.of(EMPTY_500)).mirrorAnswers());
        assertFalse(frames11(List.of(EMPTY_500)).mirrorAnswers());
    }

    @Test
    void portsSelected_isTickedByDefaultAndUntickingItShortensTheCopy() {
        final InstallerFlow flow = bsdInstall(List.of(EMPTY_500));
        assertTrue(flow.portsSelected());
        assertEquals(3, flow.steps().size(), "base, kernel and ports");
        final int total = flow.ticksTotal();
        flow.setPortsSelected(false);
        assertEquals(2, flow.steps().size(), "ports is left out of the copy");
        assertTrue(flow.ticksTotal() < total,
                "leaving a component out drops its own even share of the time rather than folding it into the "
                        + "rest: " + flow.ticksTotal() + " is not shorter than " + total);
        flow.setPortsSelected(true);
        assertEquals(3, flow.steps().size(), "and putting it back returns the step");
        assertEquals(total, flow.ticksTotal(), "and the copy's whole time with it");
    }

    @Test
    void cronAndSshd_defaultOnAndOffTheOppositeOfEveryOtherSystem() {
        final InstallerFlow flow = bsdInstall(List.of(EMPTY_500));
        assertTrue(flow.cronEnabled());
        assertFalse(flow.sshdEnabled());
        flow.setCronEnabled(false);
        flow.setSshdEnabled(true);
        assertFalse(flow.cronEnabled());
        assertTrue(flow.sshdEnabled());
    }

    @Test
    void useMirror_turnedDown_hidesTheDesktopPageAndDropsAnyDesktopAlreadyChosen() {
        final InstallerFlow flow = bsdInstallWithDesktop(List.of(EMPTY_500));
        assertTrue(flow.useMirror());
        assertTrue(flow.desktopStageActive());
        flow.chooseDesktop(0);
        assertTrue(flow.desktop() != null);
        flow.setUseMirror(false);
        assertNull(flow.desktop(), "choosing none drops whatever desktop was picked");
        assertFalse(flow.desktopStageActive());
        for (int i = 0; i < 20 && flow.page() != InstallerPage.DONE; i++) {
            assertNotEquals(InstallerPage.DESKTOP, flow.page(), "the desktop page is never landed on");
            if (flow.stage().asks()) {
                flow.next();
            } else {
                flow.advance(flow.ticksUnlocked());
            }
        }
        assertEquals(InstallerPage.DONE, flow.page(), "the installer still reaches its last page");
    }

    @Test
    void useMirror_cannotBeTurnedOnWhenNoMirrorAnswersAtAll() {
        final InstallerFlow flow = bsdInstall(List.of(EMPTY_500));
        assertFalse(flow.mirrorAnswers());
        flow.setUseMirror(true);
        assertFalse(flow.useMirror(), "there is nothing to use");
    }

    @Test
    void next_ubuntuOffAnyNetwork_stillReachesTheDesktopPage() {
        /*
         * A style with no Mirror page of its own (Ubuntu, Debian, Fedora) never had a way to turn a Mirror down:
         * it either answers or it does not. Its desktop page must show either way, unlike bsdinstall's own, which
         * a Mirror turned down really does hide.
         */
        final InstallerFlow flow = InstallerFlow.quoted(InstallerStyle.UBUNTU, "jsc:ubuntu", "Ubuntu", 8_192, 100,
                List.of(EMPTY_500), "RENDER-01", List.of(), "");
        assertFalse(flow.mirrorAnswers());
        flow.next();
        assertEquals(InstallerPage.NAME, flow.page());
        flow.next();
        assertEquals(InstallerPage.DESKTOP, flow.page(),
                "off any network, Ubuntu's desktop page comes up at its own terminal choice, not skipped");
    }

    @Test
    void cancelErase_afterConfirming_undoesItBeforeAnythingIsWritten() {
        final InstallerFlow flow = frames11(List.of(FULL_WITH_UBUNTU));
        flow.askErase(1);
        flow.confirmErase();
        assertEquals(1, flow.eraseSlot());
        flow.cancelErase();
        assertEquals(InstallerFlow.NO_DISK, flow.eraseSlot(),
                "a confirmed erase can still be taken back while nothing has been written");
    }

    @Test
    void select_anotherDiskAfterConfirmingAnErase_takesTheEraseBack() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500, FULL_WITH_UBUNTU));
        flow.askErase(FULL_WITH_UBUNTU.slot());
        flow.confirmErase();
        assertEquals(FULL_WITH_UBUNTU.slot(), flow.eraseSlot());
        flow.select(EMPTY_500.slot());
        assertEquals(InstallerFlow.NO_DISK, flow.eraseSlot(),
                "the erase was agreed to for the disk chosen then, not for the one installed onto now");
    }

    @Test
    void restored_carriesThePortsMirrorCronAndSshdAnswersThrough() {
        final InstallerFlow flow = InstallerFlow.restored(InstallerStyle.BSD_INSTALL, "jsc:freebsd", "FreeBSD",
                2_048, 100, List.of(EMPTY_500), List.of(GNOME), "CORE", 0, 0, "desk", "", InstallerFlow.NO_DISK,
                false, true, false, true);
        assertFalse(flow.portsSelected());
        assertTrue(flow.useMirror());
        assertFalse(flow.cronEnabled());
        assertTrue(flow.sshdEnabled());
    }

    private static InstallerFlow bsdInstall(final List<InstallerFlow.Disk> disks) {
        return InstallerFlow.quoted(InstallerStyle.BSD_INSTALL, "jsc:freebsd", "FreeBSD", 2_048, 100, disks,
                "desk", List.of(), "");
    }

    private static InstallerFlow bsdInstallWithDesktop(final List<InstallerFlow.Disk> disks) {
        return InstallerFlow.quoted(InstallerStyle.BSD_INSTALL, "jsc:freebsd", "FreeBSD", 2_048, 100, disks,
                "desk", List.of(GNOME), "CORE");
    }

    private static InstallerFlow frames11(final List<InstallerFlow.Disk> disks) {
        return InstallerFlow.quoted(InstallerStyle.FRAMES_11, "jsc:frames_11", "Frames 11", 20_480, 100, disks,
                "STUDIO-11", List.of(), "");
    }

    private static InstallerFlow framesXp(final List<InstallerFlow.Disk> disks) {
        return InstallerFlow.quoted(InstallerStyle.FRAMES_XP, "jsc:frames_xp", "Frames XP", 1_536, 100, disks,
                "OFFICE-XP", List.of(), "");
    }

    private static InstallerFlow ubuntu(final List<InstallerFlow.Disk> disks) {
        return InstallerFlow.quoted(InstallerStyle.UBUNTU, "jsc:ubuntu", "Ubuntu", 8_192, 100, disks,
                "RENDER-01", List.of(GNOME), "CORE");
    }

    private static InstallerFlow fedora(final List<InstallerFlow.Disk> disks) {
        return InstallerFlow.quoted(InstallerStyle.FEDORA, "jsc:fedora", "Fedora", 8_192, 100, disks,
                "LAB-02", List.of(GNOME), "CORE");
    }

    /**
     * A rate that leaves Frames XP clear of both clamps on either disk, so this is measuring the disk and
     * not the ceiling: 48 seconds on the mechanical one, 12 on the solid-state one.
     */
    private static final double CLEAR_OF_THE_CLAMPS = 32.0;

    private static InstallerFlow timedXp(final List<InstallerFlow.Disk> disks) {
        return InstallerFlow.beginning(InstallerStyle.FRAMES_XP, "jsc:frames_xp", "Frames XP", 1_536,
                CLEAR_OF_THE_CLAMPS, disks, "OFFICE-XP", List.of(), "");
    }

    /**
     * The disk page is a choice about something: the mechanical disk is the longer copy, and choosing the
     * solid-state one shortens it before a byte is written rather than after.
     */
    @Test
    void select_timesTheCopyAgainstTheDiskChosen() {
        final InstallerFlow flow = timedXp(List.of(EMPTY_500, EMPTY_SLOW));
        flow.select(EMPTY_SLOW.slot());
        final int onMechanical = flow.copyTicks();
        flow.select(EMPTY_500.slot());
        final int onSolidState = flow.copyTicks();
        assertEquals(48 * SetupTiming.TICKS_PER_SECOND, onMechanical);
        assertEquals(12 * SetupTiming.TICKS_PER_SECOND, onSolidState);
        assertEquals(EMPTY_500.speed() / EMPTY_SLOW.speed(), onMechanical / onSolidState,
                "as much sooner as the disk is faster, with neither end at a clamp");
    }

    /**
     * The steps the page shows, and what they unlock, follow the disk chosen, whether it was picked or erased to
     * be installed over. They used to keep the length of the first disk the installer had suggested.
     */
    @Test
    void steps_followTheDiskChosenOrErased() {
        final InstallerFlow picked = timedXp(List.of(EMPTY_500, EMPTY_SLOW));
        picked.select(EMPTY_SLOW.slot());
        assertEquals(picked.copyTicks(), picked.ticksTotal(), "the steps share out the slower disk's copy");

        final InstallerFlow erased = timedXp(List.of(EMPTY_500, FULL_WITH_UBUNTU));
        erased.askErase(FULL_WITH_UBUNTU.slot());
        erased.confirmErase();
        assertEquals(48 * SetupTiming.TICKS_PER_SECOND, erased.copyTicks(), "the erased disk's copy is timed");
        assertEquals(erased.copyTicks(), erased.ticksTotal(), "and the steps share out that copy");
    }

    /** A disk the machine has nothing in leaves the time exactly where the last real choice left it. */
    @Test
    void select_aSlotWithNoDisk_changesNothing() {
        final InstallerFlow flow = timedXp(List.of(EMPTY_500));
        final int chosen = flow.copyTicks();
        flow.select(9);
        assertEquals(EMPTY_500.slot(), flow.targetSlot());
        assertEquals(chosen, flow.copyTicks());
    }

    /** An installation told a time carries it, whatever disks it is holding. */
    @Test
    void quoted_carriesTheTimeItWasGivenAndDoesNotRetimeOnSelect() {
        final InstallerFlow flow = framesXp(List.of(EMPTY_500, EMPTY_SLOW));
        assertEquals(100, flow.copyTicks());
        flow.select(EMPTY_SLOW.slot());
        assertEquals(100, flow.copyTicks());
    }

    /**
     * An installation restored keeps the time it was quoted. The copy is already running at that speed, so
     * working one out again from the disks as they stand now would make the bar jump under the player.
     */
    @Test
    void restored_keepsTheTimeItWasGivenRatherThanWorkingOneOut() {
        final InstallerFlow flow = InstallerFlow.restored(InstallerStyle.FRAMES_XP, "jsc:frames_xp", "Frames XP",
                1_536, 640, List.of(EMPTY_500, EMPTY_SLOW), List.of(), "", 1, EMPTY_SLOW.slot(),
                "OFFICE-XP", "", InstallerFlow.NO_DISK);
        assertEquals(640, flow.copyTicks());
    }
}
