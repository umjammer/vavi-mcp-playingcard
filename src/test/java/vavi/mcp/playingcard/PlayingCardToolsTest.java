/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.mcp.playingcard;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import vavi.games.table.GameMaster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Plays a round through the MCP tools, as an AI client would:
 * one client controlled player against three cpu players.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
class PlayingCardToolsTest {

    @Test
    void playThroughTools() {
        PlayingCardTools tools = new PlayingCardTools(new GameMaster());

        System.out.println(">>> list_games\n" + tools.call("list_games", Map.of()));

        String created = tools.call("create_table", Map.of(
                "game", "daifugo",
                "players", List.of("You"),
                "cpu_players", List.of("Cpu1", "Cpu2", "Cpu3"),
                "seed", 42));
        System.out.println(">>> create_table\n" + created);
        assertTrue(created.startsWith("table t1 opened"));

        System.out.println(">>> start_round\n" + tools.call("start_round", Map.of("table", "t1")));

        int turns = 0;
        while (true) {
            String legal = tools.call("legal_moves", Map.of("table", "t1", "player", "You"));
            if (legal.startsWith("no round")) {
                break;
            }
            System.out.println(">>> legal_moves\n" + legal);
            // a naive client: play the first listed move (the weakest)
            String first = legal.lines().skip(1).findFirst().orElseThrow();
            String[] words = first.split("\\(")[0].trim().split("\\s+");
            Map<String, Object> args = words[0].equals("pass")
                    ? Map.of("table", "t1", "player", "You", "action", "pass")
                    : Map.of("table", "t1", "player", "You", "action", "play",
                             "cards", List.of(words).subList(1, words.length));
            System.out.println(">>> play action=" + args.get("action") + " cards=" + args.getOrDefault("cards", List.of()));
            System.out.println(tools.call("play", args));
            assertTrue(++turns < 200);
        }

        String stats = tools.call("get_stats", Map.of());
        System.out.println(">>> get_stats\n" + stats);
        assertEquals(4, stats.lines().count());
        assertTrue(tools.call("get_stats", Map.of("player", "You")).contains("played=1"));
    }

    /** cpu seats handed off to the client LLM: the server stops at their turns */
    @Test
    void llmPlayers() {
        PlayingCardTools tools = new PlayingCardTools(new GameMaster());
        System.out.println(tools.call("create_table", Map.of(
                "game", "daifugo",
                "cpu_players", List.of("Cpu1:aggressive", "Cpu2"),
                "llm_players", List.of("Llm1:cautious", "Llm2:random"),
                "seed", 3)));
        assertTrue(tools.call("list_tables", Map.of()).contains("Llm1(llm:cautious)"));
        assertTrue(tools.call("list_tables", Map.of()).contains("Cpu2(cpu:balanced)"));

        String out = tools.call("start_round", Map.of("table", "t1"));
        int turns = 0;
        while (out.contains("waiting for llm cpu seat")) {
            String current = out.replaceAll("(?s).*waiting for llm cpu seat (\\w+).*", "$1");
            assertTrue(out.contains(current + " is a cpu seat played by you"), out);
            if (turns == 0) {
                System.out.println(">>> hand-off\n" + out);
                // the game master never plays an llm seat by itself
                assertThrows(IllegalArgumentException.class, () -> tools.call("play", Map.of("table", "t1", "player", "Cpu1", "action", "pass")));
            }
            String legal = tools.call("legal_moves", Map.of("table", "t1", "player", current));
            String[] words = legal.lines().skip(1).findFirst().orElseThrow().split("\\(")[0].trim().split("\\s+");
            Map<String, Object> args = words[0].equals("pass")
                    ? Map.of("table", "t1", "player", current, "action", "pass")
                    : Map.of("table", "t1", "player", current, "action", "play", "cards", List.of(words).subList(1, words.length));
            out = tools.call("play", args);
            assertTrue(++turns < 300);
        }
        assertTrue(out.contains("round over"), out);
        assertTrue(turns > 0);
        assertThrows(IllegalArgumentException.class, () -> tools.call("create_table", Map.of(
                "game", "daifugo", "cpu_players", List.of("A:genius", "B", "C"))));
    }

    @Test
    void errors() {
        PlayingCardTools tools = new PlayingCardTools(new GameMaster());
        tools.call("create_table", Map.of("game", "daifugo", "players", List.of("A", "B", "C"), "seed", 1));
        assertEquals("no round in progress", tools.call("legal_moves", Map.of("table", "t1", "player", "A")));
        tools.call("start_round", Map.of("table", "t1"));
        String current = tools.call("list_tables", Map.of()).replaceAll("(?s).*turn=(\\w+).*", "$1");
        String other = List.of("A", "B", "C").stream().filter(p -> !p.equals(current)).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> tools.call("play", Map.of("table", "t1", "player", other, "action", "pass")));
        assertThrows(IllegalArgumentException.class,
                () -> tools.call("play", Map.of("table", "t1", "player", current, "action", "pass")), "leader can't pass");
        assertThrows(IllegalArgumentException.class, () -> tools.call("create_table", Map.of("game", "poker")));

        // the server converts user errors into error results
        PlayingCardTools.Tool play = tools.tools().stream().filter(t -> t.name().equals("play")).findFirst().orElseThrow();
        McpSchema.CallToolResult result = PlayingCardMcpServer.toSpecification(play).callHandler()
                .apply(null, new McpSchema.CallToolRequest("play", Map.of("table", "t9", "player", "A", "action", "pass"), null));
        assertTrue(result.isError());
        System.out.println(((McpSchema.TextContent) result.content().getFirst()).text());
    }
}
