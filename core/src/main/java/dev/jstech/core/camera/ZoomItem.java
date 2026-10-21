/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * An item looked through while it is held in use, as a spyglass: binoculars, a rifle's scope, a hand telescope. It
 * magnifies by real powers, the field of view narrowed as a lens narrows it (a pair of binoculars of 8 shows an
 * eighth of the width), and the mouse wheel steps between the powers it has while the player looks; the mouse turns
 * the view as much slower as it is magnified. A picture of its own, drawn over the screen as the spyglass's is, is
 * optional.
 *
 * <pre>{@code
 * CONTENT.item("binoculars", properties -> new ZoomItem(properties, List.of(4.0, 8.0),
 *         Optional.of(rl("textures/misc/binoculars.png")))).named("Binoculars").register();
 * }</pre>
 */
public class ZoomItem extends Item {

    private final List<Double> powers;
    private final Optional<ResourceLocation> overlay;

    /* How long it may be held in use: as long as a spyglass. */
    private static final int HELD_AT_MOST = 1200;

    /**
     * An item of {@code properties} that magnifies by {@code powers}, lowest first, and draws {@code overlay}, a
     * picture as large as the screen's height, over the screen while looked through.
     */
    public ZoomItem(final Properties properties, final List<Double> powers,
                    final Optional<ResourceLocation> overlay) {
        super(properties.stacksTo(1));
        this.powers = List.copyOf(powers);
        this.overlay = Objects.requireNonNull(overlay, "overlay");
        if (this.powers.isEmpty()) {
            throw new IllegalArgumentException("an item looked through magnifies by one power or more");
        }
        for (final double power : this.powers) {
            if (!(power >= 1.0) || power > CameraOptics.MOST_POWER) {
                throw new IllegalArgumentException("a power is 1 to " + CameraOptics.MOST_POWER + ", not " + power);
            }
        }
    }

    /** The powers it magnifies by, lowest first. */
    public List<Double> powers() {
        return this.powers;
    }

    /** What it draws over the screen while looked through, or none. */
    public Optional<ResourceLocation> overlay() {
        return this.overlay;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        player.awardStat(Stats.ITEM_USED.get(this));
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return HELD_AT_MOST;
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.SPYGLASS;
    }
}
