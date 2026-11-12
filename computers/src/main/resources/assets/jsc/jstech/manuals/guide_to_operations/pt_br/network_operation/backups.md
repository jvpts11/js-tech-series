---
section: network_operation
order: 108
icon: jsc:tape_library
covers_family: jsc:backups
---
# Backups e a Biblioteca de Fitas

O Serviço de Backup faz o backup da rede e a devolve. Ele roda num servidor num rack, como o Serviço de Mensagens, e o Console de Backup é a sua janela em qualquer computador com desktop: o serviço guarda as tarefas e os backups, o console os define e os restaura.

> [!DEFINE] Serviço de Backup
> O serviço num servidor de rack que faz o backup da rede e a restaura.

> [!DEFINE] Console de Backup
> A janela de desktop do Serviço de Backup, em quatro abas: Tarefas, Fazer backup, Restaurar e Fitas.

Table: O que um backup guarda

| Parte | O que é |
| --- | --- |
| Itens | O que o armazenamento da rede guarda. |
| Arquivos | Os arquivos da pasta pessoal de cada computador. |
| Configurações | As configurações de cada computador. |
| Padrões .craft | As receitas dos Crafting Computers da rede. |
| Scripts IQL e Sigma# | Os scripts de cada computador, e as views, procedures e tarefas salvas do Mainframe. |
| Imagens do sistema | O sistema de cada computador, que conserta um que se corrompeu. |
| Carteiras Teracoin | Os arquivos de carteira de cada computador, cada um com a chave do seu endereço: perdido, leva as moedas junto. |

## Para onde vai

Uma tarefa grava os seus backups numa Biblioteca de Fitas ou num disco de outro servidor. Num disco, um backup é um arquivo, na pasta Backups do disco, que pesa o que o backup pesa. Uma Biblioteca de Fitas é armazenamento frio, enorme e lento: a fita enrola até o fim do que está gravado antes de gravar, e até o backup antes de ler, o que leva um tempo.

Table: As fitas

| Fita | Era | Guarda |
| --- | --- | --- |
| Rolo de 9 trilhas | Vintage | 150 itens, 150 MB |
| DLT | Legacy | 2.560 itens, 40 GB |
| LTO-3 | Transition | 1.600 itens, 400 GB |
| LTO-6 | Standard | 10.240 itens, 2,5 TB |
| LTO-9 | Advanced | 73.728 itens, 18 TB |

Numa fita, um item pesa o que pesa num disco da era da fita. Uma biblioteca aceita só as fitas da sua própria era, e serve a rede dos servidores do seu rack.

## Quando roda

Uma tarefa roda à mão, com Executar agora na aba Tarefas, ou numa hora que você lhe dá: todo dia, ou num dia da semana. Uma tarefa com hora também é uma tarefa do Motor de Automação, que executa BACKUP e o nome da tarefa nessa hora ([](jsc:automation_engine)), por isso a rede precisa do Motor de Automação no seu Mainframe.

## Restaurar

A aba Restaurar lista todos os backups que a rede guarda, os mais novos primeiro. Escolha um e depois Restaurar. Um backup num disco volta na hora; um numa fita espera a fita enrolar até ele, e a barra mostra quanto falta.

Os itens só voltam quando deixaram de existir. A rede mantém um livro de perdas: o que um disco morto levou, o que um Defrag desistiu de salvar, o que um RAID 0 perdeu com um disco e o que um DROP destruiu. Restaurar devolve, item a item, o menor entre o que o backup guarda e o que o livro perdeu desde que o backup foi feito, e cada perda volta uma vez só, de qualquer backup que você restaure. O que foi retirado, exportado ou fabricado ainda existe, por isso nunca volta: um backup desfaz um DROP, não um SELECT.

> [!DEFINE] Livro de perdas
> O registro da rede dos itens que deixaram de existir: quando, quantos, como e onde.

Arquivos, configurações, padrões e scripts são regravados por cima do que existe agora. Uma imagem do sistema volta só nos computadores que você marcar, e devolve o sistema de cada um como era, o que conserta um sistema corrompido.

## As fitas

A aba Fitas lista as fitas de uma biblioteca com as suas etiquetas, o que cada uma guarda e quando foi gravada pela última vez. Etiquetar escreve um nome numa fita. Apagar a limpa, e os backups nela vão embora para sempre. Ejetar tira a fita do seu encaixe, na frente do rack. Nenhum dos dois mexe na fita em que a unidade está trabalhando.

## Se algo der errado

> [!PROBLEM] O console diz que nenhum Serviço de Backup está rodando nesta rede.
> Instale o Serviço de Backup num servidor num rack da rede e deixe o servidor rodar.

> [!PROBLEM] A última execução de uma tarefa diz sem espaço.
> As suas fitas ou o seu disco estão cheios. Ponha uma fita em branco na biblioteca, apague uma ou escolha outro disco.

> [!PROBLEM] A última execução de uma tarefa diz sem para onde ir.
> A sua biblioteca não tem fitas, ou o seu servidor ou disco sumiu. Edite a tarefa e escolha onde ela grava.

> [!PROBLEM] Uma tarefa com hora nunca roda.
> Instale o Motor de Automação no Mainframe; a barra de status avisa quando não há nenhum.

> [!PROBLEM] Uma restauração não devolve nada.
> Nada do que o backup guarda se perdeu desde que foi feito, ou uma restauração já o devolveu.

> [!SEE] jsc:network_services, jsc:rack_equipment, jsc:automation_engine
