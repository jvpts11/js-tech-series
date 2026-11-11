/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.block.MiningComputerBlock;
import dev.jstech.computers.block.MiningComputerPartBlock;
import dev.jstech.computers.block.MiningComputerStructure;
import dev.jstech.computers.block.MiningComputerTexts;
import dev.jstech.computers.blockentity.MiningComputerBlockEntity;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * A look at a Mining Computer's risers (computers/docs/design/teracoin.md): with a graphics card in hand at a free
 * riser, or with an empty hand at a seated card, the riser's place is outlined, as the parts standing out of a device
 * are, and the bar over the hotbar says what a click there does. The risers stand inside the rig's blocks, where the
 * game's own aim never reaches, so the look is followed through them.
 */
public final class RigAim {

    private RigAim() {
    }

    /** Hands the game's bus what this answers, once, from the mod's client constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, RenderHighlightEvent.Block.class, RigAim::onBlockHighlight);
    }

    public static void onBlockHighlight(final RenderHighlightEvent.Block event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null) {
            return;
        }
        final BlockPos origin = originAt(minecraft, event.getTarget().getBlockPos());
        if (origin == null || !(minecraft.level.getBlockEntity(origin) instanceof MiningComputerBlockEntity rig)) {
            return;
        }
        final Vec3 eye = player.getEyePosition(minecraft.getTimer().getGameTimeDeltaPartialTick(true));
        final Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        final int riser = MiningComputerBlock.aimedRiser(minecraft.level, origin, eye, end);
        final Text hint = riser < 0 ? null : hint(rig, riser, player.getMainHandItem());
        if (hint == null) {
            return;
        }
        final AABB place = MiningComputerBlock.riserPlace(origin, rig.facing(), rig.rigEra(), riser);
        final Vec3 camera = event.getCamera().getPosition();
        ButtonOutline.draw(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()),
                place.move(-camera.x, -camera.y, -camera.z));
        minecraft.gui.setOverlayMessage(GameText.component(hint), false);
        event.setCanceled(true);
    }

    /* What a click on riser {@code riser} with {@code held} would do, or nothing when it would do nothing there. */
    @Nullable
    private static Text hint(final MiningComputerBlockEntity rig, final int riser, final ItemStack held) {
        if (!rig.hasRiser(riser)) {
            return null;
        }
        if (HardwareRegistry.card(held) instanceof GpuSpec && rig.cardPart(riser) == null) {
            return MiningComputerTexts.SEAT_ON.with(GameText.of(held.getHoverName()), riser + 1);
        }
        if (held.isEmpty() && rig.cardPart(riser) != null) {
            return MiningComputerTexts.TAKE_OFF.with(riser + 1);
        }
        return null;
    }

    /* The origin of the rig whose block the look landed on, or none for a block of something else. */
    @Nullable
    private static BlockPos originAt(final Minecraft minecraft, final BlockPos pos) {
        final BlockState state = minecraft.level.getBlockState(pos);
        if (state.getBlock() instanceof MiningComputerBlock) {
            return pos;
        }
        if (state.getBlock() instanceof MiningComputerPartBlock) {
            return MiningComputerStructure.originOf(pos, state.getValue(HorizontalDirectionalBlock.FACING));
        }
        return null;
    }
}
