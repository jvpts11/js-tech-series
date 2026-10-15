/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.gui.component.ChartView;
import dev.jstech.core.client.gui.component.MapView;
import dev.jstech.core.client.gui.data.IScreenLogic;
import dev.jstech.core.client.gui.logic.GraphModel;
import dev.jstech.core.client.gui.logic.TableModel;
import dev.jstech.core.client.gui.logic.TreeModel;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What the test screens of controls and of views show and keep, as a mod's screen code would: every value the files
 * bind and every handler their actions name, held here for the client tests to read back.
 */
public final class TestControlScreens {

    /** The values the controls set, read back by the tests. */
    public static final double[] SPEED = {50};
    public static final double[] SIGNAL = {7};
    public static final boolean[] EJECT = {true};
    public static final int[] MODE = {0};
    public static final int[] SORT = {0};
    public static final int[] COLOUR = {0xFF39D6C4};
    public static final ItemStack[] FILTER = {ItemStack.EMPTY};
    public static final String[] PICKED = {""};
    public static final String[] ROW = {""};
    public static final int[] ADDED = {0};

    private TestControlScreens() {
    }

    /** The code of the screen of controls. */
    public static IScreenLogic<Void> controls() {
        return screen -> screen
                .value("speed", () -> SPEED[0]).onNumber("speed", value -> SPEED[0] = value)
                .value("signal", () -> SIGNAL[0]).onNumber("signal", value -> SIGNAL[0] = value)
                .value("eject", () -> EJECT[0]).onAction("eject", () -> EJECT[0] = !EJECT[0])
                .value("mode", () -> MODE[0]).onChoice("mode", value -> MODE[0] = value)
                .value("sort", () -> SORT[0]).onChoice("sort", value -> SORT[0] = value)
                .value("colour", () -> COLOUR[0]).onColour("colour", value -> COLOUR[0] = value)
                .value("fluid", () -> new FluidStack(Fluids.WATER, 5000))
                .value("energy", () -> 640)
                .value("filter", () -> FILTER[0]).onItem("filter", stack -> FILTER[0] = stack);
    }

    /** The code of the screen of views. */
    public static IScreenLogic<Void> views() {
        final TreeModel tree = new TreeModel(List.of(new TreeModel.Node("line", "Ore line", -1, List.of(
                TreeModel.Node.leaf("c1", "Crusher #1"), TreeModel.Node.leaf("c2", "Crusher #2"))),
                new TreeModel.Node("power", "Power", 6, List.of())));
        tree.open("line");
        final List<String[]> rows = List.of(new String[] {"Crusher #1", "1284"}, new String[] {"Furnace #1", "980"});
        final TableModel<String[]> table = new TableModel<>(List.of(
                TableModel.Column.<String[]>words("Machine", 90, r -> r[0]),
                TableModel.Column.<String[]>amounts("Made", 40, r -> r[1], r -> Double.parseDouble(r[1]))),
                () -> rows);
        final GraphModel graph = new GraphModel()
                .put(new GraphModel.Node("store", "Storage", 8, 8, 60, List.of(),
                        List.of(new GraphModel.Port("ores", "Ores", "items"))))
                .put(new GraphModel.Node("crusher", "Crusher", 96, 8, 60,
                        List.of(new GraphModel.Port("in", "Ores", "items")), List.of()));
        return screen -> screen
                .value("tree", () -> tree).on("picked", id -> PICKED[0] = String.valueOf(id))
                .value("table", () -> table).on("row", row -> ROW[0] = String.valueOf(row))
                .value("power", () -> List.of(new ChartView.Series("Crushers", new double[] {2.4, 2.8, 2.1, 3.2}),
                        new ChartView.Series("Furnaces", new double[] {1.3, 1.2, 1.5, 1.4})))
                .value("made", () -> List.of(ChartView.Series.of("C1", 21), ChartView.Series.of("F1", 16)))
                .value("share", () -> List.of(ChartView.Series.of("Crushers", 52), ChartView.Series.of("Other", 48)))
                .value("graph", () -> graph).onPoint("add", (x, y) -> ADDED[0]++)
                .value("block", Blocks.BLAST_FURNACE::defaultBlockState)
                .value("here", () -> Minecraft.getInstance().player == null ? BlockPos.ZERO
                        : Minecraft.getInstance().player.blockPosition())
                .value("markers", List::<MapView.Marker>of);
    }
}
