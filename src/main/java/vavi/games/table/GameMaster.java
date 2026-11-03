/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import vavi.games.card.Dice;


/**
 * The game master (dealer): knows the game rules, opens tables and keeps player stats.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public final class GameMaster {

    private static final Logger logger = Logger.getLogger(GameMaster.class.getName());

    private final Map<String, GameRule> rules = new LinkedHashMap<>();
    private final Map<String, Table> tables = new ConcurrentHashMap<>();
    private final Map<String, PlayerStats> stats = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    /** loads rule plugins with {@link ServiceLoader} */
    public GameMaster() {
        ServiceLoader.load(GameRule.class).forEach(this::register);
    }

    /** registers a rule plugin */
    public void register(GameRule rule) {
        rules.put(rule.id(), rule);
        logger.log(Level.FINE, "rule registered: " + rule.id());
    }

    public Collection<GameRule> rules() {
        return rules.values();
    }

    public GameRule rule(String id) {
        GameRule rule = rules.get(id);
        if (rule == null) {
            throw new IllegalArgumentException("unknown game: " + id + ", available: " + rules.keySet());
        }
        return rule;
    }

    /**
     * Opens a table. Seat order is decided by dice.
     *
     * @param seed random seed for reproducible games, null for random
     * @param options rule options, overrides {@link GameRule#defaultOptions()}
     */
    public Table createTable(String ruleId, List<Player> players, Long seed, Map<String, String> options) {
        GameRule rule = rule(ruleId);
        if (players.size() < rule.minPlayers() || players.size() > rule.maxPlayers()) {
            throw new IllegalArgumentException(rule.name() + " needs " + rule.minPlayers() + ".." + rule.maxPlayers()
                    + " players, got " + players.size());
        }
        if (new HashSet<>(players.stream().map(Player::name).toList()).size() != players.size()) {
            throw new IllegalArgumentException("player names must be unique: " + players);
        }
        Map<String, String> merged = new HashMap<>(rule.defaultOptions());
        if (options != null) {
            options.forEach((k, v) -> {
                if (!merged.containsKey(k)) {
                    throw new IllegalArgumentException("unknown option: " + k + ", available: " + rule.defaultOptions());
                }
                merged.put(k, v);
            });
        }
        Random random = seed != null ? new Random(seed) : new Random();
        Dice.Order<Player> order = new Dice(random).decideOrder(players, Player::name);
        List<String> setupLog = new ArrayList<>(order.log());
        setupLog.add("seat order: " + order.order().stream().map(Player::name).toList());

        String id = "t" + sequence.incrementAndGet();
        Table table = new Table(id, rule, order.order(), random, merged, setupLog, this::recordStats);
        tables.put(id, table);
        players.forEach(p -> stats.computeIfAbsent(p.name(), PlayerStats::new));
        return table;
    }

    public Table table(String id) {
        Table table = tables.get(id);
        if (table == null) {
            throw new IllegalArgumentException("no such table: " + id + ", open tables: " + tables.keySet());
        }
        return table;
    }

    public Collection<Table> tables() {
        return tables.values();
    }

    /** closes a table, stats are kept */
    public void closeTable(String id) {
        if (tables.remove(id) == null) {
            throw new IllegalArgumentException("no such table: " + id);
        }
    }

    /** */
    public PlayerStats stats(String player) {
        PlayerStats s = stats.get(player);
        if (s == null) {
            throw new IllegalArgumentException("no stats for player: " + player);
        }
        return s;
    }

    public Collection<PlayerStats> allStats() {
        return stats.values();
    }

    private void recordStats(Table table) {
        List<String> ranking = table.lastRanking();
        int n = ranking.size();
        for (int i = 0; i < n; i++) {
            String name = ranking.get(i);
            stats.computeIfAbsent(name, PlayerStats::new).record(i + 1, n, table.rule().title(i + 1, n));
        }
        logger.log(Level.FINER, "round finished at " + table.id() + ": " + ranking);
    }
}
