/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.LineSurvey;
import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.ProbeLink;
import dev.jstech.core.jstechseries.probe.ProbeReading;
import dev.jstech.core.jstechseries.probe.ProbeSessions;
import dev.jstech.core.jstechseries.probe.ProbeState;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestEnergies;
import dev.jstech.tests.TestEnergyEnds;
import dev.jstech.tests.TestFluidEnds;
import dev.jstech.tests.testkit.TestCables;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The network probe and what it reads: a line of pipes tells its output from its input, what it moved and its
 * narrowest pipe; a line of energy what was asked of it and what it lost; the probe links to the wire it is used on
 * and reads its line, grouping its blocks by what they are to it; and its pages go round and its rows stay in range.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkProbeGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos OUTPUT = new BlockPos(1, 2, 2);
    private static final BlockPos INPUT = new BlockPos(5, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));

    private NetworkProbeGameTests() {
    }

    @GameTest(template = ARENA)
    public static void survey_tellsAPipesOutputFromItsInput(final GameTestHelper helper) {
        pipes(helper);
        helper.succeedWhen(() -> {
            final LineSurvey survey = survey(helper, TestCableTypes.PLAIN_PIPE).orElse(null);
            helper.assertTrue(survey != null && survey.moved() == TestCableTypes.PLAIN_RATE,
                    "the line moves what its pipes carry: " + survey);
            helper.assertTrue(survey.cables() == RUN.size(), "three pipes");
            helper.assertTrue(survey.narrowest().map(LineSurvey.Narrowest::ceiling).orElse(0L)
                    == TestCableTypes.PLAIN_RATE, "the narrowest is a plain pipe");
            helper.assertTrue(survey.wanted() == LineSurvey.UNKNOWN && survey.lost() == 0L,
                    "a pipe cannot tell what was asked of it, and loses nothing");
            helper.assertTrue(member(survey, OUTPUT, helper).role() == LineSurvey.Role.SOURCE
                    && member(survey, INPUT, helper).role() == LineSurvey.Role.CONSUMER,
                    "the output gives, the input takes");
        });
    }

    @GameTest(template = ARENA)
    public static void survey_tellsWhatAnEnergyLineWasAskedAndLost(final GameTestHelper helper) {
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(OUTPUT));
        TestEnergyEnds.clear(helper.getLevel(), helper.absolutePos(INPUT));
        helper.setBlock(OUTPUT, Blocks.CUT_COPPER);
        helper.setBlock(INPUT, Blocks.EXPOSED_CUT_COPPER);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestEnergies.LOW_LINE);
        }
        TestEnergyEnds.storeAt(helper.getLevel(), helper.absolutePos(OUTPUT)).setStored(100_000L);
        helper.succeedWhen(() -> {
            final LineSurvey survey = survey(helper, TestEnergies.LOW_LINE).orElse(null);
            // Three blocks each losing a thousandth: 1004 sent bring the thousand the sink takes, and 4 are lost.
            helper.assertTrue(survey != null && survey.moved() == TestEnergyEnds.TAKES + 4L,
                    "what the sources sent: " + survey);
            helper.assertTrue(survey.wanted() == TestEnergyEnds.TAKES, "what the sink asked: " + survey.wanted());
            helper.assertTrue(survey.lost() == 4L, "what the cables lost: " + survey.lost());
            final LineSurvey.Member sink = member(survey, INPUT, helper);
            helper.assertTrue(sink.took() == TestEnergyEnds.TAKES, "the sink took a thousand: " + sink);
        });
    }

    @GameTest(template = ARENA)
    public static void probe_linksToAWireAndReadsItsLine(final GameTestHelper helper) {
        pipes(helper);
        final BlockPos cable = helper.absolutePos(RUN.get(1));
        final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                "probe_link".getBytes(StandardCharsets.UTF_8)), "probe_link"));
        player.moveTo(cable.getX() + 0.5, cable.getY() + 0.5 - player.getEyeHeight(), cable.getZ() - 1.5, 0.0F,
                0.0F);
        final ItemStack probe = new ItemStack(NetworkProbe.ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, probe);
        NetworkProbe.ITEM.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(
                new Vec3(cable.getX() + 0.5, cable.getY() + 0.5, cable.getZ() + 0.2), Direction.NORTH, cable,
                false)));
        final ProbeLink link = player.getMainHandItem().get(NetworkProbe.LINK.get());
        helper.assertTrue(link != null && link.pos().equals(cable)
                && link.cable().equals(TestCableTypes.PLAIN_PIPE.get().id()), "the probe links to the pipe: " + link);
        helper.succeedWhen(() -> {
            ProbeSessions.tick(player, player.getMainHandItem());
            final ProbeReading reading = ProbeSessions.lastReading(player).orElse(ProbeReading.nothing());
            helper.assertTrue(reading.present() && reading.unit() == ProbeReading.Unit.FLUID,
                    "it reads a line of fluids");
            helper.assertTrue(reading.cables() == RUN.size() && reading.moved() > 0, "its pipes and its flow: "
                    + reading.moved());
            helper.assertTrue(reading.count(LineSurvey.Role.SOURCE) == 1
                    && reading.count(LineSurvey.Role.CONSUMER) == 1, "one source and one consumer: "
                    + reading.groups());
            helper.assertTrue(reading.groups().stream().anyMatch(group -> group.places().contains(
                    helper.absolutePos(INPUT).asLong())), "each group knows where its blocks are");
        });
    }

    @GameTest(template = ARENA)
    public static void state_pagesGoRoundAndRowsStayInRange(final GameTestHelper helper) {
        final ProbeState state = ProbeState.DEFAULT;
        helper.assertTrue(state.paged(-1).page() == ProbeState.Page.LOSSES, "left of the summary are the losses");
        helper.assertTrue(state.onPage(ProbeState.Page.LOSSES).paged(1).page() == ProbeState.Page.SUMMARY,
                "and right of the losses, the summary");
        helper.assertTrue(state.rowMoved(-3, 5).row() == 0 && state.rowMoved(9, 5).row() == 4,
                "the row never leaves the rows there are");
        final ProbeState set = state.toggleHold().toggleLight().togglePower().rowMoved(2, 4);
        final ProbeState back = ProbeState.CODEC.parse(JsonOps.INSTANCE, ProbeState.CODEC.encodeStart(
                JsonOps.INSTANCE, set).getOrThrow()).getOrThrow();
        helper.assertTrue(back.equals(set), "it is saved as it is set: " + back);
        helper.succeed();
    }

    /* An output of water, a run of plain pipe east, and an input past it. */
    private static void pipes(final GameTestHelper helper) {
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(OUTPUT));
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(INPUT));
        TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT)).fill(new FluidStack(Fluids.WATER, 50_000),
                IFluidHandler.FluidAction.EXECUTE);
        helper.setBlock(OUTPUT, Blocks.CHISELED_TUFF);
        helper.setBlock(INPUT, Blocks.CHISELED_TUFF_BRICKS);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestCableTypes.PLAIN_PIPE);
        }
    }

    /* What the part of the run's first wire did in the last tick, as the line's mover says. */
    private static Optional<LineSurvey> survey(final GameTestHelper helper, final Supplier<CableType> type) {
        final OptionalLong number = CoreGrids.places(helper.getLevel())
                .find(TestCables.cable(helper, RUN.get(0)).place(type.get()));
        return number.isEmpty() ? Optional.empty() : CoreGrids.mover(helper.getLevel(), type.get().grid())
                .flatMap(mover -> mover.survey(number.getAsLong()));
    }

    private static LineSurvey.Member member(final LineSurvey survey, final BlockPos at,
                                            final GameTestHelper helper) {
        final long pos = helper.absolutePos(at).asLong();
        return survey.members().stream().filter(member -> member.pos() == pos).findFirst()
                .orElse(new LineSurvey.Member(pos, LineSurvey.Role.IDLE, 0L, 0L));
    }
}
