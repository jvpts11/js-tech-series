---
section: iql_verbs
order: 374
---
# PRIORITY, IF, OFFER e WITH

As palavras depois do item são cláusulas. Cada verbo aceita algumas delas. Uma cláusula que o verbo não aceita é recusada com uma mensagem, como "FROM não é válido em uma leitura". A tabela mostra o que cada verbo aceita.

Table: Cláusulas que cada verbo aceita

| Verbo | Cláusulas que aceita |
| --- | --- |
| `QUERY` | WHERE, ORDER BY e LIMIT. FROM, TO, IF e PRIORITY são recusadas. |
| `SELECT` | FROM (opcional), WHERE, IF, ORDER BY, LIMIT e PRIORITY. TO é recusado. |
| `INSERT` | FROM (opcional, um nome de barramento), WHERE, IF, ORDER BY, LIMIT e PRIORITY. TO é recusado. |
| `DELETE` | TO (obrigatório), WHERE, IF, ORDER BY, LIMIT e PRIORITY. FROM é lido, mas não tem efeito. |
| `MOVE` | FROM e TO (ambos obrigatórios), WHERE, IF, ORDER BY, LIMIT e PRIORITY. |
| `DROP` | WHERE, IF, ORDER BY, LIMIT e PRIORITY, com FROM ignorado e TO recusado. |
| `CRAFT` | WHERE, IF, ORDER BY, LIMIT e PRIORITY. FROM e TO são lidos, mas não têm efeito. |
| `COUNT` | WHERE, ORDER BY e LIMIT. IF, FROM, TO e PRIORITY são lidos, mas não têm efeito. |
| `LOCK, UNLOCK` | WHERE, IF, ORDER BY, LIMIT e PRIORITY, que não tem efeito. |
| `UPDATE` | SET (obrigatório), FROM (opcional, um servidor), WHERE, IF, ORDER BY, LIMIT e PRIORITY. TO é recusado. |
| `ANALYZE, VACUUM, REINDEX` | Nenhuma cláusula. Uma palavra de objeto, que é ignorada. |

## PRIORITY

PRIORITY define o nível de cada Operação que uma instrução coloca na fila ([](jsc:priorities)). O padrão é MEDIUM, e você escreve o nível depois da palavra, como PRIORITY HIGH.

Table: Os cinco níveis

| Nível | Rótulo curto no Gerenciador de Tarefas |
| --- | --- |
| `LOW` | `LOW` |
| `MEDIUM_LOW` | `MED-` |
| `MEDIUM` | MED, o padrão |
| `MEDIUM_HIGH` | `MED+` |
| `HIGH` | `HIGH` |

NORMAL significa MEDIUM. O hífen é lido como sublinhado, e as maiúsculas não importam, então medium-high é MEDIUM_HIGH.

## IF

IF executa a instrução só quando uma condição se cumpre. A condição usa as palavras de uma condição WHERE ([](jsc:query_and_select)). A rede a verifica contra o seu estoque do item nomeado antes de a instrução ser executada.

Table: Os campos que um IF pode verificar

| Campo | O que a verificação compara |
| --- | --- |
| `qty, count, amount` | O total que a rede tem do item, contando cada cópia. |
| `item` | O id do item, ou \* para qualquer item. |
| `name` | O nome de exibição, ou o item como você o digitou quando a rede não tem nenhum. |
| Qualquer outro campo | Nunca é verdadeiro, então a instrução não é executada. |

Quando a condição não se cumpre, a instrução não é executada. A resposta não é um erro: "o IF não se cumpriu, então o SELECT não foi executado", com o verbo que você digitou. Em um arquivo, a execução passa para a linha seguinte.

## WITH

WITH dá ao UPDATE o seu segundo item: o material para REPAIR, ou o item que COMBINE acrescenta. Ele vem depois da palavra de ação, e nenhuma outra instrução o aceita.

## OFFER

OFFER escolhe uma das três ofertas de encantamento, depois de ENCHANT. Você digita 1, 2 ou 3 depois da palavra. ([](jsc:update_cards)) mostra o que cada oferta significa.

## Se algo der errado

> [!PROBLEM] <ACTION> não aceita WITH
> Remova o WITH. SMELT, ENCHANT e NAME não aceitam nenhum.

> [!PROBLEM] COMBINE precisa de WITH e do item que acrescenta
> Adicione WITH e o segundo item.

> [!PROBLEM] token inesperado: WITH
> Coloque WITH depois da palavra de ação de um UPDATE.

> [!PROBLEM] uma oferta é 1, 2 ou 3, não <n>
> Digite 1, 2 ou 3 depois de OFFER.

> [!PROBLEM] uma oferta depois de OFFER (1, 2 ou 3)
> Você deixou o número de fora. Digite-o logo depois da palavra OFFER.

> [!PROBLEM] nível de prioridade desconhecido: <word> (esperado LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH ou HIGH)
> Digite uma das cinco palavras de nível.

> [!PROBLEM] IF não é válido em uma leitura
> Um QUERY não pode ter IF. Use WHERE para escolher as linhas.

> [!SEE] jsc:priorities, jsc:update_cards, jsc:iql_count
