/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.os.DiskSystems;
import dev.jstech.computers.os.boot.SystemWelcome;
import dev.jstech.tests.JsTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * A disk carrying several systems.
 *
 * <p>It used to carry one, written straight onto it, so installing a second wrote over the first and nothing
 * anywhere said what had been lost. These hold the new shape to what a disk with several systems has to do,
 * including the part that made the greeting move: each system on a disk remembers having been met on its own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DiskSystemsGameTests {

    private DiskSystemsGameTests() {
    }

    private static final String ARENA = "empty";

    private static final ResourceLocation XP = ResourceLocation.fromNamespaceAndPath("jsc", "frames_xp");
    private static final ResourceLocation NINETY_FIVE = ResourceLocation.fromNamespaceAndPath("jsc", "frames_95");
    private static final ResourceLocation UBUNTU = ResourceLocation.fromNamespaceAndPath("jsc", "ubuntu");

    @GameTest(template = ARENA)
    public static void with_installsBesideWhatIsThereRatherThanOverIt(final GameTestHelper helper) {
        final DiskSystems disk = DiskSystems.of(NINETY_FIVE).with(XP);
        helper.assertTrue(disk.ids().equals(List.of(NINETY_FIVE, XP)),
                "both are on the disk, in the order they were installed; got " + disk.ids());
        helper.assertTrue(XP.equals(disk.boots()), "and the one just installed is what boots");
        helper.succeed();
    }

    /** As many as a player installs, each of them kept, which is the whole point of the change. */
    @GameTest(template = ARENA)
    public static void with_takesMoreThanTwo(final GameTestHelper helper) {
        final DiskSystems disk = DiskSystems.of(NINETY_FIVE).with(XP).with(UBUNTU);
        helper.assertTrue(disk.count() == 3, "three systems on one disk; got " + disk.count());
        helper.assertTrue(disk.ids().equals(List.of(NINETY_FIVE, XP, UBUNTU)), "in order; got " + disk.ids());
        helper.assertTrue(UBUNTU.equals(disk.boots()), "the newest one boots");
        helper.succeed();
    }

    /** Installing what is already on the disk is not a first meeting all over again. */
    @GameTest(template = ARENA)
    public static void with_aSystemAlreadyThere_keepsItsMarkAndJustBootsIt(final GameTestHelper helper) {
        final DiskSystems met = DiskSystems.of(NINETY_FIVE).with(XP)
                .remembering(NINETY_FIVE, SystemWelcome.UNSEEN.met());
        final DiskSystems again = met.with(NINETY_FIVE);
        helper.assertTrue(again.count() == 2, "nothing is added; got " + again.count());
        helper.assertTrue(NINETY_FIVE.equals(again.boots()), "but it is what boots now");
        helper.assertTrue(again.welcomeOf(NINETY_FIVE).seen(), "and it is still a system this disk has met");
        helper.succeed();
    }

    /**
     * The mark is per system, not per disk. A disk that has met one system has not met the one installed beside
     * it: that is why the greeting moved onto the systems rather than staying on the disk.
     */
    @GameTest(template = ARENA)
    public static void welcomeOf_isRememberedForEachSystemOnItsOwn(final GameTestHelper helper) {
        final DiskSystems disk = DiskSystems.of(NINETY_FIVE).with(XP)
                .remembering(NINETY_FIVE, SystemWelcome.UNSEEN.met());
        helper.assertTrue(disk.welcomeOf(NINETY_FIVE).seen(), "the one that has come up has been met");
        helper.assertFalse(disk.welcomeOf(XP).seen(), "the one installed beside it has met nobody");
        helper.assertFalse(disk.welcomeOf(UBUNTU).seen(), "and one not on the disk at all has met nobody");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void booting_choosesBetweenWhatIsThereAndIgnoresWhatIsNot(final GameTestHelper helper) {
        final DiskSystems disk = DiskSystems.of(NINETY_FIVE).with(XP);
        helper.assertTrue(NINETY_FIVE.equals(disk.booting(NINETY_FIVE).boots()), "the older one boots");
        helper.assertTrue(XP.equals(disk.booting(UBUNTU).boots()),
                "a system the disk does not carry changes nothing");
        helper.succeed();
    }

    /** Taking one away leaves the one that was booting still booting, rather than sliding onto its neighbour. */
    @GameTest(template = ARENA)
    public static void without_leavesTheOneThatWasBootingChosen(final GameTestHelper helper) {
        final DiskSystems disk = DiskSystems.of(NINETY_FIVE).with(XP).with(UBUNTU).booting(XP);
        helper.assertTrue(XP.equals(disk.without(UBUNTU).boots()), "one after it went");
        helper.assertTrue(XP.equals(disk.without(NINETY_FIVE).boots()), "one before it went");
        final DiskSystems gone = disk.without(XP);
        helper.assertTrue(gone.boots() != null && gone.has(gone.boots()),
                "and taking away the one that was booting leaves something real booting; got " + gone.boots());
        helper.succeed();
    }

    /** A disk nobody has installed anything onto answers every question without falling over. */
    @GameTest(template = ARENA)
    public static void none_answersEverythingAsADiskCarryingNothing(final GameTestHelper helper) {
        helper.assertTrue(DiskSystems.NONE.isEmpty(), "it carries nothing");
        helper.assertTrue(DiskSystems.NONE.boots() == null, "a disk carrying nothing boots nothing");
        helper.assertFalse(DiskSystems.NONE.has(XP), "and carries no system anybody asks about");
        helper.assertTrue(DiskSystems.of(NINETY_FIVE).without(NINETY_FIVE).isEmpty(),
                "taking the last one away leaves a disk carrying nothing");
        helper.succeed();
    }

    /** Whatever it is handed, what a disk boots is always something it really carries. */
    @GameTest(template = ARENA)
    public static void bootIndex_isAlwaysOneOfTheSystemsItCarries(final GameTestHelper helper) {
        final List<DiskSystems.Entry> one = List.of(new DiskSystems.Entry(XP, SystemWelcome.UNSEEN));
        helper.assertTrue(new DiskSystems(one, 9).bootIndex() == 0, "past the end");
        helper.assertTrue(new DiskSystems(one, -3).bootIndex() == 0, "before the start");
        helper.assertTrue(new DiskSystems(List.of(), 5).bootIndex() == 0, "on a disk carrying nothing");
        helper.succeed();
    }

    /** A list past what a disk could hold has an end to it, since it crosses the wire. */
    @GameTest(template = ARENA)
    public static void installed_hasAnEndToIt(final GameTestHelper helper) {
        DiskSystems disk = DiskSystems.NONE;
        for (int at = 0; at < DiskSystems.MOST_SYSTEMS + 5; at++) {
            disk = disk.with(ResourceLocation.fromNamespaceAndPath("jsc", "system_" + at));
        }
        helper.assertTrue(disk.count() == DiskSystems.MOST_SYSTEMS,
                "cut to what a packet can carry; got " + disk.count());
        helper.succeed();
    }
}
