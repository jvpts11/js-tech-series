/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.tooltip.ItemTooltip;
import dev.jstech.core.client.gui.tooltip.ItemTooltips;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.moving.ForceDiagramTexts;
import dev.jstech.core.moving.PhysicsSight;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a block's tooltip says of it in the physics of a moving structure: its mass, and each force it gives, the
 * thrust of an engine, the wing of a wing, the hold of a hover coil, the pull of a magnet, the wheel, the burner and
 * what it displaces. Shown while the player holds the key of the diagram of forces, or holds or wears an item that
 * sees physics; a block that gives a force says, otherwise, which key shows it.
 */
public final class PhysicsTooltips {

    private static final double TONNE = 1_000;
    private static final double TWICE = 2;

    private PhysicsTooltips() {
    }

    /** Hands the Core's tooltips what this says, once, from the Core's client. */
    public static void listen() {
        ItemTooltips.add(JsCore.MODID, PhysicsTooltips::describe);
    }

    private static void describe(final ItemStack stack, final ItemTooltip tooltip) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return;
        }
        final BlockState state = item.getBlock().defaultBlockState();
        final BlockPhysics physics = BlockPhysics.of(state);
        final List<Component> lines = new ArrayList<>();
        if (!sees(tooltip.player())) {
            if (physics != BlockPhysics.NONE) {
                tooltip.line(GameText.component(PhysicsSight.HINT.with(keyName())).copy()
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }
        final UnitFormatter format = UnitFormatter.forCurrentLocale();
        lines.add(line(PhysicsSight.HEAD));
        final double mass = BlockPhysics.massOf(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, state);
        lines.add(line(PhysicsSight.MASS.with(GameText.resolve(mass >= TONNE
                ? ForceDiagramTexts.TONNES.with(format.decimal(mass / TONNE))
                : ForceDiagramTexts.KILOGRAMS.with(format.decimal(mass))))));
        physics.thrust().ifPresent(thrust -> {
            lines.add(line(PhysicsSight.THRUST.with(newtons(format, thrust.newtons()))));
            thrust.shaft().ifPresent(rpm -> lines.add(line(PhysicsSight.SHAFT.with(format.decimal(rpm)))));
        });
        physics.wing().ifPresent(wing -> lines.add(line(PhysicsSight.WING.with(format.decimal(wing.area()),
                format.decimal(wing.stall())))));
        physics.displacement().ifPresent(volume -> lines.add(line(PhysicsSight.DISPLACES.with(
                format.decimal(volume)))));
        physics.levitation().ifPresent(held -> lines.add(line(PhysicsSight.LEVITATION.with(
                newtons(format, held)))));
        physics.magnet().ifPresent(magnet -> lines.add(line(PhysicsSight.MAGNET.with(newtons(format,
                magnet.newtons()), format.decimal(magnet.reach())))));
        physics.wheel().ifPresent(wheel -> lines.add(line(PhysicsSight.WHEEL.with(format.decimal(wheel.radius()
                * TWICE), newtons(format, wheel.drive()), newtons(format, wheel.brake())))));
        physics.burner().ifPresent(burner -> {
            final UnitFormatter.Scaled watts = format.si(Math.round(burner.watts()));
            lines.add(line(PhysicsSight.BURNER.with(GameText.resolve(PhysicsSight.WATTS.with(watts.number(),
                    watts.prefix())))));
        });
        physics.drag().ifPresent(share -> lines.add(line(PhysicsSight.DRAG.with(format.decimal(share)))));
        lines.forEach(tooltip::line);
    }

    /* Whether the player sees a block's physics now: the key held, or an item that sees it held or worn. */
    private static boolean sees(final Player player) {
        if (player != null && PhysicsSight.sees(player)) {
            return true;
        }
        final KeyMapping mapping = KeyActionsClient.mapping(CoreKeys.STRUCTURE_FORCES);
        final InputConstants.Key key = mapping.getKey();
        return key.getType() == InputConstants.Type.KEYSYM && key.getValue() != InputConstants.UNKNOWN.getValue()
                && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getValue());
    }

    private static String keyName() {
        return KeyActionsClient.mapping(CoreKeys.STRUCTURE_FORCES).getTranslatedKeyMessage().getString();
    }

    private static String newtons(final UnitFormatter format, final double force) {
        final UnitFormatter.Scaled scaled = format.si(Math.round(force));
        return GameText.resolve(ForceDiagramTexts.NEWTONS.with(scaled.number(), scaled.prefix()));
    }

    private static Component line(final TextKey text) {
        return GameText.component(text).copy().withStyle(ChatFormatting.GRAY);
    }

    private static Component line(final Text text) {
        return GameText.component(text).copy().withStyle(ChatFormatting.GRAY);
    }
}
