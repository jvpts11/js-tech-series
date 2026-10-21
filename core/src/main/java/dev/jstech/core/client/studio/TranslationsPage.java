/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.GameLocale;
import dev.jstech.core.client.developer.ScreenFit;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.data.ScreenFiles;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.developer.studio.TranslationGaps;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.gui.screen.ScreenFile;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The board of translations: every mod with how many keys it has and how many its languages lack; what the language
 * chosen lacks, each key with its English; and the texts of the mod's screens that do not fit their room in a
 * language.
 */
final class TranslationsPage extends StudioPage {

    private final Button language;
    private final Button openFile;
    private final List<Mod> mods = new ArrayList<>();
    private final List<TooLong> tooLong = new ArrayList<>();
    private int chosen;
    private int languageAt;
    private int keysScroll;

    private static final int MOD_ROW = 24;
    private static final int KEY_ROW = 24;
    private static final int LONG_ROW = 34;
    private static final String ENGLISH = "en_us";
    private static final String JSON = ".json";

    TranslationsPage(final StudioScreen studio) {
        super(studio);
        this.language = this.panel.add(new Button(() -> GameText.resolve(ToolTexts.LANGUAGE.with(this.code())),
                () -> {
                    this.languageAt++;
                    this.keysScroll = 0;
                }));
        this.openFile = this.panel.add(new Button(() -> GameText.resolve(ToolTexts.OPEN_FILE.with(this.code()
                + JSON)), this::openFile));
    }

    @Override
    TextKey tab() {
        return StudioTool.TRANSLATIONS.title();
    }

    @Override
    List<String> crumbs() {
        return this.mods.isEmpty() ? List.of(words(this.tab())) : List.of(words(this.tab()), this.mod().name());
    }

    @Override
    List<String> lines() {
        return this.mods.isEmpty() ? List.of() : this.lacking();
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final StudioLayout.Rect middle = this.columns().get(1);
        place(this.language, new StudioLayout.Rect(middle.right() - 4 - 90, middle.y() + 2, 90, 13));
        place(this.openFile, new StudioLayout.Rect(middle.x() + 4, middle.bottom() - 20, 120, 16));
    }

    @Override
    void shown() {
        this.mods.clear();
        final StudioContext context = new StudioContext(Minecraft.getInstance().getResourceManager());
        final Map<String, Mod> byMod = new TreeMap<>();
        for (final ModContent content : ModContent.all()) {
            byMod.computeIfAbsent(content.modid(), modid -> modOf(context, modid));
        }
        byMod.values().stream().filter(mod -> !mod.texts().isEmpty()).forEach(this.mods::add);
        this.choose(Math.min(this.chosen, Math.max(0, this.mods.size() - 1)));
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final List<StudioLayout.Rect> columns = this.columns();
        final StudioLayout.Rect left = columns.get(0);
        StudioDraw.panel(g, font(), left, words(ToolTexts.EVERY_MOD));
        for (int i = 0; i < this.mods.size(); i++) {
            final int y = left.y() + StudioDraw.HEAD + 4 + i * MOD_ROW;
            if (y + MOD_ROW > left.bottom()) {
                break;
            }
            final Mod mod = this.mods.get(i);
            final boolean on = i == this.chosen;
            if (on) {
                g.fill(left.x() + 3, y - 2, left.right() - 3, y + MOD_ROW - 3, colours.selected());
                Draw.outline(g, left.x() + 3, y - 2, left.width() - 6, MOD_ROW - 1, colours.outline());
            }
            StudioDraw.text(g, font(), mod.name(), left.x() + 8, y, left.width() - 40, on ? colours.text()
                    : colours.label());
            StudioDraw.small(g, font(), GameText.resolve(ToolTexts.KEYS.with(GameLocale.count(mod.keys()))),
                    left.x() + 8, y + 10, left.width() - 40, colours.dim());
            final int missing = mod.missing();
            if (missing > 0) {
                StudioMarks.draw(g, font(), left.right() - 28, y + 2, missing > 5 ? StudioSheet.Mark.ERROR
                        : StudioSheet.Mark.WARNING);
                StudioDraw.small(g, font(), String.valueOf(missing), left.right() - 18, y + 4, 16, colours.dim());
            }
        }
        this.lacks(g, columns.get(1), colours);
        this.overflows(g, columns.get(2), colours);
        this.language.setVisible(!this.mods.isEmpty());
        this.openFile.setVisible(!this.mods.isEmpty());
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final StudioLayout.Rect left = this.columns().get(0);
        if (!left.contains(mouseX, mouseY)) {
            return false;
        }
        final int row = (int) ((mouseY - left.y() - StudioDraw.HEAD - 2) / MOD_ROW);
        if (row >= 0 && row < this.mods.size()) {
            this.choose(row);
        }
        return true;
    }

    @Override
    boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollY) {
        if (this.columns().get(1).contains(mouseX, mouseY) && !this.mods.isEmpty()) {
            this.keysScroll = ThingsPage.scrolled(this.keysScroll, scrollY, this.lacking().size(), this.keysShown());
            return true;
        }
        return false;
    }

    private List<StudioLayout.Rect> columns() {
        return StudioLayout.columns(this.regions.body(), 0.30, 0.38);
    }

    private void choose(final int row) {
        this.chosen = row;
        this.keysScroll = 0;
        this.tooLong.clear();
        if (this.mods.isEmpty()) {
            return;
        }
        final String modid = this.mod().modid();
        for (final ResourceLocation screen : ScreenFiles.all().keySet()) {
            if (!screen.getNamespace().equals(modid)) {
                continue;
            }
            try {
                for (final ScreenFit.Overflow overflow : ScreenFit.of(screen, font())) {
                    this.tooLong.add(new TooLong(screen, overflow));
                }
            } catch (final RuntimeException unbuilt) {
                // A screen that cannot be built alone, for want of its menu, has its texts measured when it opens.
            }
        }
    }

    private void lacks(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours) {
        if (this.mods.isEmpty()) {
            StudioDraw.panel(g, font(), area, "");
            return;
        }
        final List<String> lacking = this.lacking();
        StudioDraw.panel(g, font(), area, GameText.resolve(ToolTexts.LACKS.with(this.code(), lacking.size())),
                this.language.width() + StudioLayout.GAP);
        if (lacking.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NOTHING_LACKS), area.x() + StudioDraw.INSET,
                    area.y() + StudioDraw.HEAD + 4, area.width() - 2 * StudioDraw.INSET, colours.dim());
        }
        final Map<String, String> english = this.mod().texts().getOrDefault(ENGLISH, Map.of());
        final int last = Math.min(lacking.size(), this.keysScroll + this.keysShown());
        for (int i = this.keysScroll; i < last; i++) {
            final int y = area.y() + StudioDraw.HEAD + 4 + (i - this.keysScroll) * KEY_ROW;
            final String key = lacking.get(i);
            StudioMarks.draw(g, font(), area.x() + StudioDraw.INSET, y + 1, StudioSheet.Mark.ERROR);
            final int x = area.x() + StudioDraw.INSET + StudioMarks.WIDTH + 5;
            StudioDraw.text(g, font(), key, x, y, area.right() - x - StudioDraw.INSET, colours.label());
            StudioDraw.small(g, font(), GameText.resolve(ToolTexts.IN_ENGLISH.with(english.getOrDefault(key, ""))),
                    x, y + 10, area.right() - x - StudioDraw.INSET, colours.dim());
        }
    }

    private void overflows(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours) {
        StudioDraw.panel(g, font(), area, words(ToolTexts.TOO_LONG));
        if (this.tooLong.isEmpty() && !this.mods.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.EVERY_TEXT_FITS), area.x() + StudioDraw.INSET,
                    area.y() + StudioDraw.HEAD + 4, area.width() - 2 * StudioDraw.INSET, colours.dim());
        }
        for (int i = 0; i < this.tooLong.size(); i++) {
            final int y = area.y() + StudioDraw.HEAD + 4 + i * LONG_ROW;
            if (y + LONG_ROW > area.bottom()) {
                break;
            }
            final TooLong one = this.tooLong.get(i);
            StudioMarks.draw(g, font(), area.x() + StudioDraw.INSET, y + 1, StudioSheet.Mark.WARNING);
            final int x = area.x() + StudioDraw.INSET + StudioMarks.WIDTH + 5;
            final int room = area.right() - x - StudioDraw.INSET;
            StudioDraw.text(g, font(), GameText.resolve(ToolTexts.TOO_LONG_WHAT.with(one.overflow().component(),
                    one.overflow().language())), x, y, room, colours.text());
            StudioDraw.small(g, font(), ScreenFile.FOLDER + "/" + one.screen().getPath() + JSON, x, y + 10, room,
                    colours.dim());
            StudioDraw.small(g, font(), GameText.resolve(ToolTexts.PAST_ITS_BOX.with(one.overflow().over())), x,
                    y + 18, room, colours.dim());
        }
    }

    private void openFile() {
        if (!this.mods.isEmpty()) {
            this.studio.open(StudioFiles.asset(ResourceLocation.fromNamespaceAndPath(this.mod().modid(),
                    "lang/" + this.code() + JSON)));
        }
    }

    /* The keys the language chosen lacks, of the mod chosen. */
    private List<String> lacking() {
        return TranslationGaps.of(this.mod().keysByLanguage()).getOrDefault(this.code(), List.of());
    }

    /* The language chosen: of the mod's languages, the one the button has come to, English last. */
    private String code() {
        if (this.mods.isEmpty()) {
            return ENGLISH;
        }
        final List<String> codes = new ArrayList<>(this.mod().texts().keySet());
        codes.remove(ENGLISH);
        codes.add(ENGLISH);
        return codes.get(Math.floorMod(this.languageAt, codes.size()));
    }

    private int keysShown() {
        return Math.max(1, (this.columns().get(1).height() - StudioDraw.HEAD - 28) / KEY_ROW);
    }

    private Mod mod() {
        return this.mods.get(this.chosen);
    }

    private static Mod modOf(final StudioContext context, final String modid) {
        final String name = StudioThings.modName(modid);
        return new Mod(modid, name.isEmpty() ? GameText.resolve(StudioTexts.PACK.with(modid)) : name,
                context.texts(modid));
    }

    /** A mod and its languages, each with its words by key. */
    private record Mod(String modid, String name, Map<String, Map<String, String>> texts) {

        int keys() {
            return TranslationGaps.keys(this.keysByLanguage()).size();
        }

        int missing() {
            return TranslationGaps.of(this.keysByLanguage()).values().stream().mapToInt(List::size).sum();
        }

        Map<String, Set<String>> keysByLanguage() {
            final Map<String, Set<String>> out = new TreeMap<>();
            this.texts.forEach((code, words) -> out.put(code, words.keySet()));
            return out;
        }
    }

    /** A text of a screen past its room. */
    private record TooLong(ResourceLocation screen, ScreenFit.Overflow overflow) {
    }
}
