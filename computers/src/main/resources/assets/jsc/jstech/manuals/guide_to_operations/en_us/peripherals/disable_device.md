---
section: peripherals
order: 78
---
# Disabling a device

You can disable a device, so that its computer stops using it. The device keeps its port, and enabling it again brings it back. You disable a device when you want the computer to stop reading it, or writing to it.

> [!DEFINE] Disable
> To stop a computer from using a device. The device keeps its port until it is enabled again.

## In the Device Manager

On Frames, the Device Manager ([](jsc:device_manager)) lists the devices by type, by connection or by port.

1. Open the Device Manager.
2. Select the device.
3. Press Disable, or Enable if the device is already disabled.

The status line says that the device is disabled, and that the computer stops reading and writing it. If a device is missing from the list, look for it in another view, because the Show hidden devices option does nothing.

## On MC-DOS and MC-NET

MC-DOS and MC-NET have no Device Manager. Their Midsoft Diagnostics program, MSD ([](jsc:dos_msd)), takes the whole terminal. Press L for the LPT ports, or C for the COM ports. LPT and COM are the old names of the parallel and the serial ports.

Table: The LPT and COM Ports list

| Column | What it shows |
| --- | --- |
| Port | The name of the port. |
| Attached | What is plugged into the port. |
| Status | On, or Off (disabled). |

In the list, press E to enable the device on the port you picked, or D to disable it. Enter puts the list away, and F3 exits MSD.

## On the Common Desktop Environment (CDE) and KDE

On CDE, the Workstation Info window has a Devices... button ([](jsc:cde_workstation_info)). In its Devices dialog, select a device, then press Disable or Enable.

On KDE, the Info Center page Devices by port has a button for the selected device, which disables or enables it ([](jsc:kde_plasma)).

A disabled device goes quiet. A monitor shows "No signal - the computer has disabled this monitor", and a Redstone Interface reads and emits nothing. The computer also stops reading a drive, or the disks of the Dock Station.

## If something goes wrong

> [!PROBLEM] A device is dark, or not seen, after you disabled it.
> Enable it again: Enable in the Device Manager or in Workstation Info, or E in the MSD list.

> [!PROBLEM] The Device Manager does not list a device.
> Look for it in another view of the list, because the Show hidden devices option does nothing.

> [!SEE] jsc:device_manager, jsc:dos_msd, jsc:cde_workstation_info, jsc:kde_plasma
