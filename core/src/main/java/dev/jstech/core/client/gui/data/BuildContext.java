/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.gui.flex.FlexBox;
import dev.jstech.core.gui.flex.Placed;
import dev.jstech.core.gui.flex.TextMeasure;
import dev.jstech.core.gui.screen.ElementSpec;
import dev.jstech.core.gui.style.StyleMetrics;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * What a component's builder has to build with: the style the screen wears and its font, the screen's values and
 * actions by name, its menu, its title, and the builders of what a component holds. The names a file uses that nobody
 * handed over are noted, for the developer's inspector to show.
 */
public final class BuildContext {

    private final StyleSkin skin;
    private final Font font;
    private final ScreenWiring<?> wiring;
    @Nullable
    private final AbstractContainerMenu menu;
    private final Supplier<Component> title;
    private final Set<String> unwired = new LinkedHashSet<>();

    BuildContext(final StyleSkin skin, final Font font, final ScreenWiring<?> wiring,
                 @Nullable final AbstractContainerMenu menu, final Supplier<Component> title) {
        this.skin = skin;
        this.font = font;
        this.wiring = wiring;
        this.menu = menu;
        this.title = title;
    }

    /** The style the screen wears. */
    public StyleSkin skin() {
        return skin;
    }

    /** The font its text is measured and drawn with. */
    public Font font() {
        return font;
    }

    /** The style's spacing. */
    public StyleMetrics metrics() {
        return skin.style().metrics();
    }

    /** How a text is measured in the style's font, at {@code scale}, wrapped or not. */
    public TextMeasure text(final float scale, final boolean wraps) {
        return new TextMeasure(text -> skin.textWidth(font, text), font.lineHeight, scale, wraps);
    }

    /** The screen's menu, or none. */
    public Optional<AbstractContainerMenu> menu() {
        return Optional.ofNullable(menu);
    }

    /** The screen's title. */
    public Component title() {
        return title.get();
    }

    /** The value {@code name}: the screen's code's, else its menu's; noted when there is none. */
    public Optional<Supplier<?>> value(final String name) {
        final Optional<Supplier<?>> found = wiring.value(name);
        if (found.isEmpty()) {
            unwired.add("value " + name);
        }
        return found;
    }

    /** The action {@code name}; noted when nobody handed it over. */
    public Optional<Runnable> action(final String name) {
        final Optional<Runnable> found = wiring.action(name);
        if (found.isEmpty()) {
            unwired.add("action " + name);
        }
        return found;
    }

    /** What is done with the text of {@code name}; noted when nobody handed it over. */
    public Optional<Consumer<String>> textAction(final String name) {
        final Optional<Consumer<String>> found = wiring.text(name);
        if (found.isEmpty()) {
            unwired.add("text " + name);
        }
        return found;
    }

    /** What is done with a value a component hands over under {@code name}; noted when nobody handed it over. */
    public Consumer<Object> handler(final String name) {
        final Optional<Consumer<Object>> found = wiring.handler(name);
        if (found.isEmpty()) {
            unwired.add("handler " + name);
        }
        return found.orElse(value -> {
        });
    }

    /** Presses the menu's button {@code id} on the server, as the game's own screens do. */
    public void pressMenuButton(final int id) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (menu != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    /** What {@code spec} holds, each built by its kind's builder. */
    public List<BuiltNode> children(final ElementSpec spec) {
        final List<BuiltNode> out = new ArrayList<>(spec.children().size());
        for (final ElementSpec child : spec.children()) {
            out.add(ElementBuilders.build(child, this));
        }
        return out;
    }

    /**
     * The node of {@code spec}: its box with what every component says of its size and place set on it, whether it
     * shows and can be used from its file's names, and its tooltip.
     */
    public BuiltNode node(final ElementSpec spec, final FlexBox box, @Nullable final UiComponent component,
                          @Nullable final Consumer<Placed> placed, final List<BuiltNode> children) {
        spec.common().applyTo(box);
        final BooleanSupplier visible = spec.common().visibleIf().map(this::truth).orElse(() -> true);
        final BooleanSupplier enabled = spec.common().enabledIf().map(this::truth).orElse(() -> true);
        return new BuiltNode(spec.common().id(), box, component, placed, visible, enabled, spec.common().tooltip(),
                children);
    }

    /** The names the file used that nobody handed over. */
    public Set<String> unwired() {
        return Set.copyOf(unwired);
    }

    /* A value read as yes or no: true, a number other than zero, or anything else that is there. */
    private BooleanSupplier truth(final String name) {
        final Optional<Supplier<?>> value = value(name);
        if (value.isEmpty()) {
            return () -> true;
        }
        final Supplier<?> reader = value.get();
        return () -> {
            final Object now = reader.get();
            if (now instanceof Boolean yes) {
                return yes;
            }
            if (now instanceof Number number) {
                return number.doubleValue() != 0;
            }
            return now != null;
        };
    }
}
