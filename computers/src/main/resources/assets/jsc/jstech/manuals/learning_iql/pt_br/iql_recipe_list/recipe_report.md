---
section: iql_recipe_list
order: 404
icon: jsc:mainframe
---
# Um relatório salvo

Um relatório é uma consulta que você salva com um nome, para poder ler os mesmos números depois. No IQL, uma consulta salva é uma visão ([](jsc:iql_views)). Cada vez que você a executa, a visão lê a rede de novo, então os números estão atualizados.

Só o Midsoft IQL Server mantém visões. Em uma rede cujo motor é Prophet YourIQL ou NextgreIQL, CREATE VIEW é recusado.

> [!DEFINE] Linha
> Uma linha de um resultado. Na tabela de itens, uma linha é um tipo de item.

Table: O que a receita precisa

| Necessidade | O que é preciso |
| --- | --- |
| Era | Vintage no prompt; Legacy e as seguintes para o studio |
| Programa | Nenhum no prompt; o IQL Server Management Studio para um arquivo |
| Motor | O Midsoft IQL Server em execução, que todo Mainframe tem |

1. Digite a linha CREATE VIEW no prompt, com iql na frente. A resposta diz "visão stock_report criada".
2. Digite iql QUERY stock_report para ler o relatório.
3. Para guardar os resultados em um arquivo, abra o IQL Server Management Studio, execute a consulta e escolha Resultados em Arquivo.

```table
{"caption": "Linhas para um relatório", "rows": [[{"words": "Salvar"}, {"literal": "CREATE VIEW stock_report AS QUERY items WHERE qty > 0 ORDER BY qty DESC LIMIT 20"}], [{"words": "Ler"}, {"literal": "QUERY stock_report"}], [{"words": "Excluir"}, {"literal": "DROP VIEW stock_report"}]]}
```

O nome da visão pode ter no máximo 64 caracteres. Não dê a uma visão o nome de uma tabela: QUERY items executaria a sua visão, e não a tabela de itens.

Resultados em Arquivo grava a grade de resultados como um arquivo de valores separados por vírgula (CSV), uma tabela de texto simples que planilhas abrem.

Table: As tabelas que você pode ler

| Tabela | Colunas |
| --- | --- |
| `items` | `item, qty, server, tag, enchant` |
| `servers` | `name, used, free, era` |
| `disks` | `disk, server, tier, size, used` |
| `operations` | `id, verb, state, item, moved, requested, priority` |
| `recipes` | `recipe, where, machine, chance` |
| `computers` | `name, kind, era, state, os` |

A tabela de itens também lê name, count, amount, damaged e durability em um WHERE ou um ORDER BY, embora elas não apareçam como colunas. O name é o nome do item como o jogo o mostra.

A coluna tag mostra uma tag: a primeira tag c: do item, em ordem alfabética. Um item com duas tags c: mostra apenas uma, então um WHERE sobre tag pode deixar de lado a outra. Um item sem tag c: mostra a primeira tag dele.

Uma tarefa também pode executar uma consulta, mas descarta as linhas, então uma tarefa não gera relatório algum. Execute a visão você mesmo. Um arquivo de script também não pode guardar um relatório, porque run recusa QUERY.

## Se algo der errado

> [!PROBLEM] A resposta diz "nenhuma visão chamada stock_report".
> A visão não foi salva, ou tem outro nome. Digite a linha CREATE VIEW de novo.

> [!PROBLEM] A resposta diz "recursão do IQL profunda demais (um procedimento ou visão referenciando a si mesmo?)".
> A visão chama a si mesma. Remova a referência ao nome da própria visão.

> [!PROBLEM] A resposta diz "O IQL parou após 4096 instruções para uma requisição".
> A visão faz instruções demais para uma só requisição. Encurte-a, ou retire um procedimento.

> [!PROBLEM] A resposta diz "um nome tem no máximo 64 caracteres".
> Encurte o nome da visão.

> [!PROBLEM] O studio diz "Não há resultados para salvar."
> Execute a consulta antes, para que haja resultados para salvar.

> [!PROBLEM] A resposta diz "QUERY/COUNT não são suportados por 'run', use 'operation' no lugar".
> Digite o nome da visão no prompt, ou abra o studio.

> [!SEE] jsc:iql_views, jsc:studio_overview, jsc:iql_limits, jsc:items_as_data
