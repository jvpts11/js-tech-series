---
section: hardware
order: 25
icon: jsc:standard_ups
covers_family: jsc:upses
---
# Nobreaks

Um nobreak mantém seus computadores funcionando quando a energia cai. O MF PowerGuard é um bloco inteiro: você liga o cabo de energia na tomada no meio das costas ou de qualquer um dos lados, e cada computador encostado nele, em qualquer face, tira energia dele.

> [!DEFINE] Nobreak
> Fonte de energia ininterrupta (UPS): uma bateria entre a linha e o computador, que mantém o computador funcionando quando a linha cai.

Table: Os nobreaks

| Nobreak | Era | Bateria | Aguenta |
| --- | --- | --- | --- |
| MF PowerGuard 300 | Vintage, Legacy | 90 kJ | 300 W |
| MF PowerGuard 500 | Transition | 240 kJ | 500 W |
| MF PowerGuard 800 | Standard | 480 kJ | 800 W |
| MF PowerGuard 1500 | Advanced | 1,35 MJ | 1.500 W |

Enquanto a linha traz o que os computadores consomem, o nobreak os alimenta e carrega a bateria por cima: uma bateria vazia enche em dez minutos de jogo. Quando a linha cai, ou traz pouco, a bateria os mantém, a lâmpada âmbar acende, e cada sistema avisa do seu jeito: a bateria na área de notificação e um aviso na área de trabalho, duas linhas no MC-DOS, o broadcast do daemon do nobreak num console Unix.

A bateria dura os joules dela divididos pelo consumo: 480 kJ aguentam 800 W por dez minutos. A 10% da bateria os sistemas se desligam corretamente, então nada se perde e a próxima partida não verifica disco nenhum.

Acima do que aguenta, o nobreak entra em bypass, e a lâmpada verde pisca: os computadores rodam direto da linha, e a bateria não protege nada.

Ele também fica entre os computadores e uma linha da classe errada. Uma linha uma classe acima, ou um surto, não chega até eles: o nobreak não tira nada dela e os mantém na bateria. Duas classes acima, nem um nobreak aguenta.

Um rack tem o próprio nobreak, o Nobreak de Rack, que ocupa uma unidade de rack ([](jsc:rack_equipment)).

## Se algo der errado

> [!PROBLEM] Os computadores apagaram quando a energia caiu, com o nobreak do lado.
> Eles consumiam mais do que ele aguenta, então ele estava em bypass. Use um nobreak maior, ou menos computadores.

> [!PROBLEM] Os computadores se desligaram sozinhos enquanto a energia estava fora.
> A bateria chegou a 10%. Traga a linha de volta, e a bateria carrega de novo.

> [!PROBLEM] O cabo não encaixa no nobreak.
> Ele só encaixa nas costas e nos lados. Gire o nobreak, ou leve o cabo a um desses lados.

> [!SEE] jsc:power_supplies, jsc:rack_equipment
