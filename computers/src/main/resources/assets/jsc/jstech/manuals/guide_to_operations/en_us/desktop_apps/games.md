---
section: desktop_apps
order: 85
icon: jsc:monitor
---
# Minesweeper, Solitaire and Snake

Three games run on the desktop systems. Each one is installed like any other program ([](jsc:programs)), and needs 1 megabyte (MB) of memory.

## Minesweeper

Reveal a cell to see how many mines touch it. A number shows that count, in its own colour. An empty cell opens the cells around it. You flag a cell you think holds a mine.

> [!DEFINE] Flag
> A mark you place on a cell that you think holds a mine.

Table: Minesweeper levels

| Level | Button | Grid | Mines |
| --- | --- | --- | --- |
| Beginner | `Beg` | 9 by 9 | 10 |
| Intermediate | `Int` | 16 by 16 | 40 |
| Expert | `Exp` | 16 by 30 | 99 |

The game starts on Beginner.

## Solitaire

Solitaire is Klondike. You draw one card at a time from the stock, the face-down pile, into the waste, which holds the cards you drew. The seven columns of cards are the tableau, and the four piles, one for each suit, are the foundations, built from ace up to king.

> [!DEFINE] Tableau
> The seven columns of cards in Solitaire, where the cards are laid out in a row.

> [!DEFINE] Foundation
> One of the four piles in Solitaire, where a suit is built from ace up to king.

Table: Points in Solitaire

| Move | Points |
| --- | --- |
| Waste to tableau | 5 points |
| Turn over a card | 5 points |
| To a foundation | 10 points |
| From a foundation back to the tableau | minus 15 points |
| Waste back to the stock | minus 100 points |

The window shows the score and the moves, and it says "You win" when the game is done. The New button starts another game.

## Snake

Snake runs on a grid of 20 by 12 cells. You eat the food to grow, and each food is worth 10 points. The snake starts at a length of 4. Press an arrow key to start.

The speed sets how many ticks pass between two steps. A tick is the game's beat, and 20 ticks make a second ([](jsc:units_and_ticks)).

Table: Speeds of Snake

| Speed | A step every |
| --- | --- |
| Slow | 6 ticks |
| Normal | 4 ticks |
| Fast | 2 ticks |

With walls on, running into a wall ends the game. With walls off, the grid wraps round, and only running into yourself ends the game. The window shows the score, the best score and the length, and it says "Game over" when you lose.

> [!SEE] jsc:programs, jsc:units_and_ticks
