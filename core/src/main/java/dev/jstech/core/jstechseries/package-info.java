/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
/**
 * The J's Tech Series' own model: what the mods of the series must agree on, so each thing has one identity whichever
 * of them is installed. The hardware eras and the industrial tiers, the data network's model, the Operations and
 * their kinds, the links between a computer and its devices, J's Energy, the series' catalogue of elements, fluids
 * and components, the content the mods share (the basic lines, the battery, the wrench, the converters and the rest),
 * the {@code /jstech} command and the series' look all belong here.
 *
 * <p>Everything else in the Core is the platform: generic kits for any technology mod, which never reach into this
 * package. A mod outside the series builds on the platform without taking the series' model with it, and everything
 * here is made only with the platform's kits, so anyone could build it again using the Core alone.
 *
 * <p>Three questions sort a part. Would a technology mod outside the series use it without knowing J's Computers or
 * J's Industrial? It is the platform's. Does it exist because the series' mods must agree on it? It is here. Is it
 * the concrete work of one mod? Then it is that mod's, not the Core's.
 *
 * <p>What is new of the series is born here. What the Core already had of it moves here when the series' mods are
 * moved onto the Core, a domain at a time.
 */
package dev.jstech.core.jstechseries;
