/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.team.IOwned;
import dev.jstech.core.team.Ownership;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity of a {@link RadioBlock}: its tuning, its owner, and what its role keeps (a transmitter's value and
 * clash, a receiver's signal, a relay's partner). One type serves all of a mod's radios; each takes its role and power
 * from its block. It is on the air while it is loaded.
 */
public class RadioBlockEntity extends SyncedBlockEntity implements IRadio, IOwned {

    private final ValueField<Tuning> tuning = fields().value("tuning", Tuning.CODEC, Tuning.DEFAULT).save().toClient();
    private final ValueField<Ownership> ownership = fields().nullable("ownership", Ownership.CODEC).save().toClient();
    private final IntField sending = fields().integer("sending", 0).save().toClient();
    private final BoolField clashing = fields().flag("clashing", false).save().toClient();
    private final SignalLevel output = SignalLevel.in(fields(), "signal");
    private final ValueField<GlobalPos> partner = fields().nullable("partner", GlobalPos.CODEC).save();

    public RadioBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            SignalRadio.join(server, worldPosition, this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server) {
            SignalRadio.leave(server, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel server) {
            SignalRadio.leave(server, worldPosition);
        }
    }

    @Override
    public Role role() {
        return getBlockState().getBlock() instanceof RadioBlock radio ? radio.role() : Role.RECEIVER;
    }

    @Override
    public Tuning tuning() {
        return tuning.get();
    }

    /** Tunes it to {@code to}, on the air from the end of the tick. */
    public void tune(final Tuning to) {
        tuning.set(to);
        retune();
    }

    @Override
    @Nullable
    public Ownership ownership() {
        return ownership.get();
    }

    /** Hands it to {@code to}, or to nobody for none: a private or team frequency is its owner's. */
    public void own(@Nullable final Ownership to) {
        ownership.set(to);
        retune();
    }

    @Override
    @Nullable
    public UUID owner() {
        final Ownership owned = ownership.get();
        return owned == null ? null : owned.owner();
    }

    @Override
    public double power() {
        return getBlockState().getBlock() instanceof RadioBlock radio ? radio.power() : 0;
    }

    @Override
    public int value() {
        return sending.get();
    }

    /** A transmitter sends {@code value}, 0 to 15, from the end of the tick; its receivers hear only of a change. */
    public void send(final int value) {
        final int held = Math.max(0, Math.min(Reading.FULL, value));
        if (held != sending.get()) {
            sending.set(held);
            if (level instanceof ServerLevel server) {
                SignalRadio.sent(server, worldPosition);
            }
        }
    }

    /** Whether a transmitter clashes with another on its frequency. */
    public boolean clashing() {
        return clashing.get();
    }

    @Override
    public void clash(final boolean clash) {
        clashing.set(clash);
    }

    /** The redstone a receiver gives out, 0 to 15. */
    public int signal() {
        return output.strength();
    }

    @Override
    public void hear(final Reception reception) {
        if (level != null) {
            output.set(level, worldPosition, reception.value());
        }
    }

    @Override
    @Nullable
    public GlobalPos partner() {
        return partner.get();
    }

    /** Links a relay to the relay at {@code to}, which sends again all it hears, or unlinks it for none. */
    public void link(@Nullable final GlobalPos to) {
        partner.set(to);
        retune();
    }

    private void retune() {
        if (level instanceof ServerLevel server) {
            SignalRadio.retune(server, worldPosition);
        }
    }
}
