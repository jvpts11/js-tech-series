/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.MonitorPanel;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.computers.monitor.MonitorPicturePayload;
import dev.jstech.computers.monitor.MonitorPictures;
import dev.jstech.computers.monitor.VideoMemory;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.live.LiveFeed;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLink;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.core.util.Loaded;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for a Monitor: a {@code COMPUTING} {@link IPeripheralEndpoint} that displays a computer's interface.
 * Its screen lights a moment after its computer starts running and goes dark the moment it stops; the block's
 * {@code LIT} state follows whether it is lit.
 *
 * <p>Flat monitors joined into a big screen ({@link MonitorPanel}) are one monitor: the first of them to link holds
 * the link and the video output, the others refuse one of their own and light with it, and only the corner keeps its
 * power button. Whoever is near is sent what the face shows, by the monitor holding the link.
 */
public class MonitorBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint {

    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);
    private final BoolField lit = fields().flag("Lit", false).save();
    /** The terminal tab the player last used on this screen, which it reopens on. */
    private final IntField lastTab = fields().integer("LastTab", ComputerTerminalMenu.TAB_NETWORK).save();
    /*
     * While its computer runs but its video memory has no room for this monitor: what the monitor needs and what was
     * free, both 0 while it fits. The monitor stays dark and its own menu says so on the glass.
     */
    private final LongField starvedNeedKb = fields().longInteger("StarvedNeedKb", 0L).toClient();
    private final LongField starvedFreeKb = fields().longInteger("StarvedFreeKb", 0L).toClient();
    /* What the face shows, sent to whoever is near and only when it changes. */
    private final LiveFeed<IMonitorPicture> feed = new LiveFeed<>();
    private int bootTicks;
    /*
     * The machine this screen is currently showing on someone else's behalf: a Remote Control session puts a REMOTE
     * computer on this monitor, so for as long as it lasts the screen answers for that machine and not for the one
     * its cable is linked to. Transient by nature, a session does not outlive the window it was opened in.
     */
    @Nullable
    private BlockPos remoteSession;
    /* The big screen this monitor is part of, and the count of placed monitors it was worked out at. */
    @Nullable
    private MonitorPanel panel;
    private int panelAt = -1;

    private static final int BOOT_DELAY = 20;
    private static final int BATTLESTATION_MONITORS = 4;
    /* How often the face is described for the players near it: twice a second. */
    private static final int FEED_INTERVAL = 10;

    public MonitorBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.MONITOR_BE.get(), pos, state);
        fields().mirror(MonitorBlock.LIT, lit::get);
        fields().mirror(MonitorBlock.BUTTON, this::showsButton);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final MonitorBlockEntity monitor) {
        if (level instanceof ServerLevel server) {
            monitor.tick(server);
        }
    }

    @Override
    public PeripheralCableType cableType() {
        return link.cableType();
    }

    /** A screen takes a video output of its computer. */
    @Override
    public PortKind portKind() {
        return PortKind.VIDEO;
    }

    @Override
    public Optional<Long> linkedOwner() {
        return link.linkedOwner();
    }

    @Override
    public void onOwnerLinked(final long ownerPos) {
        link.linked(ownerPos);
    }

    @Override
    public void onOwnerUnlinked() {
        link.unlinked();
    }

    @Nullable
    public BlockPos ownerPos() {
        return link.ownerPos();
    }

    /** Whether the screen is lit: its computer runs and drives it. */
    public boolean lit() {
        return lit.get();
    }

    /** Whether its computer runs but has no video memory left for it, which keeps it dark. */
    public boolean starved() {
        return starvedNeedKb.get() > 0L;
    }

    /** What it needs of its computer's video memory while starved, in kilobytes. */
    public long starvedNeedKb() {
        return starvedNeedKb.get();
    }

    /** What was free of it when it was refused, in kilobytes. */
    public long starvedFreeKb() {
        return starvedFreeKb.get();
    }

    /** Starts (or ends, with {@code null}) a remote session showing {@code machine} on this screen. */
    public void setRemoteSession(@Nullable final BlockPos machine) {
        this.remoteSession = machine;
    }

    /** The machine a remote session is showing here, or null when the screen is showing its own. */
    @Nullable
    public BlockPos remoteSession() {
        return remoteSession;
    }

    /**
     * Whether this monitor legitimately shows {@code machine}: either its cable links to it, or a remote session put
     * it there. The menus validate through this, so a taken-over screen is not torn down for showing a computer that
     * is not its own.
     */
    public boolean shows(final BlockPos machine) {
        return machine != null
                && (machine.equals(ownerPos()) || machine.equals(remoteSession));
    }

    public int lastTab() {
        return lastTab.get();
    }

    public void setLastTab(final int tab) {
        if (tab >= 0) {
            lastTab.set(tab);
        }
    }

    /** The big screen this monitor is part of, or null when it stands alone. */
    @Nullable
    public MonitorPanel panel() {
        if (level == null) {
            return null;
        }
        if (panelAt != MonitorPanel.generation()) {
            panelAt = MonitorPanel.generation();
            panel = MonitorPanel.resolve(level, worldPosition);
        }
        return panel;
    }

    /**
     * The monitor whose link this one's screen runs through: the member of its big screen that holds the link, or
     * this one when it stands alone or nobody in its screen is linked yet.
     */
    public MonitorBlockEntity screen() {
        final MonitorBlockEntity holder = linkHolder();
        return holder == null ? this : holder;
    }

    /** Whether this monitor shows its power button: alone it does, in a big screen only the corner does. */
    public boolean showsButton() {
        final MonitorPanel big = panel();
        return big == null || big.buttonHolder().equals(worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        MonitorPanel.changed();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        MonitorPanel.changed();
    }

    private void tick(final ServerLevel level) {
        final MonitorBlockEntity holder = linkHolder();
        if (holder != null && holder != this) {
            // Another monitor of this big screen holds the link: this one takes no output, and lights with it.
            if (link.linkedOwner().isPresent()) {
                link.unlink(level, worldPosition);
            }
            bootTicks = 0;
            lit.set(holder.lit());
            return;
        }
        final boolean wasLinked = link.linkedOwner().isPresent();
        link.tick(level, worldPosition);
        if (!wasLinked && link.linkedOwner().isPresent()) {
            reportBattlestation(level);
        }
        updateScreen(level);
        if (level.getGameTime() % FEED_INTERVAL == Math.floorMod(worldPosition.asLong(), FEED_INTERVAL)) {
            feed.tick(level, worldPosition, () -> MonitorPictures.describe(level, this),
                    (player, picture) -> PacketDistributor.sendToPlayer(player,
                            new MonitorPicturePayload(worldPosition, picture)));
        }
    }

    /*
     * The monitor of this big screen that holds the link, the first linked in the screen's own order, or null when
     * none is linked or this monitor stands alone. Asked of loaded monitors only: a screen is never loaded for this.
     */
    @Nullable
    private MonitorBlockEntity linkHolder() {
        final MonitorPanel big = panel();
        if (big == null || level == null) {
            return null;
        }
        for (final BlockPos member : big.members()) {
            if (Loaded.blockEntity(level, member) instanceof MonitorBlockEntity monitor
                    && monitor.link.linkedOwner().isPresent()) {
                return monitor;
            }
        }
        return null;
    }

    /* A fourth monitor on one computer is a battlestation, earned by whoever put this one up. */
    private void reportBattlestation(final ServerLevel level) {
        final BlockPos owner = link.ownerPos();
        if (owner == null || !(Loaded.blockEntity(level, owner) instanceof IPeripheralOwner linkedTo)) {
            return;
        }
        int monitors = 0;
        for (final long endpoint : linkedTo.linkedEndpoints()) {
            if (Loaded.blockEntity(level, BlockPos.of(endpoint)) instanceof MonitorBlockEntity) {
                monitors++;
            }
        }
        if (monitors >= BATTLESTATION_MONITORS) {
            JscEvents.awardOperator(this, JscEvents.BATTLESTATION);
        }
    }

    private void updateScreen(final ServerLevel level) {
        // Lit only when the linked computer is actively running and drives it: not powered off, nor disabling it.
        final BlockPos owner = link.ownerPos();
        if (owner != null && !level.isLoaded(owner)) {
            return; // a computer whose chunk is away is not asked, nor loaded back; the screen stays as it is
        }
        final IOsHost host = owner != null && level.getBlockEntity(owner) instanceof IOsHost running ? running : null;
        final boolean computerRunning = host != null
                && host.isRunning() && !host.isDisabled(worldPosition.asLong());
        // A running computer lights this screen only while its video memory has room for it.
        final VideoMemory.Screen share = computerRunning
                ? VideoMemory.of(level, host).screen(worldPosition.asLong()) : null;
        final boolean starvedNow = share != null && !share.fits();
        starvedNeedKb.set(starvedNow ? share.needKb() : 0L);
        starvedFreeKb.set(starvedNow ? share.freeKb() : 0L);
        if (!computerRunning || starvedNow) {
            bootTicks = 0;
            if (lit.get()) {
                lit.set(false);
                // A picture tube is heard going dark; a flat panel, from the Transition on, goes dark without a sound.
                if (getBlockState().getBlock() instanceof MonitorBlock monitor && monitor.kind().tubeSounds()) {
                    Audio.at(level, worldPosition, ComputingSounds.MONITOR_POWER_OFF);
                }
            }
            return;
        }
        if (lit.get()) {
            return; // already booted onto the desktop
        }
        if (++bootTicks >= BOOT_DELAY) {
            lit.set(true);
            Audio.at(level, worldPosition, ComputingSounds.MONITOR_POWER_ON);
            bootTicks = 0;
        }
    }
}
