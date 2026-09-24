/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.install.voice.DiskVoices;
import dev.jstech.computers.program.install.voice.Ext4Figures;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The disks of a machine being installed by hand: how they are partitioned, what has a filesystem on it, and
 * what is mounted where.
 *
 * <p>Everything here is refused in the order a real machine refuses it. A disk somebody partitioned is not
 * written over whole, the partition the firmware reads is not given the wrong filesystem, nothing is mounted
 * that has not been made, and the place the bootloader goes hangs inside the new system, so it is mounted
 * after the root is and under it.
 */
@TextHolder
final class LiveDisks {

    /**
     * The partitions written onto each disk, by the disk's name. A disk with none is a disk nobody has
     * partitioned, which a machine of the older firmware can still be installed onto whole.
     */
    private final Map<String, List<Partition>> tables = new LinkedHashMap<>();

    /** The places of the running medium bound into the new system before stepping into it. */
    private final Set<String> binds = new LinkedHashSet<>();

    /** Where the new system's disk is mounted, which is this distribution's own choice of place. */
    private final String root;

    /** The device the new system goes on, a whole disk or one partition of it; empty until one is made. */
    private String device = "";
    private boolean formatted;
    private boolean mounted;

    /** The partition made ready to hold a bootloader, and where it is mounted; empty when none is. */
    private String espDevice = "";
    private boolean espFormatted;
    private String espMount = "";

    /** The driver number every disk of this kind answers to, which a device listing prints first. */
    private static final int SCSI_DISK = 8;

    /** How many device numbers one disk takes up, which is what leaves room for its partitions. */
    private static final int MINORS_PER_DISK = 16;

    /** How long a mechanical disk takes to have a filesystem made on it, in ticks; a faster one takes less. */
    private static final int FORMAT_TICKS = 140;

    private static final TextKey EXT4_USAGE = TextKey.of("jsc.install.live_disks.ext4_usage",
            "Usage: mkfs.ext4 /dev/<device>");
    private static final TextKey NO_DEVICE_SIZE = TextKey.of("jsc.install.live_disks.no_device_size",
            "mke2fs: No such file or directory while trying to determine filesystem size");
    private static final TextKey IS_ESP = TextKey.of("jsc.install.live_disks.is_esp",
            "mke2fs: %s is the EFI System partition.");
    private static final TextKey IS_ESP_HINT = TextKey.of("jsc.install.live_disks.is_esp_hint",
            "       (that one holds the bootloader: mkfs.fat -F 32 /dev/%s)");
    private static final TextKey HAS_TABLE = TextKey.of("jsc.install.live_disks.has_table",
            "/dev/%s contains a gpt partition table.");
    private static final TextKey USE_A_PARTITION = TextKey.of("jsc.install.live_disks.use_a_partition",
            "mke2fs: will not make a filesystem here; use one of its partitions.");
    private static final TextKey FAT_USAGE = TextKey.of("jsc.install.live_disks.fat_usage",
            "Usage: mkfs.fat -F 32 /dev/<partition>");
    private static final TextKey FAT_CANNOT_OPEN = TextKey.of("jsc.install.live_disks.fat_cannot_open",
            "mkfs.fat: unable to open %s: No such file or directory");
    private static final TextKey NOT_ESP = TextKey.of("jsc.install.live_disks.not_esp",
            "mkfs.fat: %s is not an EFI System partition.");
    private static final TextKey NOT_ESP_HINT = TextKey.of("jsc.install.live_disks.not_esp_hint",
            "       (set its type first: fdisk, then t)");
    private static final TextKey MOUNT_BAD_USAGE = TextKey.of("jsc.install.live_disks.mount_bad_usage",
            "mount: bad usage");
    private static final TextKey MOUNT_TRY = TextKey.of("jsc.install.live_disks.mount_try",
            "Try 'mount /dev/<device> %s'.");
    private static final TextKey NO_MOUNT_POINT = TextKey.of("jsc.install.live_disks.no_mount_point",
            "mount: %s: mount point does not exist.");
    private static final TextKey ROOT_FIRST = TextKey.of("jsc.install.live_disks.root_first",
            "       (mount the root first: mount /dev/<partition> %s)");
    private static final TextKey ESP_WRONG_FS = TextKey.of("jsc.install.live_disks.esp_wrong_fs",
            "mount: %s: wrong fs type on /dev/%s.");
    private static final TextKey ESP_FORMAT_FIRST = TextKey.of("jsc.install.live_disks.esp_format_first",
            "       (make it first: mkfs.fat -F 32 /dev/%s)");
    private static final TextKey WRONG_FS = TextKey.of("jsc.install.live_disks.wrong_fs",
            "mount: %s: wrong fs type, bad option, bad superblock on /dev/%s.");
    private static final TextKey FORMAT_FIRST = TextKey.of("jsc.install.live_disks.format_first",
            "       (format it first: mkfs.ext4 /dev/%s)");

    LiveDisks(final String root) {
        this.root = root;
    }

    /**
     * A partition written onto a disk.
     *
     * @param number where it sits on the disk, so {@code sda1} is number one
     * @param sizeMb how big it was asked to be, or 0 for one that took whatever was left
     * @param esp    whether it is the partition the firmware looks in for a bootloader
     */
    record Partition(int number, int sizeMb, boolean esp) {

        /** The device name of this partition on that disk. */
        String on(final String disk) {
            return disk + this.number;
        }

        /**
         * What a partition table calls it: the name of a row of the table of partition types, which the editor
         * prints the way it prints the rest of the table, as data.
         */
        Text type() {
            return Text.literal(this.esp ? "EFI System" : "Linux filesystem");
        }
    }

    String rootDevice() {
        return this.device;
    }

    boolean mounted() {
        return this.mounted;
    }

    /** Where the partition the firmware reads is mounted, empty when it is not. */
    String espMount() {
        return this.espMount;
    }

    String espDevice() {
        return this.espDevice;
    }

    /** Whether all four of the medium's own places have been bound into the new system. */
    boolean boundIn() {
        return this.binds.containsAll(List.of("/proc", "/sys", "/dev", "/run"));
    }

    /** The index of the target disk among the machine's ({@code sda} is 0), or -1 when none is chosen. */
    int targetIndex() {
        if (this.device.length() < 3 || !this.device.startsWith("sd")) {
            return -1;
        }
        return this.device.charAt(2) - 'a';
    }

    /** The identifier of the filesystem the new system is on, which is what names it everywhere. */
    String rootUuid(final LiveInstallState.Env env) {
        return Ext4Figures.uuid(this.device, sizeOf(this.device, env));
    }

    /** The serial of the filesystem the firmware reads, which is all that kind of filesystem has. */
    String espSerial(final LiveInstallState.Env env) {
        return Ext4Figures.serial(this.espDevice, sizeOf(this.espDevice, env));
    }

    List<Partition> table(final String disk) {
        return this.tables.getOrDefault(disk, List.of());
    }

    void writeTable(final String disk, final List<Partition> table) {
        this.tables.put(disk, List.copyOf(table));
    }

    /** Makes the filesystem the new system goes on, which takes the disk a while. */
    LiveTurn mkfsExt4(final String arg, final LiveInstallState.Env env) {
        final String dev = deviceName(arg);
        if (dev.isEmpty()) {
            return LiveTurn.refused(EXT4_USAGE.text());
        }
        final Partition part = this.partitionOf(dev);
        if (part == null && !env.has(dev)) {
            return LiveTurn.refused(NO_DEVICE_SIZE.text());
        }
        if (part != null && part.esp()) {
            return LiveTurn.refused(IS_ESP.with(dev), IS_ESP_HINT.with(dev));
        }
        /*
         * A disk somebody partitioned is not a disk to write a filesystem straight onto: the real tool refuses
         * rather than quietly wiping the table somebody just made.
         */
        if (part == null && !this.table(dev).isEmpty()) {
            return LiveTurn.refused(HAS_TABLE.with(dev), USE_A_PARTITION.text());
        }
        final long sizeMb = this.sizeOf(dev, env);
        return LiveTurn.running(DiskVoices.mke2fs(dev, sizeMb, sizeMb, formatTicks(dev, env), () -> {
            this.device = dev;
            this.formatted = true;
            this.mounted = false;
        }));
    }

    /** Makes the filesystem the firmware reads a bootloader out of, which only the modern one needs. */
    LiveTurn mkfsFat(final String[] parts, final LiveInstallState.Env env) {
        String arg = "";
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].startsWith("-") && !parts[i].equals("32")) {
                arg = parts[i];
            }
        }
        final String dev = deviceName(arg);
        if (dev.isEmpty()) {
            return LiveTurn.refused(FAT_USAGE.text());
        }
        final Partition part = this.partitionOf(dev);
        if (part == null) {
            return LiveTurn.refused(FAT_CANNOT_OPEN.with(dev));
        }
        if (!part.esp()) {
            return LiveTurn.refused(NOT_ESP.with(dev), NOT_ESP_HINT.text());
        }
        return LiveTurn.running(DiskVoices.mkfsFat(Math.max(8, formatTicks(dev, env) / 8), () -> {
            this.espDevice = dev;
            this.espFormatted = true;
            this.espMount = "";
        }));
    }

    /**
     * Mounts something, without a word when it works, which is how the real one says yes.
     *
     * <p>The flags that bind the running medium's own places into the new system are taken as they are and
     * remembered; the one that makes the mount point first is taken too, since the handbooks use it.
     */
    LiveTurn mount(final String[] parts, final LiveFiles files) {
        final List<String> words = new ArrayList<>();
        boolean binding = false;
        for (int i = 1; i < parts.length; i++) {
            final String word = parts[i];
            if (word.equals("--rbind") || word.equals("--bind") || word.equals("--types") || word.equals("-t")
                    || word.startsWith("--make-")) {
                binding = true;
            } else if (!word.startsWith("-")) {
                words.add(word);
            }
        }
        if (binding) {
            return this.bind(words);
        }
        if (words.size() < 2) {
            return LiveTurn.refused(MOUNT_BAD_USAGE.text(), MOUNT_TRY.with(this.root));
        }
        final String dev = deviceName(words.get(0));
        final String point = words.get(1);
        if (point.equals(this.root + "/boot") || point.equals(this.root + "/efi")) {
            if (!this.mounted) {
                return LiveTurn.refused(NO_MOUNT_POINT.with(point), ROOT_FIRST.with(this.root));
            }
            if (!this.espFormatted || !dev.equals(this.espDevice)) {
                return LiveTurn.refused(ESP_WRONG_FS.with(point, dev), ESP_FORMAT_FIRST.with(dev));
            }
            this.espMount = point;
            files.makeDir(point);
            return LiveTurn.silent();
        }
        if (!point.equals(this.root)) {
            return LiveTurn.refused(NO_MOUNT_POINT.with(point));
        }
        if (!this.formatted || !dev.equals(this.device)) {
            return LiveTurn.refused(WRONG_FS.with(this.root, dev), FORMAT_FIRST.with(dev));
        }
        this.mounted = true;
        return LiveTurn.silent();
    }

    /** Lists the machine's block devices in the real tool's seven columns, the live medium among them. */
    LiveTurn lsblk(final LiveInstallState.Env env, final long mediumMb, final String mediumMount) {
        final List<CliLine> out = new ArrayList<>();
        out.add(DiskVoices.lsblkHeader());
        out.add(DiskVoices.lsblkRow("loop0", 7, 0, false, mediumMb, true, "loop", mediumMount));
        int index = 0;
        for (final LiveInstallState.Device disk : env.devices()) {
            out.add(DiskVoices.lsblkRow(disk.name(), SCSI_DISK, index * MINORS_PER_DISK, false, disk.sizeMb(),
                    false, "disk", this.mountOf(disk.name())));
            final List<Partition> parts = this.table(disk.name());
            for (int i = 0; i < parts.size(); i++) {
                final Partition part = parts.get(i);
                final String name = part.on(disk.name());
                /* The plain branch drawing, which is what the real tool falls back to on a plain terminal. */
                out.add(DiskVoices.lsblkRow((i == parts.size() - 1 ? "`-" : "|-") + name, SCSI_DISK,
                        index * MINORS_PER_DISK + part.number(), false, this.sizeOf(name, env), false, "part",
                        this.mountOf(name)));
            }
            index++;
        }
        return LiveTurn.said(out);
    }

    /** Names the filesystems by their identifiers, which is what a table written by hand is copied from. */
    LiveTurn blkid(final LiveInstallState.Env env) {
        final List<CliLine> out = new ArrayList<>();
        if (this.espFormatted) {
            out.add(DiskVoices.blkid(this.espDevice, this.espSerial(env), true,
                    partUuid(this.espDevice)));
        }
        if (this.formatted) {
            out.add(DiskVoices.blkid(this.device, this.rootUuid(env), false, partUuid(this.device)));
        }
        return LiveTurn.said(out);
    }

    /** How big that device is: a partition's own size, what is left of its disk, or the whole disk. */
    long sizeOf(final String dev, final LiveInstallState.Env env) {
        for (final LiveInstallState.Device disk : env.devices()) {
            if (!dev.startsWith(disk.name())) {
                continue;
            }
            final Partition part = this.partitionOf(dev);
            if (part == null) {
                return disk.sizeMb();
            }
            if (part.sizeMb() > 0) {
                return part.sizeMb();
            }
            long left = disk.sizeMb();
            for (final Partition other : this.table(disk.name())) {
                left -= Math.max(0, other.sizeMb());
            }
            return Math.max(0, left);
        }
        return 0;
    }

    /** The disk a device name sits on: {@code sda2} is on {@code sda}, and {@code sda} is its own. */
    static String diskOf(final String name) {
        int end = name.length();
        while (end > 0 && Character.isDigit(name.charAt(end - 1))) {
            end--;
        }
        return name.substring(0, end);
    }

    static String deviceName(final String arg) {
        return arg.startsWith("/dev/") ? arg.substring(5) : arg;
    }

    void save(final LiveSaved out) {
        out.put("device", this.device);
        out.put("formatted", this.formatted);
        out.put("mounted", this.mounted);
        out.put("esp_device", this.espDevice);
        out.put("esp_formatted", this.espFormatted);
        out.put("esp_mount", this.espMount);
        out.put("binds", String.join(" ", this.binds));
        for (final Map.Entry<String, List<Partition>> disk : this.tables.entrySet()) {
            final StringBuilder written = new StringBuilder();
            for (final Partition part : disk.getValue()) {
                if (!written.isEmpty()) {
                    written.append(',');
                }
                written.append(part.number()).append(':').append(part.sizeMb()).append(':')
                        .append(part.esp() ? 1 : 0);
            }
            out.put("table:" + disk.getKey(), written.toString());
        }
    }

    void load(final LiveSaved saved) {
        this.device = saved.value("device", "");
        this.formatted = saved.flag("formatted");
        this.mounted = saved.flag("mounted");
        this.espDevice = saved.value("esp_device", "");
        this.espFormatted = saved.flag("esp_formatted");
        this.espMount = saved.value("esp_mount", "");
        for (final String bind : saved.value("binds", "").split(" ")) {
            if (!bind.isEmpty()) {
                this.binds.add(bind);
            }
        }
        for (final Map.Entry<String, String> line : saved.all().entrySet()) {
            if (line.getKey().startsWith("table:")) {
                this.tables.put(line.getKey().substring(6), partitions(line.getValue()));
            }
        }
    }

    /** Binds one of the running medium's own places into the new system, or takes a flag that goes with one. */
    private LiveTurn bind(final List<String> words) {
        if (!this.mounted) {
            return LiveTurn.refused(NO_MOUNT_POINT.with(this.root));
        }
        for (final String word : words) {
            if (word.equals("/proc") || word.equals("/sys") || word.equals("/dev") || word.equals("/run")) {
                this.binds.add(word);
            }
        }
        return LiveTurn.silent();
    }

    /** Where that device is mounted right now, which is what the listing's last column says. */
    private String mountOf(final String name) {
        if (this.mounted && name.equals(this.device)) {
            return this.root;
        }
        return !this.espMount.isEmpty() && name.equals(this.espDevice) ? this.espMount : "";
    }

    /** The partition of that name, or null when nothing on this machine is called it. */
    private Partition partitionOf(final String name) {
        for (final Map.Entry<String, List<Partition>> disk : this.tables.entrySet()) {
            for (final Partition part : disk.getValue()) {
                if (part.on(disk.getKey()).equals(name)) {
                    return part;
                }
            }
        }
        return null;
    }

    /** How long this machine's disk takes to have a filesystem made on it. */
    private static int formatTicks(final String dev, final LiveInstallState.Env env) {
        final LiveInstallState.Device disk = env.find(diskOf(dev));
        return Math.max(24, FORMAT_TICKS / Math.max(1, disk == null ? 1 : disk.speed()));
    }

    /** A partition's own identifier on its table, short and steady, which the identifier lister prints last. */
    private static String partUuid(final String dev) {
        final String number = dev.substring(diskOf(dev).length());
        return Ext4Figures.serial(diskOf(dev), 1).toLowerCase(Locale.ROOT).replace("-", "")
                + "-" + (number.isEmpty() ? "00" : String.format(Locale.ROOT, "%02d", Integer.parseInt(number)));
    }

    /** A partition table read back from the one line it was written as. */
    private static List<Partition> partitions(final String written) {
        final List<Partition> table = new ArrayList<>();
        for (final String one : written.split(",")) {
            final String[] field = one.split(":");
            if (field.length != 3) {
                continue;
            }
            try {
                table.add(new Partition(Integer.parseInt(field[0]), Integer.parseInt(field[1]),
                        field[2].equals("1")));
            } catch (final NumberFormatException wrong) {
                return List.copyOf(table);
            }
        }
        return List.copyOf(table);
    }
}
