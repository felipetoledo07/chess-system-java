package chess;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.pieces.King;
import chess.pieces.Rook;

import java.util.ArrayList;
import java.util.List;

public class ChessMatch {

    private Board board;
    private int turn;
    private Color currentPlayer;
    private List<Piece> piecesOnTheBoard;
    private List<Piece> capturedPieces;

    public ChessMatch() {
        this.board = new Board(8, 8);
        this.turn = 1;
        this.currentPlayer = Color.WHITE;
        this.piecesOnTheBoard = new ArrayList<>();
        this.capturedPieces = new ArrayList<>();
        this.initialSetup();
    }

    public int getTurn() {
        return turn;
    }

    public Color getCurrentPlayer() {
        return currentPlayer;
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

    public boolean[][] possibleMoves (ChessPosition sourcePosition) {
        Position position = sourcePosition.toPosition();
        this.validateSourcePosition(position);
        return this.board.piece(position).possibleMoves();

    }

    public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
        Position source = sourcePosition.toPosition();
        Position target = targetPosition.toPosition();
        this.validateSourcePosition(source);
        this.validateTargetPosition(source, target);
        Piece capturedPiece = this.makeMove(source, target);
        this.nextTurn();
        return (ChessPiece) capturedPiece;
    }

    private Piece makeMove(Position source, Position target) {
        Piece piece = this.board.removePiece(source);
        Piece capturedPiece = this.board.removePiece(target);
        this.board.placePiece(piece, target);
        if (capturedPiece != null) {
            this.piecesOnTheBoard.remove(capturedPiece);
            this.capturedPieces.add(capturedPiece);
        }
        return capturedPiece;
    }

    private void validateSourcePosition(Position position) {
        if (!this.board.thereIsAPiece(position)) {
            throw new ChesException("There is no piece on source position");
        }
        if (this.currentPlayer != ((ChessPiece) this.board.piece(position)).getColor()) {
            throw new ChesException("The chosen piece is not yours");
        }
        if (!this.board.piece(position).isThereAnyPossibleMove()) {
            throw new ChesException("There is no possible move for the chosen piece");
        }
    }

    private void validateTargetPosition(Position source, Position target) {
        if (!this.board.piece(source).possibleMove(target)) {
            throw new ChesException("The chosen piece can't move to target position");
        }
    }

    private void nextTurn() {
        this.turn++;
        this.currentPlayer = currentPlayer == Color.WHITE ? Color.BLACK : Color.WHITE;
    }


    private void placeNewPiece(char column, int row, ChessPiece piece) {
        this.board.placePiece(piece, new ChessPosition(column, row).toPosition());
        this.piecesOnTheBoard.add(piece);
    }

    public void initialSetup() {
        this.placeNewPiece('a', 8, new Rook(this.board, Color.BLACK));
        this.placeNewPiece('h', 8, new Rook(this.board, Color.BLACK));

        this.placeNewPiece('a', 1,new Rook(this.board, Color.WHITE));
        this.placeNewPiece('h', 1,new Rook(this.board, Color.WHITE));

        this.placeNewPiece('e', 8,new King(this.board, Color.BLACK));
        this.placeNewPiece('e', 1,new King(this.board, Color.WHITE));
    }


}
