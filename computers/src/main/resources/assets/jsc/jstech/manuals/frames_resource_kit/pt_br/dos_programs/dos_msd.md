---
section: dos_programs
order: 207
---
# MSD e suas chaves

Midsoft Diagnostics (MSD) lê o hardware do computador e mostra do que ele é feito. Digite msd no prompt para abri-lo. O programa ocupa a tela inteira, e funciona no MC-DOS e no [MC-NET](jsc:mc_net).

1. Digite msd no prompt e pressione [[Enter]].
2. Mova a escolha com as teclas de seta. Pressione [[Enter]] em Portas LPT ou Portas COM para abrir uma lista.
3. Na lista, escolha uma porta com as setas, depois pressione [[E]] para ativar o dispositivo dela ou [[D]] para desativá-lo.
4. Pressione [[Enter]] ou [[Esc]] para fechar a lista. Pressione [[F3]] na tela principal para devolver o terminal.

Table: Os botões da tela principal

| Botão | O que mostra |
| --- | --- |
| Computador... | O nome do primeiro processador. |
| Memória... | A memória convencional, 640K, depois a memória acima de 1 MB, contada em K. |
| Vídeo... | O nome da primeira placa de vídeo, ou (none). |
| Rede... | O cabo do primeiro adaptador de rede, ou (none). |
| Versão do SO... | O nome do sistema. |
| Unidades de disco... | As letras de unidade que a máquina enxerga, como C: D:. |
| Portas LPT... | O número de portas paralelas. Zero quando não há nenhuma. Abre uma lista. |
| Portas COM... | O número de portas seriais. Abre uma lista. |

Somente Portas LPT e Portas COM abrem uma lista. Os outros botões mostram o que encontraram, e Enter não faz nada neles.

Table: As colunas de uma lista de portas

| Coluna | O que mostra |
| --- | --- |
| Porta | O nome da porta, como COM1 ou LPT1. |
| Conectado | O dispositivo na porta, ou (none) quando nada está conectado. |
| Estado | Ligado, ou Desligado (desativado) para um dispositivo que você desativou. |

Table: As chaves

| Tela | Chave | O que faz |
| --- | --- | --- |
| Tela principal | `Arrows` | Move a escolha entre os botões. |
| Tela principal | `Enter` | Abre o botão escolhido. |
| Tela principal | `L` | Abre as portas LPT. |
| Tela principal | `C` | Abre as portas COM. |
| Tela principal | `Esc or F3` | Sai do programa. |
| Lista de portas | `Up and Down` | Escolhe uma porta. |
| Lista de portas | `E` | Ativa o dispositivo da porta escolhida. |
| Lista de portas | `D` | Desativa o dispositivo da porta escolhida, que mantém a porta enquanto o computador não lê nem escreve nela. |
| Lista de portas | `Enter or Esc` | Fecha a lista. |

Um clique do mouse também funciona: em um botão, em uma porta, e nos botões Ativar e Desativar sob a lista.

> [!DEFINE] Midsoft Diagnostics (MSD)
> O programa que lê o hardware do computador e informa o que encontra.

> [!DEFINE] Porta LPT
> Uma porta paralela, onde normalmente se conecta uma impressora.

> [!DEFINE] Porta COM
> Uma porta serial, onde um modem ou um mouse da época podia ser conectado.

```command
id: jsc:command/mc_dos/msd
```

O DEFRAG ocupa a tela inteira do mesmo jeito, para consertar um disco corrompido ([](jsc:corruption_and_the_defrag)).

```command
id: jsc:command/mc_dos/defrag
```

## Se algo der errado

> [!PROBLEM] A tela diz msd: não há máquina para ler.
> O msd não tem computador para ler. Digite msd no prompt de um computador.

> [!PROBLEM] Uma porta mostra (none) em Conectado.
> Nada está conectado a essa porta, então não há dispositivo para ativar ou desativar.

> [!PROBLEM] Uma porta mostra Desligado (desativado) em Estado.
> Você desativou o dispositivo dela. Escolha a porta e pressione [[E]] para ativar o dispositivo de novo.

> [!SEE] jsc:printers, jsc:dos_help
