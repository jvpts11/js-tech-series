/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import dev.jstech.core.client.GameLocale;
import dev.jstech.core.client.gui.component.ChartView;
import dev.jstech.core.client.gui.component.ColorPicker;
import dev.jstech.core.client.gui.component.Dropdown;
import dev.jstech.core.client.gui.component.EnergyBar;
import dev.jstech.core.client.gui.component.GhostSlot;
import dev.jstech.core.client.gui.component.GraphEditor;
import dev.jstech.core.client.gui.component.MapView;
import dev.jstech.core.client.gui.component.ModelView;
import dev.jstech.core.client.gui.component.RadioGroup;
import dev.jstech.core.client.gui.component.ScaleView;
import dev.jstech.core.client.gui.component.Slider;
import dev.jstech.core.client.gui.component.Switch;
import dev.jstech.core.client.gui.component.TableView;
import dev.jstech.core.client.gui.component.TankGauge;
import dev.jstech.core.client.gui.component.TreeView;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.logic.ColorMath;
import dev.jstech.core.client.gui.logic.GraphModel;
import dev.jstech.core.client.gui.logic.TableModel;
import dev.jstech.core.client.gui.logic.TreeModel;
import dev.jstech.core.gui.flex.FlexBox;
import dev.jstech.core.gui.flex.IMeasure;
import dev.jstech.core.gui.flex.Role;
import dev.jstech.core.gui.flex.Size;
import dev.jstech.core.gui.flex.TextMeasure;
import dev.jstech.core.gui.screen.ControlElements;
import dev.jstech.core.gui.screen.ElementSpec;
import dev.jstech.core.gui.screen.InterfaceTexts;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.ApiStatus;

/** What builds each of the richer kinds of component ({@link ControlElements}) on the player's game. */
public final class ControlElementBuilders {

    private static final int SLIDER_WIDTH = 120;
    private static final int TICKS = 4;
    private static final int DROPDOWN_WIDTH = 120;
    private static final int COLOUR_WIDTH = 80;
    private static final int VIEW_WIDTH = 120;
    private static final int VIEW_HEIGHT = 96;
    private static final int GAUGE_WIDTH = 18;
    private static final int GAUGE_HEIGHT = 52;
    private static final int SLOT = 18;
    private static final String PLAIN = "%s";

    private ControlElementBuilders() {
    }

    /** Registers what builds each of these kinds, once, from the Core's client constructor. */
    @ApiStatus.Internal
    public static void register() {
        ElementBuilders.register(ControlElements.SLIDER, ControlElementBuilders::slider);
        ElementBuilders.register(ControlElements.SWITCH, ControlElementBuilders::toggle);
        ElementBuilders.register(ControlElements.RADIO, ControlElementBuilders::radio);
        ElementBuilders.register(ControlElements.DROPDOWN, ControlElementBuilders::dropdown);
        ElementBuilders.register(ControlElements.COLOR, ControlElementBuilders::colour);
        ElementBuilders.register(ControlElements.TREE, ControlElementBuilders::tree);
        ElementBuilders.register(ControlElements.TABLE, ControlElementBuilders::table);
        ElementBuilders.register(ControlElements.GRAPH, ControlElementBuilders::graph);
        ElementBuilders.register(ControlElements.MODEL, ControlElementBuilders::model);
        ElementBuilders.register(ControlElements.CHART, ControlElementBuilders::chart);
        ElementBuilders.register(ControlElements.SCALE, ControlElementBuilders::scale);
        ElementBuilders.register(ControlElements.MAP, ControlElementBuilders::map);
        ElementBuilders.register(ControlElements.TANK, ControlElementBuilders::tank);
        ElementBuilders.register(ControlElements.ENERGY, ControlElementBuilders::energy);
        ElementBuilders.register(ControlElements.GHOST_SLOT, ControlElementBuilders::ghost);
    }

    private static BuiltNode slider(final ElementSpec spec, final ControlElements.SliderProps props,
                                    final BuildContext ctx) {
        final Optional<Supplier<?>> value = ctx.value(props.bind());
        final Consumer<Object> handler = props.action().map(ctx::handler).orElse(v -> {
        });
        final double span = props.max() - props.min();
        final String format = props.format().orElse(PLAIN);
        final Slider slider = new Slider(
                () -> span == 0 ? 0 : (number(value) - props.min()) / span,
                fraction -> handler.accept(props.min() + fraction * span),
                fraction -> Component.translatable(format, shown(props.min() + fraction * span)).getString(),
                props.steps());
        final int tall = ctx.metrics().controlHeight() + (props.steps() >= 2 ? TICKS : 0);
        return leaf(spec, ctx, slider, SLIDER_WIDTH, tall);
    }

    private static BuiltNode toggle(final ElementSpec spec, final ControlElements.ToggleProps props,
                                    final BuildContext ctx) {
        final String key = props.text().orElse("");
        final Optional<Supplier<?>> value = props.bind().flatMap(ctx::value);
        final Runnable flip = props.action().flatMap(ctx::action).orElse(() -> {
        });
        final Switch toggle = new Switch(() -> Component.translatable(key).getString(), () -> truth(value), flip);
        final FlexBox box = FlexBox.leaf(name(spec), maxWidth -> new Size(Switch.TRACK_WIDTH + Switch.LABEL_GAP
                + ctx.skin().textWidth(ctx.font(), Component.translatable(key).getString()), Switch.TRACK_HEIGHT + 2))
                .role(Role.SOLID);
        return ctx.node(spec, box, toggle, null, List.of());
    }

    private static BuiltNode radio(final ElementSpec spec, final ControlElements.ChoiceProps props,
                                   final BuildContext ctx) {
        final List<Supplier<String>> options = options(props);
        final Optional<Supplier<?>> value = props.bind().flatMap(ctx::value);
        final Consumer<Object> handler = props.action().map(ctx::handler).orElse(v -> {
        });
        final RadioGroup group = new RadioGroup(options, () -> (int) number(value), handler::accept);
        int widest = 0;
        for (final Supplier<String> option : options) {
            widest = Math.max(widest, ctx.skin().textWidth(ctx.font(), option.get()));
        }
        final int wide = RadioGroup.BUTTON + RadioGroup.LABEL_GAP + widest;
        return leaf(spec, ctx, group, wide, group.neededHeight());
    }

    private static BuiltNode dropdown(final ElementSpec spec, final ControlElements.ChoiceProps props,
                                      final BuildContext ctx) {
        final Optional<Supplier<?>> value = props.bind().flatMap(ctx::value);
        final Consumer<Object> handler = props.action().map(ctx::handler).orElse(v -> {
        });
        final String format = props.format().orElse(PLAIN);
        final Dropdown dropdown = new Dropdown(options(props), () -> (int) number(value), handler::accept,
                chosen -> Component.translatable(format, chosen).getString());
        return leaf(spec, ctx, dropdown, DROPDOWN_WIDTH, ctx.metrics().controlHeight());
    }

    private static BuiltNode colour(final ElementSpec spec, final ControlElements.ColourProps props,
                                    final BuildContext ctx) {
        final Optional<Supplier<?>> value = ctx.value(props.bind());
        final Consumer<Object> handler = props.action().map(ctx::handler).orElse(v -> {
        });
        final List<Integer> swatches = new ArrayList<>();
        for (final String hex : props.swatches()) {
            ColorMath.parse(hex).ifPresent(swatches::add);
        }
        final ColorPicker picker = new ColorPicker(() -> (int) number(value), handler::accept, swatches);
        return leaf(spec, ctx, picker, COLOUR_WIDTH, ctx.metrics().controlHeight());
    }

    @SuppressWarnings("unchecked")
    private static BuiltNode tree(final ElementSpec spec, final ControlElements.ModelProps props,
                                  final BuildContext ctx) {
        final Object model = ctx.value(props.bind()).map(Supplier::get).orElse(null);
        final Consumer<Object> handler = props.action().map(ctx::handler).orElse(v -> {
        });
        if (!(model instanceof TreeModel tree)) {
            return missing(spec, ctx);
        }
        final TreeView view = new TreeView(tree, id -> ItemStack.EMPTY, handler::accept);
        return leaf(spec, ctx, view, VIEW_WIDTH, VIEW_HEIGHT);
    }

    @SuppressWarnings("unchecked")
    private static BuiltNode table(final ElementSpec spec, final ControlElements.ModelProps props,
                                   final BuildContext ctx) {
        final Object model = ctx.value(props.bind()).map(Supplier::get).orElse(null);
        final Consumer<Object> handler = props.action().map(ctx::handler).orElse(v -> {
        });
        if (!(model instanceof TableModel<?> table)) {
            return missing(spec, ctx);
        }
        final TableView<Object> view = new TableView<>((TableModel<Object>) table, row -> ItemStack.EMPTY,
                handler::accept);
        return leaf(spec, ctx, view, VIEW_WIDTH, VIEW_HEIGHT);
    }

    private static BuiltNode graph(final ElementSpec spec, final ControlElements.ModelProps props,
                                   final BuildContext ctx) {
        final Object model = ctx.value(props.bind()).map(Supplier::get).orElse(null);
        final Consumer<Object> handler = props.action().map(ctx::handler).orElse(v -> {
        });
        if (!(model instanceof GraphModel graph)) {
            return missing(spec, ctx);
        }
        final GraphEditor editor = new GraphEditor(graph, kind -> ctx.skin().seriesColour(Math.floorMod(
                kind.hashCode(), 4)), (x, y) -> handler.accept(new int[] {x, y}));
        return leaf(spec, ctx, editor, VIEW_WIDTH * 2, VIEW_HEIGHT * 2);
    }

    private static BuiltNode model(final ElementSpec spec, final ControlElements.ModelProps props,
                                   final BuildContext ctx) {
        final Optional<Supplier<?>> value = ctx.value(props.bind());
        final ModelView view = new ModelView(() -> subject(value.map(Supplier::get).orElse(null)));
        return leaf(spec, ctx, view, VIEW_WIDTH, VIEW_HEIGHT);
    }

    @SuppressWarnings("unchecked")
    private static BuiltNode chart(final ElementSpec spec, final ControlElements.ChartProps props,
                                   final BuildContext ctx) {
        final Optional<Supplier<?>> value = ctx.value(props.bind());
        final ChartView.Kind kind = switch (props.kind()) {
            case "bar" -> ChartView.Kind.BAR;
            case "pie" -> ChartView.Kind.PIE;
            default -> ChartView.Kind.LINE;
        };
        final String format = props.format().orElse(PLAIN);
        final ChartView view = new ChartView(kind, props.title().map(k -> Component.translatable(k).getString())
                .orElse(""), () -> value.map(Supplier::get).filter(List.class::isInstance)
                .map(list -> (List<ChartView.Series>) list).orElse(List.of()),
                number -> Component.translatable(format, shown(number)).getString(),
                props.from().map(k -> Component.translatable(k).getString()).orElse(""),
                props.to().map(k -> Component.translatable(k).getString()).orElse(""));
        return leaf(spec, ctx, view, VIEW_WIDTH, VIEW_HEIGHT);
    }

    /* A scale as tall as its bar, the room over it, its marks' lines under it and a line of their words. */
    private static BuiltNode scale(final ElementSpec spec, final ControlElements.ScaleProps props,
                                   final BuildContext ctx) {
        final Optional<Supplier<?>> value = ctx.value(props.bind());
        final boolean pointer = props.pointer() == ControlElements.Pointer.ARROW;
        final TextMeasure words = ctx.text(ctx.metrics().smallScale(), false);
        final ScaleView view = new ScaleView(() -> value.<Object>map(Supplier::get)
                .filter(ScaleView.Reading.class::isInstance).map(ScaleView.Reading.class::cast)
                .orElse(ScaleView.Reading.EMPTY), pointer, words.scale());
        final int tall = ScaleView.above(pointer) + ScaleView.BAR + ScaleView.TICK + ScaleView.WORDS_GAP
                + Math.round(words.lineHeight() * words.scale());
        return leaf(spec, ctx, view, VIEW_WIDTH, tall);
    }

    @SuppressWarnings("unchecked")
    private static BuiltNode map(final ElementSpec spec, final ControlElements.MapProps props,
                                 final BuildContext ctx) {
        final Optional<Supplier<?>> centre = ctx.value(props.bind());
        final Optional<Supplier<?>> markers = props.markers().flatMap(ctx::value);
        final MapView view = new MapView(() -> centre.map(Supplier::get).filter(BlockPos.class::isInstance)
                .map(BlockPos.class::cast).orElse(BlockPos.ZERO),
                () -> markers.map(Supplier::get).filter(List.class::isInstance)
                        .map(list -> (List<MapView.Marker>) list).orElse(List.of()),
                GameText.resolve(InterfaceTexts.MAP_NORTH));
        return leaf(spec, ctx, view, VIEW_WIDTH, VIEW_HEIGHT);
    }

    private static BuiltNode tank(final ElementSpec spec, final ControlElements.AmountProps props,
                                  final BuildContext ctx) {
        final Optional<Supplier<?>> value = ctx.value(props.bind());
        final Optional<Supplier<?>> capacity = props.capacityBind().flatMap(ctx::value);
        final String format = props.format().orElse(InterfaceTexts.TANK_AMOUNT.key());
        final TankGauge gauge = new TankGauge(
                () -> value.map(Supplier::get).filter(FluidStack.class::isInstance).map(FluidStack.class::cast)
                        .orElse(FluidStack.EMPTY),
                () -> capacity.isPresent() ? (long) number(capacity) : props.capacity(),
                held -> held.isEmpty() ? List.of() : List.of(held.getHoverName(), Component.translatable(format,
                        held.getAmount(), capacity.isPresent() ? (long) number(capacity) : props.capacity())));
        return leaf(spec, ctx, gauge, GAUGE_WIDTH, GAUGE_HEIGHT);
    }

    private static BuiltNode energy(final ElementSpec spec, final ControlElements.AmountProps props,
                                    final BuildContext ctx) {
        final Optional<Supplier<?>> value = ctx.value(props.bind());
        final Optional<Supplier<?>> capacity = props.capacityBind().flatMap(ctx::value);
        final String format = props.format().orElse(InterfaceTexts.ENERGY_AMOUNT.key());
        final EnergyBar bar = new EnergyBar(() -> {
            final double most = capacity.isPresent() ? number(capacity) : props.capacity();
            return most <= 0 ? 0 : number(value) / most;
        }, () -> List.of(Component.translatable(format, shown(number(value)),
                shown(capacity.isPresent() ? number(capacity) : props.capacity()))));
        return leaf(spec, ctx, bar, GAUGE_WIDTH, GAUGE_HEIGHT);
    }

    private static BuiltNode ghost(final ElementSpec spec, final ControlElements.ToggleProps props,
                                   final BuildContext ctx) {
        final Optional<Supplier<?>> value = props.bind().flatMap(ctx::value);
        final Consumer<Object> handler = props.action().map(ctx::handler).orElse(v -> {
        });
        final GhostSlot slot = new GhostSlot(() -> value.map(Supplier::get).filter(ItemStack.class::isInstance)
                .map(ItemStack.class::cast).orElse(ItemStack.EMPTY), handler::accept);
        return leaf(spec, ctx, slot, SLOT, SLOT);
    }

    /* A component of a size of its own, solid, unless the file gives it one. */
    private static BuiltNode leaf(final ElementSpec spec, final BuildContext ctx, final UiComponent component,
                                  final int width, final int height) {
        return ctx.node(spec, FlexBox.leaf(name(spec), IMeasure.fixed(width, height)).role(Role.SOLID), component,
                null, List.of());
    }

    /* A tree, table or graph bound to nothing it can show says so, as a kind nobody builds does. */
    private static BuiltNode missing(final ElementSpec spec, final BuildContext ctx) {
        return ElementBuilders.missing(spec, ctx);
    }

    private static ModelView.Subject subject(final Object value) {
        return switch (value) {
            case ModelView.Subject subject -> subject;
            case BlockState state -> ModelView.Subject.block(state);
            case ItemStack stack -> new ModelView.Subject.Item(stack);
            case Entity entity -> new ModelView.Subject.Creature(entity);
            case null, default -> ModelView.Subject.NOTHING;
        };
    }

    private static List<Supplier<String>> options(final ControlElements.ChoiceProps props) {
        final List<Supplier<String>> out = new ArrayList<>();
        for (final String key : props.options()) {
            out.add(() -> Component.translatable(key).getString());
        }
        return out;
    }

    private static String name(final ElementSpec spec) {
        return spec.common().id().orElse(spec.type().id().getPath());
    }

    private static double number(final Optional<Supplier<?>> value) {
        final Object now = value.map(Supplier::get).orElse(null);
        return now instanceof Number count ? count.doubleValue() : 0;
    }

    private static boolean truth(final Optional<Supplier<?>> value) {
        final Object now = value.map(Supplier::get).orElse(null);
        return now instanceof Boolean yes ? yes : now instanceof Number count && count.doubleValue() != 0;
    }

    /* A number as a person reads it: whole when it is whole, to two places otherwise, in the game's language. */
    private static String shown(final double value) {
        return value == Math.rint(value) ? String.valueOf((long) value)
                : String.format(GameLocale.locale(), "%.2f", value);
    }
}
