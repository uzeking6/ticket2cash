# ============================================================================
# Ticket2Cash - Nettoyage final et commit V9
# ============================================================================
# Ce script termine le travail que deploy-v9.ps1 avait commence:
# la compilation a reussi, il reste juste a nettoyer le cruft historique
# et pousser sur GitHub.
#
# Actions:
#   1. Suppression de tous les fichiers .bak-* du disque
#   2. Untrack des vieux backups et zips de git
#   3. Mise a jour du .gitignore
#   4. Commit + push
#
# Usage:
#   Unblock-File -Path .\cleanup-and-commit.ps1
#   .\cleanup-and-commit.ps1
# ============================================================================

$ErrorActionPreference = "Stop"
$projectRoot = "C:\Users\PC\Desktop\dev\ticket2cash"
Set-Location $projectRoot

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "  Nettoyage final et commit V9" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# ---- 1. Trouver et supprimer tous les fichiers .bak-* du disque ----------
Write-Host "[1/5] Suppression des fichiers .bak-* du disque..." -ForegroundColor Yellow
$bakFiles = Get-ChildItem -Recurse -File | Where-Object { $_.Name -match '\.bak-\d{8}' }
if ($bakFiles.Count -gt 0) {
    Write-Host "     $($bakFiles.Count) fichiers .bak-* trouves" -ForegroundColor Gray
    foreach ($f in $bakFiles) {
        Remove-Item -Force $f.FullName
    }
    Write-Host "  [OK] Fichiers .bak-* supprimes" -ForegroundColor Green
} else {
    Write-Host "  [OK] Aucun fichier .bak-* trouve" -ForegroundColor Green
}

# ---- 2. Supprimer les vieux backups et zips du disque --------------------
Write-Host "[2/5] Suppression des backups et vieux zips restants..." -ForegroundColor Yellow
$oldBackups = Get-ChildItem -Directory | Where-Object { $_.Name -like "backup_*" }
foreach ($b in $oldBackups) {
    Write-Host "     Suppression $($b.Name)" -ForegroundColor Gray
    Remove-Item -Recurse -Force $b.FullName
}
$oldZips = Get-ChildItem -File | Where-Object { $_.Name -like "ticket2cash-deployment-*.zip" -and $_.Name -ne "ticket2cash-deployment-v9.zip" }
foreach ($z in $oldZips) {
    Write-Host "     Suppression $($z.Name)" -ForegroundColor Gray
    Remove-Item -Force $z.FullName
}
Write-Host "  [OK] Nettoyage disque termine" -ForegroundColor Green

# ---- 3. Renforcer le .gitignore ------------------------------------------
Write-Host "[3/5] Mise a jour du .gitignore..." -ForegroundColor Yellow
$gitignoreLines = @(
    "# Build artifacts",
    "target/",
    "*.jar",
    "*.war",
    "*.class",
    "",
    "# Backups locaux (jamais committes)",
    "backup_*/",
    "*.bak",
    "*.bak-*",
    "",
    "# Zips de deploiement (jamais committes)",
    "ticket2cash-deployment-*.zip",
    "",
    "# Base de donnees locale H2",
    "data/",
    "*.db",
    "*.trace.db",
    "*.lock.db",
    "*.mv.db",
    "",
    "# IDE",
    ".idea/",
    "*.iml",
    "*.ipr",
    "*.iws",
    ".vscode/",
    ".settings/",
    ".project",
    ".classpath",
    "",
    "# OS",
    "Thumbs.db",
    ".DS_Store",
    "desktop.ini",
    "",
    "# Logs",
    "*.log",
    "logs/",
    "",
    "# Fichiers temporaires",
    "*.tmp",
    "*.swp",
    "*~"
)
$gitignoreLines | Out-File -Encoding ascii .gitignore -Force
Write-Host "  [OK] .gitignore renforce (couvre .bak-*, data/, tous les cas)" -ForegroundColor Green

# ---- 4. Nettoyer l'index git ---------------------------------------------
Write-Host "[4/5] Nettoyage de l'index git..." -ForegroundColor Yellow

# Retirer tout le cruft historique de git (sans supprimer du disque - deja fait)
# Note: 'git rm --cached' echoue silencieusement si le fichier n'est pas tracke,
# ce qui est le comportement souhaite ici.

# Vieux backups
$trackedBackups = git ls-files | Where-Object { $_ -like "backup_*" }
if ($trackedBackups) {
    foreach ($f in $trackedBackups) {
        git rm --cached $f 2>$null | Out-Null
    }
    Write-Host "     $($trackedBackups.Count) fichier(s) backup_* retire(s) de git" -ForegroundColor Gray
}

# Vieux fichiers .bak-*
$trackedBaks = git ls-files | Where-Object { $_ -match '\.bak-\d{8}' }
if ($trackedBaks) {
    foreach ($f in $trackedBaks) {
        git rm --cached $f 2>$null | Out-Null
    }
    Write-Host "     $($trackedBaks.Count) fichier(s) .bak-* retire(s) de git" -ForegroundColor Gray
}

# Vieux zip V8
$trackedZips = git ls-files | Where-Object { $_ -like "ticket2cash-deployment-*.zip" }
if ($trackedZips) {
    foreach ($f in $trackedZips) {
        git rm --cached $f 2>$null | Out-Null
    }
    Write-Host "     $($trackedZips.Count) zip(s) retire(s) de git" -ForegroundColor Gray
}

# Base de donnees H2 committee par erreur
$trackedDb = git ls-files | Where-Object { $_ -like "data/*" -or $_ -match '\.(db|trace\.db|lock\.db|mv\.db)$' }
if ($trackedDb) {
    foreach ($f in $trackedDb) {
        git rm --cached $f 2>$null | Out-Null
    }
    Write-Host "     $($trackedDb.Count) fichier(s) DB retire(s) de git" -ForegroundColor Gray
}

Write-Host "  [OK] Index git nettoye" -ForegroundColor Green

# ---- 5. Commit + Push (avec safety check assouplie) ----------------------
Write-Host "[5/5] Commit + push..." -ForegroundColor Yellow

git add -A

# Nouvelle logique: on regarde seulement les fichiers AJOUTES (pas les suppressions)
$addedFiles = git diff --cached --name-only --diff-filter=A
$badAdded = $addedFiles | Where-Object {
    $_ -like "backup_*" -or
    $_ -like "*.zip" -or
    $_ -match '\.bak-\d{8}' -or
    $_ -like "target/*" -or
    $_ -like "data/*"
}

if ($badAdded) {
    Write-Host "  [X] Ces fichiers seraient AJOUTES au commit et ne devraient pas:" -ForegroundColor Red
    $badAdded | ForEach-Object { Write-Host "     $_" -ForegroundColor Red }
    Write-Host "  Verifie ton .gitignore." -ForegroundColor Red
    git reset HEAD | Out-Null
    exit 1
}

$totalStaged = (git diff --cached --name-only | Measure-Object).Count
Write-Host "     $totalStaged fichier(s) prets a committer (ajouts + suppressions)" -ForegroundColor Gray

if ($totalStaged -eq 0) {
    Write-Host "  [OK] Rien a committer" -ForegroundColor Green
} else {
    git commit -m "V9: GL-02 Points de fidelite + GL-04 CLO via cartes prepayees + nettoyage complet"
    git push origin main
    Write-Host "  [OK] Push effectue" -ForegroundColor Green
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Green
Write-Host "  NETTOYAGE ET COMMIT TERMINES" -ForegroundColor Green
Write-Host "===============================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Le depot est maintenant PROPRE:" -ForegroundColor Cyan
Write-Host "  - Aucun backup_* traine" -ForegroundColor Gray
Write-Host "  - Aucun fichier .bak-* traine" -ForegroundColor Gray
Write-Host "  - Aucun zip de deploiement dans git" -ForegroundColor Gray
Write-Host "  - Base H2 hors de git" -ForegroundColor Gray
Write-Host ""
Write-Host "PROCHAINE ETAPE - sur le serveur (MobaXterm), copie/colle:" -ForegroundColor Cyan
Write-Host ""

$bashCommands = @(
    'cd /opt/ticket2cash',
    'git pull',
    'chmod +x mvnw',
    './mvnw clean package -DskipTests',
    'kill $(ps aux | grep "ticket2cash.*jar" | grep -v grep | awk ' + "'{print " + '$2' + "}') 2>/dev/null",
    'sleep 3',
    'nohup java -jar target/ticket2cash-0.0.1-SNAPSHOT.jar --server.port=3000 > /var/log/ticket2cash.log 2>' + [char]38 + '1 ' + [char]38,
    'sleep 5',
    'tail -20 /var/log/ticket2cash.log'
)
foreach ($cmd in $bashCommands) {
    Write-Host "  $cmd" -ForegroundColor White
}

Write-Host ""
Write-Host "Puis dans le navigateur: Ctrl+F5 sur http://localhost:3000" -ForegroundColor Cyan
Write-Host ""
