package bguspl.set.ex;

import bguspl.set.Env;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.Random;


/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    //added fields
    /** 
     * The dealer of the game. needs to be notified when a player placed 3 tokens.,
    */
    private Dealer dealer;

    /**
     * Queue of tokens, size>0 if player can place tokens. if player removes tokens, queue.add(token)
     * the values inside the items of the queue are not relevant
     */
    private ArrayBlockingQueue<Token> tokensQueue = new ArrayBlockingQueue<>(3);

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop
            // 1. wait for a key press
            // 2. call keyPressed with the slot number
            // 3. if the queue of tokens is full, call notifyDealer
            // 4. if the game is over, terminate the thread
            // 5. if the game is not over, repeat
            




        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                while(tokensQueue.remainingCapacity()==0){
                    try{
                        synchronized(this){
                            wait();
                        }
                    }catch(InterruptedException ignored){}
                }
                int slot = simulateKeyPress();
                keyPressed(slot);
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        if(human){
            playerThread.interrupt();
        }
        else{
            aiThread.interrupt();
        }
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        if(!table.shouldDealerCheck){
          if(table.hasTokenInSlot(id, slot)){ //already has token in slot, so remove token
                table.removeToken(id,slot);
                tokensQueue.remove();
                env.ui.removeToken(id, slot);
            }
            else{                               //doesnt have token there, place
                table.placeToken(id,slot);
                env.ui.placeToken(id, slot);
                tokensQueue.add(new Token(id, slot));
            }
            if(tokensQueue.remainingCapacity()==0){ //if queue is full
                notifyDealer();
        }
    }
        // TODO implement
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        score++;
        try{
            Thread.sleep(1000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        env.ui.setScore(id, score);
        env.ui.setFreeze(id, 1000);
        //clears token queue
        if(!tokensQueue.isEmpty()){
            tokensQueue.clear();
        }
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests

    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        try{
            Thread.sleep(3000);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        if(!human){
            tokensQueue.remove();
        }
        env.ui.setFreeze(id, 3000);
        // TODO implement
    }

    public int score() {
        return score;
    }

    //added methods

    public void notifyDealer(){
        //check if cards are still on table
        synchronized(table.lock){
            for(Token currToken: tokensQueue){
                if(!table.hasTokenInSlot(id, currToken.getSlot())){
                    return;
                }
            }
            table.blockingQueue = tokensQueue;
            table.shouldDealerCheck = true;
        }
    }


    private int simulateKeyPress(){
        Random random = new Random();
        int[] availableKeys = new int[env.config.tableSize];
        int randomIndex = random.nextInt(availableKeys.length);
        return randomIndex;
    }
}
