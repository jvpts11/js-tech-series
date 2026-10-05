/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

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
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModContainer;
import org.jetbrains.annotations.Nullable;

/**
 * The settings screen of a mod that keeps its settings the Core's way, in the Core's own look: the mod's files and
 * their sections down the left, the open section's settings on the right, each with a switch, a number to type or
 * step, a word to go through or a text to type, and Done to keep what changed, or Cancel to drop it. A world's
 * settings are changed only from inside that world, on the game that runs it.
 *
 * <p>A mod hands it to the game as its settings screen: {@code container.registerExtensionPoint(
 * IConfigScreenFactory.class, CoreConfigScreen::new)}.
 */
public final class CoreConfigScreen extends Screen {

    private final Screen parent;
    private final String modId;
    private final List<Group> groups = new ArrayList<>();
    private final Map<ConfigFile, ConfigDraft> drafts = new LinkedHashMap<>();
    private final List<RowField> fields = new ArrayList<>();
    private int selected;
    private int scroll;

    private static final int SWITCH_KNOB = 12;
    private static final int CHANGED_MARK = 3;
    private static final int MOST_NOTE_LINES = 3;
    private static final int TOOLTIP_WIDTH = 220;
    private static final String TOOLTIP = ".tooltip";
    private static final String LEFT = "<";
    private static final String RIGHT = ">";
    private static final String MINUS = "-";
    private static final String PLUS = "+";

    public CoreConfigScreen(final ModContainer container, @Nullable final Screen parent) {
        super(GameText.component(ConfigScreenTexts.TITLE.with(container.getModInfo().getDisplayName())));
        this.parent = parent;
        this.modId = container.getModId();
        for (final ConfigFile file : ConfigFiles.of(modId)) {
            // Only the files whose settings are named are shown: the others are their mods' own to show.
            if (file.format() == ConfigFormats.TOML) {
                drafts.put(file, new ConfigDraft(file));
                groupsOf(file);
            }
        }
    }

    /** What the rail lists, top to bottom: each file's heading, then its sections; for a test to read. */
    public List<String> railLabels() {
        final List<String> out = new ArrayList<>();
        ConfigFile last = null;
        for (final Group group : groups) {
            if (group.file() != last) {
                out.add(fileHeading(group.file()).getString());
                last = group.file();
            }
            out.add(group.title().getString());
        }
        return out;
    }

    /** The names of the open section's settings, as the rows show them; for a test to read. */
    public List<String> rowNames() {
        final List<String> out = new ArrayList<>();
        if (selected < groups.size()) {
            for (final ConfigKey<?> key : groups.get(selected).keys()) {
                out.add(nameOf(key).getString());
            }
        }
        return out;
    }

    /** Opens the section the rail lists by that name, as a click on it does; false when there is none. */
    public boolean openSection(final String title) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).title().getString().equals(title)) {
                select(i);
                return true;
            }
        }
        return false;
    }

    /** The draft of the file the open section belongs to, for a test to change a setting as a click would. */
    @Nullable
    public ConfigDraft openDraft() {
        return selected < groups.size() ? drafts.get(groups.get(selected).file()) : null;
    }

    /** Keeps every change and goes back, as Done does. */
    public void done() {
        for (final Map.Entry<ConfigFile, ConfigDraft> draft : drafts.entrySet()) {
            if (editable(draft.getKey())) {
                draft.getValue().apply();
            }
        }
        back();
    }

    @Override
    protected void init() {
        rebuildFields();
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
        final ConfigKey<?> hovered = rowAt(mouseX, mouseY);
        if (hovered != null && mouseX < ConfigScreenLayout.controlX(width, ConfigDraft.controlOf(hovered))) {
            final String key = ConfigTexts.key(modId, hovered.dottedPath()) + TOOLTIP;
            if (I18n.exists(key)) {
                final List<FormattedCharSequence> lines = font.split(Component.translatable(key), TOOLTIP_WIDTH);
                g.renderTooltip(font, lines, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        final int x = (int) mouseX;
        final int y = (int) mouseY;
        return clickRail(x, y) || clickRow(x, y, hasShiftDown()) || clickFooter(x, y);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        if (selected >= groups.size() || mouseX < ConfigScreenLayout.contentLeft()) {
            return false;
        }
        final int most = Math.max(0, groups.get(selected).keys().size() - visibleRows());
        final int next = Math.max(0, Math.min(most, scroll - (int) Math.signum(scrollY)));
        if (next != scroll) {
            scroll = next;
            rebuildFields();
        }
        return true;
    }

    /* Leaving any other way than Done drops what changed. */
    @Override
    public void onClose() {
        back();
    }

    private void back() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    /* The file's settings grouped by the section they sit in, in the order the file declares them. */
    private void groupsOf(final ConfigFile file) {
        final Map<String, List<ConfigKey<?>>> bySection = new LinkedHashMap<>();
        for (final ConfigKey<?> key : file.keys()) {
            if (!key.title().isBlank()) {
                final String section = String.join(".", key.path().subList(0, key.path().size() - 1));
                bySection.computeIfAbsent(section, ignored -> new ArrayList<>()).add(key);
            }
        }
        bySection.forEach((section, keys) -> groups.add(new Group(file, section, List.copyOf(keys),
                section.isEmpty() ? GameText.component(ConfigScreenTexts.GENERAL)
                        : Component.translatable(ConfigTexts.key(modId, section)))));
    }

    private void select(final int index) {
        selected = index;
        scroll = 0;
        rebuildFields();
    }

    /* The typing fields of the rows showing now, made again whenever the rows change. */
    private void rebuildFields() {
        for (final RowField field : fields) {
            removeWidget(field.box());
        }
        fields.clear();
        if (selected >= groups.size()) {
            return;
        }
        final Group group = groups.get(selected);
        final ConfigDraft draft = drafts.get(group.file());
        final boolean editable = editable(group.file());
        final int notes = noteLines(group).size();
        for (int row = 0; row < visibleRows() && scroll + row < group.keys().size(); row++) {
            final ConfigKey<?> key = group.keys().get(scroll + row);
            final ConfigDraft.Control control = ConfigDraft.controlOf(key);
            if (control != ConfigDraft.Control.NUMBER && control != ConfigDraft.Control.TEXT) {
                continue;
            }
            final int y = ConfigScreenLayout.controlY(ConfigScreenLayout.rowY(notes, row), control) + 3;
            final int x = control == ConfigDraft.Control.NUMBER
                    ? ConfigScreenLayout.controlX(width, control) + ConfigScreenLayout.STEP
                    + ConfigScreenLayout.CONTROL_GAP + 3
                    : ConfigScreenLayout.controlX(width, control) + 3;
            final int w = (control == ConfigDraft.Control.NUMBER ? ConfigScreenLayout.NUMBER_FIELD
                    : ConfigScreenLayout.TEXT_WIDTH) - 6;
            final EditBox box = new EditBox(font, x, y, w, ConfigScreenLayout.CONTROL_HEIGHT - 4, nameOf(key));
            box.setBordered(false);
            box.setMaxLength(256);
            box.setTextColor(JsTechTheme.text());
            box.setValue(shown(draft.value(key)));
            box.setEditable(editable);
            box.setResponder(text -> draft.typed(key, text));
            addRenderableWidget(box);
            fields.add(new RowField(key, box));
        }
    }

    private int visibleRows() {
        return selected < groups.size()
                ? ConfigScreenLayout.visibleRows(height, noteLines(groups.get(selected)).size()) : 0;
    }

    /** Whether the file can be changed from here: a world's only inside a world this game runs. */
    private boolean editable(final ConfigFile file) {
        return file.side() != ConfigSide.SERVER || minecraft != null && minecraft.hasSingleplayerServer();
    }

    private Component fileHeading(final ConfigFile file) {
        final TextKey heading = switch (file.side()) {
            case SERVER -> ConfigScreenTexts.WORLD;
            case CLIENT -> ConfigScreenTexts.PLAYER;
            case COMMON -> ConfigScreenTexts.EVERY_GAME;
        };
        return GameText.component(heading);
    }

    private Component nameOf(final ConfigKey<?> key) {
        return Component.translatable(ConfigTexts.key(modId, key.dottedPath()));
    }

    /* The open section's note, its comment wrapped to the content's width, as many lines as the screen gives it. */
    private List<FormattedCharSequence> noteLines(final Group group) {
        final String key = ConfigTexts.key(modId, group.section()) + TOOLTIP;
        if (group.section().isEmpty() || !I18n.exists(key)) {
            return List.of();
        }
        final int wide = (int) ((ConfigScreenLayout.contentRight(width) - ConfigScreenLayout.contentLeft())
                / JsTechTheme.small());
        final List<FormattedCharSequence> lines = font.split(Component.translatable(key), wide);
        return lines.subList(0, Math.min(MOST_NOTE_LINES, lines.size()));
    }

    private void drawHeader(final GuiGraphics g) {
        g.fill(0, 0, width, ConfigScreenLayout.HEADER, JsTechTheme.rail());
        JsTechTheme.hLine(g, 0, ConfigScreenLayout.HEADER - 1, width);
        JsTechTheme.text(g, font, title.getString(), ConfigScreenLayout.PAD, 5, JsTechTheme.text());
        if (selected < groups.size()) {
            JsTechTheme.textSRight(g, font, groups.get(selected).file().fileName(),
                    width - ConfigScreenLayout.PAD, 7, JsTechTheme.dim());
        }
    }

    private void drawRail(final GuiGraphics g, final int mouseX, final int mouseY) {
        g.fill(0, ConfigScreenLayout.HEADER, ConfigScreenLayout.RAIL_WIDTH, height - ConfigScreenLayout.FOOTER,
                JsTechTheme.rail());
        JsTechTheme.vLine(g, ConfigScreenLayout.RAIL_WIDTH - 1, ConfigScreenLayout.HEADER,
                height - ConfigScreenLayout.FOOTER - ConfigScreenLayout.HEADER);
        int y = ConfigScreenLayout.HEADER + ConfigScreenLayout.PAD / 2;
        ConfigFile last = null;
        for (int i = 0; i < groups.size(); i++) {
            final Group group = groups.get(i);
            if (group.file() != last) {
                JsTechTheme.textS(g, font, fileHeading(group.file()).getString(), ConfigScreenLayout.PAD, y + 4,
                        JsTechTheme.dim());
                y += ConfigScreenLayout.RAIL_ITEM;
                last = group.file();
            }
            final boolean on = i == selected;
            if (on) {
                g.fill(0, y, ConfigScreenLayout.RAIL_WIDTH - 1, y + ConfigScreenLayout.RAIL_ITEM, JsTechTheme.tabOn());
                g.fill(0, y, 2, y + ConfigScreenLayout.RAIL_ITEM, JsTechTheme.accent());
            } else if (mouseX < ConfigScreenLayout.RAIL_WIDTH && mouseY >= y
                    && mouseY < y + ConfigScreenLayout.RAIL_ITEM) {
                g.fill(0, y, ConfigScreenLayout.RAIL_WIDTH - 1, y + ConfigScreenLayout.RAIL_ITEM, JsTechTheme.hover());
            }
            final String label = Texts.clip(font, group.title().getString(), ConfigScreenLayout.railLabelWidth());
            JsTechTheme.text(g, font, label, ConfigScreenLayout.PAD + 4, y + 3, on ? JsTechTheme.text()
                    : JsTechTheme.dim());
            y += ConfigScreenLayout.RAIL_ITEM;
        }
    }

    private void drawContent(final GuiGraphics g, final int mouseX, final int mouseY) {
        if (selected >= groups.size()) {
            return;
        }
        final Group group = groups.get(selected);
        final ConfigDraft draft = drafts.get(group.file());
        final boolean editable = editable(group.file());
        final int left = ConfigScreenLayout.contentLeft();
        final int right = ConfigScreenLayout.contentRight(width);
        JsTechTheme.text(g, font, group.title().getString(), left, ConfigScreenLayout.HEADER + ConfigScreenLayout.PAD,
                JsTechTheme.text());
        final List<FormattedCharSequence> notes = noteLines(group);
        for (int line = 0; line < notes.size(); line++) {
            drawSmall(g, notes.get(line), left, ConfigScreenLayout.HEADER + ConfigScreenLayout.PAD
                    + ConfigScreenLayout.TITLE_HEIGHT + line * ConfigScreenLayout.NOTE_LINE, JsTechTheme.dim());
        }
        for (int row = 0; row < visibleRows() && scroll + row < group.keys().size(); row++) {
            final ConfigKey<?> key = group.keys().get(scroll + row);
            final int y = ConfigScreenLayout.rowY(notes.size(), row);
            JsTechTheme.hLine(g, left, y, right - left);
            if (mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + ConfigScreenLayout.ROW_HEIGHT) {
                g.fill(left, y + 1, right, y + ConfigScreenLayout.ROW_HEIGHT, JsTechTheme.hover());
            }
            drawRow(g, draft, key, y, editable, mouseX, mouseY);
        }
        if (!editable) {
            drawSmall(g, GameText.component(ConfigScreenTexts.WORLD_ONLY).getVisualOrderText(), left,
                    ConfigScreenLayout.rowsBottom(height) - ConfigScreenLayout.NOTE_LINE, JsTechTheme.amber());
        }
    }

    private void drawRow(final GuiGraphics g, final ConfigDraft draft, final ConfigKey<?> key, final int y,
                         final boolean editable, final int mouseX, final int mouseY) {
        final ConfigDraft.Control control = ConfigDraft.controlOf(key);
        final int nameX = ConfigScreenLayout.contentLeft() + 4;
        final int room = ConfigScreenLayout.nameWidth(width, control);
        if (draft.isChanged(key)) {
            g.fill(nameX, y + 6, nameX + CHANGED_MARK, y + 6 + CHANGED_MARK, JsTechTheme.amber());
        }
        final int textX = nameX + (draft.isChanged(key) ? CHANGED_MARK + 3 : 0);
        JsTechTheme.text(g, font, Texts.clip(font, nameOf(key).getString(), room - (textX - nameX)), textX,
                y + 4, editable ? JsTechTheme.text() : JsTechTheme.dim());
        final String tooltipKey = ConfigTexts.key(modId, key.dottedPath()) + TOOLTIP;
        if (I18n.exists(tooltipKey)) {
            final String first = I18n.get(tooltipKey).lines().findFirst().orElse("");
            final String fitted = Texts.clip(font, first, (int) (room / JsTechTheme.small()));
            JsTechTheme.textS(g, font, fitted, nameX, y + 14, JsTechTheme.dim());
        }
        final int x = ConfigScreenLayout.controlX(width, control);
        final int top = ConfigScreenLayout.controlY(y, control);
        final Object value = draft.value(key);
        switch (control) {
            case TOGGLE -> drawSwitch(g, x, top, Boolean.TRUE.equals(value), editable);
            case NUMBER -> {
                drawButton(g, x, top, ConfigScreenLayout.STEP, MINUS, editable, mouseX, mouseY);
                drawField(g, x + ConfigScreenLayout.STEP + ConfigScreenLayout.CONTROL_GAP, top,
                        ConfigScreenLayout.NUMBER_FIELD);
                drawButton(g, x + ConfigScreenLayout.STEP + ConfigScreenLayout.CONTROL_GAP
                                + ConfigScreenLayout.NUMBER_FIELD + ConfigScreenLayout.CONTROL_GAP, top,
                        ConfigScreenLayout.STEP, PLUS, editable, mouseX, mouseY);
            }
            case CHOICE -> {
                final int w = ConfigScreenLayout.CHOICE_WIDTH;
                JsTechTheme.button(g, x, top, w, ConfigScreenLayout.CONTROL_HEIGHT, editable
                        && over(mouseX, mouseY, x, top, w, ConfigScreenLayout.CONTROL_HEIGHT));
                JsTechTheme.text(g, font, LEFT, x + 4, top + 3, JsTechTheme.dim());
                JsTechTheme.textRight(g, font, RIGHT, x + w - 4, top + 3, JsTechTheme.dim());
                JsTechTheme.textCenter(g, font, Texts.clip(font, String.valueOf(value), w - 24),
                        x + w / 2, top + 3, editable ? JsTechTheme.accent() : JsTechTheme.dim());
            }
            case TEXT -> drawField(g, x, top, ConfigScreenLayout.TEXT_WIDTH);
            case FIXED -> {
                drawField(g, x, top, ConfigScreenLayout.FIXED_WIDTH);
                JsTechTheme.textS(g, font, Texts.clip(font, GameText.component(ConfigScreenTexts.IN_FILE)
                        .getString(), (int) ((ConfigScreenLayout.FIXED_WIDTH - 6) / JsTechTheme.small())),
                        x + 3, top + 4, JsTechTheme.dim());
            }
        }
    }

    private void drawFooter(final GuiGraphics g, final int mouseX, final int mouseY) {
        final int top = height - ConfigScreenLayout.FOOTER;
        g.fill(0, top, width, height, JsTechTheme.rail());
        JsTechTheme.hLine(g, 0, top, width);
        final int y = ConfigScreenLayout.footerButtonY(height);
        footerButton(g, ConfigScreenLayout.PAD, y, ConfigScreenLayout.DEFAULTS_WIDTH, ConfigScreenTexts.DEFAULTS,
                false, mouseX, mouseY);
        footerButton(g, ConfigScreenLayout.cancelX(width), y, ConfigScreenLayout.CANCEL_WIDTH,
                ConfigScreenTexts.CANCEL, false, mouseX, mouseY);
        footerButton(g, ConfigScreenLayout.doneX(width), y, ConfigScreenLayout.DONE_WIDTH, ConfigScreenTexts.DONE,
                true, mouseX, mouseY);
    }

    private void footerButton(final GuiGraphics g, final int x, final int y, final int w, final TextKey label,
                              final boolean primary, final int mouseX, final int mouseY) {
        final boolean hovered = over(mouseX, mouseY, x, y, w, ConfigScreenLayout.BUTTON_HEIGHT);
        JsTechTheme.button(g, x, y, w, ConfigScreenLayout.BUTTON_HEIGHT, hovered);
        if (primary) {
            outline(g, x, y, w, ConfigScreenLayout.BUTTON_HEIGHT, JsTechTheme.accent());
        }
        JsTechTheme.textCenter(g, font, GameText.component(label).getString(), x + w / 2, y + 4,
                primary ? JsTechTheme.accent() : JsTechTheme.text());
    }

    private void drawSwitch(final GuiGraphics g, final int x, final int y, final boolean on, final boolean editable) {
        g.fill(x, y, x + ConfigScreenLayout.SWITCH_WIDTH, y + ConfigScreenLayout.SWITCH_HEIGHT,
                JsTechTheme.slotEdge());
        g.fill(x + 1, y + 1, x + ConfigScreenLayout.SWITCH_WIDTH - 1, y + ConfigScreenLayout.SWITCH_HEIGHT - 1,
                JsTechTheme.track());
        final int knobX = on ? x + ConfigScreenLayout.SWITCH_WIDTH - 2 - SWITCH_KNOB : x + 2;
        g.fill(knobX, y + 2, knobX + SWITCH_KNOB, y + ConfigScreenLayout.SWITCH_HEIGHT - 2,
                on && editable ? JsTechTheme.accent() : JsTechTheme.dim());
    }

    private void drawButton(final GuiGraphics g, final int x, final int y, final int w, final String label,
                            final boolean editable, final int mouseX, final int mouseY) {
        JsTechTheme.button(g, x, y, w, ConfigScreenLayout.CONTROL_HEIGHT,
                editable && over(mouseX, mouseY, x, y, w, ConfigScreenLayout.CONTROL_HEIGHT));
        JsTechTheme.textCenter(g, font, label, x + w / 2, y + 3, editable ? JsTechTheme.text() : JsTechTheme.dim());
    }

    private void drawField(final GuiGraphics g, final int x, final int y, final int w) {
        g.fill(x, y, x + w, y + ConfigScreenLayout.CONTROL_HEIGHT, JsTechTheme.slotEdge());
        g.fill(x + 1, y + 1, x + w - 1, y + ConfigScreenLayout.CONTROL_HEIGHT - 1, JsTechTheme.slotBg());
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

    private static void outline(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                final int colour) {
        g.fill(x, y, x + w, y + 1, colour);
        g.fill(x, y + h - 1, x + w, y + h, colour);
        g.fill(x, y, x + 1, y + h, colour);
        g.fill(x + w - 1, y, x + w, y + h, colour);
    }

    private boolean clickRail(final int x, final int y) {
        if (x >= ConfigScreenLayout.RAIL_WIDTH) {
            return false;
        }
        int top = ConfigScreenLayout.HEADER + ConfigScreenLayout.PAD / 2;
        ConfigFile last = null;
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).file() != last) {
                top += ConfigScreenLayout.RAIL_ITEM;
                last = groups.get(i).file();
            }
            if (y >= top && y < top + ConfigScreenLayout.RAIL_ITEM) {
                select(i);
                return true;
            }
            top += ConfigScreenLayout.RAIL_ITEM;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean clickRow(final int x, final int y, final boolean big) {
        final ConfigKey<?> key = rowAt(x, y);
        if (key == null || !editable(groups.get(selected).file())) {
            return false;
        }
        final ConfigDraft draft = drafts.get(groups.get(selected).file());
        final ConfigDraft.Control control = ConfigDraft.controlOf(key);
        final int left = ConfigScreenLayout.controlX(width, control);
        final int rowTop = ConfigScreenLayout.rowY(noteLines(groups.get(selected)).size(), rowIndexAt(y));
        final int top = ConfigScreenLayout.controlY(rowTop, control);
        if (y < top || y >= top + ConfigScreenLayout.CONTROL_HEIGHT || x < left
                || x >= left + ConfigScreenLayout.controlWidth(control)) {
            return false;
        }
        switch (control) {
            case TOGGLE -> draft.toggle((ConfigKey<Boolean>) key);
            case NUMBER -> {
                if (x < left + ConfigScreenLayout.STEP) {
                    draft.step(key, -1, big);
                } else if (x >= left + ConfigScreenLayout.controlWidth(control) - ConfigScreenLayout.STEP) {
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
        if (x >= ConfigScreenLayout.doneX(width)
                && x < ConfigScreenLayout.doneX(width) + ConfigScreenLayout.DONE_WIDTH) {
            done();
            return true;
        }
        if (x >= ConfigScreenLayout.cancelX(width)
                && x < ConfigScreenLayout.cancelX(width) + ConfigScreenLayout.CANCEL_WIDTH) {
            back();
            return true;
        }
        if (x >= ConfigScreenLayout.PAD && x < ConfigScreenLayout.PAD + ConfigScreenLayout.DEFAULTS_WIDTH
                && selected < groups.size() && editable(groups.get(selected).file())) {
            drafts.get(groups.get(selected).file()).defaults();
            rebuildFields();
            return true;
        }
        return false;
    }

    /* The setting of the row under the pointer, or none. */
    @Nullable
    private ConfigKey<?> rowAt(final int x, final int y) {
        if (selected >= groups.size() || x < ConfigScreenLayout.contentLeft()
                || x >= ConfigScreenLayout.contentRight(width)) {
            return null;
        }
        final int row = rowIndexAt(y);
        final List<ConfigKey<?>> keys = groups.get(selected).keys();
        return row >= 0 && row < visibleRows() && scroll + row < keys.size() ? keys.get(scroll + row) : null;
    }

    private int rowIndexAt(final int y) {
        final int top = ConfigScreenLayout.rowsTop(noteLines(groups.get(selected)).size());
        return y < top ? -1 : (y - top) / ConfigScreenLayout.ROW_HEIGHT;
    }

    /* A number stepped from its buttons shows in its field at once. */
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

    /** A typing field of a row, with the setting it types. */
    private record RowField(ConfigKey<?> key, EditBox box) {
    }
}
