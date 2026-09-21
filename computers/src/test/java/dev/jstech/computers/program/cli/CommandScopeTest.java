/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandScopeTest {

    @Test
    void nowhere_isWhatACommandThatSaysNothingGets() {
        for (final Platform platform : Platform.values()) {
            assertFalse(CommandScope.NOWHERE.onSystem(platform, 0), platform.name());
        }
    }

    @Test
    void everywhere_coversEverySystemThereIs() {
        final CommandScope scope = CommandScope.everywhere();
        for (final Platform platform : Platform.values()) {
            assertTrue(scope.onSystem(platform, 0), platform.name());
        }
        assertEquals(CommandScope.EVERY_SYSTEM.size(), scope.platforms().size());
    }

    @Test
    void on_keepsOnlyTheFamiliesNamed() {
        final CommandScope unix = CommandScope.on(CommandScope.UNIX_SYSTEMS);
        assertTrue(unix.onSystem(Platform.UNIX, 0));
        assertTrue(unix.onSystem(Platform.FREEBSD, 0));
        assertTrue(unix.onSystem(Platform.LINUX, 0));
        assertFalse(unix.onSystem(Platform.MC_DOS, 0));
        assertFalse(unix.onSystem(Platform.FRAMES, 0));
    }

    @Test
    void fromEdition_keepsTheOlderEditionsOut() {
        final CommandScope xpAndNewer = CommandScope.on(Platform.FRAMES).fromEdition(2);
        assertFalse(xpAndNewer.onSystem(Platform.FRAMES, 1));
        assertTrue(xpAndNewer.onSystem(Platform.FRAMES, 2));
        assertTrue(xpAndNewer.onSystem(Platform.FRAMES, 3));
        // A system that puts itself nowhere in its family's order is not held back by an edition.
        assertTrue(xpAndNewer.onSystem(Platform.FRAMES, 0));
    }

    @Test
    void needing_saysWhatTheMachineMustHave() {
        final CommandScope scope = CommandScope.everywhere().needing(CommandScope.Need.FILES,
                CommandScope.Need.NETWORK);
        assertTrue(scope.needs(CommandScope.Need.FILES));
        assertTrue(scope.needs(CommandScope.Need.NETWORK));
        assertFalse(scope.needs(CommandScope.Need.PORTS));
    }

    @Test
    void onHost_forManager_andFromPackage_keepEverythingElse() {
        final CommandScope scope = CommandScope.on(Platform.LINUX).fromEdition(2)
                .needing(CommandScope.Need.NETWORK).onHost(HostScope.MAINFRAME)
                .forManager(PackageManagerKind.APT).fromPackage("jsc:scc");
        assertEquals(HostScope.MAINFRAME, scope.hostScope());
        assertEquals(PackageManagerKind.APT, scope.manager());
        assertTrue(scope.fromAPackage());
        assertEquals("jsc:scc", scope.packageId());
        assertEquals(2, scope.minOsRank());
        assertTrue(scope.needs(CommandScope.Need.NETWORK));
        assertTrue(scope.onSystem(Platform.LINUX, 2));
    }

    @Test
    void aScopeCannotBeChangedAfterItIsMade() {
        final CommandScope scope = CommandScope.on(Platform.UNIX).needing(CommandScope.Need.FILES);
        assertTrue(scope.platforms().getClass().getName().contains("Immutable")
                || isUnmodifiable(scope));
    }

    private static boolean isUnmodifiable(final CommandScope scope) {
        try {
            scope.platforms().add(Platform.LINUX);
            return false;
        } catch (final UnsupportedOperationException expected) {
            return true;
        }
    }
}
