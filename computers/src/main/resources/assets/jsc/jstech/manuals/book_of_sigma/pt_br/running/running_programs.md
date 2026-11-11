---
section: running
order: 479
---
# Programas e processos

Um programa é um arquivo que o computador executa para você. Você o escreve em Sigma ou Sigma#, e o compilador o transforma numa listagem (um arquivo que termina em .asm) que o computador executa. Veja [Compilando com sgsc e scc](jsc:sigma_compile).

Enquanto executa, o programa é um processo: uma entrada na lista de programas em execução do computador, com número, nome e memória próprios. A lista fica no computador, não nos seus discos. Um programa que você instalou fica num disco e não faz nada até ser iniciado.

Um processo sai da lista quando termina. Desligar o computador interrompe todos eles. O que acontece com eles depois de um save está em [Programas e o save do mundo](jsc:saved_with_world).

## O número e o nome

Cada processo tem um número, e o primeiro é o número 1. O computador conta a partir de 1 e pula cada número que um processo em execução ainda usa. Depois do maior número que ele consegue guardar, recomeça no número 1. Um programa pode dar um nome a si mesmo com Program.SetName. Até lá, o nome dele é sigma.

## Iniciando um programa no prompt

Depois que o runtime é instalado ([Instalando os compiladores e o runtime](jsc:sigma_installing)), o prompt passa a conhecer o comando sigma. Os comandos dele são estes:

Table: Comandos no prompt

| Comando | O que faz |
| --- | --- |
| `sigma run <file>` | Inicia o programa a partir do seu arquivo, e passa a ele as palavras que vêm depois do nome do arquivo como argumentos. |
| `sigma run <file> --heap 16M` | Define a memória que o programa recebe, em megabytes: escreva 16M ou 16. |
| `sigma ps` | Lista os programas em execução, o mesmo que sigma list ou apenas sigma. |
| `sigma stop <id>` | Encerra o programa com esse número, o mesmo que sigma kill. |

1. Digite sigma run, o nome do arquivo e os argumentos que quiser, depois pressione [[Enter]].
2. Digite sigma ps para ver o número do programa e o que ele está fazendo.

Um programa que fica ativo em segundo plano é um serviço. O prompt não espera por ele e informa com qual número ele foi iniciado. Qualquer outro programa mantém o prompt até retornar, e tudo o que você digitar vai para esse programa.

A lista tem uma linha por processo. Sem nada em execução, o prompt diz que nenhum programa está em execução.

Table: As colunas de sigma ps

| Coluna | O que mostra |
| --- | --- |
| id | O número do programa, que sigma stop recebe. |
| name | O nome do programa, cortado em 20 caracteres, com um til (~) no fim quando for cortado. |
| state | O que o processo está fazendo, como explica a tabela abaixo. |
| memory | A memória que ele ocupa, de toda a memória que recebeu: bytes abaixo de um kilobyte (KB), kilobytes acima disso. |

Table: Os estados de um processo

| Estado | O que significa |
| --- | --- |
| `input` | Parou numa leitura e espera uma linha que você digita. |
| `running` | Ainda tem instruções para executar. |
| `parked` | Espera por algo fora dele, e não usa turnos até que isso termine. |
| `finished` | Executou até o fim. |
| `halted` | Parou por causa de um erro, e a mensagem dele foi escrita no console. |

Uma parada (halt) é uma interrupção causada por um erro nos próprios passos do programa. Os motivos de cada uma estão em [As paradas durante a execução de um programa](jsc:sigma_runtime_stops).

## Encerrando um programa

Um programa termina de uma de três formas: retorna do método onde começou, chama Program.Exit com um número, ou você o para. O número com que ele termina é o seu código de saída. Um programa que retorna por conta própria termina com código de saída 0, a menos que tenha definido outro código. Um programa interrompido informa código de saída 1.

Quando você para um programa, ele recebe uma despedida: pode executar os seus próprios passos de encerramento antes de ser removido. Os pais dele são avisados de que ele terminou. A despedida tem um orçamento, descrito em [Quanto uma máquina executa a cada tick](jsc:credits_per_tick).

Um processo que já terminou sai da lista assim que nada mais espera por ele. Um processo que outro programa iniciou mantém sua saída e seu código de saída até que esse pai deixe de existir.

## Console e script

Um programa tem uma de duas formas, console ou script. Um programa de console começa no seu método de entrada, mantém o prompt enquanto executa, escreve no terminal que o iniciou e some quando retorna.

Qualquer outra forma é um script. Um script é configurado uma vez, é chamado de novo a cada tick enquanto o computador estiver ligado, e é avisado quando é parado. Depois que o mundo é carregado de novo, ele volta.

## Threads

Um processo começa com uma thread, a thread principal, que sempre tem o número 1. Um programa cria mais threads com Thread.Start, e cada nova thread recebe o próximo número: thread 2, depois thread 3, e assim por diante. As threads de um processo compartilham a sua memória, mas cada uma mantém a sua própria posição nas chamadas que está fazendo.

O computador dá turnos ao processo, e o processo divide esses turnos entre as suas threads que estão prontas para executar. Cada turno é uma fatia de no máximo 64 instruções. Uma thread que espera por algo não recebe turno até poder continuar.

Iniciar uma thread custa 49 instruções além da chamada, porque cada thread mantém uma pilha de chamadas própria.

Table: Chamadas de thread

| Chamada | O que faz |
| --- | --- |
| `Thread.Start(action)` | Inicia outra thread, que executa a ação que você indicar. |
| `Thread.Sleep(ticks)` | Espera tantos ticks e depois continua. |
| `Thread.Yield()` | Abre mão do resto do seu turno. |
| `Thread.Join()` | Espera até que essa thread termine. |
| `Thread.Join(ticks)` | Espera até tantos ticks, e retorna false se a thread não tiver terminado até lá. |
| `Thread.Stop()` | Encerra essa thread. |
| `Thread.Id` | O número da thread, que é 1 para a thread principal. |

Cada objeto pode ter um bloqueio. Liberar um bloqueio que você não possui interrompe o programa. [Threads e bloqueios](jsc:threads_and_locks) explica como pegar e liberar um.

> [!DEFINE] Processo
> Um programa em execução num computador, com número, nome e memória próprios.

> [!DEFINE] Tick
> Uma batida do relógio do jogo. Vinte ticks fazem um segundo.

> [!DEFINE] Instrução
> Um passo simples do trabalho de um programa. Cada passo que um programa dá custa uma instrução.

## Se algo der errado

> [!PROBLEM] O prompt responde com o nome e a extensão do arquivo, na forma "nome: nada instalado executa um .ext".
> Nenhuma linguagem instalada executa arquivos desse tipo. Compile um arquivo-fonte com sgsc ou scc antes, ou instale o pacote que executa esse tipo de arquivo ([a página de programas](jsc:programs)).

> [!PROBLEM] O prompt responde com o nome do arquivo e "não compila", sem dar nenhum motivo.
> O prompt não deu motivo. Execute o compilador no arquivo você mesmo para ver as mensagens. [Como ler uma linha de erro](jsc:sigma_reading_an_error) mostra como interpretá-las.

> [!PROBLEM] O prompt diz "<file> não é um programa <language>".
> sigma run só inicia arquivos que terminam em .asm, .sgs ou .sg. Inicie um desses.

> [!PROBLEM] O prompt responde "sigma: nada está em execução como <id>".
> O número está errado, ou esse programa já terminou. Digite sigma ps para ver os números que estão em execução.

> [!SEE] jsc:sigma_run, jsc:sigma_entry_points
