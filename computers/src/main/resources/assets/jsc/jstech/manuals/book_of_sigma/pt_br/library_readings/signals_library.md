---
section: library_readings
order: 461
---
# Signals: rádios e as frequências ouvidas

O J's Core manda sinais sem fio por frequência: um transmissor manda um valor de 0 a 15, e todo receptor na frequência dele o repassa. Um programa chega a um rádio ligado à máquina dele do jeito que chega a um sensor: por um Redstone Interface ligado à máquina, com a lente virada para o rádio. Você os usa pelo Signals, um tipo embutido do System.Machine. O Signals veio com o Sigma# 3.

Table: Membros do Signals

| Membro | O que faz |
| --- | --- |
| `Signals.Radio(name)` | O rádio na frente da interface com esse nome, como um registro RadioInfo, ou null quando nenhuma interface tem esse nome ou ela não está virada para um rádio. |
| `Signals.Send(name, value)` | Manda o valor, de 0 a 15, pelo transmissor na frente da interface com esse nome, fazendo a interface emiti-lo. Dá não quando a interface não está virada para um transmissor, ou não está viva. |
| `Signals.Heard()` | Todas as frequências ouvidas onde a máquina está, como uma lista de registros HeardSignal, como um signal probe ali as mostraria. |

Table: Registros do Signals

| Registro | Campos | O que cada campo guarda |
| --- | --- | --- |
| `RadioInfo` | `RadioInfo.Role, RadioInfo.Band, RadioInfo.Frequency, RadioInfo.Value, RadioInfo.Clash` | Role: transmitter, receiver ou relay. Band: de quem são as frequências em que ele está. Frequency: o número dela. Value: o que um transmissor manda, ou o que um receptor ouve. Clash: sim quando dois transmissores do mesmo dono mandam na frequência dele. |
| `HeardSignal` | `HeardSignal.Band, HeardSignal.Frequency, HeardSignal.Bars, HeardSignal.Value, HeardSignal.Clash, HeardSignal.Relayed` | Band e Frequency: qual frequência é. Bars: o quão bem ela chega. Value: o que ela carrega. Clash: sim quando ela não carrega nada por causa de um conflito. Relayed: sim quando ela chega por um repetidor. |

O Send é o mesmo que o Redstone.Out naquela interface: o transmissor manda o que a lente dá a ele. A última configuração vale, seja qual for a chamada que a fez.

> [!DEFINE] Frequência
> Uma linha numerada em que um transmissor manda um valor, de 1 a 9999.

> [!DEFINE] Conflito
> Dois transmissores do mesmo dono na mesma frequência. Os receptores dela não repassam nada até um dos dois sair.

## Se algo der errado

> [!PROBLEM] O programa para e diz que nenhum Redstone Interface com esse nome está ligado a esta máquina.
> Ligue a interface à máquina com cabo e confira o nome dela na janela.

> [!PROBLEM] Signals.Send dá não.
> A lente da interface não está virada para um transmissor, ou a interface não está energizada.

> [!SEE] jsc:redstone_interfaces, jsc:redstone_library
