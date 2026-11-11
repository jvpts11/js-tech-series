---
section: library_network
order: 446
---
# Iql: a linguagem da rede a partir de um programa

IQL é a linguagem de consulta da rede. Uma instrução do IQL pede algo, ou altera o que a rede guarda e faz. A linguagem em si está em [](jsc:iql), e o livro Aprendendo IQL a ensina do zero em [](jsc:what_iql_is).

Iql permite que um programa envie essas instruções por conta própria, em vez de você digitá-las em um prompt. Toda chamada de Iql precisa de uma rede com um Mainframe. Só programas Sigma# podem usá-la.

Table: Membros de Iql

| Membro | O que faz |
| --- | --- |
| `Iql.Run(statement)` | Roda uma instrução e devolve um IqlResult. Uma instrução recusada não para o programa. |
| `Iql.Query(statement)` | Roda uma instrução e devolve suas linhas como uma lista de mapas. Uma instrução recusada para o programa. |
| `Iql.Exec(statement)` | Roda uma instrução e devolve um IqlResult. |
| `Iql.Exec(statement, values)` | O mesmo, com os valores passados para uma instrução salva. |
| `Iql.RunFile(path)` | Roda cada instrução do arquivo e devolve um IqlResult. |

Exec coloca EXEC na frente da sua instrução e acrescenta cada valor depois da instrução, um após o outro. Use-o para passar valores a uma instrução salva.

RunFile lê o arquivo primeiro. Quando o arquivo não pode ser lido, o programa para com a mensagem do arquivo.

## O que os resultados guardam

Table: Registros de Iql

| Registro | Campos | O que cada campo guarda |
| --- | --- | --- |
| `IqlResult` | `IqlResult.Ok, IqlResult.Message, IqlResult.Rows` | Ok: sim quando a rede aceitou a instrução. Message: a resposta da rede. Rows: as linhas do resultado. |

Cada linha é um mapa com três chaves: name, quantity e detail. O name é o item ou a coisa a que a linha se refere, quantity é a contagem dela, e detail diz mais sobre ela.

> [!DEFINE] Linha
> Uma linha de um resultado, dada como um mapa de nome, quantidade e detalhe.

## Se algo der errado

> [!PROBLEM] O programa para e diz que este computador não está numa rede com um Mainframe.
> Monte uma rede com um Mainframe e confira Mainframe.Online antes de chamar Iql.

> [!PROBLEM] Query para o programa com o texto do motor.
> Use Run ou Exec. Eles devolvem Ok false e a mensagem, e o programa segue.

> [!PROBLEM] Ok é false, e a mensagem diz que a instrução foi recusada.
> Leia a mensagem. É a resposta da rede, e os verbos são explicados em [](jsc:iql).

> [!PROBLEM] RunFile para o programa com a mensagem do arquivo.
> Confira o caminho com File.Exists antes de chamar RunFile.

> [!SEE] jsc:iql, jsc:what_iql_is, jsc:mainframes
