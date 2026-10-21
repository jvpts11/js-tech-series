/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.ColorPicker;
import dev.jstech.core.client.gui.component.Dropdown;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.Slider;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.TextScale;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.config.ConfigApply;
import dev.jstech.core.config.ConfigColours;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigKeyRange;
import dev.jstech.core.config.ConfigScreenTexts;
import dev.jstech.core.config.ConfigTraits;
import dev.jstech.core.gui.TextSizes;
import dev.jstech.core.gui.layout.ConfigScreenLayout;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.text.GameText;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The rows of the settings screen's list, and the controls in them: a group's header, then each of its settings, its
 * name on the left and its control at the right end, with the button that puts it back to its default between them
 * while it is off it. The control is the one its kind of value calls for: a button that turns it on and off, a slider
 * and the number typed beside it, a number stepped or typed, a drop-down list of its words, a field, a colour picker,
 * a thing picked from the game's, a chooser of its own. A list's entries follow its row, each with the cross that takes
 * it out and a way to add one; a map's, one row each with its value to change.
 *
 * <p>Every control reads the draft each frame and hands it every change, so the rows are built again only when what is
 * shown changes: a section opened, a group folded, a search typed, an entry added or taken out.
 */
final class ConfigRows {

    /** The longest text a field takes. */
    private static final int MOST_TYPED = 256;
    /** The most places a slider stops at before it slides freely. */
    private static final int MOST_STEPS = 101;
    /** The mark of the button that puts a setting back to its default. */
    private static final String RESET_MARK = "↺";
    private static final String MINUS = "-";
    private static final String PLUS = "+";
    /** The mark of the button that takes an entry of a map out. */
    private static final String CROSS_MARK = "×";
    /** How many decimal places a number with a fraction keeps when it is slid. */
    private static final double FRACTION_PLACES = 1000.0;

    private ConfigRows() {
    }

    /** What the rows need of the screen that shows them. */
    interface Host {

        ConfigScreenModel model();

        Font font();

        /** The skin the screen wears, for the widths of texts and the colours of the style. */
        StyleSkin skin();

        /** The screen's width, which places every control. */
        int screenWidth();

        /** Whether {@code file} can be changed here. */
        boolean editable(ConfigFile file);

        /** Whether a group of {@code file} is folded. */
        boolean folded(ConfigFile file, String group);

        /** Folds a group, or opens it. */
        void toggleFold(ConfigFile file, String group);

        /** Opens the picker of the game's things of {@code kind}, handing the chosen id to {@code chosen}. */
        void pick(ConfigThings.Kind kind, Consumer<String> chosen);

        /** Opens the chooser of {@code key}. */
        void choose(ConfigKey<?> key, ConfigDraft draft);

        /** Shows the whole description of {@code key}, pinned, its link to click. */
        void describe(ConfigKey<?> key);

        /** What is shown changed: the rows are built again. */
        void rebuild();
    }

    /** A row: its height, the controls in it, and where to put them once the row's top is known. */
    abstract static class Row {

        final List<UiComponent> parts = new ArrayList<>();
        private int top;

        /** How tall it is. */
        abstract int height();

        /** Puts the row's controls in place, its top at {@code y}. */
        abstract void place(int y);

        /** The setting it shows, when it shows one, for the description under the pointer. */
        Optional<ConfigKey<?>> key() {
            return Optional.empty();
        }

        /** The part the pointer rests on to read the setting's description, or none. */
        Optional<UiComponent> describedPart() {
            return Optional.empty();
        }

        /** Where its top is down the list's content. */
        int top() {
            return top;
        }

        void setTop(final int value) {
            top = value;
        }

        <T extends UiComponent> T add(final T part) {
            parts.add(part);
            return part;
        }
    }

    /** The rows of {@code groups} of {@code file}, of the settings {@code shown} keeps, in order. */
    static List<Row> build(final Host host, final ConfigFile file, final List<ConfigScreenModel.Group> groups,
                           final ShownFilter shown) {
        final List<Row> rows = new ArrayList<>();
        final ConfigDraft draft = host.model().draft(file);
        for (final ConfigScreenModel.Group group : groups) {
            final List<ConfigKey<?>> keys = group.keys().stream().filter(key -> shown.keeps(file, key)).toList();
            if (keys.isEmpty()) {
                continue;
            }
            if (!group.path().isEmpty()) {
                rows.add(new HeaderRow(host, file, group, keys.size()));
                if (host.folded(file, group.path())) {
                    continue;
                }
            }
            for (final ConfigKey<?> key : keys) {
                if (draft.isShown(key)) {
                    addSetting(host, file, draft, key, rows);
                }
            }
        }
        return rows;
    }

    /** Which settings are shown: those a search finds, and only the changed ones under the filter. */
    @FunctionalInterface
    interface ShownFilter {

        boolean keeps(ConfigFile file, ConfigKey<?> key);
    }

    /** A value as a field shows it: a number with a fraction without the zeros after its last digit. */
    static String shown(final Object value) {
        // NaN and the infinities have no decimal form; they are shown as they print.
        return value instanceof Double fraction && Double.isFinite(fraction)
                ? BigDecimal.valueOf(fraction).stripTrailingZeros().toPlainString()
                : String.valueOf(value);
    }

    private static void addSetting(final Host host, final ConfigFile file, final ConfigDraft draft,
                                   final ConfigKey<?> key, final List<Row> rows) {
        final SettingRow row = new SettingRow(host, file, draft, key);
        rows.add(row);
        switch (ConfigDraft.controlOf(key)) {
            case IDS, LIST -> addChips(host, file, draft, key, rows);
            case MAP -> addMapRows(host, file, draft, key, rows);
            default -> {
            }
        }
    }

    /* The entries of a list, as chips that wrap from row to row, the last row ending in the way to add one. */
    private static void addChips(final Host host, final ConfigFile file, final ConfigDraft draft,
                                 final ConfigKey<?> key, final List<Row> rows) {
        final ConfigDraft.Control control = ConfigDraft.controlOf(key);
        final boolean things = control == ConfigDraft.Control.IDS;
        final ConfigThings.Kind kind = ConfigThings.Kind.of(key.traits().control());
        final boolean editable = host.editable(file);
        final int left = ConfigScreenLayout.nameX(host.screenWidth()) + 4;
        final int right = ConfigScreenLayout.controlRight(host.screenWidth());
        final List<Object> entries = draft.entries(key);
        ChipRow current = new ChipRow();
        int x = left;
        for (int i = 0; i < entries.size(); i++) {
            final int index = i;
            final String raw = String.valueOf(entries.get(i));
            final ConfigThings.Thing thing = things ? ConfigThings.of(kind, raw) : null;
            final String text = thing == null ? raw : thing.name().getString();
            final int w = ConfigWidgets.Chip.widthOf(host.skin(), host.font(), text, things);
            if (x + w > right && x > left) {
                rows.add(current);
                current = new ChipRow();
                x = left;
            }
            final ConfigWidgets.Chip chip = new ConfigWidgets.Chip(text, thing == null ? ItemStack.EMPTY
                    : thing.icon(), things, List.of(Component.literal(raw)), () -> {
                        draft.removeEntry(key, index);
                        host.rebuild();
                    });
            chip.setEnabled(editable);
            current.place(chip, x, w);
            x += w + ConfigScreenLayout.GAP + 2;
        }
        if (editable) {
            final String add = GameText.resolve(ConfigScreenTexts.ADD);
            final int addWidth = host.skin().textWidth(host.font(), add) + ConfigScreenLayout.BUTTON_PADDING;
            final int fieldWidth = things ? 0 : Math.min(100, right - left - addWidth - ConfigScreenLayout.GAP);
            if (x + fieldWidth + addWidth + ConfigScreenLayout.GAP > right && x > left) {
                rows.add(current);
                current = new ChipRow();
                x = left;
            }
            if (things) {
                final Button button = new Button(add, () -> host.pick(kind, id -> {
                    if (draft.addEntry(key, id)) {
                        host.rebuild();
                    }
                })).setFocusable(true);
                current.place(button, x, addWidth);
            } else {
                final TextField field = new TextField(MOST_TYPED);
                field.setPlaceholder(GameText.resolve(ConfigScreenTexts.NEW_ENTRY));
                final Runnable take = () -> {
                    final String typed = field.edit().isBlank() ? field.value() : field.edit();
                    if (!typed.isBlank() && draft.addEntry(key, typed)) {
                        host.rebuild();
                    }
                };
                field.setOnCommit(typed -> take.run());
                current.place(field, x, fieldWidth);
                current.place(new Button(add, take).setFocusable(true), x + fieldWidth + ConfigScreenLayout.GAP,
                        addWidth);
            }
        }
        if (!current.parts.isEmpty()) {
            rows.add(current);
        }
    }

    /* A row for each entry of a map, its value changed in a field, then a row to add one. */
    private static void addMapRows(final Host host, final ConfigFile file, final ConfigDraft draft,
                                   final ConfigKey<?> key, final List<Row> rows) {
        final boolean editable = host.editable(file);
        for (final Map.Entry<String, Object> entry : draft.mapEntries(key).entrySet()) {
            rows.add(new MapEntryRow(host, draft, key, entry.getKey(), editable));
        }
        if (editable) {
            rows.add(new MapAddRow(host, draft, key));
        }
    }

    /** A line of text across the list: a section's name over what a search found in it, or why nothing is shown. */
    static Row title(final Host host, final String text, final boolean heading) {
        return new TitleRow(host, text, heading);
    }

    /** A line of text across the list. */
    private static final class TitleRow extends Row {

        private final int width;
        private final Label label;

        TitleRow(final Host host, final String text, final boolean heading) {
            this.width = host.screenWidth();
            this.label = add(new Label(text, Label.Tone.DIM));
            if (heading) {
                final StyleSkin skin = host.skin();
                this.label.setColor(() -> skin.colour(StyleRole.HEADING));
            }
        }

        @Override
        int height() {
            return ConfigScreenLayout.GROUP_GAP + ConfigScreenLayout.GROUP_ROW;
        }

        @Override
        void place(final int y) {
            final int x = ConfigScreenLayout.nameX(width);
            label.setBounds(x, y + ConfigScreenLayout.GROUP_GAP, ConfigScreenLayout.controlRight(width) - x,
                    ConfigScreenLayout.GROUP_ROW);
        }
    }

    /** A group's header, which folds it. */
    private static final class HeaderRow extends Row {

        private final UiComponent header;
        private final int width;

        HeaderRow(final Host host, final ConfigFile file, final ConfigScreenModel.Group group, final int count) {
            this.width = host.screenWidth();
            this.header = add(new ConfigWidgets.FoldHeader(group.title(), GameText.resolve(count == 1
                    ? ConfigScreenTexts.ONE_SETTING.text() : ConfigScreenTexts.SETTINGS.with(count)),
                    () -> !host.folded(file, group.path()), () -> host.toggleFold(file, group.path())));
        }

        @Override
        int height() {
            return ConfigScreenLayout.GROUP_GAP + ConfigScreenLayout.GROUP_ROW;
        }

        @Override
        void place(final int y) {
            final int x = ConfigScreenLayout.nameX(width);
            header.setBounds(x, y + ConfigScreenLayout.GROUP_GAP, ConfigScreenLayout.controlRight(width) - x,
                    ConfigScreenLayout.GROUP_ROW);
        }
    }

    /** A setting: its name with its marks, its control, and the button back to its default. */
    private static final class SettingRow extends Row {

        private final ConfigKey<?> key;
        private final ConfigDraft draft;
        private final int width;
        private final boolean noted;
        private final ConfigWidgets.SettingName name;
        private final Button reset;
        private final List<UiComponent> controls = new ArrayList<>();
        private final List<int[]> spans = new ArrayList<>();
        private final boolean editable;
        private final float textScale;
        private final int nameLines;

        SettingRow(final Host host, final ConfigFile file, final ConfigDraft draft, final ConfigKey<?> key) {
            this.key = key;
            this.draft = draft;
            this.width = host.screenWidth();
            this.editable = host.editable(file);
            final String note = noteOf(host, file, key);
            this.noted = !note.isEmpty();
            final List<ConfigWidgets.Mark> marks = new ArrayList<>();
            if (key.traits().applies() == ConfigApply.RESTART) {
                marks.add(new ConfigWidgets.Mark(GameText.resolve(ConfigScreenTexts.NEEDS_RESTART),
                        StyleRole.NOTICE));
            } else if (key.traits().applies() == ConfigApply.RELOAD) {
                marks.add(new ConfigWidgets.Mark(GameText.resolve(ConfigScreenTexts.ON_RELOAD), StyleRole.INFO));
            }
            final Component title = host.model().name(key);
            // Measured beside the button back to the default, so the row keeps its height as the button comes and goes,
            // at the size the player wants the text: a larger one takes more lines, and taller ones.
            this.textScale = TextScale.factor();
            this.nameLines = ConfigScreenLayout.nameLines(title.getString(), ConfigScreenLayout.nameWidth(width, true),
                    text -> TextScale.width(host.skin(), host.font(), text, textScale)).size();
            this.name = add(new ConfigWidgets.SettingName(title::getString, marks, note, () -> draft.isActive(key),
                    () -> draft.isChanged(key), () -> host.describe(key)));
            this.reset = add(new Button(RESET_MARK, () -> draft.toDefault(key)).setFocusable(true));
            buildControl(host);
        }

        @Override
        int height() {
            return ConfigScreenLayout.rowHeight(noted, nameLines, TextSizes.grown(ConfigScreenLayout.LINE, textScale));
        }

        @Override
        Optional<ConfigKey<?>> key() {
            return Optional.of(key);
        }

        @Override
        Optional<UiComponent> describedPart() {
            return Optional.of(name);
        }

        @Override
        void place(final int y) {
            final int h = height();
            final boolean resettable = editable && draft.isOffDefault(key)
                    && ConfigDraft.controlOf(key) != ConfigDraft.Control.FIXED;
            name.setBounds(ConfigScreenLayout.nameX(width), y, ConfigScreenLayout.nameWidth(width, resettable), h);
            reset.setVisible(resettable);
            reset.setBounds(ConfigScreenLayout.resetX(width), ConfigScreenLayout.controlY(y, h),
                    ConfigScreenLayout.RESET, ConfigScreenLayout.CONTROL_HEIGHT);
            final boolean active = draft.isActive(key);
            final int cy = ConfigScreenLayout.controlY(y, h);
            for (int i = 0; i < controls.size(); i++) {
                final int[] span = spans.get(i);
                controls.get(i).setBounds(span[0], cy, span[1], ConfigScreenLayout.CONTROL_HEIGHT);
                controls.get(i).setEnabled(editable && active);
            }
        }

        /* Puts a control in the row, {@code from} pixels from the control's left, {@code w} wide. */
        private <T extends UiComponent> T control(final T part, final int from, final int w) {
            controls.add(add(part));
            spans.add(new int[] {ConfigScreenLayout.controlX(width) + from, w});
            return part;
        }

        @SuppressWarnings("unchecked")
        private void buildControl(final Host host) {
            final int cw = ConfigScreenLayout.controlWidth(width);
            switch (ConfigDraft.controlOf(key)) {
                case TOGGLE -> {
                    final ConfigKey<Boolean> flag = (ConfigKey<Boolean>) key;
                    control(new Button(() -> GameText.resolve(draft.value(flag) ? ConfigScreenTexts.ON
                            : ConfigScreenTexts.OFF), () -> draft.toggle(flag)).setFocusable(true), 0, cw);
                }
                case NUMBER -> {
                    final int step = ConfigScreenLayout.CONTROL_HEIGHT;
                    control(numberField(), 0, cw - 2 * (step + 2));
                    control(new Button(MINUS, () -> draft.step(key, -1, false)).setFocusable(true),
                            cw - 2 * step - 2, step);
                    control(new Button(PLUS, () -> draft.step(key, 1, false)).setFocusable(true), cw - step, step);
                }
                case SLIDER -> {
                    final int field = ConfigScreenLayout.NUMBER_FIELD;
                    control(slider(host), 0, cw - field - ConfigScreenLayout.GAP);
                    control(numberField(), cw - field, field);
                }
                case CHOICE -> {
                    final ConfigKey<String> word = (ConfigKey<String>) key;
                    final List<String> words = word.allowed().orElse(List.of());
                    final List<Supplier<String>> options = new ArrayList<>();
                    for (final String one : words) {
                        options.add(() -> host.model().word(word, one));
                    }
                    control(new Dropdown(options, () -> words.indexOf(draft.value(word)),
                            index -> draft.typed(word, words.get(index)), shown -> shown), 0, cw);
                }
                case TEXT -> control(new SyncedField(() -> String.valueOf(draft.value(key)))
                        .setOnCommit(typed -> draft.typed(key, typed)), 0, cw);
                case COLOUR -> {
                    final ConfigKey<String> colour = (ConfigKey<String>) key;
                    final StyleSkin skin = host.skin();
                    final List<Integer> swatches = new ArrayList<>();
                    for (final StyleRole role : List.of(StyleRole.ACCENT, StyleRole.ACCENT2, StyleRole.GREEN,
                            StyleRole.NOTICE, StyleRole.WARNING, StyleRole.INFO, StyleRole.HEADING,
                            StyleRole.MENU_TEXT)) {
                        swatches.add(skin.colour(role));
                    }
                    control(new ColorPicker(() -> 0xFF << 24 | draft.colourOf(colour),
                            argb -> draft.slide(colour, ConfigColours.write(argb)), swatches), 0, cw);
                }
                case CHOOSE -> control(new Button(() -> ConfigChoosers.describe(key, draft.value(key)).getString(),
                        () -> host.choose(key, draft)).setFocusable(true), 0, cw);
                case ID -> {
                    final ConfigKey<String> id = (ConfigKey<String>) key;
                    final ConfigThings.Kind kind = ConfigThings.Kind.of(key.traits().control());
                    control(new ConfigWidgets.ThingButton(() -> ConfigThings.of(kind, draft.value(id)),
                            () -> host.pick(kind, chosen -> draft.typed(id, chosen))), 0, cw);
                }
                case FIXED -> control(new Label(GameText.resolve(ConfigScreenTexts.IN_FILE), Label.Tone.DIM), 0,
                        cw);
                case IDS, LIST, MAP -> {
                }
            }
        }

        /* A number typed, shown as the draft holds it while it is not being typed in. */
        private UiComponent numberField() {
            return new SyncedField(() -> shown(draft.value(key))).setOnCommit(typed -> draft.typed(key, typed));
        }

        /* A number held to a range, slid along it: in steps when it is whole and its range is short. */
        private UiComponent slider(final Host host) {
            final ConfigKeyRange<?> range = key.range().orElseThrow();
            final double min = range.min().doubleValue();
            final double max = range.max().doubleValue();
            final boolean whole = !(key.defaultValue() instanceof Double);
            final int steps = whole && max - min + 1 <= MOST_STEPS ? (int) (max - min + 1) : 0;
            final String unit = host.model().unit(key);
            return new Slider(() -> max <= min ? 0 : (((Number) draft.value(key)).doubleValue() - min) / (max - min),
                    fraction -> slideTo(min + fraction * (max - min)),
                    fraction -> shown(valueAt(min + fraction * (max - min))) + (unit.isEmpty() ? "" : " " + unit),
                    steps);
        }

        @SuppressWarnings("unchecked")
        private void slideTo(final double raw) {
            final Object value = valueAt(raw);
            if (value instanceof Integer whole) {
                draft.slide((ConfigKey<Integer>) key, whole);
            } else if (value instanceof Long whole) {
                draft.slide((ConfigKey<Long>) key, whole);
            } else if (value instanceof Double fraction) {
                draft.slide((ConfigKey<Double>) key, fraction);
            }
        }

        /* A slid number as the setting's own kind: whole ones rounded, others to a thousandth. */
        private Object valueAt(final double raw) {
            final Object kind = key.defaultValue();
            if (kind instanceof Integer) {
                return (int) Math.round(raw);
            }
            if (kind instanceof Long) {
                return Math.round(raw);
            }
            return Math.round(raw * FRACTION_PLACES) / FRACTION_PLACES;
        }

        /* The line under a setting's name: the setting it is active under, and what that one must be. */
        private static String noteOf(final Host host, final ConfigFile file, final ConfigKey<?> key) {
            final Optional<ConfigTraits.Condition> condition = key.traits().activeIf();
            if (condition.isEmpty() || condition.get().hides()) {
                return "";
            }
            final Optional<ConfigKey<?>> other = file.key(condition.get().path());
            if (other.isEmpty()) {
                return "";
            }
            final String otherName = host.model().name(other.get()).getString();
            final List<String> values = condition.get().values();
            if (values.equals(List.of(String.valueOf(true)))) {
                return GameText.resolve(ConfigScreenTexts.ACTIVE_WHEN_ON.with(otherName));
            }
            final List<String> words = new ArrayList<>();
            for (final String value : values) {
                words.add(other.get().defaultValue() instanceof String ? host.model().word(cast(other.get()), value)
                        : value);
            }
            return GameText.resolve(ConfigScreenTexts.ACTIVE_WHEN.with(otherName, String.join(", ", words)));
        }

        @SuppressWarnings("unchecked")
        private static ConfigKey<String> cast(final ConfigKey<?> key) {
            return (ConfigKey<String>) key;
        }
    }

    /** A row of a list's entries, and on the last, the way to add one. */
    private static final class ChipRow extends Row {

        private final List<int[]> spans = new ArrayList<>();

        void place(final UiComponent part, final int x, final int w) {
            add(part);
            spans.add(new int[] {x, w});
        }

        @Override
        int height() {
            return ConfigScreenLayout.ENTRY_ROW;
        }

        @Override
        void place(final int y) {
            for (int i = 0; i < parts.size(); i++) {
                parts.get(i).setBounds(spans.get(i)[0], y + 1, spans.get(i)[1], ConfigScreenLayout.CONTROL_HEIGHT);
            }
        }
    }

    /** An entry of a map: its name, its value in a field, and the cross that takes it out. */
    private static final class MapEntryRow extends Row {

        private final int width;
        private final Label name;
        private final UiComponent value;
        private final Button remove;

        MapEntryRow(final Host host, final ConfigDraft draft, final ConfigKey<?> key, final String entry,
                    final boolean editable) {
            this.width = host.screenWidth();
            this.name = add(new Label(entry, Label.Tone.DIM));
            // A value the map does not take is put back by the field's next frame, which shows the draft's again.
            this.value = add(new SyncedField(() -> shown(draft.mapEntries(key).get(entry)))
                    .setOnCommit(typed -> draft.putMapEntry(key, entry, typed)));
            this.value.setEnabled(editable);
            this.remove = add(new Button(CROSS_MARK, () -> {
                draft.removeMapEntry(key, entry);
                host.rebuild();
            }).setFocusable(true));
            this.remove.setVisible(editable);
        }

        @Override
        int height() {
            return ConfigScreenLayout.ENTRY_ROW;
        }

        @Override
        void place(final int y) {
            final int nameX = ConfigScreenLayout.nameX(width) + 10;
            final int controlX = ConfigScreenLayout.controlX(width);
            final int right = ConfigScreenLayout.controlRight(width);
            name.setBounds(nameX, y + 1, controlX - ConfigScreenLayout.GAP - nameX,
                    ConfigScreenLayout.CONTROL_HEIGHT);
            value.setBounds(controlX, y + 1, right - controlX - ConfigScreenLayout.RESET - ConfigScreenLayout.GAP,
                    ConfigScreenLayout.CONTROL_HEIGHT);
            remove.setBounds(right - ConfigScreenLayout.RESET, y + 1, ConfigScreenLayout.RESET,
                    ConfigScreenLayout.CONTROL_HEIGHT);
        }
    }

    /** The row that adds an entry to a map: its name and its value typed, then Add. */
    private static final class MapAddRow extends Row {

        private final int width;
        private final int addWidth;
        private final TextField name;
        private final TextField value;
        private final Button add;

        MapAddRow(final Host host, final ConfigDraft draft, final ConfigKey<?> key) {
            this.width = host.screenWidth();
            this.name = add(new TextField(MOST_TYPED));
            this.name.setPlaceholder(GameText.resolve(ConfigScreenTexts.NEW_NAME));
            this.value = add(new TextField(MOST_TYPED));
            this.value.setPlaceholder(GameText.resolve(ConfigScreenTexts.NEW_VALUE));
            final String label = GameText.resolve(ConfigScreenTexts.ADD);
            this.addWidth = host.skin().textWidth(host.font(), label) + ConfigScreenLayout.BUTTON_PADDING;
            this.add = add(new Button(label, () -> {
                final String entry = name.edit().isBlank() ? name.value() : name.edit();
                final String typed = value.edit().isBlank() ? value.value() : value.edit();
                if (draft.putMapEntry(key, entry, typed)) {
                    host.rebuild();
                }
            }).setFocusable(true));
        }

        @Override
        int height() {
            return ConfigScreenLayout.ENTRY_ROW;
        }

        @Override
        void place(final int y) {
            final int nameX = ConfigScreenLayout.nameX(width) + 10;
            final int controlX = ConfigScreenLayout.controlX(width);
            final int right = ConfigScreenLayout.controlRight(width);
            name.setBounds(nameX, y + 1, controlX - ConfigScreenLayout.GAP - nameX,
                    ConfigScreenLayout.CONTROL_HEIGHT);
            value.setBounds(controlX, y + 1, right - controlX - addWidth - ConfigScreenLayout.GAP,
                    ConfigScreenLayout.CONTROL_HEIGHT);
            add.setBounds(right - addWidth, y + 1, addWidth, ConfigScreenLayout.CONTROL_HEIGHT);
        }
    }

    /** A field showing a value the draft holds, kept up to date each frame while nobody types in it. */
    private static final class SyncedField extends TextField {

        private final Supplier<String> value;

        SyncedField(final Supplier<String> value) {
            super(MOST_TYPED);
            this.value = value;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            sync(value.get());
            super.render(g, ctx);
        }
    }
}
