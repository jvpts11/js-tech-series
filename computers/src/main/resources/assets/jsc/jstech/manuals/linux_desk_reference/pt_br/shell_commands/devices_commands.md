---
section: shell_commands
order: 255
---
# Listas de hardware: lspci, lsusb, pciconf e usbconfig

Um computador tem peças no seu barramento Peripheral Component Interconnect (PCI), o barramento em que ficam as placas de expansão e as peças embutidas. Ele tem dispositivos no seu Universal Serial Bus (USB), o barramento para dispositivos conectados.

> [!DEFINE] PCI
> Peripheral Component Interconnect: o barramento da placa em que ficam as placas de expansão e as peças embutidas.

> [!DEFINE] USB
> Universal Serial Bus: o barramento para dispositivos conectados.

Cada lugar em um barramento é um slot, e um slot é escrito como barramento:dispositivo:função. Os comandos abaixo listam o que está nesses slots. O Linux tem lspci e lsusb, e o FreeBSD tem pciconf e usbconfig.

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

O comando devices é uma lista diferente. Ele mostra os periféricos ligados ao seu computador por link, e está na página de comandos do sistema ([](jsc:system_commands)).

## Se algo der errado

> [!PROBLEM] O comando diz nenhum hardware para ler.
> Execute-o no computador cujas peças você quer listar.

> [!PROBLEM] O shell diz que lspci não foi encontrado no FreeBSD.
> Use pciconf no FreeBSD. O comando lspci é um comando do Linux.

> [!SEE] jsc:system_commands, jsc:file_commands
