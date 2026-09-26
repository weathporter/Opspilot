"""Validate rendered monitoring objects and extract Prometheus rules for promtool.

The Helm values unit test verifies our intent; this checks what the pinned
upstream charts actually render. Live Targets remain a separate VM acceptance
gate. Chart sources:
https://github.com/prometheus-community/helm-charts/tree/main/charts/prometheus
https://github.com/grafana-community/helm-charts/tree/main/charts/grafana
"""

import argparse
from pathlib import Path

import yaml


REQUIRED_ALERTS = {
    "NorthLedgerDeploymentUnavailable",
    "NorthLedgerTargetDown",
    "NorthLedgerNodeMemoryLow",
}


def validate_rendered_monitoring(prometheus_manifest: str, grafana_manifest: str) -> str:
    """Return rules text or explain which expected rendered resource is absent."""
    prom_objects = [obj for obj in yaml.safe_load_all(prometheus_manifest) if isinstance(obj, dict)]
    grafana_objects = [obj for obj in yaml.safe_load_all(grafana_manifest) if isinstance(obj, dict)]

    def named(objects, kind, fragment):
        return any(
            obj.get("kind") == kind and fragment in obj.get("metadata", {}).get("name", "")
            for obj in objects
        )

    if not named(prom_objects, "DaemonSet", "node-exporter"):
        raise ValueError("Rendered node-exporter DaemonSet is missing")
    if not named(prom_objects, "Deployment", "kube-state-metrics"):
        raise ValueError("Rendered kube-state-metrics Deployment is missing")
    if not named(prom_objects, "Service", "northledger-prometheus"):
        raise ValueError("Rendered Prometheus Service is missing")

    configs = [
        obj["data"]["alerting_rules.yml"]
        for obj in prom_objects
        if obj.get("kind") == "ConfigMap"
        and isinstance(obj.get("data"), dict)
        and "alerting_rules.yml" in obj["data"]
    ]
    if len(configs) != 1:
        raise ValueError("Expected one rendered Prometheus alerting_rules.yml ConfigMap")
    rules = yaml.safe_load(configs[0]) or {}
    names = {rule.get("alert") for group in rules.get("groups", []) for rule in group.get("rules", [])}
    if not REQUIRED_ALERTS.issubset(names):
        raise ValueError("Rendered Prometheus rules are missing expected NorthLedger alerts")

    # Grafana's chart stores the --set-file JSON in a ConfigMap. Checking the
    # rendered objects catches a missing --set-file flag even if source JSON is valid.
    provisioned = any(
        obj.get("kind") == "ConfigMap"
        and any("northledger-k8s" in str(value) for value in (obj.get("data") or {}).values())
        for obj in grafana_objects
    )
    if not provisioned:
        raise ValueError("Rendered Grafana dashboard is missing")
    return configs[0]


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("prometheus_manifest", type=Path)
    parser.add_argument("grafana_manifest", type=Path)
    parser.add_argument("rules_output", type=Path)
    args = parser.parse_args()
    alerts = validate_rendered_monitoring(
        args.prometheus_manifest.read_text(encoding="utf-8"),
        args.grafana_manifest.read_text(encoding="utf-8"),
    )
    args.rules_output.write_text(alerts, encoding="utf-8")
    print("MONITORING_RENDER_VALIDATION PASS")
