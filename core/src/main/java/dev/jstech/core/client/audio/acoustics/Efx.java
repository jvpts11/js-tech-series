/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import com.mojang.logging.LogUtils;
import dev.jstech.core.audio.acoustics.Reverb;
import dev.jstech.core.stepaside.StepAside;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import org.slf4j.Logger;

/**
 * The sound card's effects, through OpenAL's effects extension: one echo for the place the listener stands in, which
 * every sound of the world is sent into, and the filters that take a sound's loudness and its high notes away, for
 * the walls between, the air and the medium. Made when the game's sound engine is first there and made again whenever
 * it starts over; where the extension is missing, or a mod of sound physics gives the echoes
 * ({@link StepAside#SOUND_PHYSICS}), there are none.
 *
 * <p>The game plays each sound on a thread of its own, and the Core changes playing sounds from the game's: both only
 * set a source's parameters, which OpenAL keeps safe between threads, and the filter they share is filled and applied
 * under one lock.
 */
final class Efx {

    private static final Logger LOGGER = LogUtils.getLogger();
    /* An echo's density and how diffuse it is: full, a room's. */
    private static final float DENSITY = 1.0F;
    private static final float DIFFUSION = 1.0F;

    private static long context;
    private static int slot;
    private static int echo;
    private static int filter;
    private static boolean made;
    private static boolean missing;

    private Efx() {
    }

    /** Whether the effects are there to use: made, for the sound engine running now. */
    static synchronized boolean ready() {
        final long current = ALC10.alcGetCurrentContext();
        if (current == 0L) {
            return false;
        }
        if (current != context) {
            context = current;
            made = false;
            missing = false;
        }
        if (!made && !missing) {
            make(current);
        }
        return made;
    }

    /** Forgets the effects, as the game's sound engine starts over: they are made again for the new one. */
    static synchronized void forget() {
        context = 0L;
        made = false;
        missing = false;
    }

    /** Sets the echo of the place the listener stands in. */
    static synchronized void echo(final Reverb reverb) {
        if (!ready()) {
            return;
        }
        EXTEfx.alEffectf(echo, EXTEfx.AL_EAXREVERB_DECAY_TIME, (float) reverb.decaySeconds());
        EXTEfx.alEffectf(echo, EXTEfx.AL_EAXREVERB_REFLECTIONS_DELAY, (float) reverb.reflectionsDelay());
        EXTEfx.alEffectf(echo, EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY, (float) reverb.lateDelay());
        EXTEfx.alEffectf(echo, EXTEfx.AL_EAXREVERB_DECAY_HFRATIO, (float) Math.clamp(reverb.highRatio(), 0.1, 2.0));
        // The slot copies its effect when the effect is put in it, so a changed echo is put in again.
        EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, echo);
        EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, (float) reverb.wet());
        AL10.alGetError();
    }

    /**
     * Shapes the playing source {@code source}: {@code gain} and {@code highs} of what reaches the listener straight,
     * {@code sendGain} and {@code sendHighs} of what goes into the place's echo, and how much the air takes away
     * with distance. A source the game has since let go is left alone.
     */
    static synchronized void shape(final int source, final float gain, final float highs, final float sendGain,
                                   final float sendHighs, final float airAbsorption) {
        if (!ready() || !AL10.alIsSource(source)) {
            return;
        }
        lowpass(gain, highs);
        AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, filter);
        lowpass(sendGain, sendHighs);
        AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, slot, 0, filter);
        AL10.alSourcef(source, EXTEfx.AL_AIR_ABSORPTION_FACTOR, Math.clamp(airAbsorption, 0F, 10F));
        AL10.alGetError();
    }

    /** Sets how fast the playing source {@code source} moves, in metres a second, for the Doppler effect. */
    static synchronized void velocity(final int source, final float x, final float y, final float z) {
        if (AL10.alIsSource(source)) {
            AL10.alSource3f(source, AL10.AL_VELOCITY, x, y, z);
            AL10.alGetError();
        }
    }

    /** Sets how fast the listener moves, and how fast sound goes where they are, in metres a second. */
    static synchronized void listener(final float x, final float y, final float z, final float speedOfSound) {
        if (ALC10.alcGetCurrentContext() == 0L) {
            return;
        }
        AL10.alListener3f(AL10.AL_VELOCITY, x, y, z);
        if (speedOfSound > 0) {
            AL11.alSpeedOfSound(speedOfSound);
        }
        AL10.alGetError();
    }

    /** Whether the echoes are the Core's here: not while a mod of sound physics gives its own. */
    static boolean echoesOurs() {
        return !StepAside.SOUND_PHYSICS.steps();
    }

    private static void make(final long current) {
        final long device = ALC10.alcGetContextsDevice(current);
        if (device == 0L || !ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX")) {
            missing = true;
            LOGGER.info("The sound card offers no effects: places give no echo and walls only quiet a sound");
            return;
        }
        AL10.alGetError();
        slot = EXTEfx.alGenAuxiliaryEffectSlots();
        echo = EXTEfx.alGenEffects();
        filter = EXTEfx.alGenFilters();
        EXTEfx.alEffecti(echo, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);
        EXTEfx.alEffectf(echo, EXTEfx.AL_EAXREVERB_DENSITY, DENSITY);
        EXTEfx.alEffectf(echo, EXTEfx.AL_EAXREVERB_DIFFUSION, DIFFUSION);
        EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);
        EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, 0F);
        EXTEfx.alFilteri(filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
        final int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            missing = true;
            LOGGER.warn("The sound card's effects could not be made (OpenAL error {}): places give no echo", error);
            return;
        }
        made = true;
    }

    /* Fills the shared filter: what it lets through of a sound, and of its high notes. */
    private static void lowpass(final float gain, final float highs) {
        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN, Math.clamp(gain, 0F, 1F));
        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF, Math.clamp(highs, 0F, 1F));
    }
}
