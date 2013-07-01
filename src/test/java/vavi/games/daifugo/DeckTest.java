/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.daifugo;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import vavi.games.card.Card;
import vavi.games.card.Dealer;
import vavi.games.card.Deck;
import vavi.games.card.Game;
import vavi.games.card.Player;
import vavi.games.card.Shuffle;

import static org.junit.Assert.*;


/**
 * DeckTest. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/23 umjammer initial version <br>
 */
public class DeckTest {

    List<Card> backup;
    
    private void backup(Deck deck) {
//System.err.println(deck.getCards().size());
        backup = new ArrayList<Card>(deck.getCards());
//System.err.println(t.size());
    }
    
    private int compare(Deck deck) {
        int diffCount = 0;
        for (int i = 0; i < deck.getCards().size(); i++) {
            if (deck.getCards().get(i).equals(backup.get(i))) {
                diffCount++;
            }
        }
//System.err.println(diffCount);
        return diffCount;
    }
    
    @Test
    public void test01() {
        Dealer dealer = new Dealer();
        dealer.setShuffle(Shuffle.FisherYatesShuffle);

        Deck deck = Deck.SimpleDeck;
        for (int i = 0; i < 10; i++) {
            backup(deck);
            dealer.shuffle(deck);
            assertEquals(0, compare(deck));
        }
    }

    static final int TIMES = 10000;

    @Test
    public void test02() {
        Dealer dealer = new Dealer();
        dealer.setShuffle(Shuffle.SimpleShuffle);

        Deck deck = Deck.SimpleDeck;
        int s = 0; 
        for (int i = 0; i < TIMES; i++) {
            backup(deck);
            dealer.shuffle(deck);
            s += compare(deck);
        }
        System.err.println(s / (float) TIMES);
    }

    @Test
    public void test03() {
        Dealer dealer = new Dealer();
        dealer.setShuffle(Shuffle.SimpleRandomShuffle);

        Deck deck = Deck.SimpleDeck;
        int s = 0; 
        for (int i = 0; i < TIMES; i++) {
            backup(deck);
            dealer.shuffle(deck);
            s += compare(deck);
        }
        System.err.println(s / (float) TIMES);
    }


    @Test
    public void test2() {
        Dealer dealer = new Dealer();
        dealer.setShuffle(Shuffle.FisherYatesShuffle);

//        for (Card card : Deck.SimpleDeck.getCards()) {
//            System.err.println(card);
//        }
        Game game = new Daifugo();
        game.setDeck(Deck.SimpleDeck);
        game.setDealer(dealer);
        Player p1;
        game.addPlayer(p1 = new Player("Player1"));
        game.addPlayer(new Player("Player2"));
        game.addPlayer(new Player("Player3"));
        game.addPlayer(new Player("Player4"));

        while (!game.isFinished()) {
            game.setPlayersOrder();
            game.shuffle();
            game.deal();
            System.err.println("---- " + p1 + " ----");
            game.printPlayerCards();
            game.game();
        }
    }
}

/* */
