/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire;

import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.effect.Effects;
import dev.jstech.core.fire.ExtinguishingAgent;
import dev.jstech.core.fire.Extinguishing;
import dev.jstech.core.fire.FireClass;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A portable extinguisher of one agent: held down, it sprays a cone ahead as far as its agent carries, putting out
 * each fire in it as the agent does on that fire's class (water on live equipment shocks whoever holds it) and the
 * flames on whatever burns in it. It holds so many seconds of spraying, shown as its bar; a water one fills again from
 * water, the others at a mod's station.
 */
public final class ExtinguisherItem extends Item {

    private final ExtinguishingAgent agent;

    /** How many ticks of spraying each agent's extinguisher holds: a water one a minute, carbon dioxide ten seconds. */
    public static final int WATER_HOLDS = 1_200;
    public static final int FOAM_HOLDS = 800;
    public static final int CARBON_DIOXIDE_HOLDS = 200;
    public static final int DRY_POWDER_HOLDS = 300;
    /* How long it may be held down, in ticks, and each how many ticks it sprays and shows its spray. */
    private static final int HELD_UP_TO = 72_000;
    private static final int SPRAYS_EVERY = 2;
    private static final int SHOWN_EVERY = 4;
    private static final int HEARD_EVERY = 10;
    /* How wide its cone is: the lines followed round its middle, at a share of a block off for each block out. */
    private static final double[][] CONE = {{0, 0}, {0.18, 0}, {-0.18, 0}, {0, 0.18}, {0, -0.18}};
    private static final double STEP = 0.5;
    private static final double SPRAY_AHEAD = 0.6;

    public ExtinguisherItem(final ExtinguishingAgent agent, final Properties properties) {
        super(properties);
        this.agent = agent;
    }

    /** How many ticks of spraying an extinguisher of {@code agent} holds. */
    public static int holds(final ExtinguishingAgent agent) {
        return switch (agent) {
            case WATER -> WATER_HOLDS;
            case FOAM -> FOAM_HOLDS;
            case CARBON_DIOXIDE -> CARBON_DIOXIDE_HOLDS;
            case DRY_POWDER -> DRY_POWDER_HOLDS;
        };
    }

    /** How far {@code agent} carries from the nozzle, in blocks. */
    public static double reach(final ExtinguishingAgent agent) {
        return switch (agent) {
            case WATER -> 6;
            case FOAM, DRY_POWDER -> 5;
            case CARBON_DIOXIDE -> 2.5;
        };
    }

    /** What it sprays. */
    public ExtinguishingAgent agent() {
        return this.agent;
    }

    /** Whether {@code stack} has nothing left to spray. */
    public static boolean empty(final ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (this.agent == ExtinguishingAgent.WATER && stack.isDamaged()) {
            final BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() == HitResult.Type.BLOCK && level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
                stack.setDamageValue(0);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        if (empty(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return HELD_UP_TO;
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(final Level level, final LivingEntity entity, final ItemStack stack, final int left) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        final int held = HELD_UP_TO - left;
        if (held % SPRAYS_EVERY != 0) {
            return;
        }
        if (empty(stack)) {
            entity.stopUsingItem();
            return;
        }
        spray(server, entity, this.agent, held % SHOWN_EVERY == 0, held % HEARD_EVERY == 0);
        stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + SPRAYS_EVERY));
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        if (empty(stack)) {
            lines.add(GameText.component(FireSafetyTexts.EMPTY.text()).withStyle(ChatFormatting.RED));
        }
        final List<FireClass> good = this.agent.putsOut();
        if (!good.isEmpty()) {
            lines.add(GameText.component(FireSafetyTexts.PUTS_OUT.with(listed(good)))
                    .withStyle(ChatFormatting.GRAY));
        }
        final List<FireClass> bad = Stream.of(FireClass.values()).filter(fire -> switch (this.agent.on(fire)) {
            case SHOCKS, FLARES, REACTS -> true;
            default -> false;
        }).toList();
        if (!bad.isEmpty()) {
            lines.add(GameText.component(FireSafetyTexts.NEVER_ON.with(listed(bad))).withStyle(ChatFormatting.RED));
        }
        if (this.agent == ExtinguishingAgent.WATER) {
            lines.add(GameText.component(FireSafetyTexts.REFILLS.text()).withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, lines, flag);
    }

    /**
     * {@code sprayer} sprays {@code agent} ahead: each fire in the cone is put out as the agent does on it, each body
     * burning in it stops burning. Shows the spray and sounds it when asked.
     */
    public static void spray(final ServerLevel level, final LivingEntity sprayer, final ExtinguishingAgent agent,
                             final boolean shown, final boolean heard) {
        final Vec3 eyes = sprayer.getEyePosition();
        final Vec3 ahead = sprayer.getLookAngle();
        final Vec3 side = ahead.cross(new Vec3(0, 1, 0)).normalize();
        final Vec3 up = side.cross(ahead).normalize();
        final double reach = reach(agent);
        for (final double[] off : CONE) {
            final Vec3 way = ahead.add(side.scale(off[0])).add(up.scale(off[1])).normalize();
            follow(level, sprayer, agent, eyes, way, reach);
        }
        final AABB cone = new AABB(eyes, eyes.add(ahead.scale(reach))).inflate(1);
        for (final Entity body : level.getEntities(sprayer, cone, Entity::isOnFire)) {
            body.clearFire();
        }
        if (shown) {
            Effects.at(level, FireSafety.spray(agent), eyes.add(ahead.scale(SPRAY_AHEAD))).facing(ahead)
                    .scale((float) (reach / reach(ExtinguishingAgent.WATER))).send();
        }
        if (heard) {
            Audio.cue(level, eyes.x, eyes.y, eyes.z, FireSafety.SPRAYING, SoundContext.EMPTY, 1F, 1F);
        }
    }

    /* Follows one line of the spray until it strikes a solid block, putting out each fire on its way and on that. */
    private static void follow(final ServerLevel level, final LivingEntity sprayer, final ExtinguishingAgent agent,
                               final Vec3 from, final Vec3 way, final double reach) {
        BlockPos last = null;
        for (double along = STEP; along <= reach; along += STEP) {
            final BlockPos at = BlockPos.containing(from.add(way.scale(along)));
            if (at.equals(last) || !level.isLoaded(at)) {
                continue;
            }
            last = at;
            if (level.getBlockState(at).getBlock() instanceof BaseFireBlock) {
                Extinguishing.extinguish(level, at, agent, sprayer);
            } else if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
                // The fire burning on top of what the spray strikes.
                Extinguishing.extinguish(level, at.above(), agent, sprayer);
                return;
            }
        }
    }

    private static Text listed(final List<FireClass> classes) {
        Text all = classes.getFirst().title().text();
        for (int i = 1; i < classes.size(); i++) {
            all = FireSafetyTexts.LIST.with(all, classes.get(i).title().text());
        }
        return all;
    }
}
