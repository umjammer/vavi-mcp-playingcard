/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.daifugo;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import vavi.games.card.Card;
import vavi.games.table.Move;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Scripted 大富豪 scenarios with fixed hands.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
class DaifugoRuleTest {

    static DaifugoState state(String leader, Map<String, String> hands) {
        List<String> names = List.copyOf(new java.util.TreeMap<>(hands).keySet());
        Map<String, List<Card>> cards = new java.util.HashMap<>();
        hands.forEach((k, v) -> cards.put(k, Card.parseAll(v)));
        return new DaifugoState(names, cards, leader, Map.of());
    }

    static List<String> play(DaifugoState s, String player, String codes) {
        List<String> events = s.apply(player, Move.play(Card.parseAll(codes)));
        events.forEach(e -> System.out.println("  " + e));
        return events;
    }

    static List<String> pass(DaifugoState s, String player) {
        List<String> events = s.apply(player, Move.pass());
        events.forEach(e -> System.out.println("  " + e));
        return events;
    }

    @Test
    void revolution() {
        DaifugoState s = state("A", Map.of(
                "A", "S5 H5 D5 C5 H9 S3 HK",
                "B", "S6 JK D10",
                "C", "SK CQ H4 D4"));
        System.out.print(s.render(null));

        play(s, "A", "S5 H5 D5 C5");
        assertTrue(s.revolution(), "4 of a kind starts a revolution");
        assertEquals(List.of(Move.pass()), s.legalMoves("B"), "nobody can beat a quad");
        pass(s, "B");
        pass(s, "C");
        assertEquals("A", s.currentPlayer());
        assertNull(s.field());

        play(s, "A", "H9");
        play(s, "B", "S6");  // weaker card wins during a revolution
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> play(s, "C", "SK"));
        assertTrue(e.getMessage().contains("does not beat"), e.getMessage());
        play(s, "C", "H4");
        assertThrows(IllegalArgumentException.class, () -> play(s, "A", "HK"));
        pass(s, "A");
        play(s, "B", "JK"); // joker stays strongest
        pass(s, "C");
        // A already passed this trick, so the field is cleared and B leads
        assertEquals("B", s.currentPlayer());
        assertNull(s.field());

        play(s, "B", "D10");
        assertEquals(List.of("B"), s.ranking());
        System.out.print(s.render(null));
    }

    @Test
    void spade3AndEightCut() {
        DaifugoState s = state("A", Map.of(
                "A", "JK S4 H8 D9",
                "B", "S3 H5 D6",
                "C", "C7 D7 HQ"));
        play(s, "A", "JK");
        assertTrue(s.legalMoves("B").contains(Move.play(Card.parseAll("S3"))), "スペ3返し");
        assertThrows(IllegalArgumentException.class, () -> play(s, "B", "H5"));
        play(s, "B", "S3");
        pass(s, "C");
        List<String> events = play(s, "A", "H8");
        assertTrue(events.stream().anyMatch(x -> x.contains("8切り")));
        assertEquals("A", s.currentPlayer(), "8切り: the same player leads again");
        assertNull(s.field());
    }

    @Test
    void sequencesAndJokers() {
        DaifugoState s = state("A", Map.of(
                "A", "S3 S4 JK DK",
                "B", "H6 H7 H8 C2",
                "C", "D9 D10 DJ SQ"));
        List<Move> moves = s.legalMoves("A");
        assertTrue(moves.contains(Move.play(Card.parseAll("S3 S4 JK1"))));
        play(s, "A", "S3 S4 JK");
        play(s, "B", "H6 H7 H8"); // includes an 8 -> 8切り
        assertEquals("B", s.currentPlayer());
        play(s, "B", "C2");
        assertEquals(List.of("B"), s.ranking());
        // B went out with the field open; C and A respond
        assertEquals("C", s.currentPlayer());
        pass(s, "C");
        pass(s, "A");
        // the leader went out, so the next active player after B leads
        assertEquals("C", s.currentPlayer());
    }

    @Test
    void errors() {
        DaifugoState s = state("A", Map.of("A", "S3 S4", "B", "H5 H6", "C", "D7 D9"));
        assertThrows(IllegalArgumentException.class, () -> pass(s, "A"), "leader can't pass");
        assertThrows(IllegalArgumentException.class, () -> play(s, "B", "H5"), "not your turn");
        assertThrows(IllegalArgumentException.class, () -> play(s, "A", "S5"), "not in hand");
        assertThrows(IllegalArgumentException.class, () -> play(s, "A", "S3 S4"), "not a combo");
        assertFalse(s.legalMoves("A").contains(Move.pass()));
        assertTrue(s.legalMoves("B").isEmpty());
    }

    @Test
    void lastPlayerStanding() {
        DaifugoState s = state("A", Map.of("A", "S3", "B", "H5", "C", "D7 D9"));
        play(s, "A", "S3");
        play(s, "B", "H5");
        // A and B are out, the round ends at once and C is the 大貧民
        assertTrue(s.isFinished());
        assertEquals(List.of("A", "B", "C"), s.ranking());
        assertEquals(Title.DAIHINMIN, Title.of(3, 3));
        assertEquals(Title.HEIMIN, Title.of(2, 3));
        assertEquals(Title.FUGO, Title.of(2, 5));
        assertEquals(Title.HEIMIN, Title.of(3, 5));
        assertEquals(Title.HINMIN, Title.of(4, 5));
    }
}
