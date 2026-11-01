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
import dev.jstech.core.model.ModelPose;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * Armour drawn with a model of the Core's: each of its bones named for a part of whoever wears it follows that part,
 * as the game's own armour does, by the names Blockbench's armour templates give them ({@code armorHead},
 * {@code armorBody}, {@code armorRightArm}, {@code armorLeftArm}, {@code armorRightLeg}, {@code armorLeftLeg},
 * {@code armorRightBoot}, {@code armorLeftBoot}); a bone hangs from its part by its pivot, and what hangs from it moves
 * with it. Each piece worn draws the bones of its own slot. Leggings show the body's bone too, as the waist they hang
 * from, unless the model has a bone of its own for the waist, {@code armorWaist}, which follows the body: then the
 * body's bone is the chestplate's alone, so leggings worn alone show no chest. A bone may be shown only while the
 * piece's stack says so, a visor lowered or a module fitted. The piece is painted with its model's textures, and
 * glints when it is enchanted. A piece worn by a player covers the skin's outer layer of its parts; in first person,
 * the player's arm is drawn in the sleeve of the piece on their chest.
 *
 * <pre>{@code
 * event.registerItem(KitArmorModel.of(ResourceLocation.fromNamespaceAndPath(MODID, "exosuit"))
 *         .shownWhile("visor", stack -> stack.has(VISOR_DOWN.get())).extensions(),
 *         HELMET.get(), CHESTPLATE.get(), LEGGINGS.get(), BOOTS.get());
 * }</pre>
 */
public final class KitArmorModel {

    private final Function<ItemStack, ResourceLocation> model;
    private final List<Shown> shown;
    /* Made the first time a piece is drawn: the game's limbs are only there once its entity models are read. */
    @Nullable
    private Worn worn;

    /** The bone of a model's own for the waist leggings hang from, which follows the body. */
    public static final String WAIST = "armorWaist";
    private static final String BODY = "armorBody";
    private static final String RIGHT_ARM = "armorRightArm";
    private static final String LEFT_ARM = "armorLeftArm";
    private static final List<Follow> FOLLOWS = List.of(
            new Follow("armorHead", Set.of(EquipmentSlot.HEAD), model -> model.head),
            new Follow(BODY, Set.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS), model -> model.body),
            new Follow(WAIST, Set.of(EquipmentSlot.LEGS), model -> model.body),
            new Follow(RIGHT_ARM, Set.of(EquipmentSlot.CHEST), model -> model.rightArm),
            new Follow(LEFT_ARM, Set.of(EquipmentSlot.CHEST), model -> model.leftArm),
            new Follow("armorRightLeg", Set.of(EquipmentSlot.LEGS), model -> model.rightLeg),
            new Follow("armorLeftLeg", Set.of(EquipmentSlot.LEGS), model -> model.leftLeg),
            new Follow("armorRightBoot", Set.of(EquipmentSlot.FEET), model -> model.rightLeg),
            new Follow("armorLeftBoot", Set.of(EquipmentSlot.FEET), model -> model.leftLeg));

    private KitArmorModel(final Function<ItemStack, ResourceLocation> model, final List<Shown> shown) {
        this.model = model;
        this.shown = List.copyOf(shown);
    }

    /** Armour drawn with the model {@code model}. */
    public static KitArmorModel of(final ResourceLocation model) {
        Objects.requireNonNull(model, "model");
        return new KitArmorModel(stack -> model, List.of());
    }

    /** Armour drawn with the model each stack names. */
    public static KitArmorModel of(final Function<ItemStack, ResourceLocation> model) {
        return new KitArmorModel(Objects.requireNonNull(model, "model"), List.of());
    }

    /**
     * This armour with its bone {@code bone}, and what hangs from it, shown only while {@code shown} holds for the
     * stack of the piece that draws it.
     */
    public KitArmorModel shownWhile(final String bone, final Predicate<ItemStack> shown) {
        final List<Shown> more = new ArrayList<>(this.shown);
        more.add(new Shown(Objects.requireNonNull(bone, "bone"), Objects.requireNonNull(shown, "shown")));
        return new KitArmorModel(this.model, more);
    }

    /** What the game asks an item for to be drawn as this armour. */
    public IClientItemExtensions extensions() {
        return new Extensions(this);
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderArmEvent.class, KitArmorModel::onArm);
        NeoForge.EVENT_BUS.addListener(RenderPlayerEvent.Pre.class, KitArmorModel::onPlayer);
    }

    /*
     * A piece of this kind worn covers the skin's outer layer of the parts it is worn on, as a suit does: the hat under
     * a helmet, the jacket and the sleeves under a chest piece, the trouser legs under leggings. The game shows that
     * layer a quarter or half a pixel out from the body, where a piece fitted close to the body would let it through;
     * it shows it again by itself each frame, from the player's settings, once the piece is off.
     */
    private static void onPlayer(final RenderPlayerEvent.Pre event) {
        final Player player = event.getEntity();
        final PlayerModel<AbstractClientPlayer> body = event.getRenderer().getModel();
        if (kit(player.getItemBySlot(EquipmentSlot.HEAD))) {
            body.hat.visible = false;
        }
        if (kit(player.getItemBySlot(EquipmentSlot.CHEST))) {
            body.jacket.visible = false;
            body.leftSleeve.visible = false;
            body.rightSleeve.visible = false;
        }
        if (kit(player.getItemBySlot(EquipmentSlot.LEGS))) {
            body.leftPants.visible = false;
            body.rightPants.visible = false;
        }
    }

    /* Whether {@code stack} is a piece drawn with a model of the Core's. */
    private static boolean kit(final ItemStack stack) {
        return !stack.isEmpty() && IClientItemExtensions.of(stack) instanceof Extensions;
    }

    /*
     * The player's own arm drawn in first person: with a piece of this kind on their chest whose model has a bone for
     * that arm, the arm is drawn in its sleeve. The game's arm is drawn without its outer layer, which a sleeve close
     * to the arm would let through, and the sleeve over it by the same bone, posed the way the game poses the arm.
     */
    private static void onArm(final RenderArmEvent event) {
        final AbstractClientPlayer player = event.getPlayer();
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !(IClientItemExtensions.of(chest) instanceof Extensions worn)) {
            return;
        }
        final Optional<KitModel> found = KitModels.get(worn.armour.model.apply(chest));
        if (found.isEmpty()) {
            return;
        }
        final KitInstance instance = KitAnimations.of(chest, ItemDisplayContext.NONE, found.get());
        instance.animate(KitClock.frame(), KitClock.now(), false, IAnimationListener.NONE);
        final ModelPose pose = instance.pose();
        final boolean right = event.getArm() == HumanoidArm.RIGHT;
        final int bone = pose.index(right ? RIGHT_ARM : LEFT_ARM);
        if (bone < 0) {
            return;
        }
        for (final Shown each : worn.armour.shown) {
            final int shownBone = pose.index(each.bone);
            if (shownBone >= 0) {
                pose.setHidden(shownBone, !each.shown.test(chest));
            }
        }
        event.setCanceled(true);
        final PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(player);
        final PlayerModel<AbstractClientPlayer> body = renderer.getModel();
        // As the game poses the arm it draws in first person.
        body.attackTime = 0.0F;
        body.crouching = false;
        body.swimAmount = 0.0F;
        body.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        final ModelPart arm = right ? body.rightArm : body.leftArm;
        arm.xRot = 0.0F;
        final PoseStack poses = event.getPoseStack();
        final MultiBufferSource buffers = event.getMultiBufferSource();
        arm.render(poses, buffers.getBuffer(RenderType.entitySolid(player.getSkin().texture())),
                event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        final VertexConsumer glint = chest.hasFoil() ? buffers.getBuffer(RenderType.armorEntityGlint()) : null;
        poses.pushPose();
        arm.translateAndRotate(poses);
        poses.scale(-1, -1, 1);
        KitRenderer.drawBranch(instance, bone, poses, buffers, event.getPackedLight(), OverlayTexture.NO_OVERLAY,
                glint);
        poses.popPose();
    }

    /* Whether a piece in {@code slot} draws {@code follow}'s bone of the model posed as {@code pose}. */
    private static boolean draws(final Follow follow, final EquipmentSlot slot, final ModelPose pose) {
        if (!follow.slots.contains(slot)) {
            return false;
        }
        // A model with a waist of its own gives leggings that, and the body to the chestplate alone.
        return !(BODY.equals(follow.bone) && slot == EquipmentSlot.LEGS && pose.index(WAIST) >= 0);
    }

    /* The game's humanoid model the piece is handed to the game as: its limbs placed by the wearer's, drawn by ours. */
    private static final class Worn extends HumanoidModel<LivingEntity> {

        private final Function<ItemStack, ResourceLocation> model;
        private final List<Shown> shown;
        @Nullable
        private LivingEntity wearer;
        private ItemStack stack = ItemStack.EMPTY;
        @Nullable
        private EquipmentSlot slot;
        private long drawnFrame = -1;
        @Nullable
        private Object drawnFor;
        @Nullable
        private EquipmentSlot drawnSlot;

        Worn(final Function<ItemStack, ResourceLocation> model, final List<Shown> shown) {
            super(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
            this.model = model;
            this.shown = shown;
        }

        @Override
        public void renderToBuffer(final PoseStack poses, final VertexConsumer given, final int light,
                                   final int overlay, final int colour) {
            // The game draws a piece once for each layer of its material and once more for its trim and its glint;
            // each piece is drawn once, with its own textures, and its glint with it.
            final long frame = KitClock.frame();
            final Object drawing = this.wearer == null ? this.stack : this.wearer;
            if (frame == this.drawnFrame && drawing == this.drawnFor && this.slot == this.drawnSlot
                    || this.slot == null) {
                return;
            }
            this.drawnFrame = frame;
            this.drawnFor = drawing;
            this.drawnSlot = this.slot;
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
            final ModelPose pose = instance.pose();
            for (final Shown each : this.shown) {
                final int bone = pose.index(each.bone);
                if (bone >= 0) {
                    pose.setHidden(bone, !each.shown.test(this.stack));
                }
            }
            final MultiBufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            final VertexConsumer glint = this.stack.hasFoil() ? buffers.getBuffer(RenderType.armorEntityGlint())
                    : null;
            for (final Follow follow : FOLLOWS) {
                final int bone = pose.index(follow.bone);
                final ModelPart part = follow.part.apply(this);
                if (bone < 0 || !draws(follow, this.slot, pose) || !part.visible) {
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

    /* What the game asks a piece of this armour for, which the first person's arm knows it by. */
    private static final class Extensions implements IClientItemExtensions {

        private final KitArmorModel armour;

        Extensions(final KitArmorModel armour) {
            this.armour = armour;
        }

        @Override
        public HumanoidModel<?> getHumanoidArmorModel(final LivingEntity entity, final ItemStack stack,
                                                      final EquipmentSlot equipmentSlot,
                                                      final HumanoidModel<?> original) {
            if (this.armour.worn == null) {
                this.armour.worn = new Worn(this.armour.model, this.armour.shown);
            }
            this.armour.worn.wearer = entity;
            this.armour.worn.stack = stack;
            this.armour.worn.slot = equipmentSlot;
            return this.armour.worn;
        }
    }

    /* A bone of the armour, the slots of the pieces that show it, and the part of the wearer it follows. */
    private record Follow(String bone, Set<EquipmentSlot> slots, Function<HumanoidModel<?>, ModelPart> part) {
    }

    /* A bone shown only while its piece's stack says so. */
    private record Shown(String bone, Predicate<ItemStack> shown) {
    }
}
