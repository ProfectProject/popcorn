# Popcorn MSA 프로젝트 초기 구축 가이드

## 📋 프로젝트 개요

**프로젝트**: Popcorn MSA (팝업 이벤트 이커머스 플랫폼)  
**아키텍처**: EKS 기반 마이크로서비스  
**구축 기간**: 4주  
**핵심 역할**: Infrastructure Engineer, Kubernetes Engineer, Monitoring Engineer  

---

## 🏗️ Infrastructure Engineer 초기 구축 가이드

### 📅 Week 1: 기반 인프라 구축

#### Day 1-2: AWS 계정 및 기본 설정

**필수 작업**:
```bash
# 1. AWS CLI 설정
aws configure
aws sts get-caller-identity

# 2. Terraform 백엔드 설정
aws s3 mb s3://popcorn-terraform-state-bucket
aws dynamodb create-table \
  --table-name popcorn-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
```

**Terraform 백엔드 구성**:
```hcl
# terraform/backend.tf
terraform {
  backend "s3" {
    bucket         = "popcorn-terraform-state-bucket"
    key            = "popcorn/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "popcorn-terraform-locks"
    encrypt        = true
  }
}
```

#### Day 3-5: VPC 및 네트워킹 구축

**VPC 모듈 작성**:
```hcl
# modules/vpc/main.tf
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "${var.project_name}-${var.environment}-vpc"
    Environment = var.environment
    Project     = var.project_name
  }
}

# Public Subnets (ALB, NAT Gateway)
resource "aws_subnet" "public" {
  count = length(var.availability_zones)

  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project_name}-${var.environment}-public-${count.index + 1}"
    Type = "Public"
    "kubernetes.io/role/elb" = "1"
  }
}

# Private Subnets - App Tier (EKS Nodes)
resource "aws_subnet" "private_app" {
  count = length(var.availability_zones)

  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_app_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name = "${var.project_name}-${var.environment}-private-app-${count.index + 1}"
    Type = "Private-App"
    "kubernetes.io/role/internal-elb" = "1"
  }
}

# Private Subnets - Data Tier (RDS, ElastiCache)
resource "aws_subnet" "private_data" {
  count = length(var.availability_zones)

  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_data_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name = "${var.project_name}-${var.environment}-private-data-${count.index + 1}"
    Type = "Private-Data"
  }
}
```

**검증 체크리스트**:
- [ ] VPC 생성 확인
- [ ] 서브넷 3계층 구성 확인 (Public, Private-App, Private-Data)
- [ ] Internet Gateway 연결 확인
- [ ] NAT Gateway 구성 확인 (환경별)
- [ ] Route Table 설정 확인

#### Day 6-7: 보안 그룹 및 IAM 설정

**보안 그룹 구성**:
```hcl
# modules/security-groups/main.tf

# ALB Security Group
resource "aws_security_group" "alb" {
  name_prefix = "${var.project_name}-${var.environment}-alb-"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-alb-sg"
  }
}

# EKS Nodes Security Group
resource "aws_security_group" "eks_nodes" {
  name_prefix = "${var.project_name}-${var.environment}-eks-nodes-"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 0
    to_port         = 65535
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # Node to node communication
  ingress {
    from_port = 0
    to_port   = 65535
    protocol  = "tcp"
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-eks-nodes-sg"
  }
}

# RDS Security Group
resource "aws_security_group" "rds" {
  name_prefix = "${var.project_name}-${var.environment}-rds-"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_nodes.id]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-rds-sg"
  }
}
```

### 📅 Week 2: 데이터베이스 및 캐시 구축

#### Day 8-10: RDS PostgreSQL 구축

**RDS 모듈 작성**:
```hcl
# modules/rds/main.tf
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-db-subnet-group"
  subnet_ids = var.private_data_subnet_ids

  tags = {
    Name = "${var.project_name}-${var.environment}-db-subnet-group"
  }
}

resource "aws_db_parameter_group" "main" {
  family = "postgres15"
  name   = "${var.project_name}-${var.environment}-db-params"

  parameter {
    name  = "shared_preload_libraries"
    value = "pg_stat_statements"
  }

  parameter {
    name  = "log_statement"
    value = "all"
  }

  parameter {
    name  = "log_min_duration_statement"
    value = "1000"
  }
}

resource "aws_db_instance" "main" {
  identifier = "${var.project_name}-${var.environment}-postgres"

  # Engine
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = var.db_instance_class

  # Storage
  allocated_storage     = var.allocated_storage
  max_allocated_storage = var.max_allocated_storage
  storage_type          = "gp3"
  storage_encrypted     = true

  # Database
  db_name  = var.database_name
  username = var.master_username
  password = var.master_password

  # Network
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.rds_security_group_id]
  publicly_accessible    = false

  # Backup
  backup_retention_period = var.backup_retention_period
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"

  # Multi-AZ (환경별)
  multi_az = var.environment == "prod" ? true : false

  # Monitoring
  monitoring_interval = 60
  monitoring_role_arn = aws_iam_role.rds_monitoring.arn

  # Parameter Group
  parameter_group_name = aws_db_parameter_group.main.name

  # Deletion Protection
  deletion_protection = var.environment == "prod" ? true : false

  tags = {
    Name        = "${var.project_name}-${var.environment}-postgres"
    Environment = var.environment
  }
}
```

**환경별 변수 설정**:
```hcl
# envs/dev/terraform.tfvars
db_instance_class = "db.t4g.micro"
allocated_storage = 20
max_allocated_storage = 100
backup_retention_period = 1

# envs/prod/terraform.tfvars
db_instance_class = "db.t4g.micro"
allocated_storage = 20
max_allocated_storage = 100
backup_retention_period = 7
```

#### Day 11-12: ElastiCache Valkey 구축

**ElastiCache 모듈 작성**:
```hcl
# modules/elasticache/main.tf
resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-cache-subnet"
  subnet_ids = var.private_data_subnet_ids
}

resource "aws_elasticache_parameter_group" "main" {
  family = "valkey7"
  name   = "${var.project_name}-${var.environment}-cache-params"

  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"
  }
}

# Dev 환경: 단일 노드
resource "aws_elasticache_cluster" "single" {
  count = var.environment == "dev" ? 1 : 0

  cluster_id           = "${var.project_name}-${var.environment}-cache"
  engine               = "valkey"
  node_type            = var.node_type
  num_cache_nodes      = 1
  parameter_group_name = aws_elasticache_parameter_group.main.name
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.main.name
  security_group_ids   = [var.cache_security_group_id]

  tags = {
    Name = "${var.project_name}-${var.environment}-cache"
  }
}

# Prod 환경: Primary + Replica
resource "aws_elasticache_replication_group" "main" {
  count = var.environment == "prod" ? 1 : 0

  replication_group_id       = "${var.project_name}-${var.environment}-cache"
  description                = "Popcorn ${var.environment} cache cluster"
  
  node_type                  = var.node_type
  port                       = 6379
  parameter_group_name       = aws_elasticache_parameter_group.main.name
  
  num_cache_clusters         = 2
  automatic_failover_enabled = true
  multi_az_enabled          = true
  
  subnet_group_name = aws_elasticache_subnet_group.main.name
  security_group_ids = [var.cache_security_group_id]

  # Backup
  snapshot_retention_limit = 7
  snapshot_window         = "03:00-05:00"

  tags = {
    Name = "${var.project_name}-${var.environment}-cache"
  }
}
```

#### Day 13-14: EC2 Kafka 클러스터 구축

**Kafka 클러스터 모듈**:
```hcl
# modules/kafka/main.tf
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
}

# Kafka 인스턴스
resource "aws_instance" "kafka" {
  count = var.kafka_broker_count

  ami           = data.aws_ami.amazon_linux.id
  instance_type = var.kafka_instance_type
  key_name      = var.key_pair_name

  subnet_id                   = var.private_app_subnet_ids[count.index % length(var.private_app_subnet_ids)]
  vpc_security_group_ids      = [aws_security_group.kafka.id]
  associate_public_ip_address = false

  # EBS 볼륨 (Kafka 로그)
  ebs_block_device {
    device_name = "/dev/xvdf"
    volume_type = "gp3"
    volume_size = var.kafka_log_volume_size
    encrypted   = true
  }

  user_data = templatefile("${path.module}/kafka-userdata.sh", {
    broker_id = count.index + 1
    kafka_version = var.kafka_version
    cluster_name = "${var.project_name}-${var.environment}"
  })

  tags = {
    Name = "${var.project_name}-${var.environment}-kafka-${count.index + 1}"
    Type = "Kafka"
  }
}

# Kafka 보안 그룹
resource "aws_security_group" "kafka" {
  name_prefix = "${var.project_name}-${var.environment}-kafka-"
  vpc_id      = var.vpc_id

  # Kafka 브로커 간 통신
  ingress {
    from_port = 9092
    to_port   = 9092
    protocol  = "tcp"
    self      = true
  }

  # EKS 노드에서 Kafka 접근
  ingress {
    from_port       = 9092
    to_port         = 9092
    protocol        = "tcp"
    security_groups = [var.eks_nodes_security_group_id]
  }

  # KRaft 컨트롤러 통신
  ingress {
    from_port = 9093
    to_port   = 9093
    protocol  = "tcp"
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-kafka-sg"
  }
}
```

**Kafka 설치 스크립트**:
```bash
#!/bin/bash
# kafka-userdata.sh

# 변수 설정
KAFKA_VERSION="${kafka_version}"
BROKER_ID="${broker_id}"
CLUSTER_NAME="${cluster_name}"

# Java 설치
yum update -y
yum install -y java-11-amazon-corretto

# Kafka 다운로드 및 설치
cd /opt
wget https://downloads.apache.org/kafka/2.8.2/kafka_2.13-$KAFKA_VERSION.tgz
tar -xzf kafka_2.13-$KAFKA_VERSION.tgz
mv kafka_2.13-$KAFKA_VERSION kafka
chown -R ec2-user:ec2-user kafka

# EBS 볼륨 마운트
mkfs -t xfs /dev/xvdf
mkdir -p /kafka-logs
mount /dev/xvdf /kafka-logs
chown -R ec2-user:ec2-user /kafka-logs

# fstab 추가
echo '/dev/xvdf /kafka-logs xfs defaults,nofail 0 2' >> /etc/fstab

# Kafka 설정 파일 생성
cat > /opt/kafka/config/kraft/server.properties << EOF
# KRaft 모드 설정
process.roles=broker,controller
node.id=$BROKER_ID
controller.quorum.voters=1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093

# 리스너 설정
listeners=PLAINTEXT://:9092,CONTROLLER://:9093
advertised.listeners=PLAINTEXT://$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4):9092
controller.listener.names=CONTROLLER
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT

# 로그 디렉토리
log.dirs=/kafka-logs

# 복제 설정
default.replication.factor=3
min.insync.replicas=2

# 로그 보존 설정
log.retention.hours=168
log.segment.bytes=1073741824
log.retention.check.interval.ms=300000

# 기타 설정
num.network.threads=8
num.io.threads=8
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600
EOF

# 클러스터 ID 생성 (첫 번째 브로커에서만)
if [ "$BROKER_ID" = "1" ]; then
    /opt/kafka/bin/kafka-storage.sh random-uuid > /tmp/cluster-id
fi

# Systemd 서비스 파일 생성
cat > /etc/systemd/system/kafka.service << EOF
[Unit]
Description=Apache Kafka
After=network.target

[Service]
Type=simple
User=ec2-user
ExecStart=/opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/kraft/server.properties
ExecStop=/opt/kafka/bin/kafka-server-stop.sh
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 서비스 활성화
systemctl daemon-reload
systemctl enable kafka
```

**검증 체크리스트**:
- [ ] RDS PostgreSQL 연결 테스트
- [ ] ElastiCache Valkey 연결 테스트  
- [ ] Kafka 클러스터 상태 확인
- [ ] 보안 그룹 규칙 검증
- [ ] 백업 설정 확인

---

## ☸️ Kubernetes Engineer 초기 구축 가이드

### 📅 Week 1: EKS 클러스터 구축

#### Day 1-3: EKS 클러스터 생성

**EKS 모듈 작성**:
```hcl
# modules/eks/main.tf
resource "aws_eks_cluster" "main" {
  name     = "${var.project_name}-${var.environment}-eks"
  role_arn = aws_iam_role.cluster.arn
  version  = var.kubernetes_version

  vpc_config {
    subnet_ids              = concat(var.public_subnet_ids, var.private_app_subnet_ids)
    endpoint_private_access = true
    endpoint_public_access  = true
    public_access_cidrs     = var.cluster_endpoint_public_access_cidrs
    security_group_ids      = [aws_security_group.cluster.id]
  }

  # 로깅 활성화
  enabled_cluster_log_types = ["api", "audit", "authenticator", "controllerManager", "scheduler"]

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

#### Day 4-5: 노드 그룹 구성

**관리형 노드 그룹**:
```hcl
# modules/eks/node-groups.tf
resource "aws_eks_node_group" "main" {
  cluster_name    = aws_eks_cluster.main.name
  node_group_name = "${var.project_name}-${var.environment}-nodes"
  node_role_arn   = aws_iam_role.node_group.arn
  subnet_ids      = var.private_app_subnet_ids

  # 인스턴스 타입
  instance_types = var.node_instance_types
  capacity_type  = var.node_capacity_type  # ON_DEMAND or SPOT

  # 스케일링 설정
  scaling_config {
    desired_size = var.node_desired_size
    max_size     = var.node_max_size
    min_size     = var.node_min_size
  }

  # 업데이트 설정
  update_config {
    max_unavailable_percentage = 25
  }

  # 디스크 설정
  disk_size = var.node_disk_size

  # AMI 타입
  ami_type = "AL2_x86_64"

  # 원격 접근 설정
  remote_access {
    ec2_ssh_key = var.key_pair_name
    source_security_group_ids = [aws_security_group.node_group_remote_access.id]
  }

  # 라벨
  labels = {
    Environment = var.environment
    NodeGroup   = "main"
  }

  # 테인트 (필요시)
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
}

# Fargate 프로필 (선택적)
resource "aws_eks_fargate_profile" "main" {
  count = var.enable_fargate ? 1 : 0

  cluster_name           = aws_eks_cluster.main.name
  fargate_profile_name   = "${var.project_name}-${var.environment}-fargate"
  pod_execution_role_arn = aws_iam_role.fargate_pod_execution[0].arn
  subnet_ids             = var.private_app_subnet_ids

  selector {
    namespace = "fargate"
    labels = {
      compute-type = "fargate"
    }
  }

  selector {
    namespace = "kube-system"
    labels = {
      k8s-app = "kube-dns"
    }
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-fargate"
  }
}
```

#### Day 6-7: EKS Add-ons 설치

**필수 Add-ons 설치**:
```hcl
# modules/eks/addons.tf

# VPC CNI
resource "aws_eks_addon" "vpc_cni" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "vpc-cni"
  addon_version = var.vpc_cni_version
  resolve_conflicts = "OVERWRITE"

  tags = {
    Name = "${var.project_name}-${var.environment}-vpc-cni"
  }
}

# CoreDNS
resource "aws_eks_addon" "coredns" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "coredns"
  addon_version = var.coredns_version
  resolve_conflicts = "OVERWRITE"

  depends_on = [aws_eks_node_group.main]

  tags = {
    Name = "${var.project_name}-${var.environment}-coredns"
  }
}

# kube-proxy
resource "aws_eks_addon" "kube_proxy" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "kube-proxy"
  addon_version = var.kube_proxy_version
  resolve_conflicts = "OVERWRITE"

  tags = {
    Name = "${var.project_name}-${var.environment}-kube-proxy"
  }
}

# EBS CSI Driver
resource "aws_eks_addon" "ebs_csi" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "aws-ebs-csi-driver"
  addon_version = var.ebs_csi_version
  resolve_conflicts = "OVERWRITE"
  service_account_role_arn = aws_iam_role.ebs_csi_driver.arn

  tags = {
    Name = "${var.project_name}-${var.environment}-ebs-csi"
  }
}
```

### 📅 Week 2: Kubernetes 기본 구성

#### Day 8-10: 네임스페이스 및 RBAC 설정

**네임스페이스 구성**:
```yaml
# k8s/namespaces/popcorn-namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: popcorn
  labels:
    name: popcorn
    environment: ${ENVIRONMENT}
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
```

**RBAC 설정**:
```yaml
# k8s/rbac/popcorn-rbac.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: popcorn-service-account
  namespace: popcorn
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::${AWS_ACCOUNT_ID}:role/popcorn-${ENVIRONMENT}-service-role
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: popcorn
  name: popcorn-role
rules:
- apiGroups: [""]
  resources: ["pods", "services", "configmaps", "secrets"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets"]
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

#### Day 11-12: AWS Load Balancer Controller 설치

**Helm을 통한 설치**:
```bash
#!/bin/bash
# scripts/install-aws-load-balancer-controller.sh

# 변수 설정
CLUSTER_NAME="${PROJECT_NAME}-${ENVIRONMENT}-eks"
AWS_REGION="ap-northeast-2"
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# IAM 정책 다운로드
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.6.0/docs/install/iam_policy.json

# IAM 정책 생성
aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://iam_policy.json

# 서비스 계정 생성
eksctl create iamserviceaccount \
  --cluster=$CLUSTER_NAME \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --role-name AmazonEKSLoadBalancerControllerRole \
  --attach-policy-arn=arn:aws:iam::$AWS_ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy \
  --approve

# Helm 레포지토리 추가
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# AWS Load Balancer Controller 설치
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=$CLUSTER_NAME \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller \
  --set region=$AWS_REGION \
  --set vpcId=$VPC_ID

# 설치 확인
kubectl get deployment -n kube-system aws-load-balancer-controller
```

#### Day 13-14: 스토리지 클래스 및 HPA 설정

**스토리지 클래스 구성**:
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
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
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
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

**HPA 설정**:
```yaml
# k8s/autoscaling/metrics-server.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: metrics-server
  namespace: kube-system
  labels:
    k8s-app: metrics-server
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: metrics-server
  namespace: kube-system
  labels:
    k8s-app: metrics-server
spec:
  selector:
    matchLabels:
      k8s-app: metrics-server
  template:
    metadata:
      labels:
        k8s-app: metrics-server
    spec:
      serviceAccountName: metrics-server
      containers:
      - name: metrics-server
        image: k8s.gcr.io/metrics-server/metrics-server:v0.6.4
        args:
          - --cert-dir=/tmp
          - --secure-port=4443
          - --kubelet-preferred-address-types=InternalIP,ExternalIP,Hostname
          - --kubelet-use-node-status-port
          - --metric-resolution=15s
        resources:
          requests:
            cpu: 100m
            memory: 200Mi
        volumeMounts:
        - name: tmp-dir
          mountPath: /tmp
      volumes:
      - name: tmp-dir
        emptyDir: {}
```

### 📅 Week 3-4: 애플리케이션 배포 준비

#### Day 15-21: Helm Chart 개발

**기본 Helm Chart 구조**:
```
popcorn-msa-chart/
├── Chart.yaml
├── values.yaml
├── values-dev.yaml
├── values-prod.yaml
└── templates/
    ├── deployment.yaml
    ├── service.yaml
    ├── ingress.yaml
    ├── configmap.yaml
    ├── secret.yaml
    ├── hpa.yaml
    └── servicemonitor.yaml
```

**Chart.yaml**:
```yaml
apiVersion: v2
name: popcorn-msa
description: Popcorn MSA Helm Chart
type: application
version: 0.1.0
appVersion: "1.0.0"
dependencies:
  - name: postgresql
    version: 12.1.2
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
  - name: redis
    version: 17.4.3
    repository: https://charts.bitnami.com/bitnami
    condition: redis.enabled
```

**Deployment 템플릿**:
```yaml
# templates/deployment.yaml
{{- range $service := .Values.services }}
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ $service.name }}
  namespace: {{ $.Values.namespace }}
  labels:
    app: {{ $service.name }}
    version: {{ $.Values.image.tag | default $.Chart.AppVersion }}
spec:
  replicas: {{ $service.replicas | default 2 }}
  selector:
    matchLabels:
      app: {{ $service.name }}
  template:
    metadata:
      labels:
        app: {{ $service.name }}
        version: {{ $.Values.image.tag | default $.Chart.AppVersion }}
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: {{ $.Values.serviceAccount.name }}
      containers:
      - name: {{ $service.name }}
        image: "{{ $.Values.image.repository }}/{{ $service.name }}:{{ $.Values.image.tag | default $.Chart.AppVersion }}"
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
          value: {{ $.Values.environment }}
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: {{ $.Values.database.secretName }}
              key: url
        - name: DATABASE_USERNAME
          valueFrom:
            secretKeyRef:
              name: {{ $.Values.database.secretName }}
              key: username
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: {{ $.Values.database.secretName }}
              key: password
        - name: REDIS_URL
          valueFrom:
            secretKeyRef:
              name: {{ $.Values.redis.secretName }}
              key: url
        - name: KAFKA_BOOTSTRAP_SERVERS
          valueFrom:
            configMapKeyRef:
              name: {{ $.Values.kafka.configMapName }}
              key: bootstrap-servers
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: management
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: management
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        resources:
          {{- toYaml $service.resources | nindent 10 }}
        volumeMounts:
        - name: config
          mountPath: /app/config
          readOnly: true
      volumes:
      - name: config
        configMap:
          name: {{ $service.name }}-config
{{- end }}
```

**검증 체크리스트**:
- [ ] EKS 클러스터 정상 동작 확인
- [ ] 노드 그룹 Auto Scaling 테스트
- [ ] AWS Load Balancer Controller 동작 확인
- [ ] Helm Chart 배포 테스트
- [ ] 네임스페이스 및 RBAC 검증

---

## 📊 Monitoring Engineer 초기 구축 가이드

### 📅 Week 1: 모니터링 스택 기반 구축

#### Day 1-3: Prometheus 클러스터 구축

**Prometheus Operator 설치**:
```bash
#!/bin/bash
# scripts/install-prometheus-stack.sh

# kube-prometheus-stack 설치
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# 네임스페이스 생성
kubectl create namespace monitoring

# Prometheus Stack 설치
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --values prometheus-values.yaml \
  --wait
```

**Prometheus 설정 파일**:
```yaml
# prometheus-values.yaml
prometheus:
  prometheusSpec:
    retention: 30d
    retentionSize: 50GB
    storageSpec:
      volumeClaimTemplate:
        spec:
          storageClassName: gp3-encrypted
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 100Gi
    
    # 리소스 설정
    resources:
      requests:
        cpu: 500m
        memory: 2Gi
      limits:
        cpu: 2000m
        memory: 4Gi
    
    # 추가 스크랩 설정
    additionalScrapeConfigs:
      - job_name: 'kafka-jmx'
        static_configs:
          - targets: ['kafka-1:9999', 'kafka-2:9999', 'kafka-3:9999']
      
      - job_name: 'postgres-exporter'
        static_configs:
          - targets: ['postgres-exporter:9187']
      
      - job_name: 'redis-exporter'
        static_configs:
          - targets: ['redis-exporter:9121']

grafana:
  adminPassword: admin123
  
  # 영구 스토리지
  persistence:
    enabled: true
    storageClassName: gp3-encrypted
    size: 10Gi
  
  # 리소스 설정
  resources:
    requests:
      cpu: 100m
      memory: 256Mi
    limits:
      cpu: 500m
      memory: 512Mi
  
  # 대시보드 자동 import
  dashboardProviders:
    dashboardproviders.yaml:
      apiVersion: 1
      providers:
      - name: 'default'
        orgId: 1
        folder: ''
        type: file
        disableDeletion: false
        editable: true
        options:
          path: /var/lib/grafana/dashboards/default
  
  dashboards:
    default:
      kubernetes-cluster:
        gnetId: 7249
        revision: 1
        datasource: Prometheus
      kubernetes-pods:
        gnetId: 6417
        revision: 1
        datasource: Prometheus
      jvm-micrometer:
        gnetId: 4701
        revision: 6
        datasource: Prometheus

alertmanager:
  alertmanagerSpec:
    storage:
      volumeClaimTemplate:
        spec:
          storageClassName: gp3-encrypted
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 10Gi
    
    # Slack 알림 설정
    config:
      global:
        slack_api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
      
      route:
        group_by: ['alertname']
        group_wait: 10s
        group_interval: 10s
        repeat_interval: 1h
        receiver: 'web.hook'
      
      receivers:
      - name: 'web.hook'
        slack_configs:
        - channel: '#alerts'
          title: 'Popcorn MSA Alert'
          text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
```

#### Day 4-5: 애플리케이션 메트릭 수집 설정

**ServiceMonitor 설정**:
```yaml
# k8s/monitoring/servicemonitors.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: popcorn-services
  namespace: monitoring
  labels:
    app: popcorn-services
spec:
  selector:
    matchLabels:
      monitoring: enabled
  namespaceSelector:
    matchNames:
    - popcorn
  endpoints:
  - port: management
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
---
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: kafka-jmx
  namespace: monitoring
spec:
  selector:
    matchLabels:
      app: kafka-jmx-exporter
  endpoints:
  - port: metrics
    interval: 30s
```

**JMX Exporter for Kafka**:
```yaml
# k8s/monitoring/kafka-jmx-exporter.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-jmx-exporter
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kafka-jmx-exporter
  template:
    metadata:
      labels:
        app: kafka-jmx-exporter
    spec:
      containers:
      - name: jmx-exporter
        image: sscaling/jmx-prometheus-exporter:0.17.0
        ports:
        - containerPort: 5556
          name: metrics
        env:
        - name: CONFIG_YML
          value: |
            startDelaySeconds: 0
            ssl: false
            lowercaseOutputName: false
            lowercaseOutputLabelNames: false
            rules:
            - pattern: "kafka.server<type=(.+), name=(.+)><>Value"
              name: kafka_server_$1_$2
            - pattern: "kafka.server<type=(.+), name=(.+), clientId=(.+)><>Value"
              name: kafka_server_$1_$2
              labels:
                clientId: "$3"
        command:
        - java
        - -XX:+UnlockExperimentalVMOptions
        - -XX:+UseCGroupMemoryLimitForHeap
        - -XX:MaxRAMFraction=1
        - -XshowSettings:vm
        - -jar
        - jmx_prometheus_httpserver.jar
        - "5556"
        - /etc/jmx-exporter/config.yml
---
apiVersion: v1
kind: Service
metadata:
  name: kafka-jmx-exporter
  namespace: monitoring
  labels:
    app: kafka-jmx-exporter
spec:
  ports:
  - port: 5556
    targetPort: 5556
    name: metrics
  selector:
    app: kafka-jmx-exporter
```

#### Day 6-7: 알림 규칙 설정

**PrometheusRule 설정**:
```yaml
# k8s/monitoring/prometheus-rules.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: popcorn-alerts
  namespace: monitoring
  labels:
    prometheus: kube-prometheus
    role: alert-rules
spec:
  groups:
  - name: popcorn.rules
    rules:
    # 높은 에러율 알림
    - alert: HighErrorRate
      expr: |
        (
          sum(rate(http_requests_total{job=~".*-service",status=~"5.."}[5m])) by (job) /
          sum(rate(http_requests_total{job=~".*-service"}[5m])) by (job)
        ) * 100 > 5
      for: 2m
      labels:
        severity: critical
      annotations:
        summary: "High error rate detected for {{ $labels.job }}"
        description: "Error rate is {{ $value }}% for service {{ $labels.job }}"
    
    # 높은 응답 시간 알림
    - alert: HighLatency
      expr: |
        histogram_quantile(0.95, 
          sum(rate(http_request_duration_seconds_bucket{job=~".*-service"}[5m])) by (le, job)
        ) * 1000 > 1000
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High latency detected for {{ $labels.job }}"
        description: "95th percentile latency is {{ $value }}ms for service {{ $labels.job }}"
    
    # Pod 재시작 알림
    - alert: PodRestartingTooMuch
      expr: |
        increase(kube_pod_container_status_restarts_total{namespace="popcorn"}[1h]) > 3
      for: 0m
      labels:
        severity: warning
      annotations:
        summary: "Pod {{ $labels.pod }} is restarting too much"
        description: "Pod {{ $labels.pod }} in namespace {{ $labels.namespace }} has restarted {{ $value }} times in the last hour"
    
    # 메모리 사용률 알림
    - alert: HighMemoryUsage
      expr: |
        (
          container_memory_working_set_bytes{namespace="popcorn", container!="POD", container!=""} /
          container_spec_memory_limit_bytes{namespace="popcorn", container!="POD", container!=""} * 100
        ) > 80
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High memory usage for {{ $labels.pod }}"
        description: "Memory usage is {{ $value }}% for pod {{ $labels.pod }}"
    
    # CPU 사용률 알림
    - alert: HighCPUUsage
      expr: |
        (
          rate(container_cpu_usage_seconds_total{namespace="popcorn", container!="POD", container!=""}[5m]) /
          container_spec_cpu_quota{namespace="popcorn", container!="POD", container!=""} * 
          container_spec_cpu_period{namespace="popcorn", container!="POD", container!=""} * 100
        ) > 80
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High CPU usage for {{ $labels.pod }}"
        description: "CPU usage is {{ $value }}% for pod {{ $labels.pod }}"
    
    # Kafka 관련 알림
    - alert: KafkaConsumerLag
      expr: kafka_consumer_lag_sum > 1000
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "Kafka consumer lag is high"
        description: "Consumer lag is {{ $value }} for topic {{ $labels.topic }}"
    
    # 데이터베이스 연결 알림
    - alert: DatabaseConnectionHigh
      expr: |
        (
          pg_stat_activity_count{state="active"} /
          pg_settings_max_connections * 100
        ) > 80
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "Database connection usage is high"
        description: "Database connection usage is {{ $value }}%"
```

### 📅 Week 2: 로깅 시스템 구축

#### Day 8-10: Loki 설치 및 구성

**Loki 설치**:
```bash
#!/bin/bash
# scripts/install-loki-stack.sh

# Loki 설치
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

helm install loki grafana/loki-stack \
  --namespace monitoring \
  --values loki-values.yaml \
  --wait
```

**Loki 설정**:
```yaml
# loki-values.yaml
loki:
  enabled: true
  persistence:
    enabled: true
    storageClassName: gp3-encrypted
    size: 50Gi
  
  config:
    auth_enabled: false
    server:
      http_listen_port: 3100
    
    ingester:
      lifecycler:
        address: 127.0.0.1
        ring:
          kvstore:
            store: inmemory
          replication_factor: 1
        final_sleep: 0s
      chunk_idle_period: 5m
      chunk_retain_period: 30s
      max_transfer_retries: 0
    
    schema_config:
      configs:
        - from: 2020-10-24
          store: boltdb-shipper
          object_store: filesystem
          schema: v11
          index:
            prefix: index_
            period: 24h
    
    storage_config:
      boltdb_shipper:
        active_index_directory: /data/loki/boltdb-shipper-active
        cache_location: /data/loki/boltdb-shipper-cache
        shared_store: filesystem
      filesystem:
        directory: /data/loki/chunks
    
    limits_config:
      enforce_metric_name: false
      reject_old_samples: true
      reject_old_samples_max_age: 168h
    
    chunk_store_config:
      max_look_back_period: 0s
    
    table_manager:
      retention_deletes_enabled: false
      retention_period: 0s

promtail:
  enabled: true
  config:
    server:
      http_listen_port: 3101
    
    positions:
      filename: /tmp/positions.yaml
    
    clients:
      - url: http://loki:3100/loki/api/v1/push
    
    scrape_configs:
      # Kubernetes 로그 수집
      - job_name: kubernetes-pods
        kubernetes_sd_configs:
          - role: pod
        relabel_configs:
          - source_labels:
              - __meta_kubernetes_pod_controller_name
            regex: ([0-9a-z-.]+?)(-[0-9a-f]{8,10})?
            action: replace
            target_label: __tmp_controller_name
          - source_labels:
              - __meta_kubernetes_pod_label_app_kubernetes_io_name
              - __meta_kubernetes_pod_label_app
              - __tmp_controller_name
              - __meta_kubernetes_pod_name
            regex: ^;*([^;]+)(;.*)?$
            action: replace
            target_label: app
          - source_labels:
              - __meta_kubernetes_pod_label_app_kubernetes_io_instance
              - __meta_kubernetes_pod_label_release
            regex: ^;*([^;]+)(;.*)?$
            action: replace
            target_label: instance
          - source_labels:
              - __meta_kubernetes_pod_label_app_kubernetes_io_component
              - __meta_kubernetes_pod_label_component
            regex: ^;*([^;]+)(;.*)?$
            action: replace
            target_label: component
        pipeline_stages:
          - json:
              expressions:
                level: level
                timestamp: timestamp
                message: message
          - timestamp:
              source: timestamp
              format: RFC3339Nano
          - labels:
              level:

fluent-bit:
  enabled: false

filebeat:
  enabled: false

logstash:
  enabled: false
```

#### Day 11-12: 분산 추적 시스템 (Jaeger) 구축

**Jaeger 설치**:
```bash
#!/bin/bash
# scripts/install-jaeger.sh

# Jaeger Operator 설치
kubectl create namespace observability
kubectl apply -f https://github.com/jaegertracing/jaeger-operator/releases/download/v1.41.0/jaeger-operator.yaml -n observability

# Jaeger 인스턴스 생성
kubectl apply -f jaeger-instance.yaml
```

**Jaeger 인스턴스 설정**:
```yaml
# jaeger-instance.yaml
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: jaeger
  namespace: observability
spec:
  strategy: production
  
  collector:
    maxReplicas: 5
    resources:
      limits:
        cpu: 500m
        memory: 512Mi
      requests:
        cpu: 100m
        memory: 256Mi
  
  query:
    replicas: 2
    resources:
      limits:
        cpu: 500m
        memory: 512Mi
      requests:
        cpu: 100m
        memory: 256Mi
  
  storage:
    type: elasticsearch
    elasticsearch:
      nodeCount: 3
      redundancyPolicy: SingleRedundancy
      resources:
        requests:
          cpu: 500m
          memory: 2Gi
        limits:
          cpu: 1000m
          memory: 2Gi
      storage:
        storageClassName: gp3-encrypted
        size: 50Gi
    
  ingress:
    enabled: true
    annotations:
      kubernetes.io/ingress.class: alb
      alb.ingress.kubernetes.io/scheme: internet-facing
      alb.ingress.kubernetes.io/target-type: ip
    hosts:
      - jaeger.popcorn.local
```

#### Day 13-14: 대시보드 및 알림 최적화

**Grafana 대시보드 생성**:
```json
{
  "dashboard": {
    "title": "Popcorn MSA Overview",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{job=~\".*-service\"}[5m])) by (job)",
            "legendFormat": "{{ job }}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{job=~\".*-service\",status=~\"5..\"}[5m])) by (job) / sum(rate(http_requests_total{job=~\".*-service\"}[5m])) by (job) * 100",
            "legendFormat": "{{ job }}"
          }
        ]
      },
      {
        "title": "Response Time (95th percentile)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{job=~\".*-service\"}[5m])) by (le, job)) * 1000",
            "legendFormat": "{{ job }}"
          }
        ]
      },
      {
        "title": "Pod Status",
        "type": "table",
        "targets": [
          {
            "expr": "kube_pod_status_phase{namespace=\"popcorn\"}",
            "format": "table"
          }
        ]
      }
    ]
  }
}
```

**검증 체크리스트**:
- [ ] Prometheus 메트릭 수집 확인
- [ ] Grafana 대시보드 접속 및 데이터 확인
- [ ] Loki 로그 수집 확인
- [ ] Jaeger 분산 추적 확인
- [ ] AlertManager 알림 테스트
- [ ] Slack 알림 수신 확인

---

## 🔄 통합 검증 및 테스트

### 전체 시스템 통합 테스트

**1주차 검증 항목**:
```bash
#!/bin/bash
# scripts/integration-test.sh

echo "🔍 Infrastructure 검증"
# VPC 및 서브넷 확인
aws ec2 describe-vpcs --filters "Name=tag:Name,Values=*popcorn*"
aws ec2 describe-subnets --filters "Name=tag:Name,Values=*popcorn*"

# RDS 연결 테스트
psql -h $RDS_ENDPOINT -U $DB_USER -d $DB_NAME -c "SELECT version();"

# ElastiCache 연결 테스트
redis-cli -h $REDIS_ENDPOINT ping

# Kafka 클러스터 상태 확인
kafka-topics.sh --bootstrap-server $KAFKA_BROKERS --list

echo "✅ Infrastructure 검증 완료"

echo "🔍 Kubernetes 검증"
# 클러스터 상태 확인
kubectl cluster-info
kubectl get nodes
kubectl get pods -A

# 서비스 배포 테스트
helm install test-app ./popcorn-msa-chart --dry-run

echo "✅ Kubernetes 검증 완료"

echo "🔍 Monitoring 검증"
# Prometheus 타겟 확인
curl -s http://prometheus:9090/api/v1/targets | jq '.data.activeTargets[] | select(.health != "up")'

# Grafana 대시보드 확인
curl -s http://grafana:3000/api/health

# Loki 로그 쿼리 테스트
curl -s "http://loki:3100/loki/api/v1/query?query={namespace=\"popcorn\"}"

echo "✅ Monitoring 검증 완료"
```

### 성능 및 부하 테스트

**부하 테스트 스크립트**:
```bash
#!/bin/bash
# scripts/load-test.sh

# JMeter 부하 테스트
jmeter -n -t load-test-plan.jmx \
  -Jthreads=100 \
  -Jrampup=60 \
  -Jduration=300 \
  -Jhost=popcorn.local \
  -l results.jtl

# 결과 분석
echo "📊 부하 테스트 결과:"
awk -F',' 'NR>1 {sum+=$2; count++} END {print "평균 응답시간:", sum/count "ms"}' results.jtl
awk -F',' 'NR>1 && $8=="false" {errors++} END {print "에러율:", (errors/NR-1)*100 "%"}' results.jtl
```

이 가이드를 통해 각 역할별로 체계적이고 실무 중심의 초기 구축 작업을 수행할 수 있습니다. 각 단계별로 검증 체크리스트를 포함하여 품질을 보장하고, 실제 운영 환경에서 안정적으로 동작할 수 있는 시스템을 구축할 수 있습니다.