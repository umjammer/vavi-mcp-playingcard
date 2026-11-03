/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.table;

import java.util.Map;
import java.util.TreeMap;


/**
 * Win/lose record of a player across all tables of a game master.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public final class PlayerStats {

    private final String name;
    private int played;
    private int wins;
    private int losses;
    /** place (1 origin) -> count */
    private final Map<Integer, Integer> places = new TreeMap<>();
    /** game specific title -> count, e.g. 大富豪 */
    private final Map<String, Integer> titles = new TreeMap<>();

    /** */
    public PlayerStats(String name) {
        this.name = name;
    }

    /**
     * @param place 1 origin
     * @param players number of players in the round
     * @param title game specific title for the place, nullable
     */
    synchronized void record(int place, int players, String title) {
        played++;
        if (place == 1) {
            wins++;
        }
        if (place == players) {
            losses++;
        }
        places.merge(place, 1, Integer::sum);
        if (title != null) {
            titles.merge(title, 1, Integer::sum);
        }
    }

    public String name() { return name; }
    public synchronized int played() { return played; }
    public synchronized int wins() { return wins; }
    public synchronized int losses() { return losses; }
    public synchronized Map<Integer, Integer> places() { return new TreeMap<>(places); }
    public synchronized Map<String, Integer> titles() { return new TreeMap<>(titles); }

    @Override
    public synchronized String toString() {
        double rate = played == 0 ? 0 : 100.0 * wins / played;
        return String.format("%s: played=%d wins=%d losses=%d win-rate=%.1f%% places=%s titles=%s",
                name, played, wins, losses, rate, places, titles);
    }
}
