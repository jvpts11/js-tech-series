/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.client.sky.GroundCovers;
import dev.jstech.space.JsSpace;
import dev.jstech.space.network.FarMapPayload;
import dev.jstech.space.universe.ClimbView;
import dev.jstech.space.universe.FarRings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * The ground far below the climb to space, on the graphics card and drawn: the rings of the far map the server sends,
 * painted in the blocks' own colours ({@link GroundColours}); the blocks loaded under the climber, read into a map of
 * their own a few chunks a frame as the climb nears the height where they give way to it; and the game's clouds,
 * spread out to the horizon. Everything here is on the render thread.
 */
final class FarGround {

    /* How many loaded chunks are read into their map each frame. */
    private static final int CHUNKS_A_FRAME = 24;
    private static final int CHUNK = 16;
    private static final int FULL = 255;
    /* How solid a texel of a map is where it is water: all but whole, so the shader tells water from land by it. */
    private static final int WATER = 254;
    private static final ResourceLocation CLOUDS = ResourceLocation.withDefaultNamespace(
            "textures/environment/clouds.png");
    /* The game's clouds: how many blocks a texel of their map is, where the eye stands in it, how fast they drift,
     * how far over their height their tops are and how solid they are. */
    private static final double CLOUD_TEXEL = 12;
    private static final double CLOUD_MAP = 256;
    private static final double CLOUD_V = 0.33;
    private static final float CLOUD_DRIFT = 0.03F;
    private static final double CLOUD_TOP = 4.33;
    private static final float CLOUD_SOLID = 0.8F;
    private static final Ring[] RINGS = new Ring[FarRings.BLOCKS.size()];

    @Nullable
    private static ShaderInstance shader;
    @Nullable
    private static VertexBuffer screen;
    @Nullable
    private static ResourceKey<Level> drawnFor;
    @Nullable
    private static GroundColours colours;
    @Nullable
    private static Ring loaded;
    @Nullable
    private static Gathering gathering;
    @Nullable
    private static GroundTexture clouds;
    @Nullable
    private static Integer sea;

    private FarGround() {
    }

    /** Hands the mod's bus the far ground's shader to build, once, from the mod's client. */
    static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterShadersEvent.class, event -> {
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "far_ground"),
                        DefaultVertexFormat.POSITION), built -> shader = built);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /** A ring of the far map has come from the server: it is painted and takes the last one's place. */
    static void receive(final FarMapPayload map) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null || map.ring() < 0 || map.ring() >= RINGS.length) {
            return;
        }
        follow(level);
        final Registry<Biome> registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        final List<Holder<Biome>> biomes = map.biomes().stream().map(id -> registry
                .getHolder(ResourceKey.create(Registries.BIOME, id)).<Holder<Biome>>map(found -> found)
                .orElseGet(() -> registry.getHolderOrThrow(Biomes.PLAINS))).toList();
        final GroundColours painter = painter();
        final NativeImage colour = new NativeImage(map.size(), map.size(), false);
        final NativeImage height = new NativeImage(map.size(), map.size(), false);
        for (int row = 0; row < map.size(); row++) {
            for (int column = 0; column < map.size(); column++) {
                final int at = row * map.size() + column;
                final int index = map.pixels()[at] & FULL;
                final int ground = FarMapPayload.blocks(map.heights()[at]);
                final int rgb = painter.ofMap(biomes.get(Math.min(index, biomes.size() - 1)), ground, map.sea());
                colour.setPixelRGBA(column, row, FastColor.ABGR32.fromArgb32(
                        FastColor.ARGB32.color(ground < map.sea() ? WATER : FULL, rgb)));
                height.setPixelRGBA(column, row, FastColor.ABGR32.color(FULL, 0, 0, map.heights()[at] & FULL));
            }
        }
        if (RINGS[map.ring()] != null) {
            RINGS[map.ring()].release();
        }
        RINGS[map.ring()] = new Ring(GroundTexture.colours(colour), GroundTexture.heights(height), map.centreX(),
                map.centreZ(), map.blocks(), map.size());
        sea = map.sea();
    }

    /**
     * The height of {@code level}'s sea: the one the server drew its far map with, as only the server knows it, or
     * the game's guess until one has come.
     */
    static int sea(final ClientLevel level) {
        return level.dimension().equals(drawnFor) && sea != null ? sea : level.getSeaLevel();
    }

    /** Lets everything go, as the player leaves the world. */
    static void clear() {
        for (int ring = 0; ring < RINGS.length; ring++) {
            if (RINGS[ring] != null) {
                RINGS[ring].release();
                RINGS[ring] = null;
            }
        }
        forgetLoaded();
        drawnFor = null;
        colours = null;
        sea = null;
    }

    /** Lets the map of the loaded blocks go, as the climber comes back down among them. */
    static void forgetLoaded() {
        if (loaded != null) {
            loaded.release();
            loaded = null;
        }
        gathering = null;
    }

    /**
     * Reads a few more of the chunks loaded round {@code centre} into their map, starting it if none is under way:
     * done when it returns true.
     */
    static boolean gather(final ClientLevel level, final BlockPos centre) {
        follow(level);
        if (loaded != null) {
            return true;
        }
        if (gathering == null) {
            final int reach = Minecraft.getInstance().options.getEffectiveRenderDistance();
            gathering = new Gathering(Math.floorDiv(centre.getX(), CHUNK) - reach,
                    Math.floorDiv(centre.getZ(), CHUNK) - reach, reach * 2 + 1);
        }
        final Gathering under = gathering;
        final GroundColours painter = painter();
        for (int read = 0; read < CHUNKS_A_FRAME && under.next < under.chunks * under.chunks; read++) {
            final int chunkX = under.firstChunkX + under.next % under.chunks;
            final int chunkZ = under.firstChunkZ + under.next / under.chunks;
            under.next++;
            final LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
            if (chunk != null) {
                under.read(level, chunk, painter);
            }
        }
        if (under.next < under.chunks * under.chunks) {
            return false;
        }
        loaded = under.ring();
        gathering = null;
        return true;
    }

    /** Whether the far map has come for {@code level}. */
    static boolean has(final ClientLevel level) {
        return level.dimension().equals(drawnFor) && RINGS[0] != null;
    }

    /**
     * Draws the far ground of {@code level} as {@code eye} sees it on the way up, the climb at {@code climb}, its
     * light, haze and clouds as {@code look} gives them, over what is drawn behind. Between
     * {@link SphereDrawing#begin} and {@link SphereDrawing#end}.
     */
    static void draw(final ClientLevel level, final Vec3 eye, final ClimbView.Climb climb, final Look look,
                     final Matrix4f view, final Matrix4f projection) {
        final ShaderInstance ground = shader;
        if (ground == null || !level.dimension().equals(drawnFor)) {
            return;
        }
        final Vector3d up = new Vector3d(climb.up()[0], climb.up()[1], climb.up()[2]).normalize();
        final Vector3d south = new Vector3d(0, 0, 1).sub(new Vector3d(up).mul(up.z)).normalize();
        final Vector3d east = new Vector3d(up).cross(south).normalize();
        final int sea = sea(level);
        bind(0, RINGS[0]);
        bind(1, RINGS[1]);
        bind(2, loaded);
        RenderSystem.setShaderTexture(6, cloudMap().getId());
        ground.safeGetUniform("RayMat").set(new Matrix4f(projection).mul(view).invert());
        ground.safeGetUniform("Up").set((float) up.x, (float) up.y, (float) up.z);
        ground.safeGetUniform("East").set((float) east.x, (float) east.y, (float) east.z);
        ground.safeGetUniform("South").set((float) south.x, (float) south.y, (float) south.z);
        ground.safeGetUniform("EyeHeight").set((float) climb.eyeHeight());
        ground.safeGetUniform("Bend").set((float) climb.bend());
        ground.safeGetUniform("Sea").set((float) sea);
        ground.safeGetUniform("Top").set((float) (level.getMaxBuildHeight() - sea));
        final int water = painter().sea();
        final int sand = painter().shore();
        ground.safeGetUniform("Water").set((water >> 16 & FULL) / (float) FULL, (water >> 8 & FULL) / (float) FULL,
                (water & FULL) / (float) FULL);
        ground.safeGetUniform("Shore").set((sand >> 16 & FULL) / (float) FULL, (sand >> 8 & FULL) / (float) FULL,
                (sand & FULL) / (float) FULL);
        ring(ground, "Ring0", RINGS[0], eye);
        ring(ground, "Ring1", RINGS[1], eye);
        // The loaded blocks' own map where the game no longer draws them, and the game's clouds left to it until then.
        final boolean covered = GroundCovers.covered();
        ring(ground, "Loaded", covered ? loaded : null, eye);
        ground.safeGetUniform("Texel").set((float) FarRings.BLOCKS.get(0), (float) FarRings.BLOCKS.get(1), 1F);
        // How wide a pixel looks: the game's view upright, over the window's height.
        final double fov = 2 * Math.atan(1 / projection.m11());
        ground.safeGetUniform("PixelAngle").set((float) (fov / Minecraft.getInstance().getWindow().getHeight()));
        clouds(ground, level, eye, look, covered);
        ground.safeGetUniform("Haze").set(look.haze()[0], look.haze()[1], look.haze()[2]);
        ground.safeGetUniform("HazeEnd").set(look.hazeEnd());
        ground.safeGetUniform("Veil").set((float) climb.veil() * look.veilStrength());
        ground.safeGetUniform("VeilColour").set(look.veil()[0], look.veil()[1], look.veil()[2]);
        ground.safeGetUniform("Light").set(look.light());
        ground.safeGetUniform("SunDir").set(look.sun()[0], look.sun()[1], look.sun()[2]);
        ground.safeGetUniform("Sunlit").set((float) climb.sunlit());
        ground.safeGetUniform("Opacity").set((float) climb.ground());
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        if (screen == null) {
            screen = new VertexBuffer(VertexBuffer.Usage.STATIC);
            final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                    DefaultVertexFormat.POSITION);
            builder.addVertex(-1F, -1F, 0F);
            builder.addVertex(1F, -1F, 0F);
            builder.addVertex(1F, 1F, 0F);
            builder.addVertex(-1F, 1F, 0F);
            screen.bind();
            screen.upload(builder.buildOrThrow());
            VertexBuffer.unbind();
        }
        screen.bind();
        screen.drawWithShader(new Matrix4f(), new Matrix4f(), ground);
        VertexBuffer.unbind();
    }

    /* The game's clouds over the far ground: where the eye stands in their map, their height, colour and drift. */
    private static void clouds(final ShaderInstance ground, final ClientLevel level, final Vec3 eye, final Look look,
                               final boolean covered) {
        final float height = level.effects().getCloudHeight();
        final boolean shown = !Float.isNaN(height)
                && Minecraft.getInstance().options.getCloudsType() != CloudStatus.OFF;
        final double drift = (Minecraft.getInstance().levelRenderer.getTicks() + look.partialTick()) * CLOUD_DRIFT;
        final double u = (eye.x + drift) / CLOUD_TEXEL;
        final double v = eye.z / CLOUD_TEXEL + CLOUD_V;
        ground.safeGetUniform("Clouds").set((float) (u - Math.floor(u / CLOUD_MAP) * CLOUD_MAP),
                (float) (v - Math.floor(v / CLOUD_MAP) * CLOUD_MAP),
                (float) (height + CLOUD_TOP - sea(level)), shown ? 1F : 0F);
        final Vec3 colour = level.getCloudColor(look.partialTick());
        ground.safeGetUniform("CloudColour").set((float) colour.x, (float) colour.y, (float) colour.z,
                CLOUD_SOLID);
        ground.safeGetUniform("CloudSkip").set(covered ? 0F : 1F);
    }

    /* The game's cloud map, its copies the mean of the texels under them, read once. */
    private static GroundTexture cloudMap() {
        if (clouds == null) {
            final Resource resource = Minecraft.getInstance().getResourceManager().getResource(CLOUDS).orElse(null);
            NativeImage image = null;
            if (resource != null) {
                try (InputStream in = resource.open()) {
                    image = NativeImage.read(NativeImage.Format.RGBA, in);
                } catch (final IOException unreadable) {
                    image = null;
                }
            }
            clouds = GroundTexture.cover(image != null ? image : new NativeImage(1, 1, true));
        }
        return clouds;
    }

    private static void bind(final int pair, @Nullable final Ring ring) {
        final int colour = ring != null ? ring.colours().getId() : cloudMap().getId();
        final int height = ring != null ? ring.heights().getId() : cloudMap().getId();
        RenderSystem.setShaderTexture(pair * 2, colour);
        RenderSystem.setShaderTexture(pair * 2 + 1, height);
    }

    private static void ring(final ShaderInstance ground, final String name, @Nullable final Ring ring,
                             final Vec3 eye) {
        if (ring == null) {
            ground.safeGetUniform(name).set(0F, 0F, 1F, 0F);
            return;
        }
        ground.safeGetUniform(name).set((float) (eye.x - ring.centreX()), (float) (eye.z - ring.centreZ()),
                (float) ring.size() * ring.blocks(), 1F);
    }

    /* The rings are of the dimension the climber is in; any of another are let go. */
    private static void follow(final ClientLevel level) {
        if (!level.dimension().equals(drawnFor)) {
            clear();
            drawnFor = level.dimension();
        }
    }

    private static GroundColours painter() {
        if (colours == null) {
            colours = new GroundColours();
        }
        return colours;
    }

    /**
     * How the far ground looks this frame.
     *
     * @param partialTick   the share of the next tick gone
     * @param light         the day's light on the blocks
     * @param haze          the haze's colour, as the game's fog
     * @param hazeEnd       how far off the haze is whole, in blocks
     * @param veil          the colour of the veil the real ground melts under
     * @param veilStrength  how much the veil hides at its thickest
     * @param sun           the direction of the Sun of space
     */
    record Look(float partialTick, float light, float[] haze, float hazeEnd, float[] veil, float veilStrength,
                float[] sun) {

        Look {
            haze = haze.clone();
            veil = veil.clone();
            sun = sun.clone();
        }
    }

    /* A map of the ground on the card, its middle over a block and its pixels so many blocks wide. */
    private record Ring(GroundTexture colours, GroundTexture heights, int centreX, int centreZ, int blocks,
                        int size) {

        void release() {
            this.colours.releaseId();
            this.heights.releaseId();
        }
    }

    /* The loaded blocks being read into their map, chunk by chunk. */
    private static final class Gathering {

        private final int firstChunkX;
        private final int firstChunkZ;
        private final int chunks;
        private final int[] colours;
        private final byte[] heights;
        private final boolean[] known;
        private final boolean[] wet;
        private int next;

        private Gathering(final int firstChunkX, final int firstChunkZ, final int chunks) {
            this.firstChunkX = firstChunkX;
            this.firstChunkZ = firstChunkZ;
            this.chunks = chunks;
            final int size = chunks * CHUNK;
            this.colours = new int[size * size];
            this.heights = new byte[size * size];
            this.known = new boolean[size * size];
            this.wet = new boolean[size * size];
        }

        private void read(final ClientLevel level, final LevelChunk chunk, final GroundColours painter) {
            final int size = this.chunks * CHUNK;
            final int baseX = (chunk.getPos().x - this.firstChunkX) * CHUNK;
            final int baseZ = (chunk.getPos().z - this.firstChunkZ) * CHUNK;
            final BlockPos.MutableBlockPos top = new BlockPos.MutableBlockPos();
            for (int z = 0; z < CHUNK; z++) {
                for (int x = 0; x < CHUNK; x++) {
                    final int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    top.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                    final int at = (baseZ + z) * size + baseX + x;
                    final BlockState state = chunk.getBlockState(top);
                    this.colours[at] = painter.ofBlock(level, top, state);
                    this.heights[at] = FarMapPayload.height(y);
                    this.known[at] = true;
                    this.wet[at] = state.getFluidState().is(FluidTags.WATER);
                }
            }
        }

        private Ring ring() {
            final int size = this.chunks * CHUNK;
            final NativeImage colour = new NativeImage(size, size, false);
            final NativeImage height = new NativeImage(size, size, false);
            for (int row = 0; row < size; row++) {
                for (int column = 0; column < size; column++) {
                    final int at = row * size + column;
                    final int solid = !this.known[at] ? 0 : this.wet[at] ? WATER : FULL;
                    colour.setPixelRGBA(column, row, FastColor.ABGR32.fromArgb32(
                            FastColor.ARGB32.color(solid, this.colours[at])));
                    height.setPixelRGBA(column, row, FastColor.ABGR32.color(FULL, 0, 0, this.heights[at] & FULL));
                }
            }
            return new Ring(GroundTexture.colours(colour), GroundTexture.plain(height),
                    this.firstChunkX * CHUNK + size / 2, this.firstChunkZ * CHUNK + size / 2, 1, size);
        }
    }
}
