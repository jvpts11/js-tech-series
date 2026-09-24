/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.AudioPrefsJson;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.audio.SoundKeys;
import dev.jstech.core.audio.SoundSpace;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The series' sounds as the server has them: every declared sound is a registered event with its subtitle in the
 * English and a channel that exists, and the same sound from the same place is played once in a short while.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class AudioGameTests {

    private static final String ARENA = "empty";

    private AudioGameTests() {
    }

    @GameTest(template = ARENA)
    public static void declared_everySoundIsRegisteredSubtitledAndInAChannel(final GameTestHelper helper) {
        helper.assertTrue(SoundKeys.all().contains(TestSounds.CLICK) && SoundKeys.all().contains(TestSounds.HUM),
                "the test mod's sounds are among the declared ones");
        for (final SoundKey sound : SoundKeys.all()) {
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(sound.id()),
                    sound.id() + " is registered as a sound event");
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
        final AudioPrefs back = AudioPrefsJson.read(AudioPrefsJson.write(prefs));
        helper.assertTrue(back.volume("jscore:machines") == 0.5F && back.isMuted("jsc:computer/fan")
                && back.visualCues() && !back.occlusion(), "what the file kept comes back as it was");
        final AudioPrefs broken = AudioPrefsJson.read("{ this is not json");
        helper.assertTrue(broken.volume("jscore:machines") == 1.0F && broken.occlusion(),
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
}
