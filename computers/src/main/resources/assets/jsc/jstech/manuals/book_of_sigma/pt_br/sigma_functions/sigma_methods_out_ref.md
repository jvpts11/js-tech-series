---
section: sigma_functions
order: 416
---
# Métodos, out e ref

Um método é um trecho de código com nome que você chama pelo nome, muitas vezes com valores para trabalhar. Seu cabeçalho diz o que o método devolve, seu nome e seus parâmetros. Seu corpo guarda as instruções que rodam. Um método que não devolve nada tem void como tipo de resultado.

Table: As formas de um método

| Forma | Significado |
| --- | --- |
| `static int Twice(int x)` | Um método estático que devolve um int, a partir de um parâmetro. |
| `void Show(string text)` | Um método que não devolve nada. |
| `bool Split(int total, out int half)` | Um método com um parâmetro out, que o método preenche. |

Um método estático pertence à própria classe. Você o chama pelo nome da classe, como Maths.Twice(4). Um método sem static pertence a um objeto, e você o chama nesse objeto, como till.Show("hi").

```sg Maths.sg
class Maths
{
    static int Twice(int x) { return x * 2; }

    static bool Split(int total, out int half)
    {
        if (total % 2 != 0)
        {
            half = 0;
            return false;
        }
        half = total / 2;
        return true;
    }
}
```

Os valores que você passa a um método são os seus argumentos. O método os recebe nos seus parâmetros, que são os nomes do seu cabeçalho. Um argumento de tipo valor é copiado, então o método não pode mudar a variável do chamador. Um objeto é passado como o mesmo objeto, então o método pode mudar o que o chamador vê.

Vários métodos podem ter o mesmo nome quando os seus parâmetros são diferentes. São sobrecargas. O compilador escolhe a versão cujos parâmetros servem aos argumentos. Uma chamada que não serve a nenhuma versão é recusada, e também uma chamada que serve igualmente a duas versões.

Um parâmetro out permite que um método devolva um segundo valor. O cabeçalho escreve out antes do parâmetro, e o método deve preenchê-lo antes de cada saída. O chamador escreve out antes de uma variável existente com exatamente o tipo do parâmetro.

```sg Caller.sg
int half;
bool even = Maths.Split(10, out half);
printf("%d", half);
```

A palavra-chave ref só é escrita para strcat, que une texto ao texto que uma variável guarda. Um método que precisa preencher um valor usa out. Um método também pode chamar a si mesmo: cada chamada ocupa espaço na pilha de chamadas, e uma cadeia com mais de 1.024 chamadas de profundidade para o programa, como explica a página [paradas do runtime](jsc:sigma_runtime_stops).

> [!DEFINE] Estático
> Escrito em um método ou em um campo, significa que o membro pertence à classe, não a um objeto.

> [!DEFINE] Parâmetro
> Um nome no cabeçalho de um método, que recebe o valor passado a ele.

> [!DEFINE] Argumento
> Um valor que você passa a um método ao chamá-lo.

> [!DEFINE] Sobrecarga
> Um de vários métodos com o mesmo nome, diferenciados pelos seus parâmetros.

## Se algo der errado

> [!PROBLEM] O compilador diz que nenhuma versão de 'Twice' aceita esses argumentos.
> Confira o número de valores na chamada com os parâmetros do cabeçalho.

> [!PROBLEM] O compilador diz que a chamada a 'Twice' serve para mais de uma versão dele.
> Dê aos argumentos tipos exatos, para que só uma sobrecarga sirva.

> [!PROBLEM] O compilador diz que 'half' é preenchido pelo método, então o argumento é escrito com out.
> Escreva out antes da variável na chamada.

> [!PROBLEM] O compilador diz que 'half' é preenchido por este método, então ele precisa receber um valor antes de cada saída.
> Defina o parâmetro out antes de cada return, como o Split faz antes de retornar.

> [!PROBLEM] O compilador diz que um argumento out é exatamente 'int', sem conversão no caminho.
> Declare a variável com exatamente o tipo do parâmetro.

> [!PROBLEM] O compilador diz que 'ref' só é escrito para strcat.
> Escreva out, para que o método preencha um valor que o chamador depois lê.

> [!SEE] jsc:sigma_classes, jsc:sigma_delegates_events
