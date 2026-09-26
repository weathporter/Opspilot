#requires -Version 7.0
<#
  Regression check: an override that keeps four single replicas but removes
  ingress isolation must be rejected before the deployment job reaches Helm.
#>
$ErrorActionPreference = 'Stop'
$validator = Join-Path $PSScriptRoot 'validate-two-node-profile.ps1'
$fixture = Join-Path $PSScriptRoot 'fixtures/two-node-no-network-policy.yaml'
$rejected = $false
try {
    & $validator -ValuesFile $fixture | Out-Null
} catch {
    if ($_.Exception.Message -match 'NetworkPolicy') {
        $rejected = $true
    } else {
        throw
    }
}
if (-not $rejected) { throw 'Unsafe two-node profile was not rejected' }
Write-Output 'TWO_NODE_GATE_NEGATIVE_TEST PASS'
