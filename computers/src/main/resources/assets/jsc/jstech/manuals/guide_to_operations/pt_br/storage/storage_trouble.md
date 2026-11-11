---
section: storage
order: 123
icon: jsc:vaultis_array_pcie3
---
# Quando o armazenamento dá problema

O armazenamento falha de algumas formas conhecidas, e o jogo diz qual. Uma [Operação](jsc:what_operations_are) que não termina todo o seu trabalho mostra o status dela no Gerenciador de Rede ([](jsc:network_manager)): "parcial" quando parte do trabalho foi feita, "falhou" quando nada foi feito, e "bloqueada" quando desistiu de esperar.

Cada Operação reserva os itens que vai pegar, e outra Operação que precise dos mesmos itens espera por ela. Nas mensagens abaixo, <item> representa o nome do item.

## Operações na rede

Table: Mensagens de uma Operação

| Mensagem | O que significa, e a correção |
| --- | --- |
| "a rede não tinha mais espaço para <item>" | Nem todos os itens cabem. Adicione discos ou servidores à rede. |
| "a rede não tinha <item> suficiente armazenado" | A rede tinha menos do item do que você pediu. Armazene mais dele, ou peça menos. |
| "outra Operation estava segurando o <item> de que precisava" | Outra Operação reservou os itens de que esta precisa. Espere por ela, ou cancele-a ([](jsc:cancelling_and_the_log)). |

## Barramentos e inventários

Table: Mensagens num barramento e num inventário

| Mensagem | O que significa, e a correção |
| --- | --- |
| "o baú está cheio" | O inventário não tem espaço livre. Tire itens dele. |
| "em nenhuma rede" | O cabo do barramento não está em uma rede. Conecte-o a uma ([](jsc:data_cables)). |
| "nenhum inventário à frente" | O barramento não está virado para nada. Coloque-o num cabo de dados ao lado de um inventário. |
| SOB DEMANDA, com "Só se move enquanto o seu cabo tiver sinal de redstone" | O barramento só se move quando o cabo dele tem sinal. Dê um sinal de redstone ao cabo, ou mude o modo do barramento. |

## Se algo der errado

> [!PROBLEM] O barramento não move nada.
> O filtro dele não deixa passar nada, o modo dele está em SOB DEMANDA sem sinal de redstone no cabo, ou ele não está em rede nenhuma. Verifique cada um desses pontos.

## Discos e racks

Table: Mensagens sobre discos e RAID

| Mensagem | O que significa, e a correção |
| --- | --- |
| "<mode> - precisa de <number>+ unidades" | A baia tem menos discos do que esse modo RAID precisa, então a mudança é recusada. Adicione discos e defina o modo de novo. |
| "<name> DEGRADED" | O conjunto perdeu discos, mas ainda entrega os seus itens. Coloque um disco substituto na baia ([](jsc:raid_arrays)). |
| "<name> FAILED" | Sumiram mais discos do que o modo aguenta perder, então o conjunto não dá espaço. Recoloque os discos que faltam. |
| "REBUILD <number>%" | Um disco substituto está copiando de volta os itens do conjunto. O conjunto volta a funcionar quando a cópia termina. |
| "Bloqueado - o chassi não fornece cabeamento para este slot" | Esse slot não aceita disco. Use outro slot. |

> [!WARNING] Tirar qualquer disco de um conjunto RAID 0 apaga os dados de todos os discos do conjunto. O RAID 0 não guarda uma segunda cópia.

Tirar um disco de um array RAID 1 ou RAID 5 mantém os seus itens nos outros discos. O array continua funcionando enquanto espera uma substituição, e se reconstrói quando a substituição entra na baia ([](jsc:raid_arrays)).

## Espaço em um disco

## Se algo der errado

> [!PROBLEM] Uma Operação mostra "a rede não tinha mais espaço para <item>".
> Adicione discos ou servidores. Um disco maior guarda mais itens. O disco de uma era mais nova guarda mais megabytes.

> [!PROBLEM] Um arquivo não cabe, e o computador diz "Espaço livre insuficiente".
> Apague arquivos que você não precisa, ou use um disco maior.

> [!PROBLEM] A instalação de um sistema diz "O disco de destino não tem espaço para <system> (<number> MB necessários)."
> Use um disco maior. O número é o espaço em megabytes (MB) de que o sistema precisa.

> [!SEE] jsc:local_storage, jsc:external_storage_bus, jsc:raid_arrays, jsc:disks
