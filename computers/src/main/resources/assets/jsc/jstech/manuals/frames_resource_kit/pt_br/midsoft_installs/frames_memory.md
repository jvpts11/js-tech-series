---
section: midsoft_installs
order: 202
icon: jsc:monitor
---
# Memória e as edições do Frames

O Setup não recusa uma máquina por ter pouca memória. A página de verificação mostra a sua linha de Memória, e uma máquina com pouca memória ainda pode receber uma instalação do Frames. Ela então roda apertada e inicializa devagar, como explica a seção sobre RAM apertada abaixo.

O sistema reserva a própria RAM primeiro, antes de qualquer outra coisa: 32 MB para o Frames 95, 128 MB para o XP, 1 GB para o 7, 3 GB para o 10 e 8 GB para o Frames 11 ([](jsc:frames_which_machine)). Cada janela que você abre também ocupa memória: a RAM do próprio programa.

O que um programa ocupa cresce com a era da máquina em que roda, porque o software cresceu junto com as máquinas para as quais foi escrito. O número que um programa informa é o que ele ocupa numa máquina Legacy. Ele ocupa um dezesseis avos disso numa máquina Vintage, 8 vezes mais numa máquina Transition, 32 vezes numa máquina Standard e 128 vezes numa máquina Advanced. Um programa que vem com o sistema ocupa 16 MB numa máquina Legacy. Uma máquina típica de cada era, com dois pentes de memória do tamanho do meio, comporta o sistema e cerca de oito programas.

A memória livre é o total da máquina menos tudo o que está ocupado. Quando um programa precisa de mais do que está livre, toda edição do Frames trava: ela não recusa o programa. Isso vale para uma janela, para um programa iniciado no prompt e para um serviço. As edições mais novas avisam antes, como mostra a tabela.

Table: O que acontece quando a memória enche

| Edição | Kernel | O que acontece |
| --- | --- | --- |
| Frames 95 | Cooperativo | O sistema trava, sem aviso. |
| Frames XP, 7, 10 e 11 | Preemptivo | Um balão avisa quando a memória passa de nove décimos. Depois o sistema trava no programa que não cabe. |

## O aviso

No Frames XP, 7, 10 e 11, a janela que leva a memória além de nove décimos abre com um balão que diz "Low on memory". O texto pede que você feche alguns programas e cita os três que mais ocupam, com o quanto cada um ocupa.

## O crash

Cada edição cai na sua própria página. O Frames 95 mostra "A fatal exception has occurred.". O XP e o 7 mostram um erro de parada chamado MEMORY_MANAGEMENT. O Frames 10 e o 11 mostram uma carinha triste, um código que pode ser escaneado e o código de parada MEMORY_MANAGEMENT.

A página aparece em todo monitor que mostra o computador, e teclas e cliques não fazem nada enquanto ela está na tela. Os programas que o computador rodava param. A página fica 4,2 segundos no Frames 95, 6 segundos no XP, 8 segundos no 7 e 10 segundos no 10 e no 11. Depois o computador recomeça pelo autoteste, numa sessão vazia.

## A página vermelha

Uma peça retirada de um computador em funcionamento também derruba o sistema: um pente de memória, uma placa de expansão ou o disco de onde o sistema roda. O Frames mostra a mesma página em vermelho, e o Frames 10 e o 11 dizem o nome da peça. Se o computador ainda tem o que precisa para funcionar, ele recomeça depois da página. Se não tem, o monitor apaga. Retirar o processador, a placa-mãe ou a fonte desliga o computador na hora, sem página.

Programas gráficos também precisam de memória de vídeo. Quando uma janela precisa de mais do que a memória livre, o balão diz "Memória de vídeo insuficiente" e pede que você desligue um monitor ou feche um programa gráfico.

## RAM apertada e o tempo de inicialização

Uma máquina está apertada para um sistema quando tem menos do que o dobro da RAM própria dele. Uma inicialização apertada leva uma vez e meia mais tempo. Você pode ver o limite de cada edição na tabela.

```table
{"caption": "Memória abaixo da qual cada edição fica apertada", "heads": ["Edição", "Apertada abaixo de"], "rows": [[{"words": "Frames 95"}, {"amount": 64, "unit": "MB"}], [{"words": "Frames XP"}, {"amount": 256, "unit": "MB"}], [{"words": "Frames 7"}, {"amount": 2048, "unit": "MB"}], [{"words": "Frames 10"}, {"amount": 6144, "unit": "MB"}], [{"words": "Frames 11"}, {"amount": 16384, "unit": "MB"}]]}
```

Uma inicialização leva cerca de um segundo, mais a Ocupação do sistema em megabytes dividida por (160 vezes a Velocidade do disco vezes o fator de era da máquina). A Velocidade do disco é 1 para um disco rígido, 4 para uma unidade de estado sólido (SSD) e 16 para uma unidade NVMe. O fator de era é 2 para Legacy, 3 para Transition, 4 para Standard e 8 para Advanced. O resultado é de pelo menos 2 segundos e no máximo 20 segundos.

Com RAM suficiente, em um disco rígido, o Frames XP em uma máquina Legacy inicializa em cerca de 6 segundos, e o Frames 11 em uma máquina Advanced em cerca de 17 segundos. O Frames 7 em uma máquina Transition e o Frames 10 em uma máquina Standard chegam ao limite de 20 segundos.

> [!DEFINE] Kernel
> A parte do sistema que roda os programas e alterna entre eles.

> [!DEFINE] Kernel cooperativo
> Um kernel em que cada programa precisa devolver o processador por conta própria, porque nada o obriga a isso.

> [!DEFINE] Kernel preemptivo
> Um kernel que alterna os programas à força, para que um programa não consiga segurar o processador.

> [!DEFINE] Balão
> O pequeno aviso que aparece na área de notificação da área de trabalho.

> [!DEFINE] RAM apertada
> O estado de uma máquina que tem menos que o dobro da RAM própria do seu sistema.

## Se algo der errado

> [!PROBLEM] O computador reinicia depois de uma página azul de crash.
> Um programa precisou de mais memória do que havia livre. Feche um programa antes de abrir outro, ou adicione RAM ([](jsc:memory)).

> [!PROBLEM] Um balão diz "Pouca memória".
> A memória passou de nove décimos. Feche um programa, ou adicione RAM ao computador ([](jsc:memory)).

> [!PROBLEM] A página de crash é vermelha.
> Uma peça foi retirada com o computador funcionando. Coloque-a de volta e ligue o computador outra vez se o monitor apagou.

> [!PROBLEM] Um balão diz "Memória de vídeo insuficiente".
> Desligue um monitor, ou feche um programa gráfico.

> [!PROBLEM] O computador demora muito para iniciar.
> A máquina está com pouca RAM. Adicione RAM até que ela tenha pelo menos o dobro da RAM própria do sistema, usando a tabela acima.

> [!SEE] jsc:memory, jsc:frames_which_machine
