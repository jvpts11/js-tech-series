---
section: disk_tools
order: 307
---
# Montando os discos

Você não consegue acessar os arquivos de um sistema de arquivos enquanto ele não estiver montado: ligado a uma pasta, para que você alcance os arquivos por meio dela. O novo sistema é construído dentro de /mnt no Arch, ou dentro de /mnt/gentoo no Gentoo, e os discos são montados ali.

Monte a raiz primeiro. Uma montagem que funciona não imprime nada, e é assim que a ferramenta diz sim. A forma é mount, a partição e depois a pasta: o /dev/ na frente do nome é opcional.

Table: Os lugares onde uma montagem pode ir

| Partição | Ponto de montagem, no Arch | Ponto de montagem, no Gentoo |
| --- | --- | --- |
| A raiz | `/mnt` | `/mnt/gentoo` |
| A partição de sistema EFI | `/mnt/boot` | `/mnt/gentoo/efi` |

A ferramenta aceita a pasta raiz, e a pasta boot ou efi dentro da raiz, escritas exatamente como mostrado, sem barra no final. A partição de sistema EFI precisa ser montada depois da raiz.

## Os passos no Arch

1. Digite mount /dev/sda2 /mnt e pressione [[Enter]], para montar a raiz.
2. Digite mount --mkdir /dev/sda1 /mnt/boot e pressione [[Enter]], para montar a partição de sistema EFI.

## Os passos no Gentoo

1. Digite mount /dev/sda2 /mnt/gentoo e pressione [[Enter]], para montar a raiz.
2. Digite mount /dev/sda1 /mnt/gentoo/efi e pressione [[Enter]], para montar a partição de sistema EFI.

A ferramenta cria a pasta da partição de sistema EFI de qualquer forma, então você pode omitir --mkdir. Ela ignora essa bandeira se você a digitar, e ignora a palavra de opção depois de -o, como noatime.

## Fazendo o bind das pastas da mídia

A lista de verificação do Gentoo pede que quatro pastas da mídia ao vivo sejam vinculadas ao novo sistema antes de você entrar nele: /proc, /sys, /dev e /run. O novo sistema precisa delas enquanto é configurado. Você faz o bind com o tipo proc, ou com a flag --rbind ou --bind.

Um bind vem depois que a raiz é montada, e primeiro nomeia a pasta da mídia, depois o lugar no novo sistema. Por exemplo, mount --types proc /proc /mnt/gentoo/proc faz o bind de /proc. Cada bind é silencioso quando funciona.

> [!DEFINE] Bind mount
> Uma pasta da mídia ao vivo que reaparece dentro do novo sistema, com o mesmo conteúdo.

## Se algo der errado

> [!PROBLEM] mount: uso incorreto
> Você digitou menos de duas palavras. Digite mount /dev/sda2 /mnt, com a partição e a pasta.

> [!PROBLEM] mount: /mnt/boot: mount point does not exist.
> A raiz ainda não está montada, e a partição de sistema EFI vem depois dela. Monte a raiz primeiro com mount /dev/sda2 /mnt.

> [!PROBLEM] mount: /mnt/boot: wrong fs type on /dev/sda1.
> Essa partição não é a partição FAT que a ferramenta espera. Execute antes mkfs.fat -F 32 /dev/sda1, e confira o nome.

> [!PROBLEM] mount: /mnt: wrong fs type, bad option, bad superblock on /dev/sda2.
> A raiz ainda não está formatada como ext4. Execute primeiro mkfs.ext4 /dev/sda2.

> [!PROBLEM] mount: /mnt: mount point does not exist.
> Você tentou fazer o bind de uma pasta antes de montar a raiz. Monte a raiz primeiro e depois faça o bind.

> [!PROBLEM] mount: /mnt/data: mount point does not exist.
> Essa pasta não é /mnt, /mnt/gentoo, nem a pasta boot ou efi deles. Use uma dessas.

> [!SEE] jsc:arch_chroot, jsc:arch_fstab, jsc:when_a_tool_refuses
