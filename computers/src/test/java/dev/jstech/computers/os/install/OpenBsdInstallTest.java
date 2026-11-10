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
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenBsdInstallTest {

    private static final InstallerFlow.Disk EMPTY_WD0 = new InstallerFlow.Disk(0, "wd0", 20_480, 20_480, "", 1);

    @Test
    void enter_onTheWelcomePageTakesIAndAsksAgainForAnythingElse() {
        final InstallerFlow flow = openBsd();
        final OpenBsdInstall.Answers fresh = OpenBsdInstall.Answers.fresh(flow);
        final OpenBsdInstall.Step install = OpenBsdInstall.enter(flow, fresh, "i");
        assertFalse(install.refused());
        assertEquals(List.of(OpenBsdInstall.Action.NEXT), kinds(install));
        assertTrue(OpenBsdInstall.enter(flow, fresh, "u").refused(), "only an install is offered");
    }

    @Test
    void enter_theServicesQuestionsEndInTheirSwitchesAndMoveOn() {
        final InstallerFlow flow = openBsd();
        flow.next();
        flow.next();
        assertEquals(InstallerPage.SERVICES, flow.page());
        final OpenBsdInstall.Step sshd = OpenBsdInstall.enter(flow, OpenBsdInstall.Answers.fresh(flow), "no");
        assertTrue(sshd.actions().isEmpty(), "the X question is still to come");
        final OpenBsdInstall.Step x = OpenBsdInstall.enter(flow, sshd.answers(), "no");
        assertEquals(List.of(OpenBsdInstall.Action.SSHD, OpenBsdInstall.Action.OPTIONS, OpenBsdInstall.Action.NEXT),
                kinds(x), "no X asks nothing about xenodm");
        assertEquals(0, x.actions().get(0).value(), "sshd answered no");
        assertEquals(InstallerFlow.NO_X, x.actions().get(1).value());
    }

    @Test
    void enter_aRootDiskNobodyHasIsRefusedWithAWord() {
        final InstallerFlow flow = onDiskPage();
        final OpenBsdInstall.Step step = OpenBsdInstall.enter(flow, OpenBsdInstall.Answers.fresh(flow), "sd7");
        assertTrue(step.actions().isEmpty());
        assertFalse(step.said().isEmpty());
    }

    @Test
    void enter_theEmptyRootDiskIsTakenWholeWithoutAQuestion() {
        final InstallerFlow flow = onDiskPage();
        final OpenBsdInstall.Step step = OpenBsdInstall.enter(flow, OpenBsdInstall.Answers.fresh(flow), "");
        assertEquals(List.of(OpenBsdInstall.Action.SELECT_DISK, OpenBsdInstall.Action.NEXT), kinds(step));
    }

    @Test
    void applySets_leavesOutWhatAMinusPatternNamesAndNeverTheKernelOrBase() {
        assertEquals(InstallerFlow.NO_MAN | InstallerFlow.NO_COMP, OpenBsdInstall.applySets(0, "-man* -comp*"));
        assertEquals(0, OpenBsdInstall.applySets(0, "-bsd -base*"), "the kernel and the base system stay");
        assertEquals(InstallerFlow.NO_COMP, OpenBsdInstall.applySets(InstallerFlow.NO_COMP | InstallerFlow.NO_MAN,
                "man75.tgz"), "naming a set brings it back");
    }

    @Test
    void applySets_allBringsEverySetBack() {
        assertEquals(0, OpenBsdInstall.applySets(OpenBsdInstall.SETS_LEFT_OUT, "all"));
    }

    @Test
    void savedMb_isTheShareOfTheSystemTheSetsLeftOutWouldTake() {
        assertEquals(0, OpenBsdInstall.savedMb(0, 1_024));
        assertEquals(171, OpenBsdInstall.savedMb(InstallerFlow.NO_COMP, 1_024));
        assertEquals(199, OpenBsdInstall.savedMb(OpenBsdInstall.SETS_LEFT_OUT, 1_024),
                "the three that may go are a fifth of the system: the base is the most of it");
    }

    private static InstallerFlow openBsd() {
        return InstallerFlow.quoted(InstallerStyle.OPENBSD, "jsc:openbsd", "OpenBSD", 1_024, 100, List.of(EMPTY_WD0),
                "harbour", List.of(), "");
    }

    /* A fresh installer moved on to its disk page, as the pages before it let it. */
    private static InstallerFlow onDiskPage() {
        final InstallerFlow flow = openBsd();
        for (int page = 0; page < InstallerPage.values().length && flow.page() != InstallerPage.DISK; page++) {
            flow.next();
        }
        assertEquals(InstallerPage.DISK, flow.page());
        return flow;
    }

    private static List<Integer> kinds(final OpenBsdInstall.Step step) {
        return step.actions().stream().map(OpenBsdInstall.Action::kind).toList();
    }
}
