/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.text.Text;
import dev.jstech.space.config.SpaceServerConfig;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Rocket Hangar's terminal: it looks at its bay every few seconds ({@link HangarBays}) and holds the bay it found,
 * told to the players who see it; as the bay forms its pieces take their own shapes, its rails, its bridge and its
 * hoist turned to lie as they run, and as it is undone they are blocks of the kit again. It remembers what kept the bay
 * from forming, for the player who uses it.
 */
public final class HangarTerminalBlockEntity extends SyncedBlockEntity {

    private final ValueField<HangarBay> bay;
    private Text reason = HangarTexts.NOT_YET.text();
    private int wait;

    /* How often it looks at its bay, in ticks: oftener while it is not formed, the player building it. */
    private static final int LOOK_UNFORMED = 40;
    private static final int LOOK_FORMED = 100;

    public HangarTerminalBlockEntity(final BlockPos pos, final BlockState state) {
        super(HangarBlocks.TERMINAL_ENTITY.get(), pos, state);
        this.bay = fields().nullable("Bay", HangarBay.CODEC).save().toClient();
    }

    /** Looks at its bay when it is time to. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final HangarTerminalBlockEntity terminal) {
        if (--terminal.wait > 0) {
            return;
        }
        terminal.look();
    }

    /** The bay it found, if it is formed. */
    public Optional<HangarBay> bay() {
        return Optional.ofNullable(this.bay.get());
    }

    /** What it says to the player who uses it: its bay's size, or what keeps it from forming. */
    public Text reason() {
        return this.reason;
    }

    /** Looks at its bay now, forming it or undoing it as it finds it. */
    public void look() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        final Direction into = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        final HangarBays.Result found = HangarBays.find(this.level, this.worldPosition, into, new HangarBays.Limits(
                SpaceServerConfig.FILE.get(SpaceServerConfig.HANGAR_NARROWEST),
                SpaceServerConfig.FILE.get(SpaceServerConfig.HANGAR_WIDEST),
                SpaceServerConfig.FILE.get(SpaceServerConfig.HANGAR_LOWEST),
                SpaceServerConfig.FILE.get(SpaceServerConfig.HANGAR_TALLEST)));
        this.reason = found.reason();
        final HangarBay was = this.bay.get();
        final HangarBay now = found.bay().orElse(null);
        if (!Objects.equals(was, now)) {
            if (was != null) {
                shape(this.level, was, false);
            }
            if (now != null) {
                shape(this.level, now, true);
            }
            this.bay.set(now);
            setChanged();
        }
        this.wait = now == null ? LOOK_UNFORMED : LOOK_FORMED;
    }

    /** Undoes its bay, as it is broken: its pieces are blocks of the kit again. */
    public void undo() {
        final HangarBay was = this.bay.get();
        if (was != null && this.level != null) {
            shape(this.level, was, false);
            this.bay.set(null);
        }
    }

    /*
     * Gives the pieces of {@code bay} their own shapes, or takes them back: each piece in the bay, and each door in its
     * walls; a formed bay's rails turned to look into the bay, its bridge and its hoist to run the way it spans.
     */
    private static void shape(final Level level, final HangarBay bay, final boolean formed) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        final BlockPos low = bay.low();
        final BlockPos high = bay.high();
        for (int x = low.getX() - 1; x <= high.getX() + 1; x++) {
            for (int z = low.getZ() - 1; z <= high.getZ() + 1; z++) {
                for (int y = low.getY(); y <= high.getY(); y++) {
                    final BlockState state = level.getBlockState(at.set(x, y, z));
                    if (!(state.getBlock() instanceof HangarPieceBlock piece)) {
                        continue;
                    }
                    final boolean inside = bay.holds(at);
                    if (!inside && piece.kind() != HangarPieceBlock.Kind.DOOR) {
                        continue;
                    }
                    BlockState shaped = state.setValue(HangarPieceBlock.FORMED, formed);
                    final Direction way = formed ? way(piece.kind(), bay, at) : null;
                    if (way != null) {
                        shaped = shaped.setValue(HorizontalDirectionalBlock.FACING, way);
                    }
                    if (shaped != state) {
                        level.setBlock(at, shaped, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /* The way a formed piece looks: a rail into the bay, the bridge and the hoist along their span; others as set. */
    private static @Nullable Direction way(final HangarPieceBlock.Kind kind, final HangarBay bay, final BlockPos at) {
        return switch (kind) {
            case RAIL -> at.getZ() == bay.low().getZ() && bay.span() == Direction.Axis.Z ? Direction.SOUTH
                    : at.getZ() == bay.high().getZ() && bay.span() == Direction.Axis.Z ? Direction.NORTH
                    : at.getX() == bay.low().getX() ? Direction.EAST : Direction.WEST;
            case BRIDGE, HOIST -> bay.span() == Direction.Axis.Z ? Direction.NORTH : Direction.EAST;
            default -> null;
        };
    }
}
