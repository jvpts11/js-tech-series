---
section: desktop_apps
order: 81
icon: jsc:monitor
---
# O Monitor do Sistema e o Gerenciador de Tarefas

O Monitor do Sistema e o Gerenciador de Tarefas mostram o que a máquina está fazendo. O primeiro apenas lê: mostra a carga do processador, a memória, os gráficos e o armazenamento. O segundo lista o que está em execução e permite finalizar um programa.

## O Monitor do Sistema

O Monitor do Sistema existe nos sistemas de área de trabalho. Ele lê a máquina enquanto ela trabalha, e muda conforme a carga muda.

Table: Partes do Monitor do Sistema

| Parte | O que mostra |
| --- | --- |
| Processador | A carga do processador. |
| Memória | Os megabytes em uso e o total, depois os megabytes livres, e o swap com o que dele está em uso. |
| Gráficos | A memória de vídeo em uso e a carga da placa. Sem placa, mostra "sem GPU". |
| Armazenamento | Quantos programas estão instalados, e cada disco, com o disco do sistema marcado (system). |
| Energia e Temperatura | O que as peças consomem e a linha fornece, e o calor do processador, cada um sobre um pequeno gráfico do período que o seu menu escolhe. |

## O Gerenciador de Tarefas

Clique com o botão direito no painel e abra o Gerenciador de Tarefas pelo menu dele. Ele não está no Menu Iniciar nem na área de trabalho. Cada área de trabalho tem o seu próprio gerenciador, então o que você vê depende do seu sistema.

Table: O Gerenciador de Tarefas por área de trabalho

| Área de trabalho | Seu gerenciador |
| --- | --- |
| Frames 95 | Uma caixa Fechar Programa. |
| Frames XP | Cinco abas, com um menu e uma barra de status. |
| Frames 11 | Uma barra lateral e uma tabela larga. |
| Áreas de trabalho Linux | O monitor de sistema próprio de cada área de trabalho. |

No Frames XP as abas são Aplicativos, Processos, Desempenho, Rede e Energia. A aba Desempenho mostra o uso do processador e da memória, em números e como histórico, e a placa de vídeo quando há uma. A aba Rede mostra se a máquina está Conectada ou Não conectada, e a atividade do link no último minuto. A aba Energia mostra o consumo, a linha e a temperatura com os seus históricos ([](jsc:task_manager)).

A guia Desempenho do Frames XP mostra também o Uso do Arquivo de Paginação, o que os programas guardam no arquivo de paginação, e a barra de status a Carga: o que está prometido, contra a memória e o arquivo de paginação juntos. No Frames 10 e 11 a página Memória mostra o mesmo, e cada programa com parte de si no arquivo de paginação, e a que velocidade essa parte roda ([](jsc:swap)).

Table: Botões do Gerenciador de Tarefas

| Botão | O que faz |
| --- | --- |
| `End Task` | Encerra a janela ou o programa selecionado. |
| `End Process` | Finaliza o programa selecionado. |
| `Switch To` | Traz a janela selecionada para frente. |

Ao lado dos botões, a janela avisa: "Finalizar um programa pode causar a perda de trabalho não salvo." e "Finalizar o sistema reinicia a máquina." Um sistema, um desktop ou um serviço não pode ser finalizado daqui.

1. Clique com o botão direito no painel e abra o Gerenciador de Tarefas.
2. Selecione a janela ou o programa na lista.
3. Se o programa ainda permitir, salve primeiro o seu trabalho nele.
4. Pressione Finalizar Tarefa para uma janela, ou Finalizar Processo para um programa.

## Se algo der errado

> [!PROBLEM] Finalizar Tarefa continua em cinza.
> Selecione primeiro uma janela ou a linha de um programa.

> [!PROBLEM] A linha que você quer fechar é o sistema, uma área de trabalho ou um serviço.
> A própria máquina não pode ser finalizada daqui. Feche o programa dono daquela linha.

> [!SEE] jsc:programs, jsc:help_on_a_computer, jsc:swap
