---
section: iql_start
order: 362
---
# O que são IQL e Operações

Seus itens não ficam guardados em um único baú. Eles estão nos discos da rede: um conjunto de computadores e dispositivos ligados por cabo de dados a um Mainframe. Para tirar um item, você não abre uma caixa. Você pede o item à rede, e a rede o busca para você.

> [!DEFINE] rede
> Cada computador e dispositivo ligado por cabo de dados a um Mainframe.

> [!DEFINE] Mainframe
> O único computador de uma rede que executa o trabalho dela e mantém o controle dele.

Você pede em IQL, a Linguagem de Consulta de Itens (Item Query Language). O IQL é a linguagem própria da rede, e se parece com as linguagens de bancos de dados: uma palavra para a ação, depois o item e a quantidade. As palavras dele vêm do SQL (Structured Query Language), a linguagem que os bancos de dados falam.

> [!DEFINE] IQL
> A linguagem de consulta própria da rede: as palavras que você digita para pedir itens a ela e para dizer que trabalho ela deve fazer.

Cada linha que você digita é uma instrução. Uma instrução ou faz uma pergunta, ou pede trabalho. QUERY faz uma pergunta, e SHOW significa o mesmo. Uma pergunta recebe sua resposta em linhas, e não altera nada.

> [!DEFINE] instrução
> Uma instrução IQL completa, digitada em sua própria linha.

> [!DEFINE] consulta
> Uma pergunta feita à rede, que ela responde com linhas e sem alterar nada.

> [!DEFINE] linha
> Uma linha de uma resposta, como um disco ou um item, com os seus detalhes.

Quando uma instrução pede trabalho, a rede a transforma numa Operação. Uma Operação é uma ação que a rede executa. Ela espera a sua vez, e a rede trabalha nela ao longo dos ticks do jogo.

> [!DEFINE] Operação
> Uma ação que a rede executa, desde o momento em que é pedida até terminar.

> [!DEFINE] tick
> Uma batida do jogo. 20 ticks equivalem a um segundo.

Tome SELECT 64 iron_ingot. Isso pede à rede que traga 64 lingotes de ferro até você. É uma Operação. A resposta apenas diz que a Operação está na fila. Os itens chegam até você conforme a rede trabalha nela.

Table: As partes de uma instrução

| Parte | O que é | Exemplo |
| --- | --- | --- |
| Ação | A palavra que diz o que fazer, como SELECT ou QUERY | `SELECT` |
| Quantidade | Quantas. Você pode omitir. | `64` |
| Item | O nome do item, como uma palavra só. | `iron_ingot` |
| Palavras extras | Palavras como WHERE, que restringem a resposta. | `WHERE qty > 100` |

A quantidade é opcional. Deixe-a de fora com SELECT, e a rede traz tudo o que tem daquele item. A palavra ALL significa o mesmo. A quantidade é um número inteiro: 50% é recusado.

Escreva o nome de um item como uma palavra só, com um sublinhado no lugar de cada espaço. iron_ingot significa minecraft:iron_ingot, o lingote de ferro do Minecraft. Um nome com dois-pontos, como mod:item, é tomado como o nome completo.

Cada Operação também tem uma prioridade. Ela é MEDIUM, a menos que você diga o contrário. Escreva PRIORITY e um nível, como HIGH, no fim de uma instrução para defini-la ([](jsc:priorities)).

Uma Operação termina em um dos oito estados, e a rede informa qual ([](jsc:operation_states)).

## Se algo der errado

> [!PROBLEM] A resposta diz "a rede não tem um Mainframe em execução".
> O Mainframe está desligado, ou não está nesta rede. Ligue-o, ou junte o computador à rede dele ([](jsc:first_network)).

> [!PROBLEM] A Operação continua em PENDING.
> O Mainframe está ocupado, desligado ou não tem tempo de processador sobrando para a rede. Espere um pouco, ou dê ao Mainframe mais processadores ou placas de vídeo ([](jsc:mainframes)).

> [!PROBLEM] A Operação termina em RESOURCE_LOCKED.
> Outra Operação, ou um LOCK, retém um item de que precisa. Espere a outra Operação terminar, ou leia sobre os bloqueios ([](jsc:lock_unlock)).

> [!PROBLEM] A Operação termina em COMPLETED_PARTIAL.
> A rede tinha menos do que você pediu. Peça uma quantidade menor. QUERY items mostra quanto de cada item a rede tem.

> [!SEE] jsc:operations_catalogue, jsc:where_to_type_iql, jsc:query_and_select
