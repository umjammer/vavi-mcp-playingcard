/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.daifugo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vavi.games.card.Card;
import vavi.games.card.Rank;
import vavi.games.card.Suit;


/**
 * A combination of cards played at once in 大富豪.
 * <p>
 * Card power (weak to strong): 3 4 5 6 7 8 9 10 J Q K A 2, joker is strongest.
 * During a revolution (革命) the order of standard cards is reversed, the joker stays strongest.
 *
 * @param type kind of combination
 * @param cards the cards
 * @param strength comparable value under the revolution state it was evaluated with, higher is stronger
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public record Combo(Type type, List<Card> cards, int strength) {

    /** kind of combination */
    public enum Type {
        /** one card */
        SINGLE,
        /** two or more cards of the same rank (pair, triple, quad...), jokers act as wild cards */
        GROUP,
        /** 階段: three or more consecutive ranks of the same suit, jokers act as wild cards */
        SEQUENCE
    }

    /** strength of the joker */
    public static final int JOKER = 13;

    /** natural power of a rank: 3 -> 0, ..., K -> 10, A -> 11, 2 -> 12 */
    public static int power(Rank rank) {
        return (rank.number() + 10) % 13;
    }

    /** natural power of a card, joker is {@link #JOKER} */
    public static int power(Card card) {
        return switch (card) {
            case Card.Joker _ -> JOKER;
            case Card.Standard s -> power(s.rank());
        };
    }

    /** power of a card under the revolution state */
    public static int power(Card card, boolean revolution) {
        int p = power(card);
        return revolution && p != JOKER ? 12 - p : p;
    }

    public int size() {
        return cards.size();
    }

    /** contains a real 8 (8切り) */
    public boolean hasEight() {
        return cards.stream().anyMatch(c -> c instanceof Card.Standard s && s.rank() == Rank.EIGHT);
    }

    /** a group of four or more triggers a revolution (革命) */
    public boolean isRevolution() {
        return type == Type.GROUP && size() >= 4;
    }

    /** single joker */
    public boolean isSingleJoker() {
        return type == Type.SINGLE && cards.getFirst().isJoker();
    }

    /** single ♠3 */
    public boolean isSpade3() {
        return type == Type.SINGLE && cards.getFirst().equals(Card.of(Suit.SPADES, Rank.THREE));
    }

    /** standard rules: same kind and number of cards */
    public boolean beats(Combo field, boolean spade3) {
        return beats(field, spade3, false);
    }

    /**
     * @param field combo on the field, evaluated with the same revolution state
     * @param spade3 enable "♠3 beats a single joker" rule (スペ3返し)
     * @param coverWithMore a single or group may be covered by a stronger group of more cards
     *        (e.g. 3 or 4 of a kind on a pair), sequences still need the same length
     */
    public boolean beats(Combo field, boolean spade3, boolean coverWithMore) {
        boolean sameShape = type == field.type && size() == field.size();
        boolean more = coverWithMore && type != Type.SEQUENCE && field.type != Type.SEQUENCE && size() > field.size();
        if (!sameShape && !more) {
            return false;
        }
        if (spade3 && isSpade3() && field.isSingleJoker()) {
            return true;
        }
        return strength > field.strength;
    }

    /**
     * Every valid way to read the cards as a combo (e.g. ♠5 🃏 🃏 is both a triple and a sequence).
     * Group readings come first.
     *
     * @return empty when the cards don't form a valid combo
     */
    public static List<Combo> interpretations(List<Card> cards, boolean revolution) {
        List<Combo> result = new ArrayList<>();
        if (cards.isEmpty()) {
            return result;
        }
        group(cards, revolution, result);
        sequence(cards, revolution, result);
        return result;
    }

    private static void group(List<Card> cards, boolean revolution, List<Combo> result) {
        List<Card.Standard> standards = standards(cards);
        if (standards.stream().map(Card.Standard::rank).distinct().count() > 1) {
            return;
        }
        int strength = standards.isEmpty() ? JOKER : power(standards.getFirst(), revolution);
        result.add(new Combo(cards.size() == 1 ? Type.SINGLE : Type.GROUP, List.copyOf(cards), strength));
    }

    private static void sequence(List<Card> cards, boolean revolution, List<Combo> result) {
        if (cards.size() < 3) {
            return;
        }
        List<Card.Standard> standards = standards(cards);
        if (standards.isEmpty() || standards.stream().map(Card.Standard::suit).distinct().count() > 1) {
            return;
        }
        Set<Integer> powers = new HashSet<>();
        for (Card.Standard s : standards) {
            if (!powers.add(power(s.rank()))) {
                return; // duplicated rank
            }
        }
        int jokers = cards.size() - standards.size();
        int lo = powers.stream().min(Integer::compare).orElseThrow();
        int hi = powers.stream().max(Integer::compare).orElseThrow();
        int gaps = (hi - lo + 1) - standards.size();
        if (gaps > jokers) {
            return;
        }
        // place spare jokers where they make the sequence strongest
        int extra = jokers - gaps;
        if (!revolution) {
            hi += extra;
            if (hi > 12) { lo -= hi - 12; hi = 12; }
        } else {
            lo -= extra;
            if (lo < 0) { hi += -lo; lo = 0; }
        }
        if (lo < 0 || hi > 12) {
            return;
        }
        // strength of a sequence is its weakest card
        int strength = revolution ? 12 - hi : lo;
        result.add(new Combo(Type.SEQUENCE, List.copyOf(cards), strength));
    }

    private static List<Card.Standard> standards(List<Card> cards) {
        List<Card.Standard> result = new ArrayList<>();
        for (Card c : cards) {
            if (c instanceof Card.Standard s) {
                result.add(s);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return Card.symbols(cards);
    }
}
