/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * The world's Teracoin chain, saved as the world's state: one for the whole server, whatever dimension its miners and
 * wallets are in, so it lives with the overworld's data. Beside it, who made each wallet, which the chain itself never
 * knows, and since when a wallet has held what the advancements ask it to hold.
 */
public final class TeracoinState extends SavedData {

    private final TeracoinChain chain = new TeracoinChain();
    /* Who made each wallet, by its address. */
    private final Map<String, UUID> owners = new HashMap<>();
    /* Since when, in game ticks, each wallet has held enough without sending any. */
    private final Map<String, Long> heldSince = new HashMap<>();

    private static final String FILE = "jsc_teracoin";

    public static TeracoinState of(final MinecraftServer server) {
        final TeracoinState state = server.overworld().getDataStorage().computeIfAbsent(factory(), FILE);
        state.chain.useRules(TeracoinSettings.rules());
        return state;
    }

    public static SavedData.Factory<TeracoinState> factory() {
        return new SavedData.Factory<>(TeracoinState::new, TeracoinState::load);
    }

    /** The chain. Whoever changes it marks it changed ({@link #setDirty()}), so it is saved. */
    public TeracoinChain chain() {
        return chain;
    }

    /** Who made the wallet of {@code address}, or null when nobody is known to. */
    @Nullable
    public UUID ownerOf(final String address) {
        return owners.get(address);
    }

    /** Every wallet whose maker is known, by its address. */
    public Map<String, UUID> owners() {
        return Collections.unmodifiableMap(owners);
    }

    /** Notes that {@code player} made the wallet of {@code address}. */
    public void own(final String address, final UUID player) {
        if (!player.equals(owners.put(address, player))) {
            setDirty();
        }
    }

    /** Since when the wallet of {@code address} has held enough without sending any, or null. */
    @Nullable
    public Long heldSince(final String address) {
        return heldSince.get(address);
    }

    /** Sets since when the wallet of {@code address} has held enough, or forgets it for null. */
    public void holdSince(final String address, @Nullable final Long tick) {
        final Long was = tick == null ? heldSince.remove(address) : heldSince.put(address, tick);
        if (!Objects.equals(was, tick)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put("Chain", TeracoinNbt.save(chain));
        final ListTag made = new ListTag();
        for (final Map.Entry<String, UUID> owner : owners.entrySet()) {
            final CompoundTag one = new CompoundTag();
            one.putString("Address", owner.getKey());
            one.putUUID("Player", owner.getValue());
            final Long since = heldSince.get(owner.getKey());
            if (since != null) {
                one.putLong("HeldSince", since);
            }
            made.add(one);
        }
        tag.put("Owners", made);
        return tag;
    }

    private static TeracoinState load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final TeracoinState state = new TeracoinState();
        TeracoinNbt.load(state.chain, tag.getCompound("Chain"));
        for (final Tag element : tag.getList("Owners", Tag.TAG_COMPOUND)) {
            final CompoundTag one = (CompoundTag) element;
            if (one.hasUUID("Player")) {
                state.owners.put(one.getString("Address"), one.getUUID("Player"));
                if (one.contains("HeldSince")) {
                    state.heldSince.put(one.getString("Address"), one.getLong("HeldSince"));
                }
            }
        }
        return state;
    }
}
