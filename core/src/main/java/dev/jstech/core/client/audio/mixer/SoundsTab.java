/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.mixer;

import dev.jstech.core.audio.SoundMixerTexts;
import dev.jstech.core.client.GameLocale;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.audio.AudioPrefsStore;
import dev.jstech.core.gui.layout.SoundMixerLayout;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The Sounds tab: every sound the game knows, searchable by name or id, shown all together or by a filter, each with
 * a button to hear it once and one to turn it off; the count of those turned off, and a way to turn them all back on.
 */
final class SoundsTab implements Tab {

    private final List<SoundEntry> everything;
    private final SearchFrame frame;
    private final EditBox search;
    private final StringWidget count;
    private final CycleButton<SoundFilter> show;
    private final Button allOn;
    private final SoundList list;

    /** How long ago a sound may have been heard and still count as recent. */
    private static final long RECENT_MILLIS = 60_000L;

    SoundsTab(final Minecraft minecraft, final Font font) {
        everything = SoundEntry.all(minecraft.getSoundManager());
        frame = new SearchFrame();
        search = new EditBox(font, 0, 0, SoundMixerLayout.FULL - SoundMixerLayout.SEARCH_TEXT_INSET - 4, 12,
                GameText.component(SoundMixerTexts.SEARCH_HINT));
        search.setBordered(false);
        search.setHint(GameText.component(SoundMixerTexts.SEARCH_HINT).withColor(SoundMixerPalette.get().hint()));
        search.setResponder(query -> refresh());
        count = new StringWidget(SoundMixerLayout.SHOW_X - 4, 9, CommonComponents.EMPTY, font);
        count.setColor(SoundMixerPalette.get().note());
        count.alignLeft();
        show = CycleButton.builder((SoundFilter filter) -> GameText.component(filter.label()))
                .withValues(List.of(SoundFilter.values())).withInitialValue(SoundFilter.ALL)
                .create(0, 0, SoundMixerLayout.SHOW_WIDTH, SoundMixerLayout.CONTROL_HEIGHT,
                        GameText.component(SoundMixerTexts.SHOW), (button, filter) -> refresh());
        allOn = Button.builder(GameText.component(SoundMixerTexts.TURN_ALL_BACK_ON), button -> turnAllBackOn())
                .size(SoundMixerLayout.ALL_ON_WIDTH, SoundMixerLayout.CONTROL_HEIGHT).build();
        list = new SoundList(minecraft, font, this::counted);
        refresh();
    }

    /** The list, for a test to reach its rows. */
    SoundList list() {
        return list;
    }

    EditBox search() {
        return search;
    }

    CycleButton<SoundFilter> show() {
        return show;
    }

    Button allOn() {
        return allOn;
    }

    /** How many sounds the game knows, which the count is out of. */
    int known() {
        return everything.size();
    }

    @Override
    public Component getTabTitle() {
        return GameText.component(SoundMixerTexts.TAB_SOUNDS);
    }

    @Override
    public void visitChildren(final Consumer<AbstractWidget> visitor) {
        visitor.accept(frame);
        visitor.accept(search);
        visitor.accept(count);
        visitor.accept(show);
        visitor.accept(allOn);
        visitor.accept(list);
    }

    @Override
    public void doLayout(final ScreenRectangle area) {
        final int width = area.width();
        final int left = SoundMixerLayout.left(width);
        frame.setPosition(left, SoundMixerLayout.SEARCH_TOP);
        search.setPosition(left + SoundMixerLayout.SEARCH_TEXT_INSET, SoundMixerLayout.SEARCH_TOP + 4);
        count.setPosition(left, SoundMixerLayout.COUNT_TOP);
        show.setPosition(left + SoundMixerLayout.SHOW_X, SoundMixerLayout.FILTER_TOP);
        allOn.setPosition(left + SoundMixerLayout.ALL_ON_X, SoundMixerLayout.FILTER_TOP);
        final int bottom = SoundMixerLayout.listBottom(area.bottom() + SoundMixerLayout.FOOTER);
        list.updateSizeAndPosition(width, bottom - SoundMixerLayout.LIST_TOP, SoundMixerLayout.LIST_TOP);
    }

    /* The list as the search and the filter have it now. */
    private void refresh() {
        final String query = search.getValue();
        final List<SoundEntry> shown = new ArrayList<>();
        if (show.getValue() == SoundFilter.RECENT) {
            final Map<String, SoundEntry> byId = new HashMap<>();
            everything.forEach(one -> byId.put(one.id().toString(), one));
            for (final String id : AudioMixer.recent(RECENT_MILLIS)) {
                final SoundEntry one = byId.get(id);
                if (one != null && one.matches(query)) {
                    shown.add(one);
                }
            }
        } else {
            for (final SoundEntry one : everything) {
                if (one.matches(query) && passes(one, show.getValue())) {
                    shown.add(one);
                }
            }
        }
        list.show(shown);
        counted();
    }

    private void counted() {
        count.setMessage(GameText.component(SoundMixerTexts.TURNED_OFF_COUNT.with(
                GameLocale.count(AudioPrefsStore.prefs().mutedSounds().size()), GameLocale.count(everything.size()))));
    }

    private void turnAllBackOn() {
        for (final String sound : AudioPrefsStore.prefs().mutedSounds()) {
            AudioPrefsStore.prefs().setMuted(sound, false);
        }
        AudioPrefsStore.save();
        refresh();
    }

    private static boolean passes(final SoundEntry one, final SoundFilter filter) {
        return switch (filter) {
            case ALL, RECENT -> true;
            case SERIES -> one.series();
            case GAME -> !one.series();
            case TURNED_OFF -> AudioPrefsStore.prefs().isMuted(one.id().toString());
        };
    }

    /** The search field's frame and magnifying glass, the typed text sitting beside the glass inside the frame. */
    private final class SearchFrame extends AbstractWidget {

        private static final ResourceLocation FIELD = ResourceLocation.withDefaultNamespace("widget/text_field");
        private static final ResourceLocation FIELD_FOCUSED =
                ResourceLocation.withDefaultNamespace("widget/text_field_highlighted");
        private static final ResourceLocation GLASS = ResourceLocation.withDefaultNamespace("icon/search");

        SearchFrame() {
            super(0, 0, SoundMixerLayout.FULL, SoundMixerLayout.CONTROL_HEIGHT, CommonComponents.EMPTY);
            active = false;
        }

        @Override
        protected void renderWidget(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            g.blitSprite(search.isFocused() ? FIELD_FOCUSED : FIELD, getX(), getY(), getWidth(), getHeight());
            g.blitSprite(GLASS, getX() + 4, getY() + 4, 12, 12);
        }

        @Override
        protected void updateWidgetNarration(final NarrationElementOutput output) {
        }
    }
}
