/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.table;

import java.util.List;

import vavi.games.card.Card;


/**
 * A player's action. Generic enough for most card games:
 * an action name plus the cards involved.
 *
 * @param action e.g. "play", "pass"
 * @param cards cards involved, may be empty
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public record Move(String action, List<Card> cards) {

    public static final String PLAY = "play";
    public static final String PASS = "pass";

    public Move {
        action = action.toLowerCase();
        cards = List.copyOf(cards);
    }

    public static Move pass() {
        return new Move(PASS, List.of());
    }

    public static Move play(List<Card> cards) {
        return new Move(PLAY, cards);
    }

    public boolean isPass() {
        return PASS.equals(action);
    }

    /** "play S3 H3" or "pass" */
    @Override
    public String toString() {
        return cards.isEmpty() ? action : action + " " + Card.codes(cards);
    }
}
