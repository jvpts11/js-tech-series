/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import dev.jstech.core.gui.tooltip.TooltipBar;
import dev.jstech.core.gui.tooltip.TooltipSettings;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * What the mods built on the Core say in items' tooltips, kept together mod by mod: each mod hands the Core its
 * {@link ITooltipSection} once, and every tooltip shows, after the game's own lines, the lines of the item's own mod
 * first and the other mods' after it by their names, a little room between one mod and the next. The parts that wait
 * for a key are shown when it is held, and while none is the tooltip ends with a hint saying which key shows what;
 * last of all comes the name of the mod the item is from, in the game's blue, once, unless another mod already put
 * it there. The pieces that are not text (bars, pictures, 3D previews, what an item holds) stand as a line of their
 * words wherever only a tooltip's text is read, as a search does, and are drawn as themselves on the screen.
 *
 * <pre>{@code
 * ItemTooltips.add(MODID, (stack, tooltip) -> {
 *     if (stack.is(DRILL)) {
 *         tooltip.line(DrillTexts.AREA.text());
 *         tooltip.shift().line(DrillTexts.WHAT_IT_DOES.text());
 *     }
 * });
 * }</pre>
 */
public final class ItemTooltips {

    /* Each mod's sections, by mod id. */
    private static final Map<String, List<ITooltipSection>> SECTIONS = new LinkedHashMap<>();
    /* The pieces of the tooltips written lately, by the mark their stand-in lines bear, until they are drawn. */
    private static final Map<String, TooltipComponent> WAITING = Collections.synchronizedMap(new Waiting());
    /*
     * How many pieces wait at most, the oldest let go first: a tooltip shown is written anew each frame, and the
     * tooltips read but never drawn, as a search reads them, leave theirs.
     */
    private static final int MOST_WAITING = 256;
    private static final String MARK = "jscore:tooltip_piece/";

    private static final AtomicLong MARKS = new AtomicLong();
    /* The mods' sections as one list each, in a copy that changes only when a mod adds one. */
    private static volatile Map<String, List<ITooltipSection>> sections = Map.of();

    private ItemTooltips() {
    }

    /** Hands the Core what {@code modid} says in items' tooltips; called once a section, from the mod's client. */
    public static synchronized void add(final String modid, final ITooltipSection section) {
        Objects.requireNonNull(section, "section");
        SECTIONS.computeIfAbsent(modid, id -> new ArrayList<>()).add(section);
        final Map<String, List<ITooltipSection>> copy = new LinkedHashMap<>();
        SECTIONS.forEach((id, list) -> copy.put(id, List.copyOf(list)));
        sections = Map.copyOf(copy);
    }

    /** Listens for the game's tooltips, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, ItemTooltipEvent.class, ItemTooltips::onTooltip);
        NeoForge.EVENT_BUS.addListener(RenderTooltipEvent.GatherComponents.class, ItemTooltips::onGather);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RenderTooltipEvent.GatherComponents.class,
                ItemTooltips::onGatherLast);
    }

    /**
     * What the mods' sections add to the tooltip of {@code stack}, with Shift and Ctrl held or not: the lines, each
     * piece as a line of its words that a tooltip drawn on the screen shows as the piece, the room between mods as an
     * empty line that is drawn as that room, and the hint.
     */
    public static List<Component> lines(final ItemStack stack, final Item.TooltipContext context,
                                        final @Nullable Player player, final TooltipFlag flags, final boolean shift,
                                        final boolean ctrl) {
        final List<Component> out = new ArrayList<>();
        for (final Either<Component, TooltipComponent> part : parts(stack, context, player, flags, shift, ctrl)) {
            out.add(part.map(line -> line, ItemTooltips::standIn));
        }
        return out;
    }

    /** The name a mod is shown by, its id when no mod by that id is loaded. */
    public static String modName(final String modid) {
        return ModList.get().getModContainerById(modid).map(mod -> mod.getModInfo().getDisplayName()).orElse(modid);
    }

    private static void onTooltip(final ItemTooltipEvent event) {
        // Keys are read from the window, which only the render thread may ask; a search reading tooltips elsewhere
        // sees them as not held.
        final boolean onScreen = RenderSystem.isOnRenderThread();
        event.getToolTip().addAll(lines(event.getItemStack(), event.getContext(), event.getEntity(),
                event.getFlags(), onScreen && Screen.hasShiftDown(), onScreen && Screen.hasControlDown()));
    }

    /* Each stand-in line put in its piece's place. */
    private static void onGather(final RenderTooltipEvent.GatherComponents event) {
        final List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        for (int i = 0; i < elements.size(); i++) {
            final TooltipComponent piece = elements.get(i).left().map(ItemTooltips::pieceOf).orElse(null);
            if (piece != null) {
                elements.set(i, Either.right(piece));
            }
        }
    }

    /* Last of all, the name of the item's mod, unless it is there already. */
    private static void onGatherLast(final RenderTooltipEvent.GatherComponents event) {
        if (!TooltipSettings.modName() || event.getItemStack().isEmpty()) {
            return;
        }
        final String name = modName(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).getNamespace());
        final List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        for (final Either<FormattedText, TooltipComponent> element : elements) {
            if (element.left().map(text -> text.getString().equals(name)).orElse(false)) {
                return;
            }
        }
        elements.add(Either.left(Component.literal(name).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC)));
    }

    /* What every mod's sections say, in the order the tooltip shows it. */
    private static List<Either<Component, TooltipComponent>> parts(final ItemStack stack,
                                                                    final Item.TooltipContext context,
                                                                    final @Nullable Player player,
                                                                    final TooltipFlag flags, final boolean shift,
                                                                    final boolean ctrl) {
        final List<Either<Component, TooltipComponent>> out = new ArrayList<>();
        if (stack.isEmpty()) {
            return out;
        }
        boolean waitsShift = false;
        boolean waitsCtrl = false;
        for (final String modid : order(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
            final ItemTooltip tooltip = new ItemTooltip(stack, context, player, flags);
            for (final ITooltipSection section : sections.get(modid)) {
                section.describe(stack, tooltip);
            }
            if (tooltip.isEmpty()) {
                continue;
            }
            final List<Either<Component, TooltipComponent>> shown = new ArrayList<>(tooltip.always().parts());
            if (shift) {
                shown.addAll(tooltip.shift().parts());
            }
            if (ctrl) {
                shown.addAll(tooltip.ctrl().parts());
            }
            waitsShift |= !tooltip.shift().isEmpty();
            waitsCtrl |= !tooltip.ctrl().isEmpty();
            if (shown.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.add(Either.right(TooltipGap.GAP));
            }
            out.addAll(shown);
        }
        if (!shift && !ctrl && (waitsShift || waitsCtrl)) {
            final TextKey hint = waitsShift && waitsCtrl ? TooltipTexts.MORE_BOTH
                    : waitsShift ? TooltipTexts.MORE : TooltipTexts.MORE_NUMBERS;
            out.add(Either.left(GameText.component(hint).copy().withStyle(ChatFormatting.DARK_GRAY)));
        }
        return out;
    }

    /* The mods with sections: the item's own first, then the others by the names they are shown by. */
    private static List<String> order(final String own) {
        final List<String> mods = new ArrayList<>(sections.keySet());
        mods.sort(Comparator.comparing((String id) -> !id.equals(own)).thenComparing(ItemTooltips::modName,
                String.CASE_INSENSITIVE_ORDER));
        return mods;
    }

    /* A line of a piece's words, bearing the mark the piece waits under until the tooltip is drawn. */
    private static Component standIn(final TooltipComponent piece) {
        final String mark = MARK + MARKS.incrementAndGet();
        WAITING.put(mark, piece);
        return words(piece).copy().withStyle(style -> style.withInsertion(mark));
    }

    private static Component words(final TooltipComponent piece) {
        return piece instanceof TooltipBar bar ? bar.label() : Component.empty();
    }

    private static @Nullable TooltipComponent pieceOf(final FormattedText text) {
        if (!(text instanceof Component line)) {
            return null;
        }
        // Kept after it is found: a screen may draw the same lines frame after frame.
        final String mark = line.getStyle().getInsertion();
        return mark != null && mark.startsWith(MARK) ? WAITING.get(mark) : null;
    }

    /* The pieces waiting, the oldest let go once there are too many. */
    private static final class Waiting extends LinkedHashMap<String, TooltipComponent> {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, TooltipComponent> eldest) {
            return size() > MOST_WAITING;
        }
    }
}
