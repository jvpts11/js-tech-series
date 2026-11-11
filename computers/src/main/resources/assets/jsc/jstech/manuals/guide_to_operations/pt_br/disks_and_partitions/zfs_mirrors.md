---
section: disks_and_partitions
order: 33
icon: jsc:disk_vaultis_trench_20m
---
# Espelhos ZFS

Um pool ZFS pode viver em dois discos ao mesmo tempo, cada um com o mesmo conteúdo. Quando a corrupção danifica parte do pool, o ZFS a conserta a partir do outro disco: assim que essa parte é lida, e quando você faz o scrub do pool, que o lê todo.

> [!DEFINE] Espelho
> Um pool guardado inteiro em dois discos, para que cada um conserte o outro.

> [!DEFINE] Scrub
> Ler um pool inteiro para achar e consertar o que está danificado.

O instalador do FreeBSD cria um espelho: escolha Auto (ZFS), depois o segundo disco no passo ZFS Configuration. O segundo disco não é um volume à parte; é de onde o pool se conserta.

```session
{"lines": [{"prompt": "root@freebsd:~ # ", "text": "zpool scrub zroot", "typed": true}, {"prompt": "root@freebsd:~ # ", "text": "zpool status zroot", "typed": true}, {"text": "  pool: zroot"}, {"text": " state: ONLINE"}, {"text": "  scan: scrub repaired 8M in 00:00:00 with 0 errors on Day 4 10:12"}, {"text": "config:"}, {"text": ""}, {"text": "        NAME        STATE     READ WRITE CKSUM"}, {"text": "        zroot       ONLINE       0     0     0"}, {"text": "          mirror-0  ONLINE       0     0     0"}, {"text": "            ada0p4  ONLINE       0     0     2  (repaired)"}, {"text": "            ada1p4  ONLINE       0     0     0"}, {"text": ""}, {"text": "errors: No known data errors"}], "look": "jsc:guide/terminal/sh", "title": "FreeBSD"}
```

CKSUM conta quantas vezes o pool achou dano e o consertou. A linha scan diz o que o último scrub consertou, e quando.

Sem o seu segundo disco o pool fica DEGRADED: continua a funcionar, acha o dano e não tem de onde consertá-lo. A cura passa então a ser só o Defrag, ou um snapshot.

## Se algo der errado

> [!PROBLEM] zpool status diz DEGRADED.
> O segundo disco não está na máquina, ou morreu. Coloque-o de volta.

> [!PROBLEM] errors lista erros de dados depois de um scrub.
> O pool não tem segundo disco de onde consertar. Rode o Defrag, ou volte a um snapshot.

```command
id: jsc:command/freebsd/zpool
```

> [!SEE] jsc:snapshots, jsc:corruption_and_the_defrag
