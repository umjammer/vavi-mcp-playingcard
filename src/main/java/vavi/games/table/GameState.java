/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.table;

import java.util.List;


/**
 * State of one round, owned by a {@link GameRule} plugin.
 * All rule enforcement happens here; the {@link Table} only routes moves.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public interface GameState {

    /** name of the player to move, null when finished */
    String currentPlayer();

    boolean isFinished();

    /** legal moves for the player; empty when it is not the player's turn */
    List<Move> legalMoves(String player);

    /**
     * Applies a move.
     *
     * @return events caused by the move (human readable)
     * @throws IllegalArgumentException when the move is illegal, the message tells why
     */
    List<String> apply(String player, Move move);

    /**
     * Text view of the state.
     *
     * @param viewer player name to show the hand of, null for the omniscient view (all hands)
     */
    String render(String viewer);

    /** player names in finishing order so far, winner first */
    List<String> ranking();

    /** all events of this round */
    List<String> log();
}
