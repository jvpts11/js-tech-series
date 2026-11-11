---
section: hardware
order: 27
icon: jsc:sound_card_tone_blaster
covers_family: jsc:sound_cards
---
# Placas de som

O som vem da placa-mãe ou de uma placa de som. Uma placa-mãe Vintage ou Legacy não tem som próprio, então precisa de uma placa de som para qualquer coisa além de um bipe.

> [!DEFINE] Alto-falante do PC
> O pequeno emissor de bipes do computador. Em uma placa-mãe Vintage ou Legacy sem placa de som, ele é o único som.

A família Artisan Tone Blaster fornece as placas de som. Cada uma serve a uma era e a um barramento.

Table: As placas de som

| Placa de som | Era | Barramento |
| --- | --- | --- |
| Artisan Tone Blaster | Vintage | ISA |
| Artisan Tone Blaster 128 | Vintage | PCI |
| Artisan Tone Blaster Live | Legacy | AGP 8x |
| Artisan Tone Blaster Audigy | Legacy | PCI-e 1.0 |
| Artisan Tone Blaster Hi-Fi | Transition | PCI-e 1.0 |

Um computador aceita uma placa de som, da era da sua placa-mãe, e o barramento da placa precisa pertencer à [família de barramento](jsc:build_refusals) da placa-mãe.

A partir da Transition, a placa-mãe tem som próprio, então a placa de som não é mais necessária. A dica dela diz "Som na placa: toca tudo". As placas anteriores dizem "PC speaker: só bipes".

O som que você ouve sai pelos monitores vinculados ao computador, e pelos alto-falantes ligados a ele por [cabo periférico](jsc:speakers). Sem monitor e sem alto-falante vinculados, o som vai para o alto-falante do PC. Caso contrário, a placa de som toca, se houver uma, depois o som próprio da placa-mãe, se ela tiver, e, se não, o alto-falante do PC.

## Se algo der errado

> [!PROBLEM] A compilação diz que uma placa de som de uma era não encaixa em uma placa-mãe de outra.
> Use uma placa de som da era da placa-mãe.

> [!PROBLEM] A compilação diz "uma placa de som por máquina".
> Retire uma das duas placas de som.

> [!PROBLEM] Um computador Vintage ou Legacy só emite bipes.
> Ela não tem placa de som, e nenhum monitor ou alto-falante está ligado para levar o som. Coloque uma placa de som da era da placa-mãe, ou ligue um monitor ou alto-falantes.

> [!SEE] jsc:speakers, jsc:monitors, jsc:motherboards
