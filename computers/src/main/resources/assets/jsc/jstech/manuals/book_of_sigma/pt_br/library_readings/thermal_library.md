---
section: library_readings
order: 456
---
# Thermal: o quanto a máquina esquenta

Um programa pode ler o calor da máquina dele: o quanto ela está quente, o calor que as peças geram, o calor que o resfriamento tira e quanto do clock o calor deixa para ela. Você lê isso pelo Thermal, um tipo embutido do System.Machine. O Thermal veio com o Sigma# 3.

Table: Membros do Thermal

| Membro | O que dá |
| --- | --- |
| `Thermal.Known` | Sim quando a máquina tem uma temperatura própria. Quando dá não, cada leitura abaixo dá o seu valor de repouso. |
| `Thermal.Celsius` | O quanto a máquina está quente agora, em graus Celsius (°C). |
| `Thermal.AmbientCelsius` | O quanto o ambiente em volta está quente, em °C. |
| `Thermal.HeatWatts` | O calor que as peças geram, em watts. |
| `Thermal.BudgetWatts` | O orçamento térmico: o calor que o resfriamento tira, em watts. |
| `Thermal.ClockPercent` | A parte do clock que o calor deixa para ela, de 25 a 100. Os programas rodam nessa parte. |
| `Thermal.Throttled` | Sim enquanto a máquina reduz o clock para se segurar em 90 °C. |
| `Thermal.LastTripCelsius` | O quanto ela estava quente quando o calor a desligou pela última vez, em °C, ou 0 quando o calor nunca a desligou. |

Uma máquina cujas peças geram mais calor do que o orçamento dela esquenta até reduzir o clock. Um servidor num rack lê o calor do rack, que todos os servidores dele dividem.

```sgs Heat.sgs
if (Thermal.Throttled) {
    Console.PrintLine("Too hot: running at " + Thermal.ClockPercent + "%");
}
```

> [!DEFINE] Orçamento térmico
> O calor que o resfriamento de uma máquina tira, em watts.

> [!DEFINE] Throttling
> Reduzir o clock para gerar menos calor, para a máquina ficar numa temperatura segura.

## Se algo der errado

> [!PROBLEM] Thermal.Known dá não.
> A máquina não tem temperatura própria; algo de fora a resfria.

> [!PROBLEM] Thermal.Throttled continua sim.
> As peças geram mais calor do que o resfriamento tira. Coloque um cooler melhor ou mais ventoinhas.

> [!SEE] jsc:cooling, jsc:rack_cooling_and_heat, jsc:computer_info
