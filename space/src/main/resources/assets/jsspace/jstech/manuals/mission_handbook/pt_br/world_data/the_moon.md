---
section: world_data
order: 1
icon: jsspace:mare_regolith
covers: [jsspace:mare_basalt, jsspace:mare_basalt_stones, jsspace:anorthosite, jsspace:anorthosite_stones, jsspace:mare_regolith, jsspace:highland_regolith, jsspace:icy_regolith]
---
# A Lua

## O que é

A lua do Overworld: um mundo redondo de rocha e poeira, sem ar, sob um céu negro onde as estrelas aparecem até de dia. O chão segue o que a sua esfera mostra da órbita: mares escuros onde o mapa os tem, terras altas claras em todo o resto, Tycho no sul da face visível e, em volta de cada polo, crateras cujo fundo nunca vê o Sol. Nada vive lá.

![Vista da órbita](jsspace:gui/guide/moon "96x96")

> [!DEFINE] Regolito
> A poeira solta e a rocha partida que cobrem um mundo sem solo. Cai como a areia.

> [!DEFINE] Mar lunar
> Uma das planícies escuras da Lua, inundada de lava há muito tempo.

## Para que serve

Lá a tripulação encontra o chão próprio da Lua, que não existe em nenhum outro lugar:

- Regolito dos Mares, a poeira escura dos mares, e Regolito das Terras Altas, a poeira clara das terras altas.
- Basalto dos Mares, a rocha escura de lava sob o regolito dos mares, nua nas encostas íngremes.
- Anortosito, a rocha clara das terras altas, quase só plagioclásio. É a rocha de base da Lua, também por baixo do basalto dos mares.
- Regolito Gelado, regolito endurecido pelo próprio gelo, no fundo das crateras polares.
- Pedras de Basalto dos Mares e Pedras de Anortosito, pedrinhas soltas espalhadas pelos mares e pelas terras altas.

Os minérios ficam em veios nessas rochas: ilmenita, o titânio da Lua, com armalcolita e tranquilitita, no basalto e no regolito dos mares; merrillita, que traz terras-raras, no basalto dos mares; e o ferro nativo dos meteoritos no regolito das terras altas.

## Como conseguir

A tripulação chega à Lua de foguete. Faça a contagem regressiva no [painel de lançamento](jsspace:launch_panel) e o foguete sobe da plataforma até a órbita. Na página TRAN do Computador de Voo, escolha a Lua: não há janela de transferência para esperar, então a tripulação parte quando quiser. O cruzeiro costuma levar {{jsspace:moon_trip}}, e gastar mais que o mínimo o encurta. Sem ar para frear, o foguete queima os motores para parar na órbita da Lua.

Da órbita, marque um ponto na página POUS e o foguete [desce](jsspace:coming_down) em algum lugar da elipse de pouso em volta dele. Sem ar, não precisa de escudo térmico. Precisa, sim, de pernas, as Pernas de Pouso ou um Estágio de Descida do Módulo Lunar, e de propelente para {{jsspace:moon_landing_delta_v}} para frear até pousar nelas.

Toda equipe sabe onde fica a Lua desde o início. Um servidor também pode exigir que a equipe conheça o que mata lá antes de a tripulação pousar: a atmosfera, a temperatura, a radiação, a gravidade e o relevo.

Os blocos desta página são encontrados na Lua e minerados lá, e cada um, quebrado, dá o próprio bloco: os regolitos com pá, o Basalto dos Mares e o Anortosito com picareta, o Regolito Gelado com qualquer coisa e as pedras soltas com um toque. Eles também estão no menu criativo, aba J's Space.

Para partir, defina uma transferência no Computador de Voo: quando chegar a hora, o foguete acende o estágio mais baixo que ainda tem e sobe do chão, sem plataforma. O Estágio de Subida do Módulo Lunar deixa o estágio de descida para trás, como plataforma de lançamento.

## Como usar

### Números

Table: Os números da Lua

| Dado | Valor |
| --- | --- |
| Diâmetro | {{jsspace:moon_diameter}} |
| Gravidade | {{jsspace:moon_gravity}} |
| Ar | Nenhum ({{jsspace:moon_pressure}}) |
| Temperatura | {{jsspace:moon_temperature}} |
| Dia | {{jsspace:moon_day}}, metade dele de noite |

A gravidade é uma fração da força do Overworld. A hora segue a longitude, como num globo: quanto mais a leste você estiver, mais tarde é.

### Marcos

- Tycho, uma grande cratera, jovem e clara, no sul da face visível: uma bacia sob uma borda elevada, com um pico no meio e os seus raios espalhados pelo chão em volta.
- O Overworld, parado no céu sobre a face visível: alto perto do meio do mapa da Lua, mais baixo quanto mais longe se vai, no horizonte a um quarto da volta e escondido na face oculta.
- O céu da noite, mais nítido que o de qualquer mundo com ar: a Via Láctea, grossa e quente para os lados de Sagitário, partida pela escura Grande Fenda; Vênus, Marte, Júpiter e Saturno como pontos de luz ao longo do caminho do Sol; e, antes do nascer do Sol, a luz zodiacal, um cone tênue de pé sobre o leste.

### Biomas

- Mar: planícies de lava antiga, baixas e escuras. Regolito dos Mares sobre uma camada grossa de Basalto dos Mares, o basalto nu nas encostas íngremes e as suas pedras espalhadas. As crateras são menos numerosas e menores que as das terras altas, com bordas do basalto que quebraram.
- Rima: um mar cortado por canais estreitos e sinuosos, as calhas deixadas por rios de lava cujo teto desabou.
- Claraboia de Tubo de Lava: um mar sobre tubos de lava, túneis revestidos de basalto dos mares, abertos para o céu onde o teto desabou.
- Terras Altas: a terra clara e cheia de crateras em todo o resto, cratera sobre cratera. Regolito das Terras Altas sobre Anortosito, a rocha nua nas encostas íngremes e as suas pedras espalhadas.
- Cratera Jovem: uma cratera recente de borda afiada, com o material ejetado e os raios claros de regolito das terras altas por cima do que atingiram.
- Crateras Polares: crateras em volta dos polos cujo fundo nunca vê o Sol, com Regolito Gelado no fundo.

### Cavernas

- Cavernas serpenteiam debaixo de todo o chão da Lua: uma câmara com túneis saindo dela, uma fenda sobre a câmara aberta para o céu e uma boca onde um túnel sai, numa encosta ou numa planície.
- Tubos de lava sob os mares, com até vinte blocos de largura e revestidos de Basalto dos Mares, uma fileira de claraboias abertas no teto, o regolito que caiu amontoado debaixo de cada uma, na luz.
- Poços nos mares, como o do Mare Tranquillitatis: aberturas com a borda quebrada em degraus, que descem até uma cavidade sob o basalto; menores nas terras altas.

## O que pode dar errado

> [!WARNING]
> Não há ar. Fora de uma sala selada, o seu ar acaba como debaixo d'água, e depois você se afoga. Do lado de fora, os tanques do [Traje Lunar](jsspace:lunar_suit) vestido por inteiro são o seu único ar.

> [!CAUTION]
> As chamas precisam de ar. Tochas, lanternas, velas e fogueiras só queimam numa sala selada com ar, gastando o oxigênio dela, e as fornalhas só queimam combustível onde um fogo poderia queimar. Ilumine a base com luzes que não queimam nada, como pedra luminosa ou lâmpada de redstone. As configurações do J's Space podem deixar tochas, lanternas, velas e fogueiras queimarem em qualquer lugar.

> [!NOTE]
> Tudo o que vive lá cai pela força da Lua, {{jsspace:moon_gravity}}: um pulo leva você bem mais alto, e a descida é mais lenta.

> [!NOTE]
> Se você morrer na Lua, volta na sua cama de lá, se ela estiver numa sala selada com ar; senão, na cabine do módulo em que pousou, se ainda estiver inteira e com ar; senão, em casa, no Overworld.

> [!PROBLEM] Uma dose de radiação se acumula aos poucos.
> A radiação do céu chega ao chão a céu aberto. Cada bloco sólido acima de você barra a maior parte dela, então more sob um teto de rocha ou regolito; o Colete Antirradiação, encaixado no traje, barra uma parte também.

> [!PROBLEM] O pouso fica retido na órbita.
> O servidor exige que a sua equipe conheça antes o que mata na Lua, e ela ainda não conhece: a atmosfera, a temperatura, a radiação, a gravidade e o relevo. Ou o foguete não tem pernas, ou tem propelente de menos para frear, e o servidor retém esses pousos em vez de deixá-los acabar em queda.

> [!PROBLEM] O foguete se espatifa ao tocar o chão.
> Ele não tinha pernas, ou tinha propelente de menos para frear os {{jsspace:moon_landing_delta_v}} da descida. Instale Pernas de Pouso ou um Estágio de Descida do Módulo Lunar, e guarde essa quantidade nos tanques.
