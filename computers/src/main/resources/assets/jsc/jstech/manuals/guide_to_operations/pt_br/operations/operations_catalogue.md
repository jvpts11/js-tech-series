---
section: operations
order: 133
icon: jsc:mainframe
---
# O catálogo de Operações

Aqui está cada Operação que a rede conhece, e o que ela faz. A maioria dos nomes vem dos comandos de bancos de dados, então, se você conhece um pouco de SQL, já conhece muitas delas.

> [!DEFINE] IQL
> Linguagem de Consulta de Itens: a linguagem própria da rede, na qual você digita o trabalho que quer fazer, como SELECT 64 iron_ingot.

> [!DEFINE] instrução
> Uma instrução do IQL: uma ação, uma pergunta ou um comando de manutenção.

> [!DEFINE] verbo
> A palavra que inicia uma instrução do IQL, como SELECT ou QUERY.

> [!DEFINE] cartão de uso pessoal
> Uma placa de oficina no computador que pede um UPDATE. A placa faz o trabalho para esse computador.

Table: Todas as Operações

| Operação | O que faz | Um exemplo |
| --- | --- | --- |
| `SELECT` | Retira itens do armazenamento e os leva para o computador que pediu. | `SELECT 64 iron_ingot` |
| `INSERT` | Coloca no armazenamento da rede os itens que o computador tem. | `INSERT 64 cobblestone` |
| `MOVE` | Move itens entre dois servidores, ou entre um servidor e um barramento indicado. | `MOVE 64 cobblestone FROM <server> TO <server>` |
| `DELETE` | Envia itens para fora da rede, pelo barramento indicado depois de TO. Não destrói nada. | `DELETE 64 cobblestone TO '<bus name>'` |
| `DROP` | Destrói itens para sempre. | `DROP 64 cobblestone` |
| `CRAFT` | Fabrica itens que a rede produz, em máquinas ou em etapas. | `CRAFT 64 torch` |
| `UPDATE` | Altera um item que a rede guarda, com um cartão de uso pessoal. | `UPDATE 1 iron_pickaxe SET REPAIR` |
| `ANALYZE` | Mantém o índice atualizado. Olha apenas os servidores que mudaram e descarta as entradas dos servidores que saíram. | `ANALYZE` |
| `REINDEX` | Reconstrói o índice inteiro a partir de todos os discos. | `REINDEX` |
| `VACUUM` | Remove as entradas do índice que não guardam nada ou que apontam para um servidor que não está mais na rede. | `VACUUM` |
| `LOCK` | Reserva itens, para que outras Operações esperem por eles. | `LOCK 64 iron_ingot` |
| `UNLOCK` | Libera uma reserva. | `UNLOCK iron_ingot` |

Cada nome da primeira coluna é um verbo do IQL ([](jsc:query_and_select)). QUERY e COUNT apenas leem a rede, por isso não são Operações. O livro Aprendendo IQL explica cada verbo em sua própria entrada: UPDATE em [](jsc:update_cards), e LOCK em [](jsc:lock_unlock).

O programa Rede envia a maioria delas para você quando você clica, então você não precisa digitá-las. No IQL você pode dizer exatamente o que quer ([](jsc:iql)).

O UPDATE funciona em três etapas. O item é retirado da rede com um SELECT, a placa trabalha nele, e o resultado é devolvido com um INSERT. A placa é uma placa de oficina no computador que pediu ([](jsc:workshop)).

Table: As placas do UPDATE

| Placa | Suas ações de UPDATE |
| --- | --- |
| Placa de Fornalha | `SMELT` |
| Placa de Encantamento | `ENCHANT` |
| Placa de Bigorna | `REPAIR, COMBINE, NAME` |

Quem pediu paga o preço de experiência da placa, exceto na fundição. Um nome sem prefixo, como iron_ingot, significa o item do Minecraft com esse nome.

> [!WARNING] DROP destrói itens para sempre, e nada os traz de volta.

## Se algo der errado

> [!PROBLEM] O resultado diz "a rede não estava guardando o", seguido de um item.
> A rede não tem esse item. Armazene-o primeiro ([](jsc:storage_servers)) ou confira o nome.

> [!PROBLEM] O resultado diz que a placa não conseguiu fazer: nada para alterar, custo alto demais ou níveis insuficientes.
> Confira o item. Uma placa precisa de algo para alterar, e o jogador que pediu precisa de níveis suficientes para o trabalho.

> [!PROBLEM] O resultado diz que quem pediu não estava presente para pagar.
> O jogador que pediu precisa estar presente para pagar a experiência. Peça de novo com ele presente.

> [!SEE] jsc:operation_states, jsc:iql
