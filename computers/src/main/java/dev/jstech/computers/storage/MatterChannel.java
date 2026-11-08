/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import dev.jstech.core.material.Material;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A block face's store of one state of matter of J's Core (a gas tank, a pressure vessel, a containment cell), as a
 * data channel of that kind; amounts are millibuckets (computers/docs/design/storage.md).
 *
 * @param handler the store
 * @param kind    the kind of key it moves, which is the store's state
 */
public record MatterChannel(IMatterHandler handler, StorageKey.Kind kind) implements IDataChannel {

    @Override
    public long insert(final StorageKey key, final long amount, final boolean simulate) {
        final Optional<Material> material = materialOf(key);
        return amount <= 0L || material.isEmpty() ? 0L
                : handler.insert(new MatterStack(material.get(), amount), simulate);
    }

    @Override
    public long extract(final StorageKey key, final long amount, final boolean simulate) {
        final Optional<Material> material = materialOf(key);
        return amount <= 0L || material.isEmpty() ? 0L : handler.extract(material.get(), amount, simulate);
    }

    @Override
    public long count(final StorageKey key) {
        final Optional<Material> material = materialOf(key);
        long held = 0L;
        for (int tank = 0; material.isPresent() && tank < handler.tanks(); tank++) {
            final Optional<MatterStack> stack = handler.stack(tank);
            if (stack.isPresent() && stack.get().material().equals(material.get())) {
                held += stack.get().amount();
            }
        }
        return held;
    }

    @Override
    public List<StorageKey> available() {
        final List<StorageKey> keys = new ArrayList<>();
        for (int tank = 0; tank < handler.tanks(); tank++) {
            handler.stack(tank).map(stack -> StorageKey.matter(handler.state(), stack.material()))
                    .filter(key -> !keys.contains(key)).ifPresent(keys::add);
        }
        return keys;
    }

    @Override
    public int places() {
        return handler.tanks();
    }

    @Override
    public int placesUsed() {
        int used = 0;
        for (int tank = 0; tank < handler.tanks(); tank++) {
            if (handler.stack(tank).isPresent()) {
                used++;
            }
        }
        return used;
    }

    @Override
    public long room() {
        long room = 0L;
        for (int tank = 0; tank < handler.tanks(); tank++) {
            final long held = handler.stack(tank).map(MatterStack::amount).orElse(0L);
            room += Math.max(0L, handler.capacity(tank) - held);
        }
        return room;
    }

    /* The material a key of this channel's kind holds; none for any other key. */
    private Optional<Material> materialOf(final StorageKey key) {
        return key.kind() == kind ? key.material() : Optional.empty();
    }
}
