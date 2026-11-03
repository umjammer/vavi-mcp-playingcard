/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.mcp.playingcard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import vavi.games.card.Card;
import vavi.games.card.Dice;
import vavi.games.table.GameMaster;
import vavi.games.table.GameRule;
import vavi.games.table.Move;
import vavi.games.table.Player;
import vavi.games.table.PlayerStats;
import vavi.games.table.Table;


/**
 * MCP tool definitions backed by a {@link GameMaster}.
 * Transport independent so that it can be tested without a MCP client.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public final class PlayingCardTools {

    /**
     * A tool.
     *
     * @param name tool name
     * @param description for the AI client
     * @param inputSchema JSON schema of the arguments
     * @param handler returns text, throws {@link IllegalArgumentException} or {@link IllegalStateException} on user errors
     */
    public record Tool(String name, String description, Map<String, Object> inputSchema,
                       Function<Map<String, Object>, String> handler) {}

    private final GameMaster master;
    private final Random diceRandom = new Random();
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    /** */
    public PlayingCardTools(GameMaster master) {
        this.master = master;

        add("list_games", "List available card games with their rules, player counts and options.",
                schema(Map.of()), this::listGames);
        add("create_table", """
                Open a table for a game. Seat order is decided by dice. \
                players: seats you (the client) play as yourself. \
                cpu_players: seats the game master plays automatically with a built-in strategy. \
                llm_players: cpu seats whose turns are handed off to you; play them in the style of their profile. \
                cpu and llm entries are "Name" or "Name:profile" (profiles: see list_games).""",
                schema(Map.of(
                        "game", prop("string", "game id from list_games, e.g. daifugo"),
                        "players", array("string", "names of players controlled by the client"),
                        "cpu_players", array("string", "\"Name\" or \"Name:profile\", played automatically by the game master"),
                        "llm_players", array("string", "\"Name\" or \"Name:profile\", cpu seats played by you (the client) in the profile's style"),
                        "seed", prop("integer", "random seed for a reproducible game (optional)"),
                        "options", Map.of("type", "object", "description", "rule options (see list_games), values as strings",
                                "additionalProperties", Map.of("type", "string"))),
                        "game"), this::createTable);
        add("start_round", "Shuffle, deal and start the next round at a table. Built-in CPU players move until it's one of your seats' turn.",
                schema(Map.of("table", prop("string", "table id")), "table"), this::startRound);
        add("get_state", "Show the table from a player's point of view (their hand is shown, others' are hidden). Omit player for the public view.",
                schema(Map.of(
                        "table", prop("string", "table id"),
                        "player", prop("string", "viewer name (optional)"),
                        "reveal_all", prop("boolean", "show every hand, for spectating or debugging (optional)")),
                        "table"), this::getState);
        add("legal_moves", "List every legal move for the player whose turn it is.",
                schema(Map.of(
                        "table", prop("string", "table id"),
                        "player", prop("string", "player name")),
                        "table", "player"), this::legalMoves);
        add("play", "Make a move for a seat you control (players and llm_players). After the move, built-in CPU players move until it's one of your seats' turn again.",
                schema(Map.of(
                        "table", prop("string", "table id"),
                        "player", prop("string", "player name"),
                        "action", Map.of("type", "string", "description", "move action, e.g. play or pass", "enum", List.of("play", "pass")),
                        "cards", array("string", "card codes for play, e.g. [\"S3\",\"H3\"], JK for a joker")),
                        "table", "player", "action"), this::play);
        add("get_log", "Show every event of the current round at a table.",
                schema(Map.of("table", prop("string", "table id")), "table"), this::getLog);
        add("list_tables", "List open tables.", schema(Map.of()), this::listTables);
        add("close_table", "Close a table. Player stats are kept.",
                schema(Map.of("table", prop("string", "table id")), "table"), this::closeTable);
        add("get_stats", "Win/lose statistics of players across all tables.",
                schema(Map.of("player", prop("string", "player name (optional, all players when omitted)"))), this::getStats);
        add("roll_dice", "Roll dice.",
                schema(Map.of(
                        "sides", prop("integer", "number of sides, default 6"),
                        "count", prop("integer", "number of dice, default 1"))), this::rollDice);
    }

    private void add(String name, String description, Map<String, Object> schema, Function<Map<String, Object>, String> handler) {
        tools.put(name, new Tool(name, description, schema, handler));
    }

    public List<Tool> tools() {
        return List.copyOf(tools.values());
    }

    /** calls a tool by name */
    public String call(String name, Map<String, Object> args) {
        Tool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("unknown tool: " + name);
        }
        return tool.handler().apply(args == null ? Map.of() : args);
    }

    // ---- handlers

    private String listGames(Map<String, Object> args) {
        StringBuilder sb = new StringBuilder();
        for (GameRule rule : master.rules()) {
            sb.append("id: ").append(rule.id()).append('\n');
            sb.append("name: ").append(rule.name()).append('\n');
            sb.append("players: ").append(rule.minPlayers()).append("..").append(rule.maxPlayers()).append('\n');
            sb.append("options (defaults): ").append(rule.defaultOptions()).append('\n');
            sb.append("cpu profiles (for cpu_players and llm_players, first is default):\n");
            rule.cpuProfiles().forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append('\n'));
            sb.append(rule.rules()).append('\n');
        }
        return sb.toString();
    }

    private String createTable(Map<String, Object> args) {
        List<Player> players = new ArrayList<>();
        strings(args, "players").forEach(n -> players.add(new Player(n, false)));
        strings(args, "cpu_players").forEach(n -> players.add(seat(n, Player.Control.CPU)));
        strings(args, "llm_players").forEach(n -> players.add(seat(n, Player.Control.LLM)));
        Long seed = args.get("seed") instanceof Number n ? n.longValue() : null;
        Map<String, String> options = new LinkedHashMap<>();
        if (args.get("options") instanceof Map<?, ?> m) {
            m.forEach((k, v) -> options.put(String.valueOf(k), String.valueOf(v)));
        }
        Table table = master.createTable(string(args, "game"), players, seed, options);
        return "table " + table.id() + " opened for " + table.rule().name() + "\n"
                + String.join("\n", table.setupLog()) + "\n"
                + "options: " + table.options() + "\n"
                + "next: call start_round with table=" + table.id();
    }

    private String startRound(Map<String, Object> args) {
        Table table = table(args);
        List<String> events = table.startRound();
        return eventsAndView(table, events);
    }

    private String getState(Map<String, Object> args) {
        Table table = table(args);
        boolean all = Boolean.TRUE.equals(args.get("reveal_all"));
        String viewer = all ? null : args.get("player") instanceof String s ? s : "";
        if (viewer != null && !viewer.isEmpty()) {
            table.seat(viewer);
        }
        return table.render(viewer);
    }

    private String legalMoves(Map<String, Object> args) {
        Table table = table(args);
        String player = string(args, "player");
        table.seat(player);
        if (table.state() == null || table.state().isFinished()) {
            return "no round in progress";
        }
        List<Move> moves = table.state().legalMoves(player);
        if (moves.isEmpty()) {
            return "it's not " + player + "'s turn, it's " + table.state().currentPlayer() + "'s turn";
        }
        return moves.size() + " legal moves for " + player + ":\n" + moves.stream()
                .map(m -> m.isPass() ? "pass" : "play " + Card.codes(m.cards()) + "   (" + Card.symbols(m.cards()) + ")")
                .collect(Collectors.joining("\n"));
    }

    private String play(Map<String, Object> args) {
        Table table = table(args);
        String player = string(args, "player");
        List<Card> cards = strings(args, "cards").stream().map(Card::parse).toList();
        Move move = new Move(string(args, "action"), cards);
        List<String> events = table.play(player, move);
        return eventsAndView(table, events);
    }

    private String getLog(Map<String, Object> args) {
        Table table = table(args);
        if (table.state() == null) {
            return String.join("\n", table.setupLog());
        }
        return String.join("\n", table.state().log());
    }

    private String listTables(Map<String, Object> args) {
        if (master.tables().isEmpty()) {
            return "no open tables";
        }
        return master.tables().stream()
                .map(t -> t.id() + ": " + t.rule().id() + " round=" + t.round() + " seats=" + t.seats()
                        + (t.isRoundInProgress() ? " turn=" + t.state().currentPlayer() : " (waiting for start_round)"))
                .collect(Collectors.joining("\n"));
    }

    private String closeTable(Map<String, Object> args) {
        master.closeTable(string(args, "table"));
        return "table " + args.get("table") + " closed";
    }

    private String getStats(Map<String, Object> args) {
        if (args.get("player") instanceof String p) {
            return master.stats(p).toString();
        }
        if (master.allStats().isEmpty()) {
            return "no stats yet";
        }
        return master.allStats().stream().map(PlayerStats::toString).sorted().collect(Collectors.joining("\n"));
    }

    private String rollDice(Map<String, Object> args) {
        int sides = args.get("sides") instanceof Number n ? n.intValue() : 6;
        int count = args.get("count") instanceof Number n ? n.intValue() : 1;
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("count must be 1..100");
        }
        List<Integer> rolls = new Dice(sides, diceRandom).roll(count);
        return "d" + sides + " x" + count + ": " + rolls + " total=" + rolls.stream().mapToInt(Integer::intValue).sum();
    }

    // ---- helpers

    /** events followed by the view of the human player to move (or the result) */
    private static String eventsAndView(Table table, List<String> events) {
        StringBuilder sb = new StringBuilder();
        events.forEach(e -> sb.append(e).append('\n'));
        sb.append('\n');
        if (table.state().isFinished()) {
            sb.append(table.render(""));
            sb.append("round over. call start_round for the next round, or get_stats.");
        } else {
            String current = table.state().currentPlayer();
            sb.append(table.render(current));
            String kind = table.seat(current).control() == Player.Control.LLM ? "llm cpu seat " : "";
            sb.append("waiting for ").append(kind).append(current).append(" (use legal_moves / play with player=").append(current).append(")");
        }
        return sb.toString();
    }

    /** "Name" or "Name:profile" */
    private static Player seat(String spec, Player.Control control) {
        int p = spec.indexOf(':');
        return p < 0 ? new Player(spec.trim(), control, null)
                : new Player(spec.substring(0, p).trim(), control, spec.substring(p + 1).trim());
    }

    private Table table(Map<String, Object> args) {
        return master.table(string(args, "table"));
    }

    private static String string(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null || String.valueOf(v).isBlank()) {
            throw new IllegalArgumentException("argument required: " + key);
        }
        return String.valueOf(v);
    }

    private static List<String> strings(Map<String, Object> args, String key) {
        return switch (args.get(key)) {
            case null -> List.of();
            case List<?> l -> l.stream().map(String::valueOf).toList();
            case String s when s.isBlank() -> List.of();
            case String s -> List.of(s.trim().split("[\\s,]+"));
            case Object o -> throw new IllegalArgumentException(key + " must be an array of strings: " + o);
        };
    }

    private static Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private static Map<String, Object> array(String itemType, String description) {
        return Map.of("type", "array", "items", Map.of("type", itemType), "description", description);
    }

    private static Map<String, Object> schema(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required.length > 0) {
            schema.put("required", List.of(required));
        }
        return schema;
    }
}
