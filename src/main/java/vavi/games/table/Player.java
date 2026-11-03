/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.table;


/**
 * A seat at a table.
 *
 * @param name unique within a table, also the key of {@link PlayerStats}
 * @param control who decides the moves of this seat
 * @param profile cpu strategy profile (see {@link GameRule#cpuProfiles()}), null for {@link Control#CLIENT}
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public record Player(String name, Control control, String profile) {

    /** who decides the moves of a seat */
    public enum Control {
        /** the MCP client plays this seat as itself */
        CLIENT,
        /** the game master plays this seat with the rule's built-in strategy */
        CPU,
        /** a cpu seat whose moves are handed off to the MCP client (LLM), which plays it in the style of the profile */
        LLM
    }

    public Player {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("player name is required");
        }
        if (control == null) {
            throw new IllegalArgumentException("control is required");
        }
    }

    /** a client or a built-in cpu seat with the default profile */
    public Player(String name, boolean cpu) {
        this(name, cpu ? Control.CPU : Control.CLIENT, null);
    }

    /** true if the game master plays this seat automatically */
    public boolean cpu() {
        return control == Control.CPU;
    }

    /** true if the client decides the moves of this seat (as itself, or as an llm played cpu) */
    public boolean clientControlled() {
        return control != Control.CPU;
    }

    @Override
    public String toString() {
        return switch (control) {
            case CLIENT -> name;
            case CPU -> name + "(cpu:" + profile + ")";
            case LLM -> name + "(llm:" + profile + ")";
        };
    }
}
