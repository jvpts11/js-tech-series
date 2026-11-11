---
section: fab
order: 46
icon: jsc:large_scale_printer_print_unit
covers: [jsc:large_scale_printer_paper_feeder, jsc:large_scale_printer_perfect_binder, jsc:large_scale_printer_print_unit, jsc:large_scale_printer_inking_unit]
---
# A Impressora de Grande Escala

A Impressora de Grande Escala imprime livros em quantidade, a única máquina que os imprime. É uma máquina do Legacy, de 4 blocos de largura, 3 de altura e 3 de profundidade, feita do Carcaça de Máquina e de quatro blocos de trabalho: o Alimentador de Papel, a Unidade de Impressão, a Unidade de Tintagem e a Encadernadora. Ela puxa a sua energia pela Entrada de Energia nas costas.

Você põe o papel no alimentador à mão, pela sua Entrada de Itens a partir de um barramento, ou de um funil. Um livro gasta 3 papéis e nada mais, e 2 segundos. Um cabo de periférico de um computador até a sua Porta Periférica faz dela uma impressora desse computador: ela aparece na caixa Imprimir do sistema do computador, onde um arquivo .book ou .ench vai para ela, e a ajuda imprime nela os seus próprios livros.

Um livro escrito sai como cópia do que foi escaneado, e um livro e pena como livro e pena. Um livro encantado sai com todo encantamento no nível em que foi escaneado. Um botão na sua janela manda o que ela imprime para a rede do computador no seu cabo, ou para um baú ao lado da sua Saída de Itens; sem nenhum dos dois, os livros esperam na máquina. Ela guarda 16 trabalhos, cada um de até 64 cópias, e a sua janela pausa a impressão e cancela o trabalho que está imprimindo.

```table
{"caption": "O tamanho de cada máquina, o seu passo e o seu consumo", "heads": ["Máquina", "Tamanho", "Passo", "Consumo"], "rows": [[{"words": "Impressora de Grande Escala"}, {"words": "4 × 3 × 3"}, {"amount": 2, "unit": "s"}, {"amount": 2000, "unit": "W"}]]}
```

## Se algo der errado

> [!PROBLEM] Nenhuma Impressora de Grande Escala está ligada a este computador.
> Leve um cabo de periférico do computador até a Porta Periférica da máquina, e forme a máquina.

> [!PROBLEM] Este servidor não deixa imprimir livros encantados.
> A configuração printing.enchanted_books do servidor está desligada: livros encantados não são impressos.

> [!PROBLEM] O arquivo não é um livro encantado que um scanner deste mundo leu.
> Um arquivo .ench só imprime como foi escaneado neste mundo; um que foi alterado, ou vem de outro mundo, não imprime.

> [!PROBLEM] O seu alimentador precisa de 3 folhas de papel por livro.
> Coloque papel no alimentador. Um trabalho que não imprime nada por muito tempo também espera energia, ou espaço para os seus livros: a rede ou o baú ao lado da sua saída.

> [!SEE] jsc:building_chain_machines, jsc:printers, jsc:scanners
