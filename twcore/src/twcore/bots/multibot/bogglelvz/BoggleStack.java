package twcore.bots.multibot.bogglelvz;

/**
 * This object is used to search a 4x4 Boggle board for given words.
 * @see boggle.java at method isOnBoard(String, char[][])
 */
public class BoggleStack {
    public char[][] board;
    public String word, strFound;
    public int row, col;
    
    /**
     * The BoggleStack constructor
     * @param board - The board.
     * @param word - The word trying to be found
     * @param strFound - The current string found
     * @param row - The current location by row
     * @param col - The current location by column
     */
    public BoggleStack(char[][] board, String word, String strFound, int row, int col) {
        this.board = board;
        this.word = word;
        this.strFound = strFound;
        this.row = row;
        this.col = col;
    }
    
    /**
     * Returns the word trying to be found.
     */
    public String getWord() {
        return this.word;
    }
    
    /**
     * Returns the current string found.
     */
    public String getFound() {
        return this.strFound;
    }
    
    /**
     * Returns the current location by row.
     */
    public int getRow() {
        return this.row;
    }
    
    /**
     * Returns the current location by column.
     */
    public int getCol() {
        return this.col;
    }
    
    /**
     * Returns the board.
     */
    public char[][] getBoard() {
        return board;
    }
}