/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.FrequencyResponse;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.audio.StereoSide;
import dev.jstech.core.audio.acoustics.ListenerAirPayload;
import dev.jstech.core.audio.acoustics.SoundMedium;
import dev.jstech.core.client.audio.AlertSigns;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.client.audio.CapturingAudioSink;
import dev.jstech.core.client.audio.acoustics.AcousticsClient;
import dev.jstech.core.client.audio.acoustics.SoundShaper;
import dev.jstech.core.client.audio.acoustics.Walls;
import dev.jstech.tests.TestSounds;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Map;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;

/**
 * How sound travels on a player's game: a playing sound's source is the sound card's own, which the effects are put
 * on; a vacuum carries only what is touched; a far sound arrives late; walls muffle it by what they are made of; a
 * pack gives a block's steps their sound, and a blast becomes its rumble far off and is shown on screen; a blast
 * close by rings the ears, unless a helmet protects them; the ambience and the layers of music follow what is near; a
 * closed room of stone echoes; a helmet's radio plays through its band; and the debugging shows it all.
 */
public final class AcousticsClientTests {

    private static final int FLOOR = 2;
    private static final BlockPos STAND = new BlockPos(0, FLOOR, 0);
    private static final int WAIT = 200;
    private static final int SETTLE = 10;
    /* The music set's second layer, which the deep blocks bring in. */
    private static final String BEACON = "minecraft:block.beacon.ambient";
    private static final ListenerAirPayload SEA_LEVEL = new ListenerAirPayload(
            (float) SoundMedium.SEA_LEVEL.pressure(), (float) SoundMedium.SEA_LEVEL.temperature(), 0F, 0F);

    private AcousticsClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void source_ofAPlayingSoundIsTheSoundCardsOwn(final ClientTestContext ctx) {
        final SoundInstance[] playing = new SoundInstance[1];
        ctx.afterTest(() -> ctx.mc().getSoundManager().stop(playing[0]));
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .then(SETTLE, () -> {
                    final Vec3 at = ctx.player().position();
                    playing[0] = new SimpleSoundInstance(SoundEvents.BEACON_AMBIENT.getLocation(), SoundSource.BLOCKS,
                            1F, 1F, SoundInstance.createUnseededRandom(), true, 0, SoundInstance.Attenuation.LINEAR,
                            at.x, at.y + 1, at.z, false);
                    ctx.mc().getSoundManager().play(playing[0]);
                })
                .thenWaitUntil(() -> !AcousticsClient.sources().isEmpty(), WAIT, "a sound to start on the sound card")
                .thenAssert(0, () -> AcousticsClient.sources().stream().allMatch(AL10::alIsSource),
                        "every playing sound's source, as the Core reaches it, is a source of the sound card");
    }

    @ClientTest(timeoutTicks = 200)
    public static void vacuum_carriesOnlyWhatTheListenerTouches(final ClientTestContext ctx) {
        ctx.afterTest(() -> AcousticsClient.told(SEA_LEVEL));
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .then(SETTLE, () -> AcousticsClient.told(new ListenerAirPayload(0F, -100F, 0F, 0F)))
                .thenAssert(0, () -> SoundShaper.shape(at(ctx, SoundEvents.NOTE_BLOCK_PLING, 0, 10)) == null,
                        "through a vacuum a sound ten blocks off is not heard")
                .thenAssert(0, () -> SoundShaper.shape(at(ctx, SoundEvents.NOTE_BLOCK_PLING, 0, 0.5)) != null,
                        "and what the listener touches still is")
                .then(0, () -> AcousticsClient.told(SEA_LEVEL))
                .thenAssert(0, () -> SoundShaper.shape(at(ctx, SoundEvents.NOTE_BLOCK_PLING, 0, 10)) != null,
                        "back in air it carries");
    }

    @ClientTest(timeoutTicks = 300)
    public static void distance_holdsAFarSoundBackUntilItArrives(final ClientTestContext ctx) {
        final String pling = SoundEvents.NOTE_BLOCK_PLING.value().getLocation().toString();
        final long[] asked = new long[1];
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .then(SETTLE, () -> asked[0] = System.currentTimeMillis())
                .thenAssert(0, () -> SoundShaper.shape(at(ctx, SoundEvents.NOTE_BLOCK_PLING, 0, 120)) == null,
                        "a sound a hundred and twenty blocks off is held back as it is asked for")
                .thenAssert(1, () -> !AudioMixer.recent(System.currentTimeMillis() - asked[0] + 1).contains(pling),
                        "and not heard at once")
                .thenWaitUntil(() -> AudioMixer.recent(System.currentTimeMillis() - asked[0] + 1).contains(pling),
                        WAIT, "the far sound to arrive, a third of a second later");
    }

    @ClientTest(timeoutTicks = 300)
    public static void walls_muffleASoundByWhatTheyAreMadeOf(final ClientTestContext ctx) {
        final BlockPos wall = new BlockPos(2, FLOOR + 3, 0);
        final float[] through = new float[4];
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().setBlockAndUpdate(ctx.abs(wall),
                Blocks.AIR.defaultBlockState())));
        ctx.thenTeleport(0, STAND, Direction.NORTH);
        final Block[] walls = {Blocks.IRON_BLOCK, Blocks.STONE, Blocks.WHITE_WOOL, Blocks.REINFORCED_DEEPSLATE};
        for (int i = 0; i < walls.length; i++) {
            final int which = i;
            ctx.thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(wall), walls[which].defaultBlockState()))
                    .thenWaitUntil(() -> ctx.mc().level.getBlockState(ctx.abs(wall)).is(walls[which]), WAIT,
                            "the wall to be " + walls[which])
                    .then(0, () -> through[which] = Walls.through(ctx.mc().level,
                            Vec3.atCenterOf(ctx.abs(wall.west(2))), Vec3.atCenterOf(ctx.abs(wall.east(2)))));
        }
        ctx.thenAssert(0, () -> through[0] > through[1] && through[1] > through[2] && through[2] > through[3],
                "metal lets most of a sound through, then stone, then wool, then a block a pack calls deaf; "
                        + through[0] + ", " + through[1] + ", " + through[2] + ", " + through[3]);
    }

    @ClientTest(timeoutTicks = 300)
    public static void materials_playWhatAPackGivesABlocksSteps(final ClientTestContext ctx) {
        final BlockPos floor = new BlockPos(2, FLOOR + 3, 2);
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverLevel().setBlockAndUpdate(ctx.abs(floor),
                Blocks.AIR.defaultBlockState())));
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(floor),
                        Blocks.REINFORCED_DEEPSLATE.defaultBlockState()))
                .thenWaitUntil(() -> ctx.mc().level.getBlockState(ctx.abs(floor)).is(Blocks.REINFORCED_DEEPSLATE),
                        WAIT, "the deaf block to be placed")
                .thenAssert(0, () -> {
                    final SoundEvent step = Blocks.REINFORCED_DEEPSLATE.defaultBlockState().getSoundType()
                            .getStepSound();
                    final Vec3 on = Vec3.atBottomCenterOf(ctx.abs(floor.above()));
                    final SoundInstance heard = SoundShaper.shape(new SimpleSoundInstance(step, SoundSource.BLOCKS,
                            0.15F, 1F, SoundInstance.createUnseededRandom(), on.x, on.y, on.z));
                    return heard != null && heard.getLocation().equals(
                            SoundEvents.AMETHYST_BLOCK_STEP.getLocation());
                }, "a step on the block plays the step the test pack gives it");
    }

    @ClientTest(timeoutTicks = 300)
    public static void blast_isARumbleFarOffAndShownOnScreen(final ClientTestContext ctx) {
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        final boolean echoes = prefs.acoustics();
        final boolean signs = prefs.visualCues();
        ctx.afterTest(() -> {
            prefs.setAcoustics(echoes);
            prefs.setVisualCues(signs);
        });
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .then(SETTLE, () -> {
                    // Heard at once, rather than as late as the far one would arrive.
                    prefs.setAcoustics(false);
                    prefs.setVisualCues(true);
                })
                .thenAssert(0, () -> located(SoundShaper.shape(at(ctx, TestSounds.BLAST.event().get(), 60, 0)),
                        TestSounds.RUMBLE.id()), "sixty blocks off the blast is its rumble")
                .thenAssert(0, () -> located(SoundShaper.shape(at(ctx, TestSounds.BLAST.event().get(), 10, 0)),
                        TestSounds.BLAST.id()), "ten blocks off, the blast itself")
                .thenAssert(0, () -> AlertSigns.showing().stream()
                                .anyMatch(sign -> sign.label().equals("A test blast")),
                        "and it matters, so its sign comes up for a player who asked for signs");
    }

    @ClientTest(timeoutTicks = 600)
    public static void ringing_followsABlastCloseByUnlessAHelmetProtectsTheEars(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.serverPlayer().setInvulnerable(false);
            ctx.serverPlayer().setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }));
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenServer(0, level -> {
                    ctx.serverPlayer().setInvulnerable(true);
                    ctx.serverPlayer().setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.TURTLE_HELMET));
                    blast(ctx, level);
                })
                .thenAssert(SETTLE, () -> !AcousticsClient.ringing(),
                        "a helmet that protects the ears keeps them quiet")
                .thenServer(0, level -> {
                    ctx.serverPlayer().setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    blast(ctx, level);
                })
                .thenWaitUntil(AcousticsClient::ringing, WAIT, "the ears to ring after a blast close by")
                .thenWaitUntil(() -> !AcousticsClient.ringing(), 2 * WAIT, "the ringing to fade");
    }

    @ClientTest(timeoutTicks = 800)
    public static void ambience_andMusicFollowWhatIsNear(final ClientTestContext ctx) {
        final BlockPos from = new BlockPos(2, FLOOR, -2);
        final BlockPos to = new BlockPos(4, FLOOR + 2, 2);
        ctx.afterTest(() -> ctx.server().execute(() -> {
            fill(ctx, ctx.serverLevel(), from, to, Blocks.AIR);
            fill(ctx, ctx.serverLevel(), from.west(6), to.west(6), Blocks.AIR);
        }));
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenServer(0, level -> fill(ctx, level, from, to, Blocks.REINFORCED_DEEPSLATE))
                .thenWaitUntil(() -> AcousticsClient.soundscapes().contains("jstests:test_deep"), WAIT,
                        "the deep ambience to play near its blocks")
                .thenServer(0, level -> fill(ctx, level, from.west(6), to.west(6), Blocks.END_PORTAL_FRAME))
                .thenWaitUntil(() -> "jstests:test_frames".equals(AcousticsClient.musicSet()), WAIT,
                        "the frames' music to play near them")
                .thenAssert(0, () -> {
                    final Map<String, Float> layers = AcousticsClient.musicLayers();
                    return layers.getOrDefault(BEACON, 0F) > 0.4F
                            && layers.getOrDefault("minecraft:block.conduit.ambient", 0F) > 0.7F;
                }, "both its layers in, the deep blocks bringing the second")
                .thenServer(0, level -> fill(ctx, level, from, to, Blocks.AIR))
                .thenWaitUntil(() -> !AcousticsClient.soundscapes().contains("jstests:test_deep")
                                && AcousticsClient.musicLayers().getOrDefault(BEACON, 1F) == 0F,
                        WAIT, "the ambience and the second layer to go with the deep blocks")
                .thenServer(0, level -> fill(ctx, level, from.west(6), to.west(6), Blocks.AIR))
                .thenWaitUntil(() -> AcousticsClient.musicSet().isEmpty(), WAIT, "the music to stop with its frames");
    }

    @ClientTest(timeoutTicks = 600)
    public static void echo_ringsInAClosedRoomOfStone(final ClientTestContext ctx) {
        // Its floor where the ground is, so the player stands inside it.
        final BlockPos low = new BlockPos(-3, FLOOR - 1, -3);
        final BlockPos high = new BlockPos(3, FLOOR + 4, 3);
        final double[] open = new double[1];
        // The walls and the roof go; the stone floor stays where the ground was.
        ctx.afterTest(() -> ctx.server().execute(() -> box(ctx, ctx.serverLevel(), low.above(), high, Blocks.AIR)));
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .then(2 * SETTLE + 20, () -> open[0] = AcousticsClient.echo()[1])
                .thenServer(0, level -> {
                    box(ctx, level, low, high, Blocks.STONE);
                    // Lit, so its walls and the debugging's rays are seen in the pictures.
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(0, high.getY(), 2)),
                            Blocks.GLOWSTONE.defaultBlockState());
                })
                .thenWaitUntil(() -> AcousticsClient.echo()[1] > Math.max(0.6, open[0] + 0.3), WAIT,
                        "the closed room of stone to echo")
                .thenAssert(0, () -> AcousticsClient.echo()[0] > 0.3, "and ring for a while")
                .thenScreenshot(0, "room_of_stone")
                .then(0, AcousticsClient::toggleDebug)
                .thenAssert(SETTLE, () -> AcousticsClient.debugShown() && AcousticsClient.debugLines().stream()
                        .anyMatch(line -> line.startsWith("Echo:")), "the debugging tells the echo")
                .thenScreenshot(0, "debug_rays")
                .then(0, AcousticsClient::toggleDebug);
    }

    @ClientTest(timeoutTicks = 200)
    public static void radio_playsACueFromInsideTheHelmetThroughItsBand(final ClientTestContext ctx) {
        final CapturingAudioSink sink = new CapturingAudioSink();
        ctx.afterTest(AudioEngine::restoreSink);
        ctx.then(0, () -> {
                    AudioEngine.useSink(sink);
                    AudioEngine.playCue(TestSounds.RADIO, SoundContext.EMPTY, 0, 0, 0, 1F, 1F, StereoSide.BOTH,
                            FrequencyResponse.RADIO);
                })
                .thenAssert(1, () -> sink.played().size() == 1 && sink.played().getFirst().isRelative()
                        && !(sink.played().getFirst() instanceof SimpleSoundInstance),
                        "the call plays from inside the player's head, read through the radio's band");
    }

    /* A sound of {@code event}, {@code east} blocks east of the player's eyes and {@code up} blocks above them. */
    private static SoundInstance at(final ClientTestContext ctx, final SoundEvent event, final double east,
                                    final double up) {
        final Vec3 eyes = ctx.mc().gameRenderer.getMainCamera().getPosition();
        return new SimpleSoundInstance(event, SoundSource.BLOCKS, 1F, 1F, SoundInstance.createUnseededRandom(),
                eyes.x + east, eyes.y + up, eyes.z);
    }

    private static SoundInstance at(final ClientTestContext ctx, final Holder<SoundEvent> event, final double east,
                                    final double up) {
        return at(ctx, event.value(), east, up);
    }

    private static boolean located(final SoundInstance sound, final ResourceLocation id) {
        return sound != null && sound.getLocation().equals(id);
    }

    /* A blast of power two a block and a half from the player, which breaks nothing. */
    private static void blast(final ClientTestContext ctx, final ServerLevel level) {
        final Vec3 at = ctx.serverPlayer().getEyePosition().add(1.5, 0, 0);
        level.explode(null, at.x, at.y, at.z, 2F, Level.ExplosionInteraction.NONE);
    }

    private static void fill(final ClientTestContext ctx, final ServerLevel level, final BlockPos from,
                             final BlockPos to, final Block block) {
        for (final BlockPos pos : BlockPos.betweenClosed(ctx.abs(from), ctx.abs(to))) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
        }
    }

    /* The walls, floor and roof of a box from {@code low} to {@code high}, of {@code block}. */
    private static void box(final ClientTestContext ctx, final ServerLevel level, final BlockPos low,
                            final BlockPos high, final Block block) {
        for (final BlockPos pos : BlockPos.betweenClosed(ctx.abs(low), ctx.abs(high))) {
            final BlockPos rel = pos.subtract(ctx.origin());
            if (rel.getX() == low.getX() || rel.getX() == high.getX() || rel.getY() == low.getY()
                    || rel.getY() == high.getY() || rel.getZ() == low.getZ() || rel.getZ() == high.getZ()) {
                level.setBlockAndUpdate(pos, block.defaultBlockState());
            }
        }
    }
}
