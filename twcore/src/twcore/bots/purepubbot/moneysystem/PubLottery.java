package twcore.bots.purepubbot.moneysystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import twcore.core.BotAction;

public class PubLottery {

    private BotAction m_botAction;
    
    // amount of entry numbers per ticket; default is 2
    private int ticketSize = 2;
    
    // interval of number options; default is 1-50
    private int numberMax = 50;
    
    // default jackpot is $1000; if you change this value, also change it in resetJackpot()
    private int jackpot;
    
    // default ticket price is $100; if you change this value, also change it in resetPrice()
    private int price;
    
    // counts the number of lottery tickets submitted
    private int entries;
    
    // lottery's winning numbers
    private int[] winningNum = new int[ticketSize];
    
    // stores and displays the player's numbers that match the winning numbers
    private String matchingNumbers; // change 'store' to display?
    
    // status for lottery; true = enabled, false = disabled
    private boolean lotteryOn;
    
    
    /* TODO
     * - Price checks
     * - When to start/stop lottery -- must find a place to do startLottery() then will create winning numbers
     * - Disable commands such as !lprice and !lottery if lottery is not enabled. [methods to do this is commented]
     * - If player does not have enough money, tell player how much it costs
     * - lotteryOn if neccessary > enables/disables this lottery game
     */
    
    public PubLottery(BotAction botAction) {
        this.m_botAction = botAction;
        jackpot = 1000;
        price = 100;
        entries = 0;
        lotteryOn = false;
    }
    
    /**
     * Checks the player's submitted ticket.
     * @param sender
     * @param command
     */
    public void handleTicket(String sender, String command) {
        int matches = 0;
        String matchingNum = null;
        String[] entry = command.substring(command.indexOf(" ")+1).split(" ");
        
        matchingNumbers = "";
        try {
            if(this.isValid(sender, entry))
                return;
            if(entry.length != ticketSize) {
                m_botAction.sendSmartPrivateMessage(sender, "Please submit a ticket with two numbers.");
                return;
            }
            
            for( int i=0; i<ticketSize; i++ ) {
                for( int j=0; j<ticketSize; j++ )
                    if(Integer.parseInt(entry[i]) == winningNum[j] ) {
                        matchingNum = entry[i];
                        matches++;
    
                        this.saveLuckyNumbers(matchingNum);
                    }
            }
            
            entries++;
            this.raiseJackpot();
            this.checkLottery(sender, matches);
            
        } catch(Exception e) {
            m_botAction.sendSmartPrivateMessage(sender, "Submission denied; Must contain "+ticketSize+" unique numbers with no duplicates.");
            return;
        }
    }
    
    /**
     * Displays a message to the player about their submitted ticket.
     * @param sender
     * @param matches
     */
    public void checkLottery(String sender, int matches) {   
        if(matches > 0) {
            String message = "[Ticket #"+entries+"] Found "+matches+" lucky numbers: ["+matchingNumbers+"]";
            m_botAction.sendSmartPrivateMessage(sender, message);
        }
        else
            m_botAction.sendSmartPrivateMessage(sender, "[Ticket #"+entries+"] Sorry, it seems you aren't lucky this time.");
        
        if(matches == ticketSize) {
            m_botAction.sendArenaMessage(sender+" has won $"+jackpot+"! Ticket #"+entries+": ["+matchingNumbers+"]");
        }
    }
    
    /**
     * Creates random numbers for a winning lottery ticket. [unique numbers]
     * @return
     */
    public Set<Integer> createNumbers() {
        Set<Integer> numbers = new HashSet<Integer>(ticketSize);
       
        while (numbers.size() < ticketSize) {
            numbers.add((int) (Math.random() * numberMax + 1));
        }

        return numbers;
    }
    
    /**
     * Player's tickets will be compared to this winning ticket.
     */
    public void startLottery() {
        List<Object> list = new ArrayList<Object>(createNumbers());
        Object[] objects = list.toArray();

        for ( int i=0; i<objects.length; i++ ) {
            Object object = objects[i];
            winningNum[i] = Integer.parseInt(object.toString());
        }
        
        m_botAction.sendArenaMessage("Lottery has been started.");
    }
    
    /**
     * Checks if the player's ticket does not have any fault.
     * Such as having duplicate numbers and if their numbers are out of range.
     * @param sender
     * @param pieces
     * @return
     */
    public boolean isValid(String sender, String[] pieces) {
        for(int i = 0; i < ticketSize; i++)
            for( int j = 0; j < ticketSize; j++ ) {
                if( i == j )
                    continue;
                else if(pieces[i].equals(pieces[j])) {
                    m_botAction.sendSmartPrivateMessage(sender, "Must contain "+ticketSize+" unique numbers with no duplicates.");
                    return true; 
                }
                else if(Integer.parseInt(pieces[i]) < 1 || Integer.parseInt(pieces[i]) > numberMax) {
                    m_botAction.sendSmartPrivateMessage(sender, "Ticket entry is out of range. [1 to "+numberMax+"]");
                    return true;
                }
            }
      
        return false;
    }
    
    /**
     * Displays the jackpot money to the player.
     * @param sender
     */
    public void displayJackpot(String sender) {
        m_botAction.sendSmartPrivateMessage(sender, "Current jackpot: "+this.jackpot);
    }
    
    /**
     * Sets the price per ticket to the moderator's specified amount.
     * @param sender
     * @param command
     */
    public void setTicketPrice(String sender, String command) {
        String value = command.substring(8);
        try {
            if(Integer.parseInt(value) < 0)
                return;
            this.price = Integer.parseInt(value);
            m_botAction.sendSmartPrivateMessage(sender, "Price per ticket has been set to $"+this.price);
        } catch(Exception e) {
            m_botAction.sendSmartPrivateMessage(sender, "Invalid input. Please enter a proper value.");
            return;
        }
    }
    
    /**
     * Stores the lucky numbers into String variable: matchingNumbers
     * Used to display the matching numbers that player had in ticket.
     */
    public void saveLuckyNumbers(String matchingNum) {
        matchingNumbers += "("+matchingNum+")";
    }
    
    public void resetJackpot() {
        jackpot = 1000;
    }
    
    public void raiseJackpot() {
        jackpot += price;
    }
    
    public void resetPrice() {
        price = 100;
    }
    
    /*public boolean isEnabled() {
        return lotteryOn;
    }
    
    public void setStatus(boolean mode) {
        lotteryOn = mode;
    }*/
}