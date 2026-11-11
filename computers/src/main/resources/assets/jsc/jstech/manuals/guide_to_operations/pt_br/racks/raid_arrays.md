---
section: racks
order: 57
icon: jsc:vaultis_array_pcie3
covers_family: jsc:raid_arrays
---
# Modos RAID

Uma controladora RAID é uma placa de expansão: a Vaultis Array de cada era, num slot de expansão de um servidor. Ela junta as unidades do servidor num único array, e enquanto nenhum modo for definido, as unidades permanecem separadas. Você define o modo do array no firmware do servidor, na página de armazenamento do Setup ([](jsc:firmware)). A placa guarda o modo, então ele vai junto com a placa.

```table
source: jsc:storage_cards
caption: Placas de armazenamento
heads: Placa | O que é | Slot | Era | Corte na latência de leitura
```

Table: Modos de array

| Modo | Unidades necessárias | Sobrevive à perda de uma unidade | Vazão |
| --- | --- | --- | --- |
| NONE | 1 | Não | 100% |
| RAID 0 | 2 | Não | 125% |
| RAID 1 | 2 | Sim, até restar uma unidade | 100% |
| RAID 5 | 3 | Sim, uma unidade | 100% |

O espaço utilizável depende do modo. O RAID 0 usa o espaço de todas as suas unidades. O RAID 1 dá o espaço da menor unidade. O RAID 5 dá o espaço de todas as suas unidades menos uma, no tamanho da menor. O RAID 0 nunca reconstrói, já que não guarda cópia. A tela mostra "+25% de vazão" para o RAID 0 e "sobrevive com 1 disco" para o RAID 1.

1. Abra o Setup do servidor e vá para a página de armazenamento dele.
2. Pressione Up ou Down até que o modo desejado apareça em MODO ARRAY.
3. Pressione [[Enter]] para aplicar.

O cache de leitura, a Vaultis Cache de cada era, não precisa de modo. Num slot de expansão de um servidor, ele reduz a latência de leitura das unidades do servidor, mais a cada era mais nova, como mostra a tabela. Fora de um servidor, uma placa de armazenamento fica no slot e não faz nada.

## Se algo der errado

> [!PROBLEM] As unidades do servidor não se juntam.
> Nenhum modo está definido, então as unidades continuam independentes. Defina um modo na página de armazenamento.

> [!PROBLEM] O firmware diz "Nenhuma controladora de armazenamento instalada."
> Coloque uma placa Vaultis Array num dos slots de expansão desta máquina.

> [!PROBLEM] O firmware diz que o modo precisa de mais unidades, como "precisa de 3 unidades".
> Adicione unidades às baias do servidor, ou escolha um modo que precise de menos.

> [!SEE] jsc:rack_drive_bays, jsc:firmware
