---
section: iql_saved_objects
order: 379
---
# Procedimentos e EXEC

> [!DEFINE] procedimento
> Uma lista de instruções salvas sob um nome. O EXEC roda as instruções em ordem.

Um procedimento é uma lista de instruções salvas sob um nome. Quando você o executa com EXEC, as instruções rodam uma após a outra, então um só nome faz várias coisas por você.

```iql restock.iql
CREATE PROCEDURE restock AS { SELECT 64 iron_ingot; SELECT 64 gold_ingot }
```

1. Digite CREATE PROCEDURE, um nome, AS e as instruções entre chaves, com um ponto e vírgula depois de cada instrução, exceto a última.
2. Pressione [[Enter]] para salvar o procedimento.
3. Digite EXEC e o nome para executá-lo, como EXEC restock.

Um procedimento também pode conter uma única instrução, sem chaves. PROC significa o mesmo que PROCEDURE, e CALL significa o mesmo que EXEC.

As instruções rodam em ordem. Se uma falhar, o procedimento para ali, e as instruções antes dela já rodaram. Procedimentos precisam do mesmo motor das visões ([](jsc:iql_views)).

Table: As respostas para procedimentos

| Resposta | O que significa |
| --- | --- |
| `procedure <name> ran <n> statements` | Todas as instruções rodaram, e a resposta as conta. |
| `procedure <name> stopped at statement <n>: <error>` | A instrução com aquele número falhou. A mensagem depois dele diz o motivo. |
| `no procedure named <name>` | Nenhum procedimento tem esse nome. |
| `procedure <name> dropped` | O procedimento foi apagado. |

Um procedimento pode executar outro procedimento ou uma visão, até 32 níveis de profundidade. Um pedido pode rodar no máximo 4.096 instruções. Passando de qualquer um desses limites, a rede para e mostra a mensagem.

## Se algo der errado

> [!PROBLEM] CREATE PROCEDURE precisa de um nome e de um corpo AS
> Digite um nome depois de PROCEDURE, depois AS e as instruções.

> [!PROBLEM] o procedimento <name> parou na instrução <n>: <error>
> Corrija a instrução que falhou e execute o EXEC de novo.

> [!PROBLEM] recursão do IQL profunda demais (um procedimento ou visão referenciando a si mesmo?)
> Um procedimento roda a si mesmo, diretamente ou por meio de outro. Remova o laço.

> [!PROBLEM] o IQL parou depois de 4096 instruções num só pedido (procedimentos rodando outros vezes demais?)
> Dê aos procedimentos menos instruções para executar, ou divida o trabalho em vários pedidos.

> [!PROBLEM] EXEC precisa de um nome de procedimento
> Digite o nome depois de EXEC.

> [!PROBLEM] EXEC aceita um único nome de procedimento
> Execute um procedimento de cada vez, com um EXEC para cada nome.

> [!PROBLEM] nenhum procedimento chamado <name>
> Confira o nome, ou crie o procedimento primeiro.

> [!PROBLEM] DROP PROCEDURE precisa de um nome
> Digite o nome depois de DROP PROCEDURE.

> [!SEE] jsc:iql_views, jsc:iql_jobs, jsc:midsoft_iql_server
