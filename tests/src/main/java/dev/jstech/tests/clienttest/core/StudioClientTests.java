/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.studio.BenchState;
import dev.jstech.core.client.studio.StudioClient;
import dev.jstech.core.client.studio.StudioScreen;
import dev.jstech.core.client.studio.StudioSheet;
import dev.jstech.core.client.studio.StudioTool;
import dev.jstech.core.client.studio.ThingKind;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The studio: every mod's things by kind, a machine chosen on the bench in its states, what it is made of, each part
 * with its file and its state, and the checks across its parts.
 */
public final class StudioClientTests {

    private static final int WAIT = 60;
    private static final int SETTLE = 3;
    /* The studio reads every thing as it opens, and the ticks it held up then run with no frame between. */
    private static final int SHOT = 20;
    /* A machine of the test mod's, drawn from a states file and named in English. */
    private static final ResourceLocation MACHINE = TestKitMachine.BLOCK.getId();
    /* Where the player stands, and the machine in front of it. */
    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final BlockPos AT = new BlockPos(0, 2, -3);

    private StudioClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void studio_showsWhatAMachineIsMadeOfOnItsBench(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.THINGS)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final StudioScreen studio = ctx.screen(StudioScreen.class);
                    ctx.assertTrue(studio.choose(MACHINE), "the test mod's machine is among the things");
                    ctx.assertTrue(studio.chosen().filter(thing -> thing.kind() == ThingKind.MACHINES).isPresent(),
                            "a block whose block entity is a machine's is under the machines");
                    final StudioSheet sheet = studio.sheet().orElseThrow();
                    final List<String> parts = sheet.parts().stream().map(StudioSheet.Part::name).toList();
                    ctx.assertTrue(parts.containsAll(List.of("Model", "Names", "Recipe", "Tags", "Drops", "Tests")),
                            "it is made of its model, names, recipe, tags, drops and tests: " + parts);
                    final StudioSheet.Part model = part(sheet, "Model");
                    ctx.assertTrue(model.detail().equals("blockstates/" + MACHINE.getPath() + ".json")
                                    && model.mark() == StudioSheet.Mark.FINE,
                            "its model is its states file, there: " + model);
                    final StudioSheet.Part names = part(sheet, "Names");
                    ctx.assertTrue(names.detail().contains("en_us") && names.mark() == StudioSheet.Mark.FINE,
                            "its name is in every language of its mod: " + names);
                    ctx.assertTrue(part(sheet, "Drops").detail().equals("itself"),
                            "it drops itself: " + part(sheet, "Drops"));
                    ctx.assertTrue(sheet.checks().stream().noneMatch(check -> check.mark()
                            == StudioSheet.Mark.ERROR), "nothing across its parts is wrong: " + sheet.checks());
                    ctx.assertTrue(studio.problems() >= sheet.checks().size(),
                            "the checks count every thing's problems");
                })
                .thenScreenshot(SHOT, "studio-working")
                .then(0, () -> ctx.screen(StudioScreen.class).showState(BenchState.JAMMED))
                .thenScreenshot(SHOT, "studio-jammed");
    }

    @ClientTest(timeoutTicks = 300)
    public static void studio_readsTheBlockLookedAtOnItsPage(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenServer(SETTLE, level -> level.setBlockAndUpdate(ctx.abs(AT), TestKitMachine.BLOCK.get()
                        .defaultBlockState()))
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(AT))))
                // The game picks the block looked at each tick, and the studio takes it as it opens.
                .then(SETTLE, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.BLOCK)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .thenWaitUntil(() -> !ctx.screen(StudioScreen.class).lines().isEmpty(), WAIT,
                        "the server answers with the fields of the machine looked at")
                .then(0, () -> ctx.assertTrue(ctx.screen(StudioScreen.class).tool() == StudioTool.BLOCK,
                        "the studio opens on the tool asked"))
                .thenScreenshot(SHOT, "studio-block");
    }

    @ClientTest(timeoutTicks = 600)
    public static void studio_opensEveryToolOnItsOwnPage(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.THINGS)))
                .thenAwaitScreen(StudioScreen.class, WAIT);
        for (final StudioTool tool : StudioTool.inOrder()) {
            ctx.then(SETTLE, () -> ctx.screen(StudioScreen.class).open(tool))
                    .then(SETTLE, () -> ctx.assertTrue(ctx.screen(StudioScreen.class).tool() == tool,
                            "the tab of " + tool + " opens its page"))
                    .thenScreenshot(SETTLE, "studio-tool-" + tool.name().toLowerCase(Locale.ROOT));
        }
    }

    private static StudioSheet.Part part(final StudioSheet sheet, final String name) {
        return sheet.parts().stream().filter(part -> part.name().equals(name)).findFirst().orElseThrow();
    }
}
