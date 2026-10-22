---
section: energy
order: 7
icon: jscore:configurable_energy_converter
covers: [jscore:fe_converter_lv, jscore:fe_converter_mv, jscore:fe_converter_hv, jscore:fe_converter_ehv, jscore:fe_converter_uhv, jscore:configurable_energy_converter]
---
# Conversores FE

## O que é

Armários que transformam o FE do jogo em J's Energy, ou J's Energy em FE: um de cada classe de tensão, e o Conversor de Energia Configurável, cuja classe você escolhe.

## Para que serve

Ligar as linhas da série às máquinas e aos cabos de qualquer outro mod, nos dois sentidos.

Table: O máximo que cada um leva

| Propriedade | Valor |
| --- | --- |
| Conversor FE BT | 100 kW |
| Conversor FE MT | 10 MW |
| Conversor FE AT | 200 MW |
| Conversor FE EAT | 1,5 GW |
| Conversor FE UAT | 12 GW |
| Conversor de Energia Configurável | O da sua classe |

## Como conseguir

Fabrique o de baixa tensão com ferro, cobre, redstone e ouro, e cada classe acima com a de baixo e coisas mais caras. O configurável é feito a partir do de ultra-alta tensão.

## Como usar

1. De frente para ele, o FE entra ou sai pelo lado esquerdo e a J's Energy pelo direito.
2. Gire o botão para escolher o sentido: de FE para JE, ou de JE para FE.
3. Ajuste nos tambores o máximo que ele passa: clique na metade de cima de um tambor para subi-lo, na de baixo para descê-lo, e no último para quilowatts, megawatts ou gigawatts. Nunca passa do teto da classe.
4. No configurável, aperte uma tecla de classe para escolher a classe.
5. Um joule vale um FE, a não ser que o servidor diga outra coisa; o conversor arredonda para baixo, então nunca cria energia.

## O que pode dar errado

> [!PROBLEM] A lâmpada FALHA está acesa e nada passa.
> Uma linha de outra classe encontra o lado da J's Energy: ajuste a classe para a da linha, ou lance uma linha da classe do conversor.

> [!PROBLEM] A lâmpada LIMITE pisca.
> Chega mais do que o limite deixa passar; gire os tambores para cima, ou use um conversor de classe mais alta.
