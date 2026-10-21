/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.ProgressBar;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.style.StylePainter;
import dev.jstech.core.gui.flex.Align;
import dev.jstech.core.gui.flex.FlexBox;
import dev.jstech.core.gui.flex.IMeasure;
import dev.jstech.core.gui.flex.Insets;
import dev.jstech.core.gui.flex.Placed;
import dev.jstech.core.gui.flex.Role;
import dev.jstech.core.gui.flex.Size;
import dev.jstech.core.gui.flex.TextMeasure;
import dev.jstech.core.gui.flex.Track;
import dev.jstech.core.gui.screen.ContainerProps;
import dev.jstech.core.gui.screen.CoreElements;
import dev.jstech.core.gui.screen.ElementSpec;
import dev.jstech.core.gui.style.StylePiece;
import dev.jstech.core.gui.style.StyleRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/** What builds each of the Core's kinds of component ({@link CoreElements}) on the player's game. */
public final class CoreElementBuilders {

    /** The width a bar has when its file gives it none. */
    private static final int BAR_WIDTH = 24;
    /** The height a bar has when its file gives it none. */
    private static final int BAR_HEIGHT = 8;
    /** The width a field has when its file gives it none. */
    private static final int FIELD_WIDTH = 80;
    /** The room between the inventory's grid and its hotbar, as the game leaves it. */
    private static final int HOTBAR_GAP = 4;
    /** The room under the inventory's name. */
    private static final int LABEL_GAP = 2;
    /** The room on each side of a button's text. */
    private static final int BUTTON_PAD = 8;
    /** The translation key of the game's own name for the player's inventory. */
    private static final String INVENTORY = "container.inventory";

    private CoreElementBuilders() {
    }

    /** Registers what builds each of the Core's kinds, once, from the Core's client constructor. */
    @ApiStatus.Internal
    public static void register() {
        ElementBuilders.register(CoreElements.COLUMN, (spec, props, ctx) -> holder(spec, ctx,
                FlexBox.column(name(spec)), null, false));
        ElementBuilders.register(CoreElements.ROW, (spec, props, ctx) -> holder(spec, ctx,
                FlexBox.row(name(spec)), null, false));
        ElementBuilders.register(CoreElements.GRID, (spec, props, ctx) -> holder(spec, ctx,
                FlexBox.grid(name(spec), spec.layout().columns().isEmpty() ? List.of(Track.auto())
                        : spec.layout().columns()), null, false));
        ElementBuilders.register(CoreElements.STACK, (spec, props, ctx) -> holder(spec, ctx,
                FlexBox.stack(name(spec)), null, false));
        ElementBuilders.register(CoreElements.PANEL, (spec, props, ctx) -> holder(spec, ctx,
                FlexBox.column(name(spec)), new PieceView(StylePiece.PANEL), true));
        ElementBuilders.register(CoreElements.WELL, (spec, props, ctx) -> holder(spec, ctx,
                FlexBox.column(name(spec)), new PieceView(StylePiece.WELL), true));
        ElementBuilders.register(CoreElements.TITLE, (spec, props, ctx) -> text(spec, ctx,
                () -> ctx.title().getString(), colour(ctx, CoreElements.Tone.TITLE), CoreElements.TextAlign.LEFT, 1f,
                false));
        ElementBuilders.register(CoreElements.LABEL, CoreElementBuilders::label);
        ElementBuilders.register(CoreElements.BUTTON, CoreElementBuilders::button);
        ElementBuilders.register(CoreElements.SLOT, (spec, props, ctx) -> slots(spec, ctx, props.slot(), 1, 1));
        ElementBuilders.register(CoreElements.SLOTS, (spec, props, ctx) -> slots(spec, ctx, props.from(),
                props.count(), props.perRow()));
        ElementBuilders.register(CoreElements.INVENTORY, CoreElementBuilders::inventory);
        ElementBuilders.register(CoreElements.PROGRESS, CoreElementBuilders::progress);
        ElementBuilders.register(CoreElements.IMAGE, (spec, props, ctx) -> ctx.node(spec, FlexBox.leaf(name(spec),
                IMeasure.fixed(props.imageWidth(), props.imageHeight())).role(Role.SOLID),
                new ImageView(props.sprite()), null, List.of()));
        ElementBuilders.register(CoreElements.SPACER, (spec, props, ctx) -> ctx.node(spec,
                FlexBox.leaf(name(spec), IMeasure.NOTHING).grow(1), null, null, List.of()));
        ElementBuilders.register(CoreElements.SEPARATOR, (spec, props, ctx) -> ctx.node(spec,
                FlexBox.leaf(name(spec), IMeasure.fixed(0, StylePainter.naturalHeight(
                        ctx.skin().style().piece(StylePiece.SEPARATOR)))), new PieceView(StylePiece.SEPARATOR), null,
                List.of()));
        ElementBuilders.register(CoreElements.CHECKBOX, CoreElementBuilders::checkbox);
        ElementBuilders.register(CoreElements.TEXT_FIELD, CoreElementBuilders::textField);
    }

    /* A box that lays others out; a panel or a well draws its piece under them and keeps the style's room inside. */
    private static BuiltNode holder(final ElementSpec spec, final BuildContext ctx, final FlexBox box,
                                    @Nullable final PieceView ground, final boolean padded) {
        final ContainerProps layout = spec.layout();
        layout.applyTo(box, ctx.metrics().gap(), padded ? ctx.metrics().padding() : 0);
        final List<BuiltNode> children = ctx.children(spec);
        for (final BuiltNode child : children) {
            box.add(child.box());
        }
        return ctx.node(spec, box, ground, null, children);
    }

    private static BuiltNode label(final ElementSpec spec, final CoreElements.LabelProps props,
                                   final BuildContext ctx) {
        final Supplier<String> text;
        if (props.bind().isPresent()) {
            final Optional<Supplier<?>> value = ctx.value(props.bind().get());
            final Optional<String> format = props.format().or(props::text);
            text = () -> {
                final Object now = value.map(Supplier::get).orElse(null);
                return format.map(key -> Component.translatable(key, String.valueOf(now)).getString())
                        .orElse(String.valueOf(now));
            };
        } else {
            final String key = props.text().orElse("");
            text = () -> Component.translatable(key).getString();
        }
        return text(spec, ctx, text, colour(ctx, props.tone()), props.textAlign(), props.scale(), props.wrap());
    }

    private static BuiltNode text(final ElementSpec spec, final BuildContext ctx, final Supplier<String> text,
                                  final IntSupplier colour, final CoreElements.TextAlign align, final float scale,
                                  final boolean wrap) {
        final TextMeasure measure = ctx.text(scale, wrap);
        final FlexBox box = FlexBox.leaf(name(spec), maxWidth -> measure.of(text.get()).measure(maxWidth))
                .role(Role.TEXT);
        return ctx.node(spec, box, new TextView(text, colour, align, measure), null, List.of());
    }

    private static BuiltNode button(final ElementSpec spec, final CoreElements.ButtonProps props,
                                    final BuildContext ctx) {
        final String key = props.text().orElse("");
        final Supplier<String> label = () -> Component.translatable(key).getString();
        final Runnable press = press(props, ctx);
        // The keyboard reaches every button of a screen's file, which Enter and Space then press.
        final Button button = new Button(label, press).setPrimary(props.primary()).setFocusable(true);
        final int tall = ctx.metrics().controlHeight();
        final FlexBox box = FlexBox.leaf(name(spec), maxWidth -> new Size(
                ctx.skin().textWidth(ctx.font(), label.get()) + 2 * BUTTON_PAD, tall)).role(Role.SOLID);
        return ctx.node(spec, box, button, null, List.of());
    }

    private static Runnable press(final CoreElements.ButtonProps props, final BuildContext ctx) {
        final Optional<Runnable> action = props.action().flatMap(ctx::action);
        final Optional<Integer> menuButton = props.menuButton();
        return () -> {
            action.ifPresent(Runnable::run);
            menuButton.ifPresent(ctx::pressMenuButton);
        };
    }

    /* A run of the menu's slots: their frames in the style, and the menu's slots moved under them once placed. */
    private static BuiltNode slots(final ElementSpec spec, final BuildContext ctx, final int from, final int count,
                                   final int perRow) {
        final int rows = (count + perRow - 1) / perRow;
        final int columns = Math.min(count, perRow);
        final FlexBox box = FlexBox.leaf(name(spec), IMeasure.fixed(columns * SlotFrame.SIZE, rows * SlotFrame.SIZE))
                .role(Role.SOLID);
        final Consumer<Placed> place = placed -> ctx.menu().ifPresent(menu -> {
            for (int i = 0; i < count; i++) {
                final int index = from + i;
                if (index < menu.slots.size()) {
                    move(menu.slots.get(index), placed.x() + (i % perRow) * SlotFrame.SIZE + 1,
                            placed.y() + (i / perRow) * SlotFrame.SIZE + 1);
                }
            }
        });
        return ctx.node(spec, box, new SlotFrame(count, perRow), place, List.of());
    }

    /* The player's inventory as the game lays it: its name, three rows of nine, and the hotbar four pixels under. */
    private static BuiltNode inventory(final ElementSpec spec, final CoreElements.InventoryProps props,
                                       final BuildContext ctx) {
        final FlexBox column = FlexBox.column(name(spec)).align(Align.START);
        final List<BuiltNode> parts = new ArrayList<>();
        if (props.label()) {
            final Supplier<String> said = () -> Component.translatable(INVENTORY).getString();
            final TextMeasure measure = ctx.text(1f, false);
            final FlexBox label = FlexBox.leaf(name(spec) + " label", maxWidth -> measure.of(said.get())
                    .measure(maxWidth)).role(Role.TEXT).margin(new Insets(0, 0, LABEL_GAP, 0));
            parts.add(new BuiltNode(Optional.empty(), label, new TextView(said, colour(ctx, CoreElements.Tone.TITLE),
                    CoreElements.TextAlign.LEFT, measure), null, () -> true, () -> true, Optional.empty(), List.of()));
        }
        final FlexBox grid = FlexBox.leaf(name(spec) + " grid", IMeasure.fixed(9 * SlotFrame.SIZE,
                3 * SlotFrame.SIZE)).role(Role.SOLID);
        parts.add(new BuiltNode(Optional.empty(), grid, new SlotFrame(27, 9), placed -> placePlayer(ctx, placed, 9),
                () -> true, () -> true, Optional.empty(), List.of()));
        final FlexBox hotbar = FlexBox.leaf(name(spec) + " hotbar", IMeasure.fixed(9 * SlotFrame.SIZE,
                SlotFrame.SIZE)).role(Role.SOLID).margin(new Insets(HOTBAR_GAP, 0, 0, 0));
        parts.add(new BuiltNode(Optional.empty(), hotbar, new SlotFrame(9, 9), placed -> placePlayer(ctx, placed, 0),
                () -> true, () -> true, Optional.empty(), List.of()));
        for (final BuiltNode part : parts) {
            column.add(part.box());
        }
        return ctx.node(spec, column, null, null, parts);
    }

    /* The menu's slots of the player's inventory from {@code first} on, nine to a row, moved under their frames. */
    private static void placePlayer(final BuildContext ctx, final Placed placed, final int first) {
        ctx.menu().ifPresent(menu -> {
            for (final Slot slot : menu.slots) {
                if (!(slot.container instanceof Inventory)) {
                    continue;
                }
                final int index = slot.getContainerSlot() - first;
                final int count = first == 0 ? 9 : 27;
                if (index >= 0 && index < count) {
                    move(slot, placed.x() + (index % 9) * SlotFrame.SIZE + 1,
                            placed.y() + (index / 9) * SlotFrame.SIZE + 1);
                }
            }
        });
    }

    private static BuiltNode progress(final ElementSpec spec, final CoreElements.ProgressProps props,
                                      final BuildContext ctx) {
        final Optional<Supplier<?>> value = ctx.value(props.bind());
        final Optional<Supplier<?>> max = props.maxBind().flatMap(ctx::value);
        final ProgressBar bar = new ProgressBar(() -> {
            final double now = value.<Object>map(Supplier::get).map(CoreElementBuilders::number).orElse(0.0);
            final double most = max.map(m -> number(m.get())).filter(m -> m > 0).orElse((double) props.max());
            return (int) Math.round(Math.max(0, Math.min(1, now / most)) * 100);
        });
        final FlexBox box = FlexBox.leaf(name(spec), IMeasure.fixed(BAR_WIDTH, BAR_HEIGHT)).role(Role.SOLID);
        return ctx.node(spec, box, bar, null, List.of());
    }

    private static BuiltNode checkbox(final ElementSpec spec, final CoreElements.CheckboxProps props,
                                      final BuildContext ctx) {
        final String key = props.text().orElse("");
        final Supplier<String> label = () -> Component.translatable(key).getString();
        final Optional<Supplier<?>> value = props.bind().flatMap(ctx::value);
        final Optional<Runnable> toggle = props.action().flatMap(ctx::action);
        final int size = ctx.metrics().controlHeight();
        final Checkbox box = new Checkbox(label, () -> value.map(v -> truth(v.get())).orElse(false),
                () -> toggle.ifPresent(Runnable::run)).setBoxSize(size).setFocusable(true);
        final FlexBox flex = FlexBox.leaf(name(spec), maxWidth -> new Size(size + 4
                + ctx.skin().textWidth(ctx.font(), label.get()), size)).role(Role.SOLID);
        return ctx.node(spec, flex, box, null, List.of());
    }

    private static BuiltNode textField(final ElementSpec spec, final CoreElements.TextFieldProps props,
                                       final BuildContext ctx) {
        final TextField field = new TextField(props.maxLength());
        props.placeholder().ifPresent(key -> field.setPlaceholder(Component.translatable(key).getString()));
        props.action().flatMap(ctx::textAction).ifPresent(field::setOnCommit);
        props.bind().flatMap(ctx::value).ifPresent(value -> field.sync(String.valueOf(value.get())));
        final FlexBox box = FlexBox.leaf(name(spec), IMeasure.fixed(FIELD_WIDTH, ctx.metrics().controlHeight()))
                .role(Role.SOLID);
        return ctx.node(spec, box, field, null, List.of());
    }

    private static IntSupplier colour(final BuildContext ctx, final CoreElements.Tone tone) {
        final StyleRole role = switch (tone) {
            case TEXT -> StyleRole.TEXT;
            case DIM -> StyleRole.DIM;
            case ACCENT -> StyleRole.ACCENT;
            case TITLE -> StyleRole.TITLE;
        };
        return () -> ctx.skin().colour(role);
    }

    /* The name a box goes by in the layout: the file's id, or its kind. */
    private static String name(final ElementSpec spec) {
        return spec.common().id().orElse(spec.type().id().getPath());
    }

    private static void move(final Slot slot, final int x, final int y) {
        slot.x = x;
        slot.y = y;
    }

    private static double number(final Object value) {
        return value instanceof Number number ? number.doubleValue() : 0;
    }

    private static boolean truth(final Object value) {
        if (value instanceof Boolean yes) {
            return yes;
        }
        return value instanceof Number number && number.doubleValue() != 0;
    }
}
