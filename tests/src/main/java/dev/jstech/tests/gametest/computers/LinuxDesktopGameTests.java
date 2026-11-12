/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsBootstrap;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the Linux desktops ship (computers/docs/design/operating-systems.md): the programs a desktop names as its own
 * are the ones it bundles, so each has a launcher where the real desktop keeps it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LinuxDesktopGameTests {

    private static final String ARENA = "empty";
    private static final List<String> LINUX_DESKTOPS = List.of("kde_plasma", "gnome", "cinnamon");
    private static final ResourceLocation DISKS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "disks");

    private LinuxDesktopGameTests() {
    }

    /** Every Linux desktop ships its disk tool, listed in its menu with no icon on the wallpaper, as the real ones. */
    @GameTest(template = ARENA)
    public static void linuxDesktops_shipTheirDiskToolInTheirMenu(final GameTestHelper helper) {
        int found = 0;
        for (final DesktopEnvironmentDef desktop : OsBootstrap.builtinDesktops()) {
            if (!LINUX_DESKTOPS.contains(desktop.id().getPath())) {
                continue;
            }
            found++;
            helper.assertTrue(desktop.bundles(DISKS), desktop.id() + " bundles its disk tool");
            helper.assertTrue(desktop.menuOnly().contains(DISKS), desktop.id() + " keeps it in its menu");
        }
        helper.assertTrue(found == LINUX_DESKTOPS.size(), "every Linux desktop is checked; found " + found);
        helper.succeed();
    }

    /** A program a desktop gives its own name to is one it bundles: the name would otherwise go on nothing. */
    @GameTest(template = ARENA)
    public static void linuxDesktops_bundleWhatTheyName(final GameTestHelper helper) {
        for (final DesktopEnvironmentDef desktop : OsBootstrap.builtinDesktops()) {
            if (!LINUX_DESKTOPS.contains(desktop.id().getPath())) {
                continue;
            }
            for (final ResourceLocation named : desktop.nativeNames().keySet()) {
                helper.assertTrue(desktop.bundles(named), desktop.id() + " bundles " + named + ", which it names");
            }
        }
        helper.succeed();
    }
}
