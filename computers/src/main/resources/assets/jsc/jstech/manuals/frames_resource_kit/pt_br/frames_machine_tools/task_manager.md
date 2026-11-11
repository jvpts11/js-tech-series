---
section: frames_machine_tools
order: 223
---
# O Gerenciador de Tarefas

O Gerenciador de Tarefas mostra o que a máquina está executando e quanto cada coisa usa: as janelas abertas, os processos, o processador (CPU), a memória e os discos. Você o usa para finalizar um programa que não fecha.

Abra-o pela barra de tarefas ([](jsc:taskbar)): clique com o botão direito na barra de tarefas e escolha Gerenciador de Tarefas. Ele não fica na área de trabalho nem no Menu Iniciar.

## Sua aparência em cada edição

Table: O Gerenciador de Tarefas em cada edição

| Edição | Aparência | Páginas |
| --- | --- | --- |
| Frames 95 | Uma caixa Fechar Programa, com dois avisos | Nenhuma: a caixa não tem páginas |
| Frames XP e Frames 7 | Uma janela com barra de menus e barra de status | Aplicativos, Processos, Desempenho, Rede e Energia |
| Frames 10 e Frames 11 | Uma janela com gráficos | Processos, Desempenho, Serviços, Armazenamento, Rede, o processador gráfico (GPU) e Energia |

## Finalizando um programa ou um processo

1. Clique em uma linha que seja uma janela ou um processo.
2. Pressione Finalizar Tarefa. Na lista de processos, o botão diz Finalizar Processo.

Finalizar Tarefa fica indisponível quando a linha selecionada não é uma janela nem um processo. Finalizar uma janela fecha esse programa. Finalizar um processo interrompe o script ou o serviço que ele indica.

## A lista de processos

Table: Colunas da lista de processos

| Coluna | O que mostra |
| --- | --- |
| Nome da Imagem ou Nome do Processo | O nome do programa ou do processo. |
| Uso de Memória ou Memória | Quanta memória a linha usa. |
| CPU | Quanto do processador a linha usa. |
| Tipo | Que tipo de linha ela é: sistema, área de trabalho, serviço, programa, script ou gráficos. |

## Desempenho e rede

A página Desempenho mostra Uso da CPU e Uso de Memória em números e em históricos. A parte de gráficos mostra a memória de vídeo, com suas partes dedicada e compartilhada. Um computador sem gráficos diz "Este computador não tem gráficos."

A página Rede diz se a máquina está Conectada ou Não conectada, e mostra a atividade do link no último minuto.

> [!DEFINE] Processo
> Um script ou serviço em execução na máquina.

O Gerenciador de Tarefas não mostra as Operações de uma rede. Você as acompanha no Gerenciador de Rede ([](jsc:network_manager)). Os oito estados em que uma Operação pode estar ficam em uma página própria ([](jsc:operation_states)).

A página Energia mostra o que as peças da máquina consomem diante da potência nominal da sua fonte, o que ela tira da linha em watts e em FE por tick, e a temperatura do seu processador, cada um com o seu histórico na última hora, no último dia ou nos últimos 7 dias do jogo, e a energia que ela tirou da linha hoje. No Frames XP e no Frames 7, clique na barra de status para passar ao período seguinte; no Frames 10 e no Frames 11, escolha-o com os botões da página.

## Se algo der errado

> [!PROBLEM] Finalizar Tarefa está indisponível.
> Clique em uma linha que seja uma janela ou um processo antes de pressioná-la.

> [!PROBLEM] A caixa Fechar Programa diz "Finalizar um programa pode causar a perda de trabalho não salvo."
> Salve primeiro o seu trabalho nesse programa e depois finalize-o.

> [!PROBLEM] A caixa Fechar Programa diz "Finalizar o sistema reinicia a máquina."
> Escolha Cancelar, a menos que você queira que a máquina reinicie.

> [!PROBLEM] Você procura aqui uma tarefa de rede e não a encontra.
> Abra o Gerenciador de Rede ([](jsc:network_manager)).

> [!SEE] jsc:taskbar, jsc:network_manager
