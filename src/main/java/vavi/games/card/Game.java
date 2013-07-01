/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;



/**
 * Game. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/24 umjammer initial version <br>
 */
public abstract class Game {

    /** */
    public abstract Comparator<Card> getOrder();

    /** */
    public abstract Deal getDeal();

    /** */
    protected static final Comparator<Card> SimpleOrder = new Comparator<Card>() {
        // joker > suit > rank
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
            if (!n1.suit.equals(n2.suit)) {
                return n1.suit.ordinal() - n2.suit.ordinal();
            } else {
                return n1.rank - n2.rank;
            }
        }
    };

    /** */
    protected List<Player> players = new ArrayList<Player>();

    /** */
    public void addPlayer(Player player) {
        players.add(player);
    }

    /** */
    private Dealer dealer;

    /** */
    private Deck deck;

    /** */
    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    /** */
    public void setDealer(Dealer dealer) {
        this.dealer = dealer;
        dealer.setDeal(getDeal());
    }

    /** */
    public void shuffle() {
        dealer.shuffle(deck);
    }

    /** */
    public void deal() {
        dealer.deal(deck, players);
    }

    /** */
    public abstract boolean isFinished();

    /** */
    public abstract void setPlayersOrder();

    /** */
    public abstract void game();

    /** */
    public abstract Player getCurrentPlayer();

    /** */
    public void printPlayerCards() {
        Collections.sort(getCurrentPlayer().getCards(), getOrder());
        for (Card card : getCurrentPlayer().getCards()) {
            System.err.println(card);
        }
    }
}

/* */
