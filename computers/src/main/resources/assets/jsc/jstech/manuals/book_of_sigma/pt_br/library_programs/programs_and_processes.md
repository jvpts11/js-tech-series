---
section: library_programs
order: 443
---
# Programas, processos e mensagens

Um programa pode iniciar outros programas, perguntar quais programas rodam na máquina dele e receber mensagens deles. Um programa que você inicia vira um processo ([](jsc:running_programs)), com seu próprio número, sua própria memória e seu próprio trabalho a fazer.

Program é o tipo do próprio programa e dos programas que ele inicia. Program.Start devolve um Handle: um valor que se refere ao novo processo pelo seu número.

Table: Membros de programa

| Membro | O que faz |
| --- | --- |
| `Program.SetName(name)` | Dá o nome ao programa. A lista de processos da máquina mostra esse nome. Um programa sem nome recebe um do runtime. |
| `Program.Name` | O nome que o programa tem, como texto. |
| `Program.Args` | Os argumentos com que o programa foi iniciado, como uma lista de texto. |
| `Program.Exit(status)` | Encerra o programa com esse status. |
| `Program.DroppedEvents` | Quantos eventos o programa perdeu, porque a fila dele estava cheia. Somente Sigma#. |
| `Program.Start(path)` | Inicia o programa no caminho, com argumentos e prioridade opcionais, e devolve um Process. Somente Sigma#. |
| `Program.Current` | O próprio programa, como um Process. Somente Sigma#. |
| `Program.Shell(line)` | Executa uma linha no prompt desta máquina, espera ela terminar e devolve as linhas que ela imprimiu. Somente Sigma#. |
| `Program.OnMessage(handler)` | Define o manipulador que roda para cada mensagem que outros programas enviam a este. Somente Sigma#. |

Program.Start compila um arquivo de código-fonte (.sg ou .sgs) antes de iniciá-lo. Um arquivo compilado roda como está.

Start recebe o caminho, depois argumentos opcionais, depois uma prioridade opcional. A prioridade é um nome: low, medium ou high, em maiúsculas ou minúsculas. Qualquer outro nome, ou nenhum, dá medium. Um programa low recebe a sua vez apenas a cada duas rodadas, e um programa high roda como um medium.

Um programa iniciado por outro programa recebe 1 megabyte (MB) de memória, o heap dele. O máximo que um heap pode ter é 64 MB.

## Processo

Um Process é um Handle para um programa em uma máquina. Você obtém um a partir de Start, de Current e de Processes em outra máquina.

Table: Membros de processo

| Membro | O que faz |
| --- | --- |
| `Process.Id` | O número do processo. |
| `Process.Name` | O nome do processo, como texto. |
| `Process.Host` | Vazio nesta máquina, e o nome de host de uma máquina da rede para um processo remoto. Somente Sigma#. |
| `Process.Running` | Sim enquanto o processo roda. |
| `Process.ExitCode` | O status com que o processo terminou. |
| `Process.Wait()` | Espera até o processo terminar. |
| `Process.Wait(ticks)` | Espera no máximo esse número de ticks. Devolve sim quando o processo terminou nesse tempo. |
| `Process.Kill()` | Interrompe o processo. |
| `Process.Output()` | As linhas que o processo imprimiu, como uma lista. |
| `Process.Send(id, text)` | Envia uma mensagem ao processo com esse número. Devolve false quando a fila dele não tem espaço. |

Uma espera termina antes quando o programa que espera, ou aquele pelo qual ele espera, termina. Os tempos são em ticks, e o tick é explicado em [](jsc:credits_per_tick).

## Mensagens e eventos

Uma mensagem é um texto que um programa envia a outro. O receptor a recebe como um ProcessMessage, com o número do remetente (ProcessMessage.From), o texto (ProcessMessage.Text) e o tick em que foi enviada (ProcessMessage.Tick).

Um programa não age sobre uma mensagem no instante em que ela chega. Cada mensagem, cada clique e cada mudança em um total observado espera numa fila como um evento. O programa roda os manipuladores na ordem em que os eventos chegaram.

Table: Estados de processo

| Estado | O que significa |
| --- | --- |
| `running` | O processo tem turnos para rodar. |
| `parked` | O processo espera por algo fora dele. |
| `finished` | O processo terminou. |
| `halted` | Um erro interrompeu o processo. |

Um programa guarda no máximo 256 chamadas em espera. Um evento que não encontra espaço é descartado e contado em Program.DroppedEvents. Uma mensagem é a exceção: ela é recusada, e quem a enviou é avisado. Fechar uma janela sempre passa.

> [!DEFINE] Handle
> Um valor que se refere a um processo pelo seu número.

> [!DEFINE] Heap
> A memória que um programa tem para os próprios valores. 1 MB por padrão quando outro programa o inicia.

> [!DEFINE] Manipulador
> A parte do seu programa que roda quando um evento chega.

> [!DEFINE] Evento
> Algo que o programa é avisado: uma mensagem, um clique ou uma mudança que ele observa.

## Se algo der errado

> [!PROBLEM] Program.Start interrompe o programa: nada instalado roda um arquivo desse tipo.
> Inicie um programa compilado, ou compile o arquivo-fonte antes.

> [!PROBLEM] Program.Start interrompe o programa: o programa precisa de mais memória do que há livre.
> Feche um programa para liberar memória, depois inicie-o de novo.

> [!PROBLEM] O início para com o texto do compilador, porque o arquivo-fonte não compila.
> Leia o erro que o compilador dá e corrija a linha que ele indica. Os erros são explicados em [](jsc:sigma_reading_an_error).

> [!PROBLEM] Process.Output ou Process.ExitCode interrompe o programa: não há processo aqui.
> O processo terminou, então ele não está mais na máquina. Leia a saída dele enquanto o processo ainda está em execução.

> [!PROBLEM] Process.Send devolve false.
> O programa receptor não tem espaço na fila. Envie de novo mais tarde.

> [!PROBLEM] O programa para com um erro, e os arquivos dele são fechados.
> Leia o erro no console. O trabalho do programa para com esse erro.

> [!SEE] jsc:running_programs, jsc:threads_and_locks, jsc:sigma_runtime_stops
