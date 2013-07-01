/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;

import java.util.List;



/**
 * Dealer. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/24 umjammer initial version <br>
 */
public class Dealer {

    /** */
    private Shuffle shuffle;
    
    /** */
    private Deal deal;

    /** */
    public void setShuffle(Shuffle shuffle) {
        this.shuffle = shuffle;
    }
    
    /** */
    public void setDeal(Deal deal) {
        this.deal = deal;
    }

    /** */
    public void shuffle(Deck deck) {
        shuffle.suffle(deck.getCards());
    }

    /** */
    public void deal(Deck deck, List<Player> players) {
        deal.deal(deck.getCards(), players);
    }
}

/* */
