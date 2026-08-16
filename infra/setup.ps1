<#
.SYNOPSIS
    Sobe toda a stack do MVP (docker compose), sempre rebuildando as imagens a partir do
    codigo atual, e so retorna sucesso depois de validar a saude de todos os servicos
    (postgres, backend, n8n, frontend).

.PARAMETER TimeoutSeconds
    Tempo maximo de espera (em segundos) para todos os servicos ficarem saudaveis. Default: 300.
    O tempo de build das imagens NAO conta neste timeout.

.PARAMETER Reset
    Destroi os volumes antes de subir (postgres_data, n8n_data, pdf_storage). Use para comecar
    com um banco vazio: as migracoes Flyway rodam de novo do zero. ATENCAO: apaga todos os dados
    locais, inclusive workflows/credenciais salvos no n8n.

.PARAMETER Seed
    Aplica infra/seed-dev.sql depois que o backend estiver saudavel (ou seja, depois que o Flyway
    criou as tabelas). Cria o administrador de teste, sem o qual nao existe forma de logar na
    interface administrativa.

.PARAMETER SkipBuild
    Nao reconstroi as imagens; apenas sobe o que ja existe. Util para reiniciar rapido.

.EXAMPLE
    .\infra\setup.ps1
    .\infra\setup.ps1 -Reset -Seed
    .\infra\setup.ps1 -SkipBuild -TimeoutSeconds 120
#>

param(
    [int]$TimeoutSeconds = 300,
    [switch]$Reset,
    [switch]$Seed,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$InfraDir = Join-Path $RepoRoot "infrastructure"
$EnvFile = Join-Path $InfraDir ".env"
$EnvExample = Join-Path $InfraDir ".env.example"
$ComposeFile = Join-Path $InfraDir "docker-compose.yml"
$SeedFile = Join-Path $PSScriptRoot "seed-dev.sql"

function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Fail($msg) { Write-Host "  [FALHOU] $msg" -ForegroundColor Red }
function Write-Warn($msg) { Write-Host "  [AVISO] $msg" -ForegroundColor Yellow }

# ---------------------------------------------------------------------------
# Helper: executar `docker compose` sem que a saida em stderr aborte o script.
#
# Comandos do docker escrevem avisos e progresso em stderr. Com
# $ErrorActionPreference = "Stop", qualquer stderr de um executavel nativo capturado/redirecionado
# pelo PowerShell 5.1 e transformado em erro TERMINANTE (NativeCommandError) - mesmo quando o
# processo retorna exit code 0. Era exatamente isso que fazia a versao anterior deste script
# esperar para sempre: o health check do postgres redirecionava a saida com `*> $null`, o aviso
# "the attribute `version` is obsolete" virava excecao, o catch do laco engolia a excecao e a
# condicao nunca era considerada satisfeita.
#
# A funcao abaixo isola cada chamada nativa: baixa o ErrorActionPreference so ali dentro, captura
# stdout+stderr como texto e devolve exit code + saida para o chamador decidir.
# ---------------------------------------------------------------------------
function Invoke-Compose {
    param(
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [switch]$PassThru   # mostra a saida no console (build, up, logs)
    )

    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $full = @("compose", "-f", $ComposeFile, "--project-directory", $InfraDir) + $Arguments
        if ($PassThru) {
            & docker @full
            $out = ""
        } else {
            $out = (& docker @full 2>&1 | Out-String)
        }
        return [pscustomobject]@{ ExitCode = $LASTEXITCODE; Output = $out }
    } finally {
        $ErrorActionPreference = $previous
    }
}

# ---------------------------------------------------------------------------
# 0. Pre-requisitos
# ---------------------------------------------------------------------------
Write-Step "Verificando pre-requisitos"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Fail "docker nao encontrado no PATH. Instale/abra o Docker Desktop."
    exit 1
}

$ping = Invoke-Compose -Arguments @("version")
if ($ping.ExitCode -ne 0) {
    Write-Fail "O daemon do Docker nao respondeu. Abra o Docker Desktop e espere ficar 'Running'."
    Write-Host $ping.Output
    exit 1
}
Write-Ok "docker e docker compose disponiveis"

# ---------------------------------------------------------------------------
# 1. Validar .env
# ---------------------------------------------------------------------------
Write-Step "Verificando infrastructure/.env"

if (-not (Test-Path $EnvFile)) {
    Write-Fail ".env nao encontrado em $EnvFile"
    Write-Warn "Copie o exemplo e preencha os valores reais antes de continuar:"
    Write-Host "    Copy-Item `"$EnvExample`" `"$EnvFile`""
    exit 1
}

# Carrega as variaveis do .env para dentro do processo do script (usadas nos health checks e no
# seed, ex.: POSTGRES_USER/POSTGRES_DB no psql).
$EnvVars = @{}
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $key, $value = $line.Split("=", 2)
        $EnvVars[$key.Trim()] = $value.Trim()
    }
}

$placeholderKeys = @(
    "POSTGRES_PASSWORD", "JWT_SECRET", "CPF_ENCRYPTION_KEY", "N8N_SERVICE_TOKEN",
    "TELEGRAM_BOT_TOKEN", "STT_API_KEY", "LLM_API_KEY",
    "N8N_BASIC_AUTH_USER", "N8N_BASIC_AUTH_PASSWORD"
)
$pendentes = @()
foreach ($key in $placeholderKeys) {
    $val = $EnvVars[$key]
    if (-not $val -or $val.StartsWith("change-me")) {
        $pendentes += $key
    }
}
if ($pendentes.Count -gt 0) {
    Write-Fail "As seguintes variaveis ainda estao com valor padrao/vazio em .env:"
    $pendentes | ForEach-Object { Write-Host "    - $_" -ForegroundColor Yellow }
    Write-Warn "Preencha $EnvFile com valores reais antes de subir a stack."
    exit 1
}
Write-Ok ".env presente e sem placeholders pendentes"

# JWT_SECRET curto derruba o backend no boot (a lib exige >= 256 bits para HS256).
if ($EnvVars["JWT_SECRET"].Length -lt 32) {
    Write-Fail "JWT_SECRET tem apenas $($EnvVars['JWT_SECRET'].Length) caracteres; use no minimo 32."
    exit 1
}

# Variaveis do portal web do professor (spec 002). NAO sao bloqueantes: todas possuem default no
# backend (application.yml) e no docker-compose.yml. Apenas informamos qual valor sera usado, para
# que uma origem de CORS errada em producao nao passe despercebida.
$portalDefaults = [ordered]@{
    "CORS_ALLOWED_ORIGIN"       = "http://localhost:8080"
    "WEB_LOGIN_MAX_ATTEMPTS"    = "5"
    "WEB_LOGIN_LOCKOUT_MINUTES" = "15"
}
$usandoDefault = @()
foreach ($key in $portalDefaults.Keys) {
    if (-not $EnvVars[$key]) {
        $usandoDefault += "$key (default: $($portalDefaults[$key]))"
    }
}
if ($usandoDefault.Count -gt 0) {
    Write-Warn "Portal do professor usando valores padrao para:"
    $usandoDefault | ForEach-Object { Write-Host "    - $_" -ForegroundColor Yellow }
    Write-Host "    Defina-as em .env se o frontend nao for servido em http://localhost:8080."
} else {
    Write-Ok "Variaveis do portal do professor definidas explicitamente em .env"
}

# ---------------------------------------------------------------------------
# 2. Reset opcional dos volumes
# ---------------------------------------------------------------------------
if ($Reset) {
    Write-Step "Removendo containers e volumes (-Reset)"
    $down = Invoke-Compose -Arguments @("down", "-v", "--remove-orphans") -PassThru
    if ($down.ExitCode -ne 0) {
        Write-Fail "docker compose down -v falhou (exit $($down.ExitCode))"
        exit 1
    }
    Write-Ok "Volumes removidos; o banco subira vazio e o Flyway rodara todas as migracoes"
}

# ---------------------------------------------------------------------------
# 3. Subir docker compose e esperar a saude dos servicos
#
# `--wait` faz o proprio compose aguardar os healthchecks declarados no docker-compose.yml
# (postgres, backend, n8n) e o HEALTHCHECK da imagem do frontend. E mais confiavel do que um laco
# de polling no script: o compose distingue "ainda subindo" de "container morreu".
# ---------------------------------------------------------------------------
$upArgs = @("up", "-d", "--remove-orphans", "--wait", "--wait-timeout", "$TimeoutSeconds")
if (-not $SkipBuild) {
    Write-Step "Buildando imagens a partir do codigo atual (pode levar alguns minutos)"
    $build = Invoke-Compose -Arguments @("build") -PassThru
    if ($build.ExitCode -ne 0) {
        Write-Fail "docker compose build falhou (exit $($build.ExitCode))"
        exit 1
    }
    Write-Ok "Imagens reconstruidas"
}

Write-Step "Subindo containers e aguardando ficarem saudaveis (timeout ${TimeoutSeconds}s)"
$up = Invoke-Compose -Arguments $upArgs -PassThru
if ($up.ExitCode -ne 0) {
    Write-Fail "docker compose up --wait terminou com codigo $($up.ExitCode)"
    Write-Host "`nEstado atual dos containers:" -ForegroundColor Yellow
    (Invoke-Compose -Arguments @("ps")).Output | Write-Host
    Write-Host "Veja os logs do servico que nao ficou saudavel, por exemplo:" -ForegroundColor Yellow
    Write-Host "    docker compose -f `"$ComposeFile`" logs backend --tail 100"
    exit 1
}
Write-Ok "Todos os containers reportaram healthy"

# ---------------------------------------------------------------------------
# 4. Smoke tests via HTTP (valida o que esta publicado no host, nao so dentro da rede do compose)
# ---------------------------------------------------------------------------
function Test-Http {
    param([string]$Name, [string]$Uri, [string]$ExpectPattern)

    try {
        $resp = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 10
        if ($resp.StatusCode -ne 200) {
            Write-Fail "$Name respondeu HTTP $($resp.StatusCode) em $Uri"
            return $false
        }
        # O /actuator/health responde com Content-Type application/vnd.spring-boot.actuator.v3+json.
        # O PowerShell 5.1 so trata como texto os tipos que reconhece; para os demais, .Content vem
        # como byte[] e qualquer -match falharia sempre. Normalizamos para string antes de comparar.
        $corpo = $resp.Content
        if ($corpo -is [byte[]]) {
            $corpo = [System.Text.Encoding]::UTF8.GetString($corpo)
        }
        if ($ExpectPattern -and ($corpo -notmatch $ExpectPattern)) {
            Write-Fail "$Name respondeu 200 mas o corpo nao casou com /$ExpectPattern/"
            return $false
        }
        Write-Ok "$Name ($Uri)"
        return $true
    } catch {
        Write-Fail "$Name inacessivel em ${Uri}: $($_.Exception.Message)"
        return $false
    }
}

Write-Step "Smoke tests HTTP"

$results = [ordered]@{}
$results["backend/actuator"] = Test-Http -Name "backend" -Uri "http://127.0.0.1:8081/actuator/health" -ExpectPattern '"status"\s*:\s*"UP"'
$results["n8n/healthz"]      = Test-Http -Name "n8n"     -Uri "http://127.0.0.1:5678/healthz"
$results["frontend"]         = Test-Http -Name "frontend" -Uri "http://localhost:8080"

# ---------------------------------------------------------------------------
# 5. Conferir as migracoes Flyway (o schema NAO vem de scripts em infrastructure/postgres:
#    quem cria as tabelas e o Flyway, no boot do backend).
# ---------------------------------------------------------------------------
Write-Step "Verificando o schema do banco (migracoes Flyway)"

$pgUser = if ($EnvVars["POSTGRES_USER"]) { $EnvVars["POSTGRES_USER"] } else { "escola_musica" }
$pgDb   = if ($EnvVars["POSTGRES_DB"])   { $EnvVars["POSTGRES_DB"] }   else { "escola_musica" }

$versao = Invoke-Compose -Arguments @(
    "exec", "-T", "postgres", "psql", "-U", $pgUser, "-d", $pgDb, "-tAc",
    "SELECT COALESCE(max(version::int)::text, 'nenhuma') FROM flyway_schema_history WHERE success"
)
if ($versao.ExitCode -ne 0) {
    Write-Fail "Nao foi possivel consultar flyway_schema_history"
    Write-Host $versao.Output
    $results["flyway"] = $false
} else {
    $v = ($versao.Output -split "`n" | Where-Object { $_.Trim() } | Select-Object -Last 1).Trim()
    Write-Ok "Banco '$pgDb' na versao de schema Flyway: V$v"
    $results["flyway"] = $true
}

# ---------------------------------------------------------------------------
# 6. Seed de dados de desenvolvimento (opcional)
# ---------------------------------------------------------------------------
if ($Seed) {
    Write-Step "Aplicando seed de desenvolvimento (infra/seed-dev.sql)"
    if (-not (Test-Path $SeedFile)) {
        Write-Fail "seed-dev.sql nao encontrado em $SeedFile"
        $results["seed"] = $false
    } else {
        $previous = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            $seedOut = (Get-Content $SeedFile -Raw |
                & docker compose -f $ComposeFile --project-directory $InfraDir `
                    exec -T postgres psql -U $pgUser -d $pgDb -v ON_ERROR_STOP=1 2>&1 | Out-String)
            $seedExit = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previous
        }

        if ($seedExit -ne 0) {
            Write-Fail "psql retornou $seedExit ao aplicar o seed"
            Write-Host $seedOut
            $results["seed"] = $false
        } else {
            Write-Ok "Seed aplicado"
            Write-Host $seedOut.Trim()
            $results["seed"] = $true
        }
    }
}

# ---------------------------------------------------------------------------
# 7. Resultado final
# ---------------------------------------------------------------------------
Write-Step "Resumo"

$falhou = $false
foreach ($key in $results.Keys) {
    if ($results[$key]) { Write-Ok $key } else { Write-Fail $key; $falhou = $true }
}

if ($falhou) {
    Write-Host "`nAlgo nao subiu corretamente. Verifique os logs com:" -ForegroundColor Red
    Write-Host "    docker compose -f `"$ComposeFile`" logs -f"
    exit 1
}

Write-Host "`nAplicacao de pe! Todos os servicos estao saudaveis." -ForegroundColor Green
Write-Host "  Frontend (admin):    http://localhost:8080"
Write-Host "  Portal do professor: http://localhost:8080/portal"
Write-Host "  Swagger (API):       http://127.0.0.1:8081/swagger-ui.html"
Write-Host "  Backend health:      http://127.0.0.1:8081/actuator/health"
Write-Host "  n8n:                 http://127.0.0.1:5678"
if (-not $Seed) {
    Write-Host "`nDica: rode com -Seed para criar o administrador de teste (sem ele nao ha login)." -ForegroundColor Yellow
}
exit 0
