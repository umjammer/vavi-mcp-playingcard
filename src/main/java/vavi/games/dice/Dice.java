/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.dice;

import java.security.SecureRandom;
import java.util.Random;


/**
 * Dice. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/24 umjammer initial version <br>
 */
public class Dice {

    private static final int MAX = 6;

    private static String[] view = { "⚀", "⚁", "⚂", "⚃", "⚄", "⚅" };
    
    private Random random = new SecureRandom();

    public Dice() {
        pip = nextInternal();
    }

    public int current() {
        return pip;
    }

    private int nextInternal() {
        return random.nextInt(MAX) + 1;
    }

    public int next() {
        pip = nextInternal();
        return pip;
    }

    private int pip;
    
    public String toString() {
        return view[pip - 1];
    }
}

/* */
