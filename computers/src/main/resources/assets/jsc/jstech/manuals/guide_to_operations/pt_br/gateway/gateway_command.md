---
section: gateway
order: 167
---
# O comando gateway

O comando gateway gerencia os Gateways pela linha de comando de um computador. Ele funciona em qualquer computador com portas de periférico, mesmo em um sem Gateway, para que possa dizer o que está faltando.

1. Digite gateway list para ver os Gateways nas portas deste computador.
2. Digite o nome do Gateway, seguido de status, perms, computers ou log, para ver os detalhes dele.

Table: As formas do comando

| Você digita | O que faz |
| --- | --- |
| `gateway list` | Lista os Gateways nas portas deste computador, com NAME, LINK, CC e WHERE. |
| `<name> status` | Mostra o link, a rede, o orçamento, o buffer e as últimas linhas do log. |
| `<name> perms` | Mostra as opções de leitura e de operações, o teto de prioridade e as chamadas por tick. |
| `<name> computers` | Lista os computadores ComputerCraft conectados, com ID, LABEL, STATE, AGENT e VISTO POR ÚLTIMO. |
| `<name> log` | Mostra o que o Gateway fez, o mais antigo primeiro, na parte de baixo. |
| `<name> rename <new>` | Dá ao Gateway um novo nome, com as mesmas regras do Gerenciador de Gateway. |
| `<name> identify` | Pisca as luzes do Gateway. |
| `<name> clear` | Envia os itens do buffer para a rede. |
| `<name> test` | Envia o evento jsc_test para os computadores que escutam por ele. |
| `<name> set read on\|off` | Liga ou desliga a opção de leitura. |
| `<name> set operations on\|off` | Liga ou desliga a opção de operações. |
| `<name> set priority low\|medium\|high` | Define o teto de prioridade. |
| `<name> set cap 4\|8\|16` | Define as chamadas por tick. |

Os nomes são comparados sem diferenciar maiúsculas de minúsculas, então Gateway-1 e gateway-1 nomeiam o mesmo Gateway.

## Se algo der errado

> [!PROBLEM] O comando diz que não há gateways nas portas deste computador.
> Nenhum Gateway está conectado a uma porta deste computador. Conecte o cabo na parte de trás do Gateway ([](jsc:network_gateway)).

> [!PROBLEM] O comando diz que CC: Tweaked não está instalado.
> Instale o CC: Tweaked para usar o lado do ComputerCraft. O resto do Gateway continua funcionando.

> [!PROBLEM] O comando diz que não há gateway chamado <name> neste computador.
> Confira o nome com gateway list. Os nomes não diferenciam maiúsculas de minúsculas, mas a grafia precisa coincidir.

> [!PROBLEM] O comando diz que este terminal não está em um computador.
> Execute o comando na linha de comando de um computador.
