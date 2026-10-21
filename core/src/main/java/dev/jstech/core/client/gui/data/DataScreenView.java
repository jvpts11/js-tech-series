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
import dev.jstech.core.client.gui.component.KeyboardFocus;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.gui.flex.FlexBox;
import dev.jstech.core.gui.flex.FlexLayout;
import dev.jstech.core.gui.flex.Insets;
import dev.jstech.core.gui.flex.Placed;
import dev.jstech.core.gui.flex.Role;
import dev.jstech.core.gui.flex.TextMeasure;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.gui.screen.CoreElements;
import dev.jstech.core.gui.screen.InterfaceTexts;
import dev.jstech.core.gui.screen.ScreenFile;
import dev.jstech.core.text.GameText;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.log.CoreLogs;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * A screen's file built, laid out and drawn: the part every screen declared in a file shares, with a menu or without.
 * It finds the style the screen wears (the player's choice for its mod, else the file's, else the mod's default, else
 * the game's), builds the components the file declares through their kinds' builders, lays them out at the file's width
 * and as tall as they need, moves the menu's slots under their frames, and routes the pointer and the keys to them.
 */
public final class DataScreenView {

    private final ResourceLocation file;
    private final ScreenWiring<?> wiring;
    @Nullable
    private final AbstractContainerMenu menu;
    private final Supplier<Component> title;
    private final Panel root = new Panel();
    private final Map<UiComponent, BuiltNode> nodes = new IdentityHashMap<>();
    @Nullable
    private BuiltNode tree;
    @Nullable
    private Placed placed;
    private Style style;
    private StyleSkin skin;
    private Font font;
    private int width;
    private int height;
    private Set<String> unwired = Set.of();
    private Optional<String> problem = Optional.empty();
    private boolean warnedTall;

    private static final Logger LOGGER = CoreLogs.INTERFACE.logger();

    DataScreenView(final ResourceLocation file, final ScreenWiring<?> wiring,
                   @Nullable final AbstractContainerMenu menu, final Supplier<Component> title) {
        this.file = Objects.requireNonNull(file, "file");
        this.wiring = Objects.requireNonNull(wiring, "wiring");
        this.menu = menu;
        this.title = Objects.requireNonNull(title, "title");
    }

    /** Builds the screen from its file as it is now, in the style it wears now. */
    public void build(final Font gameFont) {
        font = gameFont;
        root.dispose();
        root.clear();
        nodes.clear();
        final Optional<ScreenFile> read = ScreenFiles.get(file);
        style = styleOf(read.orElse(null));
        skin = Styles.skin(style);
        if (read.isEmpty()) {
            problem = Optional.of(ScreenFiles.problem(file).orElseGet(() -> GameText.resolve(
                    InterfaceTexts.MISSING_SCREEN.with(file.toString()))));
            tree = missing(problem.get());
            width = ScreenFile.GAME_WIDTH;
        } else {
            problem = Optional.empty();
            final BuildContext context = new BuildContext(skin, font, wiring, menu, () -> titleOf(read.get()));
            final BuiltNode built = ElementBuilders.build(read.get().root(), context);
            // The window keeps the style's room inside its edge unless the file says how much.
            if (read.get().root().layout().padding().isEmpty()) {
                built.box().padding(Insets.of(style.metrics().padding()));
            }
            tree = built;
            width = read.get().width();
            unwired = context.unwired();
        }
        tree.forEach(node -> {
            if (node.component() != null) {
                root.add(node.component());
                nodes.put(node.component(), node);
            }
        });
        height = FlexLayout.preferred(tree.box(), width).height();
    }

    /** Lays the built screen out with its top-left corner at {@code (left, top)} on the game's screen. */
    public void layout(final int left, final int top, final int screenHeight) {
        if (tree == null) {
            return;
        }
        placed = FlexLayout.layout(tree.box(), width, height);
        if (height > screenHeight && !warnedTall) {
            warnedTall = true;
            LOGGER.warn("the screen {} needs {} pixels down and the window has {}", file, height, screenHeight);
        }
        place(tree, placed, left, top);
    }

    /** Draws the screen's window, then what it holds. */
    public void render(final GuiGraphics g, final int left, final int top, final int mouseX, final int mouseY,
                       final float partialTick) {
        if (tree == null) {
            return;
        }
        skin.window(g, left, top, width, height);
        tree.forEach(node -> {
            if (node.component() != null) {
                node.component().setVisible(node.visible().getAsBoolean());
                node.component().setEnabled(node.enabled().getAsBoolean());
            }
        });
        root.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
    }

    /** What the pointer resting at {@code (x, y)} shows: the tooltip of the component under it, or nothing. */
    public List<Component> tooltipAt(final double x, final double y) {
        final List<UiComponent> children = root.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            final UiComponent component = children.get(i);
            final BuiltNode node = nodes.get(component);
            if (node != null && component.visible() && component.contains(x, y)) {
                if (node.tooltip().isPresent()) {
                    return List.of(Component.translatable(node.tooltip().get()));
                }
                final List<Component> own = component.tooltip(x, y);
                if (!own.isEmpty()) {
                    return own;
                }
            }
        }
        return List.of();
    }

    public boolean mouseClicked(final double x, final double y, final int button) {
        final boolean taken = root.mouseClicked(x, y, button);
        if (taken && pressedPlaysClick(x, y)) {
            click();
        }
        return taken;
    }

    public boolean mouseReleased(final double x, final double y, final int button) {
        return root.mouseReleased(x, y, button);
    }

    public boolean mouseDragged(final double x, final double y, final int button) {
        return root.mouseDragged(x, y, button);
    }

    public boolean mouseScrolled(final double x, final double y, final double delta) {
        return root.mouseScrolled(x, y, delta);
    }

    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return root.keyPressed(key, scanCode, modifiers);
    }

    public boolean charTyped(final char c) {
        return root.charTyped(c);
    }

    /** Lets go of what the components hold outside themselves, when the screen goes away. */
    public void dispose() {
        root.dispose();
    }

    /**
     * Whether a component of the screen that takes typing has the keyboard (a field), so the keys go to it rather
     * than to the game; a switch or a button with it lets Tab and the arrows move the keyboard on.
     */
    public boolean typing() {
        return root.focusedChild() != null && root.focusedChild().takesTyping();
    }

    /** Whether a component has something open over the others (a drop-down's list), which Escape closes first. */
    public boolean overlayOpen() {
        return root.overlayOpen();
    }

    /** The components the keyboard reaches, as the screen hands them to the game; made once they are laid out. */
    public KeyboardFocus keyboard() {
        return KeyboardFocus.of(root);
    }

    /** How wide the screen is. */
    public int width() {
        return width;
    }

    /** How tall the screen is, as its components need. */
    public int height() {
        return height;
    }

    /** The style the screen wears. */
    public Style style() {
        return style;
    }

    /** The node the file gave {@code id}, or none. */
    public Optional<BuiltNode> find(final String id) {
        if (tree == null) {
            return Optional.empty();
        }
        final List<BuiltNode> found = new ArrayList<>();
        tree.forEach(node -> {
            if (node.id().filter(id::equals).isPresent()) {
                found.add(node);
            }
        });
        return found.stream().findFirst();
    }

    /** The laid-out screen as the overlap check reads it, in the screen's own space. */
    public GuiLayout guiLayout() {
        return placed == null ? new GuiLayout(width, height) : placed.guiLayout(width, height);
    }

    /** The laid-out boxes, in the screen's own space; none before the first layout. */
    public Optional<Placed> placed() {
        return Optional.ofNullable(placed);
    }

    /** The names the file used that the screen's code did not hand over. */
    public Set<String> unwired() {
        return unwired;
    }

    /** What is wrong with the file, when it could not be built from. */
    public Optional<String> problem() {
        return problem;
    }

    /* The style: the player's for the screen's mod, else the file's, else the mod's default, else the game's. */
    private Style styleOf(@Nullable final ScreenFile read) {
        final String owner = file.getNamespace();
        final Optional<Style> chosen = Styles.chosenFor(owner);
        if (chosen.isPresent()) {
            return chosen.get();
        }
        if (read != null && read.style().isPresent()) {
            final Optional<Style> own = Styles.get(read.style().get());
            if (own.isPresent()) {
                return own.get();
            }
        }
        return Styles.forMod(owner);
    }

    private Component titleOf(final ScreenFile read) {
        return read.title().<Component>map(Component::translatable).orElseGet(title);
    }

    /* A screen whose file is missing or broken says so where it would have been. */
    private BuiltNode missing(final String said) {
        final TextMeasure measure = new TextMeasure(text -> skin.textWidth(font, text), font.lineHeight, 1f, true);
        final FlexBox box = FlexBox.leaf("problem", measure.of(said)).role(Role.TEXT);
        final TextView text = new TextView(() -> said, skin::text, CoreElements.TextAlign.LEFT, measure);
        final FlexBox column = FlexBox.column("screen").padding(Insets.of(style.metrics().padding())).add(box);
        return new BuiltNode(Optional.empty(), column, null, null, () -> true, () -> true, Optional.empty(),
                List.of(new BuiltNode(Optional.empty(), box, text, null, () -> true, () -> true, Optional.empty(),
                        List.of())));
    }

    /* Hands each node its box on the game's screen, and its box in the screen's own space to what it does placed. */
    private static void place(final BuiltNode node, final Placed at, final int left, final int top) {
        if (node.component() != null) {
            node.component().setBounds(left + at.x(), top + at.y(), at.width(), at.height());
        }
        if (node.placed() != null) {
            node.placed().accept(at);
        }
        final List<Placed> placedChildren = at.children();
        for (int i = 0; i < node.children().size() && i < placedChildren.size(); i++) {
            place(node.children().get(i), placedChildren.get(i), left, top);
        }
    }

    /* Whether the press landed on a control that sounds, a button or a check box. */
    private boolean pressedPlaysClick(final double x, final double y) {
        for (final UiComponent child : root.children()) {
            if ((child instanceof Button || child instanceof Checkbox) && child.visible() && child.enabled()
                    && child.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    /* The style's sound of a press. */
    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvent.createVariableRangeEvent(style.clickSound()), 1.0F));
    }
}
