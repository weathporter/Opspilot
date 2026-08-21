{{/* 返回稳定的应用名；当前 Nginx 配置要求后端 Service 固定为 opspilot-backend。 */}}
{{- define "opspilot.name" -}}
opspilot
{{- end }}

{{/* 所有资源都带统一推荐标签，便于 kubectl、监控和 NetworkPolicy 选择。 */}}
{{- define "opspilot.labels" -}}
app.kubernetes.io/name: {{ include "opspilot.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Values.global.version | quote }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | quote }}
{{- end }}

{{/* 数据库 Secret 名称在本地创建模式和企业外部 Secret 模式之间切换。 */}}
{{- define "opspilot.databaseSecretName" -}}
{{- if .Values.secrets.create -}}
opspilot-database
{{- else -}}
{{ required "secrets.existingSecret is required when secrets.create=false" .Values.secrets.existingSecret }}
{{- end -}}
{{- end }}
