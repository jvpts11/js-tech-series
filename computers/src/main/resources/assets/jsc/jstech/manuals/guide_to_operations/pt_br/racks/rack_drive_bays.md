---
section: racks
order: 56
---
# Baias de unidades e troca a quente

Uma baia de unidades guarda um disco. Você pode retirar o disco da baia e colocá-lo de volta enquanto a máquina está ligada, o que se chama troca a quente.

> [!DEFINE] Troca a quente
> Retirar uma peça de uma máquina, ou colocar uma, enquanto a máquina continua ligada.

As baias pertencem ao rack, não ao servidor. Uma unidade permanece na baia quando o servidor é retirado, e o próximo servidor montado sobre essas fileiras herda suas unidades.

Uma baia de unidades aceita apenas um disco da era do rack ou de uma anterior, como os compartimentos de disco de um computador ([](jsc:disks)).

As unidades podem se juntar em um único volume, chamado RAID. Dependendo do modo, um array RAID continua funcionando quando uma unidade se perde ([](jsc:raid_arrays)).

> [!DEFINE] RAID
> Uma forma de juntar várias unidades em um único volume, com ou sem uma cópia dos dados.

O que acontece quando você retira uma unidade depende da baia em que ela está.

Table: Retirando uma unidade

| Baia | O que acontece |
| --- | --- |
| Uma unidade independente | Ela sai com os itens que estão nela. |
| Um array RAID 0 | O array inteiro perde seus itens. |
| Um array RAID 1 ou RAID 5 | O array continua funcionando enquanto uma unidade substituta o reconstrói. |

Em um array RAID 1 ou RAID 5, o conteúdo de uma unidade retirada passa para as unidades que permanecem, na medida em que elas têm espaço para isso.

Uma reconstrução leva 20 ticks para cada 1.000 itens de capacidade do array. Nunca leva menos de 40 ticks, que são 2 segundos, nem mais de 3 minutos. Enquanto roda, o rack mostra REBUILD e uma porcentagem. O rack também marca um array como DEGRADED ou FAILED.

## Se algo der errado

> [!PROBLEM] A baia recusa o item.
> Uma baia de unidades aceita apenas um disco da era do rack ou de uma anterior. Segure o item sobre a baia para ler qual regra o deixa de fora.

> [!PROBLEM] O array perdeu tudo.
> Um array RAID 0 não guarda cópia, então perde tudo quando uma unidade é perdida. Use RAID 1 ou RAID 5 para ter uma cópia ([](jsc:raid_arrays)).

> [!SEE] jsc:server_types, jsc:disks
