package escampe;

/**
 * Joueur IA pour le jeu Escampe.
 * Implemente l'interface officielle du tournoi (escampe.e dans le JAR obfusque).
 *
 * Mapping des methodes obfusquees (verifie par decompilation de JoueurAleatoire) :
 *   void a(int)   -> initJoueur(int mycolour)
 *   int  a()      -> getNumJoueur()
 *   String b()    -> choixMouvement()
 *   void b(int)   -> declareLeVainqueur(int colour)
 *   void a(String)-> mouvementEnnemi(String coup)
 *   String c()    -> binoName()
 *
 * Algorithme : Iterative Deepening + Alpha-Beta avec elagage.
 * Heuristique : mobilite + penalite passage de tour.
 *
 * @author Abdoullah ICHOU - Youssef EFFAT
 */
public class MonJoueur implements e {

    private static final int BLANC = -1;
    private static final int NOIR  =  1;

    private EscampeBoard plateau;
    private int    maCouleur;
    private String maCouleurStr;
    private String couleurAdvStr;

    // Gestion du temps : 5 min = 300s, marge de 5s
    private long   tempsDebutPartie;
    private static final long TEMPS_TOTAL_MS = 295_000L;
    private static final long BUDGET_COUP_MS  = 3_500L;

    // Flag : a-t-on deja joue le placement initial ?
    private boolean aPlace = false;

    // ─────────────────────────────────────────────────────────────────────────
    // Interface escampe.e (IJoueur obfusque)
    // ─────────────────────────────────────────────────────────────────────────

    /** initJoueur(int mycolour) */
    @Override
    public void a(int mycolour) {
        this.maCouleur      = mycolour;
        this.maCouleurStr   = (mycolour == BLANC) ? "blanc" : "noir";
        this.couleurAdvStr  = (mycolour == BLANC) ? "noir"  : "blanc";
        this.plateau        = new EscampeBoard();
        this.tempsDebutPartie = System.currentTimeMillis();
        this.aPlace         = false;
        System.out.println(c() + " initialise en tant que " + maCouleurStr);
    }

    /** getNumJoueur() */
    @Override
    public int a() {
        return maCouleur;
    }

    /**
     * choixMouvement() — calcule et retourne le coup a jouer.
     * Phase 1 : placement initial (retour d'un coup "/" fixe et equilibre).
     * Phase 2 : jeu normal via Iterative Deepening + Alpha-Beta.
     */
    @Override
    public String b() {
        // Phase de placement initial
        if (!aPlace) {
            aPlace = true;
            String placement = coupPlacementDefaut();
            plateau.play(placement, maCouleurStr);
            System.out.println("[" + c() + "] Placement : " + placement);
            return placement;
        }

        // Fin de partie
        if (plateau.gameOver()) return "xxxxx";

        // Calcul du budget temps restant
        long tempsEcoule  = System.currentTimeMillis() - tempsDebutPartie;
        long tempsRestant = TEMPS_TOTAL_MS - tempsEcoule;
        if (tempsRestant <= 0) {
            jouerSurPlateauInterne("E");
            return "E";
        }

        long deadline = System.currentTimeMillis() + Math.min(tempsRestant, BUDGET_COUP_MS);

        // DEBUG : afficher l'etat de notre plateau interne
        StringBuilder dbg = new StringBuilder();
        dbg.append("[").append(c()).append("] Mon plateau interne (dernierLisere=").append(plateau.getDernierLisere()).append("):\n");
        char[][] b = plateau.getBoard();
        dbg.append("   ABCDEF\n");
        for (int row = 0; row < 6; row++) {
            int lineNum = 6 - row;
            dbg.append(String.format("%02d ", lineNum));
            for (int col = 0; col < 6; col++) dbg.append(b[row][col]);
            dbg.append(String.format(" %02d\n", lineNum));
        }
        dbg.append("   ABCDEF");
        System.out.println(dbg.toString());

        String meilleurCoup = chercherMeilleurCoup(deadline);

        System.out.println("[" + c() + "] Je joue: " + meilleurCoup + " (dernierLisere=" + plateau.getDernierLisere() + ")");
        jouerSurPlateauInterne(meilleurCoup);
        return meilleurCoup;

    }

    /** declareLeVainqueur(int colour) */
    @Override
    public void b(int colour) {
        if (colour == maCouleur)
            System.out.println("=== " + c() + " a GAGNE ! ===");
        else
            System.out.println("=== " + c() + " a perdu. ===");
    }

    /** mouvementEnnemi(String coup) */
    @Override
    public void a(String coup) {
        System.out.println(">>> mouvementEnnemi RECU: [" + coup + "]");
        String coupInterne = "PASSE".equals(coup) ? "E" : coup;
        System.out.println("[" + c() + "] mouvementEnnemi: " + coup + " (" + couleurAdvStr + ")");
        plateau.play(coupInterne, couleurAdvStr);
        System.out.println("[" + c() + "] dernierLisere apres coup adv: " + plateau.getDernierLisere());
    }

    /** binoName() — appele en premier par ClientJeu/Solo */
    @Override
    public String c() {
        return "IchouEffat";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Placement initial equilibre (une licorne + 5 paladins)
    // Format : "UNICORNE/PAL1/PAL2/PAL3/PAL4/PAL5"
    // Critere : couvrir les 3 types de lisere (simple, double, triple)
    // ─────────────────────────────────────────────────────────────────────────

    private String coupPlacementDefaut() {
        if (maCouleur == NOIR) {
            // Noir joue sur les rangees 5-6 (bord bas)
            // C6(lisere2,licorne), D6(3), A6(1), A5(3), B5(1), F5(2)
            return "C6/D6/A6/A5/B5/F5";
        } else {
            // Blanc joue sur les rangees 1-2 (bord haut)
            // C1(lisere2,licorne), A1(3), F1(2), A2(1), C2(1), D2(3)
            return "C1/A1/F1/A2/C2/D2";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitaire
    // ─────────────────────────────────────────────────────────────────────────

    private void jouerSurPlateauInterne(String coup) {
        String coupInterne = "PASSE".equals(coup) ? "E" : coup;
        plateau.play(coupInterne, maCouleurStr);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IA : Iterative Deepening + Alpha-Beta
    // ─────────────────────────────────────────────────────────────────────────

    private String chercherMeilleurCoup(long deadline) {
        String[] coups = plateau.possiblesMoves(maCouleurStr);

        if (coups.length == 0)                              return "E";
        if (coups.length == 1 && "E".equals(coups[0]))      return "E";
        if (coups.length == 1)                              return coups[0];

        String meilleurCoup = "E".equals(coups[0]) ? "E" : coups[0];

        for (int prof = 1; prof <= 10; prof++) {
            if (System.currentTimeMillis() >= deadline - 50) break;
            String candidat = chercherAProf(prof, deadline);
            if (candidat != null) {
                meilleurCoup = candidat;
                System.out.println("[" + c() + "] prof=" + prof + " -> " + meilleurCoup);
            }
        }
        return meilleurCoup;
    }

    private String chercherAProf(int profondeur, long deadline) {
        String[] coups = plateau.possiblesMoves(maCouleurStr);
        String meilleur = null;
        int meilleurScore = Integer.MIN_VALUE;

        for (String coup : coups) {
            if (System.currentTimeMillis() >= deadline - 50) return meilleur;

            EscampeBoard copie = plateau.deepCopy();
            String coupInterne = "E".equals(coup) ? "E" : coup;
            copie.play(coupInterne, maCouleurStr);

            int score;
            if (copie.gameOver()) {
                score = 100_000 + profondeur; // victoire rapide = meilleur
            } else {
                score = alphaBeta(copie, profondeur - 1,
                                  Integer.MIN_VALUE, Integer.MAX_VALUE,
                                  false, deadline);
            }

            if (score > meilleurScore) {
                meilleurScore = score;
                meilleur = "E".equals(coup) ? "PASSE" : coup;
            }
        }
        return meilleur;
    }

    private int alphaBeta(EscampeBoard etat, int prof, int alpha, int beta,
                          boolean maximisant, long deadline) {

        if (prof == 0 || System.currentTimeMillis() >= deadline)
            return evaluer(etat);

        String joueur = maximisant ? maCouleurStr : couleurAdvStr;
        String[] coups = etat.possiblesMoves(joueur);

        if (maximisant) {
            int val = Integer.MIN_VALUE;
            for (String coup : coups) {
                EscampeBoard copie = etat.deepCopy();
                copie.play("E".equals(coup) ? "E" : coup, joueur);
                int score = copie.gameOver()
                        ? 100_000 + prof
                        : alphaBeta(copie, prof - 1, alpha, beta, false, deadline);
                val   = Math.max(val, score);
                alpha = Math.max(alpha, val);
                if (beta <= alpha) break;
            }
            return val;
        } else {
            int val = Integer.MAX_VALUE;
            for (String coup : coups) {
                EscampeBoard copie = etat.deepCopy();
                copie.play("E".equals(coup) ? "E" : coup, joueur);
                int score = copie.gameOver()
                        ? -100_000 - prof
                        : alphaBeta(copie, prof - 1, alpha, beta, true, deadline);
                val  = Math.min(val, score);
                beta = Math.min(beta, val);
                if (beta <= alpha) break;
            }
            return val;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fonction d'evaluation heuristique
    // ─────────────────────────────────────────────────────────────────────────

    private int evaluer(EscampeBoard etat) {
        String[] mesCoups = etat.possiblesMoves(maCouleurStr);
        String[] sesCoups = etat.possiblesMoves(couleurAdvStr);

        int score = 3 * (mesCoups.length - sesCoups.length);

        // Penalite forte si force de passer
        if (mesCoups.length == 1 && "E".equals(mesCoups[0])) score -= 500;
        if (sesCoups.length == 1 && "E".equals(sesCoups[0]))  score += 500;

        return score;
    }
}
