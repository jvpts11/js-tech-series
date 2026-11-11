---
section: iql_verbs
order: 367
---
# COUNT

COUNT é escrito como uma consulta, mas não soma as quantidades. Ele lê as mesmas linhas que QUERY, e a resposta informa quantas linhas existem.

```iql count.iql
COUNT items WHERE qty > 100
```

Assim, a resposta é uma contagem de linhas, como "12 linhas". Para ver a quantidade de cada linha, leia a coluna qty de um QUERY items, ou abra a visão de tabela no Management Studio ([](jsc:studio_queries)).

COUNT lê uma cláusula IF, mas a ignora. Suas linhas são listadas, não importa o que o IF diga.

## Se algo der errado

> [!PROBLEM] esperado um item
> COUNT precisa de um objeto ou de um item depois dele, como COUNT items.

> [!PROBLEM] <file>: QUERY/COUNT não são suportados por 'run', use 'operation' no lugar
> Um arquivo executado no prompt recusa QUERY e COUNT. Digite a leitura no prompt, uma instrução por vez.

> [!SEE] jsc:query_and_select
