---
section: peripherals
order: 76
icon: jsc:standard_redstone_interface
covers_family: jsc:redstone_interfaces
---
# Redstone Interfaces

Uma Redstone Interface é um bloco pequeno que permite a um programa ler o sinal de redstone em uma de suas faces, ou emitir um sinal por essa face. Redstone é a fiação do Minecraft. Um computador alcança a interface por um cabo periférico, da mesma forma que alcança seus outros dispositivos ([](jsc:peripheral_cables)).

```scene
{"balloons": [{"detail": "Ela lê ou emite o sinal de redstone na face para a qual está voltada.", "number": 1, "part": {"box": [5, 1, 15, 11, 7, 16], "x": 12, "y": 0, "z": 11}, "text": "A lente"}, {"number": 2, "part": {"x": 12, "y": 0, "z": 11}, "text": "A Redstone Interface"}, {"detail": "Ela acende enquanto a interface emite um sinal.", "number": 3, "part": {"x": 12, "y": 0, "z": 12}, "text": "Uma lâmpada de redstone"}, {"detail": "Ele se conecta à parte de trás da interface.", "number": 4, "part": {"x": 12, "y": 0, "z": 10}, "text": "O Cabo Periférico"}, {"number": 5, "part": {"x": 14, "y": 0, "z": 10}, "text": "O computador"}], "blocks": [{"state": "jsc:personal_computer[facing=south]", "x": 14, "y": 0, "z": 10}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,8240,0,0,0,0,0]}", "state": "jscore:cable", "x": 13, "y": 0, "z": 10}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2088,0,0,0,0,0]}", "state": "jscore:cable", "x": 12, "y": 0, "z": 10}, {"data": "{Emits:1b,Strength:15,LinkedOwner:3848290738176L}", "state": "jsc:standard_redstone_interface[facing=south]", "x": 12, "y": 0, "z": 11}, {"state": "minecraft:redstone_lamp[lit=true]", "x": 12, "y": 0, "z": 12}], "caption": "Uma Redstone Interface, com sua lente voltada para uma lâmpada de redstone que ela acende, cabeada ao seu computador. Arraste a cena para ver a parte de trás.", "height": 110}
```

> [!DEFINE] Lente
> A face do bloco que lê ou emite um sinal. É a face para a qual você olha ao colocar o bloco.

Coloque-a da mesma forma que um observador. Sua lente aponta para aquilo que você olha. A parte de trás, onde o cabo se conecta, pode ficar virada para qualquer lado. Só a lente lê ou emite.

A interface tem dois modos. Sua potência vai de 0 a 15 em qualquer um dos modos.

> [!DEFINE] Intensidade do sinal
> A potência de um sinal de redstone, de 0 a 15.

Table: Os dois modos

| Modo | O que faz | O que a janela mostra |
| --- | --- | --- |
| IN | Lê o sinal que chega à sua lente. | Lê, com a potência |
| OUT | Emite um sinal pela sua lente. | Emite, com a potência |

Um programa define o modo e a potência. A janela mostra qual programa os definiu por último.

Dê um nome à interface na sua janela, com até 32 caracteres. Os programas encontram uma interface por esse nome, e duas interfaces de um mesmo computador não podem ter o mesmo nome.

A interface não tem energia própria. Sem um computador na outra ponta do seu cabo, sua janela diz "Sem energia", e ela não lê nem emite nada. Ela mantém o seu modo e a sua potência até que um computador seja ligado a ela novamente.

## A janela

Table: Parte da janela

| Parte | O que mostra |
| --- | --- |
| NAME | O nome da interface. Os programas a encontram por este nome. |
| SIGNAL | Se a interface lê ou emite, com a potência. |
| STRENGTH | A potência do sinal, de 0 a 15. |
| MODE | IN ou OUT. |
| SOFTWARE | O programa que definiu por último o modo e a potência, depois de "Definido por". |

## Se algo der errado

> [!PROBLEM] Ela não lê nem emite nada.
> Sua janela diz "Sem energia". Conecte a interface a um computador com um cabo periférico ([](jsc:peripheral_cables)).

> [!PROBLEM] O nome é recusado: "Outra interface já tem este nome".
> Escolha outro nome para esta interface, na janela dela.

> [!PROBLEM] Um programa não consegue encontrar a interface.
> Confira o nome na janela dela. Os programas encontram uma interface apenas pelo nome.

> [!SEE] jsc:redstone_programs, jsc:redstone_library
