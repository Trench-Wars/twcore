package twcore.bots.multibot.boggle;

import java.util.Random;

/**
 * This object represents a 4x4 Boggle board.
 */
public class BoggleBoard {
    public static final char BLANK = ' ';
    public char[][] m_board;
    public Random m_rnd;
    
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
        for (int i = 0; i <= 16; i++) {
            z = m_rnd.nextInt(32) + 1;
            l = getLetterFromNumber(z);
            if (x == 4) {
                x = 0;
                y++;
            }
            if (y < 4 && x < 4)
                this.m_board[x][y] = l;
            x++;
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
    
    /**
     * Returns a letter based on a number between 1-38. There are 26 letters however vowels are given
     * three times the likelihood of being returned.
     * @param x - A random number
     * @return - A random character
     */
    public char getLetterFromNumber(int x) {
        char letter = BLANK;
        switch (x) {
            // Consonants...
            case 1:
                letter = 'b';
                break;
            case 11:
                letter = 'n';
                break;
            case 2:
                letter = 'c';
                break;
            case 12:
                letter = 'p';
                break;
            case 3:
                letter = 'd';
                break;
            case 13:
                letter = 'q';
                break;
            case 4:
                letter = 'f';
                break;
            case 14:
                letter = 'r';
                break;
            case 5:
                letter = 'g';
                break;
            case 15:
                letter = 's';
                break;
            case 6:
                letter = 'h';
                break;
            case 16:
                letter = 't';
                break;
            case 7:
                letter = 'j';
                break;
            case 17:
                letter = 'v';
                break;
            case 8:
                letter = 'k';
                break;
            case 18:
                letter = 'w';
                break;
            case 9:
                letter = 'l';
                break;
            case 19:
                letter = 'x';
                break;
            case 10:
                letter = 'm';
                break;
            case 20:
                letter = 'z';
                break;
            // Vowels...
            case 21:
            case 22:
            case 23:
                letter = 'a';
                break;
            case 24:
            case 25:
            case 26:
                letter = 'e';
                break;
            case 27:
            case 28:
            case 29:
                letter = 'i';
                break;
            case 30:
            case 31:
            case 32:
                letter = 'o';
                break;
            case 33:
            case 34:
            case 35:
                letter = 'u';
                break;
            case 36:
            case 37:
            case 38:
                letter = 'y';
                break;
            
            default:
                letter = 'e';
        }
        return letter;
    }
}