/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.daifugo;


/**
 * 大富豪 titles given by finishing place.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/11/03 nsano initial version <br>
 */
public enum Title {

    DAIFUGO("大富豪", "grand millionaire"),
    FUGO("富豪", "millionaire"),
    HEIMIN("平民", "commoner"),
    HINMIN("貧民", "poor"),
    DAIHINMIN("大貧民", "extreme poor");

    private final String japanese;
    private final String english;

    Title(String japanese, String english) {
        this.japanese = japanese;
        this.english = english;
    }

    public String japanese() { return japanese; }

    public String english() { return english; }

    /**
     * @param place 1 origin
     * @param players number of players
     */
    public static Title of(int place, int players) {
        if (place == 1) {
            return DAIFUGO;
        } else if (place == players) {
            return DAIHINMIN;
        } else if (players >= 4 && place == 2) {
            return FUGO;
        } else if (players >= 4 && place == players - 1) {
            return HINMIN;
        } else {
            return HEIMIN;
        }
    }

    @Override
    public String toString() {
        return japanese;
    }
}
