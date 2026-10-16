/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A port the player places as a block of its own anywhere on a multiblock's shell, as the valves and hatches of a
 * real plant, so the connection goes where the line arrives: its kind is its block's, and a dye gives it a colour.
 * Ports of one colour are one group, and a recipe takes its inputs from one group at a time, so two processes never
 * mix; a port with no colour is in the group of those with none.
 */
@TextHolder
public class MultiblockPortBlockEntity extends MultiblockPartBlockEntity implements IDescribed {

    private final PortKind kind;
    private final ValueField<DyeColor> colour = fields().nullable("Colour", DyeColor.CODEC).save().toClient();

    public static final TextKey KIND_LINE = TextKey.of("jscore.multiblock.port_kind", "Port: %s");
    public static final TextKey GROUP_LINE = TextKey.of("jscore.multiblock.port_group", "Group: %s");
    public static final TextKey NO_GROUP = TextKey.of("jscore.multiblock.port_no_group", "no colour");

    public MultiblockPortBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                     final PortKind kind) {
        super(type, pos, state);
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    /** Its kind, whatever structure it is part of. */
    @Override
    public PortKind port() {
        return this.kind;
    }

    /** A port block keeps its own kind: a pattern's letter does not change it. */
    @Override
    public void setPort(@Nullable final PortKind kind) {
        // Its kind is its block's.
    }

    /** Its colour, which puts it in a group; none for the group of ports with none. */
    @Nullable
    public DyeColor colour() {
        return this.colour.get();
    }

    /** Gives it {@code to}, or no colour for null; its structure looks at its groups again. */
    public void setColour(@Nullable final DyeColor to) {
        this.colour.set(to);
        if (this.level instanceof ServerLevel server) {
            LevelStructures.of(server).changed(this.worldPosition);
        }
    }

    @Override
    public void describe(final List<Text> lines, final boolean details) {
        lines.add(KIND_LINE.with(this.kind.text()));
        final DyeColor held = this.colour.get();
        lines.add(GROUP_LINE.with(held == null ? NO_GROUP.text()
                : GameText.of(Component.translatable("color.minecraft." + held.getSerializedName()))));
    }
}
