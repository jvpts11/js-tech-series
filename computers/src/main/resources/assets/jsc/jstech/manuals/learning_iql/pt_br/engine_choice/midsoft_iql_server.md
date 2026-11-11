---
section: engine_choice
order: 385
icon: jsc:mainframe
---
# Midsoft IQL Server

O Midsoft IQL Server é o motor com que todo Mainframe começa. Ele fala IQL, a linguagem da rede, e é o único dos três motores que guarda visões e procedimentos. Você salva uma consulta com um nome, como visão, ou como procedimento que roda pelo nome com EXEC ([](jsc:iql_views), [](jsc:iql_procedures)).

```table
{"caption": "O Midsoft IQL Server", "rows": [[{"words": "Dialeto"}, {"words": "IQL"}], [{"words": "Extras"}, {"words": "procedimentos e visões"}], [{"words": "Memória"}, {"amount": 24, "unit": "MB"}], [{"words": "Eras"}, {"words": "Vintage até Advanced"}]]}
```

A versão depende da era do Mainframe em que ele está instalado. A aba Serviços mostra a versão do motor instalado.

Table: Versões pela era do Mainframe

| Era do Mainframe | Versão do Midsoft IQL Server |
| --- | --- |
| Vintage | 4.2 |
| Legacy | 2000 |
| Transition | 2008 |
| Standard | 2012 |
| Advanced | 2022 |

As versões se comparam pelos números, então a versão 4.2 é mais antiga que a versão 2000. Seu Mainframe só consegue rodar a versão feita para a sua própria era.

Em um Mainframe Legacy ou mais novo, o IQL Server Management Studio (ISMS) trabalha com o Midsoft ([](jsc:studio_overview)). O ISMS precisa da versão 2000 ou mais nova. Um Mainframe Vintage não tem estúdio, então você trabalha com o Midsoft pelo prompt.

O Midsoft não tem EXPLAIN, KEEP nem WATCH. Essas palavras pertencem ao NextgreIQL e ao Prophet YourIQL, e o Midsoft as recusa com uma mensagem de sintaxe.

O Midsoft também não guarda Jobs. Um Job precisa do Motor de Automação instalado no Mainframe ([](jsc:automation_engine)).

## Se algo der errado

> [!PROBLEM] Você digita CREATE JOB, e a rede diz que os Jobs são guardados pelo Motor de Automação.
> Instale primeiro o Motor de Automação no Mainframe.

> [!PROBLEM] Você digita EXPLAIN, KEEP ou WATCH, e a resposta começa com "syntax:".
> Essas palavras pertencem ao NextgreIQL ou ao Prophet YourIQL. Use o motor que as oferece.

> [!SEE] jsc:iql_procedures, jsc:engine_services
