/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.tests.clienttest.computers.AssemblyScreensClientTests;
import dev.jstech.tests.clienttest.computers.AutomationManagerClientTests;
import dev.jstech.tests.clienttest.computers.BookPagesClientTests;
import dev.jstech.tests.clienttest.computers.BookStylesClientTests;
import dev.jstech.tests.clienttest.computers.BootSplashClientTests;
import dev.jstech.tests.clienttest.computers.BusModelClientTests;
import dev.jstech.tests.clienttest.computers.BusWindowClientTests;
import dev.jstech.tests.clienttest.computers.CabinetItemClientTests;
import dev.jstech.tests.clienttest.computers.CableLineClientTests;
import dev.jstech.tests.clienttest.computers.CdeClientTests;
import dev.jstech.tests.clienttest.computers.ComputerCaseClientTests;
import dev.jstech.tests.clienttest.computers.CraftingChainClientTests;
import dev.jstech.tests.clienttest.computers.CrashPagesClientTests;
import dev.jstech.tests.clienttest.computers.CreativeTabsClientTests;
import dev.jstech.tests.clienttest.computers.DesktopMenuClientTests;
import dev.jstech.tests.clienttest.computers.DeviceIconClientTests;
import dev.jstech.tests.clienttest.computers.DeviceManagerClientTests;
import dev.jstech.tests.clienttest.computers.DockStationClientTests;
import dev.jstech.tests.clienttest.computers.EraBuildClientTests;
import dev.jstech.tests.clienttest.computers.EraEndClientTests;
import dev.jstech.tests.clienttest.computers.EraEverydayClientTests;
import dev.jstech.tests.clienttest.computers.EraFabricationClientTests;
import dev.jstech.tests.clienttest.computers.EraInstallClientTests;
import dev.jstech.tests.clienttest.computers.EraInstalledProgramsClientTests;
import dev.jstech.tests.clienttest.computers.EraNetworkClientTests;
import dev.jstech.tests.clienttest.computers.EraNetworkedProgramsClientTests;
import dev.jstech.tests.clienttest.computers.EraOperationsClientTests;
import dev.jstech.tests.clienttest.computers.EraOtherShellsClientTests;
import dev.jstech.tests.clienttest.computers.EraPeripheralsClientTests;
import dev.jstech.tests.clienttest.computers.EraProgrammingClientTests;
import dev.jstech.tests.clienttest.computers.EraProgramsClientTests;
import dev.jstech.tests.clienttest.computers.EraServiceInstallClientTests;
import dev.jstech.tests.clienttest.computers.EraShellClientTests;
import dev.jstech.tests.clienttest.computers.EraStorageClientTests;
import dev.jstech.tests.clienttest.computers.EraStorageFlowClientTests;
import dev.jstech.tests.clienttest.computers.EraSystemProgramsClientTests;
import dev.jstech.tests.clienttest.computers.EraSystemSetupClientTests;
import dev.jstech.tests.clienttest.computers.EraSystemsClientTests;
import dev.jstech.tests.clienttest.computers.ExposureClientTests;
import dev.jstech.tests.clienttest.computers.ExtremesClientTests;
import dev.jstech.tests.clienttest.computers.FilesSyncClientTests;
import dev.jstech.tests.clienttest.computers.FramesSevenTenClientTests;
import dev.jstech.tests.clienttest.computers.FreeBsdClientTests;
import dev.jstech.tests.clienttest.computers.FullJourneyClientTests;
import dev.jstech.tests.clienttest.computers.GatewayManagerClientTests;
import dev.jstech.tests.clienttest.computers.GpuClientTests;
import dev.jstech.tests.clienttest.computers.GuideScenesClientTests;
import dev.jstech.tests.clienttest.computers.GuidedInstallerClientTests;
import dev.jstech.tests.clienttest.computers.HelpTerminalClientTests;
import dev.jstech.tests.clienttest.computers.HelpWindowClientTests;
import dev.jstech.tests.clienttest.computers.InfoCenterClientTests;
import dev.jstech.tests.clienttest.computers.InstallMediaClientTests;
import dev.jstech.tests.clienttest.computers.InteracTuiClientTests;
import dev.jstech.tests.clienttest.computers.IsmsClientTests;
import dev.jstech.tests.clienttest.computers.LeftoverProgramsClientTests;
import dev.jstech.tests.clienttest.computers.LiveInstallClientTests;
import dev.jstech.tests.clienttest.computers.MachineSoundClientTests;
import dev.jstech.tests.clienttest.computers.MainframeClientTests;
import dev.jstech.tests.clienttest.computers.McNetSpaceClientTests;
import dev.jstech.tests.clienttest.computers.MediaDriveClientTests;
import dev.jstech.tests.clienttest.computers.MekanismClientTests;
import dev.jstech.tests.clienttest.computers.MemoryCrashClientTests;
import dev.jstech.tests.clienttest.computers.MenuSwapCursorClientTests;
import dev.jstech.tests.clienttest.computers.MonitorClientTests;
import dev.jstech.tests.clienttest.computers.MotionClientTests;
import dev.jstech.tests.clienttest.computers.MsdClientTests;
import dev.jstech.tests.clienttest.computers.MultipartModelClientTests;
import dev.jstech.tests.clienttest.computers.MusicImportClientTests;
import dev.jstech.tests.clienttest.computers.NetworkDeviceClientTests;
import dev.jstech.tests.clienttest.computers.NetworkInteractorClientTests;
import dev.jstech.tests.clienttest.computers.NetworkLinksClientTests;
import dev.jstech.tests.clienttest.computers.NetworkServicesClientTests;
import dev.jstech.tests.clienttest.computers.NetworkSharesClientTests;
import dev.jstech.tests.clienttest.computers.NextgreClientTests;
import dev.jstech.tests.clienttest.computers.OffscreenDesktopClientTests;
import dev.jstech.tests.clienttest.computers.OpenWithClientTests;
import dev.jstech.tests.clienttest.computers.OutOfMemoryClientTests;
import dev.jstech.tests.clienttest.computers.PanelStylesClientTests;
import dev.jstech.tests.clienttest.computers.PatternEncoderClientTests;
import dev.jstech.tests.clienttest.computers.PeripheralCableClientTests;
import dev.jstech.tests.clienttest.computers.PrinterClientTests;
import dev.jstech.tests.clienttest.computers.ProphetClientTests;
import dev.jstech.tests.clienttest.computers.RackClientTests;
import dev.jstech.tests.clienttest.computers.RackCrashClientTests;
import dev.jstech.tests.clienttest.computers.RedstoneInterfaceClientTests;
import dev.jstech.tests.clienttest.computers.SettingsPersonalizeClientTests;
import dev.jstech.tests.clienttest.computers.SettingsScreenClientTests;
import dev.jstech.tests.clienttest.computers.SettingsSharingClientTests;
import dev.jstech.tests.clienttest.computers.SigmaConsoleClientTests;
import dev.jstech.tests.clienttest.computers.SigmaEditorClientTests;
import dev.jstech.tests.clienttest.computers.SigmaWidgetsClientTests;
import dev.jstech.tests.clienttest.computers.SoundfoundryClientTests;
import dev.jstech.tests.clienttest.computers.SoundfoundryShareClientTests;
import dev.jstech.tests.clienttest.computers.SoundfoundryStandardClientTests;
import dev.jstech.tests.clienttest.computers.SourceBuildClientTests;
import dev.jstech.tests.clienttest.computers.SupercomputerJourneyClientTests;
import dev.jstech.tests.clienttest.computers.SystemMotionClientTests;
import dev.jstech.tests.clienttest.computers.SystemUiClientTests;
import dev.jstech.tests.clienttest.computers.TaskbarClientTests;
import dev.jstech.tests.clienttest.computers.TerminalEditorClientTests;
import dev.jstech.tests.clienttest.computers.TerminalFontClientTests;
import dev.jstech.tests.clienttest.computers.TerminalSelectionClientTests;
import dev.jstech.tests.clienttest.computers.TerminalWidthClientTests;
import dev.jstech.tests.clienttest.computers.TransitionDesktopsClientTests;
import dev.jstech.tests.clienttest.computers.TrashClientTests;
import dev.jstech.tests.clienttest.computers.UiSweepClientTests;
import dev.jstech.tests.clienttest.computers.UnixClientTests;
import dev.jstech.tests.clienttest.computers.UnixFreeBsdEditorsClientTests;
import dev.jstech.tests.clienttest.computers.UpdateClientTests;
import dev.jstech.tests.clienttest.computers.VintageShellsClientTests;
import dev.jstech.tests.clienttest.computers.VolumeClientTests;
import dev.jstech.tests.clienttest.computers.WorkshopClientTests;
import dev.jstech.tests.clienttest.computers.WreckedMachineClientTests;
import dev.jstech.tests.clienttest.core.AcousticsClientTests;
import dev.jstech.tests.clienttest.core.AudioClientTests;
import dev.jstech.tests.clienttest.core.BigPayloadClientTests;
import dev.jstech.tests.clienttest.core.ConnectedPacksClientTests;
import dev.jstech.tests.clienttest.core.ConveyorClientTests;
import dev.jstech.tests.clienttest.core.CoreStateClientTests;
import dev.jstech.tests.clienttest.core.DataRegistryClientTests;
import dev.jstech.tests.clienttest.core.ComponentsClientTests;
import dev.jstech.tests.clienttest.core.DataScreenClientTests;
import dev.jstech.tests.clienttest.core.MaterialsClientTests;
import dev.jstech.tests.clienttest.core.NotificationsClientTests;
import dev.jstech.tests.clienttest.core.SettingsKitClientTests;
import dev.jstech.tests.clienttest.core.FluidClientTests;
import dev.jstech.tests.clienttest.core.GridPainterClientTests;
import dev.jstech.tests.clienttest.core.HazardDevicesClientTests;
import dev.jstech.tests.clienttest.core.FireClientTests;
import dev.jstech.tests.clienttest.core.MovingStructureClientTests;
import dev.jstech.tests.clienttest.core.MovingStructurePhysicsClientTests;
import dev.jstech.tests.clienttest.core.MovingStructureLifeClientTests;
import dev.jstech.tests.clienttest.core.MovingStructureToolsClientTests;
import dev.jstech.tests.clienttest.core.HudHologramClientTests;
import dev.jstech.tests.clienttest.core.ItemStateClientTests;
import dev.jstech.tests.clienttest.core.ToolClientTests;
import dev.jstech.tests.clienttest.core.TooltipClientTests;
import dev.jstech.tests.clienttest.core.KeyboardFocusClientTests;
import dev.jstech.tests.clienttest.core.HudEditorClientTests;
import dev.jstech.tests.clienttest.core.ScreenTouchClientTests;
import dev.jstech.tests.clienttest.core.StillPartsClientTests;
import dev.jstech.tests.clienttest.core.ScreenToolsClientTests;
import dev.jstech.tests.clienttest.core.CameraClientTests;
import dev.jstech.tests.clienttest.core.KeyActionClientTests;
import dev.jstech.tests.clienttest.core.EffectsClientTests;
import dev.jstech.tests.clienttest.core.LightingClientTests;
import dev.jstech.tests.clienttest.core.TerrainClientTests;
import dev.jstech.tests.clienttest.core.PlacesClientTests;
import dev.jstech.tests.clienttest.core.LandDrawingClientTests;
import dev.jstech.tests.clienttest.core.SkyClientTests;
import dev.jstech.tests.clienttest.core.PhysicsClientTests;
import dev.jstech.tests.clienttest.core.RoomClientTests;
import dev.jstech.tests.clienttest.core.TravelClientTests;
import dev.jstech.tests.clienttest.core.WorldChangesClientTests;
import dev.jstech.tests.clienttest.core.LiveReloadClientTests;
import dev.jstech.tests.clienttest.core.MachineLooksClientTests;
import dev.jstech.tests.clienttest.core.MachineScreenClientTests;
import dev.jstech.tests.clienttest.core.MediaClientTests;
import dev.jstech.tests.clienttest.core.ModelKitClientTests;
import dev.jstech.tests.clienttest.core.MultiblockToolsClientTests;
import dev.jstech.tests.clienttest.core.MultipartClientTests;
import dev.jstech.tests.clienttest.core.ProgressionClientTests;
import dev.jstech.tests.clienttest.core.ReachClientTests;
import dev.jstech.tests.clienttest.core.RenderTargetsClientTests;
import dev.jstech.tests.clienttest.core.SoundMixerClientTests;
import dev.jstech.tests.clienttest.core.WorldStabilityClientTests;
import dev.jstech.tests.clienttest.industrial.DiagnosticsClientTests;
import dev.jstech.tests.clienttest.industrial.IndustrialScreensClientTests;
import dev.jstech.tests.clienttest.industrial.RecipeViewClientTests;
import dev.jstech.tests.clienttest.series.CableBlockClientTests;
import dev.jstech.tests.clienttest.series.CableDragClientTests;
import dev.jstech.tests.clienttest.series.ClampMeterClientTests;
import dev.jstech.tests.clienttest.series.EndScreenClientTests;
import dev.jstech.tests.clienttest.series.EnergyConverterClientTests;
import dev.jstech.tests.clienttest.series.NetworkProbeClientTests;
import dev.jstech.tests.clienttest.series.OverheadLinesClientTests;
import dev.jstech.tests.clienttest.series.PipeClientTests;
import dev.jstech.tests.clienttest.series.SignalProbeClientTests;
import dev.jstech.tests.clienttest.series.SpacePersistorClientTests;
import dev.jstech.tests.clienttest.series.ManualClientTests;
import dev.jstech.tests.clienttest.series.SeriesManualsClientTests;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The explicit list of client test classes. Tests are discovered by reflection from these classes only (
 * no classpath scanning) and ordered by name so every shard sees the same list and can take its slice.
 */
public final class ClientTestSuite {

    private static final List<Class<?>> CLASSES = List.of(
            AudioClientTests.class,
            AcousticsClientTests.class,
            AutomationManagerClientTests.class,
            BootSplashClientTests.class,
            SoundMixerClientTests.class,
            MachineSoundClientTests.class,
            MainframeClientTests.class,
            RackClientTests.class,
            MediaClientTests.class,
            MusicImportClientTests.class,
            SoundfoundryClientTests.class,
            SoundfoundryShareClientTests.class,
            SoundfoundryStandardClientTests.class,
            SigmaEditorClientTests.class,
            SigmaConsoleClientTests.class,
            CabinetItemClientTests.class,
            AssemblyScreensClientTests.class,
            CdeClientTests.class,
            ComputerCaseClientTests.class,
            CoreStateClientTests.class,
            CrashPagesClientTests.class,
            CraftingChainClientTests.class,
            DesktopMenuClientTests.class,
            DeviceManagerClientTests.class,
            DeviceIconClientTests.class,
            ExposureClientTests.class,
            FilesSyncClientTests.class,
            FramesSevenTenClientTests.class,
            TransitionDesktopsClientTests.class,
            CreativeTabsClientTests.class,
            RecipeViewClientTests.class,
            HudHologramClientTests.class,
            HazardDevicesClientTests.class,
            FireClientTests.class,
            MovingStructureClientTests.class,
            MovingStructurePhysicsClientTests.class,
            MovingStructureLifeClientTests.class,
            MovingStructureToolsClientTests.class,
            WorldStabilityClientTests.class,
            ManualClientTests.class,
            SeriesManualsClientTests.class,
            BookStylesClientTests.class,
            GuideScenesClientTests.class,
            BookPagesClientTests.class,
            RenderTargetsClientTests.class,
            HelpWindowClientTests.class,
            HelpTerminalClientTests.class,
            FreeBsdClientTests.class,
            FullJourneyClientTests.class,
            EraSystemsClientTests.class,
            EraShellClientTests.class,
            EraProgramsClientTests.class,
            EraStorageClientTests.class,
            EraInstallClientTests.class,
            EraEverydayClientTests.class,
            EraSystemProgramsClientTests.class,
            EraInstalledProgramsClientTests.class,
            EraNetworkedProgramsClientTests.class,
            EraStorageFlowClientTests.class,
            EraServiceInstallClientTests.class,
            EraOperationsClientTests.class,
            EraBuildClientTests.class,
            EraSystemSetupClientTests.class,
            EraOtherShellsClientTests.class,
            EraNetworkClientTests.class,
            EraProgrammingClientTests.class,
            EraFabricationClientTests.class,
            EraPeripheralsClientTests.class,
            SupercomputerJourneyClientTests.class,
            EraEndClientTests.class,
            ExtremesClientTests.class,
            GatewayManagerClientTests.class,
            GpuClientTests.class,
            GridPainterClientTests.class,
            TerminalFontClientTests.class,
            GuidedInstallerClientTests.class,
            IndustrialScreensClientTests.class,
            InfoCenterClientTests.class,
            InstallMediaClientTests.class,
            InteracTuiClientTests.class,
            LeftoverProgramsClientTests.class,
            LiveInstallClientTests.class,
            MachineScreenClientTests.class,
            McNetSpaceClientTests.class,
            MotionClientTests.class,
            MenuSwapCursorClientTests.class,
            MediaDriveClientTests.class,
            MemoryCrashClientTests.class,
            OutOfMemoryClientTests.class,
            RackCrashClientTests.class,
            MonitorClientTests.class,
            MsdClientTests.class,
            MultipartModelClientTests.class,
            CableBlockClientTests.class,
            ClampMeterClientTests.class,
            EnergyConverterClientTests.class,
            PipeClientTests.class,
            EndScreenClientTests.class,
            NetworkProbeClientTests.class,
            CableDragClientTests.class,
            OverheadLinesClientTests.class,
            SignalProbeClientTests.class,
            SpacePersistorClientTests.class,
            MultiblockToolsClientTests.class,
            MultipartClientTests.class,
            ConveyorClientTests.class,
            ProgressionClientTests.class,
            ReachClientTests.class,
            ModelKitClientTests.class,
            MachineLooksClientTests.class,
            ConnectedPacksClientTests.class,
            LightingClientTests.class,
            EffectsClientTests.class,
            TerrainClientTests.class,
            PlacesClientTests.class,
            LandDrawingClientTests.class,
            SkyClientTests.class,
            PhysicsClientTests.class,
            RoomClientTests.class,
            TravelClientTests.class,
            WorldChangesClientTests.class,
            ItemStateClientTests.class,
            ToolClientTests.class,
            CameraClientTests.class,
            TooltipClientTests.class,
            KeyboardFocusClientTests.class,
            HudEditorClientTests.class,
            ScreenTouchClientTests.class,
            StillPartsClientTests.class,
            ScreenToolsClientTests.class,
            DataRegistryClientTests.class,
            KeyActionClientTests.class,
            DiagnosticsClientTests.class,
            FluidClientTests.class,
            BigPayloadClientTests.class,
            LiveReloadClientTests.class,
            DataScreenClientTests.class,
            ComponentsClientTests.class,
            SettingsKitClientTests.class,
            NotificationsClientTests.class,
            MaterialsClientTests.class,
            TaskbarClientTests.class,
            TerminalEditorClientTests.class,
            MekanismClientTests.class,
            CableLineClientTests.class,
            NetworkDeviceClientTests.class,
            NetworkLinksClientTests.class,
            BusWindowClientTests.class,
            BusModelClientTests.class,
            PeripheralCableClientTests.class,
            RedstoneInterfaceClientTests.class,
            NetworkInteractorClientTests.class,
            NetworkServicesClientTests.class,
            NetworkSharesClientTests.class,
            OffscreenDesktopClientTests.class,
            SystemMotionClientTests.class,
            VintageShellsClientTests.class,
            OpenWithClientTests.class,
            PanelStylesClientTests.class,
            PatternEncoderClientTests.class,
            PrinterClientTests.class,
            DockStationClientTests.class,
            WorkshopClientTests.class,
            UpdateClientTests.class,
            IsmsClientTests.class,
            NextgreClientTests.class,
            ProphetClientTests.class,
            SettingsPersonalizeClientTests.class,
            SettingsScreenClientTests.class,
            SettingsSharingClientTests.class,
            SourceBuildClientTests.class,
            SystemUiClientTests.class,
            SigmaWidgetsClientTests.class,
            TerminalSelectionClientTests.class,
            TerminalWidthClientTests.class,
            TrashClientTests.class,
            UiSweepClientTests.class,
            VolumeClientTests.class,
            WreckedMachineClientTests.class,
            UnixClientTests.class,
            UnixFreeBsdEditorsClientTests.class);

    private ClientTestSuite() {
    }

    /**
     * One discovered test: its display name, the method to invoke, its timeout and its index in the whole
     * (sorted) suite, and the index places the test's world area, so shards never overlap.
     */
    public record Entry(String name, Method method, int timeoutTicks, int index) {
    }

    /** Every test in the suite, sorted by name and numbered. */
    public static List<Entry> all() {
        final List<Entry> found = new ArrayList<>();
        for (final Class<?> type : CLASSES) {
            for (final Method method : type.getDeclaredMethods()) {
                final ClientTest annotation = method.getAnnotation(ClientTest.class);
                if (annotation == null) {
                    continue;
                }
                if (!Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())
                        || method.getParameterCount() != 1
                        || method.getParameterTypes()[0] != ClientTestContext.class) {
                    throw new IllegalStateException("client test " + type.getSimpleName() + "." + method.getName()
                            + " must be public static and take a single ClientTestContext");
                }
                found.add(new Entry(type.getSimpleName() + "." + method.getName(), method,
                        annotation.timeoutTicks(), 0));
            }
        }
        found.sort(Comparator.comparing(Entry::name));
        final List<Entry> entries = new ArrayList<>(found.size());
        for (int i = 0; i < found.size(); i++) {
            final Entry e = found.get(i);
            entries.add(new Entry(e.name(), e.method(), e.timeoutTicks(), i));
        }
        return entries;
    }

    /**
     * The slice of the suite that shard {@code shard} of {@code shards} runs. Knowing how many ticks each test took
     * last time, the longest go first, each to the shard with the least to do so far, so the shards end together and
     * no shard waits on the one that drew the long tests; a test never measured counts as the measured ones'
     * average. Knowing nothing, round-robin by index. Every shard reads the same durations, so each takes its own
     * slice of one split and no test runs twice or not at all.
     */
    public static List<Entry> shard(final int shard, final int shards, final Map<String, Integer> ticks) {
        return shard(shard, shards, ticks, entry -> true);
    }

    /**
     * The same, of only the tests {@code chosen} keeps: they are picked before the suite is split, so the few asked for
     * are shared out over the shards as the whole suite is, rather than found in one shard and missing in the others.
     */
    public static List<Entry> shard(final int shard, final int shards, final Map<String, Integer> ticks,
                                    final Predicate<Entry> chosen) {
        final List<Entry> all = all().stream().filter(chosen).toList();
        final List<Entry> mine = new ArrayList<>();
        if (ticks.isEmpty()) {
            for (int i = 0; i < all.size(); i++) {
                if (i % shards == shard) {
                    mine.add(all.get(i));
                }
            }
            return mine;
        }
        final int unknown = (int) ticks.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        final List<Entry> longestFirst = new ArrayList<>(all);
        longestFirst.sort(Comparator.comparingInt((Entry e) -> -ticks.getOrDefault(e.name(), unknown))
                .thenComparing(Entry::name));
        final long[] load = new long[shards];
        for (final Entry entry : longestFirst) {
            int least = 0;
            for (int other = 1; other < shards; other++) {
                if (load[other] < load[least]) {
                    least = other;
                }
            }
            load[least] += ticks.getOrDefault(entry.name(), unknown);
            if (least == shard) {
                mine.add(entry);
            }
        }
        mine.sort(Comparator.comparingInt(Entry::index));
        return mine;
    }

    /** The ticks a slice is expected to take, from the same durations its split was made with. */
    public static long expectedTicks(final List<Entry> slice, final Map<String, Integer> ticks) {
        final int unknown = (int) ticks.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        return slice.stream().mapToLong(entry -> ticks.getOrDefault(entry.name(), unknown)).sum();
    }
}
