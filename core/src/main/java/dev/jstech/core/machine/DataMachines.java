/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.multiblock.BoxShape;
import dev.jstech.core.multiblock.CuboidShape;
import dev.jstech.core.multiblock.IStructureShape;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.PatternById;
import dev.jstech.core.multiblock.Projectables;
import dev.jstech.core.multiblock.ShapeCodecs;
import dev.jstech.core.multiblock.WallsShape;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The multiblock machines data packs declare, by id: each file under {@code data/<namespace>/multiblock_machine/}
 * is one {@link DataMachine}, read whenever the server reloads its data and sent to every player, so a controller
 * block built on {@link DataMachineBlock} reads the same machine on both sides. A file:
 *
 * <pre>{@code
 * {"shape": {"box": {"frame": {"blocks": ["minecraft:iron_block"]},
 *                    "face": {"blocks": ["minecraft:glass"], "tags": ["jscore:multiblock_ports"]},
 *                    "sides": {"least": 3, "most": 7}, "height": {"least": 3, "most": 7}}},
 *  "recipes": "mymod:baking",
 *  "layout": {"inputs": 1, "outputs": 1, "lanes": 2},
 *  "energy": {"capacity": 10000, "receive": 1000, "per_tick": 10},
 *  "parallel": {"by": "count", "name": "unit"}}
 * }</pre>
 *
 * <p>The shape is one of {@code "pattern"} (a pattern's id), {@code "drawn"} (a pattern written in the file),
 * {@code "box"}, {@code "walls"} or {@code "cuboid"}. A machine's slots are made as its controller is placed or loaded,
 * so a change to its layout reaches the controllers placed or loaded after the reload; its shape and its recipes, at
 * once.
 */
public final class DataMachines {

    /** A shape as a file holds it: exactly one of a pattern's id, a pattern drawn, a box, walls or a cuboid. */
    public static final Codec<IStructureShape> SHAPE = RecordCodecBuilder.<RawShape>create(instance -> instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("pattern").forGetter(RawShape::pattern),
                    MultiblockPatterns.CODEC.optionalFieldOf("drawn").forGetter(RawShape::drawn),
                    ShapeCodecs.BOX.optionalFieldOf("box").forGetter(RawShape::box),
                    ShapeCodecs.WALLS.optionalFieldOf("walls").forGetter(RawShape::walls),
                    ShapeCodecs.CUBOID.optionalFieldOf("cuboid").forGetter(RawShape::cuboid))
            .apply(instance, RawShape::new))
            .flatXmap(DataMachines::shape, DataMachines::rawShape);

    /** A machine's slots, lanes and tanks; what a file leaves out it has none of, and one lane. */
    public static final Codec<ProcessingMachineBlockEntity.Layout> LAYOUT = RecordCodecBuilder.<RawLayout>create(
                    instance -> instance.group(
                            Codec.INT.optionalFieldOf("inputs", 0).forGetter(RawLayout::inputs),
                            Codec.INT.optionalFieldOf("outputs", 0).forGetter(RawLayout::outputs),
                            Codec.INT.optionalFieldOf("upgrades", 0).forGetter(RawLayout::upgrades),
                            Codec.INT.optionalFieldOf("input_tanks", 0).forGetter(RawLayout::inputTanks),
                            Codec.INT.optionalFieldOf("output_tanks", 0).forGetter(RawLayout::outputTanks),
                            Codec.INT.optionalFieldOf("tank_capacity", 0).forGetter(RawLayout::tankCapacity),
                            RecordCodecBuilder.<MatterTanks.MatterSpec>create(matter -> matter.group(
                                            MaterialProperties.STATE_CODEC.fieldOf("state")
                                                    .forGetter(MatterTanks.MatterSpec::state),
                                            Codec.INT.optionalFieldOf("inputs", 0)
                                                    .forGetter(MatterTanks.MatterSpec::inputs),
                                            Codec.INT.optionalFieldOf("outputs", 0)
                                                    .forGetter(MatterTanks.MatterSpec::outputs),
                                            Codec.LONG.fieldOf("volume").forGetter(MatterTanks.MatterSpec::volume),
                                            Codec.LONG.optionalFieldOf("pressure", 1L)
                                                    .forGetter(MatterTanks.MatterSpec::pressure))
                                    .apply(matter, MatterTanks.MatterSpec::new)).listOf()
                                    .optionalFieldOf("matter", List.of()).forGetter(RawLayout::matter),
                            Codec.INT.optionalFieldOf("lanes", 1).forGetter(RawLayout::lanes))
                            .apply(instance, RawLayout::new))
            .flatXmap(raw -> attempt(() -> new ProcessingMachineBlockEntity.Layout(raw.inputs(), raw.outputs(),
                            raw.upgrades(), raw.inputTanks(), raw.outputTanks(), raw.tankCapacity(), raw.matter(),
                            raw.lanes())),
                    layout -> DataResult.success(new RawLayout(layout.inputs(), layout.outputs(), layout.upgrades(),
                            layout.inputTanks(), layout.outputTanks(), layout.tankCapacity(), layout.matter(),
                            layout.lanes())));

    /** How many recipes at once, and from what. */
    public static final Codec<DataMachine.Parallel> PARALLEL = RecordCodecBuilder.<RawParallel>create(
                    instance -> instance.group(
                            Codec.STRING.fieldOf("by").forGetter(RawParallel::by),
                            Codec.STRING.optionalFieldOf("name", "").forGetter(RawParallel::name),
                            Codec.INT.optionalFieldOf("value", 1).forGetter(RawParallel::fixed))
                            .apply(instance, RawParallel::new))
            .flatXmap(raw -> attempt(() -> new DataMachine.Parallel(raw.by(), raw.name(), raw.fixed())),
                    parallel -> DataResult.success(new RawParallel(parallel.by(), parallel.name(), parallel.fixed())));

    /** A data machine as its file holds it. */
    public static final Codec<DataMachine> CODEC = RecordCodecBuilder.<RawMachine>create(instance -> instance.group(
                    SHAPE.fieldOf("shape").forGetter(RawMachine::shape),
                    ResourceLocation.CODEC.fieldOf("recipes").forGetter(RawMachine::recipes),
                    LAYOUT.fieldOf("layout").forGetter(RawMachine::layout),
                    RecordCodecBuilder.<RawEnergy>create(energy -> energy.group(
                                    Codec.INT.optionalFieldOf("capacity", 0).forGetter(RawEnergy::capacity),
                                    Codec.INT.optionalFieldOf("receive", 0).forGetter(RawEnergy::receive),
                                    Codec.LONG.optionalFieldOf("per_tick", 0L).forGetter(RawEnergy::perTick))
                            .apply(energy, RawEnergy::new)).optionalFieldOf("energy", new RawEnergy(0, 0, 0L))
                            .forGetter(RawMachine::energy),
                    PARALLEL.optionalFieldOf("parallel", DataMachine.Parallel.ONE).forGetter(RawMachine::parallel))
            .apply(instance, RawMachine::new))
            .flatXmap(raw -> attempt(() -> new DataMachine(raw.shape(), raw.recipes(), raw.layout(),
                            raw.energy().capacity(), raw.energy().receive(), raw.energy().perTick(), raw.parallel())),
                    machine -> DataResult.success(new RawMachine(machine.shape(), machine.recipes(), machine.layout(),
                            new RawEnergy(machine.energy(), machine.receive(), machine.perTick()),
                            machine.parallel())));

    private static final DataRegistry<DataMachine> FILES = DataRegistry.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "multiblock_machine"), "multiblock_machine", CODEC)
            .synced().register();

    static {
        // A machine whose shape names a pattern by id is shown as that pattern; the others are shown as themselves.
        Projectables.addSource(DataMachines::shown);
    }

    private DataMachines() {
    }

    /** Loads the registry of machine files, from the Core's constructor. */
    public static void register() {
        // The registry is made when this class loads; loading it while the game starts is all this does.
    }

    /** The machine of the file {@code id}: the server's, or on a player's game, what the server sent. */
    public static Optional<DataMachine> get(final ResourceLocation id) {
        final Optional<DataMachine> server = FILES.get(id);
        return server.isPresent() ? server : Optional.ofNullable(FILES.clientEntries().get(id));
    }

    /** The kind of processing machine of the recipe type {@code id}, as some mod declared it. */
    public static Optional<ProcessingKind> kind(final ResourceLocation id) {
        for (final ModContent content : ModContent.all()) {
            for (final ProcessingKind kind : content.declaredProcessing()) {
                if (kind.id().equals(id)) {
                    return Optional.of(kind);
                }
            }
        }
        return Optional.empty();
    }

    /* The machines shown before they are built, by id, on either side: those whose shape is their own. */
    private static Map<ResourceLocation, IStructureShape> shown() {
        final Map<ResourceLocation, IStructureShape> out = new TreeMap<>();
        FILES.clientEntries().forEach((id, machine) -> out.put(id, machine.shape()));
        FILES.entries().forEach((id, machine) -> out.put(id, machine.shape()));
        out.values().removeIf(shape -> shape instanceof PatternById);
        return out;
    }

    private static DataResult<IStructureShape> shape(final RawShape raw) {
        final List<Optional<? extends IStructureShape>> given = List.of(raw.pattern().map(PatternById::new),
                raw.drawn(), raw.box(), raw.walls(), raw.cuboid());
        final List<IStructureShape> present = given.stream().<IStructureShape>flatMap(Optional::stream).toList();
        return present.size() == 1 ? DataResult.success(present.getFirst())
                : DataResult.error(() -> "a machine's shape is one of pattern, drawn, box, walls or cuboid, not "
                        + present.size());
    }

    private static DataResult<RawShape> rawShape(final IStructureShape shape) {
        return switch (shape) {
            case PatternById byId -> DataResult.success(new RawShape(Optional.of(byId.id()), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty()));
            case MultiblockPattern drawn -> DataResult.success(new RawShape(Optional.empty(), Optional.of(drawn),
                    Optional.empty(), Optional.empty(), Optional.empty()));
            case BoxShape box -> DataResult.success(new RawShape(Optional.empty(), Optional.empty(),
                    Optional.of(box), Optional.empty(), Optional.empty()));
            case WallsShape walls -> DataResult.success(new RawShape(Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.of(walls), Optional.empty()));
            case CuboidShape cuboid -> DataResult.success(new RawShape(Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.of(cuboid)));
            default -> DataResult.error(() -> "a shape made in code cannot be written to a file");
        };
    }

    /* The value made, or the reason it cannot be, as a decoding error. */
    private static <T> DataResult<T> attempt(final IMaker<T> maker) {
        try {
            return DataResult.success(maker.make());
        } catch (final IllegalArgumentException | IllegalStateException wrong) {
            return DataResult.error(wrong::getMessage);
        }
    }

    @FunctionalInterface
    private interface IMaker<T> {
        T make();
    }

    private record RawShape(Optional<ResourceLocation> pattern, Optional<MultiblockPattern> drawn,
                            Optional<BoxShape> box, Optional<WallsShape> walls, Optional<CuboidShape> cuboid) {
    }

    private record RawLayout(int inputs, int outputs, int upgrades, int inputTanks, int outputTanks, int tankCapacity,
                             List<MatterTanks.MatterSpec> matter, int lanes) {
    }

    private record RawEnergy(int capacity, int receive, long perTick) {
    }

    private record RawMachine(IStructureShape shape, ResourceLocation recipes,
                              ProcessingMachineBlockEntity.Layout layout, RawEnergy energy,
                              DataMachine.Parallel parallel) {
    }

    private record RawParallel(String by, String name, int fixed) {
    }
}
