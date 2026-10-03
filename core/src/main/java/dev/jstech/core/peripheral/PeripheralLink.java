/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.BlockEntityFields;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * The link from a peripheral to the owner at the other end of its cable, declared as one of its block entity's
 * fields: the owner's position, saved and sent to the players who see the peripheral, so a lamp that shows the link
 * is right on every side. Once a tick it finds an owner to link to, or drops a link that no longer holds.
 */
public final class PeripheralLink {

    private final ValueField<Long> owner;
    private final PeripheralCableType type;
    private final ILinkWorld world;

    public PeripheralLink(final BlockEntityFields fields, final PeripheralCableType type, final ILinkWorld world) {
        this.owner = fields.nullable("LinkedOwner", Codec.LONG).save().toClient();
        this.type = type;
        this.world = world;
        // A peripheral broken or replaced frees its place on its owner for another.
        fields.whenBroken(this::unlink);
    }

    public PeripheralCableType cableType() {
        return type;
    }

    /** The owner's position, packed, while linked. */
    public Optional<Long> linkedOwner() {
        return Optional.ofNullable(owner.get());
    }

    /** The owner's position while linked, or null. */
    public @Nullable BlockPos ownerPos() {
        final Long at = owner.get();
        return at == null ? null : BlockPos.of(at);
    }

    /** The validator linked this peripheral to the owner at {@code ownerPos}. */
    public void linked(final long ownerPos) {
        owner.set(ownerPos);
    }

    /** The link is gone, from either side. */
    public void unlinked() {
        owner.set(null);
    }

    /**
     * Once a tick on the server: links to an owner the cables reach, or drops a link whose owner or cable is gone, or
     * whose port the owner no longer has (its card taken out), to wait for a free one.
     */
    public void tick(final ServerLevel level, final BlockPos self) {
        final long here = self.asLong();
        final Long at = owner.get();
        if (at == null) {
            final OptionalLong found = world.discoverOwner(level, here);
            if (found.isPresent()) {
                world.validator(level).tryEstablishLink(found.getAsLong(), here);
            }
            return;
        }
        final BlockPos ownerAt = BlockPos.of(at);
        if (!level.isLoaded(ownerAt)) {
            // An owner whose chunk is away cannot be asked; the link stands until it can be, and nothing is loaded.
            return;
        }
        final boolean holds = level.getBlockEntity(ownerAt) instanceof IPeripheralOwner linkedTo
                && linkedTo.holdsPort(here);
        if (!holds || !world.validator(level).isLinkStillValid(at, here, type)) {
            unlink(level, self);
        }
    }

    /** Breaks the link from this side, freeing the owner's place for another peripheral; nothing when unlinked. */
    public void unlink(final ServerLevel level, final BlockPos self) {
        final Long at = owner.get();
        if (at != null && Loaded.blockEntity(level, BlockPos.of(at)) instanceof IPeripheralOwner linkedTo) {
            linkedTo.onEndpointUnlinked(self.asLong());
        }
        unlinked();
    }

    /** How the links of one kind of cable are found in a world; the mod that lays the cables says. */
    public interface ILinkWorld {

        /** The owner the cables reach from the peripheral at {@code endpoint}, if any. */
        OptionalLong discoverOwner(ServerLevel level, long endpoint);

        /** The validator that establishes and checks links in {@code level}. */
        PeripheralLinkValidator validator(ServerLevel level);
    }
}
