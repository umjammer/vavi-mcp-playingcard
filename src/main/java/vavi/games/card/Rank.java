/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;


/**
 * Rank of a standard playing card, A to K.
 * <p>
 * Ranks carry no strength here; each game rule defines its own card order.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public enum Rank {

    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K");

    private final int number;
    private final String label;

    Rank(int number, String label) {
        this.number = number;
        this.label = label;
    }

    /** 1 (A) to 13 (K) */
    public int number() {
        return number;
    }

    /** A, 2, ..., 10, J, Q, K */
    public String label() {
        return label;
    }

    /** @param number 1 to 13 */
    public static Rank of(int number) {
        if (number < 1 || number > 13) {
            throw new IllegalArgumentException("rank out of range: " + number);
        }
        return values()[number - 1];
    }

    /** @param label A, 2..10, J, Q, K, also accepts T for 10 (case insensitive) */
    public static Rank of(String label) {
        String l = label.toUpperCase();
        if (l.equals("T")) {
            return TEN;
        }
        for (Rank rank : values()) {
            if (rank.label.equals(l)) {
                return rank;
            }
        }
        throw new IllegalArgumentException("unknown rank: " + label);
    }
}
