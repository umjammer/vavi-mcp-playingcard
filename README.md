[![Release](https://jitpack.io/v/umjammer/vavi-mcp-playingcard.svg)](https://jitpack.io/#umjammer/vavi-mcp-playingcard)
[![Java CI](https://github.com/umjammer/vavi-mcp-playingcard/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-mcp-playingcard/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-mcp-playingcard/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-mcp-playingcard/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-25-b07219)

# vavi-mcp-playingcard

An MCP server that acts as the game master (dealer) for playing card games.
The server keeps the cards, the players and their win/lose stats, and enforces the rules.
The AI client plays through tools. Game rules are plugins, and 大富豪 (Daifugo) is included as a sample.

## Install

* [maven](https://jitpack.io/#umjammer/vavi-mcp-playingcard)

## Usage

### build & run

```shell
mvn package
java -jar target/vavi-mcp-playingcard-0.0.1-SNAPSHOT-all.jar   # stdio MCP server
```

Claude Code:

```shell
claude mcp add playingcard -- java -jar /path/to/vavi-mcp-playingcard-0.0.1-SNAPSHOT-all.jar
```

### tools

| tool | description |
|------|-------------|
| `list_games` | games, rules, player counts, options |
| `create_table` | open a table: `game`, `players` (played by the client as itself), `cpu_players` (played by the server), `llm_players` (CPU seats handed off to the client), `seed`, `options`; seat order is decided by dice |
| `start_round` | shuffle, deal, exchange cards (大富豪), and let the CPUs play until a human player's turn |
| `get_state` | the table from a player's point of view (`reveal_all` for spectating) |
| `legal_moves` | every legal move for the current player |
| `play` | `action` = `play` with `cards` (e.g. `["S3","H3"]`, `JK` for a joker), or `pass` |
| `get_log` | events of the current round |
| `list_tables` / `close_table` | table management |
| `get_stats` | wins, losses, places, and titles per player |
| `roll_dice` | dice |

### seats and CPU profiles

| seat | who moves | how |
|---|---|---|
| `players` | the client (LLM) as itself | `play` |
| `cpu_players` | the game master | built-in strategy, runs automatically |
| `llm_players` | the client (LLM), playing a CPU character | the server stops at the seat's turn and replies with the seat's view plus a persona from its profile; the client calls `play` for it |

CPU and LLM seats are written `"Name"` or `"Name:profile"`. 大富豪 profiles (the first one is the default):

* `balanced`: plays its weakest cards first, gets rid of pairs and sequences when leading, keeps jokers and 2s until its hand is small
* `aggressive`: plays whenever it can, dumps as many cards as possible, uses jokers freely, and starts a revolution when its hand is weak
* `cautious`: passes often to keep A, 2 and jokers
* `random`: picks a random legal move

```json
{"game": "daifugo", "players": ["You"], "cpu_players": ["Cpu1:aggressive"], "llm_players": ["Rival:cautious", "Joker:random"]}
```

Card codes: suit `S H D C` + rank `A 2 … 10 J Q K` (`T` is also accepted for 10), and `JK` for a joker.

### demo

A full 4-player, 3-round 大富豪 game printed as text:

```shell
mvn test -Dtest=DaifugoDemoTest#demo
```

Each demo player uses a different profile. `DaifugoDemoTest#league` pits the profiles against each other over 200 rounds,
and `PlayingCardToolsTest` plays rounds through the MCP tools the same way an AI client would, including `llm_players` hand-off.

### architecture

| package | |
|---|---|
| `vavi.games.card` | model: `Card` (sealed: `Standard` / `Joker`), `Suit`, `Rank`, `Deck` (shuffle, deal), `Dice` (deciding the order) |
| `vavi.games.table` | game master: `GameMaster` (rule registry, tables, stats), `Table` (rounds, CPU auto play), `PlayerStats`, the plugin SPI `GameRule` / `GameState` / `Strategy`, and `Move` |
| `vavi.games.daifugo` | 大富豪 plugin: `DaifugoRule`, `DaifugoState`, `Combo`, `Title`, `DaifugoStrategy` (CPU) |
| `vavi.mcp.playingcard` | MCP server (`PlayingCardMcpServer`) and transport-independent tools (`PlayingCardTools`) |

### writing a game plugin

Implement `vavi.games.table.GameRule` (rule text, player counts, options, `newRound`, CPU profiles and strategies),
return a `GameState` that validates and applies `Move`s, and register the class in
`META-INF/services/vavi.games.table.GameRule`.

### 大富豪 rules implemented

* all cards dealt, 3 < … < K < A < 2 < joker
* singles, groups (pairs, triples, …), and sequences (階段, 3+ cards of the same suit); jokers are wild
* passing sits you out until the field is cleared
* 8切り, 革命 (4+ of a kind), スペ3返し — each can be switched off with options `eight_cut`, `revolution`, `spade3`
* `jokers`: 0..2 (default 1)
* titles 大富豪 / 富豪 / 平民 / 貧民 / 大貧民, and card exchange from round 2 (大貧民 ⇄ 大富豪: 2 cards, 貧民 ⇄ 富豪: 1 card); 大貧民 leads

## References

* https://modelcontextprotocol.io/
* https://ja.wikipedia.org/wiki/大富豪

## TODO

* persist player stats
* more local rules (縛り, 11バック, 都落ち, …)
* the 大富豪 choosing which cards to give back in the exchange
* more games (poker, blackjack, …)
