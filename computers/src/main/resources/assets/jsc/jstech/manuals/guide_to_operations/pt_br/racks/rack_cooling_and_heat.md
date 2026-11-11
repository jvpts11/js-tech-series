---
section: racks
order: 59
---
# Refrigeração e calor

Um rack tem uma temperatura para o gabinete inteiro, compartilhada por todas as máquinas nele. Quando você enche um rack ele esquenta, e a partir de certo ponto as máquinas desaceleram. Cada Unidade de Refrigeração e cada Painel de Ventilação no rack contam, onde quer que estejam.

> [!DEFINE] Orçamento de calor
> Os watts de calor que um rack tira, com os quais ele fica a 80 °C.

O orçamento de calor é 1.000 W, mais o que cada Unidade de Refrigeração acrescenta, conforme a sua era, mais 500 W por Painel de Ventilação. A carga é o consumo de energia de todas as máquinas cuja baia está ligada: seus processadores e placas, somados.

```table
source: jsc:cooling_units
caption: Unidades de Refrigeração
heads: Unidade | Era | Acrescenta ao orçamento de calor
```

A temperatura do rack segue as mesmas regras que a de um computador ([](jsc:cooling)): o ambiente mais 60 °C vezes a carga dividida pelo orçamento de calor. Até 80 °C todas as máquinas rodam em velocidade total. A partir de 90 °C todas as máquinas desaceleram na mesma proporção, só o bastante para manter o rack em 90 °C, e nunca abaixo de 25%. A 100 °C, quando nem 25% segura o calor, todas as máquinas do rack desligam na hora, e nenhuma baia liga até o rack esfriar abaixo de 60 °C.

Table: Algumas cargas, nas planícies

| Carga | Orçamento de calor | Temperatura | Proporção da velocidade |
| --- | --- | --- | --- |
| 1.000 W | 1.000 W | 80 °C | 100% |
| 1.500 W | 1.000 W | 90 °C | 66% |
| 2.000 W | 1.000 W | 90 °C | 50% |
| 4.000 W | 1.000 W | 91 °C | 25%, a menor proporção |
| 5.000 W | 1.000 W | 108 °C | Desligamento térmico: todas as máquinas desligam |
| 5.000 W | 2.500 W, com uma Unidade de Refrigeração Standard | 90 °C | 50% |

A tela do rack mostra a temperatura, e THROTTLED com a proporção enquanto as máquinas desaceleram. A redução corta a capacidade de cada servidor, inclusive o bônus de suas placas de vídeo, e gasta as fontes dos servidores duas vezes mais rápido.

## Se algo der errado

> [!PROBLEM] O rack mostra THROTTLED.
> Desligue as baias das máquinas de que você não precisa, ou adicione uma Unidade de Refrigeração ou um Painel de Ventilação ([](jsc:rack_equipment)).

> [!PROBLEM] Todos os servidores do rack apagaram ao mesmo tempo.
> O rack teve um desligamento térmico a 100 °C. Espere até ele esfriar abaixo de 60 °C, e acrescente refrigeração ou tire carga antes de ligar as baias de novo.

> [!SEE] jsc:rack_equipment, jsc:server_racks, jsc:cooling
