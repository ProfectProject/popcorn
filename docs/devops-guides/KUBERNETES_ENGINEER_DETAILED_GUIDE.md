# Kubernetes Engineer 상세 실무 가이드

## 🎯 역할 개요

**Kubernetes Engineer**는 Popcorn MSA 프로젝트의 **EKS 클러스터 및 Kubernetes 워크로드를 구축하고 관리**하는 핵심 역할입니다. 클러스터 운영부터 애플리케이션 배포, 스케일링, 서비스 메시 준비까지 모든 컨테이너 오케스트레이션을 담당합니다.

---

## 📋 주요 책임 및 업무

### 핵심 책임
- EKS 클러스터 구축 및 운영
- Kubernetes 매니페스트 작성 및 관리
- 서비스 배포 및 스케일링 관리
- 네임스페이스 및 RBAC 설정
- Helm Chart 개발 및 관리

### 일일 업무
- 클러스터 상태 모니터링
- Pod 및 노드 상태 확인
- 리소스 사용률 분석
- 배포 및 롤백 관리
- 스케일링 정책 최적화

---

## ☸️ Week 1: EKS 클러스터 구축

### Day 1-2: EKS 클러스터 생성

#### 1.1 EKS 모듈 설계

**EKS 모듈 구조**:
```
modules/eks/
├── main.tf          # 클러스터 및 노드 그룹
├── addons.tf        # EKS Add-ons
├── iam.tf           # IAM 역할 및 정책
├── variables.tf     # 입력 변수
├── outputs.tf       # 출력 값
└── README.md        # 모듈 문서
```

**EKS 클러스터 구현**:
```hcl
# modules/eks/main.tf
resource "aws_eks_cluster" "main" {
  name     = "${var.project_name}-${var.environment}-eks"
  role_arn = aws_iam_role.cluster.arn
  version  = var.kubernetes_version

  vpc_config {
    subnet_ids              = concat(var.public_subnet_ids, var.private_app_subnet_ids)
    endpoint_private_access = true
    endpoint_public_access  = var.cluster_endpoint_public_access
    public_access_cidrs     = var.cluster_endpoint_public_access_cidrs
    security_group_ids      = [aws_security_group.cluster.id]
  }

  # 로깅 활성화
  enabled_cluster_log_types = var.cluster_enabled_log_types

  # 암호화 설정
  encryption_config {
    provider {
      key_arn = aws_kms_key.eks.arn
    }
    resources = ["secrets"]
  }

  depends_on = [
    aws_iam_role_policy_attachment.cluster_AmazonEKSClusterPolicy,
    aws_cloudwatch_log_group.cluster,
  ]

  tags = {
    Name = "${var.project_name}-${var.environment}-eks"
  }
}

# CloudWatch 로그 그룹
resource "aws_cloudwatch_log_group" "cluster" {
  name              = "/aws/eks/${var.project_name}-${var.environment}-eks/cluster"
  retention_in_days = var.cloudwatch_log_retention_days

  tags = {
    Name = "${var.project_name}-${var.environment}-eks-logs"
  }
}

# KMS 키 (EKS 암호화용)
resource "aws_kms_key" "eks" {
  description             = "EKS Secret Encryption Key"
  deletion_window_in_days = 7

  tags = {
    Name = "${var.project_name}-${var.environment}-eks-key"
  }
}
```

#### 1.2 노드 그룹 구성

**관리형 노드 그룹**:
```hcl
# modules/eks/main.tf (continued)
resource "aws_eks_node_group" "main" {
  cluster_name    = aws_eks_cluster.main.name
  node_group_name = "${var.project_name}-${var.environment}-nodes"
  node_role_arn   = aws_iam_role.node_group.arn
  subnet_ids      = var.private_app_subnet_ids

  # 인스턴스 설정
  instance_types = var.node_instance_types
  capacity_type  = var.node_capacity_type
  disk_size      = var.node_disk_size
  ami_type       = var.node_ami_type

  # 스케일링 설정
  scaling_config {
    desired_size = var.node_desired_size
    max_size     = var.node_max_size
    min_size     = var.node_min_size
  }

  # 업데이트 설정
  update_config {
    max_unavailable_percentage = var.node_max_unavailable_percentage
  }

  # 원격 접근 설정
  dynamic "remote_access" {
    for_each = var.node_key_name != "" ? [1] : []
    content {
      ec2_ssh_key               = var.node_key_name
      source_security_group_ids = [aws_security_group.node_group_remote_access.id]
    }
  }

  # 라벨
  labels = merge(
    var.node_labels,
    {
      Environment = var.environment
      NodeGroup   = "main"
    }
  )

  # 테인트
  dynamic "taint" {
    for_each = var.node_taints
    content {
      key    = taint.value.key
      value  = taint.value.value
      effect = taint.value.effect
    }
  }

  depends_on = [
    aws_iam_role_policy_attachment.node_group_AmazonEKSWorkerNodePolicy,
    aws_iam_role_policy_attachment.node_group_AmazonEKS_CNI_Policy,
    aws_iam_role_policy_attachment.node_group_AmazonEC2ContainerRegistryReadOnly,
  ]

  tags = {
    Name = "${var.project_name}-${var.environment}-node-group"
  }

  lifecycle {
    ignore_changes = [scaling_config[0].desired_size]
  }
}
```

### Day 3-4: EKS Add-ons 설치

#### 3.1 필수 Add-ons 구성

**Add-ons 설치**:
```hcl
# modules/eks/addons.tf
# VPC CNI
resource "aws_eks_addon" "vpc_cni" {
  cluster_name             = aws_eks_cluster.main.name
  addon_name               = "vpc-cni"
  addon_version            = var.vpc_cni_version
  resolve_conflicts        = "OVERWRITE"
  service_account_role_arn = aws_iam_role.vpc_cni.arn

  tags = {
    Name = "${var.project_name}-${var.environment}-vpc-cni"
  }
}

# CoreDNS
resource "aws_eks_addon" "coredns" {
  cluster_name      = aws_eks_cluster.main.name
  addon_name        = "coredns"
  addon_version     = var.coredns_version
  resolve_conflicts = "OVERWRITE"

  depends_on = [aws_eks_node_group.main]

  tags = {
    Name = "${var.project_name}-${var.environment}-coredns"
  }
}

# kube-proxy
resource "aws_eks_addon" "kube_proxy" {
  cluster_name      = aws_eks_cluster.main.name
  addon_name        = "kube-proxy"
  addon_version     = var.kube_proxy_version
  resolve_conflicts = "OVERWRITE"

  tags = {
    Name = "${var.project_name}-${var.environment}-kube-proxy"
  }
}

# EBS CSI Driver
resource "aws_eks_addon" "ebs_csi" {
  cluster_name             = aws_eks_cluster.main.name
  addon_name               = "aws-ebs-csi-driver"
  addon_version            = var.ebs_csi_version
  resolve_conflicts        = "OVERWRITE"
  service_account_role_arn = aws_iam_role.ebs_csi_driver.arn

  tags = {
    Name = "${var.project_name}-${var.environment}-ebs-csi"
  }
}
```

#### 3.2 클러스터 검증

**클러스터 상태 확인 스크립트**:
```bash
#!/bin/bash
# scripts/validate-eks-cluster.sh

CLUSTER_NAME="${PROJECT_NAME}-${ENVIRONMENT}-eks"
AWS_REGION="ap-northeast-2"

echo "🔍 EKS 클러스터 검증 시작"

# 1. 클러스터 상태 확인
echo "1. 클러스터 상태 확인"
aws eks describe-cluster --name $CLUSTER_NAME --region $AWS_REGION --query 'cluster.status' --output text

# 2. kubeconfig 업데이트
echo "2. kubeconfig 업데이트"
aws eks update-kubeconfig --region $AWS_REGION --name $CLUSTER_NAME

# 3. 노드 상태 확인
echo "3. 노드 상태 확인"
kubectl get nodes -o wide

# 4. 시스템 Pod 상태 확인
echo "4. 시스템 Pod 상태 확인"
kubectl get pods -n kube-system

# 5. Add-ons 상태 확인
echo "5. Add-ons 상태 확인"
aws eks describe-addon --cluster-name $CLUSTER_NAME --addon-name vpc-cni --region $AWS_REGION --query 'addon.status'
aws eks describe-addon --cluster-name $CLUSTER_NAME --addon-name coredns --region $AWS_REGION --query 'addon.status'
aws eks describe-addon --cluster-name $CLUSTER_NAME --addon-name kube-proxy --region $AWS_REGION --query 'addon.status'
aws eks describe-addon --cluster-name $CLUSTER_NAME --addon-name aws-ebs-csi-driver --region $AWS_REGION --query 'addon.status'

echo "✅ EKS 클러스터 검증 완료"
```

### Day 5-7: AWS Load Balancer Controller 설치

#### 5.1 IRSA 설정

**서비스 계정 및 IAM 역할 생성**:
```bash
#!/bin/bash
# scripts/setup-aws-load-balancer-controller.sh

CLUSTER_NAME="${PROJECT_NAME}-${ENVIRONMENT}-eks"
AWS_REGION="ap-northeast-2"
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

echo "🚀 AWS Load Balancer Controller 설치"

# 1. IAM 정책 다운로드
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.6.0/docs/install/iam_policy.json

# 2. IAM 정책 생성
aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy-${ENVIRONMENT} \
    --policy-document file://iam_policy.json

# 3. OIDC 공급자 생성
eksctl utils associate-iam-oidc-provider --region=$AWS_REGION --cluster=$CLUSTER_NAME --approve

# 4. 서비스 계정 생성
eksctl create iamserviceaccount \
  --cluster=$CLUSTER_NAME \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --role-name AmazonEKSLoadBalancerControllerRole-${ENVIRONMENT} \
  --attach-policy-arn=arn:aws:iam::$AWS_ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy-${ENVIRONMENT} \
  --approve

# 5. Helm 레포지토리 추가
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# 6. AWS Load Balancer Controller 설치
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=$CLUSTER_NAME \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller \
  --set region=$AWS_REGION

# 7. 설치 확인
kubectl get deployment -n kube-system aws-load-balancer-controller
kubectl get pods -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller

echo "✅ AWS Load Balancer Controller 설치 완료"
```

---

## 📦 Week 2: Kubernetes 기본 구성

### Day 8-10: 네임스페이스 및 RBAC 설정

#### 8.1 네임스페이스 구성

**네임스페이스 매니페스트**:
```yaml
# k8s/namespaces/namespaces.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: popcorn
  labels:
    name: popcorn
    environment: ${ENVIRONMENT}
    istio-injection: enabled  # 향후 Istio 도입 준비
---
apiVersion: v1
kind: Namespace
metadata:
  name: monitoring
  labels:
    name: monitoring
    environment: ${ENVIRONMENT}
---
apiVersion: v1
kind: Namespace
metadata:
  name: ingress-nginx
  labels:
    name: ingress-nginx
    environment: ${ENVIRONMENT}
---
apiVersion: v1
kind: Namespace
metadata:
  name: cert-manager
  labels:
    name: cert-manager
    environment: ${ENVIRONMENT}
```

#### 8.2 RBAC 설정

**서비스 계정 및 역할**:
```yaml
# k8s/rbac/popcorn-rbac.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: popcorn-service-account
  namespace: popcorn
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::${AWS_ACCOUNT_ID}:role/${PROJECT_NAME}-${ENVIRONMENT}-service-role
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: popcorn
  name: popcorn-role
rules:
- apiGroups: [""]
  resources: ["pods", "services", "configmaps", "secrets", "persistentvolumeclaims"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets", "statefulsets"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
- apiGroups: ["networking.k8s.io"]
  resources: ["ingresses", "networkpolicies"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
- apiGroups: ["autoscaling"]
  resources: ["horizontalpodautoscalers"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: popcorn-role-binding
  namespace: popcorn
subjects:
- kind: ServiceAccount
  name: popcorn-service-account
  namespace: popcorn
roleRef:
  kind: Role
  name: popcorn-role
  apiGroup: rbac.authorization.k8s.io
```

### Day 11-12: 스토리지 클래스 및 네트워크 정책

#### 11.1 스토리지 클래스 구성

**스토리지 클래스 매니페스트**:
```yaml
# k8s/storage/storage-classes.yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3-encrypted
  annotations:
    storageclass.kubernetes.io/is-default-class: "true"
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  encrypted: "true"
  throughput: "125"
  iops: "3000"
  fsType: ext4
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
reclaimPolicy: Delete
---
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3-fast
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  encrypted: "true"
  throughput: "250"
  iops: "6000"
  fsType: ext4
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
reclaimPolicy: Delete
---
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: gp3-retain
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  encrypted: "true"
  throughput: "125"
  iops: "3000"
  fsType: ext4
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
reclaimPolicy: Retain  # 데이터 보존
```

#### 11.2 네트워크 정책

**네트워크 정책 매니페스트**:
```yaml
# k8s/network-policies/popcorn-network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: popcorn-network-policy
  namespace: popcorn
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
  ingress:
  # ALB에서 들어오는 트래픽 허용
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
  # 같은 네임스페이스 내 통신 허용
  - from:
    - podSelector: {}
  # 모니터링 네임스페이스에서 메트릭 수집 허용
  - from:
    - namespaceSelector:
        matchLabels:
          name: monitoring
    ports:
    - protocol: TCP
      port: 8080  # 애플리케이션 포트
    - protocol: TCP
      port: 8081  # 관리 포트 (actuator)
  egress:
  # DNS 해결 허용
  - to: []
    ports:
    - protocol: UDP
      port: 53
    - protocol: TCP
      port: 53
  # 외부 API 호출 허용 (HTTPS)
  - to: []
    ports:
    - protocol: TCP
      port: 443
  # 데이터베이스 접근 허용
  - to: []
    ports:
    - protocol: TCP
      port: 5432  # PostgreSQL
    - protocol: TCP
      port: 6379  # Redis
    - protocol: TCP
      port: 9092  # Kafka
  # 같은 네임스페이스 내 통신 허용
  - to:
    - podSelector: {}
```

### Day 13-14: HPA 및 VPA 설정

#### 13.1 Metrics Server 설치

**Metrics Server 설치**:
```bash
#!/bin/bash
# scripts/install-metrics-server.sh

echo "📊 Metrics Server 설치"

# Metrics Server 설치
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# 설치 확인
kubectl get deployment metrics-server -n kube-system
kubectl get pods -n kube-system -l k8s-app=metrics-server

# 메트릭 수집 확인 (잠시 대기 후)
sleep 30
kubectl top nodes
kubectl top pods -n kube-system

echo "✅ Metrics Server 설치 완료"
```

#### 13.2 HPA 설정

**HPA 매니페스트**:
```yaml
# k8s/autoscaling/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: gateway-service-hpa
  namespace: popcorn
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: gateway-service
  minReplicas: 2
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
      - type: Pods
        value: 2
        periodSeconds: 60
      selectPolicy: Max
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
  namespace: popcorn
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 3
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 60
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 70
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 20
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 30
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
      - type: Pods
        value: 5
        periodSeconds: 30
      selectPolicy: Max
```

---

## 🎯 Week 3-4: Helm Chart 개발

### Day 15-18: 기본 Helm Chart 구조

#### 15.1 Chart 구조 설계

**Helm Chart 디렉토리 구조**:
```
popcorn-msa-chart/
├── Chart.yaml
├── values.yaml
├── values-dev.yaml
├── values-prod.yaml
├── charts/                    # 의존성 차트
├── templates/
│   ├── _helpers.tpl          # 헬퍼 템플릿
│   ├── deployment.yaml       # 배포 템플릿
│   ├── service.yaml          # 서비스 템플릿
│   ├── ingress.yaml          # 인그레스 템플릿
│   ├── configmap.yaml        # 설정 맵 템플릿
│   ├── secret.yaml           # 시크릿 템플릿
│   ├── hpa.yaml              # HPA 템플릿
│   ├── pdb.yaml              # Pod Disruption Budget
│   ├── servicemonitor.yaml   # Prometheus ServiceMonitor
│   └── tests/                # 테스트 템플릿
│       └── test-connection.yaml
└── README.md
```

**Chart.yaml**:
```yaml
apiVersion: v2
name: popcorn-msa
description: Popcorn MSA Microservices Helm Chart
type: application
version: 0.1.0
appVersion: "1.0.0"
home: https://github.com/your-org/popcorn-msa
sources:
  - https://github.com/your-org/popcorn-msa
maintainers:
  - name: DevOps Team
    email: devops@popcorn.com
keywords:
  - microservices
  - ecommerce
  - spring-boot
  - kafka
dependencies: []
```

#### 15.2 Values 파일 설계

**기본 values.yaml**:
```yaml
# Global settings
global:
  imageRegistry: ""
  imagePullSecrets: []
  storageClass: "gp3-encrypted"

# Common settings
nameOverride: ""
fullnameOverride: ""
namespace: popcorn

# Image settings
image:
  registry: ${AWS_ACCOUNT_ID}.dkr.ecr.ap-northeast-2.amazonaws.com
  repository: popcorn
  tag: ""
  pullPolicy: IfNotPresent

# Service Account
serviceAccount:
  create: true
  name: popcorn-service-account
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::${AWS_ACCOUNT_ID}:role/${PROJECT_NAME}-${ENVIRONMENT}-service-role

# Services configuration
services:
  gateway-service:
    enabled: true
    replicas: 2
    image:
      repository: gateway-service
    service:
      type: ClusterIP
      port: 8080
      targetPort: 8080
    resources:
      requests:
        cpu: 250m
        memory: 256Mi
      limits:
        cpu: 500m
        memory: 512Mi
    autoscaling:
      enabled: true
      minReplicas: 2
      maxReplicas: 20
      targetCPUUtilizationPercentage: 70
      targetMemoryUtilizationPercentage: 80
    
  order-service:
    enabled: true
    replicas: 3
    image:
      repository: order-service
    service:
      type: ClusterIP
      port: 8080
      targetPort: 8080
    resources:
      requests:
        cpu: 500m
        memory: 512Mi
      limits:
        cpu: 1000m
        memory: 1Gi
    autoscaling:
      enabled: true
      minReplicas: 3
      maxReplicas: 50
      targetCPUUtilizationPercentage: 60
      targetMemoryUtilizationPercentage: 70
    
  payment-service:
    enabled: true
    replicas: 2
    image:
      repository: payment-service
    service:
      type: ClusterIP
      port: 8080
      targetPort: 8080
    resources:
      requests:
        cpu: 250m
        memory: 256Mi
      limits:
        cpu: 500m
        memory: 512Mi
    autoscaling:
      enabled: true
      minReplicas: 2
      maxReplicas: 10
      targetCPUUtilizationPercentage: 70
      targetMemoryUtilizationPercentage: 80

# Ingress configuration
ingress:
  enabled: true
  className: alb
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'
    alb.ingress.kubernetes.io/ssl-redirect: '443'
    alb.ingress.kubernetes.io/certificate-arn: ${CERTIFICATE_ARN}
  hosts:
    - host: api.popcorn.local
      paths:
        - path: /
          pathType: Prefix
          service: gateway-service
  tls:
    - secretName: popcorn-tls
      hosts:
        - api.popcorn.local

# Database configuration
database:
  host: ${RDS_ENDPOINT}
  port: 5432
  name: popcorn
  secretName: popcorn-db-secret

# Redis configuration
redis:
  host: ${REDIS_ENDPOINT}
  port: 6379
  secretName: popcorn-redis-secret

# Kafka configuration
kafka:
  bootstrapServers: ${KAFKA_BROKERS}
  configMapName: popcorn-kafka-config

# Monitoring
monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
    interval: 30s
    path: /actuator/prometheus

# Pod Disruption Budget
podDisruptionBudget:
  enabled: true
  minAvailable: 1
```

### Day 19-21: 템플릿 구현

#### 19.1 Deployment 템플릿

**templates/deployment.yaml** (일부):
```yaml
{{- range $serviceName, $service := .Values.services }}
{{- if $service.enabled }}
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ $serviceName }}
  namespace: {{ $.Values.namespace }}
  labels:
    {{- include "popcorn-msa.labels" $ | nindent 4 }}
    app.kubernetes.io/component: {{ $serviceName }}
spec:
  replicas: {{ $service.replicas | default 2 }}
  selector:
    matchLabels:
      {{- include "popcorn-msa.selectorLabels" $ | nindent 6 }}
      app.kubernetes.io/component: {{ $serviceName }}
  template:
    metadata:
      labels:
        {{- include "popcorn-msa.selectorLabels" $ | nindent 8 }}
        app.kubernetes.io/component: {{ $serviceName }}
        version: {{ $.Values.image.tag | default $.Chart.AppVersion }}
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8081"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: {{ $.Values.serviceAccount.name }}
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 2000
      containers:
      - name: {{ $serviceName }}
        image: "{{ $.Values.image.registry }}/{{ $service.image.repository }}:{{ $.Values.image.tag | default $.Chart.AppVersion }}"
        imagePullPolicy: {{ $.Values.image.pullPolicy }}
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        - name: management
          containerPort: 8081
          protocol: TCP
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: {{ $.Values.environment | default "dev" }}
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: {{ $.Values.database.secretName }}
              key: url
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: management
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: management
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          {{- toYaml $service.resources | nindent 10 }}
{{- end }}
{{- end }}
```

### Day 22-28: 배포 테스트 및 최적화

#### 22.1 Chart 테스트

**Chart 검증 스크립트**:
```bash
#!/bin/bash
# scripts/test-helm-chart.sh

CHART_PATH="./popcorn-msa-chart"
NAMESPACE="popcorn"
RELEASE_NAME="popcorn-msa"

echo "🧪 Helm Chart 테스트 시작"

# 1. Chart 문법 검증
echo "1. Chart 문법 검증"
helm lint $CHART_PATH

# 2. 템플릿 렌더링 테스트
echo "2. 템플릿 렌더링 테스트"
helm template $RELEASE_NAME $CHART_PATH --values $CHART_PATH/values-dev.yaml --debug

# 3. Dry-run 배포 테스트
echo "3. Dry-run 배포 테스트"
helm install $RELEASE_NAME $CHART_PATH \
  --namespace $NAMESPACE \
  --create-namespace \
  --values $CHART_PATH/values-dev.yaml \
  --dry-run --debug

# 4. 실제 배포 테스트
echo "4. 실제 배포 테스트"
helm install $RELEASE_NAME $CHART_PATH \
  --namespace $NAMESPACE \
  --create-namespace \
  --values $CHART_PATH/values-dev.yaml \
  --wait --timeout=10m

# 5. 배포 상태 확인
echo "5. 배포 상태 확인"
helm status $RELEASE_NAME -n $NAMESPACE
kubectl get all -n $NAMESPACE

echo "✅ Helm Chart 테스트 완료"
```

**검증 체크리스트**:
- [ ] EKS 클러스터 정상 동작 확인
- [ ] 모든 Add-ons 설치 및 동작 확인
- [ ] AWS Load Balancer Controller 동작 확인
- [ ] 네임스페이스 및 RBAC 설정 확인
- [ ] 스토리지 클래스 동작 확인
- [ ] HPA 동작 확인
- [ ] Helm Chart 배포 성공 확인
- [ ] 서비스 간 통신 확인
- [ ] 외부 접근 가능 확인

이 가이드를 통해 Kubernetes Engineer는 체계적으로 EKS 기반 컨테이너 플랫폼을 구축하고, 안정적인 마이크로서비스 배포 환경을 제공할 수 있습니다.