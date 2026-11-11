---
section: network_operation
order: 103
icon: jsc:mainframe
---
# Failover

Um Mainframe pode ser um membro de failover. Um membro de failover não roda a rede enquanto o proprietário está presente. Ele espera, ligado e pronto, e assume quando o proprietário some.

```scene
{"balloons": [{"detail": "Sua tela mostra ONLINE.", "number": 1, "part": {"x": 1, "y": 0, "z": 1}, "text": "O Mainframe que executa a rede"}, {"detail": "Ligue os dois Mainframes ao mesmo segmento.", "number": 2, "part": {"x": 4, "y": 0, "z": 1}, "text": "O cabo do segmento"}, {"detail": "Ligue a chave FAILOVER dele. Sua tela mostra STANDBY.", "number": 3, "part": {"x": 8, "y": 0, "z": 1}, "text": "O Mainframe de reserva"}], "blocks": [{"data": "{VisualFlags:7}", "state": "jsc:mainframe[facing=south,optical=true]", "x": 1, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,4144,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,48,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,48,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 5, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,8240,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 6, "y": 0, "z": 1}, {"data": "{VisualFlags:7}", "state": "jsc:mainframe[facing=south,optical=true]", "x": 8, "y": 0, "z": 1}], "caption": "Dois Mainframes na mesma rede: um a executa e o outro fica de reserva para assumir. Arraste para vê-los por trás.", "height": 110}
```

Ligar o failover remove a rede que este Mainframe possuía, em vez de deixá-la órfã. Uma máquina que vai apoiar a rede de outra não pode manter uma própria, ou as duas entrariam em conflito assim que fossem conectadas.

Você cria um membro de failover em três passos, e a tela informa qual papel o Mainframe tem.

1. Abra a tela do Mainframe e ligue a opção FAILOVER.
2. Conecte o Mainframe de failover ao mesmo segmento do proprietário.
3. Confira se a tela mostra STANDBY.

Table: Os papéis dos Mainframes

| Papel | O que faz |
| --- | --- |
| Proprietário, com failover desligado | Roda a rede no seu segmento. |
| `STANDBY` | Ligada e sincronizada, mas sem rodar a rede. |
| Ativo, após uma assunção | Assumiu a rede, e agora a roda. |

Uma reserva só assume quando nenhum proprietário está rodando no seu segmento. Então ela espera 60 ticks, que são 3 segundos de tempo de jogo, antes de decidir que o proprietário realmente sumiu. A espera dá tempo para o proprietário voltar primeiro.

Quando várias reservas esperam, só a posicionada mais abaixo no mundo, pela sua posição de bloco, assume. As outras continuam em reserva.

Uma reserva não mantém um conflito. Só um proprietário rodando pode colocar uma rede em conflito. A espera é guardada quando o mundo recarrega, então uma assunção em andamento não reinicia a sua espera.

## Se algo der errado

> [!PROBLEM] A reserva nunca assume.
> Outra reserva com posição menor está esperando primeiro, ou a reserva não está no segmento do proprietário. Uma reserva não tem nada a apoiar se não estiver nesse segmento.

## Se algo der errado

> [!PROBLEM] A tela mostra CONFLICT.
> Dois proprietários rodando compartilham um segmento. Uma reserva não causa conflito. Separe os proprietários ([](jsc:network_identity)).
