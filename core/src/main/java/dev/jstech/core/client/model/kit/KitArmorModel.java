/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.model.IAnimationListener;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

/**
 * Armour drawn with a model of the Core's: each of its bones named for a part of whoever wears it follows that part,
 * as the game's own armour does, by the names Blockbench's armour templates give them ({@code armorHead},
 * {@code armorBody}, {@code armorRightArm}, {@code armorLeftArm}, {@code armorRightLeg}, {@code armorLeftLeg},
 * {@code armorRightBoot}, {@code armorLeftBoot}); a bone hangs from its part by its pivot, and what hangs from it moves
 * with it. The piece is painted with its model's textures, and glints when it is enchanted.
 *
 * <pre>{@code
 * event.registerItem(KitArmorModel.of(ResourceLocation.fromNamespaceAndPath(MODID, "exosuit")).extensions(),
 *         HELMET.get(), CHESTPLATE.get(), LEGGINGS.get(), BOOTS.get());
 * }</pre>
 */
public final class KitArmorModel {

    private final Function<ItemStack, ResourceLocation> model;
    /* Made the first time a piece is drawn: the game's limbs are only there once its entity models are read. */
    @Nullable
    private Worn worn;

    /* Leggings show the body's bone too, as the waist they hang from; a chestplate shows it as its own. */
    private static final List<Follow> FOLLOWS = List.of(
            new Follow("armorHead", Set.of(EquipmentSlot.HEAD), model -> model.head),
            new Follow("armorBody", Set.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS), model -> model.body),
            new Follow("armorRightArm", Set.of(EquipmentSlot.CHEST), model -> model.rightArm),
            new Follow("armorLeftArm", Set.of(EquipmentSlot.CHEST), model -> model.leftArm),
            new Follow("armorRightLeg", Set.of(EquipmentSlot.LEGS), model -> model.rightLeg),
            new Follow("armorLeftLeg", Set.of(EquipmentSlot.LEGS), model -> model.leftLeg),
            new Follow("armorRightBoot", Set.of(EquipmentSlot.FEET), model -> model.rightLeg),
            new Follow("armorLeftBoot", Set.of(EquipmentSlot.FEET), model -> model.leftLeg));

    private KitArmorModel(final Function<ItemStack, ResourceLocation> model) {
        this.model = model;
    }

    /** Armour drawn with the model {@code model}. */
    public static KitArmorModel of(final ResourceLocation model) {
        Objects.requireNonNull(model, "model");
        return new KitArmorModel(stack -> model);
    }

    /** Armour drawn with the model each stack names. */
    public static KitArmorModel of(final Function<ItemStack, ResourceLocation> model) {
        return new KitArmorModel(Objects.requireNonNull(model, "model"));
    }

    /** What the game asks an item for to be drawn as this armour. */
    public IClientItemExtensions extensions() {
        final KitArmorModel armour = this;
        return new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(final LivingEntity entity, final ItemStack stack,
                                                          final EquipmentSlot equipmentSlot,
                                                          final HumanoidModel<?> original) {
                if (armour.worn == null) {
                    armour.worn = new Worn(armour.model);
                }
                armour.worn.wearer = entity;
                armour.worn.stack = stack;
                armour.worn.slot = equipmentSlot;
                return armour.worn;
            }
        };
    }

    /* The game's humanoid model the piece is handed to the game as: its limbs placed by the wearer's, drawn by ours. */
    private static final class Worn extends HumanoidModel<LivingEntity> {

        private final Function<ItemStack, ResourceLocation> model;
        @Nullable
        private LivingEntity wearer;
        private ItemStack stack = ItemStack.EMPTY;
        @Nullable
        private EquipmentSlot slot;
        private long drawnFrame = -1;
        @Nullable
        private Object drawnFor;

        Worn(final Function<ItemStack, ResourceLocation> model) {
            super(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
            this.model = model;
        }

        @Override
        public void renderToBuffer(final PoseStack poses, final VertexConsumer given, final int light,
                                   final int overlay, final int colour) {
            // The game draws a piece once for each layer of its material and once more for its trim and its glint;
            // it is drawn once, with its own textures, and its glint with it.
            final long frame = KitClock.frame();
            final Object drawing = this.wearer == null ? this.stack : this.wearer;
            if (frame == this.drawnFrame && drawing == this.drawnFor || this.slot == null) {
                return;
            }
            this.drawnFrame = frame;
            this.drawnFor = drawing;
            final Optional<KitModel> found = KitModels.get(this.model.apply(this.stack));
            if (found.isEmpty()) {
                return;
            }
            final KitInstance instance = KitAnimations.of(this.stack, ItemDisplayContext.NONE, found.get());
            if (this.wearer != null) {
                // A piece's keyframes read its wearer as an entity's model reads its own.
                EntityQueries.hand(instance.animator().context(), this.wearer,
                        Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
            }
            instance.animate(frame, KitClock.now(), false, IAnimationListener.NONE);
            final MultiBufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            final VertexConsumer glint = this.stack.hasFoil() ? buffers.getBuffer(RenderType.armorEntityGlint())
                    : null;
            for (final Follow follow : FOLLOWS) {
                final int bone = instance.pose().index(follow.bone);
                final ModelPart part = follow.part.apply(this);
                if (bone < 0 || !follow.slots.contains(this.slot) || !part.visible) {
                    continue;
                }
                poses.pushPose();
                part.translateAndRotate(poses);
                // The game's limbs count Y down and X to the wearer's left; the Core's models count Y up.
                poses.scale(-1, -1, 1);
                KitRenderer.drawBranch(instance, bone, poses, buffers, light, overlay, glint);
                poses.popPose();
            }
        }
    }

    /* A bone of the armour, the slots of the pieces that show it, and the part of the wearer it follows. */
    private record Follow(String bone, Set<EquipmentSlot> slots, Function<HumanoidModel<?>, ModelPart> part) {
    }
}
