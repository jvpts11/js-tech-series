---
section: desktop_environments
order: 262
icon: jsc:monitor
---
# Áreas de trabalho

Uma área de trabalho é o que um sistema mostra quando inicia com uma face gráfica: suas janelas, um painel ou barra de tarefas, e um menu que lista seus programas. Sem ela, o sistema para no seu terminal, um prompt onde você digita comandos. Cada edição do Frames traz a sua própria área de trabalho. Linux e FreeBSD não trazem, então você a instala como um pacote. No UNIX, a única área de trabalho é o Common Desktop Environment (CDE).

> [!DEFINE] Pacote
> Um software que o gerenciador de pacotes do sistema instala, atualiza e remove, pelo seu nome.

> [!DEFINE] Ambiente de área de trabalho
> A interface gráfica em que o sistema inicia: as suas janelas, o seu painel e o menu que lista os programas que ele mostra.

Quatro áreas de trabalho vêm embutidas. Cada uma tem o seu próprio visual e os seus próprios nomes para os programas. As eras que cada uma precisa são explicadas em [](jsc:eras).

```table
{"caption": "As áreas de trabalho embutidas", "heads": ["Área de trabalho", "Roda em", "A partir da era", "Disco para instalar", "Memória enquanto roda"], "rows": [[{"words": "KDE Plasma"}, {"words": "Linux, FreeBSD"}, {"words": "Legacy"}, {"amount": 256, "unit": "MB"}, {"amount": 224, "unit": "MB"}], [{"words": "GNOME"}, {"words": "Linux, FreeBSD"}, {"words": "Legacy"}, {"amount": 192, "unit": "MB"}, {"amount": 256, "unit": "MB"}], [{"words": "Cinnamon"}, {"words": "Linux, FreeBSD"}, {"words": "Standard"}, {"amount": 160, "unit": "MB"}, {"amount": 160, "unit": "MB"}], [{"words": "CDE"}, {"words": "UNIX, FreeBSD, Linux"}, {"words": "Legacy"}, {"amount": 32, "unit": "MB"}, {"amount": 16, "unit": "MB"}]]}
```

Os números de disco e memória são estimativas próprias do mod sobre o que cada área de trabalho ocupa. Um computador Vintage não tem área de trabalho gráfica nenhuma: ele vive no seu terminal.

Cada área de trabalho usa o visual da sua era. Em hardware Legacy, o KDE mostra KDE 2 e o GNOME mostra GNOME 1. Em Transition, eles mostram KDE 4 e GNOME 2. A partir do Standard, KDE Plasma e GNOME têm visual próprio. Cinnamon e CDE têm um visual cada, em todas as eras em que rodam.

Uma área de trabalho mostra como atalhos só os programas que ela inclui, e só aqueles que o seu computador pode rodar. KDE Plasma, GNOME e Cinnamon incluem os mesmos dez: Rede, Este Computador, Configurações, Arquivos, o Editor, o Prompt de Comando, o Monitor do Sistema, a Calculadora, o Gerenciador de Rede e Ajuda. O Gerenciador de Rede só aparece em um Mainframe ([](jsc:mainframes)).

O CDE inclui um conjunto próprio. Ele não tem Este Computador, então nenhum atalho do CDE o mostra. A resposta dele para a pergunta são as Informações da Estação de Trabalho ([](jsc:cde_workstation_info)).

Uma área de trabalho que você instala começa na próxima inicialização, não no momento em que você a instala. O computador verifica o que tem instalado durante o autoteste, então reinicie-o depois de uma instalação ([](jsc:firmware)).

## Se algo der errado

> [!PROBLEM] O sistema inicia em um shell, sem área de trabalho.
> Nenhum pacote de área de trabalho está instalado. Instale um, como [](jsc:install_a_desktop) mostra.

> [!PROBLEM] Você instalou uma área de trabalho, e a tela ainda mostra o shell.
> A área de trabalho começa na próxima inicialização. Reinicie o computador.

> [!PROBLEM] O computador é um Vintage, e a tela mostra só o terminal.
> Todas as áreas de trabalho desta página precisam de hardware Legacy ou posterior, e o Cinnamon precisa de Standard. Use um computador mais novo.

> [!SEE] jsc:install_a_desktop, jsc:desktop_program_names, jsc:the_systems, jsc:cde
