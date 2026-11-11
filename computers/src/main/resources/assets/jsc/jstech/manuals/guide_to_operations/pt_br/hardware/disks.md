---
section: hardware
order: 22
icon: jsc:disk_vaultis_trench_20m
covers_family: jsc:disks
---
# Discos

Um disco guarda o que precisa sobreviver ao desligar o computador: o seu sistema, os seus programas e os seus arquivos. O tamanho de um disco é contado em itens, como explica [Itens como dados](jsc:items_as_data).

Há três tipos de disco, e eles diferem em velocidade. O disco rígido é o mais lento, o SSD é quatro vezes mais rápido, e o NVMe é dezesseis vezes mais rápido.

> [!DEFINE] Disco rígido (HDD)
> O tipo de disco mais lento daqui.

> [!DEFINE] Unidade de estado sólido (SSD)
> Um disco quatro vezes mais rápido que um disco rígido.

> [!DEFINE] NVMe
> Um padrão de armazenamento rápido, abreviação de Non-Volatile Memory Express. É o tipo de disco mais rápido daqui.

> [!DEFINE] Latência
> Os ticks que um disco espera antes de começar uma transferência. A tabela mostra os padrões.

```table
{"caption": "Os tipos de disco", "heads": ["Tipo", "Velocidade", "Espera antes de uma transferência (padrão)", "Energia"], "rows": [[{"words": "Disco rígido (HDD)"}, {"literal": "x1"}, {"words": "10 ticks"}, {"amount": 6, "unit": "W"}], [{"words": "Unidade de estado sólido (SSD)"}, {"literal": "x4"}, {"words": "3 ticks"}, {"amount": 3, "unit": "W"}], [{"words": "NVMe"}, {"literal": "x16"}, {"words": "1 tick"}, {"amount": 5, "unit": "W"}]]}
```

O tamanho de um disco é contado em itens da sua era. Uma unidade Vintage de 20 MB guarda 20 itens. Um disco de 1 TB de uma era posterior guarda 4.096 itens ([Unidades e ticks](jsc:units_and_ticks)).

Table: Tamanhos de disco

| Tamanho | Itens | Tipos em que vem |
| --- | --- | --- |
| 500 GB | 2.000 itens | Disco rígido, SSD e NVMe |
| 1 TB | 4.096 itens | Disco rígido, SSD e NVMe |
| 2 TB | 8.192 itens | Disco rígido, SSD e NVMe |
| 4 TB | 16.384 itens | Disco rígido, SSD e NVMe |
| 8 TB | 32.768 itens | Disco rígido, SSD e NVMe |
| 12 TB | 49.152 itens | Somente disco rígido |
| 16 TB | 65.536 itens | Somente disco rígido |
| 20 TB | 81.920 itens | Somente disco rígido |
| 24 TB | 98.304 itens | Somente disco rígido |

Os discos Vintage são unidades Vaultis Trench HDD de 20M, 100M e 200M. Os discos rígidos Legacy são unidades Vaultis Link IDE-HDD de 4G, 20G e 40G, e o SSD Transition é um Vaultis Link SATA-SSD de 64G.

Um disco serve a um computador da sua própria era ou de uma era posterior. Um disco mais novo que o computador é recusado, num compartimento de disco, na baia de troca a quente e na baia de unidades de um rack, e o compartimento diz de que era o disco precisa quando você o segura sobre ele. A Dock Station ([](jsc:dock_station)) aceita um disco da Standard ou de uma era mais antiga.

Os compartimentos de disco da placa contam os discos que você coloca neles. Um disco na baia de troca a quente não ocupa nenhum desses compartimentos. Cada disco também consome energia, como mostra a tabela.

## Se algo der errado

> [!PROBLEM] O disco não entra no compartimento de disco.
> É mais novo que o computador. Use um disco da era do computador ou de uma era anterior.

> [!PROBLEM] A montagem diz que há discos demais.
> Mova os discos para a Baia Hot-Swap de Disco, ou use uma placa com mais compartimentos de disco ([as recusas de montagem](jsc:build_refusals)).

> [!SEE] jsc:case_front, jsc:items_as_data, jsc:the_systems, jsc:partitions
