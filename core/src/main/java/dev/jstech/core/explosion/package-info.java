/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Explosions with real physics for any mod, decided by their energy and not by a radius: the shock wave leaves at the
 * speed of sound of the medium, pushes through blocks by the strength of each, and hurts and throws the bodies it
 * reaches by the overpressure there; a casing breaks into fragments, broken blocks fly as debris, explosives it breaks
 * go off, and the ground round the crater is scorched. A vacuum carries no wave; water carries it far.
 *
 * <p>A mod sets one off from {@link Explosions}, with {@code Explosions.blast(level, centre, explosive, joules)}, and
 * says more on the {@link Blast} it is handed: who set it off, its casing, whether it sets fires, the medium it goes
 * out through ({@link BlastMedium}) and the most it may do to the world ({@link BlastConsequence}); then
 * {@code detonate()}. The {@link Detonation} that comes back says how the blast goes: its front, its reach, the
 * blocks it broke, whether it has finished. The blocks it breaks are broken a share each tick, within the server's
 * budget, so a great blast never stalls the server. What each block stands is a data map, {@link BlastStrength},
 * which a mod or a data pack fills; the figures of real blasts are in {@link BlastMath}, free of the game; and what
 * each mod's explosions do to the world, and how many blocks a level's blasts look at and break each tick, is the
 * server's ({@link BlastBalance}). To the rest of the game a blast is the game's own explosion
 * ({@link BlastExplosion}), so other mods and claimed land take part through its events.
 *
 * <p>How to use it, from zero, is in {@code core/docs/EXPLOSIONS_AND_FIRE.md}. An example that compiles is the Core's
 * game tests of blasts ({@code tests/src/main/java/dev/jstech/tests/gametest/core/ExplosionGameTests.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.explosion;

import org.jetbrains.annotations.ApiStatus;
