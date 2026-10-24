/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.multiblock.PatternById;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The terminal of a multiblock machine declared as data: a processing machine whose slots, lanes, tanks and energy
 * are its machine file's, made as it is placed or loaded, which forms as the file's shape says and works the file's
 * kind of recipe, as many at once as the file says. A terminal whose block names no file the game knows never forms,
 * and says so.
 */
public class DataMachineBlockEntity extends ProcessingMachineBlockEntity {

    private final ResourceLocation id;
    private final @Nullable DataMachine machine;

    /* The slots of a terminal whose file is missing: it never forms, so they never fill. */
    private static final Layout MISSING = Layout.items(1, 1);

    public DataMachineBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state, machineOf(state).map(DataMachine::layout).orElse(MISSING),
                MachineEnergy.fe(machineOf(state).map(DataMachine::energy).orElse(0),
                        machineOf(state).map(DataMachine::receive).orElse(0), 0),
                machineOf(state).map(DataMachine::perTick).orElse(0L));
        this.id = idOf(state);
        this.machine = machineOf(state).orElse(null);
        formsWith(this.machine == null ? MachineStructure.of(new PatternById(this.id))
                : MachineStructure.of(this.machine.shape()).parallel(this.machine.parallel().reader()));
    }

    /** The id of its machine file. */
    public ResourceLocation machineId() {
        return this.id;
    }

    /** Its machine, as its file was when it was placed or loaded; none when the game knows no such file. */
    public Optional<DataMachine> machine() {
        return Optional.ofNullable(this.machine);
    }

    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        if (this.machine == null) {
            return Optional.empty();
        }
        return DataMachines.kind(this.machine.recipes()).flatMap(kind -> byKind(level, kind, input));
    }

    private static ResourceLocation idOf(final BlockState state) {
        if (state.getBlock() instanceof DataMachineBlock block) {
            return block.machine();
        }
        throw new IllegalStateException("a data machine's block entity stands on a block that names no machine");
    }

    private static Optional<DataMachine> machineOf(final BlockState state) {
        return DataMachines.get(idOf(state));
    }
}
