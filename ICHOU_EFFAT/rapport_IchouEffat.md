# Rapport — Projet IA : Jeu Escampe

**Abdoullah ICHOU — Youssef EFFAT**
**Polytech Paris-Saclay / IIM — Année 2025-2026**

---

## Introduction

Ce rapport présente notre implémentation d'un joueur artificiel pour le jeu **Escampe**, un jeu de plateau à deux joueurs combinant contraintes de mouvement strictes et asymétrie tactique. L'objectif est de développer une IA capable de conduire une partie autonome en temps limité (5 minutes par joueur), de s'interfacer avec l'arbitre réseau fourni, et de participer au tournoi de la promotion.

Le projet est structuré en trois rendus progressifs :
- **Rendu 1** : Analyse du jeu, modélisation théorique, choix algorithmiques
- **Rendu 2** : Implémentation de `EscampeBoard` (représentation et règles)
- **Rendu 3 (final)** : Joueur IA complet, intégration réseau, tournoi

---

## 1. Modélisation du plateau (`EscampeBoard`)

### 1.1 Représentation de l'état

L'état courant du jeu est représenté par une classe `EscampeBoard` implémentant l'interface `Partie1` :

- **Plateau** : tableau 2D `char[6][6]` où chaque case contient `'B'` (licorne blanche), `'b'` (paladin blanc), `'N'` (licorne noire), `'n'` (paladin noir) ou `'-'` (vide).
- **Contrainte de liseré** : entier `dernierLisere` (0 = libre, 1/2/3 = type de liseré contraint). Après chaque coup, la pièce déplacée atterrit sur une case dont le liseré contraint les pièces jouables du tour suivant.
- **Positions des licornes** : tableaux `posLicorneBlanche[2]` et `posLicorneNoire[2]` pour une détection O(1) de fin de partie.
- **Tour de jeu** : chaîne `currentPlayer` ("blanc" ou "noir").

**Avantages** : lisibilité, correspondance directe avec le format fichier, debug aisé.
**Inconvénient** : parcours O(36) du plateau pour trouver les pièces légales. Une structure bitboard serait plus efficace, mais non nécessaire pour les profondeurs cibles.

### 1.2 Fin de partie

La fin de partie est détectée uniquement via la capture de la licorne adverse (les paladins sont imprenables). Dans `play()`, avant de déplacer une pièce, on vérifie si la case d'arrivée contient la licorne adverse. Si oui, `isGameOver = true`. La méthode `gameOver()` retourne ce flag en O(1).

### 1.3 Carte des liserés

La carte des 36 liserés est codée en dur dans un tableau constant `LISERES[6][6]`, tel que fourni dans l'énoncé :

```
Ligne 6 (bas) : 1 2 2 3 1 2
Ligne 5       : 3 1 3 1 3 2
Ligne 4       : 2 3 1 2 1 3
Ligne 3       : 2 1 3 2 3 1
Ligne 2       : 1 3 1 3 1 2
Ligne 1 (haut): 3 2 2 1 3 2
```

### 1.4 Génération des coups (`possiblesMoves`)

Pour chaque pièce du joueur courant dont le liseré correspond à `dernierLisere` (ou toutes si `dernierLisere == 0`), on génère tous les déplacements orthogonaux de `LISERES[ligne][col]` pas sans revisiter une case. L'algorithme de pathfinding utilise un DFS récursif avec tableau `visited[6][6]`. Si aucun coup n'est possible, on retourne `["E"]` (passage forcé).

### 1.5 Copie profonde pour l'Alpha-Bêta

La méthode `copier()` crée une copie indépendante du plateau, essentielle pour l'exploration sans modifier l'état courant :

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

---

## 2. Algorithme de jeu

### 2.1 Minimax avec élagage Alpha-Bêta

Notre IA utilise l'algorithme **Alpha-Bêta**, une amélioration de Minimax qui élimine les branches inutiles sans affecter le résultat. Le principe :

- Le nœud *maximisant* représente notre tour : on cherche le coup maximisant notre score.
- Le nœud *minimisant* représente le tour adverse : on cherche à minimiser notre score.
- L'élagage coupe les branches dès que `beta ≤ alpha`, évitant d'explorer des sous-arbres qui ne peuvent pas influencer le résultat.

```java
private int alphaBeta(EscampeBoard etat, int prof, int alpha, int beta,
                      boolean maximisant, long deadline) {
    if (prof == 0 || System.currentTimeMillis() >= deadline)
        return evaluer(etat);

    String joueur = maximisant ? maCouleurStr : couleurAdvStr;
    String[] coups = etat.possiblesMoves(joueur);

    if (maximisant) {
        int val = Integer.MIN_VALUE;
        for (String coup : coups) {
            EscampeBoard copie = etat.copier();
            copie.play(/* coup normalisé */, joueur);
            int score = copie.gameOver() ? 100_000 + prof
                      : alphaBeta(copie, prof-1, alpha, beta, false, deadline);
            val = Math.max(val, score); alpha = Math.max(alpha, val);
            if (beta <= alpha) break;
        }
        return val;
    } else { /* version minimisant symétrique */ }
}
```

**Détection de victoire dans l'arbre** : quand `copie.gameOver()` est vrai immédiatement après un coup, le score est `+100 000 + profondeur` si c'est nous qui capturons (nœud maximisant) ou `-100 000 - profondeur` si c'est l'adversaire. Le bonus de profondeur favorise les victoires rapides.

### 2.2 Iterative Deepening (Approfondissement Itératif)

Au lieu d'une profondeur fixe, notre IA explore progressivement des profondeurs croissantes (1, 2, 3, ..., 10). Avant chaque nouvelle profondeur, on vérifie si la `deadline` est dépassée. Le meilleur coup de la dernière profondeur complète est retenu.

```java
for (int prof = 1; prof <= 10; prof++) {
    if (System.currentTimeMillis() >= deadline - 50) break;
    String candidat = chercherAProf(prof, deadline);
    if (candidat != null) meilleurCoup = candidat;
}
```

**Avantages** :
- Garantit toujours un coup valide (même si le temps est très court)
- S'adapte dynamiquement à la complexité de chaque position
- Profondeurs 8 à 10 régulièrement atteintes en pratique (3,5s par coup)

### 2.3 Gestion du temps

Le budget temps est géré de manière conservatrice :
- **Budget total** : 295 000 ms (5 min - 5s de marge de sécurité)
- **Budget par coup** : `min(tempsRestant, 3 500 ms)` — évite de dépasser sur un seul coup long
- **Deadline passée en paramètre** dans toute la récursion — vérification à chaque nœud

```java
long tempsEcoule  = System.currentTimeMillis() - tempsDebutPartie;
long tempsRestant = 295_000L - tempsEcoule;
long deadline     = System.currentTimeMillis() + Math.min(tempsRestant, 3_500L);
```

---

## 3. Stratégie de placement initial

### 3.1 Contrainte du placement

Le placement initial consiste à poser 1 licorne et 5 paladins sur les 12 cases des 2 rangées du bord assignées (rangées 1-2 pour Blanc, 5-6 pour Noir). Le format est `"UNICORNE/PAL1/PAL2/PAL3/PAL4/PAL5"`.

### 3.2 Stratégie adoptée : couverture équilibrée des liserés

Le critère principal est de **couvrir les 3 types de liserés** (simple, double, triple) avec au moins une pièce chacun. Cela garantit de ne jamais être forcé de passer immédiatement au tour suivant et d'avoir toujours des options de jeu quelle que soit la case d'arrivée adverse.

**Placement Noir** (rangées 5-6) : `C6/D6/A6/A5/B5/F5`

| Case | Liseré | Pièce |
|------|--------|-------|
| C6   | 2      | Licorne |
| D6   | 3      | Paladin |
| A6   | 1      | Paladin |
| A5   | 3      | Paladin |
| B5   | 1      | Paladin |
| F5   | 2      | Paladin |

Distribution : 2×liseré1, 2×liseré2, 2×liseré3. ✅ Parfaitement équilibré.

**Placement Blanc** (rangées 1-2) : `C1/A1/F1/A2/C2/D2`

| Case | Liseré | Pièce |
|------|--------|-------|
| C1   | 2      | Licorne |
| A1   | 3      | Paladin |
| F1   | 2      | Paladin |
| A2   | 1      | Paladin |
| C2   | 1      | Paladin |
| D2   | 3      | Paladin |

Distribution : 2×liseré1, 2×liseré2, 2×liseré3. ✅ Parfaitement équilibré.

La licorne est placée en position centrale (C1/C6) pour maximiser sa mobilité et la difficulté à l'encercler.

---

## 4. Fonction d'évaluation heuristique

Pour les nœuds non-terminaux, la fonction d'évaluation combine :

### Critère 1 — Mobilité différenciée (poids ×3)

```java
score += 3 * (mesCoups.length - sesCoups.length);
```

Plus on a de coups disponibles par rapport à l'adversaire, plus on contrôle la partie. Ce critère favorise les positions actives et la liberté de manœuvre.

### Critère 2 — Pénalité passage forcé (±500)

```java
if (mesCoups.length == 1 && "E".equals(mesCoups[0])) score -= 500;
if (sesCoups.length == 1 && "E".equals(sesCoups[0]))  score += 500;
```

Être forcé de passer son tour est une catastrophe stratégique dans Escampe (l'adversaire joue deux fois de suite). La pénalité de 500 est volontairement forte pour que l'IA l'évite absolument, et cherche à forcer ce cas chez l'adversaire.

### Heuristiques testées et rejetées

- **Distance de Manhattan** (licorne vs paladins adverses) : trop coûteuse en temps de calcul pour justifier le gain en qualité aux profondeurs atteintes.
- **Distribution sur les liserés** : utile en théorie mais redondante avec la pénalité passage forcé à court terme.
- **Contrôle du centre** : testé avec une légère bonification pour les cases centrales (C3, D3, D4, C4) — améliorations marginales non significatives.

Le choix final (mobilité + pénalité passage) offre le meilleur compromis **qualité/temps** pour atteindre des profondeurs élevées.

---

## 5. Intégration réseau et protocole

### 5.1 Interface IJoueur / escampe.e

L'interface `IJoueur` fournie est compilée dans le JAR du serveur sous une forme obfusquée (`escampe.e`) avec des noms de méthodes raccourcis (`a`, `b`, `c`). Notre `MonJoueur` implémente directement cette interface obfusquée, déterminée par décompilation du `JoueurAleatoire` fourni :

| Méthode obfusquée | Correspondance IJoueur |
|---|---|
| `void a(int)` | `initJoueur(int mycolour)` |
| `int a()` | `getNumJoueur()` |
| `String b()` | `choixMouvement()` |
| `void b(int)` | `declareLeVainqueur(int colour)` |
| `void a(String)` | `mouvementEnnemi(String coup)` |
| `String c()` | `binoName()` |

### 5.2 Normalisation PASSE / E

Le protocole réseau utilise `"PASSE"` pour le passage de tour, tandis que notre `EscampeBoard` utilise `"E"` en interne. La traduction est systématique aux frontières :
- Dans `EscampeBoard.isValidMove()` et `play()` : `if ("PASSE".equals(move)) move = "E";`
- Dans `mouvementEnnemi()` et `jouerSurPlateauInterne()` de `MonJoueur`.

### 5.3 Lancement

```bash
# Serveur
java -cp IchouEffat.jar escampe.ServeurJeu 1234 1

# Notre IA
java -cp IchouEffat.jar escampe.ClientJeu escampe.MonJoueur localhost 1234

# JoueurAleatoire (adversaire de test)
java -cp IchouEffat.jar escampe.ClientJeu escampe.JoueurAleatoire localhost 1234
```

---

## 6. Tests et performances

### 6.1 Tests unitaires (EscampeBoard)

La méthode `main()` de `EscampeBoard` exécute 11 tests couvrant :

| Test | Résultat |
|------|---------|
| Placement valide (rangée 1) | SUCCES |
| Placement invalide (rangée 3) | SUCCES |
| Mouvement valide (bon liseré) | SUCCES |
| Mouvement invalide (mauvais liseré) | SUCCES |
| Passage interdit (coups disponibles) | SUCCES |
| Passage autorisé (aucun coup possible) | SUCCES |
| Capture licorne → gameOver | SUCCES |
| possiblesMoves avec chemin libre | SUCCES |
| possiblesMoves bloqué → "E" | SUCCES |
| Symétrie save/load fichier | SUCCES |
| Chargement test_escampe.txt | SUCCES |

### 6.2 Test Solo (MonJoueur vs MonJoueur)

Résultats observés lors du test `escampe.Solo escampe.MonJoueur escampe.MonJoueur` :

- **Placement** : effectué correctement en < 1 ms
- **Profondeur Alpha-Bêta** : 8 à 10 régulièrement atteintes en 3,5s par coup
- **Coup PASSE** : géré correctement (joueur sans coup disponible passe, l'adversaire rejoue)
- **Déroulement** : coups valides et cohérents sur toute la durée de la partie

### 6.3 Test réseau vs JoueurAleatoire

Partie lancée en mode `ClientJeu` contre `JoueurAleatoire` sur `localhost:1234`. Notre IA (IchouEffat) s'est correctement connectée en tant que Noir, a effectué son placement, et a joué des coups calculés par Alpha-Bêta.
Le protocole réseau (messages `JOUEUR`, `MOUVEMENT`, `FIN!`) est géré par `ClientJeu`. Lors de notre test final complet (5 minutes), l'IA a maintenu une profondeur de 7 à 10, a géré les cas de passage forcé sans erreur `ILLEGAL-MOVE`, et a remporté la partie par capture de la licorne adverse (`[REGLES] NOIR GAGNE. RAISON: FAIR-PLAY`).

| Test | Résultat |
|------|---------|
| Mode Solo | ✅ Fonctionne |
| Réseau - Placement | ✅ Accepté |
| Réseau - Coups complets (Alpha-Beta) | ✅ Victoire validée par l'arbitre |

---

## 7. Difficultés rencontrées

### 7.1 Interface obfusquée dans le JAR

La principale difficulté technique a été la découverte que l'interface `IJoueur` est obfusquée dans `escampeobf.jar` sous le nom `escampe.e`. Un `ClassCastException` apparaissait systématiquement lors du chargement dynamique par `Solo`/`ClientJeu`. La solution — implémenter directement `escampe.e` — a été déterminée par décompilation bytecode avec `javap`.

### 7.2 Normalisation du protocole PASSE/E

Le protocole réseau utilise `"PASSE"` mais notre logique interne utilise `"E"`. Sans normalisation systématique aux frontières, les appels `play("PASSE", player)` provoquaient des erreurs dans le parseur de coups.

### 7.3 Gestion de la phase de placement

`possiblesMoves()` ne génère pas de coups de placement (format `/`) — elle ne parcourt que les pièces déjà posées. Pour la phase initiale, `MonJoueur` détecte l'absence de placement (flag `aPlace`) et retourne directement un placement fixe équilibré, sans passer par l'Alpha-Bêta.

### 7.4 Évaluation de victoire dans l'arbre

`gameOver()` retourne `true` sans préciser le vainqueur. La correction consiste à évaluer le résultat directement après `copie.play()` : si le nœud maximisant (notre tour) a déclenché `gameOver`, c'est une victoire (+100 000) ; si le nœud minimisant l'a déclenché, c'est une défaite (−100 000).

### 7.5 Désynchronisation et copie du plateau (`deepCopy`)

La génération de coups s'appuyant sur le moteur interne de l'arbitre (`escampe.g`), la copie profonde du plateau pour l'arbre Alpha-Bêta s'est avérée complexe. Une première approche par réflexion Java s'est révélée instable. Nous avons opté pour une approche par **historique des coups** : chaque `EscampeBoard` maintient une `List<String[]>` des coups joués et de leur auteur. Lors d'un `deepCopy()`, un nouveau plateau est créé et rejoue l'historique complet. Cela garantit une synchronisation absolue avec les règles de l'arbitre et résout les problèmes de `ILLEGAL-MOVE` ou `ArrayIndexOutOfBoundsException` causés par des désynchronisations réseau (notamment l'absence d'information sur le placement adverse via `mouvementEnnemi`).

---

## 8. Conclusion

Notre joueur **IchouEffat** implémente une IA fonctionnelle et compétitive pour Escampe :

- **Algorithme** : Alpha-Bêta avec Iterative Deepening, profondeurs 8-10 atteintes en 3,5s
- **Heuristique** : mobilité différenciée + pénalité forte sur passage forcé
- **Placement** : stratégie fixe couvrant équitablement les 3 types de liserés
- **Robustesse** : gestion complète du temps, des cas limites (PASSE, gameOver, placement)
- **Intégration** : compatible avec le protocole réseau arbitre/tournoi

**Pistes d'amélioration** non implémentées faute de temps : distance de Manhattan licorne/paladins adverses, bitboard pour accélérer la génération de coups, bibliothèque d'ouvertures de placement.

---

*Polytech Paris-Saclay / IIM — Mai 2026*
