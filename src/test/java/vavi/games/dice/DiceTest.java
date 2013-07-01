/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.dice;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * DiceTest. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/24 umjammer initial version <br>
 */
public class DiceTest {

    @Test
    public void test() {
        Dice dice1 = new Dice();
        Dice dice2 = new Dice();
        Dice dice3 = new Dice();
        for (int i = 0; i < 100; i++) {
            dice1.next();
            dice2.next();
            dice3.next();
            System.err.println("" + dice1 + dice2 + dice3);
        }
    }
}

/* */
