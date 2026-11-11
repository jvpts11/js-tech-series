---
section: network_operation
order: 102
icon: jsc:mainframe
---
# Identidade de rede, conflitos e redes órfãs

Toda rede tem uma identidade, que a distingue de todas as outras redes. Você nunca digita essa identidade; a rede mostra uma forma curta dela.

Um Mainframe sem rede ainda cria uma rede nova própria. Um Mainframe ligado a um segmento que já pertence a uma rede assume essa rede. Um segmento são os cabos unidos por um Mainframe, ligados em um só.

Table: Palavras na tela do Mainframe

| Palavra | O que significa |
| --- | --- |
| `ONLINE` | O Mainframe está em execução. |
| `OFFLINE` | A compilação não é válida, por isso não pode rodar. |
| `READY` | A compilação é válida, mas ainda não está rodando. |
| `STANDBY` | É um membro de failover, aguardando ([](jsc:failover)). |
| `CONFLICT` | A rede está em conflito, mostrado em vermelho. |
| `LINKED` | O Mainframe está vinculado a uma rede. |
| `--` | O Mainframe não tem rede. |

Dois Mainframes em execução no mesmo segmento são um conflito. A rede entra em colapso até que eles sejam separados: ambos deixam de ser donos da rede, e nada é apagado. Só um Mainframe em execução conta como rival; um desligado não.

Quando um conflito começa, o chat mostra "[J's Computers] NETWORK_CONFLICT: dois Mainframes compartilham uma rede perto de", seguido da posição do Mainframe. Quando o conflito termina, o chat mostra "[J's Computers] Conflito de rede resolvido perto de" e a posição.

Quando o segundo Mainframe sai do segmento, o que restou volta a executar a rede, e o conflito some.

Quando o último Mainframe de uma rede é quebrado, a rede fica órfã. Uma rede órfã mantém o armazenamento, então ele continua lá quando alguém conecta um Mainframe novo. Desligar um Mainframe não torna a rede dele órfã: os dados deixam de passar por ele.

> [!DEFINE] Rede órfã
> Uma rede sem nenhum Mainframe. O armazenamento dela continua lá.

> [!DEFINE] Failover
> Um Mainframe que faz backup do dono da sua rede e assume quando esse dono some.

> [!DEFINE] Reserva
> Um Mainframe de failover que espera, energizado e pronto, sem executar a rede.

## Se algo der errado

> [!PROBLEM] A tela diz CONFLICT, e os dois Mainframes param.
> Dois Mainframes compartilham um mesmo segmento. Remova o cabo que une os segmentos deles, para que cada rede tenha um Mainframe.

## Se algo der errado

> [!PROBLEM] A rede fica órfã depois que você quebrou um Mainframe.
> Esse Mainframe era o último da sua rede, então nenhum Mainframe a executa agora. O armazenamento dele é mantido, como descrito na seção acima.
