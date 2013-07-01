/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


/**
 * Shuffle. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/23 umjammer initial version <br>
 */
public interface Shuffle {

    void suffle(List<Card> cards);

    Shuffle SimpleShuffle = new Shuffle() {
        public void suffle(List<Card> cards) {
            Collections.shuffle(cards);
        }
    };

    Shuffle SimpleRandomShuffle = new Shuffle() {
        public void suffle(List<Card> cards) {
            class X implements Comparable<X> {
                Card card;
                int sort;
                X(Card card, int sort) {
                    this.card = card;
                    this.sort = sort;
                }
                public int compareTo(X o) {
                    return this.sort - o.sort;
                }
            }
            Random random = new Random(System.currentTimeMillis());
            List<X> x = new ArrayList<X>();
            int s = cards.size();
            for (int i = 0; i < s; i++) {
                x.add(new X(cards.get(i), random.nextInt(s)));
            }
            Collections.sort(x);
            cards.clear();
            for (int i = 0; i < s; i++) {
                cards.add(x.get(i).card);
            }
        }
    };

    /**
     * @see "http://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle"
     */
    Shuffle FisherYatesShuffle = new Shuffle() {
        public void suffle(List<Card> cards) {
            Random random = new Random(System.currentTimeMillis());
            for (int i = cards.size() - 1; i >= 1; i--) {
                int swappedCardIndex = random.nextInt(i);
                Collections.swap(cards, i, swappedCardIndex);
            }
        }
    };
}

/* */
