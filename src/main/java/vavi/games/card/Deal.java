/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;

import java.util.List;


/**
 * Deal. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/24 umjammer initial version <br>
 */
public interface Deal {

    /** */
    void deal(List<Card> cards, List<Player> players);

    /** */
    public static Deal SimpleDeal = new Deal() {
        public void deal(List<Card> cards, List<Player> players) {
            // 配りきり
            int p = 0;
            outer:
            while (p < cards.size()) {
                for (Player player : players) {
                    player.addCard(cards.get(p++));
                    if (p == cards.size()) {
                        break outer;
                    }
                }
            }
        }
    };
}

/* */
