---
section: iql_saved_objects
order: 378
---
# Visões

> [!DEFINE] visão
> Uma instrução salva com um nome. Cada vez que você digita o nome, a rede roda a instrução salva de novo.

Uma visão é uma instrução que você salva sob um nome. Você a escreve uma vez, com CREATE VIEW. Depois, cada vez que você digita o nome, a rede roda a instrução salva de novo, então a resposta é o que está agora.

A instrução salva é guardada exatamente como você a digitou. Pode ser qualquer instrução, embora normalmente seja um QUERY.

```iql views.iql
CREATE VIEW low_iron AS QUERY items WHERE item = iron_ingot AND qty < 100
```

1. Digite CREATE VIEW, um nome, AS e a instrução, tudo em uma linha.
2. Pressione [[Enter]] para salvar a visão.
3. Digite QUERY e o nome para executá-la, como QUERY low_iron.

Instruções salvas são guardadas no Mainframe. Um nome é único para cada tipo de instrução salva, e maiúsculas não importam.

Visões precisam do [Midsoft IQL Server](jsc:midsoft_iql_server) no Mainframe. Outros motores as recusam.

Para remover uma visão, digite DROP VIEW e o nome, como DROP VIEW low_iron.

Table: As respostas para visões

| Resposta | O que significa |
| --- | --- |
| `view <name> dropped` | A visão foi apagada. |
| `no view named <name>` | Nenhuma visão tem esse nome. |
| `DROP VIEW needs a name` | Digite o nome depois de DROP VIEW. |

## Se algo der errado

> [!PROBLEM] CREATE expects VIEW, PROCEDURE, or JOB
> Digite VIEW, PROCEDURE ou JOB depois de CREATE.

> [!PROBLEM] CREATE VIEW precisa de um nome e de um corpo AS
> Digite um nome depois de VIEW, depois AS e a instrução. O mesmo vale para PROCEDURE e JOB.

> [!PROBLEM] esperava AS depois do nome de VIEW
> Digite a palavra AS depois do nome.

> [!PROBLEM] CREATE VIEW precisa de um corpo depois de AS
> Digite a instrução depois de AS.

> [!PROBLEM] um nome tem no máximo 64 caracteres
> Encurte o nome para 64 caracteres ou menos.

> [!PROBLEM] o engine da rede não guarda views, procedimentos nem jobs
> Use um motor que as guarde: o Midsoft IQL Server ([](jsc:midsoft_iql_server)).

> [!SEE] jsc:iql_procedures, jsc:iql_jobs, jsc:midsoft_iql_server
