---
section: disks_and_partitions
order: 34
icon: jsc:disk_vaultis_trench_20m
---
# Swap e o arquivo de paginação

Quando os programas querem mais memória do que a máquina tem, o sistema empresta-lhes espaço em disco: o swap no Linux, nos BSD e no UNIX, e o arquivo de paginação no Frames. Uma máquina promete a sua memória e o seu swap juntos, por isso cabem mais programas.

A parte de um programa que fica no swap roda mais devagar, conforme o disco em que você põe o swap.

Table: A que velocidade roda a parte no swap

| Disco | Velocidade |
| --- | --- |
| Disco rígido | Um quarto da sua velocidade |
| SSD | 60% da sua velocidade |
| NVMe | 90% da sua velocidade |

O Frames dimensiona o seu arquivo de paginação ao arrancar: uma vez e meia a memória no Frames 95 e no XP, e o mesmo que a memória do Frames 7 em diante. O arquivo de paginação pesa no disco do sistema como os seus arquivos. Os outros sistemas fazem swap para uma partição de swap, que os seus instaladores criam do tamanho da memória.

> [!DEFINE] Swap
> Espaço em disco que um sistema empresta aos seus programas quando a memória acaba.

> [!DEFINE] Arquivo de paginação
> O swap do Frames: um arquivo no disco do sistema.

## Onde vê-lo

Table: O swap em cada sistema

| Sistema | Onde |
| --- | --- |
| Frames XP | A guia Desempenho do Gerenciador de Tarefas: Uso do Arquivo de Paginação, e a Carga na barra de status |
| Frames 10 e 11 | A página Memória do Gerenciador de Tarefas, com o que cada programa tem no arquivo de paginação |
| Desktops Linux | A linha de swap do Monitor do Sistema |
| Linux | `free -h, swapon --show, top` |
| FreeBSD | `swapinfo, sysctl vm.swap_total` |

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "swapon --show", "typed": true}, {"text": "NAME      TYPE      SIZE USED PRIO"}, {"text": "/dev/sda2 partition    4G   0B   -2"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

## Se algo der errado

> [!PROBLEM] Os programas ficam lentos quando há muitos abertos.
> Parte deles está no swap. Acrescente memória, ou ponha o swap num disco mais rápido.

> [!PROBLEM] Um programa é recusado por falta de memória.
> A memória e o swap estão ambos prometidos. Feche um programa, ou acrescente memória.

```command
id: jsc:command/linux/free
```

```command
id: jsc:command/linux/swapon
```

```command
id: jsc:command/freebsd/swapinfo
```

> [!SEE] jsc:memory, jsc:system_monitor_and_task_manager
