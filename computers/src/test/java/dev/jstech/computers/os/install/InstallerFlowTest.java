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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallerFlowTest {

    private static final InstallerFlow.Disk EMPTY_500 =
            new InstallerFlow.Disk(0, "Vaultis Swift SSD 500 GB", 512_000, 512_000, "");

    private static final InstallerFlow.Disk FULL_WITH_UBUNTU =
            new InstallerFlow.Disk(1, "Vaultis Keep HDD 1 TB", 1_024_000, 512, "Ubuntu");

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
        assertFalse(flow.canContinue());
        assertTrue(flow.wants(InstallerPage.DISK));
    }

    @Test
    void beginning_suggestsTheMachinesOwnName() {
        assertEquals("STUDIO-11", frames11(List.of(EMPTY_500)).computerName());
    }

    @Test
    void setComputerName_isTrimmedToWhatANameMayBe() {
        final InstallerFlow flow = frames11(List.of(EMPTY_500));
        flow.setComputerName("  a-very-long-machine-name  ");
        assertEquals(InstallerFlow.MOST_NAME_LETTERS, flow.computerName().length());
        assertEquals("a-very-long-mac", flow.computerName());
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
        assertEquals("Installing GNOME from the Mirror on CORE", last.label());
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
        assertEquals("Copying files", flow.steps().get(flow.stepAt(0)).label());
        assertEquals("Creating the system folders", flow.steps().get(flow.stepAt(25)).label());
        assertEquals("Setting up the boot entry", flow.steps().get(flow.stepAt(99)).label());
        assertEquals("Setting up the boot entry", flow.steps().get(flow.stepAt(1000)).label());
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

    private static InstallerFlow frames11(final List<InstallerFlow.Disk> disks) {
        return InstallerFlow.beginning(InstallerStyle.FRAMES_11, "Frames 11", 20_480, 100, disks,
                "STUDIO-11", List.of(), "");
    }

    private static InstallerFlow framesXp(final List<InstallerFlow.Disk> disks) {
        return InstallerFlow.beginning(InstallerStyle.FRAMES_XP, "Frames XP", 1_536, 100, disks,
                "OFFICE-XP", List.of(), "");
    }

    private static InstallerFlow ubuntu(final List<InstallerFlow.Disk> disks) {
        return InstallerFlow.beginning(InstallerStyle.UBUNTU, "Ubuntu", 8_192, 100, disks,
                "RENDER-01", List.of(GNOME), "CORE");
    }

    private static InstallerFlow fedora(final List<InstallerFlow.Disk> disks) {
        return InstallerFlow.beginning(InstallerStyle.FEDORA, "Fedora", 8_192, 100, disks,
                "LAB-02", List.of(GNOME), "CORE");
    }
}
