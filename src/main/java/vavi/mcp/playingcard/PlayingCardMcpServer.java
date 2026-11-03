/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.mcp.playingcard;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import vavi.games.table.GameMaster;


/**
 * MCP server (stdio) for playing card games. The server is the game master:
 * it keeps the cards, the players and their stats, and enforces the rules.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public final class PlayingCardMcpServer {

    private static final Logger logger = Logger.getLogger(PlayingCardMcpServer.class.getName());

    static final String NAME = "vavi-mcp-playingcard";
    static final String VERSION = "0.0.1";

    private PlayingCardMcpServer() {}

    /** converts a tool to a MCP tool specification, user errors become error results */
    static McpServerFeatures.SyncToolSpecification toSpecification(PlayingCardTools.Tool tool) {
        McpSchema.Tool mcpTool = McpSchema.Tool.builder(tool.name(), tool.inputSchema())
                .description(tool.description())
                .build();
        return new McpServerFeatures.SyncToolSpecification(mcpTool, (exchange, request) -> {
            try {
                String text = tool.handler().apply(request.arguments() == null ? java.util.Map.of() : request.arguments());
                return result(text, false);
            } catch (IllegalArgumentException | IllegalStateException e) {
                return result("error: " + e.getMessage(), true);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "tool " + tool.name() + " failed", e);
                return result("internal error: " + e, true);
            }
        });
    }

    private static McpSchema.CallToolResult result(String text, boolean error) {
        return McpSchema.CallToolResult.builder()
                .content(List.of(new McpSchema.TextContent(text)))
                .isError(error)
                .build();
    }

    /** */
    public static void main(String[] args) throws Exception {
        McpJsonMapper mapper = McpJsonDefaults.getMapper();
        StdioServerTransportProvider transport = new StdioServerTransportProvider(mapper);
        PlayingCardTools tools = new PlayingCardTools(new GameMaster());

        McpSyncServer server = McpServer.sync(transport)
                .serverInfo(NAME, VERSION)
                .instructions("""
                        You are talking to a playing card game master. \
                        Call list_games to see the games and rules, create_table to open a table \
                        (your players + cpu players), start_round to deal, then get_state / legal_moves / play \
                        for each human player's turn. get_stats shows win/lose records.""")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
                .tools(tools.tools().stream().map(PlayingCardMcpServer::toSpecification).toList())
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(server::closeGracefully));
        logger.info(NAME + " " + VERSION + " started");
        Thread.currentThread().join();
    }
}
