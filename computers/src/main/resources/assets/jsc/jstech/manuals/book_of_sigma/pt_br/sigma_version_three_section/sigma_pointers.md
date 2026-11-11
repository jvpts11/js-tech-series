---
section: sigma_version_three_section
order: 436
---
# Ponteiros e memória crua

Um ponteiro guarda o endereço de um valor na memória crua: bytes que o programa pediu, sem nenhum objeto em volta. int\* é um ponteiro para um int. Um ponteiro aponta para um int, um long, um byte, um struct que só guarda valores, ou para nada em particular com void\*. O tipo byte, um número inteiro de 0 a 255, vem com os ponteiros.

No Sigma#, os ponteiros só são guardados em código unsafe: um método escrito como unsafe, ou um bloco escrito unsafe { ... }. Um arquivo Sigma é unsafe por inteiro, como o C era.

Table: O que um ponteiro faz

| Escrito como | O que significa |
| --- | --- |
| `&x` | O endereço de x: uma variável local, um parâmetro, ou um lugar que um ponteiro alcança. A partir daí, x vive na memória crua até o seu método retornar. |
| `*p` | O valor para que p aponta, para ler ou para escrever. |
| `p->Count` | Um campo do struct para que p aponta. |
| `p[i]` | O valor i lugares depois daquele para que p aponta. |
| `p + n, p - n, p++` | O ponteiro movido n valores do que ele aponta. |
| `p - q` | Quantos valores ficam entre dois ponteiros, como um long. |
| `p == q, p < q` | Compara dois endereços. null não aponta para nada. |
| `(byte*) p` | O mesmo endereço, lido como outro ponteiro. |
| `stackalloc int[n]` | Espaço para n valores, liberado quando o método retorna. |
| `sizeof(Cell)` | Quantos bytes um valor do tipo ocupa. |

Table: As chamadas da memória crua

| Sigma# | Sigma | O que faz |
| --- | --- | --- |
| `Memory.Alloc(bytes)` | `malloc(bytes)` | Um bloco novo com esse número de bytes. |
| `Memory.AllocZeroed(count, size)` | `calloc(count, size)` | Um bloco novo para count valores desse tamanho. |
| `Memory.Resize(p, bytes)` | `realloc(p, bytes)` | Um bloco do novo tamanho, com o que cabe do antigo, que é liberado. |
| `Memory.Free(p)` | `free(p)` | Devolve o bloco. Liberar null não faz nada. |
| `Memory.Copy(dest, src, bytes)` | `memcpy(dest, src, bytes)` | Copia bytes de src para dest. |
| `Memory.Fill(p, value, bytes)` | `memset(p, value, bytes)` | Põe o valor em todo byte. |
| `Memory.Compare(a, b, bytes)` | `memcmp(a, b, bytes)` | Abaixo de zero, zero ou acima de zero, pelo primeiro byte que difere. |

Todo bloco de memória crua começa com zeros e sabe o seu tamanho. Uma leitura ou uma escrita fora de todo bloco vivo, depois do fim de um, num já liberado, ou através de null, é uma violação. No Sigma# ela lança uma AccessViolationException. No Sigma o programa morre com as próprias palavras do seu sistema, como Falha de segmentação (imagem do núcleo gravada) no Linux e nos BSDs.

Os bytes contam exatamente na memória do programa. Uma leitura ou uma escrita através de um ponteiro custa uma instrução, Alloc e Free custam o que custam new e dispose, e Copy, Fill e Compare custam mais uma a cada 64 bytes.

```sg Cells.sg
namespace Tools;

struct Cell
{
    public int Value;
    public Cell* Next;
}

class Cells
{
    static void Main()
    {
        Cell* first = null;
        for (int i = 1; i <= 3; i++)
        {
            Cell* made = calloc(1, sizeof(Cell));
            made->Value = i;
            made->Next = first;
            first = made;
        }
        int total = 0;
        for (Cell* at = first; at != null; at = at->Next)
        {
            total += at->Value;
        }
        printf("%d\n", total);
    }
}
```

Este programa imprime "6". Um struct com um campo ponteiro é escrito em Sigma, que é unsafe por inteiro; no Sigma#, um ponteiro vive nas variáveis locais e nos parâmetros de código unsafe.

> [!DEFINE] Ponteiro
> Um valor que guarda o endereço de outro valor na memória crua.

> [!DEFINE] Memória crua
> Bytes que um programa pediu, sem nenhum objeto em volta, lidos e escritos através de ponteiros.

## Se algo der errado

> [!PROBLEM] O compilador diz que um ponteiro só é guardado em código unsafe.
> Escreva unsafe no método, ou ponha as linhas num bloco unsafe.

> [!PROBLEM] O programa para com Falha de segmentação, ou lança uma AccessViolationException.
> Um ponteiro leu depois do fim do seu bloco, ou num bloco já liberado. Verifique o índice e o tamanho que você pediu, e libere cada bloco uma vez, depois do seu último uso.

> [!SEE] jsc:sigma_exceptions, jsc:sigma_dispose
