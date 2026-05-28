# 🎯 Bilan & Plan — Projet Escampe (Rendu Final)
**Binôme : Abdoullah ICHOU – Youssef EFFAT**  
**Deadline : 30 mai 2026**

---

## 📊 Bilan de l'Existant

### ✅ Ce qui est fait (Rendu 1 & 2)

| Composant | Statut | Détail |
|-----------|--------|--------|
| `Partie1.java` | ✅ Complet | Interface fournie par l'enseignante, intacte |
| `EscampeBoard.java` | ✅ Complet | Implémente `Partie1`, toutes les méthodes codées |
| `setFromFile()` | ✅ Complet | Lecture du format texte plateau |
| `saveToFile()` | ✅ Complet | Sauvegarde compatible lecture |
| `isValidMove()` | ✅ Complet | Valide coups normaux, placement initial, passage `"E"` |
| `possiblesMoves()` | ✅ Complet | Retourne tous les coups légaux, retourne `"E"` si bloqué |
| `play()` | ✅ Complet | Joue le coup, détecte capture licorne → `gameOver` |
| `gameOver()` | ✅ Complet | Retourne booléen via flag interne |
| Méthode `main()` de test | ✅ Complet | 11 tests illustrant toutes les méthodes |
| Rapport Rendu 1 | ✅ Complet | Analyse modélisation, heuristiques, complexité |
| **Rapport Rendu 1 — Contenus** | ✅ | Modélisation plateau 2D, facteur branchement, heuristiques (mobilité, liserés, Manhattan), stratégies par phase, gestion temps (Iterative Deepening + Alpha-Bêta) |

### ❌ Ce qui manque (Rendu Final)

| Composant | Statut | Priorité |
|-----------|--------|----------|
| Package `escampe/` (4 classes fournies) | ❌ Absent | 🔴 CRITIQUE |
| `escampe.IJoueur` — interface implémentée par votre IA | ❌ Absent | 🔴 CRITIQUE |
| `MonJoueur.java` (ou `IchouEffat.java`) — votre IA | ❌ Absent | 🔴 CRITIQUE |
| Algorithme Minimax / Alpha-Bêta | ❌ Absent | 🔴 CRITIQUE |
| Gestion du temps réel (chrono + Iterative Deepening) | ❌ Absent | 🔴 CRITIQUE |
| Fonction d'évaluation heuristique | ❌ Absent | 🔴 CRITIQUE |
| Stratégie de placement initial | ❌ Absent | 🔴 CRITIQUE |
| Fichier `mainClass` (requis pour le tournoi) | ❌ Absent | 🔴 CRITIQUE |
| Archive `IchouEffat.jar` exécutable | ❌ Absent | 🔴 CRITIQUE |
| Rapport final (version étendue) | ❌ Absent | 🟠 IMPORTANT |
| Archive `IchouEffat.tgz` propre | ❌ Absent | 🟠 IMPORTANT |

---

## 📋 Plan Étape par Étape — Rendu Final

> **Deadline : 30 mai 2026** — Il reste ~2 jours. Priorité absolue à l'IA jouable.

---

### 🔴 ÉTAPE 1 — Récupérer les classes du paquetage `escampe` (urgent)

**Quoi faire :**
1. Télécharger sur eCampus les 4 fichiers sources Java :
   - `escampe/Applet.java`
   - `escampe/ClientJeu.java`
   - `escampe/IJoueur.java` ← **lire attentivement cette interface**
   - `escampe/Solo.java`
2. Télécharger `escampeobf.jar` (serveur + joueur aléatoire)
3. Placer tout dans un répertoire `escampe/` à la racine du projet

> [!IMPORTANT]
> Sans ces fichiers, impossible de compiler et de participer au tournoi.

---

### 🔴 ÉTAPE 2 — Créer votre classe `MonJoueur` implémentant `IJoueur`

**Fichier : `escampe/MonJoueur.java`** (ou `IchouEffat.java`)

```java
package escampe;

public class MonJoueur implements IJoueur {
    private EscampeBoard board;
    private String maCouleur;

    @Override
    public void initJeu(String plateau, int couleur) {
        // Initialiser le plateau depuis la chaîne reçue
        // Mémoriser votre couleur (blanc=1 ou noir=2 selon IJoueur)
    }

    @Override
    public String choisirCoup() {
        // ← Appeler l'algorithme Alpha-Bêta ici
        // Retourner le meilleur coup trouvé
    }

    @Override
    public void jouerCoup(String coup) {
        // Mettre à jour votre plateau interne
    }
}
```

> [!IMPORTANT]
> Les noms des méthodes doivent exactement correspondre à ceux définis dans `IJoueur.java`.  
> Lire `IJoueur.java` attentivement avant de coder.

---

### 🔴 ÉTAPE 3 — Implémenter l'algorithme Alpha-Bêta

**Dans `MonJoueur.java`** — Ajouter les méthodes :

```java
private int alphaBeta(EscampeBoard etat, int profondeur, int alpha, int beta, boolean maximisant) {
    if (profondeur == 0 || etat.gameOver()) {
        return evaluer(etat);
    }
    String[] coups = etat.possiblesMoves(joueurActuel);
    if (maximisant) {
        int meilleur = Integer.MIN_VALUE;
        for (String coup : coups) {
            EscampeBoard copie = etat.copier();
            copie.play(coup, joueurActuel);
            int val = alphaBeta(copie, profondeur-1, alpha, beta, false);
            meilleur = Math.max(meilleur, val);
            alpha = Math.max(alpha, val);
            if (beta <= alpha) break; // élagage
        }
        return meilleur;
    } else { /* ... version minimisant */ }
}
```

**Points clés :**
- Implémenter une méthode `EscampeBoard copier()` pour cloner le plateau sans modifier l'original
- Profondeur initiale : 3 à 4 (ajustable selon le temps)

---

### 🔴 ÉTAPE 4 — Implémenter la Fonction d'Évaluation Heuristique

**Selon votre rapport (rendu 1), utiliser ces 3 critères :**

```java
private int evaluer(EscampeBoard etat) {
    int score = 0;
    
    // 1. Mobilité : coups disponibles pour moi vs adversaire
    score += poids_mobilite * (mesCoups.length - sesCours.length);
    
    // 2. Distribution sur les liserés (pénalité si absent d'un liseré)
    score += penaliteAbsenceLisere(etat);
    
    // 3. Distance Manhattan : mes paladins vs licorne adverse (attaque)
    //    et ses paladins vs ma licorne (défense)
    score += distanceAttaque(etat) - distanceDefense(etat);
    
    return score;
}
```

---

### 🔴 ÉTAPE 5 — Gestion du Temps (Iterative Deepening)

**Selon votre rapport :**

```java
@Override
public String choisirCoup() {
    long debut = System.currentTimeMillis();
    long limite = debut + tempsRestant / coupsEstimes; // budget par coup
    String meilleurCoup = possiblesMoves(maCouleur)[0]; // fallback
    
    for (int prof = 1; prof <= MAX_PROF; prof++) {
        if (System.currentTimeMillis() > limite - MARGE_SECURITE) break;
        String candidat = chercherMeilleurCoup(prof);
        if (candidat != null) meilleurCoup = candidat;
    }
    return meilleurCoup;
}
```

---

### 🟠 ÉTAPE 6 — Stratégie de Placement Initial

**Dans `choisirCoup()`, détecter si c'est la phase de placement :**

```java
// Si aucune pièce n'est encore posée → phase placement
// Stratégie : placer la licorne au centre de la rangée,
//             répartir les paladins sur les 3 types de liserés
private String choisirPlacement() {
    // Retourner un coup de type "C1/A1/B1/D1/E1/F1"
    // Optimiser selon les liserés de la rangée (1 ou 2)
}
```

---

### 🟠 ÉTAPE 7 — Créer le fichier `mainClass`

**Fichier : `mainClass`** (sans extension, à la racine de l'archive)

```
jar : IchouEffat.jar
clientClass : escampe.ClientJeu
mainClass : escampe.MonJoueur
```

> [!CAUTION]
> Ce fichier est **obligatoire** pour le tournoi. Sans lui, votre IA ne sera pas acceptée.

---

### 🟠 ÉTAPE 8 — Tester avec `escampe.Solo`

**Tester votre IA contre le joueur aléatoire :**

```bash
# Terminal 1 : Lancer le serveur
java -cp escampeobf.jar escampe.ServeurJeu 1234 1

# Terminal 2 : Votre joueur (blanc)
java -cp . escampe.ClientJeu escampe.MonJoueur localhost 1234

# Terminal 3 : Joueur aléatoire (noir)
java -cp escampeobf.jar escampe.ClientJeu escampe.JoueurAleatoire localhost 1234
```

Utiliser aussi `escampe.Solo` pour des tests rapides sans réseau.

---

### 🟠 ÉTAPE 9 — Rédiger le Rapport Final

**Contenu obligatoire (selon `main-polytech.pdf`) :**

| Section | Contenu |
|---------|---------|
| **Placement initial** | Stratégie choisie et pourquoi |
| **Meilleur coup** | Comment vous choisissez le coup (Alpha-Bêta, profondeur) |
| **Heuristiques testées** | Lesquelles vous avez essayées, résultats comparatifs |
| **Heuristique retenue** | Justification des critères choisis |
| **Gestion du temps** | Comment vous gérez les 300-900s par partie |
| **Performances** | Temps moyen par coup, profondeur atteinte |
| **Tests effectués** | Résultats contre JoueurAléatoire, contre vous-mêmes |
| **Difficultés** | Problèmes rencontrés et solutions |

> Base : votre rapport du rendu 1 est déjà une excellente fondation. Enrichissez-le avec les résultats réels de tests.

---

### 🟢 ÉTAPE 10 — Créer et Vérifier l'Archive Finale

**Structure attendue :**

```
IchouEffat/
├── src/
│   ├── EscampeBoard.java
│   ├── Partie1.java
│   └── escampe/
│       ├── Applet.java
│       ├── ClientJeu.java
│       ├── IJoueur.java
│       ├── Solo.java
│       └── MonJoueur.java      ← votre IA
├── mainClass                   ← OBLIGATOIRE
└── IchouEffat.jar              ← JAR exécutable
```

**Commandes :**
```bash
# Compiler
javac -cp escampeobf.jar src/escampe/*.java src/*.java

# Créer le JAR
jar cvfm IchouEffat.jar MANIFEST.MF -C src .

# Créer l'archive tgz
tar cvzf IchouEffat.tgz IchouEffat/
```

---

## ⚠️ Points Critiques à ne Pas Oublier

> [!CAUTION]
> 1. **Nom de l'archive** : `IchouEffat.tgz` (Nom1Nom2, pas de prénom)
> 2. **Fichier `mainClass`** : obligatoire dans l'archive
> 3. **JAR exécutable** : `IchouEffat.jar` doit être lanceable via `java -cp`
> 4. **Méthodes `IJoueur`** : respecter exactement la signature de l'interface
> 5. **Durée max** : entre 300s et 900s par partie par joueur (sera précisée)
> 6. **Dépôt eCampus** : vérifier que l'archive se décompresse correctement avant soumission

---

## 🗓️ Plan de Travail sur 2 jours

| Quand | Quoi |
|-------|------|
| **Maintenant** | Récupérer les 4 classes `escampe/` sur eCampus + `escampeobf.jar` |
| **Maintenant** | Lire `IJoueur.java` et comprendre exactement les signatures |
| **Aujourd'hui** | Créer `MonJoueur.java` + `copier()` dans `EscampeBoard` + Alpha-Bêta basique |
| **Aujourd'hui** | Tester avec `escampe.Solo` (IA vs IA) |
| **Demain matin** | Affiner heuristique + Iterative Deepening + gestion temps |
| **Demain après-midi** | Tester vs JoueurAléatoire, relever les stats |
| **Demain soir** | Finaliser rapport, créer JAR + archive tgz, déposer sur eCampus |

---

## 📈 Notation (selon `main-polytech.pdf`)

| Critère | Points |
|---------|--------|
| Résultats au tournoi | 3 pts |
| Rapport écrit | ~3.5 pts |
| Techniques mises en œuvre | ~3.5 pts |

