---
section: disks_and_partitions
order: 29
icon: jsc:disk_vaultis_trench_20m
---
# Partições e sistemas de arquivos

Um disco pode ser dividido em partições. Cada partição é uma parte do disco com um sistema de arquivos próprio, e cada uma que tem um sistema de arquivos é um volume à parte: o Frames dá-lhe uma letra de unidade, o Linux um nome como /dev/sdb1, e a rede guarda itens nela como num disco.

Um disco que ninguém dividiu é lido como uma só partição sobre ele todo. Instalar um sistema, ou uma ferramenta de disco, grava a tabela que lista as partições.

> [!DEFINE] Tabela de partições
> A lista no início de um disco que diz onde cada partição começa e termina.

Table: Os dois tipos de tabela

| Tabela | Gravada em | O que comporta |
| --- | --- | --- |
| Master Boot Record (MBR) | Da Vintage à Transition | Quatro partições no máximo, nos primeiros 2 TB do disco. Uma delas é a ativa: a que a máquina arranca. |
| GUID Partition Table (GPT) | Standard e posteriores | Tantas partições quantas couberem no disco, sobre ele todo. |

Uma máquina que arranca com UEFI, do Standard em diante, lê os seus sistemas de uma partição EFI: 512 MB de FAT32 que todo instalador cria primeiro num disco GPT vazio. Ela guarda o que arranca os sistemas, e nunca é um volume.

> [!DEFINE] Partição EFI
> A pequena partição FAT32 de onde uma máquina UEFI arranca os seus sistemas.

## Sistemas de arquivos

Cada partição é formatada com um sistema de arquivos. Um sistema só lê os sistemas de arquivos que conhece, e uma máquina só cria os da sua era ou mais antigos.

Table: Os sistemas de arquivos

| Sistema de arquivos | Desde | Lido por | Redimensionar |
| --- | --- | --- | --- |
| `FAT12, FAT16` | Vintage | Todos os sistemas | Só desmontada |
| `FAT32` | Legacy | Todos os sistemas | Só desmontada |
| `NTFS` | Legacy | Frames; Linux desde a Transition | Cresce e encolhe em uso |
| `exFAT` | Transition | Frames; Linux desde a Advanced | Só desmontada |
| `ext2, ext3` | Legacy | Linux | Cresce em uso; encolhe desmontada |
| `ext4` | Transition | Linux | Cresce em uso; encolhe desmontada |
| `XFS` | Transition | Linux | Só cresce |
| `btrfs` | Standard | Linux | Cresce e encolhe em uso |
| `UFS` | Vintage | UNIX, FreeBSD, OpenBSD | Só desmontada |
| `ZFS` | Transition | FreeBSD | Só cresce |

Uma partição FAT16 tem no máximo 2 GB, e um arquivo em FAT tem no máximo 4 GB. O btrfs e o ZFS guardam snapshots ([](jsc:snapshots)), e o ZFS espelha um pool em dois discos ([](jsc:zfs_mirrors)). Uma partição de swap não tem sistema de arquivos: é para onde a memória transborda ([](jsc:swap)).

## Se algo der errado

> [!PROBLEM] Uma partição não aparece como unidade.
> O sistema em execução não lê o seu sistema de arquivos. Formate-a com um que o sistema leia.

> [!PROBLEM] A ferramenta não cria o sistema de arquivos que você escolheu.
> Ele é de uma era posterior à da máquina.

> [!SEE] jsc:disks, jsc:disk_tools_of_each_system, jsc:local_storage
