---
section: iql_verbs
order: 366
---
# QUERY e SELECT

QUERY lê a rede e mostra o que ela possui. Não move nada. SELECT é uma Operação que traz itens da rede para o armazenamento próprio deste computador, o seu [armazenamento local](jsc:local_storage). Ambos aceitam WHERE, ORDER BY e LIMIT, que escolhem as linhas com que você trabalha.

## QUERY

Nomeie o objeto depois de QUERY: a tabela que você quer ler. Você também pode escrever um item, ou um asterisco para todos os itens.

Table: Objetos que você pode consultar

| Objeto | O que são as linhas |
| --- | --- |
| `items, or *` | Todo item que a rede possui. |
| `servers` | Todo servidor da rede. |
| `operations` | As Operações que rodam ou esperam. |
| `computers` | Os computadores da rede. |
| `recipes` | As receitas que a rede conhece. |
| `disks` | Os discos da rede. |
| `events` | Os eventos do mundo que a rede vê: em curso onde ela está, ou a caminho quando uma máquina nela é o instrumento do evento. |
| `power` | Cada servidor de um rack com PDU: a unidade de rack em que ele começa, o que ele consome em watts e se a baia dele está ligada. |

As máquinas de outros mods na rede têm tabelas próprias. Nomeie uma pelo id, como QUERY mymod:reactors, ou pela parte depois dos dois-pontos quando nenhuma outra tabela a tem. Cada linha é uma máquina, com as suas colunas, as unidades no cabeçalho, e at, onde ela está.

Um nome que nenhuma tabela tem é recusado: "Nenhuma tabela se chama <nome>". Um nome que duas tabelas partilham pede o id. Depois de um QUERY, a resposta conta as linhas, como "12 linhas" ou "1 linha". Para ler as próprias linhas, abra a visão de tabela no Management Studio ([](jsc:studio_queries)).

```iql stock.iql
QUERY items WHERE qty > 100 ORDER BY qty DESC LIMIT 5
```

## SELECT

SELECT tira itens da rede. Você digita uma quantidade, o item e, se quiser, um servidor. Uma quantidade ALL, ou nenhuma quantidade, pede tudo o que a rede pode dar. SELECT 64 iron_ingot e SELECT ALL iron_ingot fazem a mesma coisa.

1. Digite SELECT, uma quantidade e o item, como SELECT 64 iron_ingot.
2. Para tirar de um único servidor, acrescente FROM e o nome do servidor como uma palavra só.
3. Pressione [[Enter]], depois leia a resposta.

Table: As respostas de SELECT

| Resposta | O que significa |
| --- | --- |
| `SELECT queued: <quantity> <item> -> local storage` | Os itens estão a caminho deste computador. |
| `SELECT queued: <quantity> <item> from <server> -> local storage` | O mesmo, tirado daquele servidor. |
| `nothing to select` | Você digitou SELECT \*, e a rede não possui nenhum item. |
| `no server named '<server>'` | Nenhum servidor tem esse nome. Confira a grafia. |

Um asterisco pede todos os tipos de item de uma vez: SELECT \* coloca em fila uma movimentação para cada tipo de item que a rede possui, até 256 tipos. A resposta então diz "12 tipos de item" ou "1 tipo de item".

## WHERE: escolhendo linhas

WHERE mantém só as linhas que atendem a uma condição. Uma condição compara um campo com um valor, com um operador entre eles. qty > 100 mantém as linhas cuja quantidade é maior que 100.

```table
{"caption": "Operadores", "heads": ["Operador", "O que testa"], "rows": [[{"literal": "=  or  =="}, {"words": "Igual. Números são comparados como números; palavras são comparadas sem diferenciar maiúsculas."}], [{"literal": "!="}, {"words": "Diferente."}], [{"literal": "<  >  <=  >="}, {"words": "Menor que, maior que, no máximo, no mínimo."}], [{"literal": "contains"}, {"words": "O texto contém a palavra, sem diferenciar maiúsculas."}], [{"literal": "has"}, {"words": "O mesmo teste de contains."}], [{"literal": "like"}, {"words": "Um padrão, em que * representa qualquer sequência de caracteres."}]]}
```

Combine condições com AND e OR, inverta uma delas com NOT, e use parênteses para agrupá-las. NOT tem a precedência mais alta, depois AND, depois OR. Uma condição pode ter no máximo 64 níveis de NOT e de parênteses, e no máximo 256 comparações. Uma condição termina na primeira palavra que ela não consegue usar, como IF, ORDER ou LIMIT.

```iql rare.iql
QUERY items WHERE name contains "rare" AND NOT damaged = true
```

## ORDER BY e LIMIT

ORDER BY ordena as linhas por um campo: do menor para o maior, ou de A a Z para palavras. ASC é o padrão, e DESC ordena ao contrário. LIMIT mantém só as primeiras linhas. Ele recebe uma contagem de linhas, não uma porcentagem.

Table: Ordenando um QUERY

| Campo | Como as linhas são ordenadas |
| --- | --- |
| `qty, count, amount, quantity` | Por número. |
| `name, item` | Por palavra, sem diferenciar maiúsculas. |
| `server` | Por palavra. |
| Qualquer outro campo | Não ordenado: a ordem continua como estava. |

Com ORDER BY e LIMIT juntos, as linhas são ordenadas primeiro, e LIMIT mantém as primeiras da lista ordenada. Por padrão, um QUERY lê até 4.096 linhas antes de ordená-las.

Para SELECT e as outras ações, WHERE, ORDER BY e LIMIT escolhem quais tipos de item a instrução trabalha. A ordenação usa os campos de item listados acima, e LIMIT então pega os primeiros tipos de item.

## Se algo der errado

> [!PROBLEM] FROM não é válido em uma leitura
> Um QUERY não tem FROM. Mova o servidor para WHERE, como em QUERY items WHERE server = "Main Storage".

> [!PROBLEM] item desconhecido: <name>
> O WHERE não manteve nenhuma linha para o item, então a resposta diz item desconhecido mesmo com o item existindo. Confira o WHERE, o servidor depois de FROM e o estoque com QUERY items.

> [!PROBLEM] esperado um nome de campo
> Coloque um nome de campo antes do operador, como qty.

> [!PROBLEM] operador desconhecido: <text>
> Use um dos operadores da tabela acima.

> [!PROBLEM] nenhum servidor chamado '<server>'
> Confira o nome do servidor: QUERY servers os lista.

> [!SEE] jsc:iql_names_quotes_columns, jsc:iql_count, jsc:iql_limits
