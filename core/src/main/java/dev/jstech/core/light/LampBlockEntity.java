/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.FieldEnergyStorage;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;

/**
 * The block entity of a lamp ({@link LampBlock}): it turns its lamp on as its kind's control says (always, by
 * redstone, or while it gets the watts it draws), sets its light, flickering as its kind says, and lays a spotlight's
 * or a beam's light on the world it shines on ({@link BeamLights}). A lamp of a kind that can break is broken by
 * {@link #breakLamp()}, dark until {@link #mend()}; what breaks it is its mod's. Whether it is on, broken and how
 * bright it shines are sent to the players who see it, for its light and glow on their game.
 */
public class LampBlockEntity extends SyncedBlockEntity {

    private final BoolField on;
    private final BoolField broken;
    private final IntField signal;
    private final IntField shining;
    private final FieldEnergyStorage energy;
    private int lastLevel = -1;

    /* How much a lamp powered by energy holds, in seconds of what it draws, and how often a beam looks again. */
    private static final int BUFFER_SECONDS = 10;
    private static final int TICKS_A_SECOND = 20;
    private static final int BEAM_EVERY = 20;
    private static final int FLICKER_EVERY = 2;
    private static final int MOST_STORED = 1_000_000;
    /* The lamps the player's game has, for their glow; the server's game keeps none. */
    private static final Set<LampBlockEntity> SEEN = Collections.synchronizedSet(Collections.newSetFromMap(
            new WeakHashMap<>()));

    public LampBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.on = fields().flag("On", false).toClient();
        this.broken = fields().flag("Broken", false).save().toClient();
        this.signal = fields().integer("Signal", 0).save();
        this.shining = fields().integer("Shining", 0).toClient();
        this.energy = fields().energy("Energy", MOST_STORED, MOST_STORED, 0).save().exposed();
        fields().whenLoaded(this::hear);
    }

    /** The lamps loaded on the player's game, for what draws their glow. */
    public static Set<LampBlockEntity> seen() {
        synchronized (SEEN) {
            return Set.copyOf(SEEN);
        }
    }

    /** Its kind of light, as the side it is on knows it. */
    public LightKind kind() {
        return this.level == null || !(getBlockState().getBlock() instanceof LampBlock lamp) ? LightKind.PLAIN
                : LightKinds.get(this.level, lamp.kind());
    }

    /** Whether it is on. */
    public boolean lit() {
        return this.on.get();
    }

    /** Whether it is broken. */
    public boolean broken() {
        return this.broken.get();
    }

    /** How bright it shines now, its flicker in it, from 0 to 15. */
    public int shining() {
        return this.shining.get();
    }

    /** The way it faces, which a spotlight shines. */
    public Direction facing() {
        return getBlockState().hasProperty(LampBlock.FACING) ? getBlockState().getValue(LampBlock.FACING)
                : Direction.UP;
    }

    /** Breaks it, if its kind can break: dark until mended. */
    public void breakLamp() {
        if (kind().breaks() && !this.broken.get()) {
            this.broken.set(true);
            if (this.level != null) {
                goDark(this.level);
            }
        }
    }

    /** Mends it. */
    public void mend() {
        this.broken.set(false);
    }

    /** The energy it is powered by, for one its kind turns on by power. */
    public FieldEnergyStorage energy() {
        return this.energy;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide()) {
            SEEN.add(this);
            afterClientUpdate();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        SEEN.remove(this);
    }

    /** Each tick on the server: on or off as its control says, its light set, a spotlight's laid on the world. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final LampBlockEntity lamp) {
        if (level instanceof ServerLevel server) {
            lamp.tick(server);
        }
    }

    /* The block entities' light is the server's alone, so the player's game sets its own from what it is sent. */
    @Override
    protected void afterClientUpdate() {
        if (this.level != null) {
            final AuxiliaryLightManager lights = this.level.getAuxLightManager(this.worldPosition);
            if (lights != null && lights.getLightAt(this.worldPosition) != this.shining.get()) {
                lights.setLightAt(this.worldPosition, this.shining.get());
            }
        }
    }

    /** Hears the redstone it gets. */
    void hear(final Level level) {
        this.signal.set(level.getBestNeighborSignal(this.worldPosition));
    }

    /** Takes its light away, as it is broken or removed. */
    void goDark(final Level level) {
        light(level, 0);
        this.on.set(false);
        if (level instanceof ServerLevel server) {
            BeamLights.release(server, this.worldPosition.asLong());
        }
    }

    private void tick(final ServerLevel level) {
        final LightKind kind = kind();
        final boolean wasOn = this.on.get();
        final boolean nowOn = !this.broken.get() && switch (kind.control()) {
            case ALWAYS -> true;
            case REDSTONE -> this.signal.get() > 0;
            case POWER -> draw(kind);
        };
        this.on.set(nowOn);
        final long time = level.getGameTime();
        if (wasOn != nowOn || kind.flicker().steady() || time % FLICKER_EVERY == 0) {
            light(level, nowOn ? kind.levelAt(time / (double) TICKS_A_SECOND, this.worldPosition.asLong()) : 0);
        }
        if (kind.shape() != LightKind.Shape.POINT && (wasOn != nowOn || time % BEAM_EVERY == 0)) {
            if (nowOn) {
                BeamLights.light(level, this.worldPosition.asLong(), Beams.lamp(level, this.worldPosition, facing(),
                        kind, kind.intensity()));
            } else {
                BeamLights.release(level, this.worldPosition.asLong());
            }
        }
    }

    /* Draws what it draws a tick from its energy, a joule an FE; whether it had it. */
    private boolean draw(final LightKind kind) {
        final int tick = (int) Math.ceil(kind.watts() / TICKS_A_SECOND);
        this.energy.setCapacity(Math.max(1, Math.min(MOST_STORED, tick * TICKS_A_SECOND * BUFFER_SECONDS)));
        return tick <= 0 || this.energy.consume(tick);
    }

    private void light(final Level level, final int brightness) {
        if (brightness == this.lastLevel) {
            return;
        }
        this.lastLevel = brightness;
        this.shining.set(brightness);
        final AuxiliaryLightManager lights = level.getAuxLightManager(this.worldPosition);
        if (lights != null) {
            lights.setLightAt(this.worldPosition, brightness);
        }
    }
}
