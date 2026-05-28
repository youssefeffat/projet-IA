# Plan Final — Rendu 3 Escampe (Ichou-Effat)
**Deadline : 30 mai 2026 | Duree max de jeu : 5 minutes (300s) par joueur**

---

## Architecture deja mise en place

Les 4 classes du package `escampe` ont ete copiees dans `src/escampe/` et le `escampeobf.jar` est a la racine :

```
ICHOU_EFFAT/
├── escampeobf.jar              OK - Serveur + JoueurAleatoire
└── src/
    ├── Partie1.java            OK - Interface fournie (ne pas modifier)
    ├── EscampeBoard.java       OK - Complet (implemente Partie1)
    └── escampe/                OK - Package cree et peuple
        ├── IJoueur.java        OK - Interface officielle
        ├── ClientJeu.java      OK - Client reseau (ne pas modifier)
        ├── Solo.java           OK - Testeur local (ne pas modifier)
        ├── Applet.java         OK - Affichage graphique (ne pas modifier)
        └── MonJoueur.java      A CREER - votre IA
```

---

## Signatures Exactes de IJoueur (a respecter impérativement)

Apres lecture de `IJoueur.java`, voici les méthodes exactes :

```java
package escampe;

public interface IJoueur {
    static final int BLANC = -1;
    static final int NOIR  =  1;

    void   initJoueur(int mycolour);       // appele 1 fois au debut par l'arbitre
    int    getNumJoueur();                 // retourne BLANC ou NOIR
    String choixMouvement();              // retourne "A1-B2", placement, "PASSE", ou "xxxxx"
    void   mouvementEnnemi(String coup);  // l'arbitre vous informe du coup adverse
    void   declareLeVainqueur(int colour);// fin de partie
    String binoName();                    // retourne "IchouEffat"
}
```

> [!IMPORTANT]
> - `choixMouvement()` retourne **"PASSE"** (pas "E") pour passer son tour
> - `choixMouvement()` retourne **"xxxxx"** pour signaler la fin de partie a `Solo`
> - `binoName()` est **appele en premier** par `ClientJeu` avant toute connexion
> - Le joueur **Noir commence** la partie (Solo.java ligne 92)

---

## Etapes a realiser (dans l'ordre)

---

### ETAPE 1 — Adapter EscampeBoard : ajouter copier() et gerer "PASSE"

**Fichier : `src/EscampeBoard.java`** — 2 modifications :

**1a. Methode `copier()` pour l'Alpha-Beta**
```java
public EscampeBoard copier() {
    EscampeBoard clone = new EscampeBoard();
    for (int i = 0; i < 6; i++)
        System.arraycopy(this.board[i], 0, clone.board[i], 0, 6);
    clone.posLicorneNoire   = this.posLicorneNoire.clone();
    clone.posLicorneBlanche = this.posLicorneBlanche.clone();
    clone.dernierLisere     = this.dernierLisere;
    clone.currentPlayer     = this.currentPlayer;
    clone.isGameOver        = this.isGameOver;
    return clone;
}
```

**1b. Normaliser "PASSE" en entree de isValidMove() et play()**
```java
// En tete de isValidMove() et play() :
if ("PASSE".equals(move)) move = "E";
```

---

### ETAPE 2 — Creer escampe/MonJoueur.java

```java
package escampe;

public class MonJoueur implements IJoueur {

    private static final int BLANC = IJoueur.BLANC; // -1
    private static final int NOIR  = IJoueur.NOIR;  //  1

    private EscampeBoard plateau;
    private int    maCouleur;       // BLANC=-1 ou NOIR=1
    private String maCouleurStr;    // "blanc" ou "noir"
    private String couleurAdvStr;   // "noir" ou "blanc"
    private long   tempsDebutPartie;
    private static final long TEMPS_TOTAL_MS = 295_000L; // 5 min - 5s securite

    // --- Interface IJoueur ---

    @Override
    public void initJoueur(int mycolour) {
        this.maCouleur     = mycolour;
        this.maCouleurStr  = (mycolour == BLANC) ? "blanc" : "noir";
        this.couleurAdvStr = (mycolour == BLANC) ? "noir"  : "blanc";
        this.plateau       = new EscampeBoard();
        this.tempsDebutPartie = System.currentTimeMillis();
    }

    @Override
    public int getNumJoueur() { return maCouleur; }

    @Override
    public String choixMouvement() {
        if (plateau.gameOver()) return "xxxxx";

        long tempsEcoule  = System.currentTimeMillis() - tempsDebutPartie;
        long tempsRestant = TEMPS_TOTAL_MS - tempsEcoule;
        if (tempsRestant <= 0) return "PASSE";

        String meilleurCoup = chercherMeilleurCoup(tempsRestant);
        plateau.play("PASSE".equals(meilleurCoup) ? "E" : meilleurCoup, maCouleurStr);
        return meilleurCoup;
    }

    @Override
    public void mouvementEnnemi(String coup) {
        String coupInterne = "PASSE".equals(coup) ? "E" : coup;
        plateau.play(coupInterne, couleurAdvStr);
    }

    @Override
    public void declareLeVainqueur(int colour) {
        System.out.println(colour == maCouleur ? "Gagne !" : "Perdu.");
    }

    @Override
    public String binoName() { return "IchouEffat"; }

    // --- IA : Iterative Deepening + Alpha-Beta ---

    private String chercherMeilleurCoup(long tempsRestantMs) {
        String[] coups = plateau.possiblesMoves(maCouleurStr);
        if (coups.length == 1 && "E".equals(coups[0])) return "PASSE";
        if (coups.length == 1) return coups[0];

        String meilleurCoup = coups[0];
        long deadline = System.currentTimeMillis() + Math.min(tempsRestantMs, 4000L);

        for (int prof = 1; prof <= 8; prof++) {
            if (System.currentTimeMillis() >= deadline - 50) break;
            String candidat = chercherAProf(prof, deadline);
            if (candidat != null) meilleurCoup = candidat;
        }
        return meilleurCoup;
    }

    private String chercherAProf(int profondeur, long deadline) {
        String[] coups = plateau.possiblesMoves(maCouleurStr);
        String meilleur = null;
        int meilleurScore = Integer.MIN_VALUE;

        for (String coup : coups) {
            if (System.currentTimeMillis() >= deadline - 50) return meilleur;
            EscampeBoard copie = plateau.copier();
            copie.play("E".equals(coup) ? "E" : coup, maCouleurStr);
            int score = alphaBeta(copie, profondeur - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, deadline);
            if (score > meilleurScore) {
                meilleurScore = score;
                meilleur = "E".equals(coup) ? "PASSE" : coup;
            }
        }
        return meilleur;
    }

    private int alphaBeta(EscampeBoard etat, int prof, int alpha, int beta,
                          boolean maximisant, long deadline) {
        if (prof == 0 || etat.gameOver() || System.currentTimeMillis() >= deadline)
            return evaluer(etat);

        String joueur = maximisant ? maCouleurStr : couleurAdvStr;
        String[] coups = etat.possiblesMoves(joueur);

        if (maximisant) {
            int val = Integer.MIN_VALUE;
            for (String coup : coups) {
                EscampeBoard copie = etat.copier();
                copie.play("E".equals(coup) ? "E" : coup, joueur);
                val   = Math.max(val, alphaBeta(copie, prof-1, alpha, beta, false, deadline));
                alpha = Math.max(alpha, val);
                if (beta <= alpha) break;
            }
            return val;
        } else {
            int val = Integer.MAX_VALUE;
            for (String coup : coups) {
                EscampeBoard copie = etat.copier();
                copie.play("E".equals(coup) ? "E" : coup, joueur);
                val  = Math.min(val, alphaBeta(copie, prof-1, alpha, beta, true, deadline));
                beta = Math.min(beta, val);
                if (beta <= alpha) break;
            }
            return val;
        }
    }

    // --- Fonction d'evaluation heuristique ---

    private int evaluer(EscampeBoard etat) {
        if (etat.gameOver()) return 100_000; // on a capture la licorne adverse

        int mesCoups = etat.possiblesMoves(maCouleurStr).length;
        int sesCoups = etat.possiblesMoves(couleurAdvStr).length;
        int score = 3 * (mesCoups - sesCoups);

        if (mesCoups == 1 && "E".equals(etat.possiblesMoves(maCouleurStr)[0])) score -= 500;
        if (sesCoups == 1 && "E".equals(etat.possiblesMoves(couleurAdvStr)[0])) score += 500;

        return score;
    }
}
```

---

### ETAPE 3 — Creer le fichier mainClass

**Fichier : `ICHOU_EFFAT/mainClass`** (sans extension, sans espaces)

```
jar:IchouEffat.jar
clientClass:escampe.ClientJeu
mainClass:escampe.MonJoueur
```

> [!CAUTION]
> Ce fichier est obligatoire pour le tournoi. Format exact : pas d'espaces autour des `:`.

---

### ETAPE 4 — Compiler et tester avec Solo

```bash
# Depuis src/
javac -cp ../escampeobf.jar escampe/*.java EscampeBoard.java Partie1.java

# Test MonJoueur vs MonJoueur
java -cp .:../escampeobf.jar escampe.Solo escampe.MonJoueur escampe.MonJoueur
```

---

### ETAPE 5 — Tester en reseau contre JoueurAleatoire

```bash
# Terminal 1
java -cp escampeobf.jar escampe.ServeurJeu 1234 1

# Terminal 2 — votre IA
java -cp src:escampeobf.jar escampe.ClientJeu escampe.MonJoueur localhost 1234

# Terminal 3 — adversaire aleatoire
java -cp escampeobf.jar escampe.ClientJeu escampe.JoueurAleatoire localhost 1234
```

---

### ETAPE 6 — Creer le JAR exécutable

```bash
# Depuis ICHOU_EFFAT/
jar cvf IchouEffat.jar -C src .
jar tf IchouEffat.jar | grep "\.class"
```

---

### ETAPE 7 — Creer l'archive finale IchouEffat.tgz

Structure attendue dans l'archive :

```
IchouEffat/
├── src/
│   ├── Partie1.java
│   ├── EscampeBoard.java
│   └── escampe/
│       ├── IJoueur.java
│       ├── ClientJeu.java
│       ├── Solo.java
│       ├── Applet.java
│       └── MonJoueur.java
├── mainClass
└── IchouEffat.jar
```

```bash
tar cvzf IchouEffat.tgz IchouEffat/
```

---

### ETAPE 8 — Rapport final

Sections a completer par rapport au rendu 1 :

| Section | Contenu |
|---------|---------|
| Placement initial | Strategie adoptee |
| Algorithme | Alpha-Beta + Iterative Deepening, profondeur atteinte |
| Heuristiques testees | Ce que vous avez essaye, resultats |
| Heuristique retenue | Justification |
| Gestion du temps | Budget/coup, deadline, 5 minutes fixes |
| Tests | % victoires contre JoueurAleatoire |
| Difficultes | PASSE/E, copie plateau, etc. |

---

## Points Critiques

> [!CAUTION]
> 1. **"E" vs "PASSE"** : EscampeBoard utilise "E", le protocole IJoueur utilise "PASSE" — traduire a chaque frontiere
> 2. **Noir commence** : Solo.java ligne 92 — important pour le placement
> 3. **binoName()** appele avant initJoueur() — ne depend d'aucun etat
> 4. **copier()** indispensable dans EscampeBoard avant tout Alpha-Beta
> 5. **mainClass** sans extension, sans espaces autour des `:`
> 6. **EscampeBoard et Partie1** restent hors du package escampe (a la racine de src/)

---

## Plan de Travail (4h environ)

| Temps | Tache |
|-------|-------|
| 30 min | Etape 1 : copier() + normaliser PASSE/E |
| 1h30 | Etape 2 : MonJoueur.java complet |
| 30 min | Etapes 4-5 : tests Solo et reseau |
| 30 min | Etapes 6-7 : JAR + archive |
| 1h | Etape 8 : rapport final |
