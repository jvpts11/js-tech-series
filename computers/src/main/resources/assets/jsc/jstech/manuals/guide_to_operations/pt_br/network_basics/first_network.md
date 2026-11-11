---
section: network_basics
order: 96
icon: jsc:mainframe
shows: [jsc:mainframe, jsc:server_rack, jsc:server, jsc:standard_router, jsc:fibre_cable, jsc:gigabit_cable, jsc:personal_computer]
---
# Sua primeira rede

Esta é a menor rede que faz algo útil.

```scene
{"balloons": [{"detail": "Ele executa a rede. Conecte o cabo backbone a qualquer lado dele.", "number": 1, "part": {"x": 1, "y": 0, "z": 1}, "text": "O Mainframe"}, {"detail": "Passe-o do roteador até o Mainframe.", "number": 2, "part": {"x": 3, "y": 0, "z": 1}, "text": "O cabo backbone"}, {"detail": "Conecte a ele o cabo de acesso e o cabo backbone.", "number": 3, "part": {"x": 5, "y": 0, "z": 1}, "text": "O roteador"}, {"detail": "Passe-o do computador até o roteador.", "number": 4, "part": {"x": 6, "y": 0, "z": 1}, "text": "O cabo de acesso"}, {"detail": "Conecte o cabo de acesso na parte de trás dele.", "number": 5, "part": {"x": 6, "y": 0, "z": 2}, "text": "O Computador Pessoal"}], "blocks": [{"data": "{VisualFlags:7}", "state": "jsc:mainframe[facing=south,optical=true]", "x": 1, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,4144,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,8240,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 1}, {"state": "jsc:standard_router", "x": 5, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;6168,0,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 6, "y": 0, "z": 1}, {"state": "jsc:personal_computer[facing=south]", "x": 6, "y": 0, "z": 2}], "caption": "Um Mainframe, um roteador, cabos de dados e um Computador Pessoal: a menor rede possível. Arraste para ver a parte de trás.", "height": 120}
```

1. Monte um Mainframe e dê a ele as peças: uma placa-mãe MTX, uma fonte, processadores, memória, um disco e uma placa de vídeo ([](jsc:mainframes)).
2. Instale um sistema no Mainframe, como o MC-NET, como em qualquer computador: um monitor na placa de vídeo dele, e a mídia de instalação em uma unidade conectada a ele ([](jsc:installing)). Um Mainframe sem sistema não executa nada.
3. Monte um Rack de Servidores e coloque servidores com discos nos compartimentos dele ([](jsc:server_racks)).
4. Ligue o Mainframe e o rack com o cabo backbone da era deles: HBW na era Legacy, fibra óptica na era Standard. A fibra só entra no Mainframe enquanto ele tem uma Placa de Rede Óptica ([](jsc:expansion_cards)), e só entra no rack por uma Interface de Fibra Óptica montada nele ([](jsc:rack_equipment)).
5. Coloque um roteador e ligue seu Computador Pessoal a ele com o cabo de acesso, Ethernet ou Gigabit ([](jsc:routers)). Passe o backbone do roteador até o Mainframe.
6. No computador, abra o programa Rede ([](jsc:network_interactor)). Coloque itens, e eles são guardados nos discos dos servidores; peça-os de volta, e eles saem.

O Mainframe se liga a todo cabo de dados que toca qualquer lado da sua ocupação, então um backbone pode encontrá-lo pelo lado mais próximo. A fibra é o único cabo que ele só aceita com a placa.

É isso. Daqui em diante, tudo o mais é mais do mesmo: mais racks para mais armazenamento, mais computadores na linha de acesso, um Computador de Fabricação para autofabricação.

## Se algo der errado

> [!PROBLEM] Um computador não está na rede.
> O cabo dele é mais longo que o seu alcance, ou de outra era que o roteador. Adicione um repetidor, ou um roteador da era mais nova.

> [!PROBLEM] O programa Rede diz "○ Mainframe offline".
> Não há Mainframe na rede do computador. Siga os cabos do computador até o Mainframe ([](jsc:data_cables)).

> [!PROBLEM] Nada entra no armazenamento, e toda Operação fica na fila.
> O Mainframe não tem sistema, então ele segura toda Operação até que um seja instalado ([](jsc:installing)).

> [!PROBLEM] A fibra não se liga ao Mainframe nem ao rack.
> Coloque uma Placa de Rede Óptica nos slots de placa do Mainframe ([](jsc:expansion_cards)), e monte uma Interface de Fibra Óptica no rack ([](jsc:rack_equipment)).
