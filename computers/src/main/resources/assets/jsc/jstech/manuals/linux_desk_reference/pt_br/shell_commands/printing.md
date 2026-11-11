---
section: shell_commands
order: 257
---
# Impressão pelo terminal

Você imprime pelo terminal com dois comandos. O comando lp envia arquivos para uma impressora, e o comando lpstat mostra as requisições de impressão e as impressoras.

> [!DEFINE] Destino
> A impressora para a qual vai uma tarefa de impressão.

> [!DEFINE] Cópias
> Quantas impressões de cada arquivo uma tarefa faz.

Informe o destino com -d, ou omita-o para usar o padrão. O número de cópias é informado com -n.

```command
id: jsc:command/linux/lp
```

```command
id: jsc:command/linux/lpstat
```

## Se algo der errado

> [!PROBLEM] O comando lp diz que não há destino padrão.
> Informe uma impressora com -d e confira o nome dela com lpstat.

> [!PROBLEM] O comando lp diz que o destino é inexistente.
> Digite o nome exatamente como o lpstat o mostra.

> [!PROBLEM] O comando lp diz que não há arquivos para imprimir.
> Informe pelo menos um nome de arquivo depois do comando.

> [!SEE] jsc:shell_commands, jsc:pipes_and_variables
