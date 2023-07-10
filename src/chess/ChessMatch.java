package chess;

import boardgame.Board;
import boardgame.Position;
import chess.pieces.King;
import chess.pieces.Rook;

public class ChessMatch {

    private Board board;

    public ChessMatch() {
        this.board = new Board(8, 8);
        this.initialSetup();
    }

    public ChessPiece[][] getPieces() {
        ChessPiece[][] piecesMatrix = new ChessPiece[this.board.getRows()][this.board.getColumns()];

        for (int i = 0; i < this.board.getRows(); i++) {
            for (int j = 0; j < this.board.getColumns(); j++) {

                piecesMatrix[i][j] = (ChessPiece) this.board.piece(i, j);
            }
        }

        return piecesMatrix;
    }

    public void initialSetup() {
        this.board.placePiece(new Rook(this.board, Color.BLACK), new Position(0,0));
        this.board.placePiece(new Rook(this.board, Color.BLACK), new Position(0,7));

        this.board.placePiece(new Rook(this.board, Color.WHITE), new Position(7,0));
        this.board.placePiece(new Rook(this.board, Color.WHITE), new Position(7,7));

        this.board.placePiece(new King(this.board, Color.BLACK), new Position(0,4));
        this.board.placePiece(new King(this.board, Color.WHITE), new Position(7,4));


    }

}
