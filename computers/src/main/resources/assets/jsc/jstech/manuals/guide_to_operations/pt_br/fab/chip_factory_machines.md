---
section: fab
order: 41
icon: jsc:ion_implanter_analyzer_magnet
covers: [jsc:plasma_etcher_etch_chamber, jsc:ion_implanter_end_station, jsc:ion_implanter_analyzer_magnet, jsc:deposition_chamber_sputter_chamber, jsc:packaging_line_mould_press, jsc:packaging_line_wire_bonder]
---
# A fábrica de chips

Um chip é feito camada por camada no seu wafer: uma Estação de Litografia expõe cada camada ([](jsc:lithography_stations)), e você leva o wafer por estas máquinas para o resto.

O Gravador a Plasma grava a camada exposta, com um líquido e dois gases. O Implantador de Íons dopa a camada. A Câmara de Deposição deposita metal sobre ela, e a camada magnética dos pratos e do filme de disquete, o cromo de uma máscara virgem e os eletrodos de ouro dos cristais osciladores. A Linha de Encapsulamento corta um wafer pronto nos seus dies e encapsula cada um: em cerâmica na Vintage, em epóxi com pinos depois, sobre esferas de solda a partir da Transition.

```table
{"caption": "O tamanho de cada máquina, a sua etapa e o seu consumo", "heads": ["Máquina", "Tamanho", "Etapa", "Consumo"], "rows": [[{"words": "Gravador a Plasma"}, {"words": "2 × 2 × 2"}, {"amount": 10, "unit": "s"}, {"amount": 3000, "unit": "W"}], [{"words": "Implantador de Íons"}, {"words": "3 × 2 × 2"}, {"amount": 10, "unit": "s"}, {"amount": 4000, "unit": "W"}], [{"words": "Câmara de Deposição"}, {"words": "2 × 2 × 2"}, {"amount": 10, "unit": "s"}, {"amount": 3000, "unit": "W"}], [{"words": "Linha de Encapsulamento"}, {"words": "3 × 2 × 2"}, {"amount": 20, "unit": "s"}, {"amount": 2000, "unit": "W"}]]}
```

> [!SEE] jsc:building_chain_machines, jsc:lithography_stations
