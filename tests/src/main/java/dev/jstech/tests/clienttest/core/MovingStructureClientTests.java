/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.SpinMotion;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.Box;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.clienttest.ClientTestFailure;
import dev.jstech.tests.testkit.ChunkLoadWatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A moving structure as a player sees it: its blocks reaching the player's game from the yard and drawn where the
 * structure is, lit as on the server, a torch on it giving off its flame where the player sees it, a chest on it aimed
 * at as the real block it is, and the player standing on its floor carried up as it rises. The world is then saved
 * and opened again with the player still on the structure: the structure comes back
 * drawn where it was and the player stands on it again. Screenshots show it at rest and turned. A cube made a
 * structure and set down again is drawn every frame of it, its blocks changing hands with the world's within a frame
 * each way. And structures made
 * on land still loading round a player sent there hold no chunk up.
 */
public final class MovingStructureClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final BlockPos CORNER = new BlockPos(-1, 3, -6);
    private static final BlockPos OTHER_CORNER = new BlockPos(1, 3, -4);
    private static final BlockPos CHEST = new BlockPos(0, 4, -5);
    private static final int WAIT = 200;
    private static final int SETTLE = 10;
    private static final String CORE = "dev.jstech.core.";
    private static final int LIGHTEST = 15;
    /* How many times the torch aboard is given its chance to show its flame, each a flame and a puff of smoke. */
    private static final int FLAMES = 8;
    /* A cube in front of the sky, over the player's eye; and how much bluer than red the sky is, white concrete not. */
    private static final BlockPos CUBE_FROM = new BlockPos(-1, 4, -7);
    private static final BlockPos CUBE_TO = new BlockPos(1, 6, -5);
    private static final int SKY_BLUER = 60;
    /* How many times the player is sent to new land, and how far each time, in blocks east and south. */
    private static final int TRIPS = 6;
    private static final int TRIP = 1_500;

    private MovingStructureClientTests() {
    }

    @ClientTest(timeoutTicks = 2000)
    public static void structure_isDrawnAimedAtAndStoodOn(final ClientTestContext ctx) {
        final AtomicReference<MovingStructure> made = new AtomicReference<>();
        final int[] particlesBefore = new int[1];
        ctx.afterTest(() -> ctx.server().execute(() -> takeAll(ctx.serverLevel())));
        ctx.thenServer(0, level -> {
            // A run stopped short leaves its structure in the test world; it is taken away first.
            takeAll(level);
            for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(CORNER), ctx.abs(OTHER_CORNER))) {
                level.setBlockAndUpdate(at, Blocks.OAK_PLANKS.defaultBlockState());
            }
            level.setBlockAndUpdate(ctx.abs(CHEST), Blocks.CHEST.defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(CHEST.east()), Blocks.LANTERN.defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(CHEST.west()), Blocks.TORCH.defaultBlockState());
            final StructureResult result = MovingStructures.assemble(level, ctx.abs(CORNER),
                    ctx.abs(OTHER_CORNER.above()));
            made.set(result.structure());
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .thenWaitUntil(() -> ClientMovingStructures.all().stream().anyMatch(structure -> structure.ready()
                                && structure.meshes().built() > 0 && !structure.meshes().waiting()), WAIT,
                        "the structure's blocks to reach the player's game and be built",
                        () -> ClientMovingStructures.all().size() + " structures, "
                                + ClientMovingStructures.all().stream().map(structure -> structure.ready() + "/"
                                + structure.meshes().built()).toList())
                .thenWaitUntil(() -> wronglyLit(ctx).isEmpty(), WAIT,
                        "the structure's blocks to be lit on the player's game as on the server",
                        () -> wronglyLit(ctx))
                .then(0, () -> {
                    particlesBefore[0] = particles();
                    torchFlames();
                })
                .thenAssert(2, () -> particles() >= particlesBefore[0] + FLAMES,
                        "a torch aboard gives off its flame and smoke where the player sees it")
                .thenWaitUntil(() -> {
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(CHEST)).add(0, 0.3, 0));
                    return Minecraft.getInstance().hitResult instanceof BlockHitResult hit
                            && YardLayout.isYardBlock(hit.getBlockPos().getX())
                            && Minecraft.getInstance().level.getBlockState(hit.getBlockPos()).is(Blocks.CHEST)
                            && YardLayout.isYard(hit.getLocation().x);
                }, SETTLE * 4, "the aim to rest on the chest, its own block, at a place on it",
                        () -> (Minecraft.getInstance().hitResult instanceof BlockHitResult hit ? "aimed at "
                                + hit.getType() + " " + hit.getBlockPos() + " at " + hit.getLocation() + ", "
                                + Minecraft.getInstance().level.getBlockState(hit.getBlockPos()) + "; eye "
                                + ctx.player().getEyePosition() : "aimed at " + Minecraft.getInstance().hitResult)
                                + "; " + chestSeen(ctx, made.get()))
                .thenScreenshot(SETTLE, "moving_structure")
                .thenServer(0, level -> made.get().move(new SpinMotion(0, 1, 0, 45)))
                .thenServer(20, level -> made.get().move(null))
                .thenScreenshot(SETTLE, "moving_structure_turned")
                .then(0, () -> {
                    // On the floor, beside the chest: the floor is a block below the top of the structure's box.
                    final ClientStructure seen = ClientMovingStructures.all().iterator().next();
                    final Vec3 middle = seen.worldBox().getCenter();
                    ctx.player().setPos(middle.x + 0.6, seen.worldBox().minY + 1.3, middle.z + 0.6);
                    ctx.player().setDeltaMovement(Vec3.ZERO);
                })
                .thenWaitUntil(() -> StructureFooting.standingOn(ctx.player()) instanceof ClientStructure, WAIT,
                        "the player to stand on the structure's floor")
                .thenServer(0, level -> made.get().move(new TowardMotion(made.get().pose().movedTo(
                        made.get().pose().x(), made.get().pose().y() + 2, made.get().pose().z()), 2, 0)))
                .thenWaitUntilServer(level -> made.get().resting(), WAIT, "the structure to rise and stop",
                        level -> "at " + made.get().pose())
                .thenAssert(SETTLE, () -> {
                    final IStructureBody under = StructureFooting.standingOn(ctx.player());
                    return under != null && Math.abs(ctx.player().getY() - (under.worldBox().minY + 1)) < 0.1;
                }, "the player rose with the floor they stood on")
                .thenScreenshot(SETTLE, "moving_structure_risen")
                .thenSaveAndReload(20)
                .thenAwaitLandDrawn(WAIT)
                .thenWaitUntil(() -> ClientMovingStructures.all().stream().anyMatch(structure -> structure.ready()
                                && structure.meshes().built() > 0 && !structure.meshes().waiting()), WAIT,
                        "the structure saved with the world to come back to the player's game and be built")
                .thenWaitUntil(() -> StructureFooting.standingOn(ctx.player()) instanceof ClientStructure, WAIT,
                        "the player who left the game on the structure to stand on it again",
                        () -> "the player at " + ctx.player().position() + ", the structure at "
                                + ClientMovingStructures.all().stream().map(ClientStructure::worldBox).toList())
                .thenServer(0, level -> {
                    takeAll(level);
                    for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(CORNER), ctx.abs(OTHER_CORNER))) {
                        level.setBlockAndUpdate(at, Blocks.OAK_PLANKS.defaultBlockState());
                    }
                    made.set(MovingStructures.assemble(level, ctx.abs(CORNER), ctx.abs(OTHER_CORNER)).structure());
                })
                .thenWaitUntil(() -> ClientMovingStructures.all().stream().anyMatch(structure -> structure.ready()
                                && structure.meshes().built() > 0), WAIT,
                        "a structure made again in a plot used before the world was saved to reach the player");
    }

    /*
     * A cube in front of the sky made a structure and set down again, watched every frame: the cube's blocks change
     * hands between the world and the structure each way within one frame, so no frame shows the sky where it is.
     */
    @ClientTest(timeoutTicks = 1200)
    public static void blocks_changeHandsWithTheWorldInOneFrame(final ClientTestContext ctx) {
        final AtomicReference<MovingStructure> made = new AtomicReference<>();
        final int[] frames = new int[2];
        final boolean[] watching = new boolean[1];
        final Consumer<RenderFrameEvent.Post> look = event -> {
            if (watching[0]) {
                frames[0]++;
                if (skyInTheMiddle(ctx)) {
                    frames[1]++;
                }
            }
        };
        NeoForge.EVENT_BUS.addListener(look);
        ctx.afterTest(() -> {
            NeoForge.EVENT_BUS.unregister(look);
            ctx.mc().options.hideGui = false;
            ctx.server().execute(() -> {
                takeAll(ctx.serverLevel());
                for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(CUBE_FROM), ctx.abs(CUBE_TO))) {
                    ctx.serverLevel().setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                }
            });
        });
        ctx.thenServer(0, level -> {
            takeAll(level);
            for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(CUBE_FROM), ctx.abs(CUBE_TO))) {
                level.setBlockAndUpdate(at, Blocks.WHITE_CONCRETE.defaultBlockState());
            }
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenAwaitLandDrawn(WAIT)
                .then(SETTLE, () -> {
                    ctx.mc().options.hideGui = true;
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(CUBE_FROM)).add(1, 1, 1));
                })
                .thenWaitUntil(() -> !skyInTheMiddle(ctx), WAIT, "the cube to be drawn in front of the sky")
                .then(0, () -> watching[0] = true)
                .thenServer(0, level -> made.set(MovingStructures.assemble(level, ctx.abs(CUBE_FROM),
                        ctx.abs(CUBE_TO)).structure()))
                .thenWaitUntil(() -> !ClientMovingStructures.handingOver() && ClientMovingStructures.all().stream()
                                .anyMatch(structure -> structure.ready() && !structure.meshes().waiting()), WAIT,
                        "the cube to be drawn as a structure")
                .thenServer(SETTLE, level -> MovingStructures.setDown(made.get()))
                .thenWaitUntil(() -> ClientMovingStructures.all().isEmpty() && !ClientMovingStructures.handingOver(),
                        WAIT, "the cube to be drawn as the world's blocks again")
                .then(SETTLE, () -> {
                    watching[0] = false;
                    ctx.assertTrue(frames[0] > 0 && frames[1] == 0, frames[1] + " of " + frames[0]
                            + " frames showed the sky where the cube is");
                });
    }

    /*
     * Structures made on land still loading round a player just sent there, again and again, while the land the
     * player left unloads: nothing of the Core loads a chunk by reading it, which could hold the server's chunks up
     * for good, and the world saves and opens again with its land.
     */
    @ClientTest(timeoutTicks = 3000)
    public static void structures_madeWhileTheLandLoadsHoldNothingUp(final ClientTestContext ctx) {
        final int[] loadsBefore = new int[1];
        ctx.afterTest(() -> ctx.server().execute(() -> takeAll(ctx.serverLevel())));
        ctx.thenServer(0, level -> loadsBefore[0] = ChunkLoadWatch.loadsBy(CORE));
        for (int i = 1; i <= TRIPS; i++) {
            final int far = i * TRIP;
            ctx.thenServer(i == 1 ? 0 : SETTLE * 3, level -> {
                takeAll(level);
                final BlockPos base = ctx.abs(BlockPos.ZERO).offset(far, 3, far);
                ctx.serverPlayer().teleportTo(level, base.getX() + 0.5, base.getY() + 2, base.getZ() + 0.5, 0, 0);
                for (final BlockPos at : BlockPos.betweenClosed(base, base.offset(2, 0, 2))) {
                    level.setBlockAndUpdate(at, Blocks.OAK_PLANKS.defaultBlockState());
                }
                MovingStructures.assemble(level, base, base.offset(2, 0, 2));
            });
        }
        ctx.thenServer(SETTLE * 4, MovingStructureClientTests::takeAll)
                .thenWaitUntilServer(level -> ChunkLoadWatch.loadsBy(CORE) == loadsBefore[0], 1,
                        "nothing of the Core to have loaded a chunk by reading it",
                        level -> (ChunkLoadWatch.loadsBy(CORE) - loadsBefore[0]) + " chunks loaded so")
                .thenSaveAndReload(20)
                .thenAwaitLandDrawn(WAIT * 2);
    }

    /*
     * The places of the structure's blocks, and of the shell a block thick round them a face of theirs takes its light
     * from, whose light the player's game has otherwise than the server, sky and blocks; none when it has it right.
     */
    private static String wronglyLit(final ClientTestContext ctx) {
        final ClientStructure seen = ClientMovingStructures.all().stream().findFirst().orElse(null);
        if (seen == null) {
            return "no structure on the player's game";
        }
        final Box box = seen.bounds();
        final List<BlockPos> places = new ArrayList<>();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX() - 1, box.minY() - 1, box.minZ() - 1,
                box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1)) {
            places.add(at.immutable());
        }
        final List<Integer> server = ctx.server().submit(() -> places.stream().map(at -> lightAt(ctx.serverLevel(),
                at)).toList()).join();
        final StringBuilder wrong = new StringBuilder();
        for (int i = 0; i < places.size(); i++) {
            final int game = lightAt(Minecraft.getInstance().level, places.get(i));
            if (game != server.get(i)) {
                wrong.append(places.get(i).toShortString()).append(" lit ").append(game >> 4).append('/')
                        .append(game & LIGHTEST).append(" for ").append(server.get(i) >> 4).append('/')
                        .append(server.get(i) & LIGHTEST).append("; ");
            }
        }
        return wrong.toString();
    }

    /* Whether the middle of the last frame drawn shows the sky: blue, far more than red. */
    private static boolean skyInTheMiddle(final ClientTestContext ctx) {
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            final int colour = frame.pixel(ctx.mc().getWindow().getGuiScaledWidth() / 2,
                    ctx.mc().getWindow().getGuiScaledHeight() / 2);
            return (colour & 0xFF) > (colour >> 16 & 0xFF) + SKY_BLUER;
        }
    }

    /* How many particles the player's game draws. */
    private static int particles() {
        return Integer.parseInt(Minecraft.getInstance().particleEngine.countParticles());
    }

    /*
     * Gives the torch aboard its chance to show its flame, as the game gives the blocks round the player, a few times
     * over: out in the yard, each flame and puff of smoke far past the reach the game draws particles in.
     */
    private static void torchFlames() {
        final ClientStructure seen = ClientMovingStructures.all().iterator().next();
        final Level level = Minecraft.getInstance().level;
        final Box box = seen.bounds();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            final BlockState state = level.getBlockState(at);
            if (state.is(Blocks.TORCH)) {
                final RandomSource random = RandomSource.create(1);
                for (int i = 0; i < FLAMES; i++) {
                    state.getBlock().animateTick(state, level, at.immutable(), random);
                }
                return;
            }
        }
        throw new ClientTestFailure("no torch among the structure's blocks on the player's game");
    }

    /* The sky's light at {@code at}, and the blocks', in one number: the sky's in the high four bits. */
    private static int lightAt(final Level level, final BlockPos at) {
        return level.getBrightness(LightLayer.SKY, at) << 4 | level.getBrightness(LightLayer.BLOCK, at);
    }

    /* Takes away every structure of the level, the run's own and any a run stopped short left in the test world. */
    private static void takeAll(final ServerLevel level) {
        for (final IStructureBody left : List.copyOf(LevelMovingStructures.of(level).all())) {
            if (left instanceof MovingStructure structure) {
                MovingStructures.takeAway(structure);
            }
        }
    }

    /* Where the chest is on the player's game and on the server, for a failure to say. */
    private static String chestSeen(final ClientTestContext ctx, final MovingStructure server) {
        final ClientStructure seen = ClientMovingStructures.all().iterator().next();
        BlockPos chest = null;
        for (final BlockPos at : BlockPos.betweenClosed(seen.bounds().minX(), seen.bounds().minY(),
                seen.bounds().minZ(), seen.bounds().maxX(), seen.bounds().maxY(), seen.bounds().maxZ())) {
            if (Minecraft.getInstance().level.getBlockState(at).is(Blocks.CHEST)) {
                chest = at.immutable();
            }
        }
        if (chest == null) {
            return "no chest among " + seen.bounds();
        }
        final double x = chest.getX() + 0.5;
        final double y = chest.getY() + 0.5;
        final double z = chest.getZ() + 0.5;
        final StructureFrame client = seen.frame();
        final StructureFrame host = server.frame();
        return "chest " + chest + " stands on the player's game at " + new Vec3(client.worldX(x, y, z),
                client.worldY(x, y, z), client.worldZ(x, y, z)) + " (pose " + client.pose() + ") and on the server at "
                + new Vec3(host.worldX(x, y, z), host.worldY(x, y, z), host.worldZ(x, y, z)) + ", sought at "
                + Vec3.atCenterOf(ctx.abs(CHEST)) + "; the entity's pivot on the player's game "
                + Arrays.toString(seen.entity().pivot()) + ", on the server "
                + Arrays.toString(server.entity().pivot());
    }
}
