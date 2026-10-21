/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.multiblock.FadedConsumer;
import dev.jstech.core.client.multiblock.GhostBlocks;
import dev.jstech.core.client.multiblock.MultiblockShows;
import dev.jstech.core.jstechseries.projector.ProjectorSettings;
import dev.jstech.core.jstechseries.projector.ProjectorTexts;
import dev.jstech.core.jstechseries.projector.ProjectorView;
import dev.jstech.core.jstechseries.projector.StructureProjector;
import dev.jstech.core.jstechseries.projector.StructureProjectorBlockEntity;
import dev.jstech.core.multiblock.Projectables;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The Structure Projector on the player's side: for every projector loaded near the player that shows a multiblock,
 * the blocks still missing are drawn as ghosts, pale and see-through, a block that is wrong is outlined in red through
 * whatever stands between, the controller's place is outlined in cyan until it is built, and above the structure float
 * its name, its size and how much is in place, missing and wrong. A ghost looked at says what goes there and on which
 * layer, beside the crosshair. A multiblock a recipe viewer's button asks to show is shown the same way, as a projector
 * at the player's feet would show it, until it is built or the player walks away. Its screen is registered here too.
 */
@PaletteHolder
public final class ProjectorClient {

    /** How far from the player a projector's multiblock is drawn, in blocks. */
    public static final double RANGE = 64.0;
    /** How far a ghost is looked at from, in blocks. */
    public static final double REACH = 8.0;
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "structure_projector");
    /* What is seen of the world is read again this often, in ticks: the counts need not follow every block. */
    private static final int READ_EVERY = 10;
    private static final float GHOST_ALPHA = 0.45F;
    private static final float GHOST_SCALE = 0.98F;
    private static final double GROW = 0.004;
    private static final double LOOK_STEP = 0.05;
    /** The colours of the words above a structure and of its marks. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "structure_projector", new Colours(
            0xFF << 24 | 0xFFFFFF, 0xFF << 24 | 0xB4BCC6, 0xFF << 24 | 0x5FE6F0, 0xFF << 24 | 0xF0564A,
            0xC0 << 24 | 0x262E3A, 0xF0564A, 0x5FE6F0, 0x20 << 24 | 0xFFFFFF));
    private static final float TEXT_SCALE = 0.025F;
    private static final int LINE_HEIGHT = 10;
    private static final int MOST_NAMED = 3;
    private static final int TOOLTIP_OFFSET = 10;

    /* What each projector saw when it last looked, by its place, and when it looked. */
    private static final Map<BlockPos, Seen> SEEN = new HashMap<>();
    /* What other tools of the series show the same way, each asked every frame. */
    private static final List<Supplier<Optional<Shown>>> ALSO = new CopyOnWriteArrayList<>();

    /* The multiblock a recipe viewer asked to show in front of the player, and what was last seen of it. */
    @Nullable
    private static Held held;

    private ProjectorClient() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, ProjectorClient::onRender);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> forget());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.Clone.class, event -> forget());
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                LAYER, ProjectorClient::drawLooked));
        modBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(StructureProjector.MENU.get(),
                StructureProjectorScreen::new));
        MultiblockShows.addTool(() -> new ItemStack(StructureProjector.BLOCK.asItem()));
        MultiblockShows.showsInWorld(new HeldShow());
    }

    /**
     * Shows what {@code source} gives the way a projector shows its multiblock, ghosts, marks and words, every frame it
     * gives one: for another tool of the series that shows a multiblock, as the Multiblock Builder does.
     */
    public static void showAlso(final Supplier<Optional<Shown>> source) {
        ALSO.add(source);
    }

    /** What {@code projector} sees now, read again when it is older than a moment or its settings changed. */
    @Nullable
    public static ProjectorView seen(final StructureProjectorBlockEntity projector) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        final long now = minecraft.level.getGameTime();
        final ProjectorSettings settings = projector.settings();
        final Seen known = SEEN.get(projector.getBlockPos());
        if (known != null && known.settings().equals(settings) && now - known.at() < READ_EVERY) {
            return known.view();
        }
        final ProjectorView view = ProjectorView.of(minecraft.level, projector).orElse(null);
        SEEN.put(projector.getBlockPos().immutable(), new Seen(settings, now, view));
        return view;
    }

    private static void onRender(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        final Camera camera = event.getCamera();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        for (final Projection projection : projections(minecraft.player)) {
            drawGhosts(pose, camera, buffers, minecraft.getBlockRenderer(), projection.view(), projection.settings());
            drawWords(pose, camera, minecraft.font, buffers, projection);
        }
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        buffers.endBatch(SeenThrough.LINES);
        buffers.endBatch();
    }

    /*
     * Everything shown to the player now: what each projector near enough sees, and the multiblock a recipe viewer
     * asked to show, which goes once it is built or the player walks away from it.
     */
    private static List<Projection> projections(final Player player) {
        final List<Projection> out = new ArrayList<>();
        for (final StructureProjectorBlockEntity projector : shown(player)) {
            final ProjectorView view = seen(projector);
            if (view != null) {
                out.add(named(projector.settings(), view));
            }
        }
        final Held showing = held;
        if (showing != null) {
            final ProjectorView view = showing.view(player);
            if (view == null || showing.at().distToCenterSqr(player.position()) > RANGE * RANGE
                    || view.missing() == 0 && view.wrong() == 0) {
                held = null;
            } else {
                out.add(named(showing.settings(), view));
            }
        }
        for (final Supplier<Optional<Shown>> source : ALSO) {
            source.get().ifPresent(shown -> out.add(new Projection(ProjectorSettings.FRESH, shown.view(),
                    shown.name(), shown.mod())));
        }
        return out;
    }

    /* A projection of the multiblock its settings choose, named as the multiblock and its mod are. */
    private static Projection named(final ProjectorSettings settings, final ProjectorView view) {
        final ResourceLocation id = settings.chosen().orElseThrow();
        return new Projection(settings, view, Projectables.name(id), ScreenWords.modName(id));
    }

    private static void forget() {
        SEEN.clear();
        held = null;
    }

    /* The projectors loaded in the player's level, showing, near enough to be drawn. */
    private static List<StructureProjectorBlockEntity> shown(final Player player) {
        final List<StructureProjectorBlockEntity> out = new ArrayList<>();
        for (final StructureProjectorBlockEntity projector : StructureProjectorBlockEntity.shown()) {
            if (projector.getLevel() == player.level() && !projector.isRemoved() && projector.settings().on()
                    && projector.getBlockPos().distToCenterSqr(player.position()) <= RANGE * RANGE) {
                out.add(projector);
            }
        }
        return out;
    }

    /* The ghosts of what is missing, the red outline of what is wrong, the cyan outline of a missing controller. */
    private static void drawGhosts(final PoseStack pose, final Camera camera,
                                   final MultiBufferSource.BufferSource buffers, final BlockRenderDispatcher blocks,
                                   final ProjectorView view, final ProjectorSettings settings) {
        final Vec3 eye = camera.getPosition();
        final VertexConsumer faded = new FadedConsumer(buffers.getBuffer(Sheets.translucentCullBlockSheet()),
                GHOST_ALPHA);
        final VertexConsumer lines = buffers.getBuffer(SeenThrough.LINES);
        for (final ProjectorView.Place place : view.upTo(settings.layer())) {
            if (place.state() == ProjectorView.State.PLACED) {
                continue;
            }
            final BlockPos at = place.pos();
            if (place.state() == ProjectorView.State.WRONG) {
                outline(pose, eye, lines, at, PALETTE.get().wrongMark());
                if (settings.missingOnly()) {
                    continue;
                }
            }
            final BlockState state = GhostBlocks.stateOf(place.ghost());
            if (state == null) {
                outline(pose, eye, lines, at, PALETTE.get().controllerMark());
                continue;
            }
            pose.pushPose();
            pose.translate(at.getX() - eye.x + 0.5, at.getY() - eye.y + 0.5, at.getZ() - eye.z + 0.5);
            pose.scale(GHOST_SCALE, GHOST_SCALE, GHOST_SCALE);
            pose.translate(-0.5, -0.5, -0.5);
            blocks.getModelRenderer().renderModel(pose.last(), faded, state, blocks.getBlockModel(state), 1F, 1F,
                    1F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
        }
    }

    private static void outline(final PoseStack pose, final Vec3 eye, final VertexConsumer lines, final BlockPos at,
                                final int colour) {
        pose.pushPose();
        pose.translate(-eye.x, -eye.y, -eye.z);
        LevelRenderer.renderLineBox(pose, lines, new AABB(at).inflate(GROW), (colour >> 16 & 0xFF) / 255F,
                (colour >> 8 & 0xFF) / 255F, (colour & 0xFF) / 255F, 1F);
        pose.popPose();
    }

    /* The words above the structure, every line facing the camera. */
    private static void drawWords(final PoseStack pose, final Camera camera, final Font font,
                                  final MultiBufferSource buffers, final Projection projection) {
        final ProjectorView view = projection.view();
        final ShapePreview preview = view.preview();
        final Vec3 top = Vec3.atLowerCornerOf(view.controller()).add(preview.minX() + preview.sizeX() / 2.0,
                preview.minY() + preview.sizeY() + 1.2, preview.minZ() + preview.sizeZ() / 2.0);
        final List<Line> lines = words(projection);
        final Vec3 from = top.subtract(camera.getPosition());
        pose.pushPose();
        pose.translate(from.x, from.y, from.z);
        pose.mulPose(camera.rotation());
        pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        int widest = 0;
        for (final Line line : lines) {
            widest = Math.max(widest, font.width(line.text()));
        }
        /*
         * As the game draws a name over a head: the ground first, seen through what stands between, with the words
         * faint on it, then the words themselves; drawn in one go, the ground would hide the words it is behind.
         */
        for (int i = 0; i < lines.size(); i++) {
            final Line line = lines.get(i);
            final float y = (i - lines.size()) * LINE_HEIGHT;
            font.drawInBatch(line.text(), -widest / 2.0F, y, PALETTE.get().faint(), false, pose.last().pose(),
                    buffers, Font.DisplayMode.SEE_THROUGH, PALETTE.get().ground(), LightTexture.FULL_BRIGHT);
            font.drawInBatch(line.text(), -widest / 2.0F, y, line.colour(), false, pose.last().pose(), buffers,
                    Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
        pose.popPose();
    }

    /* What floats above the structure: its name, mod and size; how much is in place; what is missing; what wrong. */
    private static List<Line> words(final Projection projection) {
        final Colours c = PALETTE.get();
        final List<Line> lines = new ArrayList<>();
        final ProjectorView view = projection.view();
        final ShapePreview preview = view.preview();
        lines.add(new Line(GameText.component(ProjectorTexts.SIZE_LINE.with(projection.name(), preview.sizeX(),
                preview.sizeY(), preview.sizeZ())), c.title()));
        lines.add(new Line(GameText.component(projection.mod()), c.words()));
        lines.add(new Line(GameText.component(ProjectorTexts.IN_PLACE.with(view.placed(), view.places().size())),
                c.words()));
        if (view.missing() > 0) {
            lines.add(new Line(GameText.component(ProjectorTexts.MISSING_LINE.with(view.missing(),
                    ScreenWords.counts(view.missingByBlock(), MOST_NAMED))), c.missing()));
        }
        final ProjectorView.Place wrong = view.places().stream()
                .filter(place -> place.state() == ProjectorView.State.WRONG).findFirst().orElse(null);
        if (wrong != null) {
            final Minecraft minecraft = Minecraft.getInstance();
            final Component found = Objects.requireNonNull(minecraft.level).getBlockState(wrong.pos()).getBlock()
                    .getName();
            lines.add(new Line(GameText.component(ProjectorTexts.WRONG_LINE.with(view.wrong(), GameText.of(found),
                    GameText.of(GhostBlocks.nameOf(wrong.ghost().first())))), c.wrong()));
        }
        if (view.missing() > 0 || view.wrong() > 0) {
            lines.add(new Line(GameText.component(ProjectorTexts.LOOK.text()), c.words()));
        }
        return lines;
    }

    /* Beside the crosshair, the ghost looked at: what goes there and on which layer. */
    private static void drawLooked(final GuiGraphics g, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        final Looked looked = looked(minecraft.player, delta.getGameTimeDeltaPartialTick(true));
        if (looked == null) {
            return;
        }
        final ShapePreview.Ghost ghost = looked.place().ghost();
        final Component what = ghost.first() == null ? GameText.component(ProjectorTexts.CONTROLLER)
                : GhostBlocks.nameOf(ghost.first());
        g.renderComponentTooltip(minecraft.font, List.of(what, GameText.component(ProjectorTexts.GHOST_LAYER.with(
                ghost.layer() + 1, looked.layers()))), g.guiWidth() / 2 + TOOLTIP_OFFSET,
                g.guiHeight() / 2 + TOOLTIP_OFFSET);
    }

    /* The ghost along the player's look, nearer than any real block they look at; none for none. */
    @Nullable
    private static Looked looked(final Player player, final float partial) {
        final Map<BlockPos, Looked> ghosts = new HashMap<>();
        for (final Projection projection : projections(player)) {
            for (final ProjectorView.Place place : projection.view().upTo(projection.settings().layer())) {
                if (place.state() != ProjectorView.State.PLACED) {
                    ghosts.put(place.pos(), new Looked(place, projection.view().preview().layers()));
                }
            }
        }
        if (ghosts.isEmpty()) {
            return null;
        }
        final Vec3 eye = player.getEyePosition(partial);
        final Vec3 look = player.getViewVector(partial);
        for (double t = 0; t <= REACH; t += LOOK_STEP) {
            final BlockPos at = BlockPos.containing(eye.add(look.scale(t)));
            final Looked place = ghosts.get(at);
            if (place != null) {
                return place;
            }
            if (!player.level().getBlockState(at).isAir()) {
                return null;
            }
        }
        return null;
    }

    /** What a projector last saw, with the settings it saw it with and when. */
    private record Seen(ProjectorSettings settings, long at, @Nullable ProjectorView view) {
    }

    /** One multiblock shown: how it is shown, what is seen of it, and what it and its mod are called. */
    private record Projection(ProjectorSettings settings, ProjectorView view, Text name, Text mod) {
    }

    /**
     * A multiblock another tool shows the way a projector does, every layer of it.
     *
     * @param view what is seen of it in the world
     * @param name what it is called
     * @param mod  what its mod is called
     */
    public record Shown(ProjectorView view, Text name, Text mod) {
    }

    /** A ghost looked at, and how many layers its multiblock has. */
    private record Looked(ProjectorView.Place place, int layers) {
    }

    /**
     * The multiblock a recipe viewer asked to show, as a projector at the player's feet facing where they looked
     * would show it, and what was last seen of it and when.
     */
    private static final class Held {

        private final ResourceLocation id;
        private final Supplier<Optional<ShapePreview>> shape;
        private final BlockPos at;
        private final Direction facing;
        private final ProjectorSettings settings;
        @Nullable
        private ProjectorView seen;
        private long seenAt = Long.MIN_VALUE;

        private Held(final ResourceLocation id, final Supplier<Optional<ShapePreview>> shape, final BlockPos at,
                     final Direction facing) {
            this.id = id;
            this.shape = shape;
            this.at = at.immutable();
            this.facing = facing;
            this.settings = ProjectorSettings.FRESH.choosing(id);
        }

        ResourceLocation id() {
            return this.id;
        }

        BlockPos at() {
            return this.at;
        }

        ProjectorSettings settings() {
            return this.settings;
        }

        /* What is seen of it now, read again when older than a moment; none when it can no longer be shown. */
        @Nullable
        ProjectorView view(final Player player) {
            final long now = player.level().getGameTime();
            if (this.seen != null && now - this.seenAt < READ_EVERY) {
                return this.seen;
            }
            final Optional<ShapePreview> preview = this.shape.get();
            if (preview.isEmpty()) {
                return null;
            }
            final int axis = this.facing.getAxis() == Direction.Axis.X ? 0 : 2;
            final int[] controller = preview.get().anchor(this.at.getX(), this.at.getY(), this.at.getZ(), axis,
                    this.facing.getAxisDirection().getStep());
            this.seen = ProjectorView.of(player.level(), preview.get(),
                    new BlockPos(controller[0], controller[1], controller[2]));
            this.seenAt = now;
            return this.seen;
        }
    }

    /* What shows a multiblock in front of the player when a recipe viewer's button asks. */
    private static final class HeldShow implements MultiblockShows.IWorldShow {

        @Override
        public void show(@Nullable final ResourceLocation id) {
            final Player player = Minecraft.getInstance().player;
            held = id == null || player == null ? null : new Held(id, () -> Projectables.preview(id, 0),
                    player.blockPosition(), player.getDirection());
        }

        /* Blocks that are no multiblock, such as a manual's scene, shown the same way. */
        @Override
        public void showShape(final ResourceLocation id, final ShapePreview preview) {
            final Player player = Minecraft.getInstance().player;
            held = player == null ? null : new Held(id, () -> Optional.of(preview), player.blockPosition(),
                    player.getDirection());
        }

        @Override
        public Optional<ResourceLocation> shown() {
            final Held showing = held;
            return showing == null ? Optional.empty() : Optional.of(showing.id());
        }
    }

    /** One line of the words above a structure, and its colour. */
    private record Line(Component text, int colour) {
    }

    /**
     * The colours of the projector's words and marks.
     *
     * @param title          the multiblock's name, the first line above it
     * @param words          the lines that only tell
     * @param missing        the line of what is missing
     * @param wrong          the line of what is wrong
     * @param ground         the dim ground behind the words
     * @param wrongMark      the outline of a block that is wrong, without its alpha
     * @param controllerMark the outline of the controller's place, without its alpha
     * @param faint          the words as they show through what stands between, faint
     */
    public record Colours(int title, int words, int missing, int wrong, int ground, int wrongMark,
                          int controllerMark, int faint) {
    }

    /* Lines drawn over everything, as a mark seen through walls is: the game's own lines with no depth test. */
    private static final class SeenThrough extends RenderType {

        private static final RenderType LINES = create("jscore_projector_marks",
                DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536, CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(2.0)))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .createCompositeState(false));

        /* Never made: the class only reaches the game's render states, which only a render type may. */
        private SeenThrough(final String name, final VertexFormat format, final VertexFormat.Mode mode,
                            final int bufferSize, final boolean crumbling, final boolean sort, final Runnable setup,
                            final Runnable clear) {
            super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
        }
    }
}
