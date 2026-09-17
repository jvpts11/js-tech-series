/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.tests.JsTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Which system is newer than which is something the systems say, not something the registry knows.
 *
 * <p>It used to be three names written into the registry, so the only systems that could ever be put in order
 * were the three the mod shipped. A fourth edition, or an addon's family, had no way to say it was newer than
 * anything, which meant a program could never ask for it. Each system declares its own place now, and this is
 * what says the registry reads that place and nothing else.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OsFamilyOrderGameTests {

    private static final String ARENA = "empty";

    private OsFamilyOrderGameTests() {
    }

    @GameTest(template = ARENA)
    public static void familyOrder_isWhatTheSystemsThemselvesDeclare(final GameTestHelper helper) {
        for (final OsDef os : OsRegistry.oses()) {
            helper.assertTrue(OsRegistry.osVersionRank(os.id()) == os.familyRank(),
                    os.displayName() + " is placed at " + os.familyRank()
                            + " but the registry reads it as " + OsRegistry.osVersionRank(os.id()));
        }
        helper.succeed();
    }

    /* The one family with an order to it runs oldest to newest, and no two of them share a place. */
    @GameTest(template = ARENA)
    public static void familyOrder_putsTheDesktopSystemsInOrder(final GameTestHelper helper) {
        final int ninetyFive = rankOf("frames_95");
        final int xp = rankOf("frames_xp");
        final int eleven = rankOf("frames_11");
        helper.assertTrue(ninetyFive > 0 && ninetyFive < xp && xp < eleven,
                "the desktop systems read as " + ninetyFive + ", " + xp + " and " + eleven);
        helper.succeed();
    }

    /*
     * A family with no order says so with a zero, and that zero is what tells a program to stop asking: a
     * system there is neither newer nor older than the others, so the platform alone decides.
     */
    @GameTest(template = ARENA)
    public static void familyOrder_isEmptyForAFamilyWithNoOrder(final GameTestHelper helper) {
        int unordered = 0;
        for (final OsDef os : OsRegistry.oses()) {
            if (os.platform() == Platform.LINUX) {
                helper.assertTrue(os.familyRank() == 0,
                        os.displayName() + " claims a place in an order its family does not have");
                unordered++;
            }
        }
        helper.assertTrue(unordered > 0, "no system of that family is registered at all");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void familyOrder_readsAsNothingForASystemNobodyRegistered(final GameTestHelper helper) {
        helper.assertTrue(OsRegistry.osVersionRank(null) == 0, "no system at all read as a place in an order");
        helper.assertTrue(rankOf("not_a_system") == 0, "a system nobody registered read as a place in an order");
        helper.succeed();
    }

    /* The name a message shows for a place comes from the system at it, so nothing spells the names out. */
    @GameTest(template = ARENA)
    public static void systemOfRank_namesTheSystemAtThatPlace(final GameTestHelper helper) {
        helper.assertTrue("Frames XP".equals(OsRegistry.systemOfRank(rankOf("frames_xp"))),
                "the second desktop system read as " + OsRegistry.systemOfRank(rankOf("frames_xp")));
        helper.assertTrue("a newer system".equals(OsRegistry.systemOfRank(9999)),
                "a place past the end of every family named a system anyway");
        helper.succeed();
    }

    private static int rankOf(final String path) {
        return OsRegistry.osVersionRank(ResourceLocation.fromNamespaceAndPath("jsc", path));
    }
}
