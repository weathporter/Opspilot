"""Check the two-node monitoring contract before chart-specific Helm rendering.

This is intentionally a static, dependency-light gate: it catches missing or
miswired profiles in CI, while real chart rendering and live Targets still need
their own checks. PyYAML is required by this local test.
"""

from pathlib import Path
import json
import unittest
import yaml


ROOT = Path(__file__).resolve().parents[2]
MONITORING = ROOT / "deploy" / "k8s" / "monitoring"


class MonitoringProfileTest(unittest.TestCase):
    def test_prometheus_has_bounded_two_node_collectors_and_rules(self):
        values = yaml.safe_load((MONITORING / "prometheus-values.yaml").read_text(encoding="utf-8"))
        self.assertTrue(values["alertmanager"]["enabled"])
        self.assertTrue(values["prometheus-node-exporter"]["enabled"])
        self.assertTrue(values["kube-state-metrics"]["enabled"])
        self.assertFalse(values["prometheus-pushgateway"]["enabled"])
        self.assertEqual(values["server"]["service"]["type"], "ClusterIP")
        rules = values["serverFiles"]["alerting_rules.yml"]["groups"]
        names = {rule["alert"] for group in rules for rule in group["rules"]}
        self.assertIn("NorthLedgerDeploymentUnavailable", names)
        self.assertIn("NorthLedgerTargetDown", names)
        self.assertIn("NorthLedgerNodeMemoryLow", names)

    def test_grafana_uses_secret_and_cluster_only_prometheus(self):
        values = yaml.safe_load((MONITORING / "grafana-values.yaml").read_text(encoding="utf-8"))
        self.assertEqual(values["admin"]["existingSecret"], "northledger-grafana-admin")
        self.assertEqual(values["service"]["type"], "ClusterIP")
        source = values["datasources"]["datasources.yaml"]["datasources"][0]
        self.assertEqual(source["type"], "prometheus")
        self.assertIn(".monitoring.svc.cluster.local", source["url"])
        self.assertEqual(values["dashboardProviders"]["dashboardproviders.yaml"]["providers"][0]["name"], "default")

    def test_dashboard_covers_health_and_capacity_without_color_only_status(self):
        dashboard = json.loads((MONITORING / "northledger-dashboard.json").read_text(encoding="utf-8"))
        self.assertEqual(dashboard["uid"], "northledger-k8s")
        panels = dashboard["panels"]
        self.assertGreaterEqual(len(panels), 4)
        expressions = " ".join(target["expr"] for panel in panels for target in panel.get("targets", []))
        for metric in ("kube_deployment_status_replicas_available", "node_memory_MemAvailable_bytes", "up{"):
            self.assertIn(metric, expressions)
        self.assertTrue(all(panel.get("title") for panel in panels))


if __name__ == "__main__":
    unittest.main()
