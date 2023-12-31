package chess;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.pieces.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChessMatch {

    private Board board;
    private int turn;
    private Color currentPlayer;
    private List<Piece> piecesOnTheBoard;
    private List<Piece> capturedPieces;
    private boolean check;
    private boolean checkmate;
    private ChessPiece enPassantVulnerable;
    private ChessPiece promoted;

    public ChessMatch() {
        this.board = new Board(8, 8);
        this.turn = 1;
        this.currentPlayer = Color.WHITE;
        this.piecesOnTheBoard = new ArrayList<>();
        this.capturedPieces = new ArrayList<>();
        this.check = false;
        this.initialSetup();
    }

    public int getTurn() {
        return turn;
    }

    public boolean isCheck() {
        return check;
    }

    public boolean isCheckmate() {
        return checkmate;
    }

    public Color getCurrentPlayer() {
        return currentPlayer;
    }

    public ChessPiece getEnPassantVulnerable() {
        return enPassantVulnerable;
    }

    public ChessPiece getPromoted() {
        return promoted;
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
        if (testCheck(this.currentPlayer)) {
            this.undoMove(source, target, capturedPiece);
            throw new ChesException("You can't put yourself in check");
        }
        ChessPiece movedPiece = (ChessPiece) this.board.piece(target);

        // promotion
        this.promoted = null;
        if (movedPiece instanceof Piece) {
            if (movedPiece.getColor() == Color.WHITE && target.getRow() == 0 || movedPiece.getColor() == Color.BLACK && target.getRow() == 7) {
                this.promoted = (ChessPiece) this.board.piece(target);
                this.promoted = this.replacePromotedPiece("Q");
            }
        }


        this.check = (this.testCheck(this.opponent(this.currentPlayer)));

        if (this.testCheckMate(this.opponent(this.currentPlayer))) {
            this.checkmate = true;
        } else {
            this.nextTurn();
        }

        // en passant
        if (movedPiece instanceof Pawn && (target.getRow() == source.getRow() - 2 || target.getRow() == source.getRow() + 2)) {
            this.enPassantVulnerable = movedPiece;
        } else {
            this.enPassantVulnerable = null;
        }

        return (ChessPiece) capturedPiece;
    }

    public ChessPiece replacePromotedPiece(String type) {
        if (this.promoted == null) {
            throw new IllegalStateException("There is no piece to be promoted");
        }
        if (!type.equals("B") && !type.equals("N") && !type.equals("R") && !type.equals("Q")) {
            return this.promoted;
        }

        Position pos = this.promoted.getChessPosition().toPosition();
        Piece p = this.board.removePiece(pos);
        piecesOnTheBoard.remove(p);

        ChessPiece newPiece = this.newPiece(type, this.promoted.getColor());
        this.board.placePiece(newPiece, pos);
        this.piecesOnTheBoard.add(newPiece);

        return newPiece;
    }

    private ChessPiece newPiece(String type, Color color) {
        if (type.equals("B")) return new Bishop(this.board, color);
        if (type.equals("N")) return new Knight(this.board, color);
        if (type.equals("R")) return new Rook(this.board, color);
        return new Queen(this.board, color);
    }

    private Piece makeMove(Position source, Position target) {
        ChessPiece piece = (ChessPiece) this.board.removePiece(source);
        piece.increaseMoveCount();
        Piece capturedPiece = this.board.removePiece(target);
        this.board.placePiece(piece, target);
        if (capturedPiece != null) {
            this.piecesOnTheBoard.remove(capturedPiece);
            this.capturedPieces.add(capturedPiece);
        }

        // king side castling
        if (piece instanceof King && target.getColumn() == source.getColumn() + 2) {
            Position rookSource = new Position(source.getRow(), source.getColumn() + 3);
            Position rookTarget = new Position(source.getRow(), source.getColumn() + 1);
            ChessPiece rook = (ChessPiece) this.board.removePiece(rookSource);
            this.board.placePiece(rook, rookTarget);
            rook.increaseMoveCount();
        }

        // queen side castling
        if (piece instanceof King && target.getColumn() == source.getColumn() - 2) {
            Position rookSource = new Position(source.getRow(), source.getColumn() - 4);
            Position rookTarget = new Position(source.getRow(), source.getColumn() - 1);
            ChessPiece rook = (ChessPiece) this.board.removePiece(rookSource);
            this.board.placePiece(rook, rookTarget);
            rook.increaseMoveCount();
        }

        // en passant
        if (piece instanceof Pawn) {
            if (source.getColumn() != target.getColumn() && capturedPiece == null) {
                Position pawnPosition;
                if (piece.getColor() == Color.WHITE) {
                    pawnPosition = new Position(target.getRow() + 1, target.getColumn());
                } else {
                    pawnPosition = new Position(target.getRow() - 1, target.getColumn());
                }
                capturedPiece = this.board.removePiece(pawnPosition);
                capturedPieces.add(capturedPiece);
                piecesOnTheBoard.remove(capturedPiece);
            }
        }

        return capturedPiece;
    }

    private void undoMove(Position source, Position target, Piece capturedPiece) {
        ChessPiece piece = (ChessPiece) this.board.removePiece(target);
        piece.decreaseMoveCount();
        this.board.placePiece(piece, source);

        if (capturedPiece != null) {
            this.board.placePiece(capturedPiece, target);
            this.capturedPieces.remove(capturedPiece);
            this.piecesOnTheBoard.add(capturedPiece);
        }

        // king side castling
        if (piece instanceof King && target.getColumn() == source.getColumn() + 2) {
            Position rookSource = new Position(source.getRow(), source.getColumn() + 3);
            Position rookTarget = new Position(source.getRow(), source.getColumn() + 1);
            ChessPiece rook = (ChessPiece) this.board.removePiece(rookTarget);
            this.board.placePiece(rook, rookSource);
            rook.decreaseMoveCount();
        }

        // queen side castling
        if (piece instanceof King && target.getColumn() == source.getColumn() - 2) {
            Position rookSource = new Position(source.getRow(), source.getColumn() - 4);
            Position rookTarget = new Position(source.getRow(), source.getColumn() - 1);
            ChessPiece rook = (ChessPiece) this.board.removePiece(rookTarget);
            this.board.placePiece(rook, rookSource);
            rook.decreaseMoveCount();
        }

        // en passant
        if (piece instanceof Pawn) {
            if (source.getColumn() != target.getColumn() && capturedPiece == enPassantVulnerable) {
                ChessPiece pawn = (ChessPiece) this.board.removePiece(target);
                Position pawnPosition;
                if (piece.getColor() == Color.WHITE) {
                    pawnPosition = new Position(3, target.getColumn());
                } else {
                    pawnPosition = new Position(4, target.getColumn());
                }
                this.board.placePiece(pawn, pawnPosition);
            }
        }
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

    private Color opponent(Color color) {
        return color == Color.WHITE ? Color.BLACK : Color.WHITE;
    }

    private ChessPiece king(Color color) {
        List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color).collect(Collectors.toList());
        for (Piece p : list) {
            if (p instanceof King) {
                return (ChessPiece) p;
            }
        }
        throw new IllegalStateException("There is no " + color + " king on the board");
    }

    private boolean testCheck(Color color) {
        Position kingPosition = this.king(color).getChessPosition().toPosition();
        List<Piece> opponentsPiece = this.piecesOnTheBoard.stream().filter(x -> ((ChessPiece)x).getColor() == this.opponent(color)).toList();

        for (Piece p: opponentsPiece) {
            boolean[][] mat = p.possibleMoves();
            if (mat[kingPosition.getRow()][kingPosition.getColumn()]) {
                return true;
            }
        }
        return false;
    }

    private boolean testCheckMate(Color color) {
        if (!this.testCheck(color)) {
            return false;
        }
        List<Piece> list = this.piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color).toList();
        for (Piece p : list) {
            boolean mat[][] = p.possibleMoves();
            for (int i = 0; i < this.board.getRows(); i++) {
                for (int j = 0; j < this.board.getColumns(); j++) {
                    if (mat[i][j]) {
                        Position source = ((ChessPiece)p).getChessPosition().toPosition();
                        Position target = new Position(i, j);
                        Piece capturedPiece = this.makeMove(source, target);
                        boolean testCheck = this.testCheck(color);
                        this.undoMove(source, target, capturedPiece);
                        if (!testCheck) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void placeNewPiece(char column, int row, ChessPiece piece) {
        this.board.placePiece(piece, new ChessPosition(column, row).toPosition());
        this.piecesOnTheBoard.add(piece);
    }

    public void initialSetup() {
        this.placeNewPiece('a', 8, new Rook(this.board, Color.BLACK));
        this.placeNewPiece('b', 8, new Knight(this.board, Color.BLACK));
        this.placeNewPiece('c', 8, new Bishop(this.board, Color.BLACK));
        this.placeNewPiece('d', 8, new Queen(this.board, Color.BLACK));
        this.placeNewPiece('e', 8,new King(this.board, Color.BLACK, this));
        this.placeNewPiece('f', 8, new Bishop(this.board, Color.BLACK));
        this.placeNewPiece('g', 8, new Knight(this.board, Color.BLACK));
        this.placeNewPiece('h', 8, new Rook(this.board, Color.BLACK));
        this.placeNewPiece('a', 7, new Pawn(this.board, Color.BLACK, this));
        this.placeNewPiece('b', 7, new Pawn(this.board, Color.BLACK, this));
        this.placeNewPiece('c', 7, new Pawn(this.board, Color.BLACK, this));
        this.placeNewPiece('d', 7, new Pawn(this.board, Color.BLACK, this));
        this.placeNewPiece('e', 7, new Pawn(this.board, Color.BLACK, this));
        this.placeNewPiece('f', 7, new Pawn(this.board, Color.BLACK, this));
        this.placeNewPiece('g', 7, new Pawn(this.board, Color.BLACK, this));
        this.placeNewPiece('h', 7, new Pawn(this.board, Color.BLACK, this));



        this.placeNewPiece('a', 1,new Rook(this.board, Color.WHITE));
        this.placeNewPiece('b', 1,new Knight(this.board, Color.WHITE));
        this.placeNewPiece('c', 1,new Bishop(this.board, Color.WHITE));
        this.placeNewPiece('d', 1,new Queen(this.board, Color.WHITE));
        this.placeNewPiece('e', 1,new King(this.board, Color.WHITE, this));
        this.placeNewPiece('f', 1,new Bishop(this.board, Color.WHITE));
        this.placeNewPiece('g', 1,new Knight(this.board, Color.WHITE));
        this.placeNewPiece('h', 1,new Rook(this.board, Color.WHITE));
        this.placeNewPiece('a', 2, new Pawn(this.board, Color.WHITE, this));
        this.placeNewPiece('b', 2, new Pawn(this.board, Color.WHITE, this));
        this.placeNewPiece('c', 2, new Pawn(this.board, Color.WHITE, this));
        this.placeNewPiece('d', 2, new Pawn(this.board, Color.WHITE, this));
        this.placeNewPiece('e', 2, new Pawn(this.board, Color.WHITE, this));
        this.placeNewPiece('f', 2, new Pawn(this.board, Color.WHITE, this));
        this.placeNewPiece('g', 2, new Pawn(this.board, Color.WHITE,this));
        this.placeNewPiece('h', 2, new Pawn(this.board, Color.WHITE, this));
    }


}
