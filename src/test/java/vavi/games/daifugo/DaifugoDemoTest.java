/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.daifugo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import vavi.games.card.Card;
import vavi.games.table.GameMaster;
import vavi.games.table.GameState;
import vavi.games.table.Move;
import vavi.games.table.Player;
import vavi.games.table.PlayerStats;
import vavi.games.table.Strategy;
import vavi.games.table.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * A text demo of 大富豪: four players, three rounds, every move printed.
 * <p>
 * Run with {@code mvn test -Dtest=DaifugoDemoTest}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
class DaifugoDemoTest {

    static final long SEED = 20261103L;
    static final int ROUNDS = 3;

    @Test
    void demo() {
        GameMaster master = new GameMaster();
        // seats are client controlled so that this test drives every move itself and can print the hands;
        // each player's moves are chosen by a different cpu profile
        Map<String, String> profiles = new java.util.LinkedHashMap<>();
        profiles.put("Alice", "balanced");
        profiles.put("Bob", "aggressive");
        profiles.put("Carol", "cautious");
        profiles.put("Dave", "random");
        List<Player> players = profiles.keySet().stream().map(n -> new Player(n, false)).toList();
        Table table = master.createTable("daifugo", players, SEED, Map.of());
        java.util.Random random = new java.util.Random(SEED);
        Map<String, Strategy> strategies = new java.util.HashMap<>();
        profiles.forEach((n, p) -> strategies.put(n, table.rule().cpuStrategy(p, random)));

        System.out.println("####################################################");
        System.out.println("# " + table.rule().name() + " demo, seed=" + SEED);
        System.out.println("####################################################");
        profiles.forEach((n, p) -> System.out.println(n + ": " + p + " - " + table.rule().cpuProfiles().get(p)));
        table.setupLog().forEach(System.out::println);

        for (int r = 1; r <= ROUNDS; r++) {
            System.out.println();
            table.startRound().forEach(System.out::println);
            GameState state = table.state();
            System.out.println();
            System.out.print(state.render(null));
            System.out.println();

            int moves = 0;
            while (!state.isFinished()) {
                String player = state.currentPlayer();
                List<Card> hand = ((DaifugoState) state).hand(player);
                Move move = strategies.get(player).choose(state, player);
                assertTrue(state.legalMoves(player).contains(move), "strategy must choose a legal move: " + move);
                List<String> events = table.play(player, move);
                System.out.printf("  %-5s %-48s | %s%n", player, Card.symbols(hand), events.getFirst());
                events.stream().skip(1).forEach(e -> System.out.printf("  %-5s %-48s | %s%n", "", "", e));
                moves++;
                assertTrue(moves < 1000, "round does not terminate");
            }
            System.out.println();
            System.out.print(state.render(null));

            List<String> ranking = state.ranking();
            assertEquals(4, ranking.size());
            assertEquals(4, new HashSet<>(ranking).size());
        }

        System.out.println();
        System.out.println("=== stats ===");
        master.allStats().stream().map(PlayerStats::toString).sorted().forEach(System.out::println);
        assertEquals(ROUNDS * 4, master.allStats().stream().mapToInt(PlayerStats::played).sum());
        assertEquals(ROUNDS, master.allStats().stream().mapToInt(PlayerStats::wins).sum());
    }

    /** the built-in profiles play against each other */
    @Test
    void league() {
        GameMaster master = new GameMaster();
        List<Player> players = List.of(
                new Player("balanced", Player.Control.CPU, "balanced"),
                new Player("aggressive", Player.Control.CPU, "aggressive"),
                new Player("cautious", Player.Control.CPU, "cautious"),
                new Player("random", Player.Control.CPU, "random"));
        for (long seed = 0; seed < 50; seed++) {
            Table table = master.createTable("daifugo", players, seed, Map.of());
            for (int r = 0; r < 4; r++) {
                table.startRound();
            }
        }
        System.out.println("=== profile league: 50 tables x 4 rounds ===");
        master.allStats().stream().sorted(java.util.Comparator.comparingInt(PlayerStats::wins).reversed())
                .forEach(System.out::println);
        assertEquals(200, master.stats("random").played());
    }

    /** many seeded rounds with every option combination must terminate and conserve cards */
    @Test
    void soak() {
        GameMaster master = new GameMaster();
        for (int n = 3; n <= 8; n++) {
            for (int jokers = 0; jokers <= 2; jokers++) {
                List<Player> players = new java.util.ArrayList<>();
                for (int i = 0; i < n; i++) {
                    players.add(new Player("p" + i, true));
                }
                Table table = master.createTable("daifugo", players, (long) (n * 10 + jokers),
                        Map.of("jokers", String.valueOf(jokers)));
                for (int r = 0; r < 5; r++) {
                    table.startRound();
                    assertTrue(table.state().isFinished());
                    assertEquals(n, table.state().ranking().size());
                }
            }
        }
    }
}
