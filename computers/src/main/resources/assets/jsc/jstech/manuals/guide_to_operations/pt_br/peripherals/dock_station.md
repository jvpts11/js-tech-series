---
section: peripherals
order: 72
icon: jsc:dock_station
---
# A Estação de Dock

A Estação de Dock lê discos de armazenamento e pendrives USB (Barramento Serial Universal) para um computador. Você a conecta a um computador como qualquer dispositivo ([](jsc:peripheral_cables)), com um cabo de qualquer era. Ela tem três baias para discos e uma porta USB para um pendrive.

```scene
{"balloons": [{"detail": "Ela aceita um disco rígido.", "number": 1, "part": {"box": [5, 10, 15, 14, 14, 16], "x": 15, "y": 0, "z": 15}, "text": "A baia de 3,5\""}, {"detail": "Ela aceita uma unidade de estado sólido (SSD).", "number": 2, "part": {"box": [5, 6, 15, 14, 9, 16], "x": 15, "y": 0, "z": 15}, "text": "A baia de 2,5\""}, {"detail": "Ela aceita um disco Non-Volatile Memory Express (NVMe).", "number": 3, "part": {"box": [5, 2, 15, 14, 5, 16], "x": 15, "y": 0, "z": 15}, "text": "A bandeja M.2"}, {"detail": "Ela aceita um pen drive USB.", "number": 4, "part": {"box": [2, 7, 15, 4, 9, 16], "x": 15, "y": 0, "z": 15}, "text": "A porta USB"}, {"number": 5, "part": {"x": 15, "y": 0, "z": 14}, "text": "O Cabo Periférico"}, {"number": 6, "part": {"x": 12, "y": 0, "z": 14}, "text": "O computador"}], "blocks": [{"state": "jsc:personal_computer[facing=south]", "x": 12, "y": 0, "z": 14}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,4144,0,0,0,0,0]}", "state": "jscore:cable", "x": 13, "y": 0, "z": 14}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,48,0,0,0,0,0]}", "state": "jscore:cable", "x": 14, "y": 0, "z": 14}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2072,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 14}, {"data": "{LinkedOwner:3298534940672L,MediaSlot:{Size:1,Items:[{Slot:0,id:\"jsc:usb_flash_drive\",count:1}]},Bays:{Size:3,Items:[{Slot:0,id:\"jsc:disk_hdd_500g\",count:1},{Slot:1,id:\"jsc:disk_ssd_500g\",count:1},{Slot:2,id:\"jsc:disk_nvme_500g\",count:1}]}}", "state": "jsc:dock_station[facing=south,loaded=true]", "x": 15, "y": 0, "z": 15}], "caption": "Uma Estação de Dock ligada a um Computador Pessoal, com um disco em cada uma das três baias e um pen drive USB em sua porta. Arraste a cena para ver a parte de trás.", "height": 120, "view": {"zoom": 1.35}}
```

> [!DEFINE] Baia
> Uma bandeja da Estação de Dock que aceita um disco de um só tamanho.

Table: As baias e a porta

| Local | Aceita | Exibido na Dock |
| --- | --- | --- |
| Baia | Um disco rígido | BAIA 3.5" |
| Baia | Uma unidade de estado sólido (SSD) | BAIA 2.5" |
| Baia | Um disco Non-Volatile Memory Express (NVMe) | GAVETA M.2 |
| Porta | Um pen drive USB | USB |

Uma baia aceita um disco, e somente um disco do seu próprio tamanho e da era da Dock, a Standard, ou de uma mais antiga. A Dock recusa tudo que não seja um disco ou um pen drive.

## Colocar e tirar coisas

1. Clique na Dock com um disco na mão para colocá-lo na sua baia.
2. Clique na Dock com um pen drive USB na mão para encaixá-lo na porta.
3. Agache e clique na Dock para tirar a última coisa que você colocou: primeiro o pen drive, depois as baias de baixo para cima.
4. Clique na Dock com a mão vazia para abrir a janela dela.

O instalador de um sistema pode estar em um pen drive USB na Dock, para que você instale a partir da Dock ([](jsc:installing)).

## A janela

A janela mostra cada baia e a porta, com o que elas guardam. Você também pode colocar discos e tirá-los por lá.

Table: Parte da janela

| Parte | O que mostra |
| --- | --- |
| Linha de ligação | "Ligada a um computador" quando um computador chega à Dock, e "Nenhum computador vinculado" quando nenhum chega. |
| Linhas das baias | Cada baia, vazia, ou com seu disco e quanto dele está em uso. |
| Linha da porta | A porta USB, com o pen drive dentro, ou vazia. |
| Letras | A letra de unidade que o computador dá a cada disco e ao pen drive. |

> [!DEFINE] Letra da unidade
> O nome curto que um computador dá a uma unidade ou a um disco acoplado à Dock, para que os programas possam encontrá-lo.

O computador enxerga cada disco na Dock como uma unidade externa, e pode abrir e copiar para ele. A Dock funciona somente enquanto um computador estiver vinculado a ela.

## Se algo der errado

> [!PROBLEM] Essa gaveta já tem um disco - agache e clique ou use a janela para tirá-lo.
> Retire o disco pela janela primeiro.

> [!PROBLEM] A Estação de Dock mostra "Não está ligada a um computador" ou "Nenhum computador vinculado".
> Ligue a Dock a um computador com um cabo periférico ([](jsc:peripheral_cables)).

> [!PROBLEM] Nada acontece quando você clica na Estação de Dock.
> Só discos e pen drives USB entram no Dock. Qualquer outra coisa é recusada, e o Dock não diz nada.

> [!PROBLEM] Novo demais para esta Dock Station: uma dock Standard aceita discos da sua era ou mais antigos.
> O disco é da Advanced. Leia-o num computador Advanced.

> [!SEE] jsc:drives, jsc:disks, jsc:installing
