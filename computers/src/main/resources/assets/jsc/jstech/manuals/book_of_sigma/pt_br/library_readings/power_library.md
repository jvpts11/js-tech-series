---
section: library_readings
order: 455
---
# Power: o consumo, a fonte e o UPS

Um programa pode ler o que a máquina dele puxa da rede elétrica, a fonte de alimentação que a alimenta e o UPS que a sustenta. Você lê tudo isso pelo Power, um tipo embutido do System.Machine, do mesmo jeito que lê o Computer. O Power veio com o Sigma# 3.

Table: Membros do Power

| Membro | O que dá |
| --- | --- |
| `Power.DrawWatts` | O que a máquina puxa da rede agora, em watts (W): o que as peças consomem, mais o que a fonte perde. Dá 0 enquanto a máquina está desligada. |
| `Power.Supply` | A fonte de alimentação, como um registro SupplyInfo, ou null quando a máquina não tem nenhuma. |
| `Power.Ups` | O UPS que sustenta a máquina, como um registro UpsInfo, ou null quando nenhum UPS a sustenta. |

Table: Registros do Power

| Registro | Campos | O que cada campo guarda |
| --- | --- | --- |
| `SupplyInfo` | `SupplyInfo.Name, SupplyInfo.RatingWatts, SupplyInfo.Units, SupplyInfo.UnitsFailed, SupplyInfo.Degraded, SupplyInfo.Failed` | Name: o nome da fonte. RatingWatts: o máximo que ela fornece, em watts. Units: quantas unidades ela tem, mais de uma numa fonte redundante. UnitsFailed: quantas delas falharam. Degraded: sim quando uma unidade está gasta ou falhou e as outras seguem. Failed: sim quando todas as unidades falharam. |
| `UpsInfo` | `UpsInfo.Name, UpsInfo.OnBattery, UpsInfo.Bypass, UpsInfo.ChargePercent, UpsInfo.MinutesLeft` | Name: o nome do UPS. OnBattery: sim quando a rede caiu e a bateria sustenta a máquina. Bypass: sim quando a máquina consome mais do que o UPS sustenta, e por isso roda direto da rede, sem proteção. ChargePercent: o quanto a bateria está cheia, de 0 a 100. MinutesLeft: quanto tempo a bateria dura com esse consumo. |

Um servidor num rack lê a própria fonte e o UPS do rack, que sustenta todos os servidores do rack.

```sgs Power.sgs
UpsInfo ups = Power.Ups;
if (ups != null && ups.OnBattery && ups.MinutesLeft < 5) {
    Console.PrintLine("Saving: the battery is almost empty");
}
```

> [!DEFINE] UPS
> Fonte de alimentação ininterrupta: uma bateria que sustenta uma máquina quando a rede elétrica cai.

> [!DEFINE] Fonte redundante
> Uma fonte de alimentação feita de várias unidades, em que as outras seguem quando uma falha.

## Se algo der errado

> [!PROBLEM] Power.Ups dá null.
> Nenhum UPS sustenta a máquina. Um bloco de UPS fica encostado no computador; um rack leva um Rack UPS.

> [!PROBLEM] Power.DrawWatts dá 0.
> A máquina está desligada, ou ainda não tem uma montagem.

> [!SEE] jsc:power_supplies, jsc:upses, jsc:computer_info
