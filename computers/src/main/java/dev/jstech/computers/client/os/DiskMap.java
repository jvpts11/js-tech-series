/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.DiskReport;
import java.util.ArrayList;
import java.util.List;

/**
 * A disk as the disk tools draw it across their maps: its partitions and its stretches of unallocated space in the
 * order they lie, each a block as wide as its share of the disk, none narrower than its name needs.
 */
final class DiskMap {

    private DiskMap() {
    }

    /** The partitions and the unallocated stretches of {@code disk} in the order they lie, as {start, size, number}. */
    static List<long[]> spans(final DiskReport.Disk disk) {
        final List<long[]> spans = new ArrayList<>();
        for (final DiskReport.Part part : disk.parts()) {
            spans.add(new long[]{part.startMb(), part.sizeMb(), part.number()});
        }
        for (final DiskLayout.FreeSpace gap : disk.layout().free()) {
            spans.add(new long[]{gap.startMb(), gap.sizeMb(), -1L});
        }
        spans.sort((a, b) -> Long.compare(a[0], b[0]));
        return spans;
    }

    /**
     * Where each block starts across {@code width}, by its size on a disk of {@code diskMb}, none narrower than
     * {@code narrowest}; one more edge closes the last block.
     */
    static int[] edges(final List<long[]> spans, final long diskMb, final int width, final int narrowest) {
        final int[] edges = new int[spans.size() + 1];
        final int room = Math.max(0, width - narrowest * spans.size());
        int at = 0;
        for (int i = 0; i < spans.size(); i++) {
            edges[i] = at;
            at += narrowest + (int) (room * spans.get(i)[1] / Math.max(1L, diskMb));
        }
        edges[spans.size()] = Math.min(width, Math.max(at, width));
        return edges;
    }
}
