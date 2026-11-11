---
section: freebsd_tools
order: 340
---
# pciconf e usbconfig: o que há na placa

A placa do seu computador tem dois barramentos que levam os seus dispositivos: o barramento Peripheral Component Interconnect (PCI) e o barramento Universal Serial Bus (USB). O barramento PCI guarda as peças embutidas e as placas de expansão. O barramento USB guarda o que você conecta. O pciconf lista os dispositivos PCI, e o usbconfig lista os dispositivos USB. Os dois comandos são explicados junto com as outras listas de hardware ([](jsc:devices_commands)); esta página mostra o que o FreeBSD imprime.

> [!DEFINE] Nome do driver
> O nome que o FreeBSD dá a um dispositivo. O número conta os dispositivos desse tipo.

> [!DEFINE] ugen
> O nome que o FreeBSD dá a um dispositivo USB, como ugen0.2.

Digite pciconf -lv com as duas letras juntas, para listar os dispositivos do barramento PCI com seus detalhes. Digite usbconfig sem nenhuma opção, para listar os dispositivos USB.

```table
{"caption": "A linha de pciconf -l", "heads": ["Parte", "O que ela mostra"], "rows": [[{"literal": "<driver name>@pci0:<bus>:<device>:<function>:"}, {"words": "O nome do driver do dispositivo, depois o seu slot como barramento, dispositivo e função."}], [{"literal": "class=0x<six hex digits>"}, {"words": "O tipo de dispositivo, como código."}], [{"literal": "device   = '<name>'"}, {"words": "Com -lv: o nome do dispositivo."}], [{"literal": "class    = <class name>"}, {"words": "Com -lv: o tipo de dispositivo por extenso."}]]}
```

## Os dispositivos da placa-mãe

Table: Os dispositivos que o pciconf lista

| Dispositivo | Nome do driver |
| --- | --- |
| Ponte host, no slot 00:00.0 | `hostb0` |
| Portas seriais e paralelas, ou um controlador USB, no slot 00:14.0 | uart0 para serial e paralela, ehci0 para USB 2, xhci0 para USB 3 |
| Áudio: o áudio próprio da placa, no slot 00:1b.0 | `hdac0, hdac1` |
| Rede: a primeira, no slot 00:19.0 | `em0, em1` |
| Vídeo | `vgapci0` |
| Placas de processamento | `none<n>` |

## usbconfig

usbconfig não aceita opções. A sua primeira linha nomeia o controlador, controlador USB 2 ou controlador USB 3, com o seu número de portas.

Cada linha seguinte é um dispositivo, numerado como ugen0.2, ugen0.3 e assim por diante, na ordem em que as portas se sucedem. Um hub aparece listado antes dos dispositivos nele.

Um dispositivo que o computador desativou mostra cfg=255. Esse dispositivo não está em uso.

## Se algo der errado

> [!PROBLEM] pciconf diz usage: pciconf -l [-v] depois que você digitou -l -v.
> Digite as duas letras juntas: pciconf -lv.

> [!PROBLEM] usbconfig diz opção inválida.
> usbconfig não aceita opções. Digite usbconfig sozinho.

> [!PROBLEM] Um dispositivo mostra cfg=255.
> O computador desativou o dispositivo, então ele não está em uso.

> [!PROBLEM] pciconf diz nenhum hardware para ler.
> Execute o comando no computador cujas peças você quer listar.

> [!SEE] jsc:devices_commands, jsc:system_commands
