---
section: storage
order: 122
icon: minecraft:water_bucket
---
# Fluidos e substâncias químicas

A rede guarda fluidos da mesma forma que guarda itens: como dados, contados em milibaldes (mB). Um balde contém 1.000 milibaldes, então um balde cheio ocupa exatamente o mesmo espaço que um item ([](jsc:items_as_data)).

> [!DEFINE] Milibalde (mB)
> Um milésimo de balde. Fluidos e substâncias químicas são contados nessa unidade, e cada um ocupa um milésimo do espaço de um item.

Um Barramento de Armazenamento Externo pode ficar voltado para um tanque de qualquer mod. A rede então usa o fluido do tanque como armazenamento, como faz com itens.

Fluido também entra e sai em recipientes. Você pode usar um balde ou um tanque para isso, e cada um se comporta de um jeito próprio.

Table: Recipientes e a rede

| Recipiente | O que acontece |
| --- | --- |
| Balde | Entrega seus 1.000 mB inteiros, ou nada. O balde vazio volta. |
| Item de tanque | Entrega o máximo que couber, até o limite que o item permite de uma vez. O tanque esvaziado volta. |

Tirar fluido funciona ao contrário. Um balde só é preenchido com o equivalente a um balde cheio. Qualquer fluido que a rede não conseguiu pegar volta para o recipiente.

## Substâncias químicas

> [!DEFINE] Substância química
> Uma substância que um mod como o Mekanism guarda em seus próprios tanques.

> [!DEFINE] Ponte
> A peça deste mod que ensina a rede a ler os tanques de um mod de química.

Substâncias químicas ocupam espaço em millibaldes, como um fluido. Elas precisam do Mekanism: instale-o, e uma ponte química permite que a rede leia os seus tanques de substâncias químicas ([](jsc:mekanism)).

## Se algo der errado

> [!PROBLEM] O fluido não entra.
> O armazenamento da rede não tem mais espaço. Adicione discos ou servidores ([](jsc:storage_trouble)).

> [!PROBLEM] Nenhuma substância química aparece no armazenamento da rede.
> Instale o Mekanism. A rede lê substâncias químicas apenas por meio dele.

> [!SEE] jsc:items_as_data, jsc:external_storage_bus, jsc:mekanism
