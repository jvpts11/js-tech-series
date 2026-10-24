/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.industrial;

import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.Ownership;
import dev.jstech.industrial.client.crc.CrcScreen;
import dev.jstech.industrial.crc.CrcBlockEntity;
import dev.jstech.industrial.crc.CrcContent;
import dev.jstech.industrial.crc.CrcLayout;
import dev.jstech.industrial.crc.CrcPart;
import dev.jstech.industrial.crc.CrcReadings;
import dev.jstech.industrial.crc.CrcStructure;
import dev.jstech.industrial.research.IndustrialResearch;
import dev.jstech.tests.TestCrcResearch;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Computational Research Centre's screen on a player's game, to set beside the approved design: a CRC owned by
 * the player, formed with its Power Entry, its places filled as the design draws them and some data in its data
 * places, working a discovery its team has made a part of; each of its six tabs in turn.
 */
public final class CrcScreenClientTests {

    /* The terminal, the body running south of it; the player north of it, looking at its console. */
    private static final BlockPos TERMINAL = new BlockPos(0, 2, 2);
    private static final BlockPos STAND = new BlockPos(0, 2, -1);
    private static final int WAIT = 200;
    private static final int TICKS_A_SECOND = 20;
    /* The places as the design draws them, section after section: R a research processor, I an integrated one, S a
     * heat sink, L a liquid cooler. */
    private static final String PLACES = "RSRSRSRSR" + "LILILILIL" + "SRSSRSSRS";
    private static final int CRYSTAL = 7;
    private static final int SPECTRAL = 3;
    private static final double MADE = 0.62;

    private CrcScreenClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void screen_showsEachOfItsSixTabs(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> build(ctx, level))
                .thenWaitUntilServer(level -> crc(ctx, level).formed(), WAIT, "the CRC to form",
                        level -> String.valueOf(crc(ctx, level).structure().failure()))
                .thenServer(0, level -> {
                    final CrcBlockEntity crc = crc(ctx, level);
                    final String team = CoreTeams.teamOf(level.getServer(), ctx.player().getUUID());
                    TeamProgress.grant(level.getServer(), team, TestCrcResearch.LONG.knowledge(), MADE);
                    crc.choose(level, TestCrcResearch.LONG.id());
                    crc.enqueue(level, TestCrcResearch.SHORT.id());
                })
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(TERMINAL))))
                .then(2, () -> ctx.rightClick(TERMINAL, Direction.NORTH))
                .thenWaitUntil(() -> ctx.mc().screen instanceof CrcScreen screen && screen.getMenu()
                        .screenData(CrcReadings.class).points() > 0.0F, WAIT,
                        "the CRC's screen to open on it at work")
                .then(0, () -> ctx.pointAt(1, 1))
                .thenScreenshot(TICKS_A_SECOND, "crc-processors")
                .then(0, () -> tab(ctx, 1))
                .thenScreenshot(5, "crc-research")
                .then(0, () -> tab(ctx, 2))
                .thenScreenshot(5, "crc-group")
                .then(0, () -> tab(ctx, 3))
                .thenAssert(2, () -> menu(ctx).tab() == MachineMenu.SIDES, "the fourth tab is Sides")
                .thenScreenshot(5, "crc-sides")
                .then(0, () -> tab(ctx, 4))
                .thenScreenshot(5, "crc-redstone")
                .then(0, () -> tab(ctx, 5))
                .thenAssert(2, () -> menu(ctx).tab() == MachineMenu.PROTECTION, "the last tab is Protection")
                .thenScreenshot(5, "crc-protection")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> clear(ctx, level));
    }

    /* The CRC, owned by the player, formed, its places and data filled, its energy full. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        final MultiblockPattern pattern = CrcStructure.PATTERN;
        for (int y = 0; y < pattern.sizeY(); y++) {
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    final BlockPos at = ctx.abs(TERMINAL.offset(x - pattern.terminalX(), y - pattern.terminalY(),
                            z - pattern.terminalZ()));
                    final char letter = pattern.charAt(x, y, z);
                    final BlockState state = letter == '#' ? CrcContent.TERMINAL.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                            : letter == 'G' ? CrcContent.GATE.get().defaultBlockState()
                            : letter == 'B' ? CrcContent.BUS.get().defaultBlockState()
                            : CrcContent.CASING.get().defaultBlockState();
                    level.setBlockAndUpdate(at, state);
                }
            }
        }
        level.setBlockAndUpdate(ctx.abs(TERMINAL.offset(0, 0, pattern.sizeZ() - 1)), CrcContent.POWER_ENTRY.get()
                .defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        final CrcBlockEntity crc = crc(ctx, level);
        crc.own(Ownership.of(ctx.player().getUUID()));
        for (int place = 0; place < CrcBlockEntity.PLACES; place++) {
            final CrcPart part = switch (PLACES.charAt(place)) {
                case 'R' -> CrcPart.RESEARCH_PROCESSOR;
                case 'I' -> CrcPart.INTEGRATED_RESEARCH_PROCESSOR;
                case 'L' -> CrcPart.LIQUID_COOLER;
                default -> CrcPart.HEAT_SINK;
            };
            crc.getInventory().setStackInSlot(place, new ItemStack(CrcContent.PARTS.get(part).get()));
        }
        crc.getInventory().setStackInSlot(CrcBlockEntity.FIRST_DATA + 1, new ItemStack(
                IndustrialResearch.CRYSTAL_DATA.get(), CRYSTAL));
        crc.getInventory().setStackInSlot(CrcBlockEntity.FIRST_DATA + 2, new ItemStack(
                IndustrialResearch.SPECTRAL_DATA.get(), SPECTRAL));
        Objects.requireNonNull(crc.power().portStore(), "a port").setStored(crc.power().portStore().capacity());
    }

    /* Takes the CRC away, every block of it. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        final MultiblockPattern pattern = CrcStructure.PATTERN;
        for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(TERMINAL.offset(-pattern.terminalX(), 0, 0)),
                ctx.abs(TERMINAL.offset(pattern.sizeX() - 1 - pattern.terminalX(), pattern.sizeY() - 1,
                        pattern.sizeZ() - 1)))) {
            level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
        }
    }

    private static CrcBlockEntity crc(final ClientTestContext ctx, final ServerLevel level) {
        return (CrcBlockEntity) Objects.requireNonNull(level.getBlockEntity(ctx.abs(TERMINAL)), "no CRC terminal");
    }

    private static MachineMenu menu(final ClientTestContext ctx) {
        return ((CrcScreen) Objects.requireNonNull(ctx.mc().screen, "no screen")).getMenu();
    }

    /*
     * Clicks the tab at place {@code place} along the top of the screen, drawn in the middle of the window, and takes
     * the pointer off the screen, so no tooltip covers it.
     */
    private static void tab(final ClientTestContext ctx, final int place) {
        final int left = (ctx.mc().getWindow().getGuiScaledWidth() - CrcLayout.WIDTH) / 2;
        final int top = (ctx.mc().getWindow().getGuiScaledHeight() - CrcLayout.HEIGHT) / 2;
        ctx.click(left + CrcLayout.tabX(place) + MachineScreenLayout.TAB_WIDTH / 2.0,
                top + MachineScreenLayout.TAB_HEIGHT / 2.0);
        ctx.pointAt(1, 1);
    }
}
