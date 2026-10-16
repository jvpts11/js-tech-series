/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import com.mojang.serialization.Codec;
import dev.jstech.core.audio.IAudible;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.client.audio.SoundDirector;
import dev.jstech.core.signal.ISignalOutput;
import dev.jstech.core.signal.Reading;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity of a part of the Core's kit ({@link MechanicalPartBlock}): it turns with its network, its speed
 * saved and sent to the players who see it, and meets its neighbours as its block's spec and axis say, and as the
 * redstone it gets says for a clutch, a brake or a speed controller. A shear pin keeps whether it sheared, and a
 * pulley the pulley its belt or chain joins it to; breaking a pulley lets the belt go, dropped, and frees the other.
 * Every part is a sensor of its speed, as a tachometer, and a torque meter of the torque through it too, offered to
 * whatever reads sensors and to a comparator.
 */
public class MechanicalPartBlockEntity extends SyncedBlockEntity implements IMechanicalPart, ISignalOutput,
        IAudible {

    private final ValueField<Double> rpm;
    private final ValueField<Double> torque;
    private final IntField signal;
    private final BoolField sheared;
    private final ValueField<BlockPos> linked;
    private final ValueField<ResourceLocation> linkItem;
    private final BoolField chain;
    private int lastStrength = -1;

    /** What a belt holds back on each of its pulleys, in newton-metres: it slips a little. */
    public static final double BELT_FRICTION = 0.5;
    /** What a chain holds back on each of its sprockets, in newton-metres. */
    public static final double CHAIN_FRICTION = 0.2;
    /** The top of a part's tachometer span, in revolutions a minute, where it gives the full 15. */
    public static final double TACHOMETER_MOST = 3_000.0;
    /** The top of a torque meter's span, in newton-metres. */
    public static final double TORQUE_METER_MOST = 1_000.0;
    /* How far a torque meter's reading moves, in newton-metres, before it is sent again. */
    private static final double TORQUE_STEP = 0.01;
    /* Below this share of its loudest speed a turning part is not heard. */
    private static final double QUIET_SHARE = 0.02;
    /* Its sound's pitch at a crawl, and how much higher it goes at its loudest speed. */
    private static final double LOWEST_PITCH = 0.5;
    private static final double PITCH_SPAN = 1.0;
    /* What a block that is no part of the kit is taken as: a shaft that weighs and holds back nothing. */
    private static final MechanicalPartSpec NOTHING = MechanicalPartSpec.shaft(0.0, 0.0);

    public MechanicalPartBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.rpm = fields().value("Rpm", Codec.DOUBLE, 0.0).save().toClient();
        this.torque = fields().value("Torque", Codec.DOUBLE, 0.0).toClient();
        this.signal = fields().integer("Signal", 0).save();
        this.sheared = fields().flag("Sheared", false).save().toClient();
        this.linked = fields().nullable("Linked", BlockPos.CODEC).save().toClient();
        this.linkItem = fields().nullable("LinkItem", ResourceLocation.CODEC).save();
        this.chain = fields().flag("Chain", false).save().toClient();
        fields().whenLoaded(this::hear);
        fields().whenNeighbourChanges((level, face) -> hear(level));
        fields().whenBroken((level, at) -> letGo(level));
        MechanicalPorts.keep(this);
    }

    /** What the part is, as its block says. */
    public MechanicalPartSpec spec() {
        return getBlockState().getBlock() instanceof MechanicalPartBlock block ? block.spec() : NOTHING;
    }

    /** The redstone it gets, from 0 to 15. */
    public int signal() {
        return this.signal.get();
    }

    /** Whether it is a shear pin that sheared. */
    public boolean sheared() {
        return this.sheared.get();
    }

    /** Pins a shear pin that sheared again, as a player fits a new pin. */
    public void repin() {
        if (this.sheared.get()) {
            this.sheared.set(false);
            LevelMechanics.changed(this.level, this.worldPosition);
        }
    }

    /** The pulley its belt or chain joins it to, or none. */
    public @Nullable BlockPos linked() {
        return this.linked.get();
    }

    /** Whether its belt is a chain. */
    public boolean chained() {
        return this.chain.get();
    }

    /**
     * Joins it to the pulley at {@code other} by a belt, or a chain when {@code isChain}, made of {@code item}, which
     * it drops when the join is broken.
     */
    public void link(final BlockPos other, final boolean isChain, final ResourceLocation item) {
        this.linked.set(other.immutable());
        this.chain.set(isChain);
        this.linkItem.set(item);
        LevelMechanics.changed(this.level, this.worldPosition);
    }

    /** Frees it from its belt or chain, with nothing dropped: the other pulley lets it go. */
    public void unlink() {
        if (this.linked.get() != null) {
            this.linked.set(null);
            this.linkItem.set(null);
            LevelMechanics.changed(this.level, this.worldPosition);
        }
    }

    @Override
    public ShaftContact contact(final Direction face) {
        final BlockState state = getBlockState();
        if (!state.hasProperty(RotatedPillarBlock.AXIS) || this.sheared.get()) {
            return ShaftContact.NONE;
        }
        return spec().contact(state.getValue(RotatedPillarBlock.AXIS), face, this.signal.get());
    }

    @Override
    public List<ShaftJoin> joins() {
        final BlockPos other = this.linked.get();
        if (other == null || this.level == null || !(this.level.getBlockEntity(other)
                instanceof MechanicalPartBlockEntity pulley) || !this.worldPosition.equals(pulley.linked())) {
            return List.of();
        }
        // A belt or a chain moves as fast at both pulleys, so the smaller one turns the faster, the same way.
        return List.of(new ShaftJoin(other, spec().radius() / pulley.spec().radius()));
    }

    @Override
    public double inertia() {
        return spec().inertia();
    }

    @Override
    public double friction() {
        final double own = spec().friction();
        return this.linked.get() == null ? own : own + (this.chain.get() ? CHAIN_FRICTION : BELT_FRICTION);
    }

    @Override
    public @Nullable ITorque torque() {
        final MechanicalPartSpec spec = spec();
        return spec.kind() == MechanicalPartSpec.Kind.BRAKE
                ? own -> -Math.signum(own) * spec.braking(this.signal.get()) : null;
    }

    @Override
    public double rating() {
        return spec().rating();
    }

    @Override
    public void overloaded(final ServerLevel level, final BlockPos pos) {
        if (spec().kind() == MechanicalPartSpec.Kind.SHEAR_PIN) {
            if (!this.sheared.get()) {
                this.sheared.set(true);
                LevelMechanics.changed(level, pos);
            }
        } else if (MechanicalBalance.partsBreak()) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public boolean measures() {
        return spec().kind() == MechanicalPartSpec.Kind.TORQUE_METER;
    }

    @Override
    public void carrying(final double newtonMetres) {
        if (Math.abs(newtonMetres - this.torque.get()) > TORQUE_STEP) {
            this.torque.set(newtonMetres);
            tellComparators();
        }
    }

    /** The torque through it as it last measured it, in newton-metres; none for a part that does not measure. */
    public double torqueThrough() {
        return this.torque.get();
    }

    @Override
    public double rpm() {
        return this.rpm.get();
    }

    @Override
    public double audioX() {
        return this.worldPosition.getX() + 0.5;
    }

    @Override
    public double audioY() {
        return this.worldPosition.getY() + 0.5;
    }

    @Override
    public double audioZ() {
        return this.worldPosition.getZ() + 0.5;
    }

    /** The sound it turns with, if its block has one, higher and louder the faster it turns. */
    @Override
    public List<LoopRequest> loops() {
        if (!(getBlockState().getBlock() instanceof MechanicalPartBlock block) || block.sound() == null) {
            return List.of();
        }
        final double share = Math.abs(this.rpm.get()) / block.fullRpm();
        if (share < QUIET_SHARE) {
            return List.of();
        }
        final float volume = (float) Math.min(1.0, share);
        final float pitch = (float) (LOWEST_PITCH + PITCH_SPAN * Math.min(1.0, share));
        return List.of(new LoopRequest(block.sound(), volume, pitch, null));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide()) {
            SoundDirector.track(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && this.level.isClientSide()) {
            SoundDirector.untrack(this);
        }
    }

    @Override
    public void turning(final double rpm) {
        this.rpm.set(rpm);
        tellComparators();
    }

    /** What a sensor reads of it: its speed, as a tachometer does, and the torque through a torque meter. */
    @Override
    public List<Reading> readings() {
        final Reading speed = MechanicalReadings.tachometer(rpm(), TACHOMETER_MOST);
        return measures() ? List.of(speed, MechanicalReadings.torqueMeter(torqueThrough(), TORQUE_METER_MOST))
                : List.of(speed);
    }

    /* Tells the comparators beside it when the signal it gives changed. */
    private void tellComparators() {
        final int now = strength();
        if (now != this.lastStrength && this.level != null && !this.level.isClientSide()) {
            this.lastStrength = now;
            this.level.updateNeighbourForOutputSignal(this.worldPosition, getBlockState().getBlock());
        }
    }

    /* Hears the redstone it gets; a clutch or a speed controller is joined again by its network's change. */
    private void hear(final Level level) {
        this.signal.set(level.getBestNeighborSignal(this.worldPosition));
    }

    /* Broken, a pulley lets its belt go: the other pulley is freed and the belt dropped where it hung. */
    private void letGo(final ServerLevel level) {
        final BlockPos other = this.linked.get();
        final ResourceLocation item = this.linkItem.get();
        if (other == null) {
            return;
        }
        if (level.getBlockEntity(other) instanceof MechanicalPartBlockEntity pulley
                && this.worldPosition.equals(pulley.linked())) {
            pulley.unlink();
        }
        if (item != null) {
            Containers.dropItemStack(level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5,
                    this.worldPosition.getZ() + 0.5, new ItemStack(BuiltInRegistries.ITEM.get(item)));
        }
        this.linked.set(null);
    }
}
