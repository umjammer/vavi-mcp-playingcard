/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;



/**
 * Card. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/23 umjammer initial version <br>
 */
public abstract class Card {

    /** */
    public static class Normal extends Card {
        static final String[] view = { "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K" };
        /** */
        enum Suit {
            Spade("♠"),
            Heart("♥"),
            Diamond("♦"),
            Club("♣");
            String s;
            Suit(String s) {
                this.s = s;
            }
        }
        /** */
        Normal(Suit suit, int rank) {
            this.suit = suit;
            this.rank = rank;
        }
        Suit suit;
        int rank;
        public int getRank() {
            return rank;
        }
        public String toString() {
            return suit.s + view[rank - 1];
        }
    }
    
    /** */
    public static class Joker extends Card {
        public String toString() {
            return "🃏";
        }
    }
}

/* */
