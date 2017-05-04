
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ConnectFour {
	public static int[][] board = new int[7][6]; // 0 = empty, -1 and 1 are players
	public static int player;
	public static int maxDepth = 3;
	public static int bestPlay = 3;

	public static final int[][] possibles = new int[][]{ //array of all possible win options for each place
		{11, 15, 3}, //ROW 0
		{11, 10, 15, 3},
		{11, 10, 9, 15, 3},
		{11, 10, 9, 8, 15, 3, 4},
		{8, 9, 10, 15, 4},
		{8, 9, 15, 4},
		{8, 15, 4},

		{11, 14, 15, 3}, //ROW 1
		{11, 10, 14, 15, 3, 2},
		{11, 10, 9, 14, 15, 3, 2, 5},
		{11, 10, 9, 8, 14, 15, 3, 2, 5, 4},
		{8, 9, 10, 14, 15, 2, 5, 4},
		{8, 9, 14, 15, 5, 4},
		{8, 14, 15, 4},

		{11, 13, 14, 15, 3}, //ROW 2
		{11, 10, 13, 14, 15, 3, 2, 6},
		{11, 10, 9, 13, 14, 15, 3, 2, 1, 6, 5},
		{11, 10, 9, 8, 13, 14, 15, 3, 2, 1, 6, 5, 4},
		{8, 9, 10, 13, 14, 15, 2, 1, 6, 5, 4},
		{8, 9, 13, 14, 15, 1, 5, 4},
		{8, 13, 14, 15, 4},

		{11, 12, 13, 14, 7}, //ROW 3
		{11, 10, 12, 13, 14, 2, 7, 6},
		{11, 10, 9, 12, 13, 14, 2, 1, 7, 6, 5},
		{11, 10, 9, 8, 12, 13, 14, 2, 1, 0, 7, 6, 5},
		{8, 9, 10, 12, 13, 14, 2, 1, 0, 6, 5},
		{8, 9, 12, 13, 14, 1, 0, 5},
		{8, 12, 13, 14, 0},

		{11, 12, 13, 7}, //ROW 4
		{11, 10, 12, 13, 7, 6},
		{11, 10, 9, 12, 13, 1, 7, 6},
		{11, 10, 9, 8, 12, 13, 1, 0, 7, 6},
		{8, 9, 10, 12, 13, 1, 0, 6},
		{8, 9, 12, 13, 1, 0},  
		{8, 12, 13, 0},

		{11, 12, 7}, //ROW 5
		{11, 10, 12, 7},
		{11, 10, 9, 12, 7},
		{11, 10, 9, 8, 12, 7, 0},
		{8, 9, 10, 12, 0},
		{8, 9, 12, 0},
		{8, 12, 0}

	};
	public static final int[][] moves = new int[][]{ //array of win types
		{0,0},{1,1},{2,2},{3,3},
		{0,0},{1,-1},{2,-2},{3,-3},
		{0,0},{1,0},{2,0},{3,0},
		{0,0},{0,1},{0,2},{0,3},
		{-1, -1}, {-1, 1}, {-1, 0}, {0, -1} //directions
	};

	public static void main(String[] args) {
		{// read input
			int i = 0;
			int x, y;
			for (int charIndex = 0; charIndex < args[0].length(); charIndex++) {
				x = i % 7;
				y = 6 - ((int) i / 7) - 1;

				switch (args[0].charAt(charIndex)) {
				case 'r':
					board[x][y] = 1;
					i++;
					break;
				case 'y':
					board[x][y] = -1;
					i++;
					break;
				case '.':
					board[x][y] = 0;
					i++;
					break;
				case ',':
					break;
				}
			}
		} //(BLOCK... BEWARE)
		player = (args[1].equals("red") ? 1 : -1);

		final ExecutorService service = Executors.newSingleThreadExecutor(); //thread runner

		try{ //IDS till nearly 1 second up
			@SuppressWarnings("rawtypes")
			final Future f = service.submit(() -> {
				while(true){
					bestPlay = (int) prune(board, maxDepth, player, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true)[1]; //IDS
					maxDepth++;
				}
			});
			f.get(900, TimeUnit.MILLISECONDS);
		} catch (final TimeoutException e) {
			System.out.println(bestPlay);
		}  catch (final Exception e) {
			throw new RuntimeException(e);
		}
		
		service.shutdownNow();
		System.exit(0);
	}

	public static double[] prune(int[][] board, int depth, int player, double alpha, double beta, boolean maxer) { //minimax with A/B pruning
		double[] ret = new double[2]; //value and move
		boolean leaf;
		int[] movePos = new int[2];

		if (depth == 0) {
			return new double[] { evaluateState(board, player), 0 };
		}

		if (maxer) { //maximising
			leaf = true;
			ret[0] = Double.NEGATIVE_INFINITY;

			int i;
			for (int j = 0; j < 7; j++) {
				i = 3 + (int)((j%2==0?1:-1) * Math.ceil(j/2.0)); //check middle columns first (AB speedup)
				int[][] nextState = makeMove(board, i, player, movePos); //record movePos for checking win
				if (nextState != null) {
					leaf = false;
					double v = checkWin(nextState, movePos, player)?Double.POSITIVE_INFINITY:prune(nextState, depth - 1, player, alpha, beta, false)[0];

					if (v > ret[0]) {
						ret[0] = v;
						ret[1] = i;
					}
					alpha = Math.max(alpha, v);
					if (beta <= alpha) break; //a/b prune
				}
			}
			return leaf ? new double[2] : ret;
		} else { //minimising
			leaf = true;
			ret[0] = Double.POSITIVE_INFINITY;

			int i;
			for (int j = 0; j < 7; j++) {
				i = 3 + (int)((j%2==0?1:-1) * Math.ceil(j/2.0));

				int[][] nextState = makeMove(board, i, -1 * player, movePos);
				if (nextState != null) {
					leaf = false;
					double v = checkWin(nextState, movePos, -1 * player)?Double.NEGATIVE_INFINITY:prune(nextState, depth - 1, player, alpha, beta, true)[0];
					if (v < ret[0]) {
						ret[0] = v;
						ret[1] = i;
					}
					beta = Math.min(beta, v);
					if (beta <= alpha)	break; //a/b prune
				}
			}
			return leaf ? new double[2] : ret;
		}
	}

	public static int[][] makeMove(int[][] board, int position, int player, int[] movePos) {
		if (board[position][0] != 0) { // check if move is possible
			return null;
		}

		int[][] newBoard = new int[7][6];
		for (int j = 0; j < board.length; j++) {
			System.arraycopy(board[j], 0, newBoard[j], 0, board[j].length);
		}

		int i = 0;
		while (i < 6 - 1 && board[position][i + 1] == 0) { //find where piece would fall to
			i++;
		}
		
		newBoard[position][i] = player;
		movePos[0] = position;
		movePos[1] = i;
		return newBoard;
	}

	public static float evaluateState(int[][] board, int player) { //points of current player minus other's
		return score(board, player) - score(board, -1 * player);
	}

	public static float score(int[][] board, int player) { //heuristic function
		int score = 0;
		for(int x = 0; x < 7; x++){
			for(int y = 0; y < 6; y++){ //for each piece
				if(board[x][y] == player){
					int openCount = 0;
					int pieceCount = 0;
					int[] possibleMoves = possibles[x + y * 7];

					for(int i = 0; i < possibleMoves.length; i++){ //count all the possible win's available

						int currentX = x+moves[possibleMoves[i]][0];
						int currentY = y+moves[possibleMoves[i]][1];

						int vX = moves[16+(int)possibleMoves[i]/4][0];
						int vY = moves[16+(int)possibleMoves[i]/4][1];

						boolean open = true;
						int localCount = 0;

						for(int j = 0; j < 4; j++){
							if(board[currentX][currentY] == player*-1){
								open = false;
								break;
							} else if(board[currentX][currentY] != 0){
								localCount++;
							}

							currentX+=vX;
							currentY+=vY;
						}

						if(open){
							openCount++;
							pieceCount+=localCount;
						}
					}

					score += pieceCount * openCount; //Heuristic definition <---
				}
			}
		}
		return score;
	}

	public static boolean checkWin(int[][] board, int[] movePos, int player) { //utility function for checking endstate
		int[] possibleMoves = possibles[movePos[0] + movePos[1] * 7];
		for(int i = 0; i < possibleMoves.length; i++){ //win possibilities
			boolean four = true;
			int x = movePos[0]+moves[possibleMoves[i]][0]; //win starting position
			int y = movePos[1]+moves[possibleMoves[i]][1];

			int vX = moves[16+(int)possibleMoves[i]/4][0]; //direction of win
			int vY = moves[16+(int)possibleMoves[i]/4][1];
			for(int j = 0; j < 4; j++){
				if(board[x][y] != player){
					four = false;
					break;
				}
				x+=vX;
				y+=vY;
			}
			
			if(four){
				return true;
			}
		}
		return false;
	}
}
