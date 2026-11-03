/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;


/**
 * Suit of a standard playing card.
 * <p>
 * Each suit has a one letter code ({@code S, H, D, C}) used in card codes
 * such as {@code "S3"} or {@code "H10"}, which are easy for both humans and AI to type.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public enum Suit {

    SPADES('S', "♠"),
    HEARTS('H', "♥"),
    DIAMONDS('D', "♦"),
    CLUBS('C', "♣");

    private final char code;
    private final String symbol;

    Suit(char code, String symbol) {
        this.code = code;
        this.symbol = symbol;
    }

    /** one letter code: S, H, D, C */
    public char code() {
        return code;
    }

    /** symbol: ♠, ♥, ♦, ♣ */
    public String symbol() {
        return symbol;
    }

    /** @param c S, H, D or C (case insensitive) */
    public static Suit of(char c) {
        char u = Character.toUpperCase(c);
        for (Suit suit : values()) {
            if (suit.code == u) {
                return suit;
            }
        }
        throw new IllegalArgumentException("unknown suit: " + c);
    }
}
