package twcore.bots.multibot.boggle;

import java.util.Random;

/**
 * 
 * @author milosh
 */
public class BoggleBoard {
	public static final char BLANK = ' ';
	public char[][] m_board;
	public Random m_rnd;
	
	public BoggleBoard(){
		m_rnd = new Random();
		this.m_board = new char[4][4];
	}
	
	public void fill(){
		clear();
		int x=0,y=0,z;
		char l;
		for(int i=0;i<=16;i++){
			z = m_rnd.nextInt(32) + 1;
			l = getLetterFromNumber(z);
			if(x == 4){
				x = 0;
				y++;
			}
			if(y<4&&x<4)
				this.m_board[x][y] = l;
			x++;
		}
		/*SAMPLE BOARD
		this.m_board[0][0] = 't';
		this.m_board[1][0] = 'a';
		this.m_board[2][0] = 'c';
		this.m_board[3][0] = 'q';
		//
		this.m_board[0][1] = 'a';
		this.m_board[1][1] = 'i';
		this.m_board[2][1] = 'r';
		this.m_board[3][1] = 'd';
		//
		this.m_board[0][2] = 'n';
		this.m_board[1][2] = 'p';
		this.m_board[2][2] = 'o';
		this.m_board[3][2] = 't';
		//
		this.m_board[0][3] = 'k';
		this.m_board[1][3] = 'l';
		this.m_board[2][3] = 'a';
		this.m_board[3][3] = 'y';
		*/
	}
	
	public void clear(){
		for (int i=0; i < 4; i++)
			   for (int j=0; j < 4; j++)
			      this.m_board[i][j] = BLANK;
	}
	
	public char[][] getBoard(){
		return this.m_board;
	}
	
	public char getBlock(int x, int y){
		return this.m_board[x][y];
	}
	
	public char getLetterFromNumber(int x){
		char letter = BLANK;
		switch(x){
		//Consonants...
		case 1: letter = 'b'; break; case 11: letter = 'n'; break;
		case 2: letter = 'c'; break; case 12: letter = 'p'; break;
		case 3: letter = 'd'; break; case 13: letter = 'q'; break;
		case 4: letter = 'f'; break; case 14: letter = 'r'; break;
		case 5: letter = 'g'; break; case 15: letter = 's'; break;
		case 6: letter = 'h'; break; case 16: letter = 't'; break;
		case 7: letter = 'j'; break; case 17: letter = 'v'; break;
		case 8: letter = 'k'; break; case 18: letter = 'w'; break;
		case 9: letter = 'l'; break; case 19: letter = 'x'; break;
		case 10: letter = 'm'; break; case 20: letter = 'z'; break;
		//Vowels...
		case 21: case 22: case 23: letter = 'a'; break;
		case 24: case 25: case 26: letter = 'e'; break;
		case 27: case 28: case 29: letter = 'i'; break;
		case 30: case 31: case 32: letter = 'o'; break;
		case 33: case 34: case 35: letter = 'u'; break;
		case 36: case 37: case 38: letter = 'y'; break;

		default: letter = 'e';
		}
		return letter;
	}
	
	public boolean isWord(String s){
		
		return true;
	}
	
	
}