/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.AudioDevice;
import dev.jstech.core.audio.AudioDevices;
import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.AudioPrefsJson;
import dev.jstech.core.audio.CueSoundPayload;
import dev.jstech.core.audio.IAudioHost;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.audio.SoundCues;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundKeys;
import dev.jstech.core.audio.SoundSet;
import dev.jstech.core.audio.SoundSetJson;
import dev.jstech.core.audio.SoundSpace;
import dev.jstech.core.audio.ToneSoundPayload;
import dev.jstech.core.audio.pcm.Tone;
import dev.jstech.core.audio.pcm.Waveform;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSounds;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The series' sounds as the server has them: every declared sound is a registered event with its subtitle in the
 * English and a channel that exists, and the same sound from the same place is played once in a short while. A sound
 * made as it plays has no event, goes out as its notes and comes back from the wire as it went. A cue is raised where
 * it belongs, its rules survive their file, and a machine with hardware of its own plays through it or stays silent.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class AudioGameTests {

    private static final String ARENA = "empty";

    private AudioGameTests() {
    }

    @GameTest(template = ARENA)
    public static void declared_everySoundIsRegisteredSubtitledAndInAChannel(final GameTestHelper helper) {
        helper.assertTrue(SoundKeys.all().contains(TestSounds.CLICK) && SoundKeys.all().contains(TestSounds.HUM)
                        && SoundKeys.all().contains(TestSounds.BEEP),
                "the test mod's sounds are among the declared ones");
        for (final SoundKey sound : SoundKeys.all()) {
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(sound.id()) != sound.spec().made(),
                    sound.id() + " is registered as a sound event when it plays files, and not when it is made");
            helper.assertTrue(Language.getInstance().has(sound.subtitle().key()),
                    sound.id() + " has its subtitle in the English: " + sound.subtitle().key());
            helper.assertTrue(AudioChannels.find(sound.spec().channel().id()) != null,
                    sound.id() + " is mixed in a declared channel");
            helper.assertTrue(sound.spec().space() == SoundSpace.WORLD || !sound.spec().loop()
                            || sound.spec().channel() == AudioChannels.MUSIC,
                    sound.id() + " does not loop on a screen, which only music may do");
        }
        helper.assertTrue(SoundKeys.find(TestSounds.CLICK.id()) == TestSounds.CLICK, "a sound is found by its id");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void at_playsASoundFromOnePlaceOnceInAShortWhile(final GameTestHelper helper) {
        final BlockPos where = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(Audio.at(helper.getLevel(), where, TestSounds.HUM), "the first time it is played");
        helper.assertFalse(Audio.at(helper.getLevel(), where, TestSounds.HUM),
                "the same sound from the same place in the same tick is not played again");
        helper.assertTrue(Audio.at(helper.getLevel(), where.above(2), TestSounds.HUM),
                "but from another place it is");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(Audio.at(helper.getLevel(), where, TestSounds.HUM),
                    "and from the first place again once the moment has passed");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void prefs_readBackWhatTheirFileKeptAndForgiveWhatIsSpoiled(final GameTestHelper helper) {
        final AudioPrefs prefs = new AudioPrefs();
        prefs.setVolume("jscore:machines", 0.5F);
        prefs.setMuted("jsc:computer/fan", true);
        prefs.setVisualCues(true);
        prefs.setOcclusion(false);
        prefs.setDucking(false);
        final AudioPrefs back = AudioPrefsJson.read(AudioPrefsJson.write(prefs));
        helper.assertTrue(back.volume("jscore:machines") == 0.5F && back.isMuted("jsc:computer/fan")
                && back.visualCues() && !back.occlusion() && !back.ducking(),
                "what the file kept comes back as it was");
        final AudioPrefs broken = AudioPrefsJson.read("{ this is not json");
        helper.assertTrue(broken.volume("jscore:machines") == 1.0F && broken.occlusion() && broken.ducking(),
                "a file that cannot be read gives the defaults");
        final AudioPrefs odd = AudioPrefsJson.read("{\"volumes\": {\"jscore:machines\": \"loud\"}, \"muted\": 4}");
        helper.assertTrue(odd.volume("jscore:machines") == 1.0F && odd.mutedSounds().isEmpty(),
                "and what is written wrongly is left at its default");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void at_refusesASoundOfTheScreen(final GameTestHelper helper) {
        try {
            Audio.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), TestSounds.CLICK);
            helper.fail("a sound of the interface has no place in the world to be played from");
        } catch (final IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    @GameTest(template = ARENA)
    public static void tones_goOnlyAsASoundMadeAsItPlaysWithAFewNotes(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos where = helper.absolutePos(new BlockPos(1, 2, 1));
        final List<Tone> notes = List.of(Tone.beep(880, 120), Tone.rest(40), Tone.beep(660, 120));
        Audio.tones(level, where, TestSounds.BEEP, notes);
        refused(helper, () -> Audio.tones(level, where, TestSounds.HUM, notes), "a sound that plays its own files");
        refused(helper, () -> Audio.tones(level, where, TestSounds.TUNE, notes), "a made sound of the interface");
        refused(helper, () -> Audio.tones(level, where, TestSounds.BEEP, List.of()), "no notes at all");
        refused(helper, () -> Audio.tones(level, where, TestSounds.BEEP,
                Collections.nCopies(ToneSoundPayload.MAX_TONES + 1, Tone.beep(440, 10))), "more notes than one send");
        refused(helper, () -> Audio.at(level, where, TestSounds.BEEP), "a made sound played as if it had a file");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void tonePayload_comesBackFromTheWireAsItWent(final GameTestHelper helper) {
        final ToneSoundPayload sent = new ToneSoundPayload(TestSounds.BEEP.id(), false, 10.5, 64.0, -3.25,
                List.of(new Tone(Waveform.TRIANGLE, 523.25, 200, 0.75F), Tone.rest(50), Tone.beep(1046.5, 90)));
        final RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        try {
            ToneSoundPayload.STREAM_CODEC.encode(wire, sent);
            helper.assertTrue(sent.equals(ToneSoundPayload.STREAM_CODEC.decode(wire)),
                    "the notes, the sound and the place arrive as they were sent");
            helper.assertTrue(wire.readableBytes() == 0, "and nothing is left over on the wire");
        } finally {
            wire.release();
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cue_isRaisedWhereItBelongsOnceInAShortWhile(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos where = helper.absolutePos(new BlockPos(2, 2, 2));
        final SoundContext vintage = SoundContext.EMPTY.with(SoundContext.ERA, "vintage");
        helper.assertTrue(SoundCues.find(TestSounds.ALARM.id()) == TestSounds.ALARM, "a cue is found by its id");
        helper.assertTrue(Audio.cue(level, where, TestSounds.ALARM, vintage), "the first time it is raised");
        helper.assertFalse(Audio.cue(level, where, TestSounds.ALARM, vintage),
                "the same cue from the same place in the same tick is not raised again");
        refused(helper, () -> Audio.cueOnScreen(FakePlayerFactory.getMinecraft(level), TestSounds.ALARM, vintage),
                "a cue of the world raised on a screen");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cueFile_readsBackWhatItWroteAndSaysWhatIsWrong(final GameTestHelper helper) {
        final SoundSet written = TestSounds.ALARM.defaults();
        final JsonObject file = SoundSetJson.write(written);
        helper.assertTrue(SoundSetJson.read(file).equals(written), "the rules come back from their file as they were");
        helper.assertTrue(file.getAsJsonArray("sounds").size() == 2
                        && file.getAsJsonArray("sounds").get(0).getAsJsonObject().has("when")
                        && !file.getAsJsonArray("sounds").get(1).getAsJsonObject().has("when"),
                "a rule with no condition is written without one");
        for (final String broken : List.of("[]", "{}", "{\"sounds\": {}}", "{\"sounds\": [4]}",
                "{\"sounds\": [{\"when\": {}}]}", "{\"sounds\": [{\"sound\": \"a:b\", \"when\": []}]}",
                "{\"sounds\": [{\"sound\": \"a:b\", \"when\": {\"era\": {}}}]}")) {
            refused(helper, () -> SoundSetJson.read(JsonParser.parseString(broken)), "the file " + broken);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cuePayload_comesBackFromTheWireAsItWent(final GameTestHelper helper) {
        final CueSoundPayload sent = new CueSoundPayload(TestSounds.ALARM.id(), false, 1.5, 70.0, -8.0,
                new SoundContext(Map.of(SoundContext.ERA, "legacy", SoundContext.DEVICE, TestSounds.BUZZER.id())),
                0.8F, 1.25F);
        final RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        try {
            CueSoundPayload.STREAM_CODEC.encode(wire, sent);
            helper.assertTrue(sent.equals(CueSoundPayload.STREAM_CODEC.decode(wire)),
                    "the cue, its context, the place, the volume and the pitch arrive as they were sent");
            helper.assertTrue(wire.readableBytes() == 0, "and nothing is left over on the wire");
        } finally {
            wire.release();
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void host_playsFromEachSpeakerThroughItsDeviceOrNotAtAll(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Vec3 left = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        final Vec3 right = Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 1)));
        final List<Tone> tune = List.of(new Tone(Waveform.TRIANGLE, 440, 100, 0.8F), Tone.beep(660, 100));
        helper.assertTrue(Audio.cue(level, new Host(TestSounds.BUZZER, 0.5F, List.of(left, right)),
                TestSounds.ALARM) == 2, "a machine with two speakers raises the cue from both");
        helper.assertTrue(Audio.tones(level, new Host(TestSounds.BUZZER, 0.5F, List.of(left)), TestSounds.BEEP,
                tune) == 1, "and plays notes from its speaker");
        helper.assertTrue(Audio.cue(level, new Host(AudioDevices.NONE, 1.0F, List.of(left)), TestSounds.ALARM) == 0
                        && Audio.tones(level, new Host(AudioDevices.NONE, 1.0F, List.of(left)), TestSounds.BEEP,
                        tune) == 0, "a machine with no sound hardware is silent");
        helper.assertTrue(Audio.cue(level, new Host(TestSounds.BUZZER, 0.0F, List.of(right.add(0, 3, 0))),
                TestSounds.ALARM) == 0, "and so is one whose volume is down");
        helper.succeed();
    }

    /** A machine playing through a device, at a volume, from its speakers. */
    private record Host(AudioDevice audioDevice, float audioVolume, List<Vec3> audioOutputs) implements IAudioHost {

        @Override
        public SoundContext soundContext() {
            return SoundContext.EMPTY.with(SoundContext.ERA, "vintage");
        }
    }

    private static void refused(final GameTestHelper helper, final Runnable call, final String what) {
        try {
            call.run();
            helper.fail(what + " is refused");
        } catch (final IllegalArgumentException expected) {
            // what is wanted
        }
    }
}
