/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigScreenTexts;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.ConfigTexts;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.gui.layout.ConfigScreenLayout;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModContainer;
import org.jetbrains.annotations.Nullable;

/**
 * The settings screen of a mod that keeps its settings the Core's way, in the Core's own look. The header carries the
 * mod's mark, its name and version, and a box that finds its settings by their names and what they do. Down the left,
 * a tab for each kind of file the mod keeps (the world's, the player's, every game's), a line saying where that kind
 * is kept, and the tab's sections, each with how many settings it holds and a mark while one of them is changed and
 * not saved. On the right, the open section's title and what it is for, then a card for each setting: its name, its
 * description, its default and bounds, and its control (a switch, a number to type or step with its unit, a word to
 * go through, a text to type), with a Default button while it is off its default. The footer puts the section back to
 * its defaults, says how many changes wait, and drops them (Cancel) or keeps them (Save).
 *
 * <p>A world's settings are changed only from inside that world, on the game that runs it; anywhere else they are
 * shown dimmed under a line saying so. A mod hands this to the game as its settings screen:
 * {@code container.registerExtensionPoint(IConfigScreenFactory.class, CoreConfigScreen::new)}, or with its mark,
 * {@code (mod, parent) -> new CoreConfigScreen(mod, parent, MARK)}.
 */
@PaletteHolder
public final class CoreConfigScreen extends Screen {

    private final Screen parent;
    private final String modId;
    private final String modName;
    private final String version;
    private final IConfigBadge badge;
    private final List<Scope> scopes = new ArrayList<>();
    private final Map<ConfigFile, ConfigDraft> drafts = new LinkedHashMap<>();
    private final List<RowField> fields = new ArrayList<>();
    private final List<Card> cards = new ArrayList<>();
    private int scope;
    private int selected;
    private int scroll;
    private String query = "";
    private @Nullable EditBox search;

    private static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gui/config_screen",
            new Colours(0xFF1E1A0F, 0xFF4A3B16, 0xFF0F1620, 0xFF062521, 0xFF4E5A6B, 0x8C0B0E13));
    private static final int SWITCH_KNOB = 11;
    private static final float TITLE_SCALE = 1.25F;
    private static final String LEFT = "<";
    private static final String RIGHT = ">";
    private static final String MINUS = "-";
    private static final String PLUS = "+";
    private static final String NEW_LINE = "\n";

    public CoreConfigScreen(final ModContainer container, @Nullable final Screen parent) {
        this(container, parent, IConfigBadge.CHIP);
    }

    public CoreConfigScreen(final ModContainer container, @Nullable final Screen parent, final IConfigBadge badge) {
        super(GameText.component(ConfigScreenTexts.TITLE.with(container.getModInfo().getDisplayName())));
        this.parent = parent;
        this.modId = container.getModId();
        this.modName = container.getModInfo().getDisplayName();
        this.version = container.getModInfo().getVersion().toString();
        this.badge = badge;
        final Map<ConfigSide, List<Group>> bySide = new LinkedHashMap<>();
        for (final ConfigSide side : List.of(ConfigSide.SERVER, ConfigSide.CLIENT, ConfigSide.COMMON)) {
            bySide.put(side, new ArrayList<>());
        }
        for (final ConfigFile file : ConfigFiles.of(modId)) {
            // Only the files whose settings are named are shown: the others are their mods' own to show.
            if (file.format() == ConfigFormats.TOML) {
                drafts.put(file, new ConfigDraft(file));
                bySide.get(file.side()).addAll(groupsOf(file));
            }
        }
        bySide.forEach((side, groups) -> {
            if (!groups.isEmpty()) {
                scopes.add(new Scope(side, List.copyOf(groups)));
            }
        });
    }

    /** The tabs across the top of the rail, one for each kind of file the mod keeps; for a test to read. */
    public List<String> scopeLabels() {
        final List<String> out = new ArrayList<>();
        for (final Scope one : scopes) {
            out.add(GameText.resolve(tabOf(one.side())));
        }
        return out;
    }

    /** The sections the rail lists under the open tab; for a test to read. */
    public List<String> railLabels() {
        final List<String> out = new ArrayList<>();
        if (scope < scopes.size()) {
            for (final Group group : scopes.get(scope).groups()) {
                out.add(group.title().getString());
            }
        }
        return out;
    }

    /** The names of the settings the cards show, all of them however far they scroll; for a test to read. */
    public List<String> rowNames() {
        final List<String> out = new ArrayList<>();
        for (final Row row : shownRows()) {
            out.add(nameOf(row.key()).getString());
        }
        return out;
    }

    /** Opens the section called {@code title}, under whichever tab holds it, as a click on it does. */
    public boolean openSection(final String title) {
        for (int s = 0; s < scopes.size(); s++) {
            final List<Group> groups = scopes.get(s).groups();
            for (int i = 0; i < groups.size(); i++) {
                if (groups.get(i).title().getString().equals(title)) {
                    scope = s;
                    select(i);
                    return true;
                }
            }
        }
        return false;
    }

    /** Finds settings by their names and what they do, as typing in the search box does. */
    public void search(final String words) {
        if (search != null) {
            search.setValue(words);
        }
        setQuery(words);
    }

    /** The draft of the file the open section belongs to, for a test to change a setting as a click would. */
    @Nullable
    public ConfigDraft openDraft() {
        final Group group = openGroup();
        return group == null ? null : drafts.get(group.file());
    }

    /** Keeps every change and goes back, as Save does. */
    public void done() {
        for (final Map.Entry<ConfigFile, ConfigDraft> draft : drafts.entrySet()) {
            if (editable(draft.getKey())) {
                draft.getValue().apply();
            }
        }
        back();
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        g.fill(0, 0, width, height, JsTechTheme.screen());
        drawHeader(g);
        drawRail(g, mouseX, mouseY);
        drawContent(g, mouseX, mouseY);
        drawFooter(g, mouseX, mouseY);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final Card hovered = cardAt(mouseX, mouseY);
        if (hovered != null && mouseX < ConfigScreenLayout.textX(width) + nameRoom(hovered)) {
            drawHint(g, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (search != null && search.isFocused()) {
            search.setFocused(false);
        }
        final int x = (int) mouseX;
        final int y = (int) mouseY;
        return clickTabs(x, y) || clickRail(x, y) || clickCard(x, y, hasShiftDown()) || clickFooter(x, y);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        if (mouseX < ConfigScreenLayout.contentLeft(width)) {
            return false;
        }
        final int most = Math.max(0, shownRows().size() - 1);
        final int next = Math.max(0, Math.min(most, scroll - (int) Math.signum(scrollY)));
        if (next != scroll && (scrollY > 0 || !lastShown())) {
            scroll = next;
            rebuild();
        }
        return true;
    }

    /* Leaving any other way than Save drops what changed. */
    @Override
    public void onClose() {
        back();
    }

    @Override
    protected void init() {
        final int x = ConfigScreenLayout.searchX(width);
        search = new EditBox(font, x + 14, ConfigScreenLayout.SEARCH_Y + 3, ConfigScreenLayout.SEARCH_WIDTH - 18,
                ConfigScreenLayout.SEARCH_HEIGHT - 4, GameText.component(ConfigScreenTexts.SEARCH));
        search.setBordered(false);
        search.setMaxLength(64);
        search.setTextColor(JsTechTheme.text());
        search.setValue(query);
        search.setResponder(this::setQuery);
        addRenderableWidget(search);
        rebuild();
    }

    private void back() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    /* The file's settings grouped by the section they sit in, in the order the file declares them. */
    private List<Group> groupsOf(final ConfigFile file) {
        final Map<String, List<ConfigKey<?>>> bySection = new LinkedHashMap<>();
        for (final ConfigKey<?> key : file.keys()) {
            if (!key.title().isBlank()) {
                final String section = String.join(".", key.path().subList(0, key.path().size() - 1));
                bySection.computeIfAbsent(section, ignored -> new ArrayList<>()).add(key);
            }
        }
        final List<Group> out = new ArrayList<>();
        bySection.forEach((section, keys) -> out.add(new Group(file, section, List.copyOf(keys),
                section.isEmpty() ? GameText.component(ConfigScreenTexts.GENERAL)
                        : Component.translatable(ConfigTexts.key(modId, section)))));
        return out;
    }

    private void setQuery(final String words) {
        final String trimmed = words.trim();
        if (!trimmed.equals(query)) {
            query = trimmed;
            scroll = 0;
            rebuild();
        }
    }

    private void select(final int index) {
        selected = index;
        scroll = 0;
        query = "";
        if (search != null) {
            search.setValue("");
        }
        rebuild();
    }

    @Nullable
    private Group openGroup() {
        if (scope >= scopes.size()) {
            return null;
        }
        final List<Group> groups = scopes.get(scope).groups();
        return selected < groups.size() ? groups.get(selected) : null;
    }

    /* What the cards show: the open section's settings, or every setting the search finds, under whichever tab. */
    private List<Row> shownRows() {
        final List<Row> out = new ArrayList<>();
        if (query.isEmpty()) {
            final Group group = openGroup();
            if (group != null) {
                for (final ConfigKey<?> key : group.keys()) {
                    out.add(new Row(group.file(), key));
                }
            }
            return out;
        }
        final String words = query.toLowerCase(Locale.ROOT);
        for (final Scope one : scopes) {
            for (final Group group : one.groups()) {
                for (final ConfigKey<?> key : group.keys()) {
                    final String said = (nameOf(key).getString() + NEW_LINE + description(key))
                            .toLowerCase(Locale.ROOT);
                    if (said.contains(words)) {
                        out.add(new Row(group.file(), key));
                    }
                }
            }
        }
        return out;
    }

    /* The cards that fit from the first one scrolled to, laid out down the content, and their typing fields. */
    private void rebuild() {
        for (final RowField field : fields) {
            removeWidget(field.box());
        }
        fields.clear();
        cards.clear();
        final List<Row> rows = shownRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - 1)));
        int y = ConfigScreenLayout.cardsTop(sectionNote().size()) + (lockedHere()
                ? ConfigScreenLayout.LOCK_HEIGHT + ConfigScreenLayout.CARD_GAP : 0);
        for (int i = scroll; i < rows.size(); i++) {
            final Row row = rows.get(i);
            final ConfigDraft.Control control = ConfigDraft.controlOf(row.key());
            final boolean unit = hasUnit(row.key());
            final int room = ConfigScreenLayout.nameWidth(width, control, unit, true);
            final List<String> lines = wrapPlain(firstParagraph(description(row.key())), room);
            final int most = ConfigScreenLayout.MOST_DESCRIPTION_LINES;
            final boolean cut = lines.size() > most;
            final List<String> shown = new ArrayList<>(lines.subList(0, Math.min(most, lines.size())));
            if (cut) {
                // The last line shown runs on into the next and ends in dots: there is more where the pointer rests.
                shown.set(most - 1, Texts.clip(font, shown.get(most - 1) + " " + lines.get(most),
                        (int) (room / JsTechTheme.small())));
            }
            final int tall = ConfigScreenLayout.cardHeight(shown.size());
            if (y + tall > ConfigScreenLayout.cardsBottom(height) && !cards.isEmpty()) {
                break;
            }
            cards.add(new Card(row, y, tall, List.copyOf(shown)));
            y += tall + ConfigScreenLayout.CARD_GAP;
        }
        for (final Card card : cards) {
            addField(card);
        }
    }

    /* A typing field for a number's or a text's card, over the box its control draws. */
    private void addField(final Card card) {
        final ConfigKey<?> key = card.row().key();
        final ConfigDraft.Control control = ConfigDraft.controlOf(key);
        if (control != ConfigDraft.Control.NUMBER && control != ConfigDraft.Control.TEXT) {
            return;
        }
        final int left = ConfigScreenLayout.controlX(width, control, hasUnit(key));
        final int x = control == ConfigDraft.Control.NUMBER
                ? left + ConfigScreenLayout.STEP + ConfigScreenLayout.CONTROL_GAP + 3 : left + 3;
        final int w = (control == ConfigDraft.Control.NUMBER ? ConfigScreenLayout.NUMBER_FIELD
                : ConfigScreenLayout.TEXT_WIDTH) - 6;
        final int y = ConfigScreenLayout.controlY(card.y(), card.height(), control) + 3;
        final ConfigDraft draft = drafts.get(card.row().file());
        final EditBox box = new EditBox(font, x, y, w, ConfigScreenLayout.CONTROL_HEIGHT - 4, nameOf(key));
        box.setBordered(false);
        box.setMaxLength(256);
        box.setTextColor(JsTechTheme.text());
        box.setValue(shown(draft.value(key)));
        box.setEditable(editable(card.row().file()));
        box.setResponder(text -> draft.typed(key, text));
        addRenderableWidget(box);
        fields.add(new RowField(key, box));
    }

    /** Whether the file can be changed from here: a world's only inside a world this game runs. */
    private boolean editable(final ConfigFile file) {
        return file.side() != ConfigSide.SERVER || minecraft != null && minecraft.hasSingleplayerServer();
    }

    /* Whether the open tab's settings are only read here, which the banner over its cards says. */
    private boolean lockedHere() {
        final Group group = openGroup();
        return query.isEmpty() && group != null && !editable(group.file());
    }

    private static TextKey tabOf(final ConfigSide side) {
        return switch (side) {
            case SERVER -> ConfigScreenTexts.WORLD;
            case CLIENT -> ConfigScreenTexts.PLAYER;
            case COMMON -> ConfigScreenTexts.EVERY_GAME;
        };
    }

    private static TextKey noteOf(final ConfigSide side) {
        return switch (side) {
            case SERVER -> ConfigScreenTexts.WORLD_NOTE;
            case CLIENT -> ConfigScreenTexts.PLAYER_NOTE;
            case COMMON -> ConfigScreenTexts.EVERY_GAME_NOTE;
        };
    }

    private Component nameOf(final ConfigKey<?> key) {
        return Component.translatable(ConfigTexts.key(modId, key.dottedPath()));
    }

    /* What a setting does, all of it, as the mod's language says it; empty when it says nothing. */
    private String description(final ConfigKey<?> key) {
        final String translation = ConfigTexts.key(modId, key.dottedPath()) + ConfigTexts.TOOLTIP;
        return I18n.exists(translation) ? I18n.get(translation) : "";
    }

    private boolean hasUnit(final ConfigKey<?> key) {
        return ConfigDraft.controlOf(key) == ConfigDraft.Control.NUMBER
                && I18n.exists(ConfigTexts.key(modId, key.dottedPath()) + ConfigTexts.UNIT);
    }

    private String unitOf(final ConfigKey<?> key) {
        return hasUnit(key) ? I18n.get(ConfigTexts.key(modId, key.dottedPath()) + ConfigTexts.UNIT) : "";
    }

    /* The open section's note, what it is for, as many lines as the screen gives it. */
    private List<FormattedCharSequence> sectionNote() {
        final Group group = openGroup();
        if (!query.isEmpty() || group == null || group.section().isEmpty()) {
            return List.of();
        }
        final String key = ConfigTexts.key(modId, group.section()) + ConfigTexts.TOOLTIP;
        if (!I18n.exists(key)) {
            return List.of();
        }
        final List<FormattedCharSequence> lines = wrapSmall(I18n.get(key),
                ConfigScreenLayout.contentRight(width) - ConfigScreenLayout.contentLeft(width));
        return lines.subList(0, Math.min(ConfigScreenLayout.MOST_SECTION_NOTE_LINES, lines.size()));
    }

    /* Text wrapped to a width in screen units, for the small size it is drawn at. */
    private List<FormattedCharSequence> wrapSmall(final String text, final int room) {
        if (text.isEmpty()) {
            return List.of();
        }
        return font.split(Component.literal(text), (int) (room / JsTechTheme.small()));
    }

    /* The same, as the plain lines, for a card that may end its last one in dots. */
    private List<String> wrapPlain(final String text, final int room) {
        final List<String> out = new ArrayList<>();
        if (!text.isEmpty()) {
            for (final FormattedText line : font.getSplitter().splitLines(text, (int) (room / JsTechTheme.small()),
                    Style.EMPTY)) {
                out.add(line.getString());
            }
        }
        return out;
    }

    /* The first paragraph of a description, which a card shows; the hint over it shows them all. */
    private static String firstParagraph(final String text) {
        final int end = text.indexOf(NEW_LINE);
        return end < 0 ? text : text.substring(0, end);
    }

    /* A scale near {@code wanted} that puts every pixel of the font on whole pixels of the screen. */
    private float crisp(final float wanted) {
        final double gui = minecraft == null ? 1.0 : minecraft.getWindow().getGuiScale();
        return (float) (Math.max(1L, Math.round(wanted * gui)) / gui);
    }

    private void drawHeader(final GuiGraphics g) {
        g.fill(0, 0, width, ConfigScreenLayout.HEADER, JsTechTheme.rail());
        JsTechTheme.hLine(g, 0, ConfigScreenLayout.HEADER - 1, width);
        final int bx = ConfigScreenLayout.PAD;
        final int by = ConfigScreenLayout.BADGE_Y;
        g.fill(bx, by, bx + ConfigScreenLayout.BADGE, by + ConfigScreenLayout.BADGE, JsTechTheme.panel());
        Draw.outline(g, bx, by, ConfigScreenLayout.BADGE, ConfigScreenLayout.BADGE, JsTechTheme.accent());
        badge.draw(g, bx + 4, by + 4, ConfigScreenLayout.BADGE - 8, JsTechTheme.accent());
        final int room = ConfigScreenLayout.searchX(width) - 8 - ConfigScreenLayout.nameX();
        final float scale = crisp(TITLE_SCALE);
        Draw.textScaled(g, font, Texts.clip(font, modName, (int) (room / scale)), ConfigScreenLayout.nameX(),
                ConfigScreenLayout.NAME_Y, JsTechTheme.text(), scale);
        JsTechTheme.textS(g, font, GameText.resolve(ConfigScreenTexts.SUBTITLE.with(version)),
                ConfigScreenLayout.nameX(), ConfigScreenLayout.SUBTITLE_Y, JsTechTheme.dim());
        final int sx = ConfigScreenLayout.searchX(width);
        final int sy = ConfigScreenLayout.SEARCH_Y;
        g.fill(sx, sy, sx + ConfigScreenLayout.SEARCH_WIDTH, sy + ConfigScreenLayout.SEARCH_HEIGHT,
                JsTechTheme.slotBg());
        Draw.outline(g, sx, sy, ConfigScreenLayout.SEARCH_WIDTH, ConfigScreenLayout.SEARCH_HEIGHT,
                search != null && search.isFocused() ? JsTechTheme.accent() : JsTechTheme.slotEdge());
        // The magnifier: a ring and its handle.
        final int faint = PALETTE.get().faint();
        Draw.outline(g, sx + 4, sy + 3, 6, 6, faint);
        g.fill(sx + 9, sy + 9, sx + 11, sy + 11, faint);
        if (query.isEmpty() && (search == null || search.getValue().isEmpty())) {
            JsTechTheme.textS(g, font, GameText.resolve(ConfigScreenTexts.SEARCH), sx + 14, sy + 4, faint);
        }
    }

    private void drawRail(final GuiGraphics g, final int mouseX, final int mouseY) {
        final int rail = ConfigScreenLayout.railWidth(width);
        g.fill(0, ConfigScreenLayout.HEADER, rail, height - ConfigScreenLayout.FOOTER, JsTechTheme.rail());
        JsTechTheme.vLine(g, rail - 1, ConfigScreenLayout.HEADER,
                height - ConfigScreenLayout.FOOTER - ConfigScreenLayout.HEADER);
        if (scopes.isEmpty()) {
            return;
        }
        final int tabs = scopes.size();
        final int tabsWidth = ConfigScreenLayout.tabWidth(width, tabs) * tabs;
        Draw.outline(g, ConfigScreenLayout.TAB_INSET - 1, ConfigScreenLayout.TAB_Y - 1, tabsWidth + 2,
                ConfigScreenLayout.TAB_HEIGHT + 2, JsTechTheme.slotEdge());
        for (int i = 0; i < tabs; i++) {
            final int x = ConfigScreenLayout.tabX(width, i, tabs);
            final int w = ConfigScreenLayout.tabWidth(width, tabs);
            final boolean on = i == scope;
            if (on) {
                g.fill(x, ConfigScreenLayout.TAB_Y, x + w, ConfigScreenLayout.TAB_Y + ConfigScreenLayout.TAB_HEIGHT,
                        JsTechTheme.tabOn());
                g.fill(x, ConfigScreenLayout.TAB_Y + ConfigScreenLayout.TAB_HEIGHT - 1, x + w,
                        ConfigScreenLayout.TAB_Y + ConfigScreenLayout.TAB_HEIGHT, JsTechTheme.accent());
            }
            final String label = GameText.resolve(tabOf(scopes.get(i).side()));
            JsTechTheme.textSCenter(g, font, Texts.clip(font, label, (int) ((w - 4) / JsTechTheme.small())),
                    x + w / 2, ConfigScreenLayout.TAB_Y + 4, on ? JsTechTheme.accent() : JsTechTheme.dim());
        }
        final List<FormattedCharSequence> note = wrapSmall(GameText.resolve(noteOf(scopes.get(scope).side())),
                rail - 2 * ConfigScreenLayout.TAB_INSET);
        for (int line = 0; line < Math.min(ConfigScreenLayout.SCOPE_NOTE_LINES, note.size()); line++) {
            drawSmall(g, note.get(line), ConfigScreenLayout.TAB_INSET,
                    ConfigScreenLayout.scopeNoteY() + line * ConfigScreenLayout.NOTE_LINE, PALETTE.get().faint());
        }
        final List<Group> groups = scopes.get(scope).groups();
        for (int i = 0; i < groups.size(); i++) {
            final Group group = groups.get(i);
            final int y = ConfigScreenLayout.railItemY(i);
            final boolean on = i == selected && query.isEmpty();
            if (on) {
                g.fill(0, y, rail - 1, y + ConfigScreenLayout.RAIL_ITEM, JsTechTheme.tabOn());
                g.fill(0, y, 2, y + ConfigScreenLayout.RAIL_ITEM, JsTechTheme.accent());
            } else if (mouseX < rail && mouseY >= y && mouseY < y + ConfigScreenLayout.RAIL_ITEM) {
                g.fill(0, y, rail - 1, y + ConfigScreenLayout.RAIL_ITEM, JsTechTheme.hover());
            }
            final String label = Texts.clip(font, group.title().getString(), ConfigScreenLayout.railLabelWidth(width));
            JsTechTheme.text(g, font, label, ConfigScreenLayout.RAIL_LABEL_X, y + 4, on ? JsTechTheme.text()
                    : JsTechTheme.dim());
            if (changedIn(group)) {
                final int dot = ConfigScreenLayout.RAIL_LABEL_X + font.width(label) + 3;
                g.fill(dot, y + 7, dot + 3, y + 10, JsTechTheme.amber());
            }
            JsTechTheme.textSRight(g, font, String.valueOf(group.keys().size()), rail - 8, y + 5,
                    PALETTE.get().faint());
        }
    }

    private boolean changedIn(final Group group) {
        final ConfigDraft draft = drafts.get(group.file());
        for (final ConfigKey<?> key : group.keys()) {
            if (draft.isChanged(key)) {
                return true;
            }
        }
        return false;
    }

    private void drawContent(final GuiGraphics g, final int mouseX, final int mouseY) {
        final int left = ConfigScreenLayout.contentLeft(width);
        final int right = ConfigScreenLayout.contentRight(width);
        final Group group = openGroup();
        final String heading = !query.isEmpty() ? GameText.resolve(ConfigScreenTexts.RESULTS.with(query))
                : group == null ? "" : group.title().getString();
        final float scale = crisp(TITLE_SCALE);
        Draw.textScaled(g, font, Texts.clip(font, heading, (int) ((right - left) / scale)), left,
                ConfigScreenLayout.titleY(), JsTechTheme.text(), scale);
        final List<FormattedCharSequence> note = sectionNote();
        for (int line = 0; line < note.size(); line++) {
            drawSmall(g, note.get(line), left, ConfigScreenLayout.sectionNoteY() + line * ConfigScreenLayout.NOTE_LINE,
                    JsTechTheme.dim());
        }
        if (lockedHere()) {
            drawLock(g, left, ConfigScreenLayout.cardsTop(note.size()), right - left);
        }
        if (cards.isEmpty() && !query.isEmpty()) {
            JsTechTheme.textS(g, font, GameText.resolve(ConfigScreenTexts.NO_RESULTS), left,
                    ConfigScreenLayout.cardsTop(0), JsTechTheme.dim());
        }
        for (final Card card : cards) {
            drawCard(g, card, mouseX, mouseY);
        }
        drawScrollbar(g);
    }

    private void drawLock(final GuiGraphics g, final int x, final int y, final int w) {
        final Colours c = PALETTE.get();
        g.fill(x, y, x + w, y + ConfigScreenLayout.LOCK_HEIGHT, c.lockGround());
        Draw.outline(g, x, y, w, ConfigScreenLayout.LOCK_HEIGHT, c.lockEdge());
        // The padlock: its shackle over its body.
        final int px = x + 6;
        final int py = y + 4;
        Draw.outline(g, px + 1, py, 4, 4, JsTechTheme.amber());
        g.fill(px, py + 3, px + 6, py + 8, JsTechTheme.amber());
        final String said = GameText.resolve(ConfigScreenTexts.WORLD_ONLY);
        JsTechTheme.textS(g, font, Texts.clip(font, said, (int) ((w - 20) / JsTechTheme.small())), x + 16, y + 5,
                JsTechTheme.amber());
    }

    private void drawCard(final GuiGraphics g, final Card card, final int mouseX, final int mouseY) {
        final Row row = card.row();
        final ConfigKey<?> key = row.key();
        final ConfigDraft draft = drafts.get(row.file());
        final boolean editable = editable(row.file());
        final int left = ConfigScreenLayout.contentLeft(width);
        final int right = ConfigScreenLayout.contentRight(width);
        final int y = card.y();
        final int h = card.height();
        final boolean hovered = editable && mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + h;
        g.fill(left, y, right, y + h, hovered ? JsTechTheme.hover() : JsTechTheme.panel());
        Draw.outline(g, left, y, right - left, h, JsTechTheme.line());
        final boolean changed = draft.isChanged(key);
        if (changed) {
            g.fill(left, y, left + 1, y + h, JsTechTheme.amber());
        }
        final ConfigDraft.Control control = ConfigDraft.controlOf(key);
        final boolean unit = hasUnit(key);
        final boolean reset = editable && draft.isOffDefault(key) && control != ConfigDraft.Control.FIXED;
        final int textX = ConfigScreenLayout.textX(width);
        final int room = ConfigScreenLayout.nameWidth(width, control, unit, reset);
        final String name = nameOf(key).getString();
        final String tag = GameText.resolve(ConfigScreenTexts.CHANGED);
        final int tagWidth = Math.round(JsTechTheme.widthS(font, tag)) + 6;
        // The mark of a change stands after the name where it fits, and at the start of the line under it where not.
        final boolean tagBeside = changed && font.width(name) + 5 + tagWidth <= room;
        final String shownName = Texts.clip(font, name, room);
        final int nameY = y + ConfigScreenLayout.CARD_PAD + 1;
        JsTechTheme.text(g, font, shownName, textX, nameY, JsTechTheme.text());
        if (tagBeside) {
            drawTag(g, tag, textX + font.width(shownName) + 5, nameY, tagWidth);
        }
        int lineY = y + ConfigScreenLayout.CARD_PAD + ConfigScreenLayout.CARD_NAME;
        for (final String line : card.lines()) {
            drawSmall(g, Component.literal(line).getVisualOrderText(), textX, lineY, JsTechTheme.dim());
            lineY += ConfigScreenLayout.NOTE_LINE;
        }
        final int metaY = lineY + ConfigScreenLayout.META_GAP;
        int metaX = textX;
        if (changed && !tagBeside) {
            drawTag(g, tag, textX, metaY - 1, tagWidth);
            metaX += tagWidth + 4;
        }
        drawSmall(g, Component.literal(Texts.clip(font, meta(key),
                        (int) ((room - (metaX - textX)) / JsTechTheme.small()))).getVisualOrderText(), metaX, metaY,
                PALETTE.get().faint());
        final int cx = ConfigScreenLayout.controlX(width, control, unit);
        final int cy = ConfigScreenLayout.controlY(y, h, control);
        if (reset) {
            final int rx = ConfigScreenLayout.resetX(width, control, unit);
            final int ry = ConfigScreenLayout.controlY(y, h, ConfigDraft.Control.NUMBER);
            g.fill(rx, ry, rx + ConfigScreenLayout.RESET_WIDTH, ry + ConfigScreenLayout.CONTROL_HEIGHT,
                    over(mouseX, mouseY, rx, ry, ConfigScreenLayout.RESET_WIDTH, ConfigScreenLayout.CONTROL_HEIGHT)
                            ? JsTechTheme.hover() : JsTechTheme.slotBg());
            Draw.outline(g, rx, ry, ConfigScreenLayout.RESET_WIDTH, ConfigScreenLayout.CONTROL_HEIGHT,
                    JsTechTheme.line());
            JsTechTheme.textSCenter(g, font, GameText.resolve(ConfigScreenTexts.TO_DEFAULT),
                    rx + ConfigScreenLayout.RESET_WIDTH / 2, ry + 4, JsTechTheme.amber());
        }
        drawControl(g, draft, key, control, cx, cy, editable, mouseX, mouseY);
        if (!editable) {
            g.fill(left, y, right, y + h, PALETTE.get().locked());
        }
    }

    /* The mark of a setting changed and not saved: its word in amber, in an amber frame. */
    private void drawTag(final GuiGraphics g, final String tag, final int x, final int y, final int w) {
        Draw.outline(g, x, y, w, 8, JsTechTheme.amber());
        JsTechTheme.textS(g, font, tag, x + 3, y + 2, JsTechTheme.amber());
    }

    private void drawControl(final GuiGraphics g, final ConfigDraft draft, final ConfigKey<?> key,
                             final ConfigDraft.Control control, final int x, final int y, final boolean editable,
                             final int mouseX, final int mouseY) {
        final Object value = draft.value(key);
        switch (control) {
            case TOGGLE -> {
                final boolean on = Boolean.TRUE.equals(value);
                JsTechTheme.textS(g, font, GameText.resolve(on ? ConfigScreenTexts.ON : ConfigScreenTexts.OFF), x,
                        y + 3, on ? JsTechTheme.accent() : JsTechTheme.dim());
                drawSwitch(g, x + ConfigScreenLayout.STATE_WIDTH + ConfigScreenLayout.CONTROL_GAP, y, on, editable);
            }
            case NUMBER -> {
                drawStep(g, x, y, MINUS, editable, mouseX, mouseY);
                final int fx = x + ConfigScreenLayout.STEP + ConfigScreenLayout.CONTROL_GAP;
                drawField(g, fx, y, ConfigScreenLayout.NUMBER_FIELD, focused(key));
                final int px = fx + ConfigScreenLayout.NUMBER_FIELD + ConfigScreenLayout.CONTROL_GAP;
                drawStep(g, px, y, PLUS, editable, mouseX, mouseY);
                if (hasUnit(key)) {
                    JsTechTheme.textS(g, font, Texts.clip(font, unitOf(key),
                                    (int) (ConfigScreenLayout.UNIT_WIDTH / JsTechTheme.small())),
                            px + ConfigScreenLayout.STEP + ConfigScreenLayout.CONTROL_GAP, y + 4, JsTechTheme.dim());
                }
            }
            case CHOICE -> {
                final int w = ConfigScreenLayout.CHOICE_WIDTH;
                JsTechTheme.button(g, x, y, w, ConfigScreenLayout.CONTROL_HEIGHT, editable
                        && over(mouseX, mouseY, x, y, w, ConfigScreenLayout.CONTROL_HEIGHT));
                JsTechTheme.text(g, font, LEFT, x + 4, y + 3, JsTechTheme.dim());
                JsTechTheme.textRight(g, font, RIGHT, x + w - 4, y + 3, JsTechTheme.dim());
                JsTechTheme.textCenter(g, font, Texts.clip(font, String.valueOf(value), w - 24), x + w / 2, y + 3,
                        editable ? JsTechTheme.accent() : JsTechTheme.dim());
            }
            case TEXT -> drawField(g, x, y, ConfigScreenLayout.TEXT_WIDTH, focused(key));
            case FIXED -> {
                drawField(g, x, y, ConfigScreenLayout.FIXED_WIDTH, false);
                JsTechTheme.textS(g, font, Texts.clip(font, GameText.resolve(ConfigScreenTexts.IN_FILE),
                        (int) ((ConfigScreenLayout.FIXED_WIDTH - 6) / JsTechTheme.small())), x + 3, y + 4,
                        JsTechTheme.dim());
            }
        }
    }

    /* A setting's line under its description: its default, and a number's bounds, each with its unit. */
    private String meta(final ConfigKey<?> key) {
        final String unit = unitOf(key);
        final String after = unit.isEmpty() ? "" : " " + unit;
        final Object fallback = key.defaultValue();
        if (fallback instanceof Boolean on) {
            return GameText.resolve(ConfigScreenTexts.DEFAULT_IS.with(GameText.resolve(on ? ConfigScreenTexts.ON
                    : ConfigScreenTexts.OFF)));
        }
        if (key.range().isPresent()) {
            return GameText.resolve(ConfigScreenTexts.DEFAULT_IN_RANGE.with(shown(fallback) + after,
                    shown(key.range().get().min()), shown(key.range().get().max())));
        }
        return GameText.resolve(ConfigScreenTexts.DEFAULT_IS.with(shown(fallback) + after));
    }

    private boolean focused(final ConfigKey<?> key) {
        for (final RowField field : fields) {
            if (field.key() == key && field.box().isFocused()) {
                return true;
            }
        }
        return false;
    }

    private void drawScrollbar(final GuiGraphics g) {
        final int total = shownRows().size();
        if (total <= cards.size() || total == 0) {
            return;
        }
        final int x = ConfigScreenLayout.scrollbarX(width);
        final int top = ConfigScreenLayout.cardsTop(sectionNote().size());
        final int bottom = ConfigScreenLayout.cardsBottom(height);
        g.fill(x, top, x + ConfigScreenLayout.SCROLLBAR, bottom, JsTechTheme.track());
        final int span = bottom - top;
        final int thumb = Math.max(8, span * cards.size() / total);
        final int at = top + (span - thumb) * scroll / Math.max(1, total - cards.size());
        g.fill(x, at, x + ConfigScreenLayout.SCROLLBAR, Math.min(bottom, at + thumb), JsTechTheme.slotEdge());
    }

    /* The whole of a setting's description over the card the pointer rests on, its name above it. */
    private void drawHint(final GuiGraphics g, final Card card, final int mouseX, final int mouseY) {
        final String text = description(card.row().key());
        if (text.isEmpty()) {
            return;
        }
        final int w = ConfigScreenLayout.TOOLTIP_WIDTH;
        final List<FormattedCharSequence> lines = new ArrayList<>();
        for (final String paragraph : text.split(NEW_LINE)) {
            if (!lines.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
            }
            lines.addAll(wrapSmall(paragraph, w - 10));
        }
        final int h = 6 + ConfigScreenLayout.NOTE_LINE + 3 + lines.size() * ConfigScreenLayout.NOTE_LINE + 4;
        final int x = Math.max(2, Math.min(width - w - 2, mouseX + 10));
        final int y = Math.max(2, Math.min(height - h - 2, mouseY + 6));
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x, y, x + w, y + h, PALETTE.get().tipGround());
        Draw.outline(g, x, y, w, h, JsTechTheme.accent());
        JsTechTheme.textS(g, font, Texts.clip(font, nameOf(card.row().key()).getString(),
                (int) ((w - 10) / JsTechTheme.small())), x + 5, y + 5, JsTechTheme.accent());
        for (int line = 0; line < lines.size(); line++) {
            drawSmall(g, lines.get(line), x + 5, y + 6 + ConfigScreenLayout.NOTE_LINE + 3
                    + line * ConfigScreenLayout.NOTE_LINE, JsTechTheme.text());
        }
        g.pose().popPose();
    }

    private void drawFooter(final GuiGraphics g, final int mouseX, final int mouseY) {
        final int top = height - ConfigScreenLayout.FOOTER;
        g.fill(0, top, width, height, JsTechTheme.rail());
        JsTechTheme.hLine(g, 0, top, width);
        final int y = ConfigScreenLayout.footerButtonY(height);
        final boolean defaults = query.isEmpty() && openGroup() != null && editable(openGroup().file());
        footerButton(g, ConfigScreenLayout.PAD, y, ConfigScreenLayout.DEFAULTS_WIDTH,
                ConfigScreenTexts.SECTION_DEFAULTS, defaults ? JsTechTheme.dim() : PALETTE.get().faint(), mouseX,
                mouseY);
        int waiting = 0;
        for (final Map.Entry<ConfigFile, ConfigDraft> draft : drafts.entrySet()) {
            if (editable(draft.getKey())) {
                waiting += draft.getValue().changeCount();
            }
        }
        final String pending = GameText.resolve(waiting == 0 ? ConfigScreenTexts.NOTHING_CHANGED.text()
                : waiting == 1 ? ConfigScreenTexts.ONE_CHANGE.text() : ConfigScreenTexts.CHANGES.with(waiting));
        final int middle = (ConfigScreenLayout.PAD + ConfigScreenLayout.DEFAULTS_WIDTH
                + ConfigScreenLayout.cancelX(width)) / 2;
        JsTechTheme.textCenter(g, font, pending, middle, y + 4, waiting == 0 ? PALETTE.get().faint()
                : JsTechTheme.amber());
        footerButton(g, ConfigScreenLayout.cancelX(width), y, ConfigScreenLayout.CANCEL_WIDTH,
                ConfigScreenTexts.CANCEL, JsTechTheme.text(), mouseX, mouseY);
        final int sx = ConfigScreenLayout.saveX(width);
        g.fill(sx, y, sx + ConfigScreenLayout.SAVE_WIDTH, y + ConfigScreenLayout.BUTTON_HEIGHT, JsTechTheme.accent());
        JsTechTheme.textCenter(g, font, GameText.resolve(ConfigScreenTexts.SAVE),
                sx + ConfigScreenLayout.SAVE_WIDTH / 2, y + 4, PALETTE.get().accentInk());
    }

    private void footerButton(final GuiGraphics g, final int x, final int y, final int w, final TextKey label,
                              final int ink, final int mouseX, final int mouseY) {
        final boolean hovered = over(mouseX, mouseY, x, y, w, ConfigScreenLayout.BUTTON_HEIGHT);
        g.fill(x, y, x + w, y + ConfigScreenLayout.BUTTON_HEIGHT, hovered ? JsTechTheme.hover() : JsTechTheme.panel());
        Draw.outline(g, x, y, w, ConfigScreenLayout.BUTTON_HEIGHT, JsTechTheme.slotEdge());
        JsTechTheme.textCenter(g, font, GameText.resolve(label), x + w / 2, y + 4, ink);
    }

    private void drawSwitch(final GuiGraphics g, final int x, final int y, final boolean on, final boolean editable) {
        final int w = ConfigScreenLayout.SWITCH_WIDTH;
        final int h = ConfigScreenLayout.SWITCH_HEIGHT;
        g.fill(x, y, x + w, y + h, on && editable ? JsTechTheme.accent() : JsTechTheme.slotEdge());
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, JsTechTheme.track());
        final int knobX = on ? x + w - 2 - SWITCH_KNOB : x + 2;
        g.fill(knobX, y + 2, knobX + SWITCH_KNOB, y + h - 2, on && editable ? JsTechTheme.accent()
                : JsTechTheme.dim());
    }

    private void drawStep(final GuiGraphics g, final int x, final int y, final String label, final boolean editable,
                          final int mouseX, final int mouseY) {
        final int side = ConfigScreenLayout.STEP;
        g.fill(x, y, x + side, y + ConfigScreenLayout.CONTROL_HEIGHT, editable
                && over(mouseX, mouseY, x, y, side, ConfigScreenLayout.CONTROL_HEIGHT)
                ? JsTechTheme.hover() : JsTechTheme.panel());
        Draw.outline(g, x, y, side, ConfigScreenLayout.CONTROL_HEIGHT, JsTechTheme.slotEdge());
        JsTechTheme.textCenter(g, font, label, x + side / 2 + 1, y + 3, editable ? JsTechTheme.text()
                : JsTechTheme.dim());
    }

    private void drawField(final GuiGraphics g, final int x, final int y, final int w, final boolean focused) {
        g.fill(x, y, x + w, y + ConfigScreenLayout.CONTROL_HEIGHT, JsTechTheme.slotBg());
        Draw.outline(g, x, y, w, ConfigScreenLayout.CONTROL_HEIGHT, focused ? JsTechTheme.accent()
                : JsTechTheme.slotEdge());
    }

    private void drawSmall(final GuiGraphics g, final FormattedCharSequence line, final int x, final int y,
                           final int colour) {
        final float scale = JsTechTheme.small();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        Draw.text(g, font, line, 0, 0, colour);
        g.pose().popPose();
    }

    private boolean clickTabs(final int x, final int y) {
        if (y < ConfigScreenLayout.TAB_Y || y >= ConfigScreenLayout.TAB_Y + ConfigScreenLayout.TAB_HEIGHT) {
            return false;
        }
        for (int i = 0; i < scopes.size(); i++) {
            final int tx = ConfigScreenLayout.tabX(width, i, scopes.size());
            if (x >= tx && x < tx + ConfigScreenLayout.tabWidth(width, scopes.size())) {
                scope = i;
                select(0);
                return true;
            }
        }
        return false;
    }

    private boolean clickRail(final int x, final int y) {
        if (x >= ConfigScreenLayout.railWidth(width) || scope >= scopes.size()) {
            return false;
        }
        final List<Group> groups = scopes.get(scope).groups();
        for (int i = 0; i < groups.size(); i++) {
            final int top = ConfigScreenLayout.railItemY(i);
            if (y >= top && y < top + ConfigScreenLayout.RAIL_ITEM) {
                select(i);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean clickCard(final int x, final int y, final boolean big) {
        final Card card = cardAt(x, y);
        if (card == null || !editable(card.row().file())) {
            return false;
        }
        final ConfigKey<?> key = card.row().key();
        final ConfigDraft draft = drafts.get(card.row().file());
        final ConfigDraft.Control control = ConfigDraft.controlOf(key);
        final boolean unit = hasUnit(key);
        final int resetX = ConfigScreenLayout.resetX(width, control, unit);
        final int resetY = ConfigScreenLayout.controlY(card.y(), card.height(), ConfigDraft.Control.NUMBER);
        if (draft.isOffDefault(key) && control != ConfigDraft.Control.FIXED
                && over(x, y, resetX, resetY, ConfigScreenLayout.RESET_WIDTH, ConfigScreenLayout.CONTROL_HEIGHT)) {
            draft.toDefault(key);
            refreshField(key, draft);
            return true;
        }
        final int left = ConfigScreenLayout.controlX(width, control, unit);
        final int top = ConfigScreenLayout.controlY(card.y(), card.height(), control);
        final int tall = control == ConfigDraft.Control.TOGGLE ? ConfigScreenLayout.SWITCH_HEIGHT
                : ConfigScreenLayout.CONTROL_HEIGHT;
        if (y < top || y >= top + tall || x < left || x >= left + ConfigScreenLayout.controlWidth(control, unit)) {
            return false;
        }
        switch (control) {
            case TOGGLE -> draft.toggle((ConfigKey<Boolean>) key);
            case NUMBER -> {
                final int plus = left + ConfigScreenLayout.STEP + ConfigScreenLayout.CONTROL_GAP
                        + ConfigScreenLayout.NUMBER_FIELD + ConfigScreenLayout.CONTROL_GAP;
                if (x < left + ConfigScreenLayout.STEP) {
                    draft.step(key, -1, big);
                } else if (x >= plus && x < plus + ConfigScreenLayout.STEP) {
                    draft.step(key, 1, big);
                } else {
                    return false;
                }
                refreshField(key, draft);
            }
            case CHOICE -> draft.cycle((ConfigKey<String>) key, x < left + ConfigScreenLayout.CHOICE_WIDTH / 2 ? -1
                    : 1);
            case TEXT, FIXED -> {
                return false;
            }
        }
        return true;
    }

    private boolean clickFooter(final int x, final int y) {
        final int top = ConfigScreenLayout.footerButtonY(height);
        if (y < top || y >= top + ConfigScreenLayout.BUTTON_HEIGHT) {
            return false;
        }
        final int save = ConfigScreenLayout.saveX(width);
        if (x >= save && x < save + ConfigScreenLayout.SAVE_WIDTH) {
            done();
            return true;
        }
        if (x >= ConfigScreenLayout.cancelX(width)
                && x < ConfigScreenLayout.cancelX(width) + ConfigScreenLayout.CANCEL_WIDTH) {
            back();
            return true;
        }
        final Group group = openGroup();
        if (x >= ConfigScreenLayout.PAD && x < ConfigScreenLayout.PAD + ConfigScreenLayout.DEFAULTS_WIDTH
                && query.isEmpty() && group != null && editable(group.file())) {
            drafts.get(group.file()).defaults(group.keys());
            rebuild();
            return true;
        }
        return false;
    }

    /* The card under the pointer, or none. */
    @Nullable
    private Card cardAt(final int x, final int y) {
        if (x < ConfigScreenLayout.contentLeft(width) || x >= ConfigScreenLayout.contentRight(width)) {
            return null;
        }
        for (final Card card : cards) {
            if (y >= card.y() && y < card.y() + card.height()) {
                return card;
            }
        }
        return null;
    }

    /* How wide a card's text runs, which is where the pointer rests to read its whole description. */
    private int nameRoom(final Card card) {
        final ConfigKey<?> key = card.row().key();
        return ConfigScreenLayout.nameWidth(width, ConfigDraft.controlOf(key), hasUnit(key), true);
    }

    /* Whether the last setting is already on the screen, past which the wheel does not scroll. */
    private boolean lastShown() {
        return cards.isEmpty() || scroll + cards.size() >= shownRows().size();
    }

    /* A number stepped from its buttons, or put back to its default, shows in its field at once. */
    private void refreshField(final ConfigKey<?> key, final ConfigDraft draft) {
        for (final RowField field : fields) {
            if (field.key() == key) {
                field.box().setValue(shown(draft.value(key)));
            }
        }
    }

    private static boolean over(final int mouseX, final int mouseY, final int x, final int y, final int w,
                                final int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /* A value as its field shows it: a number with a fraction without the zeros after its last digit. */
    private static String shown(final Object value) {
        return value instanceof Double fraction ? BigDecimal.valueOf(fraction).stripTrailingZeros().toPlainString()
                : String.valueOf(value);
    }

    /** The settings of one kind of file, the world's, the player's or every game's, by section. */
    private record Scope(ConfigSide side, List<Group> groups) {
    }

    /**
     * One entry of the rail: a section of a file and its settings.
     *
     * @param file    the file
     * @param section the section's dotted path; empty for the settings in none
     * @param keys    its settings that are shown, in the file's order
     * @param title   what the rail and the content's heading call it
     */
    private record Group(ConfigFile file, String section, List<ConfigKey<?>> keys, Component title) {
    }

    /** A setting and the file it is kept in. */
    private record Row(ConfigFile file, ConfigKey<?> key) {
    }

    /** A card on the screen: its setting, where it stands, how tall it is, and the lines of description it shows. */
    private record Card(Row row, int y, int height, List<String> lines) {
    }

    /** A typing field of a card, with the setting it types. */
    private record RowField(ConfigKey<?> key, EditBox box) {
    }

    /**
     * The screen's own colours, beyond the series' theme: the banner of a world's settings read from outside it, its
     * edge, the ground of a hint, the ink on the accent's Save, the faintest text, and the veil over a card only read.
     */
    private record Colours(int lockGround, int lockEdge, int tipGround, int accentInk, int faint, int locked) {
    }
}
