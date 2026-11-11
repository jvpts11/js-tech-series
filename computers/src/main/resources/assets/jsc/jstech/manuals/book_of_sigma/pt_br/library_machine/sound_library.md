---
section: library_machine
order: 453
---
# Sound: bipes, melodias, gravações e alto-falantes

Uma máquina emite som pelo alto-falante do seu gabinete, e pelas placas de som que você instala. Sound é o tipo da máquina como um todo, e Speaker é um alto-falante que você pode nomear. O hardware é explicado em [](jsc:sound_cards) e [](jsc:speakers).

> [!DEFINE] Hertz
> Quantas vezes uma onda sonora se repete a cada segundo. Um número maior é um tom mais agudo.

> [!DEFINE] Voz
> Um som que a placa de som pode tocar de uma vez.

> [!DEFINE] Gravação
> Um arquivo de música no disco da máquina.

> [!DEFINE] Nota
> Uma altura nomeada: uma letra de A a G, um sustenido ou bemol opcional, e uma oitava de 0 a 8.

Table: Membros de Sound

| Membro | O que faz |
| --- | --- |
| `Sound.Beep(hertz, millis)` | Emite um bipe nessa altura, por essa quantidade de milissegundos (ms). |
| `Sound.Tones(tune)` | Toca uma melodia de notas. Retorna true quando toca. |
| `Sound.Play(path)` | Toca uma gravação do disco da máquina. Retorna true quando toca. |
| `Sound.Stop()` | Para as gravações que os programas estão tocando. |

Table: Membros de Speaker

| Membro | O que faz |
| --- | --- |
| `Speaker.Named(name)` | O alto-falante com esse nome, vinculado a esta máquina, ou null quando não há nenhum. |
| `Speaker.Name` | O nome do alto-falante. |
| `Speaker.Play(path)` | Toca uma gravação somente por esse alto-falante. Retorna true quando toca. |

O bipe sai pelo alto-falante na caixa da máquina. Um novo bipe corta o anterior. Sua altura fica entre 20 e 20.000 hertz (Hz), e sua duração entre 1 e 60.000 milissegundos (ms).

## Melodias

Uma melodia é escrita como itens separados por espaços ou vírgulas. Uma melodia como C4:250 E4 G4 C4+E4+G4:500 toca três notas e depois um acorde.

Table: Itens de uma melodia

| Item | O que significa |
| --- | --- |
| `C4` | Uma nota: uma letra de A a G, depois a sua oitava de 0 a 8. |
| `F#3, Bb5` | Um sustenido com #, ou um bemol com b, em uma nota. |
| `440` | Uma altura em hertz, acima de 0 e até 20.000. |
| `R` | Uma pausa: silêncio pela duração do item. |
| `C4+E4+G4` | Notas tocadas juntas como um acorde, até 16 notas. |
| `:250` | Uma duração em milissegundos, de 1 a 60.000, depois do item. |

Um item sem duração dura o mesmo que o item anterior. O primeiro item dura 250 milissegundos. Uma melodia tem no máximo 1.024 itens.

Cada nota de um acorde ocupa uma voz da placa de som. Uma máquina sem placa de som toca apenas a primeira nota de cada acorde, pelo alto-falante do seu gabinete.

## Gravações

Play lê uma gravação dos discos da máquina. Ele retorna false quando o arquivo não é uma gravação, quando a máquina não toca gravações, quando o volume é 0, ou quando o servidor não mantém a gravação. Um novo Play substitui a gravação que os programas estavam tocando.

Um som sai no volume da máquina. Um som que não encontra voz livre toma a voz do som mais antigo.

Um alto-falante é encontrado pelo seu nome exato. Um alto-falante sem nome não pode ser encontrado desse jeito.

## Se algo der errado

> [!PROBLEM] O programa para e diz que o item não é uma nota.
> Escreva cada item como uma nota, uma altura em hertz, ou R para uma pausa.

> [!PROBLEM] Sound.Tones retorna false.
> O som da máquina está desligado, ou a máquina não emite som. Confira se o som dela está ligado.

> [!PROBLEM] Sound.Play retorna false.
> Confira se o arquivo é uma gravação, se o volume está acima de 0, e se o servidor mantém a gravação.

> [!PROBLEM] Speaker.Named retorna null.
> Nenhum alto-falante vinculado a esta máquina tem esse nome exato. Confira o nome, porque ele precisa coincidir exatamente.

> [!SEE] jsc:sound_cards, jsc:speakers
