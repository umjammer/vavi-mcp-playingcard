/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.random.RandomGenerator;


/**
 * A pile of cards that can be shuffled, drawn from and dealt.
 * <p>
 * The top of the deck is index 0.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public final class Deck {

    private final List<Card> cards;

    /** */
    public Deck(List<? extends Card> cards) {
        this.cards = new ArrayList<>(cards);
    }

    /** a 52 card deck plus the given number of jokers, in natural order */
    public static Deck standard(int jokers) {
        List<Card> cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(Card.of(suit, rank));
            }
        }
        for (int i = 1; i <= jokers; i++) {
            cards.add(new Card.Joker(i));
        }
        return new Deck(cards);
    }

    /** Fisher–Yates shuffle with the given random generator (pass a seeded one for reproducible games) */
    public Deck shuffle(RandomGenerator random) {
        for (int i = cards.size() - 1; i > 0; i--) {
            Collections.swap(cards, i, random.nextInt(i + 1));
        }
        return this;
    }

    /** */
    public int size() {
        return cards.size();
    }

    /** */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /** removes the top card */
    public Card draw() {
        if (cards.isEmpty()) {
            throw new NoSuchElementException("deck is empty");
        }
        return cards.removeFirst();
    }

    /** removes n cards from the top */
    public List<Card> draw(int n) {
        List<Card> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(draw());
        }
        return result;
    }

    /** deals every card one by one, round robin, to the given number of hands (配りきり) */
    public List<List<Card>> dealAll(int hands) {
        List<List<Card>> result = emptyHands(hands);
        for (int i = 0; !cards.isEmpty(); i++) {
            result.get(i % hands).add(draw());
        }
        return result;
    }

    /** deals {@code each} cards to every hand, round robin */
    public List<List<Card>> deal(int hands, int each) {
        if (hands * each > cards.size()) {
            throw new IllegalArgumentException("not enough cards: " + cards.size() + " < " + hands * each);
        }
        List<List<Card>> result = emptyHands(hands);
        for (int i = 0; i < hands * each; i++) {
            result.get(i % hands).add(draw());
        }
        return result;
    }

    private static List<List<Card>> emptyHands(int hands) {
        List<List<Card>> result = new ArrayList<>();
        for (int i = 0; i < hands; i++) {
            result.add(new ArrayList<>());
        }
        return result;
    }

    /** unmodifiable view, top first */
    public List<Card> cards() {
        return Collections.unmodifiableList(cards);
    }

    @Override
    public String toString() {
        return Card.symbols(cards);
    }
}
