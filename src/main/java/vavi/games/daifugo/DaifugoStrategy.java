/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.daifugo;

import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

import vavi.games.card.Card;
import vavi.games.table.GameState;
import vavi.games.table.Move;
import vavi.games.table.Strategy;


/**
 * Rule based cpu for 大富豪 with selectable profiles.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public class DaifugoStrategy implements Strategy {

    /** strength of A (normal order), K is 10, 2 is 12, joker 13 */
    private static final int ACE = 11;
    private static final int NEVER = Integer.MAX_VALUE;

    /**
     * Strategy profiles.
     *
     * @see #description()
     */
    public enum Profile {
        BALANCED("plays its weakest cards first, gets rid of pairs and sequences when leading, "
                + "keeps jokers and 2s until its hand is small",
                4, 3, ACE, 6, true, false),
        AGGRESSIVE("plays whenever it can, dumps as many cards as possible at once, uses jokers freely "
                + "and starts a revolution (革命) when its hand is weak",
                7, 5, NEVER, 0, false, true),
        CAUTIOUS("passes often to keep A, 2 and jokers, spending strong cards only near the end",
                3, 1, ACE - 1, 4, true, false),
        RANDOM("picks a random legal move",
                0, 0, NEVER, 0, false, false);

        private final String description;
        /** hand size at or below which strong cards and jokers are spent freely */
        final int endGame;
        /** when leading, how many strength points one extra card in a combo is worth */
        final int dumpWeight;
        /** when following, pass rather than play a combo stronger than this ... */
        final int saveAbove;
        /** ... while the hand is bigger than this */
        final int saveWhileOver;
        /** when following, pass rather than spend a joker before the end game */
        final boolean holdJokers;
        /** play 4 of a kind for a revolution when the hand is weak */
        final boolean seekRevolution;

        Profile(String description, int endGame, int dumpWeight, int saveAbove, int saveWhileOver,
                boolean holdJokers, boolean seekRevolution) {
            this.description = description;
            this.endGame = endGame;
            this.dumpWeight = dumpWeight;
            this.saveAbove = saveAbove;
            this.saveWhileOver = saveWhileOver;
            this.holdJokers = holdJokers;
            this.seekRevolution = seekRevolution;
        }

        /** profile name used in tools, e.g. "balanced" */
        public String id() {
            return name().toLowerCase();
        }

        /** how the profile plays, also used as the persona for an LLM playing the seat */
        public String description() {
            return description;
        }

        /** @param id null for {@link #BALANCED} */
        public static Profile of(String id) {
            if (id == null) {
                return BALANCED;
            }
            for (Profile p : values()) {
                if (p.id().equalsIgnoreCase(id)) {
                    return p;
                }
            }
            throw new IllegalArgumentException("unknown profile: " + id);
        }
    }

    private final Profile profile;
    private final RandomGenerator random;

    /** */
    public DaifugoStrategy(Profile profile, RandomGenerator random) {
        this.profile = profile;
        this.random = random;
    }

    public Profile profile() {
        return profile;
    }

    @Override
    public Move choose(GameState gameState, String player) {
        DaifugoState state = (DaifugoState) gameState;
        List<Move> moves = state.legalMoves(player);
        if (profile == Profile.RANDOM) {
            return moves.get(random.nextInt(moves.size()));
        }
        List<Card> hand = state.hand(player);
        boolean leading = state.field() == null;
        boolean endGame = hand.size() <= profile.endGame;

        List<Move> plays = moves.stream().filter(m -> !m.isPass()).toList();
        if (plays.isEmpty()) {
            return Move.pass();
        }
        // going out wins
        for (Move m : plays) {
            if (m.cards().size() == hand.size()) {
                return m;
            }
        }
        // a weak hand turns strong after a revolution
        if (profile.seekRevolution && !state.revolution()
                && hand.stream().mapToInt(Combo::power).average().orElse(0) < 6) {
            for (Move m : plays) {
                if (state.evaluate(m.cards()).isRevolution()) {
                    return m;
                }
            }
        }
        // when leading, getting rid of more cards at once is worth a few points of strength
        Comparator<Move> order = Comparator
                .comparingInt((Move m) -> !endGame && usesJoker(m) ? 1 : 0)
                .thenComparingInt(m -> state.evaluate(m.cards()).strength() - (leading ? profile.dumpWeight * (m.cards().size() - 1) : 0))
                .thenComparingInt(m -> leading ? -m.cards().size() : m.cards().size());
        Move best = plays.stream().min(order).orElseThrow();

        if (!leading && !endGame) {
            int strength = state.evaluate(best.cards()).strength();
            if ((profile.holdJokers && usesJoker(best))
                    || (strength > profile.saveAbove && hand.size() > profile.saveWhileOver)) {
                return Move.pass();
            }
        }
        return best;
    }

    private static boolean usesJoker(Move m) {
        return m.cards().stream().anyMatch(Card::isJoker);
    }
}
