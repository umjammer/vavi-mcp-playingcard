/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.table;


/**
 * Decides a move for a cpu player.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
@FunctionalInterface
public interface Strategy {

    /** must return one of {@link GameState#legalMoves(String)} */
    Move choose(GameState state, String player);
}
