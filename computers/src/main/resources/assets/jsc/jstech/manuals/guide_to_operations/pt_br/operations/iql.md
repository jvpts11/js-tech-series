---
section: operations
order: 138
icon: jsc:mainframe
---
# IQL, a linguagem da rede

Clicar no programa Rede é bom para pegar uma pilha de ferro. Mas e se você quiser perguntar de quais itens a rede guarda mais de 100, ou fabricar 64 tochas a partir de um script? Para isso serve o IQL: pedir à rede qualquer coisa, com palavras, em um prompt de comando, em um programa ou em um script.

O IQL se parece com a linguagem de bancos de dados. Algumas linhas mostram a ideia:

```table
{"caption": "Algumas linhas de IQL", "rows": [[{"words": "Pegar itens"}, {"literal": "SELECT 64 iron_ingot"}], [{"words": "Fabricar"}, {"literal": "CRAFT 64 torch"}], [{"words": "Perguntar"}, {"literal": "QUERY items WHERE qty > 100"}], [{"words": "Ordenar e cortar"}, {"literal": "QUERY items ORDER BY name LIMIT 2"}], [{"words": "Ver os discos"}, {"literal": "QUERY disks"}]]}
```

Uma instrução começa com um verbo. Uma ação precisa de uma quantidade e de um item, e pode acrescentar outras cláusulas. Uma pergunta começa com QUERY ou SHOW. A quantidade é um número, ou ALL para tudo o que a rede guarda desse item. Porcentagens como 50% não são suportadas.

```table
{"caption": "A forma de uma ação", "rows": [[{"words": "Início"}, {"literal": "VERB [quantity] item"}], [{"words": "Depois, em qualquer ordem"}, {"literal": "[FROM place] [TO place] [WHERE condition] [IF condition] [ORDER BY field] [LIMIT n] [PRIORITY level]"}]]}
```

## Executando uma instrução

1. Abra o Prompt de Comando em um computador que esteja na rede.
2. Digite iql, um espaço e a instrução, como iql SELECT 64 Cobblestone.
3. Pressione [[Enter]].

As palavras operation, op e sql fazem o mesmo que iql. Fora da rede, o comando se recusa a rodar e avisa que o computador não está em uma rede.

O IQL Server Management Studio é o console completo: você consulta a rede, rastreia o trabalho dela, cuida do índice e dos bloqueios e executa suas tarefas em um só lugar. Ele precisa do Midsoft IQL Server, versão 2000 ou mais recente ([](jsc:studio_overview)).

Um script é uma sequência de instruções separadas por ponto e vírgula. Dois traços iniciam um comentário que vai até o fim da linha. Um script também pode ter parâmetros, escritos como <name, type, default>, e o Studio pede os valores deles quando você o executa.

> [!WARNING] DROP destrói itens para sempre. DELETE com ALL envia para fora tudo o que a rede guarda de um item. O Studio pergunta antes de executar qualquer um dos dois, a menos que você tenha desativado essa pergunta.

Em um motor que as mantenha, você pode salvar uma consulta como visão ou procedimento. Uma tarefa executa instruções em um temporizador, ou quando uma condição é atendida. Uma tarefa precisa do Motor de Automação instalado no Mainframe ([](jsc:automation_engine)).

O livro Aprendendo IQL explica cada verbo, cláusula e erro, cada um em sua própria entrada ([](jsc:what_iql_is)).

## Se algo der errado

> [!PROBLEM] A rede recusa uma linha, com uma mensagem como verbo desconhecido.
> Confira a palavra que a mensagem indica. O Studio aponta o lugar onde o IQL parou de ler ([](jsc:iql_errors)).

> [!PROBLEM] O resultado diz que DELETE exporta para fora da rede e precisa de um destino TO.
> Acrescente TO e o nome de um barramento ([](jsc:buses)).

> [!PROBLEM] O resultado diz que MOVE é interno e precisa de FROM e TO.
> Nomeie o servidor de onde pegar e o servidor onde colocar ([](jsc:storage_servers)).

> [!PROBLEM] O resultado diz que não há nada para selecionar.
> A rede não guarda nenhum desse item. Confira o nome dele, ou armazene alguns antes ([](jsc:storage_servers)).

> [!PROBLEM] Uma visão, um procedimento ou uma tarefa é recusado, e o resultado diz que o motor da rede não mantém nenhum deles.
> Use um motor que as mantenha ([](jsc:engines)). Uma tarefa também precisa do Motor de Automação ([](jsc:automation_engine)).

> [!PROBLEM] O resultado diz que o IF não se confirmou, então a ação não foi executada.
> Confira a condição. O IF lê quanto do item a rede guarda.

> [!SEE] jsc:what_iql_is, jsc:iql_errors
