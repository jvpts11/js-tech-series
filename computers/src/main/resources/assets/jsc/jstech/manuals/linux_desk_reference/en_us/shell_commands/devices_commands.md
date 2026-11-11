---
section: shell_commands
order: 255
---
# Hardware lists: lspci, lsusb, pciconf and usbconfig

A computer has parts on its Peripheral Component Interconnect (PCI) bus, the bus that expansion cards and built-in parts sit on. It has devices on its Universal Serial Bus (USB), the bus for plugged-in devices.

> [!DEFINE] PCI
> Peripheral Component Interconnect: the bus on the board that expansion cards and built-in parts sit on.

> [!DEFINE] USB
> Universal Serial Bus: the bus for plugged-in devices.

Each place on a bus is a slot, and a slot is written as bus:device:function. The commands below list what sits in those slots. Linux has lspci and lsusb, and FreeBSD has pciconf and usbconfig.

```command
id: jsc:command/linux/lspci
```

```command
id: jsc:command/linux/lsusb
```

```command
id: jsc:command/freebsd/pciconf
```

```command
id: jsc:command/freebsd/usbconfig
```

The devices command is a different list. It shows the peripherals linked to your computer, and it is on the system commands page ([](jsc:system_commands)).

## If something goes wrong

> [!PROBLEM] The command says no hardware to read.
> Run it on the computer whose parts you want to list.

> [!PROBLEM] The shell says lspci is not found on FreeBSD.
> Use pciconf on FreeBSD. The lspci command is a Linux command.

> [!SEE] jsc:system_commands, jsc:file_commands
