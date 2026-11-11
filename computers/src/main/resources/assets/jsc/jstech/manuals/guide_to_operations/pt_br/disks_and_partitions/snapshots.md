---
section: disks_and_partitions
order: 32
icon: jsc:disk_vaultis_trench_20m
---
# Snapshots

O btrfs e o ZFS podem guardar um snapshot de uma partição: os seus itens e os seus arquivos exatamente como estão. Voltar a ele devolve a partição a esse ponto, o que desfaz uma corrupção e tudo o que você perdeu desde então.

Um snapshot é tirado na hora e não pesa nada. À medida que a partição muda, ele pesa o que mudou desde que foi tirado, e isso ocupa espaço no disco como quaisquer itens. Exclua um snapshot de que já não precisa, para devolver o espaço.

> [!DEFINE] Snapshot
> Uma partição guardada como estava num momento, partilhando tudo o que não mudou desde então.

## btrfs, no Linux

btrfs subvolume snapshot tira um sob um caminho que você indica. O retorno espera pelo próximo arranque: set-default escolhe o snapshot, e a máquina monta-o como / quando arrancar de novo.

```session
{"lines": [{"prompt": "[player@fedora ~]$ ", "text": "btrfs subvolume snapshot / /.snapshots/day4", "typed": true}, {"text": "Create a snapshot of '/' in '/.snapshots/day4'"}, {"prompt": "[player@fedora ~]$ ", "text": "btrfs subvolume list /", "typed": true}, {"text": "ID 256 gen 1842 top level 5 path root"}, {"text": "ID 257 gen 1842 top level 5 path home"}, {"text": "ID 258 gen 1842 top level 5 path .snapshots/day4"}, {"prompt": "[player@fedora ~]$ ", "text": "btrfs subvolume set-default 258 /", "typed": true}], "look": "jsc:guide/terminal/bash", "title": "Fedora"}
```

btrfs filesystem du -s mostra o que um snapshot guarda, e em Exclusive o que só ele guarda: o espaço que excluí-lo devolve. Outra partição é indicada pelo seu dispositivo, como /dev/sdb1.

## ZFS, no FreeBSD

zfs snapshot tira um de um pool, com o nome depois de uma arroba, e zfs rollback volta a ele na hora. Enquanto houver snapshots mais novos guardados, o rollback recusa, e -r descarta-os.

```session
{"lines": [{"prompt": "root@freebsd:~ # ", "text": "zfs snapshot zroot/usr/home@day4", "typed": true}, {"prompt": "root@freebsd:~ # ", "text": "zfs list -t snapshot", "typed": true}, {"text": "NAME                    USED  AVAIL  REFER  MOUNTPOINT"}, {"text": "zroot/usr/home@day4       0B      -  41.2G  -"}, {"prompt": "root@freebsd:~ # ", "text": "zfs rollback zroot/usr/home@day4", "typed": true}], "look": "jsc:guide/terminal/sh", "title": "FreeBSD"}
```

O pool do próprio sistema é o zroot; outro tem o nome do rótulo da sua partição.

## Se algo der errado

> [!PROBLEM] O disco enche depois de um snapshot.
> Ele guarda o que você tirou desde então. Exclua os snapshots de que não precisa.

> [!PROBLEM] O comando diz que a partição não é btrfs.
> Só o btrfs e o ZFS guardam snapshots. Indique a partição pelo seu dispositivo se não for a raiz.

```command
id: jsc:command/linux/btrfs
```

```command
id: jsc:command/freebsd/zfs
```

> [!SEE] jsc:zfs_mirrors, jsc:corruption_and_the_defrag
