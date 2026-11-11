---
section: editions
order: 232
---
# Frames 11

O Frames 11 é a edição mais recente, a quinta. Ele exige um computador Advanced, a era mais nova com peças, e precisa do maior espaço da família: 20.480 MB, o que equivale a 80 itens em um disco Advanced.

Ele se instala a partir de um pendrive USB na [Dock Station](jsc:dock_station) e reserva 8.192 MB de RAM para si.

```table
{"caption": "O Frames 11 em resumo", "rows": [[{"words": "Era mínima"}, {"words": "Advanced"}], [{"words": "Mídia de instalação"}, {"words": "Pen drive USB, na Estação de Dock"}], [{"words": "Ocupação"}, {"amount": 20480, "unit": "MB"}], [{"words": "RAM em uso"}, {"amount": 8192, "unit": "MB"}], [{"words": "Linha de versão"}, {"words": "Frames NT 10.0"}]]}
```

## Instalando o Frames 11

1. Coloque o pen drive do Frames 11 na Estação de Dock e inicialize o computador por ele. Se o computador não inicializar por ele, abra o Menu de Inicialização ([](jsc:boot_menu)).
2. Na página "Onde você quer instalar o Frames 11?", escolha um disco.
3. Na página "Nomeie este PC", dê um nome ao computador.
4. Aguarde enquanto a cópia mostra, em sequência, "Copiando arquivos", "Criando as pastas do sistema", "Adicionando os programas embutidos" e "Configurando a entrada de boot", sob a legenda "Instalando o Frames 11".
5. Aguarde até o instalador terminar.

As páginas do instalador são cartões claros, com uma pergunta por página.

O visual dele é Modern: uma barra de tarefas escura, centralizada na parte de baixo da tela, e um Menu Iniciar que flutua acima dela, com uma caixa de pesquisa e uma grade de programas fixados.

O programa de arquivos se chama Explorador de Arquivos, como no Frames 10.

O Gerenciador de Dispositivos dele tem a forma plana e moderna. Os efeitos visuais ficam em Personalizar > Efeitos visuais, com uma opção, "Efeitos de animação". A nota dela diz "Janelas abrem, fecham e minimizam com movimento, os painéis deslizam".

Em um computador novo, o Frames 11 diz "Olá.", como o Frames 10 faz. Ele toca "Frames 11 toca" ao iniciar e "Erro do Frames 11" quando algo dá errado. A tela de abertura é quase preta, com a marca do fabricante no meio e um anel de pontos girando embaixo.

## O Megashell

O Frames 11 tem um shell próprio, com o nome Megashell. O Frames 95, XP, 7 e 10 o chamam de Prompt de Comando, e no Frames 11 a sua janela e o seu nome na área de trabalho são Megashell.

> [!DEFINE] Megashell
> O shell do Frames 11: o programa que recebe os seus comandos, com o nome próprio dessa edição.

Para os comandos que você digita, veja [O Prompt de Comando](jsc:command_prompt). A ajuda desta edição abre a janela Obter Ajuda ([](jsc:help_on_a_computer)).

## Encaixe nas bordas

O encaixe nas bordas encaixa uma janela contra uma borda da tela. Ele funciona no Frames 11 e nas áreas de trabalho KDE, GNOME e Cinnamon dos sistemas Linux ([](jsc:unix_desktops)). Frames 95, XP, 7 e 10 não encaixam.

> [!DEFINE] Encaixe nas bordas
> Encaixar uma janela contra uma borda da tela quando você a solta ali.

> [!DEFINE] Área útil
> A parte da tela que a barra de tarefas não cobre.

A posição em que você solta decide o que a janela faz. Uma janela a menos de 4 pixels de uma borda muda de tamanho e de lugar:

Table: Bordas e o que fazem

| Onde você soltou | O que a janela faz |
| --- | --- |
| A menos de 4 pixels do topo da área útil | A janela maximiza e ocupa a área útil. |
| A menos de 4 pixels da borda esquerda da tela | A janela ocupa a metade esquerda da área útil. |
| A menos de 4 pixels da borda direita da tela | A janela ocupa a metade direita da área útil. |
| Em qualquer outro lugar | A janela fica onde você a soltou. |

O encaixe acontece quando você solta, não enquanto arrasta. Mantenha a janela na borda e, depois, solte o botão do mouse.

Uma janela encaixada mantém o lugar antigo. Quando você a restaura de maximizada, ela volta para onde estava antes do encaixe.

## Se algo der errado

> [!PROBLEM] Uma janela solta no meio da tela não encaixa.
> Só as bordas da tabela encaixam uma janela. Arraste a janela até uma delas e solte.

> [!PROBLEM] Uma janela não encaixa no Frames 10.
> O Frames 10 não tem encaixe nas bordas. Mova e redimensione a janela na mão.

## A página da GPU

A página da GPU fica no Gerenciador de Tarefas e mostra a unidade de processamento gráfico (GPU) do computador: a placa que desenha a imagem dos monitores. O Frames 10 tem a mesma página. Ela mostra o quanto a placa está ocupada e quais monitores e janelas usam a sua memória de vídeo.

> [!DEFINE] Memória de vídeo
> A memória de uma placa de vídeo, usada pelos monitores e pelas janelas gráficas.

Table: O que a página da GPU mostra

| Parte da página | O que ela mostra |
| --- | --- |
| Título e nome da placa | GPU, e o nome da placa à direita, em texto pequeno. |
| Gráfico 3D | O quanto a placa está ocupada, de 0 a 100. |
| Gráfico de memória | O quanto a memória está cheia: "Uso de memória dedicada da GPU" para uma placa, ou "Uso de memória compartilhada da GPU" para gráficos integrados. |
| Utilização | O quanto a placa está ocupada, em porcentagem. |
| Memória dedicada, ou Memória compartilhada | A memória em uso, do total. |
| Núcleos | O número de núcleos, e a velocidade do clock em MHz ou GHz. |
| Slot | O slot PCI Express (PCIe) em que a placa está, ou "No processador" para gráficos integrados. |
| Usada por | Uma linha para cada monitor ou janela gráfica que ocupa memória de vídeo, com o tamanho dele. |

A página mostra a primeira placa de vídeo na montagem do computador. Sem placa, ela mostra os gráficos do processador, se ele tiver algum, e a memória fica então compartilhada com o computador.

A porcentagem de utilização cresce com duas coisas. O preenchimento da memória soma até 50 por cento, e cada janela gráfica aberta soma 20 por cento. Ela é 0 quando nenhuma memória de vídeo está em uso, e nunca passa de 100 por cento.

Um monitor aceso guarda Memória de vídeo em blocos, e uma janela gráfica ocupa um quarto de um Bloco de monitor, ou um Bloco inteiro enquanto preenche a tela. O tamanho de um Bloco depende da era do computador.

```table
{"caption": "Memória de vídeo por era", "heads": ["Era", "Um bloco de monitor", "Janela", "Janela em tela cheia"], "rows": [[{"words": "Vintage"}, {"amount": 256, "unit": "KB"}, {"amount": 64, "unit": "KB"}, {"amount": 256, "unit": "KB"}], [{"words": "Legacy"}, {"amount": 4, "unit": "MB"}, {"amount": 1, "unit": "MB"}, {"amount": 4, "unit": "MB"}], [{"words": "Transition"}, {"amount": 16, "unit": "MB"}, {"amount": 4, "unit": "MB"}, {"amount": 16, "unit": "MB"}], [{"words": "Standard"}, {"amount": 64, "unit": "MB"}, {"amount": 16, "unit": "MB"}, {"amount": 64, "unit": "MB"}], [{"words": "Advanced"}, {"amount": 256, "unit": "MB"}, {"amount": 64, "unit": "MB"}, {"amount": 256, "unit": "MB"}]]}
```

Estes tamanhos são estimativas. A tabela dá o bloco de 16 cores; um monitor Vintage que mostra só preto e branco usa 64 KB por bloco. A página escreve uma quantidade em KB abaixo de 1 MB, em MB abaixo de 1 GB e em GB acima disso, com uma casa decimal.

## Se algo der errado

> [!PROBLEM] O monitor diz "O computador não tem saída de vídeo: instale uma placa de vídeo".
> Instale uma placa de vídeo no computador ([](jsc:graphics_cards)).

> [!PROBLEM] O monitor fica apagado e mostra "Memória de vídeo insuficiente". Ele acende quando a placa tem espaço.
> Desligue um monitor, ou use uma placa com mais memória ([](jsc:graphics_cards)).

> [!PROBLEM] Uma janela gráfica não abre, e a área de trabalho diz "Memória de vídeo insuficiente".
> Desligue um monitor ou feche um programa gráfico.

> [!PROBLEM] A página da GPU diz "Este computador não tem gráficos."
> O computador não tem placa de vídeo nem gráficos no processador. Instale uma placa de vídeo ([](jsc:graphics_cards)).

> [!SEE] jsc:graphics_cards, jsc:monitors, jsc:frames_10
