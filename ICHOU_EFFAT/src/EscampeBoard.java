public class EscampeBoard implements Partie1 {

    private char[][] board = new char[6][6];
    
    private static final int[][] LISERES = {
        {1, 2, 2, 3, 1, 2},
        {3, 1, 3, 1, 3, 2},
        {2, 3, 1, 2, 1, 3},
        {2, 1, 3, 2, 3, 1},
        {1, 3, 1, 3, 1, 2},
        {3, 2, 2, 1, 3, 2}
    };
    
    private int[] posLicorneNoire = new int[2];
    private int[] posLicorneBlanche = new int[2];
    
    private int dernierLisere = 0;
    
    private String currentPlayer = "blanc";
    
    private boolean isGameOver = false;

    public EscampeBoard() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                board[i][j] = '-';
            }
        }
    }

    @Override
    public void setFromFile(String fileName) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("%")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    try {
                        int lineNum = Integer.parseInt(parts[0]);
                        String pieces = parts[1];
                        if (pieces.length() == 6 && lineNum >= 1 && lineNum <= 6) {
                            int i = 6 - lineNum;
                            for (int j = 0; j < 6; j++) {
                                char p = pieces.charAt(j);
                                board[i][j] = p;
                                if (p == 'B') {
                                    posLicorneBlanche[0] = i;
                                    posLicorneBlanche[1] = j;
                                } else if (p == 'N') {
                                    posLicorneNoire[0] = i;
                                    posLicorneNoire[1] = j;
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Ignorer les lignes mal formatées
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        }
    }

    @Override
    public void saveToFile(String fileName) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileName))) {
            writer.println("% ABCDEF");
            for (int i = 0; i < 6; i++) {
                int lineNum = 6 - i;
                String lineStr = String.format("%02d", lineNum);
                writer.print(lineStr + " ");
                for (int j = 0; j < 6; j++) {
                    writer.print(board[i][j]);
                }
                writer.println(" " + lineStr);
            }
            writer.println("%");
        } catch (java.io.IOException e) {
            System.err.println("Erreur lors de la sauvegarde du fichier : " + e.getMessage());
        }
    }

    private int[] getCoords(String pos) {
        if (pos == null || pos.length() != 2) return null;
        int col = pos.charAt(0) - 'A';
        int row = 5 - (pos.charAt(1) - '1');
        if (col < 0 || col > 5 || row < 0 || row > 5) return null;
        return new int[]{row, col};
    }

    private boolean checkPath(int currRow, int currCol, int destRow, int destCol, int remainingSteps, boolean[][] visited) {
        if (currRow < 0 || currRow > 5 || currCol < 0 || currCol > 5) return false;
        if (visited[currRow][currCol]) return false;

        boolean isStart = true;
        for (int i = 0; i < 6 && isStart; i++) {
            for (int j = 0; j < 6; j++) {
                if (visited[i][j]) {
                    isStart = false;
                    break;
                }
            }
        }

        if (!isStart && board[currRow][currCol] != '-') {
            if (!(currRow == destRow && currCol == destCol && remainingSteps == 0)) {
                return false;
            }
        }

        if (remainingSteps == 0) {
            return currRow == destRow && currCol == destCol;
        }

        visited[currRow][currCol] = true;

        boolean found = checkPath(currRow + 1, currCol, destRow, destCol, remainingSteps - 1, visited) ||
                        checkPath(currRow - 1, currCol, destRow, destCol, remainingSteps - 1, visited) ||
                        checkPath(currRow, currCol + 1, destRow, destCol, remainingSteps - 1, visited) ||
                        checkPath(currRow, currCol - 1, destRow, destCol, remainingSteps - 1, visited);

        visited[currRow][currCol] = false;

        return found;
    }

    @Override
    public boolean isValidMove(String move, String player) {
        if (move == null || player == null) return false;
        
        char myPaladin = player.equalsIgnoreCase("blanc") ? 'b' : 'n';
        char myUnicorn = player.equalsIgnoreCase("blanc") ? 'B' : 'N';
        char oppUnicorn = player.equalsIgnoreCase("blanc") ? 'N' : 'B';

        if (move.contains("/")) {
            String[] positions = move.split("/");
            if (positions.length != 6) return false;
            
            boolean[][] placed = new boolean[6][6];
            boolean allOn12 = true;
            boolean allOn56 = true;
            
            for (String pos : positions) {
                int[] coords = getCoords(pos);
                if (coords == null) return false;
                
                int r = coords[0];
                int c = coords[1];
                
                if (board[r][c] != '-') return false;
                if (placed[r][c]) return false;
                
                if (r != 4 && r != 5) allOn12 = false;
                if (r != 0 && r != 1) allOn56 = false;
                
                placed[r][c] = true;
            }
            
            if (!allOn12 && !allOn56) return false;
            
            if (player.equalsIgnoreCase("blanc")) {
                boolean noirSur12 = false;
                boolean noirSur56 = false;
                
                for (int i = 4; i <= 5; i++) {
                    for (int j = 0; j < 6; j++) {
                        if (board[i][j] == 'n' || board[i][j] == 'N') noirSur12 = true;
                    }
                }
                for (int i = 0; i <= 1; i++) {
                    for (int j = 0; j < 6; j++) {
                        if (board[i][j] == 'n' || board[i][j] == 'N') noirSur56 = true;
                    }
                }
                
                if (noirSur12 && allOn12) return false;
                if (noirSur56 && allOn56) return false;
            }
            
            return true;
        }

        if (move.equals("E")) {
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 6; j++) {
                    if ((board[i][j] == myPaladin || board[i][j] == myUnicorn) && 
                        (dernierLisere == 0 || LISERES[i][j] == dernierLisere)) {
                        
                        int steps = LISERES[i][j];
                        for (int r = 0; r < 6; r++) {
                            for (int c = 0; c < 6; c++) {
                                char pieceArrivee = board[r][c];
                                if (pieceArrivee == '-' || pieceArrivee == oppUnicorn) {
                                    boolean[][] visited = new boolean[6][6];
                                    if (checkPath(i, j, r, c, steps, visited)) {
                                        return false; 
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return true;
        }

        String[] parts = move.split("-");
        if (parts.length != 2) return false;
        
        int[] depart = getCoords(parts[0]);
        int[] arrivee = getCoords(parts[1]);
        
        if (depart == null || arrivee == null) return false;
        
        int departLigne = depart[0];
        int departColonne = depart[1];
        int arriveeLigne = arrivee[0];
        int arriveeColonne = arrivee[1];

        char pieceDepart = board[departLigne][departColonne];
        if (pieceDepart != myPaladin && pieceDepart != myUnicorn) {
            return false;
        }

        if (dernierLisere != 0 && LISERES[departLigne][departColonne] != dernierLisere) {
            return false;
        }

        char pieceArrivee = board[arriveeLigne][arriveeColonne];
        if (pieceArrivee != '-') {
            if (pieceArrivee != oppUnicorn) {
                return false;
            }
        }

        int steps = LISERES[departLigne][departColonne];
        boolean[][] visited = new boolean[6][6];

        return checkPath(departLigne, departColonne, arriveeLigne, arriveeColonne, steps, visited);
    }

    @Override
    public String[] possiblesMoves(String player) {
        java.util.ArrayList<String> moves = new java.util.ArrayList<>();
        char myPaladin = player.equalsIgnoreCase("blanc") ? 'b' : 'n';
        char myUnicorn = player.equalsIgnoreCase("blanc") ? 'B' : 'N';
        char oppUnicorn = player.equalsIgnoreCase("blanc") ? 'N' : 'B';

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                if ((board[i][j] == myPaladin || board[i][j] == myUnicorn) && 
                    (dernierLisere == 0 || LISERES[i][j] == dernierLisere)) {
                    
                    int steps = LISERES[i][j];
                    String posDepart = "" + (char) (j + 'A') + (char) ('1' + (5 - i));

                    for (int r = 0; r < 6; r++) {
                        for (int c = 0; c < 6; c++) {
                            char pieceArrivee = board[r][c];
                            if (pieceArrivee == '-' || pieceArrivee == oppUnicorn) {
                                boolean[][] visited = new boolean[6][6];
                                if (checkPath(i, j, r, c, steps, visited)) {
                                    String posArrivee = "" + (char) (c + 'A') + (char) ('1' + (5 - r));
                                    moves.add(posDepart + "-" + posArrivee);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (moves.isEmpty()) {
            moves.add("E");
        }

        return moves.toArray(new String[0]);
    }

    @Override
    public void play(String move, String player) {
        if (move.equals("E")) {
            currentPlayer = currentPlayer.equalsIgnoreCase("blanc") ? "noir" : "blanc";
            return;
        }

        if (move.contains("/")) {
            String[] positions = move.split("/");
            char unicorn = player.equalsIgnoreCase("blanc") ? 'B' : 'N';
            char paladin = player.equalsIgnoreCase("blanc") ? 'b' : 'n';

            for (int i = 0; i < 6; i++) {
                int[] coords = getCoords(positions[i]);
                int r = coords[0];
                int c = coords[1];
                
                if (i == 0) {
                    board[r][c] = unicorn;
                    if (player.equalsIgnoreCase("blanc")) {
                        posLicorneBlanche[0] = r;
                        posLicorneBlanche[1] = c;
                    } else {
                        posLicorneNoire[0] = r;
                        posLicorneNoire[1] = c;
                    }
                } else {
                    board[r][c] = paladin;
                }
            }
            currentPlayer = currentPlayer.equalsIgnoreCase("blanc") ? "noir" : "blanc";
            return;
        }

        String[] parts = move.split("-");
        int[] depart = getCoords(parts[0]);
        int[] arrivee = getCoords(parts[1]);

        int rDep = depart[0];
        int cDep = depart[1];
        int rArr = arrivee[0];
        int cArr = arrivee[1];

        char piece = board[rDep][cDep];
        char cible = board[rArr][cArr];

        char oppUnicorn = player.equalsIgnoreCase("blanc") ? 'N' : 'B';
        if (cible == oppUnicorn) {
            isGameOver = true;
        }

        board[rArr][cArr] = piece;
        board[rDep][cDep] = '-';
        dernierLisere = LISERES[rArr][cArr];

        if (piece == 'B') {
            posLicorneBlanche[0] = rArr;
            posLicorneBlanche[1] = cArr;
        } else if (piece == 'N') {
            posLicorneNoire[0] = rArr;
            posLicorneNoire[1] = cArr;
        }

        currentPlayer = currentPlayer.equalsIgnoreCase("blanc") ? "noir" : "blanc";
    }

    @Override
    public boolean gameOver() {
        return isGameOver;
    }

       public static void main(String[] args) {
        System.out.println("=== DEBUT DES TESTS ===");
        EscampeBoard board = new EscampeBoard();

        // 1. Tests de placement initial
        System.out.print("Test 1 (Placement Valide) : ");
        boolean test1 = board.isValidMove("C1/A1/B1/D1/E1/F1", "blanc");
        System.out.println(test1 ? "SUCCES" : "ECHEC");

        System.out.print("Test 2 (Placement Invalide) : ");
        boolean test2 = board.isValidMove("C3/A3/B3/D3/E3/F3", "blanc");
        System.out.println(!test2 ? "SUCCES" : "ECHEC");

        // 2. Tests de mouvements classiques
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                board.board[i][j] = '-';
            }
        }
        board.board[5][0] = 'B';

        System.out.print("Test 3 (Mouvement Valide) : ");
        board.dernierLisere = 3;
        boolean test3 = board.isValidMove("A1-A4", "blanc");
        System.out.println(test3 ? "SUCCES" : "ECHEC");

        System.out.print("Test 4 (Mouvement Invalide - Mauvais liseré) : ");
        board.dernierLisere = 2;
        boolean test4 = board.isValidMove("A1-A4", "blanc");
        System.out.println(!test4 ? "SUCCES" : "ECHEC");

        // 3. Tests du passage de tour ("E")
        System.out.print("Test 5 (Passage interdit) : ");
        board.dernierLisere = 3;
        boolean test5 = board.isValidMove("E", "blanc");
        System.out.println(!test5 ? "SUCCES" : "ECHEC");

        System.out.print("Test 6 (Passage autorisé) : ");
        board.board[4][0] = 'n'; // A2
        board.board[5][1] = 'n'; // B1
        boolean test6 = board.isValidMove("E", "blanc");
        System.out.println(test6 ? "SUCCES" : "ECHEC");

        // 4. Tests de play et gameOver
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                board.board[i][j] = '-';
            }
        }
        board.isGameOver = false;

        System.out.print("Test 7 (Capture et gameOver) : ");
        board.board[3][1] = 'N'; // B3
        board.board[4][1] = 'b'; // B2
        board.play("B2-B3", "blanc");
        boolean test7 = board.gameOver();
        System.out.println(test7 ? "SUCCES" : "ECHEC");

        // 5. Tests de possiblesMoves
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                board.board[i][j] = '-';
            }
        }
        board.board[5][0] = 'B'; // A1
        board.dernierLisere = 3;
        
        System.out.print("Test 8 (possiblesMoves avec chemin libre) : ");
        String[] moves1 = board.possiblesMoves("blanc");
        boolean test8 = (moves1 != null && moves1.length > 0 && !moves1[0].equals("E"));
        System.out.println(test8 ? "SUCCES" : "ECHEC");

        System.out.print("Test 9 (possiblesMoves bloqué) : ");
        board.board[4][0] = 'n'; // A2
        board.board[5][1] = 'n'; // B1
        String[] moves2 = board.possiblesMoves("blanc");
        boolean test9 = (moves2 != null && moves2.length == 1 && moves2[0].equals("E"));
        System.out.println(test9 ? "SUCCES" : "ECHEC");

        // 6. Tests des fichiers (saveToFile et setFromFile)
        System.out.print("Test 10 (Fichiers save/set symétrie) : ");
        board.board[2][3] = 'N';
        board.board[1][4] = 'b';
        board.saveToFile("test_escampe.txt");

        EscampeBoard board2 = new EscampeBoard();
        board2.setFromFile("test_escampe.txt");
        
        boolean gridsMatch = true;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                if (board.board[i][j] != board2.board[i][j]) {
                    gridsMatch = false;
                    break;
                }
            }
        }
        System.out.println(gridsMatch ? "SUCCES" : "ECHEC");

        System.out.print("Test 11 (Chargement de test_3.txt) : ");
        EscampeBoard board3 = new EscampeBoard();
        board3.setFromFile("test_escampe.txt");
        
        // D'après test_3.txt, la ligne 01 est "nnN---". 
        // Ligne 01 correspond à l'index [5]. Les cases [5][0], [5][1] et [5][2] doivent être 'n', 'n', 'N'.
        boolean test11 = (board3.board[5][0] == 'n' && board3.board[5][1] == 'n' && board3.board[5][2] == 'N');
        System.out.println(test11 ? "SUCCES" : "ECHEC");

        System.out.println("=== FIN DES TESTS ===");
    }

}
