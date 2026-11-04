/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.daifugo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vavi.games.card.Card;
import vavi.games.card.Deck;
import vavi.games.card.Rank;
import vavi.games.card.Suit;
import vavi.games.table.GameRule;
import vavi.games.table.GameState;
import vavi.games.table.Move;
import vavi.games.table.Player;


/**
 * One round of 大富豪.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public final class DaifugoState implements GameState {

    /** weak to strong, then by suit */
    static final Comparator<Card> HAND_ORDER = Comparator.<Card>comparingInt(Combo::power)
            .thenComparing(Comparator.naturalOrder());

    private final int round;
    private final List<String> names;
    private final Map<String, List<Card>> hands = new LinkedHashMap<>();
    private final Map<String, Title> previousTitles = new HashMap<>();
    private final boolean eightCut;
    private final boolean revolutionRule;
    private final boolean spade3;
    private final boolean coverWithMore;

    /** a richer player still has to give cards back to a poorer one */
    private record GiveBack(String rich, String poor, int count) {}

    /** exchange phase: give backs not done yet, the first one is the current */
    private final Deque<GiveBack> giveBacks = new ArrayDeque<>();
    /** who leads after the exchange */
    private String leader;

    /** seat index of the player to move, -1 when finished */
    private int turn;
    /** combo on the field, null when the field is empty */
    private Combo field;
    /** who played the field combo */
    private String lastPlayer;
    /** players who passed since the last play (they sit out until the trick is cleared) */
    private final Set<String> passed = new HashSet<>();
    private boolean revolution;
    private final List<String> finished = new ArrayList<>();
    private final List<String> log = new ArrayList<>();

    /** */
    DaifugoState(GameRule.RoundSetup setup) {
        this.round = setup.round();
        this.names = setup.seats().stream().map(Player::name).toList();
        Map<String, String> options = setup.options();
        int jokers = Integer.parseInt(options.getOrDefault(DaifugoRule.OPTION_JOKERS, "1"));
        if (jokers < 0 || jokers > 2) {
            throw new IllegalArgumentException("jokers must be 0..2: " + jokers);
        }
        this.eightCut = flag(options, DaifugoRule.OPTION_EIGHT_CUT);
        this.revolutionRule = flag(options, DaifugoRule.OPTION_REVOLUTION);
        this.spade3 = flag(options, DaifugoRule.OPTION_SPADE3);
        this.coverWithMore = flag(options, DaifugoRule.OPTION_COVER_WITH_MORE);

        log("=== 大富豪 round " + round + " ===");
        Deck deck = Deck.standard(jokers).shuffle(setup.random());
        List<List<Card>> dealt = deck.dealAll(names.size());
        for (int i = 0; i < names.size(); i++) {
            List<Card> hand = new ArrayList<>(dealt.get(i));
            hand.sort(HAND_ORDER);
            hands.put(names.get(i), hand);
        }
        log("dealt " + (52 + jokers) + " cards: " + names.stream().map(n -> n + "=" + hands.get(n).size()).toList());

        List<String> prev = setup.previousRanking();
        int n = names.size();
        if (prev.size() == n) {
            for (int i = 0; i < n; i++) {
                previousTitles.put(prev.get(i), Title.of(i + 1, n));
            }
            tribute(prev.get(n - 1), prev.getFirst(), 2);
            if (n >= 4) {
                tribute(prev.get(n - 2), prev.get(1), 1);
            }
            leader = prev.get(n - 1);
            nextGiveBack();
        } else {
            Card d3 = Card.of(Suit.DIAMONDS, Rank.THREE);
            String holder = names.stream().filter(name -> hands.get(name).contains(d3)).findFirst().orElse(names.getFirst());
            turn = names.indexOf(holder);
            log(holder + " has ♦3 and leads");
        }
    }

    /** for scenario tests: fixed hands, no dealing, no exchange */
    DaifugoState(List<String> names, Map<String, List<Card>> fixedHands, String leader, Map<String, String> options) {
        this.round = 1;
        this.names = List.copyOf(names);
        this.eightCut = flag(options, DaifugoRule.OPTION_EIGHT_CUT);
        this.revolutionRule = flag(options, DaifugoRule.OPTION_REVOLUTION);
        this.spade3 = flag(options, DaifugoRule.OPTION_SPADE3);
        this.coverWithMore = flag(options, DaifugoRule.OPTION_COVER_WITH_MORE);
        for (String name : names) {
            List<Card> hand = new ArrayList<>(fixedHands.get(name));
            hand.sort(HAND_ORDER);
            hands.put(name, hand);
        }
        turn = names.indexOf(leader);
        log("scenario: " + leader + " leads");
    }

    private static boolean flag(Map<String, String> options, String key) {
        return Boolean.parseBoolean(options.getOrDefault(key, DaifugoRule.DEFAULT_OPTIONS.get(key)));
    }

    /** カード交換 step 1: the poorer must give their strongest cards, the richer chooses what to give back later */
    private void tribute(String poor, String rich, int count) {
        List<Card> poorHand = hands.get(poor);
        List<Card> give = new ArrayList<>(poorHand.subList(poorHand.size() - count, poorHand.size()));
        poorHand.removeAll(give);
        hands.get(rich).addAll(give);
        hands.get(rich).sort(HAND_ORDER);
        log("exchange: " + poor + "(" + previousTitles.get(poor) + ") gives " + Card.symbols(give)
                + " to " + rich + "(" + previousTitles.get(rich) + ")");
        giveBacks.add(new GiveBack(rich, poor, count));
    }

    /** moves to the next give back, or starts the play when all are done */
    private void nextGiveBack() {
        GiveBack g = giveBacks.peek();
        if (g != null) {
            turn = names.indexOf(g.rich());
            log(g.rich() + "(" + previousTitles.get(g.rich()) + ") chooses " + g.count() + " card" + (g.count() > 1 ? "s" : "")
                    + " to give back to " + g.poor() + "(" + previousTitles.get(g.poor()) + ")");
        } else {
            turn = names.indexOf(leader);
            log(leader + " (大貧民) leads");
        }
    }

    /** カード交換 step 2: the richer gives back any cards they like */
    private void giveBack(String player, List<Card> requested) {
        GiveBack g = giveBacks.peek();
        if (g == null) {
            throw new IllegalArgumentException("give is only for the card exchange at the start of a round");
        }
        if (requested.size() != g.count()) {
            throw new IllegalArgumentException(player + " must give exactly " + g.count() + " card" + (g.count() > 1 ? "s" : "")
                    + " to " + g.poor() + ", got " + requested.size());
        }
        List<Card> cards = resolve(player, requested);
        hands.get(player).removeAll(cards);
        hands.get(g.poor()).addAll(cards);
        hands.get(g.poor()).sort(HAND_ORDER);
        log("exchange: " + player + "(" + previousTitles.get(player) + ") gives back " + Card.symbols(cards)
                + " to " + g.poor() + "(" + previousTitles.get(g.poor()) + ")");
        giveBacks.poll();
        nextGiveBack();
    }

    private void log(String event) {
        log.add(event);
    }

    private boolean active(String name) {
        return !finished.contains(name);
    }

    private long activeCount() {
        return names.stream().filter(this::active).count();
    }

    // ---- queries (also used by strategies)

    public int round() { return round; }

    public boolean revolution() { return revolution; }

    /** true while a richer player has to choose cards to give back (the round starts after it) */
    public boolean exchanging() { return !giveBacks.isEmpty(); }

    /** number of cards the current player must give back, 0 when not exchanging */
    public int giveBackCount() { return giveBacks.isEmpty() ? 0 : giveBacks.peek().count(); }

    /** who receives the given back cards, null when not exchanging */
    public String giveBackTo() { return giveBacks.isEmpty() ? null : giveBacks.peek().poor(); }

    /** player names in seat order */
    public List<String> players() { return names; }

    /** sorted weak to strong, unmodifiable */
    public List<Card> hand(String player) {
        List<Card> hand = hands.get(player);
        if (hand == null) {
            throw new IllegalArgumentException("no such player: " + player);
        }
        return List.copyOf(hand);
    }

    /** combo on the field re-evaluated with the current revolution state, null when empty */
    public Combo field() {
        if (field == null) {
            return null;
        }
        return Combo.interpretations(field.cards(), revolution).stream()
                .filter(c -> c.type() == field.type()).findFirst().orElse(field);
    }

    /** who played the field combo, null when the field is empty */
    public String fieldOwner() {
        return field == null ? null : lastPlayer;
    }

    /** title from the previous round, null on the first round */
    public Title previousTitle(String player) {
        return previousTitles.get(player);
    }

    /**
     * Evaluates cards against the current field.
     *
     * @return the reading of the cards that can be played now, null if they can't be played
     */
    public Combo evaluate(List<Card> cards) {
        Combo f = field();
        return Combo.interpretations(cards, revolution).stream()
                .filter(c -> f == null || c.beats(f, spade3, coverWithMore))
                .findFirst().orElse(null);
    }

    // ---- GameState

    @Override
    public String currentPlayer() {
        return turn < 0 ? null : names.get(turn);
    }

    @Override
    public boolean isFinished() {
        return turn < 0;
    }

    @Override
    public List<Move> legalMoves(String player) {
        if (isFinished() || !player.equals(currentPlayer())) {
            return List.of();
        }
        if (exchanging()) {
            List<Move> gives = new ArrayList<>();
            choose(hands.get(player), giveBackCount(), 0, new ArrayList<>(), gives);
            return gives;
        }
        Map<String, Combo> playable = new LinkedHashMap<>();
        for (List<Card> candidate : candidates(hands.get(player))) {
            Combo combo = evaluate(candidate);
            if (combo != null) {
                List<Card> sorted = new ArrayList<>(candidate);
                sorted.sort(HAND_ORDER);
                playable.putIfAbsent(Card.codes(sorted), new Combo(combo.type(), List.copyOf(sorted), combo.strength()));
            }
        }
        List<Move> moves = new ArrayList<>(playable.values().stream()
                .sorted(Comparator.comparingInt(Combo::strength)
                        .thenComparingLong(c -> c.cards().stream().filter(Card::isJoker).count())
                        .thenComparingInt(Combo::size))
                .map(c -> Move.play(c.cards())).toList());
        if (field != null) {
            moves.add(Move.pass());
        }
        return moves;
    }

    /** every way to pick {@code count} cards, in hand order */
    private static void choose(List<Card> hand, int count, int from, List<Card> picked, List<Move> result) {
        if (picked.size() == count) {
            result.add(Move.give(picked));
            return;
        }
        for (int i = from; i < hand.size(); i++) {
            picked.add(hand.get(i));
            choose(hand, count, i + 1, picked, result);
            picked.removeLast();
        }
    }

    /** every card set that may form a combo: groups (with jokers) and sequences (with jokers) */
    private static List<List<Card>> candidates(List<Card> hand) {
        List<Card> jokers = hand.stream().filter(Card::isJoker).toList();
        Map<Rank, List<Card>> byRank = new LinkedHashMap<>();
        for (Card c : hand) {
            if (c instanceof Card.Standard s) {
                byRank.computeIfAbsent(s.rank(), _ -> new ArrayList<>()).add(c);
            }
        }
        List<List<Card>> result = new ArrayList<>();
        // jokers alone
        for (int j = 1; j <= jokers.size(); j++) {
            result.add(new ArrayList<>(jokers.subList(0, j)));
        }
        // groups
        for (List<Card> same : byRank.values()) {
            for (int mask = 1; mask < (1 << same.size()); mask++) {
                List<Card> subset = new ArrayList<>();
                for (int i = 0; i < same.size(); i++) {
                    if ((mask & (1 << i)) != 0) {
                        subset.add(same.get(i));
                    }
                }
                for (int j = 0; j <= jokers.size(); j++) {
                    List<Card> cards = new ArrayList<>(subset);
                    cards.addAll(jokers.subList(0, j));
                    result.add(cards);
                }
            }
        }
        // sequences
        for (Suit suit : Suit.values()) {
            Card[] byPower = new Card[13];
            for (Card c : hand) {
                if (c instanceof Card.Standard s && s.suit() == suit) {
                    byPower[Combo.power(s.rank())] = c;
                }
            }
            for (int lo = 0; lo <= 10; lo++) {
                for (int hi = lo + 2; hi <= 12; hi++) {
                    List<Card> cards = new ArrayList<>();
                    int missing = 0;
                    for (int p = lo; p <= hi; p++) {
                        if (byPower[p] != null) {
                            cards.add(byPower[p]);
                        } else {
                            missing++;
                        }
                    }
                    if (missing > jokers.size()) {
                        break; // a longer span misses even more
                    }
                    if (cards.isEmpty()) {
                        continue;
                    }
                    cards.addAll(jokers.subList(0, missing));
                    result.add(cards);
                }
            }
        }
        return result;
    }

    @Override
    public List<String> apply(String player, Move move) {
        if (isFinished()) {
            throw new IllegalArgumentException("the round is over");
        }
        if (!player.equals(currentPlayer())) {
            throw new IllegalArgumentException("not " + player + "'s turn, it's " + currentPlayer() + "'s turn");
        }
        int mark = log.size();
        if (exchanging() && !move.action().equals(Move.GIVE)) {
            throw new IllegalArgumentException("card exchange first: " + player + " must give " + giveBackCount()
                    + " card(s) to " + giveBackTo() + " with action give");
        }
        switch (move.action()) {
            case Move.GIVE -> giveBack(player, move.cards());
            case Move.PASS -> pass(player);
            case Move.PLAY -> play(player, move.cards());
            default -> throw new IllegalArgumentException("unknown action: " + move.action() + ", use play, pass or give");
        }
        return List.copyOf(log.subList(mark, log.size()));
    }

    private void pass(String player) {
        if (field == null) {
            throw new IllegalArgumentException(player + " leads this trick and can't pass, play some cards");
        }
        passed.add(player);
        log(player + " passes");
        advance();
    }

    private void play(String player, List<Card> requested) {
        if (requested.isEmpty()) {
            throw new IllegalArgumentException("no cards given, to pass use the pass action");
        }
        List<Card> cards = resolve(player, requested);
        Combo combo = evaluate(cards);
        if (combo == null) {
            if (Combo.interpretations(cards, revolution).isEmpty()) {
                throw new IllegalArgumentException(Card.symbols(cards) + " is not a valid combination"
                        + " (single, same rank group, or 3+ consecutive same suit sequence)");
            }
            throw new IllegalArgumentException(Card.symbols(cards) + " does not beat the field " + field()
                    + " (need the same kind and " + (coverWithMore ? "at least the same" : "the same") + " number of cards, and stronger"
                    + (revolution ? ", revolution is on" : "") + ")");
        }
        hands.get(player).removeAll(cards);
        field = combo;
        lastPlayer = player;
        log(player + " plays " + combo + describe(combo) + " [" + hands.get(player).size() + " left]");

        if (revolutionRule && combo.isRevolution()) {
            revolution = !revolution;
            log("革命! revolution is now " + (revolution ? "ON: card order reversed" : "OFF: card order back to normal"));
        }
        if (hands.get(player).isEmpty()) {
            finished.add(player);
            int place = finished.size();
            log(player + " goes out! place " + place + " " + Title.of(place, names.size()));
        }
        if (activeCount() <= 1) {
            finishRound();
        } else if (eightCut && combo.hasEight()) {
            log("8切り! the field is cleared");
            clearTrick();
        } else {
            advance();
        }
    }

    private static String describe(Combo combo) {
        return switch (combo.type()) {
            case SINGLE -> "";
            case GROUP -> " (" + combo.size() + " of a kind)";
            case SEQUENCE -> " (sequence)";
        };
    }

    /** finds the requested cards in the hand, "JK" matches any joker */
    private List<Card> resolve(String player, List<Card> requested) {
        List<Card> hand = new ArrayList<>(hands.get(player));
        List<Card> result = new ArrayList<>();
        List<Card> jokers = new ArrayList<>();
        for (Card c : requested) {
            if (c.isJoker()) {
                jokers.add(c);
            } else if (hand.remove(c)) {
                result.add(c);
            } else {
                throw new IllegalArgumentException(player + " does not have " + c.code() + " " + c.symbol());
            }
        }
        for (Card j : jokers) {
            Card found = hand.contains(j) ? j : hand.stream().filter(Card::isJoker).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(player + " does not have a joker"));
            hand.remove(found);
            result.add(found);
        }
        return result;
    }

    /** moves the turn to the next player who can still respond, or clears the trick */
    private void advance() {
        int n = names.size();
        for (int i = 1; i <= n; i++) {
            int idx = (turn + i) % n;
            String name = names.get(idx);
            if (active(name) && !passed.contains(name) && !name.equals(lastPlayer)) {
                turn = idx;
                return;
            }
        }
        log("everyone passed, the field is cleared");
        clearTrick();
    }

    /** 場が流れる: the last player leads, or the next active player if they went out */
    private void clearTrick() {
        field = null;
        passed.clear();
        int n = names.size();
        int from = names.indexOf(lastPlayer);
        for (int i = 0; i < n; i++) {
            int idx = (from + i) % n;
            if (active(names.get(idx))) {
                turn = idx;
                log(names.get(idx) + " leads");
                return;
            }
        }
        throw new IllegalStateException("no active player");
    }

    private void finishRound() {
        names.stream().filter(this::active).forEach(name -> {
            finished.add(name);
            log(name + " is the last one, place " + finished.size() + " " + Title.of(finished.size(), names.size()));
        });
        field = null;
        turn = -1;
        StringBuilder sb = new StringBuilder("round " + round + " result:");
        for (int i = 0; i < finished.size(); i++) {
            sb.append(' ').append(i + 1).append('.').append(finished.get(i)).append('(').append(Title.of(i + 1, names.size())).append(')');
        }
        log(sb.toString());
    }

    @Override
    public List<String> ranking() {
        return List.copyOf(finished);
    }

    @Override
    public List<String> log() {
        return List.copyOf(log);
    }

    @Override
    public String render(String viewer) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- 大富豪 round ").append(round).append(" --- revolution: ").append(revolution ? "ON" : "off").append('\n');
        sb.append("field: ").append(field == null ? "(empty)" : field + " by " + lastPlayer).append('\n');
        int width = names.stream().mapToInt(String::length).max().orElse(4);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            sb.append(i == turn ? " > " : "   ");
            sb.append(String.format("%-" + width + "s", name));
            Title prev = previousTitles.get(name);
            if (prev != null) {
                sb.append(" [").append(prev).append(']');
            }
            if (!active(name)) {
                int place = finished.indexOf(name) + 1;
                sb.append("  out: place ").append(place).append(' ').append(Title.of(place, names.size()));
            } else {
                sb.append(String.format("  %2d cards", hands.get(name).size()));
                if (passed.contains(name)) {
                    sb.append(" (passed)");
                }
                if (viewer == null) {
                    sb.append("  ").append(Card.symbols(hands.get(name)));
                }
            }
            sb.append('\n');
        }
        if (viewer != null && hands.containsKey(viewer)) {
            List<Card> hand = hands.get(viewer);
            sb.append("hand of ").append(viewer).append(": ").append(Card.symbols(hand)).append('\n');
            sb.append("  codes: ").append(Card.codes(hand)).append('\n');
        }
        if (isFinished()) {
            sb.append("round finished: ").append(finished).append('\n');
        } else if (exchanging()) {
            sb.append("card exchange: ").append(currentPlayer()).append(" (").append(previousTitles.get(currentPlayer()))
                    .append(") chooses ").append(giveBackCount()).append(" card(s) to give back to ").append(giveBackTo())
                    .append(", use action give\n");
        } else {
            sb.append("turn: ").append(currentPlayer()).append(field == null ? " (leading)" : "").append('\n');
        }
        return sb.toString();
    }
}
