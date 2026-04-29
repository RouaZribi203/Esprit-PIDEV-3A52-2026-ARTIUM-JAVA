param(
    [string]$SmtpHost,
    [string]$SmtpPort,
    [string]$SmtpUsername,
    [string]$SmtpPassword,
    [string]$SmtpFrom,
    [string]$SmtpStartTls,
    [string]$SmtpSsl
)

function Resolve-Input {
    param(
        [string]$CurrentValue,
        [string]$Prompt,
        [string]$DefaultValue = "",
        [bool]$Required = $false
    )

    $value = $CurrentValue
    if ([string]::IsNullOrWhiteSpace($value)) {
        $raw = Read-Host $Prompt
        if ([string]::IsNullOrWhiteSpace($raw) -and -not [string]::IsNullOrWhiteSpace($DefaultValue)) {
            $value = $DefaultValue
        } else {
            $value = $raw
        }
    }

    $value = if ($null -eq $value) { "" } else { $value.Trim() }
    if ($Required -and [string]::IsNullOrWhiteSpace($value)) {
        throw "Valeur obligatoire manquante: $Prompt"
    }
    return $value
}

function Normalize-Bool {
    param([string]$Value, [string]$DefaultValue)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $DefaultValue
    }
    $normalized = $Value.Trim().ToLowerInvariant()
    switch ($normalized) {
        "1" { return "true" }
        "0" { return "false" }
        "yes" { return "true" }
        "no" { return "false" }
        "y" { return "true" }
        "n" { return "false" }
        "true" { return "true" }
        "false" { return "false" }
        default { throw "Valeur booléenne invalide: $Value (utilisez true/false)." }
    }
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$configFile = Join-Path $root 'smtp.properties'

Write-Host 'Configuration SMTP minimale' -ForegroundColor Cyan
Write-Host 'Les valeurs seront écrites dans smtp.properties a la racine du projet.'

try {
    $smtpHost = Resolve-Input -CurrentValue $SmtpHost -Prompt 'SMTP host (ex: smtp.gmail.com)' -Required $true
    $smtpPort = Resolve-Input -CurrentValue $SmtpPort -Prompt 'SMTP port (ex: 587)' -DefaultValue '587' -Required $true
    if ($smtpPort -notmatch '^[0-9]+$') {
        throw 'SMTP port invalide: utilisez uniquement des chiffres (ex: 587).'
    }

    $smtpUsername = Resolve-Input -CurrentValue $SmtpUsername -Prompt 'SMTP username' -Required $true
    $smtpPassword = Resolve-Input -CurrentValue $SmtpPassword -Prompt 'SMTP password' -Required $true
    $smtpFrom = Resolve-Input -CurrentValue $SmtpFrom -Prompt 'SMTP from (laisser vide pour utiliser username)'
    if ([string]::IsNullOrWhiteSpace($smtpFrom)) { $smtpFrom = $smtpUsername }

    $smtpStartTls = Resolve-Input -CurrentValue $SmtpStartTls -Prompt 'Use STARTTLS? (true/false) [true]' -DefaultValue 'true'
    $smtpSsl = Resolve-Input -CurrentValue $SmtpSsl -Prompt 'Use SSL? (true/false) [false]' -DefaultValue 'false'
    $smtpStartTls = Normalize-Bool -Value $smtpStartTls -DefaultValue 'true'
    $smtpSsl = Normalize-Bool -Value $smtpSsl -DefaultValue 'false'

    @(
        "smtp.host=$smtpHost"
        "smtp.port=$smtpPort"
        "smtp.username=$smtpUsername"
        "smtp.password=$smtpPassword"
        "smtp.from=$smtpFrom"
        "smtp.starttls=$smtpStartTls"
        "smtp.ssl=$smtpSsl"
    ) | Set-Content -Path $configFile -Encoding ASCII

    if (-not (Test-Path -Path $configFile)) {
        throw "Echec creation fichier: $configFile"
    }

    Write-Host "Configuration enregistree dans $configFile" -ForegroundColor Green
    exit 0
}
catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

