package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    //added fields

    private Object lock;

    public int[] slotsToRemove = new int[3];

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     * 
     * needs to add another theread that counts 60 seconds or goes to sleep for 60 second, together with reshuffle time, 
     * this thread's job is to notify dealer that the reshuffle time has came because without him, dealer doesn't get notified
     * that the time has passed.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {

        // TODO implement
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        int cards = table.countCards();
        if (cards < 12) {
            int toPlace = Math.min(12 - cards, deck.size());
            for (int i = 0; i < toPlace; i++) {
                int card = deck.remove(0);
                int firstEmptySlot = table.findFirstEmptySlot();
                if(firstEmptySlot != -1) {
                    table.placeCard(card, firstEmptySlot);
                }
            }
        }
        // TODO implement
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     * should be woken if: terminate, timeout reached, provoked by player
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        //while(table.lock==true);
        
        synchronized(table) {
            try {
                table.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        for(int i = 0; i<table.slotToCard.length; i++){
            if(table.slotToCard[i] != null){
                deck.add(table.slotToCard[i]);
                table.removeCard(i);
            }
        }
        if(env.util.findSets(deck, 1).size() == 0){
            terminate();
        }
        Collections.shuffle(deck);

        // TODO implement

        // shuffle should happen here
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        List<Player> winningPlayers = new ArrayList<>();
        Player winningPlayer = players[0];
        for(Player player: players){
            if(player.score() > winningPlayer.score()){
                winningPlayer = player;
            }
        }
        for(Player player: players){
            if(player.score() == winningPlayer.score()){
                winningPlayers.add(player);
            }
        }
        int[] winningPlayersArray = new int[winningPlayers.size()];
        for(int i = 0; i < winningPlayers.size(); i++){
            winningPlayersArray[i] = winningPlayers.get(i).id;
        }
        env.ui.announceWinner(winningPlayersArray);
        terminate();
    }

    /**
     * Notifies the dealer that a player placed 3 tokens.
     *
     * @param playerId - the id of the player that placed 3 tokens.
     */
    public void checkSet(Player player) {
        int[] cards = new int[3];
        int[] slots = new int[3];
        int counter = 0;
        List<Token> playerTokens = table.playerToToken.get(player.id) ;
        for(Token token: playerTokens){
            int slot = token.getSlot();
            slots[counter] = slot;
            int currCard = table.slotToCard[slot];
            cards[counter] = currCard;
            counter++;
        }
        if(env.util.testSet(cards)){
            slotsToRemove = slots;
            removeCardsFromTable();
            player.point();
        // player.playerThread.sleep(1000);
        }
    }
}
