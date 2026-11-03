/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.daifugo;

import java.util.Comparator;
import java.util.List;

import vavi.games.card.Card;
import vavi.games.table.GameState;
import vavi.games.table.Move;
import vavi.games.table.Strategy;


/**
 * A simple, deterministic cpu for 大富豪.
 * <ul>
 * <li>go out immediately when possible</li>
 * <li>otherwise play the weakest combo, dumping more cards at once when leading</li>
 * <li>keep jokers and the strongest cards for the end game</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public class DaifugoStrategy implements Strategy {

    /** strength of A */
    private static final int ACE = 11;

    @Override
    public Move choose(GameState gameState, String player) {
        DaifugoState state = (DaifugoState) gameState;
        List<Move> moves = state.legalMoves(player);
        List<Card> hand = state.hand(player);
        boolean leading = state.field() == null;
        boolean endGame = hand.size() <= 4;

        List<Move> plays = moves.stream().filter(m -> !m.isPass()).toList();
        if (plays.isEmpty()) {
            return Move.pass();
        }
        // going out wins
        for (Move m : plays) {
            if (m.cards().size() == hand.size()) {
                return m;
            }
        }
        // when leading, getting rid of more cards at once is worth a few points of strength
        Comparator<Move> order = Comparator
                .comparingInt((Move m) -> !endGame && m.cards().stream().anyMatch(Card::isJoker) ? 1 : 0)
                .thenComparingInt(m -> state.evaluate(m.cards()).strength() - (leading ? 3 * (m.cards().size() - 1) : 0))
                .thenComparingInt(m -> leading ? -m.cards().size() : m.cards().size());
        Move best = plays.stream().min(order).orElseThrow();

        if (!leading && !endGame) {
            int strength = state.evaluate(best.cards()).strength();
            boolean usesJoker = best.cards().stream().anyMatch(Card::isJoker);
            // save strong cards and jokers while the hand is still big
            if (usesJoker || (strength > ACE && hand.size() > 6)) {
                return Move.pass();
            }
        }
        return best;
    }
}
