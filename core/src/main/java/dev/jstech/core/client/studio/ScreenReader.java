/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.data.DataScreens;
import dev.jstech.core.client.gui.data.ScreenFiles;
import dev.jstech.core.gui.screen.CoreElements;
import dev.jstech.core.gui.screen.ElementSpec;
import dev.jstech.core.gui.screen.ScreenFile;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The screen: the screen file named as the thing, {@code assets/<mod>/jstech/screens/<thing>.json}, as the new screen
 * writes it. Wrong when the file does not load. A thing with no screen file has no row.
 *
 * <p>Its slots are checked against the menu the file's screen was registered for, which neither file can see on its
 * own: the menu is made on the player's game, from the thing's block entity as it opens on the server, or with nothing
 * more sent for a menu of no block, its slots counted, and the file's {@code slot}, {@code slots} and
 * {@code inventory} read against them ({@link SlotCoverage}). A menu that cannot be made so is not checked.
 */
final class ScreenReader implements IStudioReader {

    private static final ItemStack ICON = new ItemStack(Items.ITEM_FRAME);

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(thing.id().getNamespace(),
                ScreenFile.FOLDER + "/" + thing.id().getPath() + ".json");
        final Optional<String> problem = ScreenFiles.problem(thing.id());
        final Optional<ScreenFile> loaded = ScreenFiles.get(thing.id());
        if (loaded.isEmpty() && problem.isEmpty()) {
            return;
        }
        final StudioSheet.File source = StudioFiles.asset(file);
        sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.SCREEN), file.getPath(),
                problem.isPresent() ? StudioSheet.Mark.ERROR : StudioSheet.Mark.FINE, Optional.of(source)));
        problem.ifPresent(wrong -> sheet.check(new StudioSheet.Check(StudioSheet.Mark.ERROR,
                GameText.resolve(StudioTexts.SCREEN_BROKEN), wrong, Optional.of(source))));
        final Optional<MenuType<?>> type = DataScreens.menuOf(thing.id());
        if (loaded.isPresent() && type.isPresent()) {
            final AbstractContainerMenu menu = made(type.get(), thing);
            if (menu != null) {
                checkSlots(loaded.get(), menu, source, sheet);
            }
        }
    }

    /* Each thing wrong with what the screen shows of the menu's slots, as a check of the sheet. */
    private static void checkSlots(final ScreenFile screen, final AbstractContainerMenu menu,
                                   final StudioSheet.File source, final StudioSheet sheet) {
        final Set<Integer> playerSlots = new HashSet<>();
        for (int i = 0; i < menu.slots.size(); i++) {
            final Slot slot = menu.slots.get(i);
            if (slot.container instanceof Inventory) {
                playerSlots.add(i);
            }
        }
        final List<Integer> shown = new ArrayList<>();
        boolean inventory = false;
        for (final ElementSpec spec : screen.root().all()) {
            if (spec.type() == CoreElements.SLOT) {
                shown.add(spec.props(CoreElements.SLOT).slot());
            } else if (spec.type() == CoreElements.SLOTS) {
                final CoreElements.SlotsProps run = spec.props(CoreElements.SLOTS);
                for (int i = 0; i < run.count(); i++) {
                    shown.add(run.from() + i);
                }
            } else if (spec.type() == CoreElements.INVENTORY) {
                inventory = true;
            }
        }
        for (final SlotCoverage.Finding found : SlotCoverage.check(menu.slots.size(), playerSlots, shown,
                inventory)) {
            final String slots = found.slots().stream().map(String::valueOf).collect(Collectors.joining(", "));
            final TextKey title = switch (found.kind()) {
                case BEYOND_THE_MENU -> StudioTexts.SLOTS_BEYOND;
                case SHOWN_NOWHERE -> StudioTexts.SLOTS_NOWHERE;
                case SHOWN_TWICE -> StudioTexts.SLOTS_TWICE;
                case NO_INVENTORY -> StudioTexts.SLOTS_NO_INVENTORY;
            };
            final String detail = switch (found.kind()) {
                case BEYOND_THE_MENU -> GameText.resolve(StudioTexts.SLOTS_BEYOND_DETAIL.with(slots,
                        menu.slots.size()));
                case SHOWN_NOWHERE -> GameText.resolve(StudioTexts.SLOTS_NOWHERE_DETAIL.with(slots));
                case SHOWN_TWICE -> GameText.resolve(StudioTexts.SLOTS_TWICE_DETAIL.with(slots));
                case NO_INVENTORY -> GameText.resolve(StudioTexts.SLOTS_NO_INVENTORY_DETAIL.with(
                        found.slots().size()));
            };
            sheet.check(new StudioSheet.Check(StudioSheet.Mark.WARNING, GameText.resolve(title), detail,
                    Optional.of(source)));
        }
    }

    /*
     * The thing's menu, made on the player's game to be counted and let go of at once: from its block entity, as it
     * opens on the server, or for a menu of no block with nothing more sent; null when it cannot be made so.
     */
    private static @Nullable AbstractContainerMenu made(final MenuType<?> type, final StudioThing thing) {
        final Minecraft game = Minecraft.getInstance();
        final LocalPlayer player = game.player;
        if (player == null || game.level == null) {
            return null;
        }
        try {
            AbstractContainerMenu menu = null;
            if (thing.block() instanceof EntityBlock holder) {
                final BlockEntity entity = holder.newBlockEntity(BlockPos.ZERO, thing.block().defaultBlockState());
                if (entity instanceof MenuProvider provider) {
                    menu = provider.createMenu(0, player.getInventory(), player);
                }
            }
            if (menu == null) {
                menu = type.create(0, player.getInventory(), new RegistryFriendlyByteBuf(Unpooled.buffer(),
                        game.level.registryAccess()));
            }
            if (menu != null && menu.getType() != type) {
                return null;
            }
            if (menu != null) {
                menu.removed(player);
            }
            return menu;
        } catch (final RuntimeException cannot) {
            // A menu that needs more than the studio has, a place in a world or what the server sends, is not checked.
            return null;
        }
    }
}
