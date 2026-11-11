---
section: shell_commands
order: 256
---
# Discos, swap e snapshots

Estes comandos leem os discos e as suas partições, dão um sistema de arquivos a uma partição, mostram o swap e guardam snapshots. Cada sistema tem os seus, que você digita no prompt dele: o que são uma partição e um sistema de arquivos está em [](jsc:partitions).

## Linux

fdisk -l imprime a tabela de partições de cada disco, e lsblk lista os discos com as suas partições em árvore; lsblk -f acrescenta o sistema de arquivos de cada uma. Cada mkfs formata uma partição com o sistema de arquivos do seu nome, e recusa aquela de onde o sistema roda. Para mudar as próprias partições, use o KDE Partition Manager ou o Discos do desktop.

```command
id: jsc:command/linux/fdisk
```

```command
id: jsc:command/linux/lsblk
```

```command
id: jsc:command/linux/mkfs.ext3
```

```command
id: jsc:command/linux/mkfs.ext2
```

```command
id: jsc:command/linux/mkfs.xfs
```

```command
id: jsc:command/linux/mkfs.btrfs
```

```command
id: jsc:command/linux/mkfs.vfat
```

```command
id: jsc:command/linux/mkfs.exfat
```

```command
id: jsc:command/linux/mkfs.ntfs
```

free -h mostra a memória e o swap, swapon --show as partições de swap, e top o que está rodando com o que disso está no swap ([](jsc:swap)). O btrfs tira e guarda os snapshots de uma partição btrfs ([](jsc:snapshots)).

```command
id: jsc:command/linux/free
```

```command
id: jsc:command/linux/swapon
```

```command
id: jsc:command/linux/top
```

```command
id: jsc:command/linux/btrfs
```

## FreeBSD

gpart show imprime as tabelas de partições. O zfs tira, lista e faz rollback dos snapshots de um pool ZFS, e o zpool faz o scrub de um pool e mostra o que o seu espelho consertou ([](jsc:zfs_mirrors)). swapinfo e sysctl vm.swap_total mostram o swap.

```command
id: jsc:command/freebsd/gpart
```

```command
id: jsc:command/freebsd/zfs
```

```command
id: jsc:command/freebsd/zpool
```

```command
id: jsc:command/freebsd/swapinfo
```

```command
id: jsc:command/freebsd/sysctl
```

## OpenBSD e UNIX

No OpenBSD, o fdisk imprime o Master Boot Record e o disklabel -h as partições do próprio sistema dentro dele. No UNIX System V, o fdisk abre um menu que cria e exclui partições e define a ativa, aquela que a máquina arranca.

```command
id: jsc:command/openbsd/fdisk
```

```command
id: jsc:command/openbsd/disklabel
```

```command
id: jsc:command/unix/fdisk
```

## Se algo der errado

> [!PROBLEM] O mkfs diz que a partição está montada.
> É a partição de onde o sistema roda. Formate outra, ou trabalhe a partir de uma mídia ao vivo.

> [!PROBLEM] O zfs diz que o dataset não existe.
> Indique o pool como o zpool status o mostra: zroot para o do próprio sistema.

> [!SEE] jsc:partitions, jsc:disk_tools_of_each_system, jsc:file_commands
