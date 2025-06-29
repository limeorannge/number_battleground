# Number battleground! ➕➖✖️➗

This website is the final result for 2025-1 SNU Computer Programming (M1522.000600) project assignment.

Currently database server is dead, so leaderboard system is disabled unless I get motivated enough to fix it. 😢

## Rules

Objective of this web-game is to build most efficient mathematical expression, which result is close to the target number.

Expression can only be composed by tan(), parenthesis, +, -, *, /, and single-digit numbers.

Target number is randomly selected in a range of 1 from 1e9, and new target number appears every 24 hours. (09:00 KST)

Player with least penalty wins! 🥇

## Ranking system

Submission with least penalty wins. If multiple submissions have same penalty, earlier submission gets higher rank.

Penalty is calculated as :

```
10*(component count) + exp(10*error rate)
```





