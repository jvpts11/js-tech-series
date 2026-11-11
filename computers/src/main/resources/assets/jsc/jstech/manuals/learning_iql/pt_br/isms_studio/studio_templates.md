---
section: isms_studio
order: 396
---
# Modelos e a Referência da IQL

Um modelo é uma instrução com lacunas, que você preenche antes de ela rodar. O Gerenciador de Modelos, no menu Exibir, lista oito deles. Clique em um, e ele abre em uma nova consulta.

> [!DEFINE] Modelo
> Uma instrução salva com lacunas. As lacunas são preenchidas antes de a instrução rodar.

Table: Os oito modelos

| Modelo | Instrução que abre |
| --- | --- |
| Consultar uma tabela | `QUERY <table, table, items> WHERE <column, column, qty> < <value, number, 64> ORDER BY <column, column, qty> DESC LIMIT <rows, number, 100>;` |
| Contar um item | `COUNT <item, name, iron_ingot>;` |
| Fabricar um item | `CRAFT <quantity, number, 64> <item, name, iron_ingot>;` |
| Criar uma visão | `CREATE VIEW <name, name, low_stock> AS QUERY items WHERE qty < <value, number, 64>;` |
| Criar um procedimento | `CREATE PROCEDURE <name, name, restock> AS { CRAFT <quantity, number, 64> <item, name, torch>; };` |
| Criar uma tarefa | `CREATE JOB <name, name, nightly_vacuum> AS VACUUM EVERY <interval, interval, 20m>;` |
| Reter um item | `LOCK <quantity, number, 64> <item, name, diamond>;` |
| Atualizar um item | `UPDATE <quantity, number, 1> <item, name, iron_sword> SET REPAIR;` |

Uma lacuna tem três partes entre os colchetes angulares: um nome, um tipo e um valor padrão. Por exemplo, <quantity, number, 64> é uma lacuna chamada quantity, do tipo number, cujo valor começa em 64.

Pressione [[Ctrl]]+[[Shift]]+[[M]] para preencher as lacunas da consulta atual. A caixa Especificar Valores para Parâmetros de Modelo pede cada valor, e mostra cada lacuna pelo seu nome e tipo.

## Referência da IQL

Pressione [[F1]] para abrir a Referência da IQL. Ela lista cada instrução com o que ela faz. A tabela abaixo traz a mesma lista, com o significado que o editor dá a cada linha.

Table: Referência da IQL

| Sintaxe | O que faz |
| --- | --- |
| `QUERY table [WHERE ...] [ORDER BY ...] [LIMIT n]` | Lê as linhas de uma tabela. |
| `COUNT item` | Conta um item que a rede guarda. |
| `SELECT n item` | Traz um item para este computador. |
| `INSERT n item` | Armazena um item deste computador. |
| `DELETE n item TO place` | Envia um item para fora da rede. |
| `MOVE n item FROM server TO server` | Move um item entre servidores. |
| `DROP n item` | Destrói um item, para sempre. |
| `CRAFT n item` | Fabrica um item a partir das suas receitas. |
| `UPDATE n item SET action` | Altera um item com um cartão de uso pessoal. |
| `LOCK n item / UNLOCK item` | Retém um item, ou o libera. |
| `ANALYZE / REINDEX / VACUUM` | Cuida do índice. |
| `CREATE VIEW name AS query` | Salva uma consulta com um nome. |
| `CREATE PROCEDURE name AS { ... }` | Salva instruções para rodar juntas. |
| `CREATE JOB name AS ... EVERY time` | Roda instruções em um temporizador ou em uma condição. |
| `EXEC name` | Roda um procedimento salvo. |
| `WHERE / ORDER BY / LIMIT` | WHERE mantém as linhas que combinam, ORDER BY as ordena, LIMIT para depois de tantas. |
| `; --` | Um ponto e vírgula encerra uma instrução; dois traços iniciam um comentário. |

## Se algo der errado

> [!PROBLEM] [[Ctrl]]+[[Shift]]+[[M]] diz "Esta consulta não tem parâmetros de modelo."
> Abra um modelo pelo Gerenciador de Modelos, ou escreva a instrução à mão.

> [!SEE] jsc:first_queries, jsc:query_and_select, jsc:iql_count, jsc:insert_delete_move_drop, jsc:iql_craft, jsc:update_cards, jsc:lock_unlock, jsc:iql_views, jsc:iql_procedures, jsc:iql_jobs
