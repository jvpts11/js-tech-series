---
section: disk_tools
order: 308
---
# O que significa cada erro de disco

A maioria das mensagens começa com o nome da ferramenta, como fdisk, mkfs.ext4 ou mount. Quando uma ferramenta parar você, encontre a mensagem dela nas tabelas abaixo. Uma mensagem em frase comum, como Dispositivo não contém uma tabela de partições reconhecida, é informação e não uma recusa.

O lsblk nunca recusa nada. Ele sempre mostra a tabela.

Table: As mensagens do fdisk

| Mensagem | O que significa, e a solução |
| --- | --- |
| `fdisk: bad usage` | Nenhum disco foi informado: digite fdisk /dev/sda. |
| `fdisk: cannot open /dev/sda2: No such file or directory` | Nenhum disco com esse nome está no computador: use um nome de disco do lsblk, não um nome de partição. |
| `Device does not contain a recognized partition table.` | Informação: o disco ainda não tem partições, então digite g para um GPT. |
| `No partition is defined yet!` | t ou d precisa de uma partição: crie uma com n primeiro. |
| `No free sectors available.` | O disco está cheio, uma partição anterior ficou com o resto, ou há 128 partições: apague a última com d. |
| `Value out of range.` | O tamanho ou o número está fora do que o disco tem: use um tamanho menor ou um número da lista. |
| `Failed to parse partition type 'swap'.` | A palavra do tipo não é conhecida: digite 1 para EFI System, 20 para Linux filesystem, ou L para a lista. |
| `x: unknown command` | A letra x não é um comando do editor: digite m para a lista. |
| `Changed type of partition 'Linux filesystem' to 'EFI System'.` | Sucesso, sem nada a corrigir. |

Table: As mensagens do mkfs.ext4

| Mensagem | O que significa, e a solução |
| --- | --- |
| `Usage: mkfs.ext4 /dev/<device>` | Nenhuma partição foi informada: informe uma, como /dev/sda2. |
| `mke2fs: No such file or directory while trying to determine filesystem size` | A partição ainda não existe: crie-a com fdisk, ou confira o número dela. |
| `mke2fs: sda1 is the EFI System partition.` | A partição de sistema EFI foi informada: use mkfs.fat -F 32 nela, e mkfs.ext4 em outra partição. |
| `/dev/sda contains a gpt partition table.` | O disco inteiro foi informado, e ele tem uma tabela de partições: use uma partição, como sda2. |

Table: As mensagens do mkfs.fat

| Mensagem | O que significa, e a solução |
| --- | --- |
| `Usage: mkfs.fat -F 32 /dev/<partition>` | Nenhuma partição foi informada: informe uma, como /dev/sda1. |
| `mkfs.fat: unable to open sda1: No such file or directory` | A partição não existe: crie-a com fdisk, ou confira o número dela. |
| `mkfs.fat: sda1 is not an EFI System partition.` | O tipo dela ainda não é EFI System: rode fdisk, depois t, depois 1, depois w, e tente de novo. |

Table: As mensagens do mount

| Mensagem | O que significa, e a solução |
| --- | --- |
| `mount: bad usage` | Foram informadas menos de duas palavras: informe uma partição e uma pasta. |
| `mount: /mnt/boot: mount point does not exist.` | A raiz ainda não está montada, então monte-a primeiro. |
| `mount: /mnt/data: mount point does not exist.` | A pasta não é um lugar que pode ser montado: use /mnt, ou a pasta boot ou efi dentro dela. |
| `mount: /mnt/boot: wrong fs type on /dev/sda1.` | A partição de sistema EFI não está formatada como FAT, então rode mkfs.fat -F 32 nela primeiro. |
| `mount: /mnt: wrong fs type, bad option, bad superblock on /dev/sda2.` | A partição não é a que o mkfs.ext4 formatou por último, então rode mkfs.ext4 nela primeiro. |

Um mount ou um bind que funciona não mostra nada, então o silêncio é o sucesso dele.

## Se algo der errado

> [!PROBLEM] A mesma mensagem volta depois que você corrige algo.
> Rode lsblk e compare o nome que você digitou com o nome que ele mostra. O nome segue o slot em que o disco está.

> [!PROBLEM] A mensagem não está nestas tabelas.
> Encontre o nome da ferramenta no início da mensagem, leia a tabela dessa ferramenta e confira o nome com lsblk.

> [!SEE] jsc:seeing_the_disks, jsc:partitioning_with_fdisk, jsc:mounting
