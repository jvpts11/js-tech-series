---
section: how_things_are_counted
order: 13
icon: jsc:disk_vaultis_trench_20m
---
# Itens como dados

A rede guarda seus itens nos discos dos computadores dela, como dados. Os discos dela contam itens, não espaço bruto. Um balde de fluido ocupa tanto espaço quanto um item.

Quanto espaço um item ocupa depende da era do disco, porque computadores mais antigos tinham palavras menores: 1 MB na era Vintage, 16 MB na era Legacy e 256 MB a partir da era Transition. Assim, uma unidade Vintage de 20 MB guarda 20 itens, e um disco de 1 terabyte (TB) guarda 4.096 itens a partir da era Transition.

> [!DEFINE] Tamanho do item
> O espaço que um item ocupa num disco: 1 MB na era Vintage, 16 MB na era Legacy, 256 MB a partir da era Transition.

Table: Tamanhos de disco em itens, a partir da era Transition

| Disco | Itens |
| --- | --- |
| 500 GB | 2.000 itens |
| 1 TB | 4.096 itens |
| 2 TB | 8.192 itens |
| 4 TB | 16.384 itens |
| 8 TB | 32.768 itens |

Acima de 8 TB, só são fabricados discos rígidos, até 24 TB, que comportam 98.304 itens.

A memória também funciona em itens, do seu jeito. Cada módulo de memória guarda um buffer de itens para o Mainframe, um item para cada 4 MB do tamanho do módulo ([](jsc:memory)). Um buffer pequeno demais para o processador deixa o processador esperando pela memória.

Os discos diferem em velocidade. Uma unidade de estado sólido (SSD) é 4 vezes mais rápida que um disco rígido, e um disco NVMe é 16 vezes mais rápido ([](jsc:disks)).

> [!SEE] jsc:disks, jsc:units_and_ticks, jsc:fluids
