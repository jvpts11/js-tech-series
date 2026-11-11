---
section: iql_limits_and_errors
order: 382
---
# Mensagens de erro e suas correções

Uma recusa é uma mensagem de erro que interrompe uma instrução antes de ela rodar. Você a lê do início: as primeiras palavras dizem o que deu errado, e as palavras entre colchetes angulares mostram onde entra o seu próprio texto. Encontre a mensagem abaixo e leia a correção ao lado dela.

## A forma de uma instrução

Table: Recusas da forma

| A mensagem | O que significa, e a correção |
| --- | --- |
| `empty statement` | A linha não tem nada. Digite uma instrução. |
| `unknown verb: <word>` | A primeira palavra não é um verbo. Confira a grafia dela com os verbos deste livro. |
| `unexpected token: <text>` | Uma palavra ou sinal aparece onde a instrução não o aceita. Remova-o ou use um verbo que o aceite. |
| `percentage quantities are not supported yet: <quantity>` | Digite um número inteiro, ou ALL. |
| `LIMIT takes a row count, not a percentage: <quantity>` | Digite um número inteiro de linhas depois de LIMIT. |
| `unknown priority level: <word> (expected LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH or HIGH)` | Digite um dos cinco níveis. |
| `DELETE exports out of the network and needs a TO destination` | Acrescente TO e o nome de um barramento. |
| `MOVE is internal and needs both FROM and TO` | Acrescente FROM e TO. |
| `<VERB> has no TO destination` | Remova o TO. SELECT, INSERT, DROP e UPDATE não aceitam nenhum. |
| `<VERB> is not valid on a read` | Um QUERY recusa FROM, TO, IF e PRIORITY. Coloque o servidor no WHERE, no lugar delas. |
| `UPDATE needs SET and what the card is to do, after the item` | Acrescente SET e, depois, a palavra de ação. |
| `unknown action: <word> (expected SMELT, ENCHANT, REPAIR, COMBINE or NAME)` | Use uma das cinco palavras de ação. |
| `an offer is 1, 2 or 3, not <n>` | Digite 1, 2 ou 3. |
| `a name is at most 50 characters long` | Encurte o novo nome da bigorna para 50 caracteres ou menos. |
| `a condition nests at most 64 NOTs and brackets deep` | Remova alguns NOT ou parênteses. |
| `a condition compares at most 256 things` | Encurte a condição. |
| `<ACTION> takes no WITH` | Só REPAIR e COMBINE aceitam WITH, então remova-o. |
| `COMBINE needs WITH and the item it adds` | Acrescente WITH e o segundo item. |
| `unknown operator: <text>` | Use um dos operadores do WHERE ([](jsc:query_and_select)). |
| `unknown duration unit '<unit>' in: <text>` | Use t, s, m ou h como unidade, ou nenhuma para segundos. |

Table: Partes faltando: esperado

| A mensagem | O que significa, e a correção |
| --- | --- |
| `expected a verb` | Digite um verbo no início, como QUERY. |
| `expected an item` | Digite o id do item depois do verbo. |
| `expected an object to query` | Digite o objeto depois de QUERY, como items. |
| `expected a quantity, got: -5` | Uma quantidade é um número inteiro igual ou maior que zero. |
| `expected a location after FROM` | Digite o nome do servidor ou do barramento como uma palavra só. |
| `expected a location after TO` | Digite o nome do barramento como uma palavra só. |
| `expected BY` | Digite BY depois de ORDER. |
| `expected a sort field after ORDER BY` | Digite um nome de campo depois de ORDER BY. |
| `expected a level after PRIORITY (LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH)` | Digite um dos cinco níveis depois de PRIORITY. |
| `expected a row count after LIMIT` | Digite um número inteiro depois de LIMIT. |
| `expected a field name` | Digite um nome de campo, como qty. |
| `expected a comparison operator` | Digite um operador, como = ou >. |
| `expected a value` | Digite um valor depois do operador. |
| `expected an action after SET (SMELT, ENCHANT, REPAIR, COMBINE, NAME)` | Digite uma palavra de ação depois de SET. |
| `expected an offer after OFFER (1, 2 or 3)` | Digite 1, 2 ou 3. |
| `expected a name in quotes after NAME` | Coloque o novo nome entre aspas. |
| `expected the pattern's name` | Digite o nome do padrão. |
| `expected one of the pattern's inputs` | Digite uma das entradas do padrão. |
| `expected the router's name` | Digite o nome do roteador. |
| `expected a list of items` | Digite pelo menos um item ou tag. |
| `expected a count` | Digite um número inteiro. |

## As respostas da rede

Table: Recusas da rede

| A mensagem | O que significa, e a correção |
| --- | --- |
| `Network Operations Service unavailable` | Nenhum motor de que a instrução precisa roda no Mainframe. Inicie um ([](jsc:iql_engines_overview)). |
| `the network has no running Mainframe` | Um Mainframe precisa estar na rede ([](jsc:mainframes)). |
| `syntax: <reason>` | O motor não conseguiu ler a instrução. O motivo depois dela diz o que está errado. |
| `unknown item: <name>` | O nome não é um item, ou o WHERE não manteve nenhuma linha para ele. Confira a grafia e o WHERE. |
| `no server named '<server>'` | O servidor não existe. Confira o nome dele. |
| `no server or bus named '<name>'` | Nem um servidor nem um barramento tem esse nome. |
| `no bus named '<bus>'` | Nenhum barramento tem esse nome na rede. |
| `the bus '<bus>' touches no inventory` | O barramento não tem baú nem máquina ao lado. |
| `the network holds no <item>` | Nenhuma cópia do item está na rede. |
| `no pattern crafts <item>` | Nenhuma receita da rede fabrica o item. |
| `this computer has no <card name>` | Coloque esse cartão neste Computador Pessoal. |
| `only a Personal Computer's own cards change items for the network` | Execute a instrução num Computador Pessoal. |
| `somebody has to ask for it: the card works it for them, as they would at the block` | Execute a instrução a partir do terminal de um jogador. |
| `the <item> does not smelt` | Escolha um item que possa ser fundido. |
| `the <item> takes no enchantment` | Escolha um item que aceite um encantamento. |
| `the <item> is not worn` | REPAIR precisa de uma cópia danificada. |
| `the network holds nothing that mends the <item>` | Coloque um material na rede, ou dê o nome de um depois de WITH. |
| `maintenance runs on the Mainframe only` | Digite o comando no próprio Mainframe. |
| `the network's engine keeps no views, procedures or jobs` | Use o [Midsoft IQL Server](jsc:midsoft_iql_server). |
| `jobs are kept by the Automation Engine; install it on the Mainframe first` | Instale o Motor de Automação no Mainframe. |
| `the destination server is unavailable` | O destino não pode aceitar itens agora. |
| `could not start the MOVE` | A rede não iniciou a movimentação. |
| `could not start the UPDATE` | A rede não iniciou a alteração. |
| `nothing to lock: the network holds no free <item>` | Espere o estoque livre e, então, bloqueie-o. |
| `<item> is already locked` | A retenção já está em vigor. |
| `<item> is not locked` | Não havia retenção para liberar. |

## Configurações de barramentos, interfaces e redstone

Table: Recusas das configurações

| A mensagem | O que significa, e a correção |
| --- | --- |
| `SET BUS needs the bus's name, in quotes when it has spaces` | Digite o nome do barramento depois de BUS. |
| `SET BUS needs a setting: ON, OFF, MODE, FILTER, MATCH, KEEP, MAX, PRIORITY, WHEN, AFTER BUS or ACCESS` | Acrescente uma palavra de configuração depois do nome. |
| `a bus has no setting <word>` | Use uma configuração da tabela de SET BUS. |
| `not an hour of the day: <text>` | Digite uma hora de 0 a 23. |
| `SET INTERFACE needs the interface's name, in quotes when it has spaces` | Digite o nome da interface depois de INTERFACE. |
| `SET ROUTER needs the router's name, in quotes when it has spaces` | Digite o nome do roteador depois de ROUTER. |
| `SET INTERFACE needs a setting: EXCLUSIVE, MAX JOBS or ROUTE` | Acrescente uma dessas três configurações depois do nome. |
| `an interface has no setting <word>` | Use EXCLUSIVE, MAX JOBS ou ROUTE. |
| `RENAME needs TO and the new name` | Escreva TO e o novo nome depois do nome antigo. |
| `SET REDSTONE needs the interface's name, in quotes when it has spaces` | Digite o nome da interface depois de REDSTONE. |
| `SET REDSTONE needs IN, or OUT and a strength from 0 to 15` | Acrescente IN, ou OUT seguido de um número. |
| `not a redstone strength, which runs from 0 to 15: <text>` | Digite um número inteiro de 0 a 15. |

## Instruções salvas, durações e arquivos

Table: Mensagens de instruções salvas, durações e arquivos

| A mensagem | O que significa, e a correção |
| --- | --- |
| `CREATE <TYPE> needs a name and an AS body` | Digite um nome, depois AS e o corpo, nessa ordem. TYPE é VIEW, PROCEDURE ou JOB. |
| `expected AS after the <TYPE> name` | Digite AS depois do nome. |
| `CREATE <TYPE> needs a body after AS` | Digite o corpo depois de AS. |
| `CREATE JOB needs a trigger: EVERY <duration> or WHEN <condition>` | Acrescente EVERY e uma duração, ou WHEN e uma condição. |
| `CREATE JOB needs a body before its trigger` | Coloque a instrução antes de EVERY ou WHEN. |
| `CREATE JOB trigger needs a value` | Digite a duração ou a condição depois da palavra de gatilho. |
| `DROP <TYPE> needs a name` | Digite o nome depois do tipo. |
| `EXEC needs a procedure name` | Digite o nome depois de EXEC. |
| `EXEC takes a single procedure name` | Execute um procedimento por EXEC. |
| `empty duration` | Digite uma duração depois de EVERY, como 30s. |
| `not a duration: <text>` | Digite um número inteiro, com t, s, m ou h depois dele. Um decimal como 1.5s é recusado. |
| `duration must be >= 0: <text>` | Digite uma duração igual ou maior que 0. |
| `no view named <name>` | Nenhuma visão tem esse nome. |
| `no procedure named <name>` | Nenhum procedimento tem esse nome. |
| `no job named <name>` | Nenhuma tarefa tem esse nome. |
| `procedure <name> stopped at statement <n>: <error>` | Corrija a instrução que falhou e execute o EXEC de novo. |
| `IQL recursion too deep (a procedure or view referencing itself?)` | Uma instrução salva executa a si mesma. Remova o laço. |
| `IQL stopped after 4096 statements for one request (procedures running others too many times?)` | Dê aos procedimentos menos instruções para executar. |
| `the IF did not hold, so the <VERB> was not run` | Não é uma falha: o IF deu falso. Confira o valor do campo que ele compara. |
| `<file>: syntax error in '<statement>': <reason>` | Uma linha do arquivo não é uma instrução. Nada roda até que a linha seja corrigida. |
| `<file>: only .iql files can be run (got .<ext>)` | Só roda um arquivo que termina em .iql. Renomeie o arquivo. |
| `<file>: nothing to run` | O arquivo não tem nenhuma instrução. |
| `<file>: QUERY/COUNT are not supported by 'run', use 'operation' instead` | Um arquivo não pode executar uma leitura. Digite a leitura no prompt. |

Um arquivo roda uma instrução por linha. Linhas em branco e linhas que começam com -- são ignoradas. Um ponto e vírgula não divide uma linha num arquivo, então coloque uma instrução em cada linha. O arquivo para na primeira instrução que falha, e as instruções acima dela já foram executadas. O arquivo inteiro é lido primeiro, então um erro de sintaxe em qualquer linha significa que nada roda.

## Se algo der errado

> [!PROBLEM] um nome tem no máximo 50 caracteres
> Encurte o nome. Só o novo nome de uma bigorna tem este limite.

> [!PROBLEM] item desconhecido: <name>
> Confira a grafia e, depois, confira se o WHERE mantém a linha que você quer.

> [!SEE] jsc:iql_limits, jsc:query_and_select, jsc:iql_engines_overview
