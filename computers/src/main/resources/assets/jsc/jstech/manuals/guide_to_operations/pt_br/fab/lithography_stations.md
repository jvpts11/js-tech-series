---
section: fab
order: 42
icon: jsc:lithography_wafer_stage
covers: [jsc:lithography_wafer_stage, jsc:lithography_glass_optics, jsc:lithography_mercury_lamp, jsc:lithography_krf_laser, jsc:lithography_arf_laser, jsc:lithography_fluoride_optics, jsc:lithography_euv_source, jsc:lithography_mirror_optics]
---
# As Estações de Litografia

Uma Estação de Litografia expõe a camada de um wafer pela sua máscara. Cada era tem a sua, o mesmo corpo iluminado do jeito da sua era: a lâmpada de mercúrio da Vintage, o laser de fluoreto de criptônio da Legacy, o laser de fluoreto de argônio com imersão da Estação de Litografia ArF, que faz tanto as receitas da Transition quanto as da Standard, e a fonte de ultravioleta extremo da Advanced com os seus espelhos. Você dá a um laser o seu gás pela Entrada de Gás, e a toda estação o seu líquido pela Entrada de Líquido.

Da Legacy em diante, uma estação só trabalha ligada a um computador: passe um [cabo periférico](jsc:peripheral_cables) da sua Porta Periférica até um computador da era anterior à da receita, ou de uma posterior, ligado e em funcionamento. O computador roda a exposição como o seu processo litho e ocupa memória para ela enquanto isso, o que você vê entre os seus processos. Um computador duas eras mais novo do que o pedido expõe na metade do tempo.

```table
{"caption": "O que as receitas de cada era pedem", "heads": ["Receitas da", "Computador", "Memória", "Consumo"], "rows": [[{"words": "Vintage"}, {"words": "nenhum"}, {"words": "nenhuma"}, {"amount": 2000, "unit": "W"}], [{"words": "Legacy"}, {"words": "Vintage ou posterior"}, {"amount": 2, "unit": "MB"}, {"amount": 3000, "unit": "W"}], [{"words": "Transition"}, {"words": "Legacy ou posterior"}, {"amount": 32, "unit": "MB"}, {"amount": 4000, "unit": "W"}], [{"words": "Standard"}, {"words": "Transition ou posterior"}, {"amount": 256, "unit": "MB"}, {"amount": 6000, "unit": "W"}], [{"words": "Advanced"}, {"words": "Standard ou posterior"}, {"amount": 2048, "unit": "MB"}, {"amount": 10000, "unit": "W"}]]}
```

```table
{"caption": "O tamanho de cada estação e a sua etapa", "heads": ["Estação", "Tamanho", "Etapa"], "rows": [[{"words": "Estação de Litografia Vintage"}, {"words": "3 × 3 × 2"}, {"amount": 20, "unit": "s"}], [{"words": "Estação de Litografia Legacy"}, {"words": "3 × 3 × 2"}, {"amount": 20, "unit": "s"}], [{"words": "Estação de Litografia ArF"}, {"words": "3 × 3 × 2"}, {"amount": 20, "unit": "s"}], [{"words": "Estação de Litografia Advanced"}, {"words": "3 × 3 × 2"}, {"amount": 20, "unit": "s"}]]}
```

## Se algo der errado

> [!PROBLEM] A estação está formada mas não expõe nada.
> O seu computador está desligado, é de uma era mais antiga do que a receita pede, não está ligado à sua Porta Periférica, ou não tem a memória que a exposição ocupa.

> [!SEE] jsc:building_chain_machines, jsc:chip_factory_machines
