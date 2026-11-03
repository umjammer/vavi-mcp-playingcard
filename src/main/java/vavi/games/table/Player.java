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
 * @param cpu true if the game master plays this seat automatically
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public record Player(String name, boolean cpu) {

    public Player {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("player name is required");
        }
    }

    @Override
    public String toString() {
        return cpu ? name + "(cpu)" : name;
    }
}
