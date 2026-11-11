---
section: the_achievements
order: 172
icon: jsc:mainframe
---
# Conquistas de rede e de Operações

A aba Redes e Operações acompanha o trabalho de uma rede: um Mainframe em uma rede, computadores entrando nela, Operações e IQL (a linguagem de consulta da rede), autofabricação e os problemas em que uma rede pode se meter. A tabela diz o que cada uma pede e como o jogo a verifica.

Table: As conquistas de rede e de Operações

| Conquista | O que pede | Como é ganha |
| --- | --- | --- |
| Redes e Operações (raiz) | Coloque um Mainframe em uma rede. | Um Mainframe entra em uma rede pela primeira vez. |
| Uma Série de Tubos (tarefa) | Conecte um computador à rede de um Mainframe. | Um computador entra em uma rede que tem um Mainframe. |
| SELECT \* FROM chest (tarefa) | Tire itens do armazenamento de rede com uma Operação. | Uma Operação SELECT termina. |
| DROP DATABASE production_database (tarefa) | Execute uma instrução IQL. | Uma instrução IQL que você digita termina sem erro. |
| Configure e Esqueça (meta) | Faça um JOB do IQL rodar sozinho. | Um JOB do IQL roda sozinho, e o operador do Mainframe recebe o crédito. |
| Dois em Três Não É Ruim (segredo) | Faça uma Operação terminar apenas parcialmente feita. | Uma Operação termina como PARTIAL, como o terminal a mostra. |
| Deadlock (segredo) | Faça uma Operação desistir de esperar por um recurso bloqueado. | Uma Operação termina como RECURSO BLOQUEADO, como o terminal a mostra. |
| Receita de Sucesso (tarefa) | Codifique um padrão de fabricação. | Um Codificador de Padrões codifica um padrão. |
| A Fábrica Deve Crescer (tarefa) | Termine uma autofabricação. | Uma autofabricação termina. |
| Machine Learning (meta) | Termine uma autofabricação por meio de uma máquina. | Uma máquina termina um passo de uma autofabricação, ou uma autofabricação executa um dos seus passos em uma máquina. |
| Problemas na Cadeia de Suprimentos (desafio) | Termine uma autofabricação de dez estágios ou mais. | Uma autofabricação com dez ou mais estágios termina. |
| Just-in-Time (tarefa) | Coloque um Barramento de Importação e um Barramento de Exportação em uma mesma rede. | Uma peça de cabo de barramento é colocada em uma rede que já tem o outro tipo de barramento. |
| Quem Desligou o Servidor? (segredo) | Corte o último cabo entre um computador ligado e o seu Mainframe. | Um computador ligado perde a sua última rota até um Mainframe. |
| Só Pode Haver Um (segredo) | Coloque dois Mainframes em uma mesma rede. | Começa um conflito de rede. O chat também o anuncia. |
| Conversando com os Vizinhos (meta, só com ComputerCraft) | Ouça um computador do ComputerCraft por meio de um Gateway de Rede. | Um Gateway de Rede recebe uma mensagem de um computador do ComputerCraft. |
| Information Superhighway (meta) | Ligue duas redes pelos seus WAN Gateways. | Dois WAN Gateway Computers levantam a sua linha em federação. |
| Pretty Fly for a Wi-Fi (tarefa) | Ligue um computador ou um portátil a um ponto de acesso. | Um computador ou um portátil se liga por um ponto de acesso sem fio. |
| Trabalho Remoto (tarefa) | Peça itens à rede num portátil. | O app de rede de um portátil pede itens à rede. |
| Foto ou Não Aconteceu (tarefa) | Tire uma foto num portátil e envie-a a um computador. | Uma foto da galeria de um portátil é gravada num computador. |
| Mineração de Dados (tarefa) | Ache um veio com o app de prospecção. | O app de prospecção acha um veio à sua volta. |
| a/s/l? (tarefa) | Envie uma mensagem no Messenger. | Você diz algo numa sala do Messenger, num computador ou num portátil. |
| Thin Client (tarefa) | Inicie um computador pela rede. | Um computador inicia um sistema que o Mirror serve, por PXE ou HTTP Boot. |

Uma Operação que você pediu credita você. Uma Operação agendada credita o operador do Mainframe ([](jsc:mainframes)), e o mesmo acontece com um job do IQL que roda sozinho.

Dois em Três Não É Ruim e Deadlock nomeiam duas das formas como uma Operação pode terminar: [os oito estados de uma Operação](jsc:operation_states) explica todas elas.

Conversando com os Vizinhos aparece somente quando o mod ComputerCraft está instalado junto com o J's Computers, e a conquista precisa de um [Gateway de Rede](jsc:network_gateway).

## Se algo der errado

> [!PROBLEM] Machine Learning não aparece, mesmo com as autofabricações terminando.
> Ela precisa de uma máquina para executar um passo da fabricação. A Fábrica Deve Crescer conta qualquer autofabricação.

> [!PROBLEM] Just-in-Time não aparece com um só barramento.
> Coloque o outro tipo de barramento na mesma rede: um Barramento de Importação com um Barramento de Exportação.

> [!PROBLEM] Conversando com os Vizinhos está faltando.
> Ela precisa do ComputerCraft e de um Gateway de Rede que ouça um dos seus computadores.

> [!SEE] jsc:what_a_network_is, jsc:what_operations_are, jsc:how_autocrafting_works
