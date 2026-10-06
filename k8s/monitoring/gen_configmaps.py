#!/usr/bin/env python3
"""Builds k8s/monitoring/configmaps.yaml from the compose monitoring files in infra/.

infra/ is the single source for Prometheus and Grafana config. Kustomize cannot read files
outside k8s/, so this script embeds them as ConfigMaps. Re-run after editing infra/:

    python3 k8s/monitoring/gen_configmaps.py
"""
import glob
import os

HERE = os.path.dirname(os.path.abspath(__file__))
INFRA = os.path.normpath(os.path.join(HERE, "..", "..", "infra"))
OUT = os.path.join(HERE, "configmaps.yaml")
NS = "razorpay"


def block(text):
    """Indent file content for a YAML literal block scalar (8 spaces)."""
    lines = text.rstrip("\n").split("\n")
    return "\n".join(("        " + l) if l.strip() else "" for l in lines)


def configmap(name, files):
    body = [
        "apiVersion: v1",
        "kind: ConfigMap",
        "metadata:",
        f"  name: {name}",
        f"  namespace: {NS}",
        "data:",
    ]
    for key, path in files:
        with open(path, encoding="utf-8") as f:
            body.append(f"  {key}: |")
            body.append(block(f.read()))
    return "\n".join(body) + "\n"


docs = []
docs.append(configmap("prometheus-config", [
    ("prometheus.yml", os.path.join(INFRA, "prometheus", "prometheus.yml")),
]))
docs.append(configmap("grafana-datasources", [
    ("prometheus.yml", os.path.join(INFRA, "grafana", "provisioning", "datasources", "prometheus.yml")),
]))
docs.append(configmap("grafana-dashboards-provider", [
    ("razorpay.yml", os.path.join(INFRA, "grafana", "provisioning", "dashboards", "razorpay.yml")),
]))
dashboards = sorted(glob.glob(os.path.join(INFRA, "grafana", "dashboards", "*.json")))
docs.append(configmap("grafana-dashboards", [
    (os.path.basename(p), p) for p in dashboards
]))

with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n---\n".join(docs))
print("wrote", OUT, "with", len(dashboards), "dashboards")
