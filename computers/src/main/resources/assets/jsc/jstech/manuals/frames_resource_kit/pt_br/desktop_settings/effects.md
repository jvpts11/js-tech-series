---
section: desktop_settings
order: 215
icon: jsc:monitor
---
# Efeitos visuais e o visual básico

Efeitos são as animações e os extras visuais de uma área de trabalho do Frames: o vidro do Frames 7, os esmaecimentos, as sombras e o movimento quando uma janela abre. Você pode ligá-los ou desligá-los. Cada edição tem sua própria página de Efeitos, acessada por outra página das Configurações.

Table: Onde fica a página Efeitos

| Edição | Acessada por | Título |
| --- | --- | --- |
| Frames 95 | Vídeo | Efeitos |
| Frames XP | Sistema | Opções de desempenho |
| Frames 7 | Sistema | Opções de desempenho |
| Frames 10 | Personalizar | Personalizar > Efeitos |
| Frames 11 | Personalizar | Personalizar > Efeitos visuais |

## O que cada página de Efeitos oferece

Table: As opções de cada página de Efeitos

| Edição | As opções |
| --- | --- |
| Frames 95 | Uma caixa: "Animar janelas ao minimizar e restaurar". O rodapé tem OK, Cancelar e Aplicar. |
| Frames XP | Quatro predefinições, de "Deixar o Frames escolher a melhor opção para o computador" até "Personalizado:". Quatro caixas: animar janelas, menus com fade ou deslizamento, ToolTips com fade ou deslizamento e sombras para os rótulos dos ícones; o rodapé tem OK, Cancelar e Aplicar. |
| Frames 7 | As mesmas predefinições, mais "Habilitar vidro transparente", e caixas para as animações, os menus, as dicas de ferramenta e as sombras dos ícones. Em Facilidade de Acesso, uma opção: "Desativar todas as animações desnecessárias (quando possível)". |
| Frames 10 | Duas opções: "Efeitos de transparência" e "Reproduzir animações no Frames". Não há botões: a mudança vale na hora. |
| Frames 11 | Uma opção: "Efeitos de animação". Não há botões: a mudança vale na hora. |

No Frames 10, com Reproduzir animações no Frames desligado, as janelas abrem, fecham e minimizam de uma vez, e os blocos dinâmicos param de girar.

## O visual básico do Frames 7

O Frames 7 desenha o vidro só quando os gráficos conseguem executá-lo. Seu computador verifica os gráficos primeiro. Se forem fracos demais, o Frames 7 volta ao Visual básico: os mesmos quadros e botões, mas opacos, sem vidro. A verificação vale só para um computador montado com peças.

A verificação usa a pontuação de gráficos do computador. A pontuação vem da melhor placa de vídeo, ou dos gráficos integrados quando não há placa. A memória de vídeo (VRAM), a memória da placa de vídeo, define a pontuação máxima que uma placa pode alcançar:

Table: A maior pontuação pela memória de vídeo

| Memória de vídeo da placa | Pontuação máxima |
| --- | --- |
| Abaixo de 128 MB | 2,9 |
| 128 MB a 255 MB | 4,9 |
| 256 MB a 511 MB | 5,9 |
| 512 MB a 1.023 MB | 6,9 |
| 1.024 MB ou mais | 7,9 |

O Visual básico vale quando a pontuação está abaixo de 3,0. Uma placa com menos de 128 MB de memória de vídeo, portanto, sempre recebe o Visual básico no Frames 7, e o mesmo acontece com um computador com gráficos integrados e sem placa.

O Frames 10 e o Frames 11 também verificam os gráficos. Eles mantêm o visual, e só os efeitos são desligados.

Quando o Visual básico está ligado, todos os efeitos ficam desligados. A página de Efeitos então mostra esta nota: "Gráficos fracos demais para estes efeitos: estão todos desligados. Uma placa mais forte traz de volta o que se escolhe aqui." Suas escolhas continuam salvas.

Quando o Frames 7 volta ao Visual básico, aparece uma vez um balão intitulado "Frames 7 Basic".

## O índice de desempenho

A página Sistema do Frames 7 mostra um índice de desempenho. Ele tem cinco pontuações: o processador, a memória, os gráficos, os jogos e o disco. A menor das cinco é a pontuação base.

## Se algo der errado

> [!PROBLEM] O vidro é opaco, e os efeitos somem no Frames 7.
> Os gráficos são fracos demais para o vidro, então o Frames 7 usa o Visual básico. Instale uma placa de vídeo mais forte, com mais memória de vídeo, e confira a página Sistema.

> [!PROBLEM] As animações param no Frames 10.
> A opção Reproduzir animações no Frames está desligada. Ligue-a na página Efeitos.

> [!PROBLEM] As animações param no Frames 7.
> A opção Desligar todas as animações desnecessárias, da Facilidade de Acesso, está ligada. Desligue-a.

> [!DEFINE] Visual básico
> A aparência opaca para a qual o Frames 7 volta, quando os gráficos não conseguem exibir o vidro.

> [!SEE] jsc:personalize, jsc:graphics_cards, jsc:frames_settings
