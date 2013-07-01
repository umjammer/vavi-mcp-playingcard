/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.daifugo;

import java.util.Comparator;

import vavi.games.card.Card;
import vavi.games.card.Deal;
import vavi.games.card.Game;
import vavi.games.card.Player;


/**
 * Daifugo. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/24 umjammer initial version <br>
 */
public class Daifugo extends Game {

    /** */
    private static final Comparator<Card> DaifugoOrder = new Comparator<Card>() {
        final int[] strength = { 12, 13, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
        // joker > rank
        // TODO revolution
        public int compare(Card o1, Card o2) {
            if (Card.Joker.class.isInstance(o1) && Card.Joker.class.isInstance(o2)) {
                return 0;
            } else if (Card.Joker.class.isInstance(o1)) {
                return -1;
            } else if (Card.Joker.class.isInstance(o2)) {
                return 1;
            }

            Card.Normal n1 = Card.Normal.class.cast(o1); 
            Card.Normal n2 = Card.Normal.class.cast(o2);
            return strength[n2.getRank() - 1] - strength[n1.getRank() - 1];
        }
    };

    @Override
    public Comparator<Card> getOrder() {
        return DaifugoOrder;
    }

    @Override
    public Deal getDeal() {
        return Deal.SimpleDeal;
    }

    @Override
    public void setPlayersOrder() {
    }

    @Override
    public void game() {
    }

    @Override
    public Player getCurrentPlayer() {
        return players.get(0);
    }

    boolean isFinished = false;

    @Override
    public boolean isFinished() {
        if (!isFinished) {
            isFinished = true;
            return false;
        } else {
            return true;
        }
    }
}

/* */
