package twcore.bots.multibot.boggle;

public class BoggleStack {
	char[][] board;
	String word;
	String strFound;
	Integer row;
	Integer col;
	
	public BoggleStack(char[][] board, String word, String strFound, Integer row, Integer col) {
		this.board = board;
		this.word = word;
		this.strFound = strFound;
		this.row = row;
		this.col = col;
	}
	
	public String getWord() {return this.word;}
	public String getFound() {return this.strFound;}
	public Integer getRow() {return this.row;}
	public Integer getCol() {return this.col;}
	public char[][] getBoard() {return board;}
}