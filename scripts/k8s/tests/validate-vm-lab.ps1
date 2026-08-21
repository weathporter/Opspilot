[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$LabRoot = Join-Path $ProjectRoot "deploy\k8s\vm-lab"

function Assert-Text {
    param(
        [string]$Text,
        [string]$Pattern,
        [string]$Message
    )

    if ($Text -notmatch $Pattern) {
        throw "ASSERTION FAILED: $Message"
    }
}

function Assert-NoText {
    param(
        [string]$Text,
        [string]$Pattern,
        [string]$Message
    )

    if ($Text -match $Pattern) {
        throw "ASSERTION FAILED: $Message"
    }
}

function Get-ResourceDocument {
    param(
        [string[]]$Documents,
        [string]$Kind,
        [string]$Name
    )

    $KindPattern = "(?m)^kind: {0}\s*$" -f [regex]::Escape($Kind)
    $NamePattern = "(?m)^  name: {0}\s*$" -f [regex]::Escape($Name)
    foreach ($Document in $Documents) {
        if ($Document -match $KindPattern -and $Document -match $NamePattern) {
            return $Document
        }
    }

    throw "ASSERTION FAILED: rendered resource $Kind/$Name was not found"
}

$RequiredFiles = @(
    "kustomization.yaml",
    "namespace.yaml",
    "configmap.yaml",
    "storage.yaml",
    "mysql.yaml",
    "api.yaml",
    "web.yaml",
    "pdb.yaml",
    "networkpolicy.yaml",
    "README.md",
    "..\..\..\scripts\k8s\preflight-vm-lab.sh",
    "..\..\..\scripts\k8s\deploy-vm-lab.sh"
)

foreach ($RelativePath in $RequiredFiles) {
    $Target = Join-Path $LabRoot $RelativePath
    if (-not (Test-Path -LiteralPath $Target)) {
        throw "ASSERTION FAILED: missing deployment file $Target"
    }
}

$Rendered = (& kubectl kustomize $LabRoot 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0) {
    throw "ASSERTION FAILED: kubectl kustomize failed:`n$Rendered"
}

$Documents = $Rendered -split '(?m)^---\s*$'
$NamespaceDocument = Get-ResourceDocument $Documents "Namespace" "opspilot"
$PersistentVolumeDocument = Get-ResourceDocument $Documents "PersistentVolume" "opspilot-mysql-pv"
$PersistentVolumeClaimDocument = Get-ResourceDocument $Documents "PersistentVolumeClaim" "opspilot-mysql-data"
$ApiDeploymentDocument = Get-ResourceDocument $Documents "Deployment" "opspilot-api"
$WebDeploymentDocument = Get-ResourceDocument $Documents "Deployment" "opspilot-web"

Assert-Text $NamespaceDocument '(?m)^apiVersion: v1\s*$' "namespace must use core/v1"
Assert-Text $PersistentVolumeDocument '(?ms)^  local:\s+path: /data/opspilot/mysql\s*$' "local PV must use node1 data path"
Assert-Text $PersistentVolumeDocument '(?m)^\s+values:\s*\r?\n\s+- k8s-node1\s*$' "local PV must be pinned to k8s-node1"
Assert-Text $PersistentVolumeClaimDocument '(?m)^  storageClassName: opspilot-local\s*$' "PVC must request the manual lab storage class"
Assert-Text $ApiDeploymentDocument '(?m)^  replicas: 2\s*$' "API must start with two replicas"
Assert-Text $WebDeploymentDocument '(?m)^  replicas: 2\s*$' "Web must start with two replicas"
Assert-Text $Rendered 'image: opspilot-api:0\.1\.1\s' "API image must use immutable version 0.1.1"
Assert-Text $Rendered 'image: opspilot-web:0\.1\.1\s' "Web image must use immutable version 0.1.1"
Assert-Text $Rendered 'image: mysql:8\.4\s' "MySQL image must use version 8.4"
Assert-Text $Rendered 'nodePort: 30080\s' "Web must be reachable through NodePort 30080"
Assert-Text $Rendered 'opspilot\.io/workload: "true"' "business pods must select explicitly labelled workers"
Assert-Text $Rendered 'topologyKey: kubernetes\.io/hostname' "stateless replicas must prefer cross-node spread"
Assert-Text $Rendered '(?ms)^apiVersion: policy/v1\s+kind: PodDisruptionBudget\b' "PDB must use policy/v1"
Assert-Text $Rendered 'imagePullPolicy: IfNotPresent' "preloaded lab images must not be pulled unconditionally"

Assert-NoText $Rendered '(?ms)^kind: Secret\s*$' "tracked manifests must not contain a Secret object"
Assert-NoText $Rendered 'opspilot_local_change_me|opspilot_root_local_change_me|password:\s+[^$]' "tracked manifests must not contain a database password"
Assert-NoText $Rendered 'image:\s+[^\r\n]+:latest\s*$' "latest image tags are not allowed"
Assert-NoText $Rendered 'apiVersion: autoscaling/v2\s' "Kubernetes v1.21 lab base must not include autoscaling/v2"

Write-Host "PASS: VM lab Kubernetes package rendered and passed structural checks."
