---
section: disks_and_partitions
order: 30
icon: jsc:disk_vaultis_trench_20m
---
# As ferramentas de disco

Cada sistema tem as suas ferramentas para criar, formatar, redimensionar e excluir partições. Todas leem os mesmos discos, por isso nunca discordam sobre eles.

Table: As ferramentas por sistema

| Sistema | Numa janela | No prompt |
| --- | --- | --- |
| MC-DOS | \- | `FDISK` |
| Frames XP ao 11 | Gerenciamento de Disco | `diskpart` |
| Linux com KDE | KDE Partition Manager | `fdisk -l, lsblk, mkfs` |
| Outros desktops Linux | Discos | `fdisk -l, lsblk, mkfs` |
| UNIX System V | \- | `fdisk` |
| FreeBSD | \- | `gpart show` |
| OpenBSD | \- | `fdisk, disklabel -h` |

O Gerenciamento de Disco mostra cada volume numa lista e cada disco como uma barra. Clique com o botão direito num volume para formatá-lo, diminuí-lo, estendê-lo ou excluí-lo, e no espaço livre para criar um novo. No Frames XP ele não redimensiona volumes. O KDE Partition Manager guarda as suas mudanças como operações pendentes e só as grava quando você pressiona Aplicar. O Discos mostra um disco de cada vez, com os seus volumes numa barra.

## O que toda ferramenta recusa

A partição de onde o sistema em execução roda, e um swap que ele usa, nunca são excluídos nem formatados. Uma partição nunca encolhe abaixo do que guarda, e cada sistema de arquivos redimensiona pelas suas regras, na tabela de [](jsc:partitions): uma partição ext4 em uso só cresce. Um disco só passa de MBR para GPT, ou de volta, quando está vazio, e uma nova tabela de partições apaga o disco todo.

1. Abra o Gerenciamento de Disco, ou a ferramenta do seu sistema.
2. Clique com o botão direito no volume e escolha Diminuir Volume.
3. Digite quantos megabytes liberar e pressione Diminuir.
4. Clique com o botão direito no espaço livre e crie nele um novo volume.

## No prompt

O FDISK do MC-DOS e o fdisk do UNIX abrem um menu: criar uma partição, definir a ativa, excluir uma, ou mostrá-las. O diskpart do Frames recebe comandos no seu próprio prompt: "list disk", "select disk 1", depois "create partition primary", "format fs=ntfs quick" e "exit". No Linux, mkfs.ext4 /dev/sdb1 formata uma partição, e recusa aquela de onde o sistema roda.

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "mkfs.ext4 /dev/sda1", "typed": true}, {"text": "/dev/sda1 is mounted; will not make a filesystem here!"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

## Se algo der errado

> [!PROBLEM] A ferramenta diz que o volume está em uso.
> É a partição do próprio sistema, ou o seu swap. Mude-a a partir de uma mídia ao vivo ou de outro sistema.

> [!PROBLEM] Converter em Disco GPT está acinzentado.
> O disco ainda tem partições. Exclua-as primeiro, ou grave uma nova tabela, o que apaga o disco.

```command
id: jsc:command/frames/diskpart
```

```command
id: jsc:command/mc_dos/fdisk
```

```command
id: jsc:command/linux/fdisk
```

```command
id: jsc:command/linux/lsblk
```

```command
id: jsc:command/linux/mkfs.ext4
```

```command
id: jsc:command/unix/fdisk
```

```command
id: jsc:command/freebsd/gpart
```

```command
id: jsc:command/openbsd/disklabel
```

> [!SEE] jsc:partitions, jsc:two_systems_on_a_disk
