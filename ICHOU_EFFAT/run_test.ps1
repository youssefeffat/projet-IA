# Arrête tous les processus java existants pour libérer le port 1234
Stop-Process -Name java -Force -ErrorAction SilentlyContinue

# Démarre le serveur en arrière-plan
Start-Process java -ArgumentList "-cp IchouEffat.jar escampe.ServeurJeu 1234 1" -NoNewWindow -RedirectStandardOutput "serveur.log" -RedirectStandardError "serveur.err"

# Attend 2 secondes pour que le serveur soit prêt
Start-Sleep -Seconds 2

# Connecte le Joueur Aléatoire (Blanc) en arrière-plan
Start-Process java -ArgumentList "-cp IchouEffat.jar escampe.ClientJeu escampe.JoueurAleatoire localhost 1234" -NoNewWindow -RedirectStandardOutput "rand.log" -RedirectStandardError "rand.err"

# Attend 1 seconde
Start-Sleep -Seconds 1

# Connecte notre IA MonJoueur (Noir) en arrière-plan
Start-Process java -ArgumentList "-cp IchouEffat.jar escampe.ClientJeu escampe.MonJoueur localhost 1234" -NoNewWindow -RedirectStandardOutput "monjoueur.log" -RedirectStandardError "monjoueur.err"

Write-Host "Partie lancée ! Les joueurs calculent leurs coups en arrière-plan."
Write-Host "Vous pouvez suivre la partie en lisant le fichier serveur.log :"
Write-Host "-> Get-Content serveur.log -Wait"
