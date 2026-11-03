/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A playing card, either a standard card or a joker.
 * <p>
 * Every card has a short ASCII {@link #code() code} that is used to talk about
 * cards through the MCP interface:
 * <ul>
 * <li>standard card: suit letter + rank label, e.g. {@code S3}, {@code H10}, {@code DQ}, {@code CA}</li>
 * <li>joker: {@code JK1}, {@code JK2} (plain {@code JK} means "any joker" when picking from a hand)</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public sealed interface Card extends Comparable<Card> permits Card.Standard, Card.Joker {

    /** ASCII code, e.g. "S3", "H10", "JK1" */
    String code();

    /** human friendly symbol, e.g. "♠3", "♥10", "🃏" */
    String symbol();

    /** */
    default boolean isJoker() {
        return this instanceof Joker;
    }

    /** natural order: suit, then rank, jokers last */
    @Override
    default int compareTo(Card o) {
        return switch (this) {
            case Joker a when o instanceof Joker b -> Integer.compare(a.id(), b.id());
            case Joker _ -> 1;
            case Standard a when o instanceof Standard b ->
                    a.suit() != b.suit() ? a.suit().compareTo(b.suit()) : a.rank().compareTo(b.rank());
            case Standard _ -> -1;
        };
    }

    /** A standard card. */
    record Standard(Suit suit, Rank rank) implements Card {
        @Override
        public String code() {
            return suit.code() + rank.label();
        }

        @Override
        public String symbol() {
            return suit.symbol() + rank.label();
        }

        @Override
        public String toString() {
            return symbol();
        }
    }

    /** A joker, id distinguishes multiple jokers in a deck. */
    record Joker(int id) implements Card {
        @Override
        public String code() {
            return "JK" + id;
        }

        @Override
        public String symbol() {
            return "🃏";
        }

        @Override
        public String toString() {
            return symbol();
        }
    }

    /** factory */
    static Card of(Suit suit, Rank rank) {
        return new Standard(suit, rank);
    }

    /**
     * Parses a card code.
     *
     * @param code e.g. "S3", "h10", "DT", "JK1", "JK" (treated as JK1)
     */
    static Card parse(String code) {
        String c = code.trim().toUpperCase();
        if (c.startsWith("JK") || c.equals("JOKER")) {
            String n = c.startsWith("JK") ? c.substring(2) : "";
            return new Joker(n.isEmpty() ? 1 : Integer.parseInt(n));
        }
        if (c.length() < 2) {
            throw new IllegalArgumentException("bad card code: " + code);
        }
        return new Standard(Suit.of(c.charAt(0)), Rank.of(c.substring(1)));
    }

    /** @param codes space or comma separated codes */
    static List<Card> parseAll(String codes) {
        if (codes == null || codes.isBlank()) {
            return List.of();
        }
        List<Card> result = new ArrayList<>();
        Arrays.stream(codes.trim().split("[\\s,]+")).forEach(s -> result.add(parse(s)));
        return result;
    }

    /** "S3 H3" */
    static String codes(Collection<? extends Card> cards) {
        return cards.stream().map(Card::code).collect(Collectors.joining(" "));
    }

    /** "♠3 ♥3" */
    static String symbols(Collection<? extends Card> cards) {
        return cards.stream().map(Card::symbol).collect(Collectors.joining(" "));
    }
}
