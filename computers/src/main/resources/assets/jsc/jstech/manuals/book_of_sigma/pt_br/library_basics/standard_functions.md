---
section: library_basics
order: 438
---
# Os nomes antigos: printf, rand e os demais

Programas escritos no estilo antigo chamam nomes simples, como printf, rand e strlen. Sigma e Sigma# ainda os aceitam. Cada nome simples é uma chamada a um membro da biblioteca, então puts(s) faz o mesmo que Console.PrintLine(s).

printf é da versão 1, as chamadas da memória crua são da versão 3, e todos os outros nomes desta página são da versão 2, então uma compilação mais antiga os recusa. Cada linha da tabela indica o membro que o nome antigo chama, para que você possa ler a entrada desse membro neste livro.

Table: Impressão e leitura

| Nome antigo | Chamada da biblioteca |
| --- | --- |
| `printf(format, ...)` | Console.Print, preenchido com os valores depois do formato. |
| `sprintf(format, ...)` | Devolve o texto em vez de imprimi-lo. |
| `puts(text)` | Console.PrintLine: imprime e encerra a linha. |
| `putchar(char)` | Console.Print para um caractere. |
| `gets()` | Console.ReadLine: espera por uma linha. |
| `getchar()` | Console.Read: o próximo caractere como número. |
| `scanf(format, out ...)` | Console.Scan, um valor por lacuna. |

Table: Números e texto

| Nome antigo | Chamada da biblioteca |
| --- | --- |
| `exit(status)` | Program.Exit: encerra o programa com o status. |
| `abs, min, max` | Math.Abs, Math.Min e Math.Max. |
| `floor, sqrt, pow` | Math.Floor, Math.Sqrt e Math.Pow. |
| `atoi(text)` | Convert.ToInt(text, 0): 0 quando o texto não é um número. |
| `atof(text)` | Convert.ToDouble. |
| `itoa(value, base)` | Convert.ToString(number, base). |
| `srand(seed)` | Random.Seed: reinicia a sequência. |
| `rand()` | Random.Next(32768): um número de 0 a 32767. |
| `strlen(text)` | O tamanho do texto, como string.Length. |
| `strstr(text, part)` | A posição da parte dentro do texto, como IndexOf. |
| `strcmp(a, b)` | string.Compare: a ordem de dois textos. |
| `strcpy(out dest, src)` | Coloca o texto em dest. |
| `strcat(ref dest, src)` | Adiciona o texto ao fim de dest. |
| `toupper, tolower` | A forma maiúscula ou minúscula de um caractere. |
| `isdigit, isalpha, isspace` | char.IsDigit, char.IsLetter e char.IsWhiteSpace. |

Table: Arquivos e a máquina

| Nome antigo | Chamada da biblioteca |
| --- | --- |
| `remove(path)` | File.Delete: devolve true ou false. |
| `rename(from, to)` | File.Move: devolve true ou false. |
| `fopen(path, mode)` | File.Open: devolve um FILE, ou null. |
| `fclose(file)` | FILE.Close(). |
| `fgets(out line, file)` | FILE.ReadLine. |
| `fputs(text, file)` | FILE.Write com o texto. |
| `fprintf(file, format, ...)` | FILE.Write com o texto formatado. |
| `fscanf(file, format, ...)` | FILE.Scan. |
| `fgetc(file)` | FILE.Read(), o próximo caractere como número. |
| `fputc(char, file)` | FILE.Write com um caractere. |
| `feof(file)` | FILE.AtEnd. |
| `rewind(file)` | FILE.Seek(0): volta ao início. |
| `fseek(file, place)` | FILE.Seek com a posição. |
| `ftell(file)` | FILE.Position: a posição atual. |
| `bus(name)` | Bus.Named: o barramento com esse nome. |
| `redstone(name)` | Redstone.Named: a Redstone Interface com esse nome. |
| `craftInterface(name)` | CraftInterface.Named. |
| `craftRouter(name)` | CraftRouter.Named. |

Os formatos de printf, sprintf, scanf, fprintf e fscanf funcionam do mesmo jeito. Uma lacuna começa com um sinal de porcentagem e uma letra: %d para um número inteiro, %f para um número decimal, %s para texto, %c para um caractere e %% para um sinal de porcentagem.

O compilador conhece %d, %i, %u, %x, %X, %o, %f, %e, %E, %s, %c e %%. Uma lacuna da versão 1 aceita apenas as letras simples d, i, f, s e c, sem nada entre o sinal de porcentagem e a letra. Opções, larguras e precisões vieram na versão 2.

> [!DEFINE] Função avulsa
> Um nome do estilo antigo, como printf ou rand, que a biblioteca atende com um membro que também pode ser chamado pelo nome completo.

> [!DEFINE] Lacuna
> Uma posição em um texto de formato, iniciada por um sinal de porcentagem, que é preenchida com um valor.

> [!DEFINE] FILE
> Um arquivo que o programa abriu e com o qual trabalha um pedaço por vez.

> [!DEFINE] Null
> Nenhum valor: o programa não guarda nada, e uma chamada que devolve null responde com nada.

Table: Memória crua, na versão 3

| Nome antigo | Chamada da biblioteca |
| --- | --- |
| `malloc(bytes)` | Memory.Alloc: um bloco novo de memória crua. |
| `calloc(count, size)` | Memory.AllocZeroed: um bloco para count valores desse tamanho. |
| `realloc(p, bytes)` | Memory.Resize: o bloco com um tamanho novo. |
| `free(p)` | Memory.Free: devolve o bloco. |
| `memcpy(dest, src, bytes)` | Memory.Copy, na mesma ordem. |
| `memset(p, value, bytes)` | Memory.Fill, na mesma ordem. |
| `memcmp(a, b, bytes)` | Memory.Compare: abaixo de zero, zero ou acima de zero. |

## Se algo der errado

> [!PROBLEM] O compilador diz que a lacuna não é uma que o printf tem.
> Use uma das lacunas listadas acima, como %d para um número inteiro ou %s para texto.

> [!PROBLEM] O compilador diz que uma largura ou uma precisão não pode vir de um valor aqui.
> Escreva o número no formato, como em %5d, em vez de um asterisco.

> [!PROBLEM] O compilador diz que o formato precisa ser escrito por extenso.
> Coloque o formato entre aspas na chamada, e não em uma variável.

> [!PROBLEM] O programa para e diz que a lacuna não é uma que o printf desta máquina pode escrever.
> Use uma lacuna da lista acima.

> [!SEE] jsc:sigma_printf_old_names, jsc:sigma_methods_out_ref
