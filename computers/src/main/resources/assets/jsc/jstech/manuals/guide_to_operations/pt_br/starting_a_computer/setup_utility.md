---
section: starting_a_computer
order: 5
icon: jsc:motherboard_atx_standard_lga1150
---
# O Setup

O Setup é a própria tela de ajustes do firmware. Você a abre com [[DEL]] enquanto o autoteste roda ([](jsc:turning_on)). O título da tela termina com BIOS Setup Utility.

O Setup tem páginas. Pressione [[Tab]] para ir para a próxima.

Table: As páginas do Setup

| Página | O que mostra |
| --- | --- |
| `Boot` | As entradas com que você pode iniciar. Enter inicia a selecionada. |
| `Boot Order` | Os discos na ordem em que o firmware os tenta. Enter sobre um disco o define como dispositivo de inicialização principal. |
| `Hardware` | As partes que o firmware encontrou nesta máquina. |
| `Storage` | Os modos RAID. Só uma máquina com controladora de armazenamento tem esta página. |

A página Hardware lista Processador, Conjunto de Instruções, Núcleos, Memória, Gráficos, Vídeo, Placa-mãe, Monitores, Era do Hardware, Disco de Inicialização e Destino da Instalação. A linha Memória mostra o tamanho em megabytes (MB) e quantos dos slots têm um módulo.

Table: As teclas do Setup

| Tecla | O que faz |
| --- | --- |
| `Tab` | Vai para a próxima página. |
| `Up and Down` | Move a seleção. Na página Storage, elas mudam o modo RAID. |
| `Enter` | Ativa a linha selecionada: inicia uma entrada, define um dispositivo de inicialização principal ou aplica um modo RAID. |
| `F` | Formata o disco selecionado. O primeiro toque apenas o prepara, e o segundo toque formata. |
| `1 to 9` | Seleciona essa linha. |
| `Escape` | Sai do Setup. |

> [!WARNING] Formatar um disco apaga tudo o que há nele, e os dados não podem ser recuperados.

A página Storage mostra MODO ARRAY com quatro opções para o array RAID ([](jsc:raid_arrays)). Cada uma precisa de unidades. Enter aplica o modo, e trocá-lo apaga o array.

Table: Os modos RAID na página Storage

| Opção | Nota na tela |
| --- | --- |
| `NONE (independent)` | Nenhum: cada unidade funciona por conta própria. |
| `RAID 0` | \+25% de vazão |
| `RAID 1` | sobrevive até 1 unidade |
| `RAID 5` | sobrevive a 1 perda |

A linha Sair deixa o Setup.

## Se algo der errado

> [!PROBLEM] A página Storage diz "Nenhuma controladora de armazenamento instalada."
> Esta máquina não tem controladora de armazenamento, então não tem os modos RAID ([](jsc:raid_arrays)).

> [!SEE] jsc:firmware, jsc:boot_menu, jsc:installing
