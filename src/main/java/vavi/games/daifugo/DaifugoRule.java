/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.daifugo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

import vavi.games.table.GameRule;
import vavi.games.table.GameState;
import vavi.games.table.Strategy;


/**
 * 大富豪 (Daifugo, a.k.a. 大貧民) rule plugin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public class DaifugoRule implements GameRule {

    public static final String OPTION_JOKERS = "jokers";
    public static final String OPTION_EIGHT_CUT = "eight_cut";
    public static final String OPTION_REVOLUTION = "revolution";
    public static final String OPTION_SPADE3 = "spade3";
    public static final String OPTION_COVER_WITH_MORE = "cover_with_more";

    static final Map<String, String> DEFAULT_OPTIONS = Map.of(OPTION_JOKERS, "1", OPTION_EIGHT_CUT, "true",
            OPTION_REVOLUTION, "true", OPTION_SPADE3, "true", OPTION_COVER_WITH_MORE, "false");

    @Override
    public String id() {
        return "daifugo";
    }

    @Override
    public String name() {
        return "大富豪 (Daifugo)";
    }

    @Override
    public String rules() {
        return """
                大富豪 (Daifugo): get rid of all your cards first.
                - All cards are dealt. Card power, weak to strong: 3 4 5 6 7 8 9 10 J Q K A 2, joker strongest.
                - The leader plays any combo: a single card, a group of the same rank (pair, triple, quad...),
                  or a sequence (階段) of 3+ consecutive ranks in the same suit. Jokers are wild in groups and sequences.
                - Next players must play the same kind and number of cards, stronger; or pass.
                  A player who passed sits out until the field is cleared.
                - When everyone else has passed, the field is cleared and the last player leads.
                - 8切り (eight_cut): playing any 8 clears the field immediately, the same player leads again.
                - 革命 (revolution): playing 4+ of a kind reverses the card order (the joker stays strongest).
                - スペ3返し (spade3): the ♠3 beats a single joker.
                - cover_with_more (off by default): a single or group may be covered by a stronger group
                  of more cards, e.g. ♠J ♥J ♦J on a pair of 7s. Sequences still need the same length.
                - Finishing order gives titles: 大富豪, 富豪, 平民..., 貧民, 大貧民.
                - From round 2: 大貧民 must give their 2 strongest cards to 大富豪, then 大富豪 chooses any 2 cards
                  to give back (action "give"). With 4+ players 貧民 and 富豪 do the same with 1 card.
                  Then 大貧民 leads. In round 1 the ♦3 holder leads.
                Moves: action "play" with card codes, e.g. ["S3","H3"], action "pass",
                or action "give" with card codes during the card exchange.
                Card codes: suit S H D C + rank A 2..10 J Q K (e.g. "D10", "CQ"); "JK" for a joker.
                """;
    }

    @Override
    public int minPlayers() {
        return 3;
    }

    @Override
    public int maxPlayers() {
        return 8;
    }

    @Override
    public Map<String, String> defaultOptions() {
        return DEFAULT_OPTIONS;
    }

    @Override
    public GameState newRound(RoundSetup setup) {
        return new DaifugoState(setup);
    }

    @Override
    public Map<String, String> cpuProfiles() {
        Map<String, String> profiles = new LinkedHashMap<>();
        for (DaifugoStrategy.Profile p : DaifugoStrategy.Profile.values()) {
            profiles.put(p.id(), p.description());
        }
        return profiles;
    }

    @Override
    public Strategy cpuStrategy(String profile, RandomGenerator random) {
        return new DaifugoStrategy(DaifugoStrategy.Profile.of(profile), random);
    }

    @Override
    public String title(int place, int players) {
        return Title.of(place, players).japanese();
    }
}
