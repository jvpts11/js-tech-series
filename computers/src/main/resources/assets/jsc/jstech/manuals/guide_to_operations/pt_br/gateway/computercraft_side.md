---
section: gateway
order: 164
---
# O Gateway e o ComputerCraft

Na frente dele, o Gateway se junta a uma rede cabeada do ComputerCraft. Um computador do ComputerCraft o vê como um periférico do tipo jsc_gateway. O nome dele é jsc_gateway_ seguido do nome do Gateway, com cada traço trocado por sublinhado: gateway-1 vira jsc_gateway_gateway_1.

Um computador do ComputerCraft só encontra o Gateway quando ele está no fio dele. Coloque um cabo de rede ou um modem com fio encostado na frente do Gateway. Sem o CC: Tweaked, não existe lado do ComputerCraft de forma alguma.

> [!DEFINE] Orçamento
> A parcela de cada tick do jogo que as chamadas do Gateway consomem dos programas do seu host, em porcentagem.

## O que o lado do ComputerCraft pode fazer

Um programa do ComputerCraft chama o periférico do Gateway. O Gateway atende cada chamada com o mesmo serviço que os seus próprios programas usam, sob as permissões que você definiu no host ([](jsc:gateway_permissions)). O custo de uma chamada sai do orçamento do host.

O Gateway mostra o estado deste lado na sua tela e com o comando gateway. Ele diz "alcançável" quando um computador do ComputerCraft está conectado ou no seu fio, e "nada conectado" quando nenhum está.

## Eventos que o Gateway envia

O Gateway envia eventos aos computadores do ComputerCraft, para que um programa possa esperar por eles em vez de perguntar sem parar.

Table: Eventos que o Gateway envia

| Evento | Enviado quando |
| --- | --- |
| `jsc_operation(id, status)` | Uma operação que o computador iniciou é concluída. Só esse computador ouve. |
| `jsc_stock(name, total, previous)` | O total de um nome observado muda. O Gateway verifica cada observador a cada 20 ticks. |
| `jsc_test(name)` | Você pressiona Test event no Gerenciador de Gateway, ou envia o teste pelo comando gateway. |

O estado de uma operação é um de done, partial, failed, running, waiting, locked, pending, discarded ou unknown. Os tipos de operação são pull, push, delete, move, craft, analyze, reindex, vacuum, drop, update e operation.

## Enviando uma linha de volta

Um programa do ComputerCraft pode chamar send para dizer uma linha ao host. Só um programa do J's Computers que esteja ouvindo escuta a linha, e cada linha é gravada no log do Gateway conforme foi dita. Sem ninguém ouvindo, a linha não chega a ninguém.

O progresso Conversando com os Vizinhos é conquistado quando um programa ouve um computador do ComputerCraft por meio de um Gateway de Rede.

## Se algo der errado

> [!PROBLEM] Um programa do ComputerCraft recebe um erro de volta.
> Leia o texto dele: é a recusa do Gateway, como "negado: operações estão desligadas". Cada recusa e a sua correção estão em [](jsc:gateway_permissions).

> [!PROBLEM] O erro é a mensagem de um método do próprio CC: Tweaked.
> O texto é a mensagem própria daquele método. Quando o método não dá nenhuma, o texto é o nome do método seguido de failed.

> [!PROBLEM] O programa recebe o texto "não há <name> no fio deste Gateway".
> O periférico citado não está no fio do Gateway. Confira o nome dele, e conecte-o ao fio.

> [!PROBLEM] O programa recebe o texto "não existe o método <name> em <peripheral>".
> O nome do método não é um dos métodos do periférico. Confira a grafia em [](jsc:computercraft_peripheral).

> [!PROBLEM] Um computador do ComputerCraft não consegue ver o Gateway.
> O Gateway não está no fio dele. Coloque um cabo de rede ou um modem com fio encostado na frente dele.

> [!SEE] jsc:gateway_manager, jsc:computercraft_peripheral
