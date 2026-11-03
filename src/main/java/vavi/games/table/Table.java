/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;


/**
 * A table where players play a game, round after round.
 * The table routes moves to the {@link GameState} and plays cpu seats automatically.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public final class Table {

    /** safety limit for cpu auto play */
    private static final int MAX_AUTO_MOVES = 10_000;

    private final String id;
    private final GameRule rule;
    private final List<Player> seats;
    private final RandomGenerator random;
    private final Map<String, String> options;
    private final Consumer<Table> onRoundFinished;
    private final List<String> setupLog;
    /** cpu seat name -> strategy */
    private final Map<String, Strategy> strategies = new HashMap<>();

    private int round;
    private GameState state;
    private List<String> lastRanking = List.of();
    private boolean recorded;

    Table(String id, GameRule rule, List<Player> seats, RandomGenerator random, Map<String, String> options,
          List<String> setupLog, Consumer<Table> onRoundFinished) {
        this.id = id;
        this.rule = rule;
        this.seats = List.copyOf(seats);
        this.random = random;
        this.options = Map.copyOf(options);
        this.setupLog = List.copyOf(setupLog);
        this.onRoundFinished = onRoundFinished;
        for (Player p : this.seats) {
            if (p.cpu()) {
                strategies.put(p.name(), rule.cpuStrategy(p.profile(), random));
            }
        }
    }

    public String id() { return id; }
    public GameRule rule() { return rule; }
    public List<Player> seats() { return seats; }
    public int round() { return round; }
    public Map<String, String> options() { return options; }
    /** how the seating was decided */
    public List<String> setupLog() { return setupLog; }
    /** null before the first round */
    public GameState state() { return state; }
    /** ranking of the last finished round, winner first */
    public List<String> lastRanking() { return lastRanking; }

    /** */
    public synchronized boolean isRoundInProgress() {
        return state != null && !state.isFinished();
    }

    /**
     * Deals a new round and lets cpu players move until it's a human's turn.
     *
     * @return events
     */
    public synchronized List<String> startRound() {
        if (isRoundInProgress()) {
            throw new IllegalStateException("round " + round + " is still in progress");
        }
        round++;
        recorded = false;
        state = rule.newRound(new GameRule.RoundSetup(seats, round, lastRanking, random, options));
        List<String> events = new ArrayList<>(state.log());
        events.addAll(autoPlay());
        return events;
    }

    /**
     * Applies a human player's move, then lets cpu players move until it's a human's turn again.
     *
     * @return events
     * @throws IllegalArgumentException on illegal move
     */
    public synchronized List<String> play(String player, Move move) {
        requireRound();
        Player p = seat(player);
        if (p.cpu()) {
            throw new IllegalArgumentException(player + " is played by the game master (cpu)");
        }
        List<String> events = new ArrayList<>(state.apply(player, move));
        checkFinished();
        events.addAll(autoPlay());
        return events;
    }

    /** lets cpu players move while it is their turn */
    public synchronized List<String> autoPlay() {
        requireRound();
        List<String> events = new ArrayList<>();
        int count = 0;
        while (!state.isFinished() && seat(state.currentPlayer()).cpu()) {
            if (++count > MAX_AUTO_MOVES) {
                throw new IllegalStateException("cpu auto play does not terminate");
            }
            String current = state.currentPlayer();
            events.addAll(state.apply(current, strategies.get(current).choose(state, current)));
            checkFinished();
        }
        return events;
    }

    private void checkFinished() {
        if (state.isFinished() && !recorded) {
            recorded = true;
            lastRanking = List.copyOf(state.ranking());
            onRoundFinished.accept(this);
        }
    }

    private void requireRound() {
        if (state == null) {
            throw new IllegalStateException("no round started, call start_round first");
        }
    }

    /** */
    public Player seat(String name) {
        return seats.stream().filter(p -> p.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no such player at table " + id + ": " + name));
    }

    /** instruction for the client LLM playing a cpu seat */
    public String persona(Player p) {
        return p.name() + " is a cpu seat played by you (the client). Play it in the style of profile '" + p.profile()
                + "': " + rule.cpuProfiles().get(p.profile())
                + ". Decide only from " + p.name() + "'s own view, as a fair opponent.";
    }

    /** text summary */
    public synchronized String render(String viewer) {
        StringBuilder sb = new StringBuilder();
        sb.append("table ").append(id).append(" [").append(rule.name()).append("] seats: ").append(seats).append('\n');
        if (state == null) {
            sb.append("(no round started)\n");
        } else {
            sb.append(state.render(viewer));
        }
        if (viewer != null && !viewer.isEmpty()) {
            Player p = seat(viewer);
            if (p.control() == Player.Control.LLM) {
                sb.append(persona(p)).append('\n');
            }
        }
        return sb.toString();
    }
}
