/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.random.RandomGenerator;


/**
 * Dice, mainly for deciding seat or starting order.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public final class Dice {

    private final int sides;
    private final RandomGenerator random;

    /** */
    public Dice(int sides, RandomGenerator random) {
        if (sides < 2) {
            throw new IllegalArgumentException("sides must be >= 2: " + sides);
        }
        this.sides = sides;
        this.random = random;
    }

    /** a six sided die */
    public Dice(RandomGenerator random) {
        this(6, random);
    }

    /** 1..sides */
    public int roll() {
        return random.nextInt(sides) + 1;
    }

    /** */
    public List<Integer> roll(int count) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(roll());
        }
        return result;
    }

    /**
     * Result of {@link #decideOrder}.
     *
     * @param order items, highest roll first
     * @param log human readable roll log
     */
    public record Order<T>(List<T> order, List<String> log) {}

    /**
     * Everyone rolls one die, highest goes first. Tied items re-roll among themselves.
     */
    public <T> Order<T> decideOrder(List<T> items, Function<T, String> name) {
        List<String> log = new ArrayList<>();
        List<T> order = new ArrayList<>();
        decide(items, name, log, order, 1);
        return new Order<>(order, log);
    }

    private <T> void decide(List<T> items, Function<T, String> name, List<String> log, List<T> order, int attempt) {
        if (items.size() == 1) {
            order.add(items.getFirst());
            return;
        }
        Map<T, Integer> rolls = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder(attempt == 1 ? "dice:" : "re-roll:");
        for (T item : items) {
            int r = roll();
            rolls.put(item, r);
            sb.append(' ').append(name.apply(item)).append('=').append(r);
        }
        log.add(sb.toString());
        // group by roll, descending, recurse on ties
        rolls.values().stream().distinct().sorted(Comparator.reverseOrder()).forEach(value -> {
            List<T> tied = rolls.entrySet().stream().filter(e -> e.getValue().equals(value)).map(Map.Entry::getKey).toList();
            decide(tied, name, log, order, attempt + 1);
        });
    }
}
