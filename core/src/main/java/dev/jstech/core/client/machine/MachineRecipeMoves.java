/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import dev.jstech.core.machine.MachineFill;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineRecipePayload;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.text.TextKey;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A recipe moved into the open machine from a recipe viewer, JEI's or EMI's alike: whether the machine takes it from
 * the player, and the move itself, which the server makes ({@link MachineFill}) while the screen turns to its Main tab
 * to show the inputs filled.
 */
public final class MachineRecipeMoves {

    private MachineRecipeMoves() {
    }

    /** Why the machine of {@code menu} does not take {@code recipe} from {@code player}; none when it does. */
    public static Optional<TextKey> refusal(final MachineMenu menu, final Player player,
                                            final ProcessingRecipe recipe) {
        if (!(menu.machine() instanceof ProcessingMachineBlockEntity processing)
                || !MachineFill.works(menu.machine(), recipe.kind())) {
            return Optional.of(MachineScreenTexts.MOVE_ELSEWHERE);
        }
        if (recipe.inputs().size() + recipe.catalysts().size() > processing.layout().inputs()) {
            return Optional.of(MachineScreenTexts.MOVE_NO_ROOM);
        }
        return MachineFill.hasASet(player.getInventory(), processing, recipe) ? Optional.empty()
                : Optional.of(MachineScreenTexts.MOVE_MISSING);
    }

    /** Moves the recipe named {@code recipe} into the machine of {@code menu}: one set, or a stack's worth. */
    public static void move(final MachineMenu menu, final ResourceLocation recipe, final boolean all) {
        menu.showTab(MachineMenu.MAIN);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(menu.containerId, MachineMenu.FIRST_TAB + MachineMenu.MAIN);
        }
        PacketDistributor.sendToServer(new MachineRecipePayload(recipe, all));
    }
}
