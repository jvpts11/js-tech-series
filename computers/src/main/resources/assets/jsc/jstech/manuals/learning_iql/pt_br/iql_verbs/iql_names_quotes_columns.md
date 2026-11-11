---
section: iql_verbs
order: 365
---
# Nomes, aspas e colunas

Toda instrução começa com um verbo: a palavra que diz o que a instrução faz, como SELECT ou QUERY. Depois do verbo vem o item ou o objeto em que a instrução trabalha. As palavras de cláusula vêm depois disso. Cada uma acrescenta uma parte: WHERE escolhe linhas, ORDER BY as ordena e LIMIT as encurta.

Verbos e palavras de cláusula não se importam com maiúsculas, então query items e QUERY Items significam a mesma coisa. Um nome sem dois-pontos é um item vanilla: cobblestone significa minecraft:cobblestone. Leia os três tipos de texto abaixo, porque o IQL lê cada tipo de um jeito próprio.

Table: Os três tipos de texto

| Tipo | Como você escreve |
| --- | --- |
| Palavra | Qualquer sequência de caracteres sem espaço, colchete, aspas ou sinal de comparação, como iron_ingot ou qty. |
| Número | Um número inteiro, como 64. Um número com sinal de porcentagem é uma porcentagem. |
| Texto entre aspas | Texto entre aspas simples ou duplas, como 'Old Faithful'. Pode ter espaços. |

Um sinal como = ou < se junta a uma palavra que não tem espaço ao redor. Então qty<100 é lido como três partes: qty, < e 100. Coloque um espaço de cada lado de um sinal e você evita o problema.

## Onde cada tipo é aceito

Table: O que cada lugar de uma instrução aceita

| Onde ela fica na instrução | O que o IQL aceita ali |
| --- | --- |
| O item depois do verbo | Uma palavra simples. Texto entre aspas é recusado. |
| O nome depois de FROM ou TO | Uma palavra simples, sem espaço. |
| O nome de um barramento, interface ou roteador depois de SET ou RENAME | Uma palavra ou texto entre aspas, então 'Ore in' funciona. |
| O nome de uma bigorna depois de SET NAME | Texto entre aspas, com no máximo 50 caracteres. |
| O valor no WHERE ou no IF | Uma palavra, um número ou texto entre aspas. |
| O nome do campo no WHERE ou no IF | Uma palavra simples. |
| O nome de uma instrução salva, depois de CREATE ou DROP | Uma palavra simples, com no máximo 64 caracteres. |

Nomes de item não têm espaços, então um item é sempre uma palavra simples. Um nome de barramento pode ter espaços, então você o coloca entre aspas depois de SET: SET BUS 'Ore in' ON.

## Colunas e campos

Estas são as tabelas e as suas colunas:

Table: As tabelas e suas colunas

| Tabela | Colunas |
| --- | --- |
| `items` | item, qty, server, tag, enchant |
| `servers` | name, used, free, era |
| `disks` | disk, server, tier, size, used |
| `operations` | id, verb, state, item, moved, requested, priority |
| `recipes` | recipe, where, machine, chance |
| `computers` | name, kind, era, state, os |
| `events` | event, name, state, starts, ends |
| `power` | server, row, watts, on |

Um item tem mais campos do que a tabela mostra. Uma cláusula WHERE pode comparar qualquer um destes:

Table: Campos de um item

| Campo | O que guarda |
| --- | --- |
| `item` | O nome interno do item, sem o namespace, como iron_ingot. |
| `name` | O nome de exibição que o jogo mostra. |
| `qty` | A quantidade. count e amount significam o mesmo. |
| `server` | O servidor a que a linha pertence. |
| `damaged` | true ou false. |
| `durability` | A durabilidade como porcentagem, de 0 a 100. Um item que não pode se desgastar dá 100. |
| `tag` | A primeira tag do item. |
| `enchant` | Os encantamentos, escritos como texto. |

Um comentário começa com dois hifens, --, e vai até o fim da linha. Uma linha que começa com os hifens não faz nada quando um arquivo roda. O Management Studio também pode pedir um valor quando um script roda: os modelos dele estão em ([](jsc:studio_templates)).

> [!DEFINE] verbo
> A palavra que começa uma instrução e diz o que ela faz, como SELECT ou QUERY.

> [!DEFINE] cláusula
> Uma palavra que acrescenta uma parte a uma instrução, como WHERE, ORDER BY ou LIMIT. As cláusulas vêm depois do item.

> [!DEFINE] coluna
> Um campo de uma tabela que um QUERY mostra. Os mesmos nomes funcionam no WHERE e no ORDER BY.

## Se algo der errado

> [!PROBLEM] esperava um item, recebeu: 'Old Faithful'
> Ids de item não têm espaços. Digite o id do item como uma palavra simples, sem aspas.

> [!PROBLEM] esperava uma localização depois de FROM, recebeu: Ore in
> Digite o nome como uma palavra só, sem espaço, depois de FROM ou TO.

> [!PROBLEM] Uma linha que você espera está faltando na resposta.
> Um campo que você digitou não existe na tabela, então a comparação dele é falsa. Confira a grafia com as tabelas acima.

> [!SEE] jsc:query_and_select, jsc:clauses_priority_if_with
