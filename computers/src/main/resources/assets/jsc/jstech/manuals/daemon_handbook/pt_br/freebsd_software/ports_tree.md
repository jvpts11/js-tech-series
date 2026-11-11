---
section: freebsd_software
order: 337
---
# A árvore de ports: portsnap

O FreeBSD também compila programas a partir do código-fonte, no seu próprio computador. As receitas deles ficam na árvore de ports, uma pasta no disco do sistema. Você baixa a árvore com o portsnap, e o Mirror a oferece como uma única cópia.

> [!DEFINE] Árvore de ports
> A pasta de receitas, uma por programa, que compila programas neste computador.

> [!DEFINE] Port
> A receita de um programa na árvore de ports.

> [!DEFINE] Snapshot
> A cópia da árvore de ports que o Mirror oferece, baixada de uma vez.

A árvore é feita de arquivos reais. Cada port tem uma pasta em /usr/ports/<category>/<name>/ com três arquivos. Makefile guarda a receita, distinfo guarda a soma de verificação e o tamanho do código-fonte, e pkg-descr guarda a descrição em uma linha.

## As palavras do portsnap

Table: As palavras do portsnap

| Palavra | O que faz |
| --- | --- |
| `fetch` | Traz o snapshot do Mirror sem montar uma árvore. |
| `extract` | Monta a árvore do zero a partir do snapshot, substituindo qualquer árvore existente. |
| `update` | Atualiza uma árvore já montada. |
| `auto` | Baixa e, em seguida, extrai ou atualiza, o que o computador precisar. |

```command
id: jsc:command/freebsd/portsnap
```

1. Na primeira vez, digite portsnap fetch extract.
2. Depois, digite portsnap fetch update.

As palavras se combinam na ordem em que você as digita. Enquanto roda, o portsnap imprime o progresso, e a última linha informa a quantidade de ports e o espaço em disco que ocupam.

> [!WARNING] portsnap extract substitui a árvore de ports inteira, e as pastas de compilação dentro dela também são apagadas.

Table: Onde alguns programas ficam na árvore

| Programa | Categoria |
| --- | --- |
| screenfetch | `sysutils` |
| emacs | `editors` |
| minesweeper, solitaire, snake | `games` |
| scc, sgsc, sigma | `lang` |
| knot, knothub | `devel` |
| kde_plasma | `x11` |
| gnome, cinnamon | `x11` |
| cde | `x11` |

## Mensagens do portsnap

```table
{"caption": "Mensagens do portsnap", "heads": ["Mensagem", "O que significa"], "rows": [[{"literal": "portsnap: this computer has no system disk"}, {"words": "O computador não tem disco para guardar a árvore."}], [{"literal": "Looking up the Mirror for the ports tree... none found."}, {"words": "O fetch não encontrou nenhum Mirror, então nenhum snapshot chega; em seguida ele imprime \"Nenhum mirror restante, desistindo.\""}], [{"literal": "No snapshot available.  Try running portsnap fetch"}, {"words": "extract e update exigem um snapshot primeiro."}], [{"literal": "/usr/ports was not created by portsnap."}, {"words": "update só funciona em uma árvore que o portsnap montou, então execute portsnap extract uma vez."}], [{"literal": "portsnap: No space left on device"}, {"words": "O disco está cheio."}]]}
```

## Se algo der errado

> [!PROBLEM] portsnap diz que não encontrou nenhum para o Mirror.
> Conecte a rede a um Mainframe que rode o serviço Mirror.

> [!PROBLEM] portsnap diz que não há snapshot disponível.
> Digite primeiro portsnap fetch, depois extract ou update.

> [!PROBLEM] portsnap diz que /usr/ports não foi criado pelo portsnap.
> Digite portsnap extract uma vez. Depois disso, update funciona.

> [!PROBLEM] portsnap diz que não há mais espaço no dispositivo.
> Libere algum espaço em disco.

> [!SEE] jsc:ports_build, jsc:pkg, jsc:disks
