/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;

import java.util.ArrayList;
import java.util.List;



/**
 * Player. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/24 umjammer initial version <br>
 */
public class Player {

    protected String name;

    protected List<Card> cards = new ArrayList<Card>();

    public Player(String name) {
        this.name = name;
    }

    public String toString() {
        return "Player: " + name;
    }

    /** */
    public void addCard(Card card) {
        cards.add(card);
    }

    /** */
    public void clearCards() {
        cards.clear();
    }

    /** hands */
    public List<Card> getCards() {
        return cards;
    }
}

/* */
