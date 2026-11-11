---
section: examples_language
order: 512
---
# Nomes antigos

Os nomes antigos são funções curtas e conhecidas, como printf, puts e strlen, que programas antigos usam. Cada um é uma forma curta de uma chamada que a biblioteca já tem. Por exemplo, puts dá o mesmo resultado que Console.PrintLine.

Um método ou uma variável do seu próprio programa com o mesmo nome tem prioridade. O programa chama o seu próprio método, não o nome antigo. Este programa precisa da versão 2, porque usa nomes antigos que a versão 2 acrescenta, e o seu Main recebe argumentos.

## O programa

```sgs OldNames.sgs
using System.*;
using System.IO.*;
namespace Corpus;
class OldNamesCorpus {
    static int Main(string[] args) {
        puts("How many ingots?");
        putchar('>');
        Console.Print(' ');
        string line = gets();
        int letter = getchar();
        int count;
        int scanned = scanf("%d", out count) + Console.Scan(out count) + letter;
        string built;
        strcpy(out built, line);
        strcat(ref built, "!");
        FILE notes = fopen("notes.txt", "a+");
        string held;
        bool gotLine = fgets(out held, notes) && !feof(notes) && notes.ReadLine(out held) && notes.AtEnd;
        fputs(built, notes);
        fputc('.', notes);
        notes.Write("x");
        notes.Write('y');
        int where = fgetc(notes) + notes.Read() + ftell(notes) + notes.Position + notes.Scan(out count);
        rewind(notes);
        fseek(notes, 2);
        notes.Seek(1);
        fprintf(notes, "%05d\n", where);
        int more = fscanf(notes, "%d", out count);
        fclose(notes);
        FILE again = File.Open("notes.txt", "r");
        again.Close();
        bool moved = remove("old.txt") || rename("a.txt", "b.txt") || File.Move("b.txt", "c.txt");
        double asked = atof(line);
        int n = abs(-3) + min(4, 5) + max(1, 2);
        double root = sqrt(pow(3, 2)) + floor(2.5) + abs(-0.5) + min(1.5, 2) + max(0.5, 1);
        srand(1987);
        int lucky = rand();
        int length = strlen(line);
        int at = strstr(line, "ingot");
        string said = sprintf("%s: %d, %f", itoa(n), lucky + length + at, root + asked);
        printf("%s\n", said);
        printf("%-12s|%5d|%05.2f|%#x|%e|%u\n", said, n, root, lucky, asked, length);
        int order = strcmp(line, said) + string.Compare(said, line) + atoi(line) + Convert.ToInt(line, -1);
        char first = toupper('a');
        char last = tolower(char.ToUpper('z'));
        bool kinds = isdigit(first) || isalpha(last) || isspace(' ') || char.ToLower(first) == 'a'
                || char.IsDigit('1') || char.IsLetter('b') || char.IsWhiteSpace('\t');
        puts(itoa(order, 16) + Convert.ToString(lucky, 2) + first + last + kinds);
        if (args.Length > 1) {
            exit(0);
        }
        return args.Length;
    }
}
```

## Entrada e saída

Table: Nomes antigos para imprimir e ler

| Nome antigo | A forma longa que ele chama |
| --- | --- |
| `printf` | `Console.Print, with a formatted text` |
| `puts` | `Console.PrintLine` |
| `putchar` | `Console.Print, with one character` |
| `gets` | `Console.ReadLine` |
| `getchar` | `Console.Read` |
| `scanf` | `Console.Scan, with one value` |
| `exit` | `Program.Exit` |

## Números e texto

Table: Nomes antigos para números e texto

| Nome antigo | A forma longa que ele chama |
| --- | --- |
| `abs, min, max` | `Math.Abs, Math.Min, Math.Max` |
| `sqrt, pow, floor` | `Math.Sqrt, Math.Pow, Math.Floor` |
| `atoi` | `Convert.ToInt, which gives 0 for text that is not a number` |
| `atof` | `Convert.ToDouble` |
| `itoa` | `Convert.ToString` |
| `sprintf` | `the formatted text, as a value` |
| `srand, rand` | `Random.Seed, and Random.Next(32768)` |
| `strlen` | `the Length of the text` |
| `strstr` | `IndexOf of the text` |
| `strcmp` | `string.Compare` |
| `strcpy` | `the out place takes the text` |
| `strcat` | `the ref place gets the text added on` |
| `toupper, tolower` | `Char.ToUpper, Char.ToLower` |
| `isdigit, isalpha, isspace` | `Char.IsDigit, Char.IsLetter, Char.IsWhiteSpace` |

## Arquivos

Table: Nomes antigos para arquivos

| Nome antigo | A forma longa que ele chama |
| --- | --- |
| `fopen` | `File.Open, which gives a FILE` |
| `fclose` | `Close of the file` |
| `fgets` | `ReadLine of the file` |
| `fputs, fprintf` | `Write of the text` |
| `fscanf` | `Scan of the file` |
| `fgetc` | `Read of the file` |
| `fputc` | `Write of one character` |
| `feof` | `AtEnd of the file` |
| `rewind` | `Seek to 0` |
| `fseek` | `Seek to the given position` |
| `ftell` | `Position of the file` |
| `remove` | `File.Delete` |
| `rename` | `File.Move` |

O texto de um formato de printf tem lacunas, e um valor preenche cada lacuna. Uma lacuna é um sinal de porcentagem e uma letra, e um número escrito dentro da lacuna define a sua largura.

Table: As lacunas de um formato

| Lacuna | Recebe |
| --- | --- |
| `%d, %i, %u, %x, %X, %o` | Um número inteiro. |
| `%f, %e, %E` | Um número do tipo double. |
| `%s` | Texto. |
| `%c` | Um caractere. |
| `%%` | Um sinal de porcentagem, escrito como ele mesmo. |

> [!DEFINE] Função avulsa
> Uma função escrita sem tipo nem objeto na frente do nome, como puts(...).

> [!DEFINE] String de formato
> Um texto com lacunas, como %d, que os valores de uma chamada preenchem em ordem.

> [!DEFINE] Lacuna
> Um lugar em uma string de formato que um valor preenche.

> [!DEFINE] FILE
> Um arquivo que o programa tem aberto, a partir de fopen.

> [!DEFINE] Out
> Uma variável que um método escreve, e que a chamada entrega ao método.

> [!DEFINE] Ref
> Uma variável que um método lê e escreve, e que a chamada entrega ao método.

## Se algo der errado

> [!PROBLEM] O programa é compilado na versão 1, e um nome que precisa da versão 2 é recusado com o código S3057. A mensagem informa o nome, a linguagem e a versão de que ele precisa.
> Compile com --lang 2, ou tire o nome do programa. Veja ([](jsc:sigma_examples_intro)).

> [!PROBLEM] O formato de printf fica guardado em uma variável, e o compilador o recusa com o código S3053.
> Escreva o formato entre aspas dentro da chamada, onde ele é usado.

> [!PROBLEM] O formato tem mais lacunas do que a chamada tem valores, e o compilador o recusa com o código S3055.
> Dê um valor para cada lacuna, na ordem.

> [!PROBLEM] Um valor não combina com a sua lacuna, como um texto para um %d, e o compilador o recusa com o código S3056.
> Mude o valor, ou a letra da lacuna, para que combinem.

> [!PROBLEM] scanf é escrito com duas lacunas, ou sem out, e o compilador o recusa.
> Escreva scanf com uma lacuna e nada em volta, e a variável com out, como faz scanf("%d", out n).

> [!PROBLEM] O seu método deixa um argumento out vazio em uma saída, e o compilador o recusa com o código S3037.
> Dê um valor ao argumento out antes de cada return.

> [!SEE] jsc:sigma_printf_old_names, jsc:sigma_methods_out_ref, jsc:sigma_library
