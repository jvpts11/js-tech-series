/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.protection;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.team.CoreTeams;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * The player a machine stands in as when the game asks who is acting (who breaks a block, who places one): never one
 * generic fake player shared by every machine, which every protection would let through or stop alike, but a stand-in
 * for the machine's owner, under the owner's id and the name the server knew them by, so a protection that asks by
 * either finds the owner. The Core's own question, {@link Protection#may}, weighs the owner directly, online or not.
 */
public final class MachineActors {

    private MachineActors() {
    }

    /** The stand-in for {@code owner} in {@code level}. */
    public static FakePlayer standIn(final ServerLevel level, final UUID owner) {
        return standIn(level, owner, CoreTeams.nameOf(level.getServer(), owner).orElse(owner.toString()));
    }

    /** The stand-in for a thing owned by nobody, under its own id and a name of its own. */
    public static FakePlayer standIn(final ServerLevel level, final UUID id, final String name) {
        return FakePlayerFactory.get(level, new GameProfile(id, name));
    }
}
