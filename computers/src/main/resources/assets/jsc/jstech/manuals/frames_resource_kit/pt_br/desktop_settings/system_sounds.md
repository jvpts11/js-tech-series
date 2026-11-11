---
section: desktop_settings
order: 216
icon: jsc:monitor
---
# Sons do sistema

O Frames toca sons curtos para os eventos da máquina: inicialização, desligamento, um erro, um aviso, um bipe, e um dispositivo que você conecta ou retira. Cada edição tem os seus próprios toques, então a inicialização do Frames 95 soa diferente da do Frames 11. Cada som tem uma legenda: o texto que dá o nome dele na tela, como "Frames 95 toca".

Table: O toque de inicialização e o som de erro

| Edição | Inicialização e desligamento | Erro |
| --- | --- | --- |
| Frames 95 | Toques do Frames 95 | Erro do Frames 95 |
| Frames XP | Toques do Frames XP | Erro do Frames XP |
| Frames 7 | Toques do Frames 7 | Erro do Frames XP, emprestado |
| Frames 10 | Toques do Frames 10 | Erro do Frames 11, emprestado |
| Frames 11 | Toques do Frames 11 | Erro do Frames 11 |

O Frames 7 não tem um som de erro próprio, então usa o do Frames XP. O Frames 10 usa o do Frames 11. Um desligamento toca de novo o toque de inicialização da mesma edição.

## Sons que as edições compartilham

Table: Sons compartilhados pelas edições

| Som | Quando você ouve |
| --- | --- |
| Aviso | "Frames toca um aviso", quando um balão aparece, a menos que o Horário silencioso esteja ligado. |
| Bipe | "Frames apita", quando você clica ao lado de uma caixa de diálogo. |
| Dispositivo conectado | Quando você conecta um dispositivo. O Frames 95 não tem esse som. |
| Dispositivo desconectado | Quando você retira um dispositivo. O Frames 95 não tem esse som. |

## Para onde vai o som

A máquina toca o som pelo monitor, pelos alto-falantes ou por ambos. A opção Saída da página Som de [Configurações](jsc:frames_settings) escolhe: Monitor, Alto-falantes ou Ambos.

A mesma página Som tem o Volume, que muda em passos de 10 por cento, de 0 a 100 por cento, e a chave Mudo, que mostra Ligado ou Desligado. No Frames 10, o Horário silencioso também interrompe o som de aviso, como explica a entrada de [avisos](jsc:notices).

## Se algo der errado

> [!PROBLEM] Você não ouve nenhum som.
> Abra a página Som das Configurações. Mudo deve estar Desligado, e a Saída deve chegar a um alto-falante ou ao monitor.

> [!PROBLEM] A página Som diz "Nenhum alto-falante ligado".
> Ligue os alto-falantes à saída de som do computador.

> [!PROBLEM] A página Som diz "Sem hardware de som".
> O computador não tem placa de som nem som da placa-mãe. Veja [Placas de som](jsc:sound_cards).

> [!PROBLEM] O botão Testar está cinza.
> O computador não consegue tocar um som no momento, então Testar está desativado.

> [!DEFINE] toque
> Uma melodia curta que a máquina toca na inicialização e no desligamento.

> [!DEFINE] legenda
> A legenda que mostra o nome de um som, como o nome do seu toque.

> [!SEE] jsc:frames_settings, jsc:speakers, jsc:sound_cards
