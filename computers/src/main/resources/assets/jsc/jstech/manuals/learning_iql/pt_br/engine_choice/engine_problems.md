---
section: engine_choice
order: 390
icon: jsc:mainframe
---
# Quando um motor recusa

Um motor responde a uma requisição que não consegue executar com uma mensagem curta, e a aba Serviços mostra as suas próprias mensagens. Encontre a sua mensagem nas tabelas abaixo, confira quando ela aparece e siga o que fazer.

Table: Mensagens dos motores e da aba Serviços

| Mensagem | Quando aparece | O que fazer |
| --- | --- | --- |
| `Network Operations Service unavailable` | Pede-se uma fabricação, consulta ou atualização enquanto nenhum motor está em execução. | Inicie um motor na aba Serviços, ou digite iqlengine start. |
| `No Network Operations Engine is running` | A aba Serviços, quando nenhum motor está em execução. | Pressione Iniciar... e escolha um motor. |
| `No engine is installed on this Mainframe. Install one from its disc to plan the network's work.` | A aba Serviços, quando nada está instalado. | Instale um motor pelo seu disco, em uma unidade vinculada ao Mainframe. |
| `No compatible <engine name> was found on this network.` | Um programa precisa de um motor que a rede não executa. | Instale esse motor, se estiver faltando, depois inicie-o para que a rede o execute. |
| `<program> needs an engine that offers <capability>.` | Um programa precisa de uma capacidade que o motor em execução não oferece. | Execute um motor que a ofereça. Os nomes de capacidade são EXPLAIN e PLANNER_HINTS, entre outros. |
| `No other engine is installed on this Mainframe. Install one from its disc to replace this one.` | A caixa de diálogo Substituir, quando só um motor está instalado. | Instale outro motor pelo seu disco. |
| `the Midsoft IQL Server is already installed` | O comando iqlengine, quando o verbo não mudaria nada, como iniciar um motor que já está em execução. | Leia primeiro o resultado de iqlengine status. |
| `the network has no running Mainframe to host the Midsoft IQL Server` | iqlengine, quando a rede não tem Mainframe. | Construa um Mainframe na rede e ligue-o. |
| `iqlengine needs its install disc in a linked drive` | Pede-se uma instalação, e nenhum disco correspondente está em uma unidade vinculada. | Coloque o disco em uma unidade vinculada ao Mainframe. |

Algumas recusas não mostram mensagem nenhuma. Um clique em um botão de Serviços que o servidor não aceita não faz nada, e a aba continua mostrando o estado real. Confira o estado que a aba mostra antes de pressionar novamente.

Table: Mensagens do NextgreIQL e do Prophet YourIQL

| Mensagem | Quando aparece | O que fazer |
| --- | --- | --- |
| `EXPLAIN needs a statement to explain` | Não há nada depois de EXPLAIN. | Escreva o CRAFT ou o SELECT depois de EXPLAIN. |
| `EXPLAIN shows the plans of CRAFT and SELECT` | EXPLAIN vem antes de outra instrução. | Coloque EXPLAIN somente antes de um CRAFT ou de um SELECT. |
| `hints change the plan of a CRAFT` | Uma dica vem depois de outra instrução. | Remova a dica, ou transforme a instrução em um CRAFT. |
| `MAX PARALLEL takes a whole number above nought` | MAX PARALLEL não tem número, ou tem 0. | Escreva um número inteiro igual a 1 ou maior. |
| `no server on the network is called <name>` | Um PREFER ou AVOID SOURCE não cita nenhum servidor da rede. | Escreva o nome do servidor como a rede o mostra. |
| `no item is called <name>` | Um nome de item não é reconhecido. | Confira a grafia, como em iron_ingot. |
| `the network could not start the craft of <item>` | A fabricação planejada não pode começar. | Confira o armazenamento e as máquinas da rede. |
| `nothing on the network knows how to make <item>` | Nenhuma receita fabrica o item. | Escolha um item que alguma receita fabrique. |
| `every plan for <item> was set aside` | As regras do planejador rejeitaram todos os planos. | Olhe a aba Regras do Nextgre Planner Studio. |
| `the network's engine keeps no views, procedures or jobs` | CREATE, DROP ou EXEC no NextgreIQL ou no Prophet YourIQL. | Use o Midsoft IQL Server para views e procedimentos. |
| `jobs are kept by the Automation Engine; install it on the Mainframe first` | CREATE JOB, quando o Motor de Automação não está instalado. | Instale o Motor de Automação no Mainframe. |
| `syntax: <the parser's message>` | Uma instrução não pode ser lida; a mensagem cita a palavra onde ela parou. | Corrija a palavra que a mensagem cita. |
| `syntax: KEEP item >= n, or KEEP item BETWEEN a AND b` | Um KEEP está mal formado. | Veja Prophet YourIQL: manter um nível. |
| `syntax: WATCH item < n DO statement (the comparison one of <, <=, >, >=)` | Um WATCH está mal formado. | Veja Prophet YourIQL: observadores, esquecimento e SHOW STATES. |
| `syntax: FORGET item, or FORGET WATCH n` | Um FORGET está mal formado. | Veja Prophet YourIQL: observadores, esquecimento e SHOW STATES. |
| `<name> is not kept, or no watch is numbered <n>` | FORGET cita um item ou um observador que não existe. | Confira os nomes e números com SHOW STATES. |

> [!SEE] jsc:engine_services, jsc:nextgre_hints, jsc:prophet_youriql
