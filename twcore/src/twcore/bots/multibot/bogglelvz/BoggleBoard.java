package twcore.bots.multibot.bogglelvz;

import java.util.Iterator;
import java.util.Random;

import twcore.core.game.Player;
import twcore.core.util.StringBag;

/**
 * This object represents a 4x4 Boggle board.
 */
public class BoggleBoard {
    public static final char BLANK = ' ';
    public char[][] m_board;
    public Random m_rnd;

    // we'll be using the real dice of boggle to generate the game board
    public String[] m_dice = {"lrytte","vthrwe","eghwne","seotis","anaeeg","idsytt","oattow","mtoicu","afpkfs","xlderi","hcpoas","ensieu","yldevr","znrnhl","nmiqhu","obbaoj"};
    
    /**
     * The BoggleBoard constructor.
     */
    public BoggleBoard() {
        m_rnd = new Random();
        this.m_board = new char[4][4];
    }

    /**
     * Clears this board and fills it with random characters.
     */
    public void fill() {
        clear();
        int x = 0, y = 0, z;
        char l;
        
        StringBag diceBag = new StringBag();
        for (x = 0; x < m_dice.length; x++) {diceBag.add(m_dice[x]);}

        char[] die;
        for (x = 0; x < 4; x++) {
            for (y = 0; y < 4; y++) {
                // grab a random die
                die = diceBag.grabAndRemove().toCharArray();
                
                // grab a random letter (side) of that die
                this.m_board[x][y] = die[(int)(Math.random()*6)];
            }
        }
    }
    
    /**
     * Clears the board and fills it with blank spaces.
     */
    public void clear() {
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                this.m_board[i][j] = BLANK;
    }
    
    /**
     * Gets the char[][] board.
     */
    public char[][] getBoard() {
        return this.m_board;
    }
    
    /**
     * Gets a character by x,y location on the board
     * @param x - The x location
     * @param y - The y location
     * @return - The character found at that location
     */
    public char getBlock(int x, int y) {
        return this.m_board[x][y];
    }
}