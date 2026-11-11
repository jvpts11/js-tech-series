---
section: examples_devices
order: 507
---
# Som

Sound faz um bipe ou uma melodia pela máquina, e toca uma gravação dos seus discos. Este exemplo usa as quatro chamadas de som, e um alto-falante encontrado pelo nome.

Um bipe sai do alto-falante dentro do gabinete, e cada novo bipe corta o anterior. Uma melodia toca pela placa de som, que toca todas as notas de um acorde.

Uma gravação dos discos da máquina sai pelos monitores e alto-falantes, conforme o seu sistema indicar.

## O programa

```sgs Sound.sgs
using System.*;
using System.IO.*;
using System.Sound.*;
namespace Corpus;
class SoundCorpus {
    static void Main() {
        Sound.Beep(880, 200);
        bool tune = Sound.Tones("C4:250 E4 G4 C4+E4+G4:500");
        bool song = Sound.Play("Users/Public/Music/intro.ogg");
        Sound.Stop();
        Speaker desk = Speaker.Named("Desk left");
        if (desk != null) {
            bool alone = desk.Play("Users/Public/Music/alarm.wav");
            Console.PrintLine(desk.Name + alone);
        }
        Console.PrintLine("" + tune + song);
    }
}
```

## O que as linhas fazem

Table: Linhas do programa

| Linha | O que faz |
| --- | --- |
| `Sound.Beep(880, 200)` | Toca um bipe em 880 hertz por 200 milissegundos. |
| `Sound.Tones(...)` | Toca uma melodia: C4 por 250 milissegundos, depois E4 e G4, e depois o acorde C4, E4 e G4 por 500 milissegundos. Responde true quando a melodia toca. |
| `Sound.Play(...)` | Toca uma gravação dos discos da máquina. Responde false quando o arquivo não pode ser tocado. |
| `Sound.Stop()` | Para o som do programa. |
| `Speaker.Named("Desk left")` | Encontra um alto-falante ligado à máquina pelo nome. Dá null quando nenhum tem esse nome. |
| `desk.Play(...)` | Toca uma gravação somente por esse alto-falante, e responde true quando ela toca. |
| `desk.Name` | O nome do alto-falante. |

> [!DEFINE] Bipe
> Um tom simples em uma altura, por um tempo definido.

> [!DEFINE] Altura
> O quão agudo é um som, medido em hertz. 440 hertz é a nota A4.

> [!DEFINE] Nota
> Uma altura com nome, como C4, que uma melodia pode tocar.

> [!DEFINE] Combinação de teclas
> Várias notas tocadas juntas, escritas com sinais de mais, como C4+E4+G4.

> [!DEFINE] Alto-falante
> Um bloco que faz som para a máquina.

> [!DEFINE] Gravação
> Um arquivo de música no disco da máquina, como um arquivo .ogg ou .wav.

## Se algo der errado

> [!PROBLEM] Tones responde false, e nenhuma melodia toca.
> Confira se a máquina tem um dispositivo de som e se o volume está acima de 0.

> [!PROBLEM] Uma melodia tem uma palavra que não é uma nota, e o programa para com uma mensagem que termina em "não é uma nota".
> Escreva notas como C4, uma altura em hertz, ou R para uma pausa.

> [!PROBLEM] Um acorde no alto-falante dentro do gabinete soa só pela primeira nota.
> Toque a melodia pela placa de som, que toca o acorde inteiro.

> [!PROBLEM] Speaker.Named dá null, porque nenhum alto-falante tem esse nome.
> Teste contra null antes de usar o alto-falante, e confira o seu nome.

> [!PROBLEM] Play responde false para um arquivo que está ausente ou que não pode ser tocado.
> Confira o caminho, escrito com barras, e o tipo do arquivo.

> [!PROBLEM] Um bipe pede uma altura abaixo de 20 ou acima de 20.000 hertz.
> Um bipe é ajustado para o limite mais próximo, então dê uma altura de 20 a 20.000 hertz.

> [!SEE] jsc:soundfoundry, jsc:sigma_library
