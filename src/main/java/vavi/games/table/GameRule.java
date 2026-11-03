/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.table;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;


/**
 * Game rule plugin SPI.
 * <p>
 * Implementations are discovered with {@link java.util.ServiceLoader}, register yours in
 * {@code META-INF/services/vavi.games.table.GameRule}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public interface GameRule {

    /** short id used in tool calls, e.g. "daifugo" */
    String id();

    /** display name */
    String name();

    /** rules explanation including the move syntax; shown to AI clients */
    String rules();

    int minPlayers();

    int maxPlayers();

    /** supported options and their defaults, e.g. {"jokers": "1"} */
    default Map<String, String> defaultOptions() {
        return Map.of();
    }

    /** starts a new round (a hand of the game) */
    GameState newRound(RoundSetup setup);

    /** strategy used for cpu seats */
    Strategy cpuStrategy();

    /** game specific title for a finishing place (1 origin, e.g. 1 -> 大富豪), null if none */
    default String title(int place, int players) {
        return null;
    }

    /**
     * Input for {@link #newRound}.
     *
     * @param seats players in seat (turn) order
     * @param round 1 origin
     * @param previousRanking player names, winner first, of the previous round; empty on the first round
     * @param random seeded random for reproducibility
     * @param options rule options merged with {@link #defaultOptions()}
     */
    record RoundSetup(List<Player> seats, int round, List<String> previousRanking,
                      RandomGenerator random, Map<String, String> options) {
    }
}
