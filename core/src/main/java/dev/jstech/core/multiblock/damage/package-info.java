/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
/**
 * Damage to formed models: holes, petals, craters, burns and dents laid on a formed structure's model where it was
 * struck, on the ground and aboard a moving structure alike, re-shaping the blocks they reach.
 *
 * <p>A kind of damage is a stamp ({@link DamageStamp}), made once as geometry: the hole it cuts, how rough its rim is,
 * the field that pushes the faces round it and the pixels its edge is drawn with. The Core declares six
 * ({@link DamageStamps}); a mod declares more. A strike ({@link FormedDamage#strike}) lays a stamp where it struck,
 * turned by the surface, at the damage's size and by a seed, and every block of the model it reaches keeps the record
 * of it ({@link CellMark}), saved with the block, carried with it aboard and sent to the players. The model's faces at
 * rest never change: from the records, the server and every player's game cut the faces round each strike finer, push
 * the hull's faces by the stamp's field and cut away those within its rim ({@link MeshDamage}), only the hull and
 * never what stands behind it; each reached block then collides, is aimed at and is outlined by its faces as the
 * strikes left them, worked out by the same code that shapes a formed block in the first place, and the player's game
 * draws the model so, with the pixels of each strike's edge over it ({@link DamageLook}). Taking the records away
 * ({@link FormedDamage#erase}) undoes all of it.
 *
 * <p>How to use it is in {@code core/docs/MULTIBLOCKS.md}, under "Damaged where struck". A working example is the
 * game test of the test mod's turbine ({@code tests/src/main/java/dev/jstech/tests/gametest/core/
 * FormedDamageGameTests.java}) and the test of what a player sees
 * ({@code tests/src/main/java/dev/jstech/tests/clienttest/core/FormedDamageClientTests.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.multiblock.damage;

import org.jetbrains.annotations.ApiStatus;
