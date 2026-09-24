/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.AudioPrefs;
import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.client.audio.mixer.SoundMixerScreen;
import dev.jstech.core.gui.layout.SoundMixerLayout;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

/**
 * The Sound Mixer as a player uses it: reached from the game's own Music and Sound Options, a slider per channel
 * that sets the channel's volume and a reset, a list of every sound to turn any of them off and back on, and the
 * options, whose key button opens the game's controls and comes back to the options.
 */
public final class SoundMixerClientTests {

    private static final String MACHINES = AudioChannels.MACHINES.id().toString();
    private static final String BELL = SoundEvents.NOTE_BLOCK_BELL.value().getLocation().toString();

    private SoundMixerClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void entry_standsBesideDoneInTheGameSoundOptionsAndOpensTheMixer(final ClientTestContext ctx) {
        final String open = GameText.component(SoundMixerTexts.OPEN).getString();
        final String done = CommonComponents.GUI_DONE.getString();
        ctx.then(0, () -> ctx.mc().setScreen(new SoundOptionsScreen(null, ctx.mc().options)))
                .thenAwaitScreen(SoundOptionsScreen.class, 40)
                .thenAssert(1, () -> {
                    final Screen screen = ctx.screen(SoundOptionsScreen.class);
                    final Button mixer = shown(screen, open);
                    final Button close = shown(screen, done);
                    return mixer != null && close != null && mixer.getWidth() == SoundMixerLayout.COLUMN
                            && close.getWidth() == SoundMixerLayout.COLUMN && mixer.getY() == close.getY()
                            && mixer.getX() == screen.width / 2 - 154 && close.getX() == mixer.getX() + 158;
                }, "Sound Mixer... and Done stand side by side in the footer, 150 wide and 8 apart, centred")
                .thenScreenshot(2, "sound-options")
                .then(0, () -> {
                    final Button mixer = shown(ctx.screen(SoundOptionsScreen.class), open);
                    ctx.click(mixer.getX() + 4, mixer.getY() + 4);
                })
                .thenAwaitScreen(SoundMixerScreen.class, 40)
                .then(0, () -> ctx.mc().setScreen(null));
    }

    @ClientTest(timeoutTicks = 400)
    public static void channels_setTheVolumeTheirSliderShowsAndResetToWhole(final ClientTestContext ctx) {
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        ctx.then(0, () -> ctx.mc().setScreen(new SoundMixerScreen(null)))
                .thenAwaitScreen(SoundMixerScreen.class, 40)
                .thenAssert(1, () -> {
                    final Screen screen = ctx.screen(SoundMixerScreen.class);
                    final List<AbstractSliderButton> sliders = of(screen, AbstractSliderButton.class);
                    return sliders.size() == AudioChannels.all().size()
                            && sliders.getFirst().getX() == SoundMixerLayout.left(screen.width)
                            && sliders.getFirst().getY() == SoundMixerLayout.CONTENT_TOP
                            && sliders.getFirst().getMessage().getString().equals("Machines: 100%");
                }, "a slider per channel, the first where the approved screen has it, at 100%")
                .then(0, () -> {
                    final AbstractSliderButton slider = of(ctx.screen(SoundMixerScreen.class),
                            AbstractSliderButton.class).getFirst();
                    ctx.pointAt(slider.getX() + 35, slider.getY() + 10);
                })
                .thenScreenshot(30, "channels")
                .then(0, () -> {
                    final AbstractSliderButton slider = of(ctx.screen(SoundMixerScreen.class),
                            AbstractSliderButton.class).getFirst();
                    ctx.click(slider.getX() + 4 + (slider.getWidth() - 8) * 0.5, slider.getY() + 10);
                })
                .thenAssert(1, () -> Math.abs(prefs.volume(MACHINES) - 0.5F) < 0.02F
                                && of(ctx.screen(SoundMixerScreen.class), AbstractSliderButton.class).getFirst()
                                .getMessage().getString().equals("Machines: 50%"),
                        "clicking the middle of the machines' slider sets them to half and says so")
                .then(0, () -> shown(ctx.screen(SoundMixerScreen.class),
                        GameText.component(SoundMixerTexts.RESET_CHANNELS).getString()).onPress())
                .thenAssert(1, () -> prefs.volume(MACHINES) == 1.0F, "and Reset Channels sets them back to whole")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    @ClientTest(timeoutTicks = 400)
    public static void sounds_listEveryKnownSoundToTurnOffAndBackOn(final ClientTestContext ctx) {
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        ctx.then(0, () -> {
                    prefs.setMuted(BELL, false);
                    ctx.mc().setScreen(new SoundMixerScreen(null));
                })
                .thenAwaitScreen(SoundMixerScreen.class, 40)
                .then(0, () -> ctx.screen(SoundMixerScreen.class).selectTab(1))
                .thenScreenshot(2, "sounds")
                .then(1, () -> of(ctx.screen(SoundMixerScreen.class), EditBox.class).getFirst()
                        .setValue("note_block.bell"))
                .thenAssert(1, () -> rows(ctx.screen(SoundMixerScreen.class)).size() == 1,
                        "searching by id finds the bell alone")
                .then(0, () -> toggle(rows(ctx.screen(SoundMixerScreen.class)).getFirst()).onPress())
                .thenAssert(1, () -> prefs.isMuted(BELL)
                                && toggle(rows(ctx.screen(SoundMixerScreen.class)).getFirst()).getMessage()
                                .equals(CommonComponents.OPTION_OFF)
                                && count(ctx.screen(SoundMixerScreen.class))
                                .startsWith(prefs.mutedSounds().size() + " of "),
                        "its OFF turns the bell off, the button says so and the count has it")
                .then(0, () -> shown(ctx.screen(SoundMixerScreen.class),
                        GameText.component(SoundMixerTexts.TURN_ALL_BACK_ON).getString()).onPress())
                .thenAssert(1, () -> !prefs.isMuted(BELL) && prefs.mutedSounds().isEmpty(),
                        "and Turn All Back On brings every sound back")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    @ClientTest(timeoutTicks = 400)
    public static void options_keepTheirChoiceAndTheKeyComesBackToThem(final ClientTestContext ctx) {
        final AudioPrefs prefs = AudioPrefsStore.prefs();
        final String muffle = GameText.component(SoundMixerTexts.MUFFLE).getString();
        final String options = GameText.component(SoundMixerTexts.TAB_OPTIONS).getString();
        ctx.then(0, () -> ctx.mc().setScreen(new SoundMixerScreen(null)))
                .thenAwaitScreen(SoundMixerScreen.class, 40)
                .then(0, () -> ctx.screen(SoundMixerScreen.class).selectTab(2))
                .thenScreenshot(2, "options")
                .then(1, () -> starting(ctx.screen(SoundMixerScreen.class), muffle).onPress())
                .thenAssert(1, () -> !prefs.occlusion(), "Muffle Behind Walls turns off")
                .then(0, () -> starting(ctx.screen(SoundMixerScreen.class), muffle).onPress())
                .thenAssert(1, () -> prefs.occlusion(), "and back on")
                .then(0, () -> starting(ctx.screen(SoundMixerScreen.class), "Turn Off Last Sound").onPress())
                .thenAwaitScreen(KeyBindsScreen.class, 40)
                .then(0, () -> ctx.screen(KeyBindsScreen.class).onClose())
                .thenAwaitScreen(SoundMixerScreen.class, 40)
                .thenAssert(1, () -> ctx.screen(SoundMixerScreen.class).currentTab() != null
                                && ctx.screen(SoundMixerScreen.class).currentTab().getTabTitle().getString()
                                .equals(options),
                        "the key opens the game's controls, which come back to the mixer's options")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    @Nullable
    private static Button shown(final Screen screen, final String message) {
        for (final Button button : of(screen, Button.class)) {
            if (button.visible && button.getMessage().getString().equals(message)) {
                return button;
            }
        }
        return null;
    }

    /* By the start of what it says, a cycling button ("Muffle Behind Walls: ON") included. */
    @Nullable
    private static AbstractButton starting(final Screen screen, final String caption) {
        for (final AbstractButton button : of(screen, AbstractButton.class)) {
            if (button.visible && button.getMessage().getString().startsWith(caption)) {
                return button;
            }
        }
        return null;
    }

    private static <T> List<T> of(final Screen screen, final Class<T> type) {
        final List<T> out = new ArrayList<>();
        for (final GuiEventListener child : screen.children()) {
            if (type.isInstance(child)) {
                out.add(type.cast(child));
            }
        }
        return out;
    }

    private static List<? extends GuiEventListener> rows(final Screen screen) {
        for (final GuiEventListener child : screen.children()) {
            if (child instanceof AbstractSelectionList<?> list) {
                return list.children();
            }
        }
        return List.of();
    }

    private static Button toggle(final GuiEventListener row) {
        return (Button) ((ContainerEventHandler) row).children().get(1);
    }

    private static String count(final Screen screen) {
        for (final AbstractWidget text : of(screen, StringWidget.class)) {
            return text.getMessage().getString();
        }
        return "";
    }
}
