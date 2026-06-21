# SmartScrabble

A two-player JavaFX Scrabble game — Java OOP course project.

**Sabir Ali | 73971 | BS(AI) – Iqra University**

## How to Run

Requires **Java 17+**. No Maven install needed.

```
cd ScrabbleCIS
mvn javafx:run
```

Or double-click **Launch SmartScrabble.bat** (Windows).

## How to Play

1. Your 7 rack tiles appear at the bottom — click one to select it.
2. Click any empty board square to place it.
3. First word must pass through the centre **★** square.
4. Click **Submit Word** to validate and score.
5. **Recall** takes back unsubmitted tiles. **Pass** skips your turn.
6. **Save / Load** persists the game session via Java Serialization.

## Project Structure

```
ScrabbleCIS/
├── pom.xml
├── mvnw / mvnw.cmd
├── src/main/java/com/example/scrabble/
│   ├── Main.java          ← JavaFX entry point
│   └── ScrabbleGame.java  ← all game classes in one file
└── src/main/resources/
    └── style.css          ← dark theme
```

## OOP Concepts Covered

| Concept | Where |
|---------|-------|
| Constructor overloading (default / parameterised / copy) | `Tile` |
| Encapsulation | All model classes |
| Static vs instance members | `Player.count`, `TileBag.totalEverCreated` |
| Method overloading | `Player.addScore()`, `Player.removeFromRack()` |
| Composition & aggregation | `Board` owns `Square[][]`; `Player` owns rack |
| Abstract class + `super` | `Square → NormalSquare / PremiumSquare` |
| Multilevel inheritance | `Square → PremiumSquare → CenterSquare` |
| Interfaces | `Scorable`, `Validatable` |
| Runtime polymorphism | `Square.getLabel()` / multiplier dispatch |
| Custom exceptions | `InvalidWordException`, `InvalidPlacementException` |
| `try / catch / finally` + multi-catch | `onSave()`, `onLoad()`, `onSubmitWord()` |
| Collections — List, Set, Map | `ArrayList`, `HashSet`, `HashMap` |
| Custom iterable collection | `WordHistory implements Iterable<String>` |
| Generics | `GameRecord<T extends Scorable>` |
| Serialization | `GameState` via `ObjectOutputStream / InputStream` |
| Static nested class | `ScoreCalculator.ScoreBreakdown` |
| Lambda expressions | Event handlers, stream, `Map.merge`, sort |
| Anonymous inner class | Pass Turn button handler |
| `final` class | `GameState` |

## Tech Stack

- Java 17 · JavaFX 17 · Apache Maven 3.9.6 · CSS3
