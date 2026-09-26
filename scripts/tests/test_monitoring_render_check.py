"""Behavior tests for checks against rendered third-party Helm resources."""

import unittest

from monitoring_render_check import validate_rendered_monitoring


PROM = """\
apiVersion: v1
kind: ConfigMap
metadata: {name: northledger-prometheus}
data:
  alerting_rules.yml: |
    groups:
      - name: northledger-two-node-lab
        rules:
          - {alert: NorthLedgerDeploymentUnavailable, expr: 'up == 0'}
          - {alert: NorthLedgerTargetDown, expr: 'up == 0'}
          - {alert: NorthLedgerNodeMemoryLow, expr: 'up == 0'}
---
apiVersion: apps/v1
kind: DaemonSet
metadata: {name: northledger-metrics-prometheus-node-exporter}
---
apiVersion: apps/v1
kind: Deployment
metadata: {name: northledger-metrics-kube-state-metrics}
---
apiVersion: v1
kind: Service
metadata: {name: northledger-prometheus}
"""

GRAFANA = """\
apiVersion: v1
kind: ConfigMap
metadata: {name: northledger-grafana-dashboards}
data:
  northledger.json: |
    {"uid": "northledger-k8s", "panels": []}
"""


class RenderedMonitoringCheckTest(unittest.TestCase):
    def test_accepts_rendered_collectors_rules_and_dashboard(self):
        alerts = validate_rendered_monitoring(PROM, GRAFANA)
        self.assertIn("NorthLedgerTargetDown", alerts)

    def test_rejects_missing_exporter(self):
        with self.assertRaisesRegex(ValueError, "node-exporter"):
            validate_rendered_monitoring(PROM.replace("kind: DaemonSet", "kind: Deployment"), GRAFANA)

    def test_rejects_unprovisioned_dashboard(self):
        with self.assertRaisesRegex(ValueError, "dashboard"):
            validate_rendered_monitoring(PROM, GRAFANA.replace("northledger-k8s", "other-dashboard"))


if __name__ == "__main__":
    unittest.main()
