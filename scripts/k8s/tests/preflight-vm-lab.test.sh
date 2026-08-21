#!/usr/bin/env bash
set -Eeuo pipefail

TEST_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PREFLIGHT_SCRIPT="${TEST_DIR}/../preflight-vm-lab.sh"
FAKE_BIN="$(mktemp -d)"
trap 'rm -rf "${FAKE_BIN}"' EXIT

cat >"${FAKE_BIN}/kubectl" <<'FAKE_KUBECTL'
#!/usr/bin/bash
set -u

if [[ "$*" == "cluster-info" ]]; then
  exit 0
fi

if [[ "$*" == "version --short" ]]; then
  printf 'Client Version: v1.21.14\nServer Version: v1.21.14\n'
  exit 0
fi

if [[ "$1" == "get" && "$2" == "node" ]]; then
  if [[ "$*" == *'containerRuntimeVersion'* ]]; then
    printf 'docker://20.10.24'
  elif [[ "$*" == *'status.conditions'* ]]; then
    printf 'True'
  fi
  exit 0
fi

if [[ "$*" == "-n kube-system get daemonset calico-node" ]]; then
  exit 0
fi

if [[ "$*" == "get --raw /apis/policy/v1" ]]; then
  printf '{"resources":[{"name":"poddisruptionbudgets","kind":"PodDisruptionBudget"}]}'
  exit 0
fi

# 模拟旧版 kubectl：api-resources 不认识 --api-version 筛选参数。
if [[ "$1" == "api-resources" ]]; then
  printf 'error: unknown flag: --api-version\n' >&2
  exit 2
fi

if [[ "$*" == "get nodes -o wide" ]]; then
  printf 'NAME         STATUS   VERSION\nk8s-master   Ready    v1.21.14\nk8s-node1    Ready    v1.21.14\nk8s-node2    Ready    v1.21.14\n'
  exit 0
fi

printf 'unexpected kubectl invocation: %s\n' "$*" >&2
exit 3
FAKE_KUBECTL

chmod +x "${FAKE_BIN}/kubectl"

output="$(PATH="${FAKE_BIN}:${PATH}" "${BASH}" "${PREFLIGHT_SCRIPT}" 2>&1)" || {
  printf '%s\n' "${output}" >&2
  exit 1
}

grep -q 'PASS: Kubernetes=v1.21.14' <<<"${output}"
printf 'PASS: preflight detects policy/v1 PDB through API discovery on kubectl v1.21.\n'
