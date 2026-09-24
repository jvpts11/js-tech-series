/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.core.text.Text;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallerStyleTest {

    @Test
    void stages_everyInstaller_endsWaitingToBeRestarted() {
        for (final InstallerStyle style : InstallerStyle.values()) {
            final List<InstallerStyle.Stage> stages = style.stages();
            assertEquals(InstallerPage.DONE, stages.get(stages.size() - 1).page(), style.name());
        }
    }

    @Test
    void stages_everyInstaller_hasWorkToDo() {
        for (final InstallerStyle style : InstallerStyle.values()) {
            assertFalse(steps(style).isEmpty(), style.name());
        }
    }

    @Test
    void stages_everyInstaller_asksBeforeItWorks() {
        for (final InstallerStyle style : InstallerStyle.values()) {
            final List<InstallerStyle.Stage> stages = style.stages();
            assertTrue(stages.get(0).asks(), style.name());
        }
    }

    @Test
    void stages_framesXp_asksItsQuestionWhileTheCopyRuns() {
        for (final InstallerStyle.Stage stage : InstallerStyle.FRAMES_XP.stages()) {
            if (stage.page() == InstallerPage.NAME) {
                assertEquals(List.of("Collecting information"), stage.steps().stream().map(Text::english).toList());
                return;
            }
        }
        throw new AssertionError("the graphical phase asks for the computer's name");
    }

    @Test
    void stages_framesXp_changesShapePartwayThrough() {
        final List<InstallerStyle.Stage> stages = InstallerStyle.FRAMES_XP.stages();
        assertEquals(InstallerChrome.FULL_TEXT, stages.get(0).chrome());
        assertEquals(InstallerChrome.SIDE_PANEL, stages.get(stages.size() - 1).chrome());
    }

    @Test
    void asks_onlyThePagesThatWantSomethingFromThePlayer() {
        /*
         * Named rather than counted. These used to be read by their place in the list, so adding a page at the
         * front of an installer moved every one of them along and the test began asking the wrong pages.
         */
        assertTrue(asks(InstallerStyle.FRAMES_11, InstallerPage.DISK), "the disk page waits to be answered");
        assertTrue(asks(InstallerStyle.FRAMES_11, InstallerPage.NAME), "so does the one that asks for a name");
        assertTrue(asks(InstallerStyle.FRAMES_11, InstallerPage.WELCOME),
                "and so does the word before it starts, which wants a Next rather than an answer");
        assertFalse(asks(InstallerStyle.FRAMES_11, InstallerPage.COPY), "the work does not wait for anybody");
        assertFalse(asks(InstallerStyle.FRAMES_11, InstallerPage.DONE),
                "and neither does the page that says it is done");
    }

    /** Whether that installer's page waits for the player, found by the page itself and not by its place. */
    private static boolean asks(final InstallerStyle style, final InstallerPage page) {
        for (final InstallerStyle.Stage stage : style.stages()) {
            if (stage.page() == page) {
                return stage.asks();
            }
        }
        throw new IllegalArgumentException(style + " has no " + page + " page");
    }

    @Test
    void offersDesktop_onlyTheDistributionsServedByAMirror() {
        assertTrue(InstallerStyle.UBUNTU.offersDesktop());
        assertTrue(InstallerStyle.DEBIAN.offersDesktop());
        assertTrue(InstallerStyle.FEDORA.offersDesktop());
        assertFalse(InstallerStyle.MC_DOS.offersDesktop());
        assertFalse(InstallerStyle.FRAMES_11.offersDesktop());
        assertFalse(InstallerStyle.PLAIN.offersDesktop());
    }

    @Test
    void gathersQuestions_onlyTheInstallerThatListsThem() {
        for (final InstallerStyle style : InstallerStyle.values()) {
            assertEquals(style == InstallerStyle.FEDORA, style.gathersQuestions(), style.name());
        }
    }

    @Test
    void gathersQuestions_thatInstaller_opensOnItsList() {
        assertEquals(InstallerPage.HUB, InstallerStyle.FEDORA.stages().get(0).page());
    }

    @Test
    void heading_isEachInstallersOwnWords() {
        assertEquals("Partition disks", InstallerStyle.DEBIAN.heading(InstallerPage.DISK, "Debian").english());
        assertEquals("Guided storage configuration",
                InstallerStyle.UBUNTU.heading(InstallerPage.DISK, "Ubuntu").english());
        assertEquals("Installation Destination",
                InstallerStyle.FEDORA.heading(InstallerPage.DISK, "Fedora").english());
        assertEquals("Where do you want to install Frames 11?",
                InstallerStyle.FRAMES_11.heading(InstallerPage.DISK, "Frames 11").english());
        assertEquals("Choose a Disk", InstallerStyle.FRAMES_95.heading(InstallerPage.DISK, "Frames 95").english());
    }

    @Test
    void title_namesTheSystemBeingInstalled() {
        assertEquals("MC-DOS Setup", InstallerStyle.MC_DOS.title("MC-DOS").english());
        assertEquals("Debian installer", InstallerStyle.DEBIAN.title("Debian").english());
    }

    @Test
    void hint_theTextInstallers_sayWhichKeysWork() {
        assertEquals("ENTER=Continue  F3=Exit", InstallerStyle.MC_DOS.hint(InstallerPage.WELCOME).english());
        assertEquals("ENTER=Restart", InstallerStyle.MC_DOS.hint(InstallerPage.DONE).english());
        assertEquals("ENTER=Continue  F3=Exit", InstallerStyle.FRAMES_95.hint(InstallerPage.WELCOME).english());
        assertTrue(InstallerStyle.FRAMES_XP.hint(InstallerPage.DISK).english().contains("E=Erase disk"));
        assertTrue(InstallerStyle.FEDORA.hint(InstallerPage.HUB).english().contains("'b' to begin installation"));
        assertEquals("", InstallerStyle.FRAMES_11.hint(InstallerPage.DISK).english());
    }

    private static List<String> steps(final InstallerStyle style) {
        final List<String> all = new ArrayList<>();
        for (final InstallerStyle.Stage stage : style.stages()) {
            for (final Text step : stage.steps()) {
                all.add(step.english());
            }
        }
        return all;
    }
}
