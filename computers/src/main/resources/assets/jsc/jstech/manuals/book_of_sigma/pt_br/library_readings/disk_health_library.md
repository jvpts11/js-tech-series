---
section: library_readings
order: 458
---
# DiskHealth: o que aconteceu com os discos

Um programa pode ler a saúde das unidades da máquina dele como o Defrag a lê: se um disco morreu, o quanto está danificado, os setores ruins, os itens que a corrupção guarda e quando uma passada do Defrag terminou nele pela última vez. Você lê isso pelo DiskHealth, um tipo embutido do System.Machine. O DiskHealth veio com o Sigma# 3.

Table: Membros do DiskHealth

| Membro | O que dá |
| --- | --- |
| `DiskHealth.Of(drive)` | A saúde do disco naquela unidade, indicada pela letra, como C, ou null quando não há disco ali. |
| `DiskHealth.All()` | A saúde de cada unidade com um disco nela, em ordem de letra. |
| `DiskHealth.Drive` | A letra da unidade. |
| `DiskHealth.Dead` | Sim quando o disco morreu e não guarda mais nada. |
| `DiskHealth.DamagePercent` | O quanto o disco está danificado, de 0 a 100. |
| `DiskHealth.BadSectorsPercent` | O quanto do disco os setores ruins tomaram, de 0 a 100. |
| `DiskHealth.CorruptedItems` | Quantos itens e arquivos a corrupção guarda no disco. |
| `DiskHealth.LastDefragTick` | O tick do mundo em que uma passada do Defrag terminou no disco pela última vez, que você compara com Time.Tick, ou -1 quando nenhuma terminou. |

```sgs Health.sgs
foreach (DiskHealth disk in DiskHealth.All()) {
    if (disk.CorruptedItems > 0) {
        Console.PrintLine(disk.Drive + ": run the Defrag");
    }
}
```

> [!DEFINE] Setor ruim
> Uma parte de um disco que não guarda mais o que se grava nela. Uma passada do Defrag conserta alguns deles.

## Se algo der errado

> [!PROBLEM] DiskHealth.Of dá null.
> Não há disco naquela unidade. Confira a letra com Computer.Disks().

> [!PROBLEM] DiskHealth.LastDefragTick dá -1.
> Nenhuma passada do Defrag terminou naquele disco nesta máquina.

> [!SEE] jsc:corruption_and_the_defrag, jsc:disks, jsc:computer_info
