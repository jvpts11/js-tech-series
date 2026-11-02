/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A console of the Rocket Hangar's control room, in the row before the windows into the bay, as a launch control room
 * has them: its cabinet, the keyboard's shelf with its lamps, the sloped top with its screen toward the chair. Its
 * screen is dark until its hangar forms; then, along the row, each shows the rocket in its stages, a reading over time
 * or the build's list.
 */
public final class HangarConsoleBlock extends HangarFittingBlock {

    private final MapCodec<HangarConsoleBlock> codec;

    /** What its screen shows. */
    public static final EnumProperty<Screen> SCREEN = EnumProperty.create("screen", Screen.class, Screen.OFF,
            Screen.ROCKET, Screen.GRAPH, Screen.LIST);
    /** Its shape as it faces north: the cabinet with its shelf, and the sloped top's housing over its back. */
    public static final int[][] BOXES = {{0, 0, 1, 16, 13, 16}, {0, 13, 5, 16, 20, 16}};

    public HangarConsoleBlock(final BlockBehaviour.Properties properties) {
        super(properties, BOXES);
        this.codec = simpleCodec(HangarConsoleBlock::new);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(SCREEN, Screen.OFF));
    }

    /** The screen a console shows at {@code along} consoles from the row's first, once its hangar forms. */
    public static Screen screenAt(final int along) {
        return switch (Math.floorMod(along, 3)) {
            case 0 -> Screen.ROCKET;
            case 1 -> Screen.GRAPH;
            default -> Screen.LIST;
        };
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SCREEN);
    }

    /** What a console's screen shows: nothing, its part of the row's pictures, or its half of the door's console. */
    public enum Screen implements StringRepresentable {
        OFF("off"),
        ROCKET("rocket"),
        GRAPH("graph"),
        LIST("list"),
        LEFT("left"),
        RIGHT("right");

        private final String name;

        Screen(final String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
