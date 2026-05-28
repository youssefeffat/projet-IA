# Bilan Technique — Projet Escampe IA (pour Youssef)
**Abdoullah ICHOU — Youssef EFFAT | Polytech Paris-Saclay / IIM — 2025-2026**  
_Document de passation — mis à jour le 28 mai 2026 à 23h45_

---

## 🎯 Contexte rapide

Le projet consiste à implémenter un joueur IA pour le jeu **Escampe** (jeu de plateau 6×6) qui doit :
1. Implémenter l'interface `escampe.e` (obfusquée dans `escampeobf.jar`)
2. Se connecter à un serveur réseau `escampe.ServeurJeu` via `escampe.ClientJeu`
3. Jouer une partie complète en moins de **5 minutes par joueur**
4. Participer au **tournoi de la promotion**

### Commandes importantes
```bash
# Lancer le serveur
java -cp IchouEffat.jar escampe.ServeurJeu 1234 1

# Lancer notre IA (dans un autre terminal)
java -cp IchouEffat.jar escampe.ClientJeu escampe.MonJoueur localhost 1234

# Lancer un joueur aléatoire adversaire (test)
java -cp IchouEffat.jar escampe.ClientJeu escampe.JoueurAleatoire localhost 1234
```

---

## 📁 Structure du projet

```
ICHOU_EFFAT/
├── IchouEffat.jar          ← JAR de soumission (à régénérer si modif)
├── escampeobf.jar          ← JAR serveur fourni par l'enseignante (NE PAS MODIFIER)
├── mainClass               ← Fichier requis pour la soumission (déjà présent ✓)
├── rapport_IchouEffat.md   ← Rapport (INCOMPLET, à finir)
├── BILAN_PASSATION.md      ← Ce fichier
└── src/
    ├── EscampeBoard.java   ← Logique plateau (SANS package escampe)
    └── escampe/
        ├── MonJoueur.java  ← Notre IA (Alpha-Beta + Iterative Deepening)
        ├── EscampeBoard.java  ← Copie auto-générée avec "package escampe;"
        ├── Partie1.java    ← Interface plateau (fournie)
        ├── IJoueur.java    ← Interface joueur lisible (fournie)
        ├── ClientJeu.java  ← Client réseau (fourni)
        └── Solo.java       ← Mode solo (fourni)
```

### Recompiler et régénérer le JAR (commandes exactes)
```bash
cd "/home/abdoullah/ws/POLYTECH /iim/parcC/IA/projet-IA/ICHOU_EFFAT"

{ echo "package escampe;"; cat src/EscampeBoard.java; } > src/escampe/EscampeBoard.java
cd src && find . -name "*.class" -delete
javac -cp ../escampeobf.jar escampe/Partie1.java escampe/EscampeBoard.java
javac -cp .:../escampeobf.jar escampe/MonJoueur.java
cd .. && cp escampeobf.jar IchouEffat.jar
cd src && jar uf ../IchouEffat.jar escampe/EscampeBoard.class escampe/Partie1.class escampe/MonJoueur.class
```

---

## ✅ Ce qui fonctionne

### Interface réseau (MonJoueur.java)
- Connexion serveur réseau ✓
- Phase de placement acceptée par l'arbitre ✓
- Gestion du temps (5 min, budget 3.5s/coup) ✓
- Mode Solo fonctionnel ✓

### Mapping obfusqué (découvert par décompilation)

| Méthode dans MonJoueur | Correspond à dans IJoueur.java |
|------------------------|-------------------------------|
| `void a(int)`          | `initJoueur(int mycolour)`    |
| `int a()`              | `getNumJoueur()`              |
| `String b()`           | `choixMouvement()`            |
| `void b(int)`          | `declareLeVainqueur(int)`     |
| `void a(String)`       | `mouvementEnnemi(String)`     |
| `String c()`           | `binoName()`                  |

- Blanc = `-1`, Noir = `+1`

### Algorithme IA
- **Iterative Deepening Alpha-Beta** : opérationnel, atteint profondeur ~10
- **Mode Solo** : parties complètes sans crash

---

## ❌ Ce qui ne fonctionne pas (problème critique)

### Coups illégaux en réseau (ILLEGAL-MOVE)

**Symptôme** : après 1-2 coups, l'arbitre renvoie `ILLEGAL-MOVE` et on perd.

**Exemples** :
- `C6-C5-C4` → illégal (mauvais format, 3 cases au lieu de 2)
- `D6-D3` → illégal (mécanique mal comprise)
- `A6-B6` → illégal (désynchronisation plateau)

### Cause racine : mécanique de mouvement mal implémentée

Après ~4h d'analyse du bytecode de `escampe.g` (le moteur du serveur), voici ce qu'on sait :

**Format correct du coup** : `"départ-arrivée"` (2 cases, ex: `"C6-C4"`)  
Pas `"C6-C5-C4"` ! Confirmé dans `IJoueur.java` officiel et dans le bytecode.

**`dernierLisere`** = liseré de la case d'**arrivée** du coup précédent.  
Le prochain joueur doit utiliser une pièce sur une case de ce liseré.

**Ce qu'on ne comprend pas encore** : la contrainte exacte sur le nombre de pas.  
Depuis D6 (liseré 3), le coup `D6-D5` (1 pas) EST valide → le liseré n'impose pas un nombre exact de pas.  
Mais `D6-D3` (3 pas en ligne droite) a été refusé par le serveur → raison inconnue.

---

## 🔧 Solution implémentée (non encore testée en réseau)

### Délégation directe à `escampe.g`

La dernière version de `EscampeBoard.java` (compilée, dans le JAR) **délègue entièrement** la logique de mouvement au moteur officiel `escampe.g` :

```java
private escampe.g moteur = new escampe.g();

public String[] possiblesMoves(String player) {
    return moteur.a(toPlayerInt(player));  // g.a(int) = possiblesMoves du serveur
}

public void play(String move, String player) {
    moteur.b(toPlayerInt(player), move);   // g.b(int, String) = play du serveur
    syncBoard();                            // synchronise notre char[][] depuis moteur.c()
}
```

**Avantage** : synchronisation parfaite avec l'arbitre (même moteur).  
**Statut** : compilé ✓, non encore testé en réseau ⏳

### API publique de `escampe.g` (découverte par décompilation)

| Méthode | Description |
|---------|-------------|
| `String[] a(int player)` | `possiblesMoves(player)` |
| `boolean a(int player, String move)` | `isValidMove(player, move)` |
| `void b(int player, String move)` | `play(player, move)` |
| `boolean b()` | `gameOver()` |
| `int[][] c()` | Retourne le plateau int[][] |
| `int d()` | Retourne le joueur courant |
| `int g()` | Retourne le gagnant |

### `deepCopy()` pour Alpha-Beta

La copie du plateau pour l'Alpha-Beta utilise la réflexion Java sur les champs privés de `escampe.g` (j, m, n, q, p, k). **Si la réflexion échoue**, il y a un fallback basique.

---

## 📋 TODO priorisé pour Youssef

### PRIORITÉ 1 — Tester `EscampeBoard` avec délégation (30 min)
```bash
# Déjà compilé, lancer directement :
pkill -f "ServeurJeu|ClientJeu"
java -cp IchouEffat.jar escampe.ServeurJeu 1234 1 &
sleep 2
java -cp IchouEffat.jar escampe.ClientJeu escampe.JoueurAleatoire localhost 1234 &
sleep 1
java -cp IchouEffat.jar escampe.ClientJeu escampe.MonJoueur localhost 1234
```
**Vérifier** : pas de `ILLEGAL-MOVE` sur plusieurs tours.

### PRIORITÉ 2 — Fixer `deepCopy()` si besoin (1h)
Si l'Alpha-Beta fait des erreurs (joue toujours le même coup), c'est `deepCopy()` qui merde.  
Alternative robuste : au lieu de réflexion, **rejouer tous les coups depuis le début** :
```java
// Dans deepCopy() : créer un nouveau EscampeBoard et rejouer 
// la liste des coups joués depuis le début de la partie
// (stocker une List<String> coups dans EscampeBoard)
```

### PRIORITÉ 3 — Rapport (1-2h)
Sections manquantes dans `rapport_IchouEffat.md` :
- Description de l'algorithme Alpha-Beta avec les paramètres réels
- Section "Difficultés rencontrées" (problème synchronisation réseau)
- Résultats des tests (tableau ci-dessous)
- Conclusion

```
| Test | Résultat |
|------|---------|
| Mode Solo | ✅ Fonctionne |
| Réseau - Placement | ✅ Accepté |
| Réseau - Coups après délégation g | ⏳ À valider |
```

### PRIORITÉ 4 — Archive de soumission (15 min)
```bash
cd "/home/abdoullah/ws/POLYTECH /iim/parcC/IA/projet-IA"
tar czf IchouEffat.tgz ICHOU_EFFAT/mainClass ICHOU_EFFAT/IchouEffat.jar \
    ICHOU_EFFAT/src/ ICHOU_EFFAT/rapport_IchouEffat.md
```
Le fichier `mainClass` doit être **à la racine de l'archive**.

---

## ⚠️ Risques

1. **Réflexion `deepCopy()`** : peut échouer selon la JVM → Alpha-Beta incorrect
2. **Performance** : chaque nœud Alpha-Beta appelle `moteur.a(int)` sur `g.class` → potentiellement lent
3. **Placement codé en dur** : non optimal mais acceptable

---

## 📊 État final des composants

| Composant | État | Commentaire |
|-----------|------|-------------|
| `MonJoueur.java` | ✅ Complet | Interface réseau + Alpha-Beta |
| `EscampeBoard.java` (délégation g) | ⏳ Compilé, non testé réseau | Solution correcte probable |
| `rapport_IchouEffat.md` | 🔧 Incomplet | Sections résultats manquantes |
| `IchouEffat.jar` | ✅ Généré | Dernière version compilée |
| `mainClass` | ✅ Présent | Prêt pour soumission |

