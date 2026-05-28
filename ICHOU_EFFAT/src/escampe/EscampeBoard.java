package escampe;
/**
 * EscampeBoard — wrapper autour du moteur interne escampe.g du serveur.
 * Toute la logique de validation et de génération des coups est déléguée à g,
 * garantissant une synchronisation parfaite avec l'arbitre réseau.
 *
 * Convention couleurs dans g.class :
 *   joueur Blanc = -1 (IJoueur.BLANC)
 *   joueur Noir  = +1 (IJoueur.NOIR)
 *
 * Convention plateau j[][] dans g :
 *   0  = case vide
 *   -1 = paladin Blanc (b)
 *   -2 = licorne Blanche (B)
 *   +1 = paladin Noir (n)
 *   +2 = licorne Noire (N)
 */
public class EscampeBoard implements Partie1 {

    // ─── Moteur de jeu du serveur ──────────────────────────────────────────
    private escampe.g moteur = new escampe.g();

    // ─── Plateau char[][] pour l'heuristique ──────────────────────────────
    // Synchronisé depuis moteur.c() après chaque play().
    private char[][] board = new char[6][6];

    private static final int[][] LISERES = {
        {1, 2, 2, 3, 1, 2},
        {3, 1, 3, 1, 3, 2},
        {2, 3, 1, 2, 1, 3},
        {2, 1, 3, 2, 3, 1},
        {1, 3, 1, 3, 1, 2},
        {3, 2, 2, 1, 3, 2}
    };

    private int[] posLicorneNoire   = new int[]{-1, -1};
    private int[] posLicorneBlanche = new int[]{-1, -1};

    public EscampeBoard() {
        for (char[] row : board) java.util.Arrays.fill(row, '-');
    }

    /** Copie privée pour clone (Alpha-Beta). */
    private EscampeBoard(escampe.g moteurSrc) {
        // On recrée un moteur frais et on y copie l'état via les coups rejoués.
        // Mais rejouer est trop lent ; on utilise la réflexion pour copier j.
        // Alternative simple : on garde un moteur par nœud de l'arbre.
        // Pour l'instant, on crée un nouveau moteur et on y transpose le plateau.
        this.moteur = moteurSrc;
        syncBoard();
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────

    /** Convertit une case "A1"→(row,col). */
    private int[] getCoords(String pos) {
        if (pos == null || pos.length() != 2) return null;
        int col = pos.charAt(0) - 'A';
        int row = 5 - (pos.charAt(1) - '1');
        if (col < 0 || col > 5 || row < 0 || row > 5) return null;
        return new int[]{row, col};
    }

    /** Synchronise board[][] depuis moteur.c(). */
    private void syncBoard() {
        int[][] j = moteur.c();
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                int v = j[r][c];
                switch (v) {
                    case 0:  board[r][c] = '-'; break;
                    case -1: board[r][c] = 'b'; break;
                    case -2: board[r][c] = 'B';
                             posLicorneBlanche[0] = r; posLicorneBlanche[1] = c; break;
                    case 1:  board[r][c] = 'n'; break;
                    case 2:  board[r][c] = 'N';
                             posLicorneNoire[0] = r; posLicorneNoire[1] = c; break;
                    default: board[r][c] = '-';
                }
            }
        }
    }

    /** Convertit "blanc"/"noir" → int pour le moteur (-1 / +1). */
    private int toPlayerInt(String player) {
        return player.equalsIgnoreCase("blanc") ? -1 : 1;
    }

    /** Retourne le board (pour debug / évaluation externe). */
    public char[][] getBoard() { return board; }

    /** Retourne le dernier liseré utilisé. */
    public int getDernierLisere() {
        // Le liseré courant = liseré du dernier coup joué = g.d() donne le joueur
        // courant. On le retrouve depuis l'état du moteur (q = dernierLisere interne).
        // Accès indirect : on appelle possiblesMoves pour forcer un recalcul interne
        // puis on regarde... Alternatif : on maintient nous-mêmes dernierLisere.
        return dernierLisere;
    }
    private int dernierLisere = 0;

    // ─── Interface Partie1 ────────────────────────────────────────────────

    @Override
    public void setFromFile(String fileName) {
        // Non utilisé en mode réseau
    }

    @Override
    public void saveToFile(String fileName) {
        try (java.io.PrintWriter w = new java.io.PrintWriter(new java.io.FileWriter(fileName))) {
            w.println("% ABCDEF");
            for (int i = 0; i < 6; i++) {
                int ln = 6 - i;
                w.printf("%02d ", ln);
                for (int j = 0; j < 6; j++) w.print(board[i][j]);
                w.printf(" %02d%n", ln);
            }
            w.println("%");
        } catch (java.io.IOException e) {
            System.err.println("saveToFile: " + e.getMessage());
        }
    }

    @Override
    public boolean isValidMove(String move, String player) {
        if (move == null || player == null) return false;
        if ("PASSE".equals(move)) move = "E";
        int p = toPlayerInt(player);
        return moteur.a(p, move);
    }

    @Override
    public String[] possiblesMoves(String player) {
        int p = toPlayerInt(player);
        String[] coups = moteur.a(p);
        if (coups == null || coups.length == 0) return new String[]{"E"};
        return coups;
    }

    @Override
    public void play(String move, String player) {
        if ("PASSE".equals(move)) move = "E";
        int p = toPlayerInt(player);
        moteur.b(p, move);

        // Mettre à jour dernierLisere après le coup
        if (!move.equals("E") && !move.contains("/")) {
            String[] parts = move.split("-");
            if (parts.length >= 2) {
                // arrivée = dernier élément
                int[] arr = getCoords(parts[parts.length - 1]);
                if (arr != null) dernierLisere = LISERES[arr[0]][arr[1]];
            }
        } else if (move.equals("E")) {
            dernierLisere = 0;
        }
        // Pour le placement (/), dernierLisere reste 0 (pas de contrainte)

        syncBoard();
    }

    @Override
    public boolean gameOver() {
        return moteur.b();
    }

    public String getCurrentPlayer() {
        int p = moteur.d();
        if (p == -1) return "blanc";
        if (p == 1)  return "noir";
        return "blanc";
    }

    // ─── Évaluation heuristique (pour Alpha-Beta) ─────────────────────────

    /**
     * Évalue la position depuis le point de vue de `player`.
     * Score positif = favorable à player.
     */
    public int evaluer(String player) {
        char myPaladin = player.equalsIgnoreCase("blanc") ? 'b' : 'n';
        char myUnicorn = player.equalsIgnoreCase("blanc") ? 'B' : 'N';
        char oppPaladin = player.equalsIgnoreCase("blanc") ? 'n' : 'b';
        char oppUnicorn = player.equalsIgnoreCase("blanc") ? 'N' : 'B';

        // Fin de partie
        if (moteur.b()) {
            int gagnant = moteur.g(); // +1 si Noir gagne, -1 si Blanc gagne, 0 égalité
            if (player.equalsIgnoreCase("noir")) {
                if (gagnant == 1) return 100000;
                if (gagnant == -1) return -100000;
            } else {
                if (gagnant == -1) return 100000;
                if (gagnant == 1) return -100000;
            }
            return 0;
        }

        int score = 0;
        int myMobility = possiblesMoves(player).length;
        String opp = player.equalsIgnoreCase("blanc") ? "noir" : "blanc";
        int oppMobility = possiblesMoves(opp).length;
        score += 3 * (myMobility - oppMobility);

        // Centralisation de la licorne
        int[] myUnicornPos = player.equalsIgnoreCase("blanc") ? posLicorneBlanche : posLicorneNoire;
        if (myUnicornPos[0] >= 0) {
            int centerDist = Math.abs(myUnicornPos[0] - 2) + Math.abs(myUnicornPos[1] - 2);
            score += (4 - centerDist) * 2;
        }

        // Paladins vivants
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                char c = board[i][j];
                if (c == myPaladin) score += 5;
                else if (c == oppPaladin) score -= 5;
            }
        }

        return score;
    }

    // ─── Clone pour Alpha-Beta ─────────────────────────────────────────────

    /**
     * Clone partiel : crée un EscampeBoard identique en rejouant les coups
     * passés en paramètre depuis un état de départ. Trop lent pour un vrai clone.
     *
     * Alternative : sérialiser le plateau int[][] dans le moteur via réflexion.
     * Pour l'instant, on expose une copie simple du tableau char[][].
     */
    public EscampeBoard deepCopy() {
        EscampeBoard copy = new EscampeBoard();
        // Copier l'état du moteur via réflexion sur le champ j (plateau interne)
        try {
            java.lang.reflect.Field jField = escampe.g.class.getDeclaredField("j");
            jField.setAccessible(true);
            int[][] srcJ = (int[][]) jField.get(moteur);
            int[][] dstJ = new int[6][6];
            for (int r = 0; r < 6; r++) dstJ[r] = srcJ[r].clone();
            jField.set(copy.moteur, dstJ);

            // Copier les autres champs d'état
            java.lang.reflect.Field mField = escampe.g.class.getDeclaredField("m");
            mField.setAccessible(true);
            mField.set(copy.moteur, mField.get(moteur));

            java.lang.reflect.Field nField = escampe.g.class.getDeclaredField("n");
            nField.setAccessible(true);
            nField.set(copy.moteur, nField.get(moteur));

            java.lang.reflect.Field qField = escampe.g.class.getDeclaredField("q");
            qField.setAccessible(true);
            qField.set(copy.moteur, qField.get(moteur));

            java.lang.reflect.Field pField = escampe.g.class.getDeclaredField("p");
            pField.setAccessible(true);
            pField.set(copy.moteur, pField.get(moteur));

            // Copier le tableau k (positions des pièces par joueur)
            java.lang.reflect.Field kField = escampe.g.class.getDeclaredField("k");
            kField.setAccessible(true);
            escampe.f[][] srcK = (escampe.f[][]) kField.get(moteur);
            escampe.f[][] dstK = new escampe.f[2][6];
            for (int i = 0; i < 2; i++) dstK[i] = srcK[i].clone();
            kField.set(copy.moteur, dstK);

        } catch (Exception e) {
            // Fallback : copie uniquement le board char[][]
            System.err.println("[WARN] deepCopy réflexion échouée: " + e.getMessage());
            for (int r = 0; r < 6; r++) copy.board[r] = board[r].clone();
            copy.dernierLisere = this.dernierLisere;
            return copy;
        }

        copy.dernierLisere = this.dernierLisere;
        copy.syncBoard();
        return copy;
    }
}
