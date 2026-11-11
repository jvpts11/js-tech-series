---
section: network_basics
order: 99
icon: jsc:standard_router
covers_family: jsc:routers
---
# Roteadores e repetidores

Um roteador é onde as linhas de uma rede se encontram. Cada era tem o seu, e um roteador une o acesso da sua era à espinha dorsal: seus computadores pequenos de um lado, o Mainframe e os racks do outro. Ele aceita os cabos da sua própria era e de todas as anteriores, em qualquer uma das suas seis faces, e todos eles se tornam uma só rede. Um roteador estende uma rede; ele nunca a divide. Quando sua rede precisar chegar mais longe, você adiciona um roteador.

Table: Os roteadores

| Roteador | Era | O que exige |
| --- | --- | --- |
| Roteador Vintage | Vintage | Os cabos Vintage |
| Roteador Pessoal | Legacy | Os cabos Legacy, e os Vintage |
| Roteador Transition | Transition | Os cabos Transition, e os mais antigos |
| Roteador Standard | Standard | Os cabos Standard, e os mais antigos |
| Roteador Advanced | Advanced | Os cabos Advanced, e os mais antigos |
| Roteador Óptico | Standard | Apenas a fibra da espinha dorsal |
| Roteador Óptico Advanced | Advanced | Apenas a fibra da espinha dorsal |

As eras importam aqui. Uma porta aceita o cabo da sua própria era e de todas as mais antigas, nunca o de uma mais nova. Duas eras de uma mesma linha que se tocam não se unem: elas se encontram num roteador da era mais nova. Por isso, uma máquina antiga numa rede nova precisa de um roteador entre elas.

A partir do Standard, a fibra da espinha dorsal corre só em linha reta. Um roteador óptico aceita apenas a fibra da espinha dorsal, desde a era Standard até a sua própria, e é o bloco que faz a fibra virar e se ramificar.

Um repetidor renova o alcance de um cabo. Coloque-o num trecho de cabo, e o cabo depois dele recomeça a contar o seu alcance, de modo que o trecho pode continuar além do primeiro alcance. Um repetidor aceita todas as linhas da sua era e de todas as anteriores, exceto a linha de longa distância. Ele leva cada linha por conta própria e nunca une uma linha a outra; isso é trabalho de um roteador.

```scene
{"balloons": [{"detail": "Ele liga a linha de acesso ao backbone.", "number": 1, "part": {"x": 0, "y": 0, "z": 0}, "text": "O Roteador Standard"}, {"detail": "Ele leva o backbone longe, mas só em linha reta.", "number": 2, "part": {"x": 1, "y": 0, "z": 0}, "text": "O Cabo de Fibra Óptica"}, {"detail": "Coloque-o em um trecho longo para renovar o alcance do cabo.", "number": 3, "part": {"x": 3, "y": 0, "z": 0}, "text": "O Repetidor Standard"}, {"detail": "Vire e ramifique a fibra aqui.", "number": 4, "part": {"x": 6, "y": 0, "z": 0}, "text": "O Roteador Óptico"}, {"detail": "Os cabos de acesso dos computadores distantes se conectam a ele.", "number": 5, "part": {"x": 6, "y": 0, "z": 3}, "text": "O roteador da outra ponta"}], "blocks": [{"state": "jsc:standard_router", "x": 0, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,4144,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 1, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,8240,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 2, "y": 0, "z": 0}, {"state": "jsc:standard_repeater", "x": 3, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,4144,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,8240,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 5, "y": 0, "z": 0}, {"state": "jsc:optical_router", "x": 6, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,1036,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 6, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,2060,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 6, "y": 0, "z": 2}, {"state": "jsc:standard_router", "x": 6, "y": 0, "z": 3}], "caption": "Roteadores, um repetidor e fibra óptica levando uma rede para longe. A fibra segue em linha reta, então o Roteador Óptico a faz virar. Arraste para vê-la pelo outro lado.", "height": 120, "view": {"zoom": 1.35}}
```

Hubs não são blocos de dados. Um hub pertence à linha de periféricos: ele recebe uma das portas de dispositivo do seu computador e oferece portas próprias, para que um computador alcance mais dispositivos. Um hub não transporta a rede ([](jsc:peripheral_cables)).

Table: Os hubs

| Hub | Portas que oferece |
| --- | --- |
| Hub Vintage | 2 portas |
| Hub Legacy | 4 portas |
| Hub Transition | 4 portas |
| Hub Standard | 7 portas |
| Hub Advanced | 7 portas |

O Roteador de Servidores é um bloco diferente, com página própria ([](jsc:server_router)).

## Se algo der errado

> [!PROBLEM] Uma máquina mais antiga não entra numa rede mais nova.
> Coloque um roteador da era mais nova entre eles.

> [!PROBLEM] A ponta distante continua fora da rede.
> Um trecho de um dos lados do repetidor ainda é mais longo que o seu alcance. Adicione um repetidor dentro dele.
