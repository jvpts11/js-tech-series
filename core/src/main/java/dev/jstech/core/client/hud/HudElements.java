/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hud;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.config.ConfigChoosers;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.gui.HudPlacement;
import dev.jstech.core.gui.HudSettings;
import dev.jstech.core.gui.HudSnap;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

/**
 * Every mod's HUD elements, declared once from the mod's client set-up and drawn by the Core in one layer of the
 * game's HUD, under the chat: in each corner, the elements that show this frame are stacked from the corner inwards in
 * the order they were declared, so the mods share the corners without drawing over one another; in the top right they
 * start under the game's own toasts while there are any. An element the player moved, resized or hid in the HUD
 * editor ({@link HudSettings}) is drawn where and as big as they left it, or not at all, and leaves its corner's stack.
 * The layer hides with the rest of the HUD when the player hides it.
 *
 * <p>An element is named in the editor by the translation key {@code hud.<namespace>.<path>} of its id.
 */
public final class HudElements {

    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "hud_elements");
    /** How tall a slot of the game's toasts is. */
    private static final int TOAST_SLOT = 32;
    /** The smallest box an element has in the editor, so that one empty now can still be taken hold of. */
    private static final int LEAST_BOX_WIDTH = 48;
    private static final int LEAST_BOX_HEIGHT = 16;
    private static final String NAME_PREFIX = "hud";
    private static final List<Declared> DECLARED = new CopyOnWriteArrayList<>();
    private static final Map<ResourceLocation, Declared> BY_ID = new ConcurrentHashMap<>();

    /* While the HUD editor is open, what it holds, which the HUD follows before it is kept. */
    private static volatile @Nullable Map<String, HudPlacement> editing;

    private HudElements() {
    }

    /**
     * Has the HUD draw its elements as {@code placements} say while the editor is open, before Done keeps them; null
     * goes back to what the player's settings say.
     */
    public static void preview(final @Nullable Map<String, HudPlacement> placements) {
        editing = placements == null ? null : Map.copyOf(placements);
    }

    /**
     * Declares {@code element}, known as {@code id}, in {@code corner}.
     *
     * @throws IllegalStateException when an element of that id is declared already
     */
    public static void declare(final ResourceLocation id, final HudStack.Corner corner, final IHudElement element) {
        declare(id, () -> corner, element);
    }

    /**
     * Declares {@code element}, known as {@code id}, in the corner {@code corner} says, asked every frame: an element
     * whose corner the player chooses.
     *
     * @throws IllegalStateException when an element of that id is declared already
     */
    public static void declare(final ResourceLocation id, final Supplier<HudStack.Corner> corner,
                               final IHudElement element) {
        final Declared declared = new Declared(id, corner, element);
        if (BY_ID.putIfAbsent(id, declared) != null) {
            throw DeclarationError.of(id.getNamespace(), "the HUD element " + id, "is declared twice",
                    "declare it once, from the mod's client set-up");
        }
        DECLARED.add(declared);
    }

    /** Where each element of {@code corner} that shows now is drawn, by its id; for a test to look at. */
    public static Map<ResourceLocation, HudStack.Point> placedIn(final HudStack.Corner corner) {
        final Minecraft minecraft = Minecraft.getInstance();
        final List<Declared> shown = shownIn(minecraft, corner);
        final List<HudStack.Point> points = HudStack.place(corner, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), sizes(minecraft, shown), reserved(minecraft, corner));
        final Map<ResourceLocation, HudStack.Point> out = new LinkedHashMap<>();
        for (int i = 0; i < shown.size(); i++) {
            out.put(shown.get(i).id(), points.get(i));
        }
        return out;
    }

    /** Every element declared, in the order it was, as the HUD editor shows them. */
    public static List<Editable> editable() {
        final List<Editable> out = new ArrayList<>();
        for (final Declared declared : DECLARED) {
            out.add(new Editable(declared.id(), Component.translatable(declared.id().toLanguageKey(NAME_PREFIX)),
                    declared.element()));
        }
        return out;
    }

    /** How big {@code element}'s box is in the editor at its own size, never smaller than one can take hold of. */
    public static HudStack.Size boxSize(final Minecraft minecraft, final IHudElement element) {
        final HudStack.Size size = element.editSize(minecraft);
        return new HudStack.Size(Math.max(LEAST_BOX_WIDTH, size.width()), Math.max(LEAST_BOX_HEIGHT, size.height()));
    }

    /**
     * Where the editor shows each element of {@code ids}, the ones the player has not placed, on a screen
     * {@code width} by {@code height}: stacked in its corner as the HUD stacks it, whether it shows now or not.
     */
    public static Map<ResourceLocation, HudSnap.Box> declaredBoxes(final Minecraft minecraft, final int width,
                                                                   final int height,
                                                                   final Set<ResourceLocation> ids) {
        final Map<ResourceLocation, HudSnap.Box> out = new LinkedHashMap<>();
        for (final HudStack.Corner corner : List.of(HudStack.Corner.TOP_LEFT, HudStack.Corner.TOP_RIGHT,
                HudStack.Corner.BOTTOM_LEFT, HudStack.Corner.BOTTOM_RIGHT)) {
            final List<Declared> here = new ArrayList<>();
            for (final Declared declared : DECLARED) {
                if (declared.corner().get() == corner && ids.contains(declared.id())) {
                    here.add(declared);
                }
            }
            final List<HudStack.Size> sizes = here.stream().map(d -> boxSize(minecraft, d.element())).toList();
            final List<HudStack.Point> points = HudStack.place(corner, width, height, sizes);
            for (int i = 0; i < here.size(); i++) {
                out.put(here.get(i).id(), new HudSnap.Box(points.get(i).x(), points.get(i).y(),
                        sizes.get(i).width(), sizes.get(i).height()));
            }
        }
        return out;
    }

    /**
     * Hands the mod's bus what this answers, and the settings screen and the editor's key the HUD editor, once, from
     * the mod's constructor on a player's game.
     */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterGuiLayersEvent.class, HudElements::onRegisterLayers);
        ConfigChoosers.register(HudSettings.EDITOR, new HudEditorChooser());
        KeyActionsClient.onPress(CoreKeys.EDIT_HUD, () -> {
            final Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new HudEditorScreen(minecraft.screen, new ConfigDraft(HudSettings.FILE),
                    HudSettings.PLACED, true));
        });
    }

    public static void onRegisterLayers(final RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CHAT, LAYER, HudElements::render);
    }

    private static void render(final GuiGraphics graphics, final DeltaTracker delta) {
        if (DECLARED.isEmpty()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        for (final HudStack.Corner corner : List.of(HudStack.Corner.TOP_LEFT, HudStack.Corner.TOP_RIGHT,
                HudStack.Corner.BOTTOM_LEFT, HudStack.Corner.BOTTOM_RIGHT)) {
            final List<Declared> shown = shownIn(minecraft, corner);
            if (shown.isEmpty()) {
                continue;
            }
            final List<HudStack.Point> points = HudStack.place(corner, graphics.guiWidth(), graphics.guiHeight(),
                    sizes(minecraft, shown), reserved(minecraft, corner));
            for (int i = 0; i < shown.size(); i++) {
                shown.get(i).element().render(graphics, delta, points.get(i).x(), points.get(i).y());
            }
        }
        for (final Declared declared : DECLARED) {
            final Optional<HudPlacement> placed = placementOf(declared.id());
            if (placed.isEmpty() || placed.get().hidden() || !declared.element().shown(minecraft)) {
                continue;
            }
            drawPlaced(graphics, delta, minecraft, declared.element(), placed.get());
        }
    }

    /* An element the player placed: at its place, as big as they made it. */
    private static void drawPlaced(final GuiGraphics graphics, final DeltaTracker delta, final Minecraft minecraft,
                                   final IHudElement element, final HudPlacement placed) {
        final HudStack.Size size = placed.scaled(element.size(minecraft));
        final HudStack.Point at = placed.topLeft(graphics.guiWidth(), graphics.guiHeight(), size);
        // Scaled about its own top left and drawn there, so an element that keeps where it drew (for a click) keeps
        // the true place at its own size.
        graphics.pose().pushPose();
        graphics.pose().translate(at.x(), at.y(), 0);
        graphics.pose().scale(placed.scale(), placed.scale(), 1);
        graphics.pose().translate(-at.x(), -at.y(), 0);
        element.render(graphics, delta, at.x(), at.y());
        graphics.pose().popPose();
    }

    /* The elements of {@code corner} that show now and that the player did not place, which the corner stacks. */
    private static List<Declared> shownIn(final Minecraft minecraft, final HudStack.Corner corner) {
        final List<Declared> shown = new ArrayList<>();
        for (final Declared declared : DECLARED) {
            if (declared.corner().get() == corner && placementOf(declared.id()).isEmpty()
                    && declared.element().shown(minecraft)) {
                shown.add(declared);
            }
        }
        return shown;
    }

    /* Where the player put the element: as the open editor holds it, or as their settings keep it. */
    private static Optional<HudPlacement> placementOf(final ResourceLocation id) {
        final Map<String, HudPlacement> open = editing;
        return open != null ? Optional.ofNullable(open.get(id.toString())) : HudSettings.placement(id.toString());
    }

    /* The top right corner's slots the game's own toasts hold now, each a toast's height; nothing elsewhere. */
    private static int reserved(final Minecraft minecraft, final HudStack.Corner corner) {
        if (corner != HudStack.Corner.TOP_RIGHT || minecraft.options.hideGui) {
            return 0;
        }
        return minecraft.getToasts().occupiedSlots.length() * TOAST_SLOT;
    }

    private static List<HudStack.Size> sizes(final Minecraft minecraft, final List<Declared> shown) {
        return shown.stream().map(declared -> declared.element().size(minecraft)).toList();
    }

    /**
     * An element as the HUD editor shows it.
     *
     * @param id      its id
     * @param name    what it is called
     * @param element the element
     */
    public record Editable(ResourceLocation id, Component name, IHudElement element) {
    }

    private record Declared(ResourceLocation id, Supplier<HudStack.Corner> corner, IHudElement element) {
    }
}
