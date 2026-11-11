---
section: library_toolkit
order: 466
---
# Calendar e Stopwatch

O Calendar lê o calendário do mundo onde a máquina está, e um Stopwatch cronometra um trecho do seu programa. Os dois ficam no System.Utils e vieram com o Sigma# 3.

Table: Membros do Calendar

| Membro | O que dá |
| --- | --- |
| `Calendar.Hour, Calendar.Minute` | A hora que o relógio mostra. Um dia começa às seis da manhã. |
| `Calendar.Day, Calendar.Week, Calendar.Year` | Qual dia, semana e ano é, contados a partir de 0. |
| `Calendar.DayOfWeek, Calendar.DayOfSeason` | O dia dentro da semana e dentro da estação, a partir de 0. |
| `Calendar.Season` | spring, summer, autumn ou winter. |

Table: Membros do Stopwatch

| Membro | O que dá |
| --- | --- |
| `Stopwatch.Start()` | Um cronômetro iniciado agora. |
| `Stopwatch.Ticks` | Os ticks do mundo que passaram desde que ele começou. |
| `Stopwatch.Credits` | Os créditos que o seu programa gastou desde que ele começou. |
| `Stopwatch.Restart()` | Começa de novo a partir de agora. |

```sgs Timing.sgs
Stopwatch watch = Stopwatch.Start();
Report();
Console.PrintLine("The report cost " + watch.Credits + " credits");
```

> [!DEFINE] Crédito
> Uma unidade do trabalho de um programa: uma instrução custa um, e perguntar algo à máquina custa mais.

## Se algo der errado

> [!PROBLEM] Stopwatch.Ticks fica em 0 ticks.
> O seu programa ainda não deixou um tick passar: o relógio anda entre um tick e outro, não dentro de um.

> [!SEE] jsc:numbers_and_time, jsc:credits_per_tick
