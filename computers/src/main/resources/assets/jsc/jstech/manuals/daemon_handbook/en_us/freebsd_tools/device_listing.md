---
section: freebsd_tools
order: 340
---
# pciconf and usbconfig: what is on the board

The board of your computer has two buses that carry its devices: the Peripheral Component Interconnect (PCI) bus, and the Universal Serial Bus (USB). The PCI bus holds the built-in parts and the expansion cards. The USB bus holds what you plug in. pciconf lists the PCI devices, and usbconfig lists the USB devices. Both commands are explained with the other hardware lists ([](jsc:devices_commands)); this page shows what FreeBSD prints.

> [!DEFINE] Driver name
> The name FreeBSD gives a device. The number counts the devices of that kind.

> [!DEFINE] ugen
> The name FreeBSD gives a USB device, such as ugen0.2.

Type pciconf -lv with both letters together, to list the devices on the PCI bus with their details. Type usbconfig with no words, to list the USB devices.

```table
{"caption": "The line of pciconf -l", "heads": ["Part", "What it tells you"], "rows": [[{"literal": "<driver name>@pci0:<bus>:<device>:<function>:"}, {"words": "The device's driver name, then its slot as bus, device and function."}], [{"literal": "class=0x<six hex digits>"}, {"words": "The kind of device, as a code."}], [{"literal": "device   = '<name>'"}, {"words": "With -lv: the device's name."}], [{"literal": "class    = <class name>"}, {"words": "With -lv: the kind of device in words."}]]}
```

## The devices of the board

Table: The devices pciconf lists

| Device | Driver name |
| --- | --- |
| Host bridge, in slot 00:00.0 | `hostb0` |
| Serial and parallel ports, or a USB controller, in slot 00:14.0 | uart0 for serial and parallel, ehci0 for USB 2, xhci0 for USB 3 |
| Audio: the board's own audio, in slot 00:1b.0 | `hdac0, hdac1` |
| Network: the first one, in slot 00:19.0 | `em0, em1` |
| Video | `vgapci0` |
| Processing cards | `none<n>` |

## usbconfig

usbconfig takes no options. Its first line names the controller, USB 2 controller or USB 3 controller, with its number of ports.

Each following line is a device, numbered as ugen0.2, ugen0.3 and so on, in the order the ports run. A hub is listed before the devices on it.

A device the computer disabled shows cfg=255. Such a device is not in use.

## If something goes wrong

> [!PROBLEM] pciconf says usage: pciconf -l [-v] after you typed -l -v.
> Type the two letters together: pciconf -lv.

> [!PROBLEM] usbconfig says invalid option.
> usbconfig takes no options. Type usbconfig alone.

> [!PROBLEM] A device shows cfg=255.
> The computer disabled the device, so it is not in use.

> [!PROBLEM] pciconf says no hardware to read.
> Run the command on the computer whose parts you want to list.

> [!SEE] jsc:devices_commands, jsc:system_commands
