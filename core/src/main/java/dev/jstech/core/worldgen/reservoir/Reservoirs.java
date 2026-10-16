/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.reservoir;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.region.Box;
import dev.jstech.core.region.RegionIndex;
import dev.jstech.core.region.SpatialIndex;
import dev.jstech.core.worldgen.vein.VeinField;
import dev.jstech.core.worldgen.vein.Veins;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * Oil, gas and brines as physical things in the ground: each kind of reservoir declared in code by its mod, or a file a
 * data pack writes over or adds; where each reservoir lies worked out from the world's seed, in zones eight chunks a
 * side; and what has been drawn from each kept in the dimension's data over areas, so every mod reads the same
 * reservoirs and the same blocks left in them. A well drains a reservoir a block at a time, and a block that holds no
 * more turns back into plain rock.
 *
 * <pre>{@code
 * public static final ResourceLocation OIL = Reservoirs.reservoir(rl("oil")).fluid(MyFluids.CRUDE_OIL.getId())
 *         .rocks(rl("oil_sand"), ResourceLocation.withDefaultNamespace("sandstone"),
 *                 ResourceLocation.withDefaultNamespace("smooth_basalt"))
 *         .perBlock(1000).heights(-40, 20).frequency(0.25).size(28, 5).biomes(Tags.Biomes.IS_DESERT).declare();
 *
 * Optional<Reservoirs.Held> held = Reservoirs.at(level, pos);           // what a block of one holds
 * int drained = Reservoirs.drain(level, pos, 250, false);               // millibuckets taken
 * }</pre>
 */
public final class Reservoirs {

    /** A reservoir zone's side, in blocks: eight chunks. */
    public static final int ZONE = 128;
    /** The kinds the data packs say, by id. */
    public static final DataRegistry<ReservoirType> FILES = DataRegistry.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "reservoirs"), "jstech/reservoirs",
            ReservoirType.CODEC).register();
    /** What has been drawn from each reservoir, by its key, over its box. */
    public static final RegionIndex<Drawn> DRAWN = RegionIndex.declare(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "reservoirs_drawn"), Drawn.CODEC);

    private static final long SALT = -3_255_812_049_006_331_667L;
    private static final int UPDATE = 3;
    private static final Map<ResourceLocation, ReservoirType> DECLARED = new ConcurrentHashMap<>();
    private static volatile Known known = new Known(-1, -1, List.of());

    private Reservoirs() {
    }

    /** Declares the registry of the files, from the Core's constructor. */
    public static void register() {
        // Loading the class declares it.
    }

    /** Starts declaring the kind of reservoir {@code id}. */
    public static ReservoirBuilder reservoir(final ResourceLocation id) {
        return new ReservoirBuilder(id);
    }

    /** Every kind declared in code, by id. */
    public static Map<ResourceLocation, ReservoirType> declared() {
        return Map.copyOf(DECLARED);
    }

    /** Every kind of reservoir the server lays, by id in order: the files', then the declared ones no file replaces. */
    public static List<Kind> all() {
        final Known now = known;
        final long files = FILES.version();
        if (now.files() == files && now.declared() == DECLARED.size()) {
            return now.kinds();
        }
        final Map<ResourceLocation, Kind> byId = new TreeMap<>();
        DECLARED.forEach((id, type) -> byId.put(id, new Kind(id, type)));
        FILES.entries().forEach((id, type) -> byId.put(id, new Kind(id, type)));
        final List<Kind> kinds = List.copyOf(byId.values());
        known = new Known(files, DECLARED.size(), kinds);
        return kinds;
    }

    /** Where the reservoirs of {@code kinds} lie. */
    public static VeinField field(final List<Kind> kinds) {
        final List<VeinField.Kind> placed = new ArrayList<>(kinds.size());
        kinds.forEach(kind -> placed.add(kind.type().kind()));
        return new VeinField(placed, ZONE, SALT);
    }

    /**
     * Every reservoir found where it lies whose lens reaches the columns from {@code x0, z0} to {@code x1, z1} of the
     * world {@code generator} makes.
     */
    public static List<Reservoir> reaching(final long seed, final ChunkGenerator generator, final RandomState random,
                                           final int x0, final int z0, final int x1, final int z1) {
        final List<Kind> kinds = all();
        final List<Reservoir> found = new ArrayList<>();
        for (final VeinField.Vein body : field(kinds).reaching(seed, x0, z0, x1, z1)) {
            final Kind kind = kinds.get(body.kind());
            if (Veins.found(generator, random, kind.type().biomes(), kind.type().elsewhere(), body)) {
                found.add(new Reservoir(kind, body));
            }
        }
        return found;
    }

    /** The reservoirs found where they lie within {@code radius} blocks across of {@code pos}, nearest first. */
    public static List<Reservoir> near(final ServerLevel level, final BlockPos pos, final int radius) {
        final List<Reservoir> found = new ArrayList<>(reaching(level.getSeed(), level.getChunkSource().getGenerator(),
                level.getChunkSource().randomState(), pos.getX() - radius, pos.getZ() - radius, pos.getX() + radius,
                pos.getZ() + radius));
        found.removeIf(reservoir -> Math.hypot(reservoir.body().x() - pos.getX(), reservoir.body().z() - pos.getZ())
                > radius + reservoir.body().radius());
        found.sort((a, b) -> Double.compare(Math.hypot(a.body().x() - pos.getX(), a.body().z() - pos.getZ()),
                Math.hypot(b.body().x() - pos.getX(), b.body().z() - pos.getZ())));
        return found;
    }

    /** What the block at {@code pos} holds, when it is a block of a reservoir's soaked rock; none otherwise. */
    public static Optional<Held> at(final ServerLevel level, final BlockPos pos) {
        for (final Reservoir reservoir : reaching(level.getSeed(), level.getChunkSource().getGenerator(),
                level.getChunkSource().randomState(), pos.getX(), pos.getZ(), pos.getX(), pos.getZ())) {
            if (reservoir.inLens(pos) && level.getBlockState(pos).is(reservoir.soaked())) {
                final SpatialIndex.Entry<String, Drawn> drawn = DRAWN.get(level, reservoir.key());
                final int left = drawn == null ? reservoir.kind().type().perBlock()
                        : drawn.value().leftIn(pos, reservoir.kind().type().perBlock());
                return Optional.of(new Held(reservoir, left));
            }
        }
        return Optional.empty();
    }

    /**
     * Draws up to {@code amount} millibuckets from the block of a reservoir at {@code pos}, and gives how much it drew;
     * the block turns into its plain rock once it holds no more. Only {@code simulate}d, nothing changes.
     */
    public static int drain(final ServerLevel level, final BlockPos pos, final int amount, final boolean simulate) {
        final Optional<Held> held = at(level, pos);
        if (held.isEmpty() || amount <= 0) {
            return 0;
        }
        final int drawn = Math.min(amount, held.get().left());
        if (simulate || drawn == 0) {
            return drawn;
        }
        final Reservoir reservoir = held.get().reservoir();
        final SpatialIndex.Entry<String, Drawn> before = DRAWN.get(level, reservoir.key());
        final Drawn was = before == null ? Drawn.NONE : before.value();
        final int left = held.get().left() - drawn;
        DRAWN.put(level, reservoir.key(), reservoir.box(), was.after(pos, left, drawn));
        if (left == 0) {
            level.setBlock(pos, reservoir.drained().defaultBlockState(), UPDATE);
        }
        return drawn;
    }

    /**
     * The block of {@code reservoir} still holding something nearest {@code from}, among the loaded blocks, for a well
     * to drain next; none when its loaded blocks are all drained.
     */
    public static Optional<BlockPos> nearestHolding(final ServerLevel level, final Reservoir reservoir,
                                                    final BlockPos from) {
        final Box box = reservoir.box();
        BlockPos best = null;
        double nearest = Double.MAX_VALUE;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    at.set(x, y, z);
                    final double distance = at.distSqr(from);
                    if (distance < nearest && reservoir.inLens(at) && level.getBlockState(at).is(reservoir.soaked())) {
                        nearest = distance;
                        best = at.immutable();
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** About how much is left in {@code reservoir}, in millibuckets: what its lens held, less what was drawn. */
    public static long left(final ServerLevel level, final Reservoir reservoir) {
        final SpatialIndex.Entry<String, Drawn> drawn = DRAWN.get(level, reservoir.key());
        return Math.max(0, reservoir.held() - (drawn == null ? 0 : drawn.value().total()));
    }

    /**
     * A kind of reservoir the server lays.
     *
     * @param id   its id
     * @param type what it is
     */
    public record Kind(ResourceLocation id, ReservoirType type) {
    }

    /**
     * A reservoir where it lies.
     *
     * @param kind its kind
     * @param body its lens: its middle, its radius across and its thickness up and down
     */
    public record Reservoir(Kind kind, VeinField.Vein body) {

        /** The key its drawn blocks are kept under: its kind and its middle. */
        public String key() {
            return this.kind.id() + "@" + this.body.x() + "," + this.body.y() + "," + this.body.z();
        }

        /** The box its lens and its cap lie in. */
        public Box box() {
            final int across = (int) Math.ceil(this.body.radius());
            final int up = (int) Math.ceil(this.body.thickness()) + this.kind.type().capping();
            return Box.between(this.body.x() - across, this.body.y() - up, this.body.z() - across,
                    this.body.x() + across, this.body.y() + up, this.body.z() + across);
        }

        /** Whether {@code pos} lies in its lens, where its soaked rock is. */
        public boolean inLens(final BlockPos pos) {
            return this.body.share(pos.getX(), pos.getY(), pos.getZ()) > 0;
        }

        /** About how much its lens holds, full, in millibuckets: the lens's volume by what each block holds. */
        public long held() {
            final double volume = 4.0 / 3.0 * Math.PI * this.body.radius() * this.body.radius() * this.body.thickness();
            return Math.round(volume) * this.kind.type().perBlock();
        }

        /** The liquid or gas it holds; empty when the game has no fluid of that id. */
        public Fluid fluid() {
            return BuiltInRegistries.FLUID.get(this.kind.type().fluid());
        }

        public Block soaked() {
            return BuiltInRegistries.BLOCK.get(this.kind.type().soaked());
        }

        public Block drained() {
            return BuiltInRegistries.BLOCK.get(this.kind.type().drained());
        }

        public Block cap() {
            return BuiltInRegistries.BLOCK.get(this.kind.type().cap());
        }
    }

    /**
     * A block of a reservoir.
     *
     * @param reservoir the reservoir
     * @param left      how much it holds now, in millibuckets
     */
    public record Held(Reservoir reservoir, int left) {
    }

    /**
     * What has been drawn from a reservoir: how much each block it was drawn from still holds, and how much was drawn
     * in all; a block drained to nothing is rock again and leaves the list.
     *
     * @param left  how much each block drawn from still holds, by its packed place
     * @param total how much was drawn in all, in millibuckets
     */
    public record Drawn(Map<String, Integer> left, long total) {

        public static final Codec<Drawn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("left").forGetter(Drawn::left),
                Codec.LONG.fieldOf("total").forGetter(Drawn::total))
                .apply(instance, Drawn::new));
        static final Drawn NONE = new Drawn(Map.of(), 0);

        public Drawn {
            left = Map.copyOf(left);
        }

        /** How much the block at {@code pos} holds, {@code full} when nothing was drawn from it. */
        public int leftIn(final BlockPos pos, final int full) {
            return this.left.getOrDefault(Long.toString(pos.asLong()), full);
        }

        Drawn after(final BlockPos pos, final int nowLeft, final int drawn) {
            final Map<String, Integer> next = new HashMap<>(this.left);
            if (nowLeft == 0) {
                next.remove(Long.toString(pos.asLong()));
            } else {
                next.put(Long.toString(pos.asLong()), nowLeft);
            }
            return new Drawn(next, this.total + drawn);
        }
    }

    /* The kinds as last worked out, and what they were worked out from. */
    private record Known(long files, int declared, List<Kind> kinds) {
    }

    /** A kind of reservoir being declared; it starts as a reservoir of the overworld's stone, a bucket a block. */
    public static final class ReservoirBuilder {

        private final ResourceLocation id;
        private @Nullable ResourceLocation fluid;
        private @Nullable ResourceLocation soaked;
        private @Nullable ResourceLocation drained;
        private @Nullable ResourceLocation cap;
        private int perBlock = 1000;
        private int minY = -40;
        private int maxY = 30;
        private double frequency = 0.2;
        private double radius = 24;
        private double thickness = 4;
        private int capping = 2;
        private @Nullable TagKey<Biome> biomes;
        private double elsewhere;
        private TagKey<Block> replaces = BlockTags.BASE_STONE_OVERWORLD;

        private ReservoirBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /** The liquid or gas it holds, by its id. */
        public ReservoirBuilder fluid(final ResourceLocation held) {
            this.fluid = held;
            return this;
        }

        /** Its soaked rock, the plain rock that becomes once drained, and the cap rock over it, by their ids. */
        public ReservoirBuilder rocks(final ResourceLocation soakedRock, final ResourceLocation plainRock,
                                      final ResourceLocation capRock) {
            this.soaked = soakedRock;
            this.drained = plainRock;
            this.cap = capRock;
            return this;
        }

        /** Each block holds {@code millibuckets}. */
        public ReservoirBuilder perBlock(final int millibuckets) {
            this.perBlock = millibuckets;
            return this;
        }

        public ReservoirBuilder heights(final int lowest, final int highest) {
            this.minY = lowest;
            this.maxY = highest;
            return this;
        }

        /** How many zones of eight chunks a side hold one, 0 to 1. */
        public ReservoirBuilder frequency(final double share) {
            this.frequency = share;
            return this;
        }

        /** Its lens reaches {@code across} blocks from its middle, and {@code upAndDown} up and down. */
        public ReservoirBuilder size(final double across, final double upAndDown) {
            this.radius = across;
            this.thickness = upAndDown;
            return this;
        }

        /** Its cap is {@code blocks} thick. */
        public ReservoirBuilder capping(final int blocks) {
            this.capping = blocks;
            return this;
        }

        public ReservoirBuilder biomes(final TagKey<Biome> where) {
            this.biomes = where;
            return this;
        }

        public ReservoirBuilder elsewhere(final double share) {
            this.elsewhere = share;
            return this;
        }

        /** It takes the place of {@code rock} rather than the overworld's stone. */
        public ReservoirBuilder replaces(final TagKey<Block> rock) {
            this.replaces = rock;
            return this;
        }

        /**
         * Declares the kind, and gives its id.
         *
         * @throws IllegalStateException when it names no fluid or rocks, or its numbers make no reservoir
         */
        public ResourceLocation declare() {
            if (fluid == null || soaked == null || drained == null || cap == null) {
                throw new IllegalStateException("the reservoir " + id + " names its fluid and its rocks");
            }
            if (perBlock < 1 || frequency < 0 || frequency > 1 || radius < 2 || thickness < 1 || capping < 0) {
                throw new IllegalStateException("the reservoir " + id + " has an amount, a frequency and a size");
            }
            DECLARED.put(id, new ReservoirType(fluid, soaked, drained, cap, perBlock, minY, maxY, frequency, radius,
                    thickness, capping, Optional.ofNullable(biomes), elsewhere, replaces));
            return id;
        }
    }
}
