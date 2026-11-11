---
section: gateway
order: 163
icon: jsc:network_gateway
covers_family: jsc:network_gateway
---
# O Gateway de Rede

Você também joga com o ComputerCraft? O Gateway de Rede é uma ponte entre os computadores dele e os seus. Você o coloca como qualquer bloco, e ele une dois lados: uma rede cabeada do ComputerCraft na frente, e um dos seus computadores atrás.

O Gateway é colocado virado para você. A frente dele se liga ao lado do ComputerCraft, e a parte traseira tem o soquete para um [Cabo Periférico](jsc:peripheral_cables). Todo computador aceita esse cabo, e um cabo de uma era posterior não encaixa neste soquete.

```scene
{"balloons": [{"detail": "Sua frente se conecta ao ComputerCraft, e sua parte de trás recebe o cabo que leva ao seu computador.", "number": 1, "part": {"x": 15, "y": 0, "z": 15}, "text": "O Gateway de Rede"}, {"number": 2, "part": {"x": 15, "y": 0, "z": 16}, "text": "Um computador ComputerCraft"}, {"number": 3, "part": {"x": 15, "y": 0, "z": 14}, "text": "O Cabo Periférico"}, {"number": 4, "part": {"x": 13, "y": 0, "z": 13}, "text": "O host"}], "blocks": [{"state": "jsc:personal_computer[facing=south]", "x": 13, "y": 0, "z": 13}, {"data": "{Wires:[{cable:\"jsc:vintage_peripheral_cable\"}],Links:[I;0,0,0,4144,0,0,0,0,0]}", "state": "jscore:cable", "x": 14, "y": 0, "z": 13}, {"data": "{Wires:[{cable:\"jsc:vintage_peripheral_cable\"}],Links:[I;0,0,0,24,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 13}, {"data": "{Wires:[{cable:\"jsc:vintage_peripheral_cable\"}],Links:[I;0,0,0,2060,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 14}, {"data": "{LinkedOwner:3573412843520L,LinkLength:3}", "state": "jsc:network_gateway[facing=south,lit=true]", "x": 15, "y": 0, "z": 15}, {"state": "computercraft:computer_normal[facing=north]", "x": 15, "y": 0, "z": 16}], "caption": "Um Gateway de Rede ligado a um Computador Pessoal, com um computador ComputerCraft encostado em sua frente. O computador ComputerCraft só aparece quando o ComputerCraft está instalado. Arraste a cena para ver a parte de trás.", "height": 120, "view": {"yaw": -75.0, "zoom": 1.3}}
```

> [!DEFINE] Rede cabeada
> Os cabos e os modems cabeados que os computadores do ComputerCraft usam para alcançar uns aos outros e os dispositivos ao redor.

## Vinculando a um host

O computador ao qual um Gateway está vinculado é o seu host. Os dois se vinculam de uma de duas maneiras, e a tela do Gateway mostra qual delas ele encontrou.

Table: Como um Gateway se vincula ao seu host

| Vínculo | O que significa |
| --- | --- |
| Cabo Periférico | Um cabo sai da parte traseira do Gateway até o host, com até 16 blocos de comprimento. |
| adjacente | O Gateway fica diretamente encostado no host, sem cabo. |

Quando o Gateway está vinculado, a tela mostra o nome dele, o host e o vínculo, como "cabo, 5 blocos" ou "adjacente". Quando ele não tem host, a tela diz que não está vinculado e pede para você conectar o cabo na parte traseira.

> [!DEFINE] Host
> O computador ao qual um Gateway de Rede está vinculado por meio do soquete traseiro.

## O buffer

Um clique com o botão direito abre o buffer do Gateway. Ele tem nove slots de item sob o rótulo "BUFFER DE ITENS", e a dica "pull chega aqui, push sai" informa para que ele serve.

Os itens que um programa do ComputerCraft puxa da rede chegam ao buffer, e os itens que ele empurra saem dali. Os itens do buffer caem no chão quando você quebra o bloco, então esvazie-o antes.

Todo o resto do Gateway, como as suas permissões e os outros controles, é definido no seu computador Host.

## Nome e luz

Na primeira vez que se vincula, o Gateway recebe o nome gateway-1, depois gateway-2, e assim por diante. Você o renomeia no Gerenciador de Gateway ou com o comando gateway ([](jsc:gateway_manager)).

O nome é ajustado enquanto você digita: minúsculas, letras, dígitos, sublinhados e traços, e qualquer outra sequência de caracteres vira um único traço. Um nome tem no máximo 24 caracteres, e um nome vazio volta ao padrão.

A luz de status do bloco fica acesa enquanto o Gateway está vinculado. Identificar faz ela piscar por 3 segundos, para que você encontre o bloco entre outros.

## Se algo der errado

> [!PROBLEM] O Gateway não faz nada.
> Ele não está vinculado a um host, ou o chunk do host não está carregado, ou o host o desativou. Confira o vínculo, mantenha o chunk do host carregado e ative o Gateway no host.

> [!PROBLEM] O lado do ComputerCraft não faz nada.
> O CC: Tweaked não está instalado. Sem ele, o bloco continua vinculado ao seu host e guarda itens; só o lado do ComputerCraft nunca sobe.

> [!PROBLEM] A tela diz que não está vinculado e pede para conectar o cabo na parte traseira.
> Coloque um Cabo Periférico no soquete traseiro e leve-o até o host, ou encoste o Gateway diretamente no host.

> [!PROBLEM] Renomear é recusado com "outro gateway na mesma rede já se chama <name>".
> Escolha outro nome. Dois gateways na mesma rede não podem ter o mesmo nome.

> [!SEE] jsc:peripheral_cables, jsc:mainframes
