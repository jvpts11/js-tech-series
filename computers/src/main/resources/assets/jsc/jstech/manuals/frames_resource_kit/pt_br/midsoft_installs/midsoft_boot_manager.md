---
section: midsoft_installs
order: 201
icon: jsc:cd_rom
---
# O Gerenciador de Inicialização Midsoft

O Gerenciador de Inicialização Midsoft é a lista que uma máquina Frames mostra ao iniciar. Ela só aparece quando há uma escolha, ou seja, duas ou mais entradas. Com um sistema só, a máquina inicia esse sistema de imediato.

A lista dá a cada sistema o seu nome simples. Os outros sistemas também mostram, entre colchetes, o número do disco deles. O sistema que o computador está executando é o padrão: se você não apertar nada, a contagem termina e esse sistema inicia.

Table: As teclas da lista

| Tecla | O que faz |
| --- | --- |
| `Up` | Move o cursor para cima na lista. |
| `Down` | Move o cursor para baixo na lista. |
| `Enter` | Inicia o sistema sob o cursor. |
| `Esc` | Sai do monitor, e a lista mantém a posição para a próxima abertura. |

A contagem dura 5 segundos, o que equivale a 100 ticks ([](jsc:units_and_ticks)). A primeira tecla que você pressionar para a contagem, e a lista então espera por você. Qualquer tecla conta, não apenas as setas e o Enter.

> [!DEFINE] Tick
> Uma batida do jogo: 20 ticks fazem um segundo.

No Frames 10 e 11 a lista termina com "Alterar configurações de firmware", que abre o firmware ([](jsc:firmware)). No Frames 95, XP e 7 a lista não tem essa entrada. As teclas numéricas não escolhem nesta lista; quem escolhe são as setas, e a lista não oferece reinício.

## Se algo der errado

> [!PROBLEM] A tela diz "kickmgr está faltando", e o computador não vai iniciar esse sistema.
> O arquivo do gerenciador sumiu da pasta do sistema, Frames/kickmgr.sys. Instale o sistema de novo no mesmo disco para consertá-lo ([](jsc:two_systems)).

> [!PROBLEM] A contagem inicia o sistema que você não queria.
> Pressione uma tecla logo no início da contagem para pará-la, depois escolha com as setas e Enter.

> [!PROBLEM] A lista não aparece, apesar de dois sistemas estarem instalados.
> A configuração do menu de inicialização do servidor está desligada. Ligue-a, com a configuração boot.show_boot_menu.

> [!SEE] jsc:two_systems, jsc:firmware, jsc:units_and_ticks
