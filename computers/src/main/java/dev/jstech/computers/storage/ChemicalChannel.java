/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import dev.jstech.core.chemical.IChemicalBridge;
import dev.jstech.core.chemical.IChemicalPort;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/**
 * A block face's chemical port (from an {@link IChemicalBridge}), as a data channel of one kind; amounts are
 * millibuckets. As {@link StorageKey.Kind#CHEMICAL} it moves the chemicals J's Core has no state for; as a kind of
 * matter it moves the chemicals J's Core links to one of its materials in that state (a chemical mod's gases and
 * slurries), which the network keeps as that material.
 *
 * @param chemicals the port
 * @param kind      the kind of key it moves
 */
public record ChemicalChannel(IChemicalPort chemicals, StorageKey.Kind kind) implements IDataChannel {

    @Override
    public long insert(final StorageKey key, final long amount, final boolean simulate) {
        final ResourceLocation chemical = chemicalOf(key);
        return amount <= 0L || chemical == null ? 0L : chemicals.fill(chemical, amount, simulate);
    }

    @Override
    public long extract(final StorageKey key, final long amount, final boolean simulate) {
        final ResourceLocation chemical = chemicalOf(key);
        return amount <= 0L || chemical == null ? 0L : chemicals.drain(chemical, amount, simulate);
    }

    @Override
    public long count(final StorageKey key) {
        final ResourceLocation chemical = chemicalOf(key);
        return chemical == null ? 0L : chemicals.count(chemical);
    }

    @Override
    public List<StorageKey> available() {
        final List<StorageKey> keys = new ArrayList<>();
        for (final ResourceLocation chemical : chemicals.available()) {
            final StorageKey key = StorageKey.chemical(chemical);
            if (key.kind() == kind) {
                keys.add(key);
            }
        }
        return keys;
    }

    /* The chemical a key of this channel's kind is known by in the chemical mod; null for any other key. */
    private ResourceLocation chemicalOf(final StorageKey key) {
        return key.kind() == kind ? key.chemicalId() : null;
    }
}
