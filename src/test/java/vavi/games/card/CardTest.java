/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * CardTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
class CardTest {

    @Test
    void parse() {
        assertEquals(Card.of(Suit.SPADES, Rank.THREE), Card.parse("S3"));
        assertEquals(Card.of(Suit.HEARTS, Rank.TEN), Card.parse("h10"));
        assertEquals(Card.of(Suit.HEARTS, Rank.TEN), Card.parse("HT"));
        assertEquals(Card.of(Suit.CLUBS, Rank.QUEEN), Card.parse("CQ"));
        assertEquals(new Card.Joker(1), Card.parse("JK"));
        assertEquals(new Card.Joker(2), Card.parse("JK2"));
        assertEquals("♦A", Card.parse("DA").symbol());
        assertEquals(List.of(Card.parse("S3"), Card.parse("D4")), Card.parseAll("S3, D4"));
        assertThrows(IllegalArgumentException.class, () -> Card.parse("X3"));
        assertThrows(IllegalArgumentException.class, () -> Card.parse("S14"));
    }

    @Test
    void codeRoundTrip() {
        for (Card card : Deck.standard(2).cards()) {
            assertEquals(card, Card.parse(card.code()));
        }
    }

    @Test
    void deck() {
        Deck deck = Deck.standard(1);
        assertEquals(53, deck.size());
        assertEquals(53, new HashSet<>(deck.cards()).size());

        Deck a = Deck.standard(0).shuffle(new Random(1));
        Deck b = Deck.standard(0).shuffle(new Random(1));
        assertEquals(a.cards(), b.cards(), "same seed, same order");
        assertNotEquals(Deck.standard(0).cards(), a.cards());

        List<List<Card>> hands = a.dealAll(5);
        assertEquals(List.of(11, 11, 10, 10, 10), hands.stream().map(List::size).toList());
        assertTrue(a.isEmpty());

        Deck c = Deck.standard(0);
        List<List<Card>> five = c.deal(4, 5);
        assertEquals(4, five.size());
        assertEquals(32, c.size());
        assertThrows(IllegalArgumentException.class, () -> c.deal(4, 9));
    }

    @Test
    void dice() {
        Dice dice = new Dice(new Random(0));
        for (int r : dice.roll(1000)) {
            assertTrue(r >= 1 && r <= 6);
        }
        Dice.Order<String> order = dice.decideOrder(List.of("Alice", "Bob", "Carol", "Dave"), s -> s);
        assertEquals(4, order.order().size());
        assertEquals(4, new HashSet<>(order.order()).size());
        System.out.println(order.log() + " -> " + order.order());
    }
}
