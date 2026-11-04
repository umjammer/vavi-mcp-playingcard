/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.daifugo;

import java.util.List;

import org.junit.jupiter.api.Test;
import vavi.games.card.Card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * ComboTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
class ComboTest {

    static Combo combo(String codes, boolean revolution) {
        List<Combo> combos = Combo.interpretations(Card.parseAll(codes), revolution);
        return combos.isEmpty() ? null : combos.getFirst();
    }

    static Combo combo(String codes) {
        return combo(codes, false);
    }

    @Test
    void power() {
        assertEquals(0, Combo.power(Card.parse("S3")));
        assertEquals(10, Combo.power(Card.parse("SK")));
        assertEquals(11, Combo.power(Card.parse("SA")));
        assertEquals(12, Combo.power(Card.parse("S2")));
        assertEquals(Combo.JOKER, Combo.power(Card.parse("JK")));
    }

    @Test
    void singles() {
        assertTrue(combo("S2").beats(combo("SA"), true));
        assertFalse(combo("S3").beats(combo("S4"), true));
        assertTrue(combo("JK").beats(combo("S2"), true));
        assertTrue(combo("S3").beats(combo("JK"), true), "スペ3返し");
        assertFalse(combo("S3").beats(combo("JK"), false));
        assertFalse(combo("H3").beats(combo("JK"), true));
    }

    @Test
    void groups() {
        assertEquals(Combo.Type.GROUP, combo("S5 H5").type());
        assertEquals(Combo.Type.GROUP, combo("S5 JK").type());
        assertTrue(combo("S6 H6").beats(combo("S5 D5"), true));
        assertFalse(combo("S6 H6 D6").beats(combo("S5 D5"), true), "different size");
        assertEquals(null, combo("S5 H6"));
        assertTrue(combo("S5 H5 D5 C5").isRevolution());
    }

    @Test
    void coverWithMore() {
        assertFalse(combo("SJ HJ DJ").beats(combo("S7 H7"), true, false));
        assertTrue(combo("SJ HJ DJ").beats(combo("S7 H7"), true, true));
        assertTrue(combo("SJ HJ DJ CJ").beats(combo("S7 H7"), true, true));
        assertTrue(combo("SJ HJ").beats(combo("S7"), true, true));
        assertFalse(combo("S5 H5 D5").beats(combo("S7 H7"), true, true), "still needs to be stronger");
        assertFalse(combo("S7").beats(combo("S5 H5"), true, true), "fewer cards never cover more");
        assertFalse(combo("D4 D5 D6 D7").beats(combo("S3 S4 S5"), true, true), "sequences keep the same length");
    }

    @Test
    void revolution() {
        assertTrue(combo("S3", true).beats(combo("S2", true), true));
        assertTrue(combo("JK", true).beats(combo("S3", true), true), "joker stays strongest");
        assertTrue(combo("S4 H4", true).beats(combo("SK HK", true), true));
    }

    @Test
    void sequences() {
        Combo seq = Combo.interpretations(Card.parseAll("S3 S4 S5"), false).getFirst();
        assertEquals(Combo.Type.SEQUENCE, seq.type());
        assertEquals(0, seq.strength());

        assertEquals(Combo.Type.SEQUENCE, combo("HQ HK HA").type());
        assertEquals(Combo.Type.SEQUENCE, combo("HK HA H2").type());
        assertEquals(null, combo("H2 H3 H4"), "no wrap around");
        assertEquals(null, combo("S3 S4 H5"), "mixed suits");
        assertEquals(Combo.Type.SEQUENCE, combo("S3 JK S5").type(), "joker fills the gap");
        assertEquals(null, combo("S3 S6"));

        // ♠5 🃏 🃏 can be a triple or a sequence
        List<Combo> both = Combo.interpretations(Card.parseAll("S5 JK1 JK2"), false);
        assertEquals(List.of(Combo.Type.GROUP, Combo.Type.SEQUENCE), both.stream().map(Combo::type).toList());

        assertTrue(combo("D4 D5 D6").beats(combo("S3 S4 S5"), true));
        assertFalse(combo("D3 D4 D5").beats(combo("S3 S4 S5"), true));
        // spare joker goes to the strong end: ♦4 ♦5 🃏 = 4-5-6, strength 1
        assertEquals(1, combo("D4 D5 JK").strength());
        // during a revolution it goes to the other end: 3-4-5, whose weakest card is now the 5
        assertEquals(12 - 2, combo("D4 D5 JK", true).strength());
    }
}
