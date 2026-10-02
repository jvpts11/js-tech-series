/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.tier.HardwareEra;

/**
 * The Supercomputer Rack: the same 8U cabinet as the Server Rack (units, front slots, KVM, the
 * physical GUI), but it seats only Supercomputer Nodes, and its rear port speaks only the high-compute
 * fabric. A supercomputer is every such rack tied together by that fabric behind one HBW Interface;
 * the interface is what joins the data network, so this cabinet never carries a data-cable tier. Like
 * a Server Rack it has an era, given where it is declared, and seats nodes of its own era or earlier.
 */
public class SupercomputerRackBlock extends ServerRackBlock {

    public static final MapCodec<SupercomputerRackBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(SupercomputerRackBlock::era)
    ).apply(i, SupercomputerRackBlock::new));
    /*
     * Only the compute fabric reaches a supercomputer cabinet. A data cable on this port would put the nodes on the
     * data network directly, which is exactly what the HBW Interface exists to prevent.
     */
    private static final FacePorts PORTS = FacePorts.builder()
            .port(FaceRule.BACK, DataLines.of(DataTier.HPC))
            .build();

    public SupercomputerRackBlock(final Properties properties, final HardwareEra era) {
        super(properties, era);
    }

    @Override
    protected MapCodec<SupercomputerRackBlock> codec() {
        return CODEC;
    }

    @Override
    public RackChassis.RackType rackType() {
        return RackChassis.RackType.SUPERCOMPUTER;
    }

    @Override
    public FacePorts ports() {
        return PORTS;
    }
}
