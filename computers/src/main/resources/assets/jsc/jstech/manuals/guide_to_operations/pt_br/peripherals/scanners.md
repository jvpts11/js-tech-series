---
section: peripherals
order: 74
icon: jsc:scanner
covers_family: jsc:scanners
---
# Scanners

Um scanner lê o que está sobre o seu vidro para um arquivo, no computador na outra ponta do seu cabo de periférico. Há um scanner de mesa em cada era. O item fica como estava: escanear só o lê.

```table
{"caption": "Os scanners", "heads": ["Scanner", "Era", "Uma página leva"], "rows": [[{"words": "Pakard ScanJot"}, {"words": "Vintage"}, {"amount": 8, "unit": "s"}], [{"words": "Epsilon Perfecta 1240U"}, {"words": "Legacy"}, {"amount": 4, "unit": "s"}], [{"words": "Pakard ScanJot G4050"}, {"words": "Transition"}, {"amount": 2, "unit": "s"}], [{"words": "Epsilon Perfecta V600"}, {"words": "Standard"}, {"amount": 1, "unit": "s"}], [{"words": "Epsilon Perfecta V39"}, {"words": "Advanced"}, {"words": "0,5 s"}]]}
```

Table: O que ele lê

| Sobre o vidro | Arquivo | Abre no |
| --- | --- | --- |
| Livro escrito | `.book` | Editor |
| Livro e pena | .book, sem assinatura | Editor |
| Livro encantado | `.ench` | Editor |
| Mapa | `.map` | Paint |
| Papel Impresso | .txt, ou .pix para uma imagem | Editor, ou Paint |

1. Clique com o botão direito no scanner segurando o item para pô-lo sobre o vidro.
2. Abra o scanner e aperte ESCANEAR. A tampa se abre e ele lê página por página.
3. O arquivo fica na pasta de escaneados do sistema do computador.

> [!DEFINE] Pasta de escaneados
> Onde um scanner põe os seus arquivos: Users/Public/Documents/Scans no Frames e no MC-DOS, e Documents/Scans na pasta pessoal no Linux, no FreeBSD e no UNIX System V.

Um .book começa com o seu título e o seu autor, e cada página sob o seu cabeçalho. Edite-o como quiser, e a Impressora de Grande Escala o imprime ([](jsc:large_scale_printer)); um sem assinatura volta impresso como livro e pena. Um .ench e um .map ficam selados ao mundo que os escaneou: alterados, ou levados para outro mundo, eles não imprimem mais. O Paint abre um .map para ver, e o salva como .pix.

## Se algo der errado

> [!PROBLEM] O scanner diz VIDRO VAZIO.
> Coloque um livro, um mapa ou uma folha impressa sobre o vidro.

> [!PROBLEM] O scanner diz SEM COMPUTADOR.
> Ligue-o a um computador com um cabo de periférico ([](jsc:peripheral_cables)).

> [!PROBLEM] O scanner diz SEM SISTEMA.
> O computador não tem sistema para guardar o arquivo: instale um.

> [!PROBLEM] O scanner diz DISCO CHEIO.
> Libere espaço no disco do computador.

> [!SEE] jsc:printers, jsc:large_scale_printer
