/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;

import java.util.ArrayList;
import java.util.List;

import vavi.games.card.Card.Normal.Suit;


/**
 * Deck. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/23 umjammer initial version <br>
 */
public abstract class Deck {

    protected List<Card> cards = new ArrayList<Card>();

    public List<Card> getCards() {
        return cards;
    }

    public static final Deck SimpleDeck = new Deck() {
        {
            for (Suit suit : Suit.values()) {
                for (int i = 1; i <= 13; i++) {
                    cards.add(new Card.Normal(suit, i));
                }
            }
            cards.add(new Card.Joker());
        }
    };

}

/* */
