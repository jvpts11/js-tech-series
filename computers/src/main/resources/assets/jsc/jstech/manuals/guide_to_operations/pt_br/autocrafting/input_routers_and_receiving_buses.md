---
section: autocrafting
order: 146
icon: jsc:crafting_input_router
---
# Roteadores de entrada e Barramentos de Recepção

Um Roteador de Entrada de Fabricação fica no cabo que sai de uma Interface de Fabricação, encostado em uma face de entrada de uma máquina que tem várias. Sua dica diz "No cabo que sai de uma Interface de Fabricação, encostado em uma face de entrada de uma máquina com várias".

A interface envia cada entrada de um padrão pelo roteador escolhido para ela. Um roteador não move nada sozinho: a interface alimenta por ele, e a interface mede e contabiliza cada item.

Cada roteador tem um filtro, configurado do mesmo modo que os filtros dos barramentos ([](jsc:buses)). O filtro aceita um item ou uma tag, e a tela pede "item, ou #tag". Uma tag tem a forma "c:ores".

O roteador padrão de uma entrada é aquele cujo filtro a aceita. A tela diz "Escolhido na interface", então você pode escolher outro roteador na interface.

Table: O que a interface diz sobre um roteador

| O que a tela diz | O que significa |
| --- | --- |
| aceita aqui | O roteador aceita esta entrada para a máquina nesta face. |
| não aceita nada aqui | O roteador não aceita nenhuma entrada para a face. |
| nenhuma máquina nessa face | Não há nada na frente do roteador. |
| também aceita por outro roteador | Dois roteadores aceitam a mesma entrada, e a tela informa qual deles a interface usa. |
| Lingote de Cobre x3 para Lingote de Bronze | Uma rota: três Lingotes de Cobre para o padrão do Lingote de Bronze. |

Um Barramento de Recepção de Fabricação fica no cabo de fabricação, encostado na saída de uma máquina. Sua dica diz "No cabo de fabricação, encostado na saída de uma máquina; recolhe o que os jobs da sua interface fizeram". Ele recolhe o que sai da máquina e o credita à fabricação que alimentou a máquina.

```scene
{"balloons": [{"detail": "Ele fica encostado em uma face de entrada da máquina.", "number": 1, "part": {"box": [6, 2, 2, 16, 14, 14], "x": 0, "y": 0, "z": 1}, "text": "O Roteador de Entrada de Fabricação"}, {"detail": "Ele fica encostado na saída da máquina.", "number": 2, "part": {"box": [0, 2, 2, 10, 14, 14], "x": 2, "y": 0, "z": 1}, "text": "O Barramento de Recepção de Fabricação"}, {"number": 3, "part": {"x": 1, "y": 0, "z": 1}, "text": "A máquina"}, {"number": 4, "part": {"x": 0, "y": 0, "z": 2}, "text": "O Cabo de Fabricação"}], "blocks": [{"state": "minecraft:furnace[facing=south]", "x": 1, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,8,0,0,0],Parts:[{Face:5b,Type:\"jsc:crafting_input_router\",Data:{}}]}", "state": "jscore:cable", "x": 0, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,4,0,0,0]}", "state": "jscore:cable", "x": 0, "y": 0, "z": 2}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,8,0,0,0],Parts:[{Face:4b,Type:\"jsc:receiving_bus\",Data:{}}]}", "state": "jscore:cable", "x": 2, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,4,0,0,0]}", "state": "jscore:cable", "x": 2, "y": 0, "z": 2}], "caption": "Um Roteador de Entrada e um Barramento de Recepção no cabo de fabricação, cada um encostado em uma face de uma fornalha. Arraste para vê-los por trás.", "height": 110, "view": {"zoom": 1.2}}
```

O barramento se liga sozinho à Interface de Fabricação que alimenta a sua máquina, encostada nela ou pelos roteadores do próprio cabo. Você também pode ligá-lo manualmente a uma interface, ou a várias. A ligação manual é como se trata uma saída que vai para um baú ao lado da máquina, e, manualmente, o barramento aceita até 8 ligações.

Table: Os textos de vinculação de um barramento

| O texto | O que significa |
| --- | --- |
| sozinho: a interface que alimenta esta máquina | O barramento encontrou a interface por conta própria. |
| à mão | Você vinculou o barramento à interface pessoalmente. |
| TIE TO INTERFACE | O botão que vincula o barramento a uma interface à mão. |

Enquanto uma tarefa das suas interfaces executa, o que sai da máquina é creditado a essa tarefa, até o total que a tarefa colocou nela. O que já estava na máquina antes de a tarefa começar continua lá. Um fluido ou uma substância química que nenhum padrão lista fica na máquina como seu buffer.

Table: Os estados de um barramento

| Estado | O que significa |
| --- | --- |
| CREDITED | A saída conta para a fabricação que alimentou a máquina. |
| UNEXPECTED | A saída não está no padrão, então vai para a rede. |
| LATE | A tarefa já tinha terminado, então a saída vai para onde vão as saídas daquela tarefa. |

Os roteadores e os barramentos são peças de cabo, então se encaixam no cabo de fabricação. O cabo próprio de uma interface tem uma cor separada do cabo de fabricação principal, o que mantém os dois trechos separados ([](jsc:data_cables)).

## Se algo der errado

> [!PROBLEM] A interface diz "nenhum roteador do cabo aceita: este padrão não pode correr".
> Coloque um Roteador de Entrada de Fabricação no cabo da interface, encostado na face da entrada, com um filtro que aceite essa entrada.

> [!PROBLEM] A tela diz "Não está no cabo de nenhuma interface: nada o usa."
> Coloque o roteador no cabo que sai de uma interface.

> [!PROBLEM] A tela diz que uma entrada é roteada aqui, mas o filtro não a aceita.
> Altere o filtro para que ele aceite essa entrada, ou escolha outro roteador.

> [!PROBLEM] O barramento diz "em nenhuma rede".
> Coloque o barramento no cabo de fabricação de uma rede, ao lado de uma interface.

> [!PROBLEM] O barramento diz "Não vinculado: nenhuma interface alimenta esta máquina. Vincule-o a uma à mão."
> Vincule-o a uma interface à mão, com o botão TIE TO INTERFACE.

> [!SEE] jsc:crafting_interfaces, jsc:buses, jsc:data_cables, jsc:when_a_craft_goes_wrong
