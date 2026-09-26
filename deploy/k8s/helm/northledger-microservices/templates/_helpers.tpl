{{- define "northledger.labels" -}}
app.kubernetes.io/name: northledger
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Values.global.version | quote }}
{{- end -}}
