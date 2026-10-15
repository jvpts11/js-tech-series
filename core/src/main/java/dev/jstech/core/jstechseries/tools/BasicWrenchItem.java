/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEnd;
import dev.jstech.core.cable.CableEnds;
import dev.jstech.core.cable.EndTexts;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.grid.EndMode;
import dev.jstech.core.text.GameText;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/**
 * The Basic Wrench, the series' shared wrench: on a cable, it sets how the end of the wire it points at meets the block
 * beyond, going round normal, push, pull and off, or back round when sneaking, and says what it set.
 */
public final class BasicWrenchItem extends Item {

    public BasicWrenchItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null || !(context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof CableBlockEntity cable)) {
            return InteractionResult.PASS;
        }
        final Vec3 eye = player.getEyePosition();
        final Wire wire = cable.aim(eye, eye.add(player.getLookAngle().scale(player.blockInteractionRange() + 1.0)))
                .wire();
        if (wire == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        final Direction face = CableEnds.facing(context.getClickedPos(), context.getClickLocation(),
                context.getClickedFace());
        final CableEnd before = cable.end(wire.type(), face);
        final CableEnd after = before.withMode(player.isShiftKeyDown() ? previous(before.mode())
                : before.mode().next());
        cable.setEnd(wire.type(), face, after);
        player.displayClientMessage(GameText.component(EndTexts.set(face, after)), true);
        return InteractionResult.CONSUME;
    }

    /* The mode before {@code mode} as the wrench goes round them. */
    private static EndMode previous(final EndMode mode) {
        return mode.next().next().next();
    }
}
