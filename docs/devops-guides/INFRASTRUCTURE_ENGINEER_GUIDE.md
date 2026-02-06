# Infrastructure Engineer 작업 가이드

## 🎯 역할 개요
Infrastructure Engineer는 AWS 인프라 구축 및 관리, Terraform IaC 개발, 데이터베이스 운영을 담당합니다.

## 📋 일일 체크리스트

### 아침 (09:00 - 10:00)
- [ ] AWS 계정 비용 대시보드 확인
- [ ] CloudWatch 알림 및 이벤트 검토
- [ ] RDS 인스턴스 상태 및 성능 확인
- [ ] ElastiCache 클러스터 상태 점검
- [ ] EC2 Kafka 클러스터 상태 확인
- [ ] 백업 작업 성공 여부 확인

### 오전 (10:00 - 12:00)
- [ ] Terraform 상태 파일 무결성 확인
- [ ] 인프라 변경 요청 검토 및 계획
- [ ] 보안 그룹 및 네트워크 설정 점검
- [ ] VPC 엔드포인트 사용량 분석
- [ ] Route 53 DNS 레코드 상태 확인

### 오후 (13:00 - 18:00)
- [ ] Terraform 코드 개발 및 리뷰
- [ ] 인프라 변경사항 적용
- [ ] 데이터베이스 성능 튜닝
- [ ] 비용 최적화 작업
- [ ] 문서 업데이트 및 아키텍처 다이어그램 관리

## 🏗️ Terraform 인프라 관리

### 일일 Terraform 상태 점검
```bash
#!/bin/bash
# terraform-daily-check.sh

ENVIRONMENTS=("dev" "prod")
BASE_DIR="/Users/beom/IdeaProjects/popcorn-terraform-feature"

echo "🔍 Terraform 상태 일일 점검"
echo "=========================="

for env in "${ENVIRONMENTS[@]}"; do
    echo -e "\n📁 Environment: $env"
    cd "$BASE_DIR/envs/$env"
    
    # 상태 파일 확인
    echo "📊 Terraform 상태:"
    terraform show -json | jq '.values.root_module.resources | length'
    
    # 드리프트 감지
    echo "🔄 설정 드리프트 확인:"
    terraform plan -detailed-exitcode
    PLAN_EXIT_CODE=$?
    
    if [ $PLAN_EXIT_CODE -eq 0 ]; then
        echo "✅ 드리프트 없음"
    elif [ $PLAN_EXIT_CODE -eq 2 ]; then
        echo "⚠️ 드리프트 감지됨 - 검토 필요"
    else
        echo "❌ 계획 실행 오류"
    fi
    
    # 리소스 상태 요약
    echo "📈 리소스 요약:"
    terraform state list | wc -l | xargs echo "총 리소스 수:"
done

cd "$BASE_DIR"
echo -e "\n✅ Terraform 점검 완료"
```

### Terraform 모듈 개발 가이드

**모듈 구조 표준**:
```
modules/
├── vpc/
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   └── README.md
├── eks/
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   ├── iam.tf
│   └── README.md
└── rds/
    ├── main.tf
    ├── variables.tf
    ├── outputs.tf
    └── README.md
```

**모듈 개발 체크리스트**:
```yaml
Code Quality:
  - [ ] 변수명 명명 규칙 준수
  - [ ] 모든 변수에 description 추가
  - [ ] 적절한 기본값 설정
  - [ ] 출력값 정의 및 문서화

Security:
  - [ ] 민감한 정보 변수로 분리
  - [ ] 최소 권한 원칙 적용
  - [ ] 암호화 설정 기본 활성화
  - [ ] 보안 그룹 규칙 최소화

Best Practices:
  - [ ] 리소스 태깅 표준 적용
  - [ ] 조건부 리소스 생성 지원
  - [ ] 환경별 설정 분리
  - [ ] 버전 관리 및 호환성 고려
```

### 환경별 인프라 관리

**개발 환경 (Dev)**:
```hcl
# envs/dev/terraform.tfvars
environment = "dev"
region      = "ap-northeast-2"

# VPC 설정
vpc_cidr = "10.0.0.0/16"
availability_zones = ["ap-northeast-2a"]

# EKS 설정
eks_cluster_version = "1.35"
eks_node_groups = {
  main = {
    instance_types = ["t3.medium"]
    min_size      = 2
    max_size      = 5
    desired_size  = 2
  }
}

# RDS 설정
rds_instance_class = "db.t4g.micro"
rds_multi_az      = false
rds_backup_retention_period = 1

# ElastiCache 설정
elasticache_node_type = "cache.t4g.micro"
elasticache_num_cache_nodes = 1

# Kafka 설정
kafka_instance_type = "t3.small"
kafka_cluster_size = 1
```

**프로덕션 환경 (Prod)**:
```hcl
# envs/prod/terraform.tfvars
environment = "prod"
region      = "ap-northeast-2"

# VPC 설정
vpc_cidr = "10.1.0.0/16"
availability_zones = ["ap-northeast-2a", "ap-northeast-2c"]

# EKS 설정
eks_cluster_version = "1.35"
eks_node_groups = {
  main = {
    instance_types = ["t3.medium"]
    min_size      = 3
    max_size      = 10
    desired_size  = 3
  }
}

# RDS 설정
rds_instance_class = "db.t4g.micro"
rds_multi_az      = true
rds_backup_retention_period = 7

# ElastiCache 설정
elasticache_node_type = "cache.t4g.small"
elasticache_num_cache_nodes = 2

# Kafka 설정
kafka_instance_type = "t3.medium"
kafka_cluster_size = 3
```

## 🗄️ 데이터베이스 관리

### RDS PostgreSQL 일일 점검
```bash
#!/bin/bash
# rds-daily-check.sh

ENVIRONMENTS=("dev" "prod")

echo "🗄️ RDS PostgreSQL 일일 점검"
echo "=========================="

for env in "${ENVIRONMENTS[@]}"; do
    echo -e "\n📁 Environment: $env"
    
    DB_IDENTIFIER="goorm-popcorn-$env-postgres"
    
    # RDS 인스턴스 상태
    echo "📊 RDS 인스턴스 상태:"
    aws rds describe-db-instances \
        --db-instance-identifier $DB_IDENTIFIER \
        --query 'DBInstances[0].[DBInstanceStatus,Engine,EngineVersion,DBInstanceClass,MultiAZ]' \
        --output table
    
    # 성능 메트릭
    echo "📈 성능 메트릭 (최근 1시간):"
    END_TIME=$(date -u +"%Y-%m-%dT%H:%M:%S")
    START_TIME=$(date -u -d '1 hour ago' +"%Y-%m-%dT%H:%M:%S")
    
    aws cloudwatch get-metric-statistics \
        --namespace AWS/RDS \
        --metric-name CPUUtilization \
        --dimensions Name=DBInstanceIdentifier,Value=$DB_IDENTIFIER \
        --start-time $START_TIME \
        --end-time $END_TIME \
        --period 3600 \
        --statistics Average \
        --query 'Datapoints[0].Average' \
        --output text | xargs echo "평균 CPU 사용률:"
    
    # 연결 수
    aws cloudwatch get-metric-statistics \
        --namespace AWS/RDS \
        --metric-name DatabaseConnections \
        --dimensions Name=DBInstanceIdentifier,Value=$DB_IDENTIFIER \
        --start-time $START_TIME \
        --end-time $END_TIME \
        --period 3600 \
        --statistics Average \
        --query 'Datapoints[0].Average' \
        --output text | xargs echo "평균 연결 수:"
    
    # 백업 상태
    echo "💾 백업 상태:"
    aws rds describe-db-snapshots \
        --db-instance-identifier $DB_IDENTIFIER \
        --snapshot-type automated \
        --max-items 1 \
        --query 'DBSnapshots[0].[DBSnapshotIdentifier,Status,SnapshotCreateTime]' \
        --output table
done

echo -e "\n✅ RDS 점검 완료"
```

### 데이터베이스 성능 최적화 체크리스트
```yaml
Performance Monitoring:
  - [ ] CPU 사용률 < 70%
  - [ ] 메모리 사용률 < 80%
  - [ ] 연결 수 < max_connections의 80%
  - [ ] 디스크 I/O 대기 시간 < 10ms

Query Optimization:
  - [ ] 슬로우 쿼리 로그 분석
  - [ ] 인덱스 사용률 검토
  - [ ] 쿼리 실행 계획 분석
  - [ ] 통계 정보 업데이트

Configuration Tuning:
  - [ ] shared_buffers 최적화
  - [ ] effective_cache_size 조정
  - [ ] work_mem 설정 검토
  - [ ] checkpoint 설정 최적화

Maintenance:
  - [ ] VACUUM 및 ANALYZE 실행
  - [ ] 인덱스 재구성 (필요시)
  - [ ] 로그 파일 정리
  - [ ] 백업 무결성 검증
```

### ElastiCache 관리
```bash
#!/bin/bash
# elasticache-check.sh

ENVIRONMENTS=("dev" "prod")

echo "🔄 ElastiCache 일일 점검"
echo "======================="

for env in "${ENVIRONMENTS[@]}"; do
    echo -e "\n📁 Environment: $env"
    
    CLUSTER_ID="goorm-popcorn-$env-redis"
    
    # 클러스터 상태
    echo "📊 클러스터 상태:"
    aws elasticache describe-cache-clusters \
        --cache-cluster-id $CLUSTER_ID \
        --show-cache-node-info \
        --query 'CacheClusters[0].[CacheClusterStatus,Engine,EngineVersion,CacheNodeType,NumCacheNodes]' \
        --output table
    
    # 성능 메트릭
    echo "📈 성능 메트릭:"
    END_TIME=$(date -u +"%Y-%m-%dT%H:%M:%S")
    START_TIME=$(date -u -d '1 hour ago' +"%Y-%m-%dT%H:%M:%S")
    
    # CPU 사용률
    aws cloudwatch get-metric-statistics \
        --namespace AWS/ElastiCache \
        --metric-name CPUUtilization \
        --dimensions Name=CacheClusterId,Value=$CLUSTER_ID \
        --start-time $START_TIME \
        --end-time $END_TIME \
        --period 3600 \
        --statistics Average \
        --query 'Datapoints[0].Average' \
        --output text | xargs echo "평균 CPU 사용률:"
    
    # 메모리 사용률
    aws cloudwatch get-metric-statistics \
        --namespace AWS/ElastiCache \
        --metric-name DatabaseMemoryUsagePercentage \
        --dimensions Name=CacheClusterId,Value=$CLUSTER_ID \
        --start-time $START_TIME \
        --end-time $END_TIME \
        --period 3600 \
        --statistics Average \
        --query 'Datapoints[0].Average' \
        --output text | xargs echo "평균 메모리 사용률:"
done

echo -e "\n✅ ElastiCache 점검 완료"
```

## ☁️ AWS 서비스 관리

### VPC 및 네트워킹 점검
```bash
#!/bin/bash
# network-check.sh

ENVIRONMENTS=("dev" "prod")

echo "🌐 네트워크 인프라 점검"
echo "===================="

for env in "${ENVIRONMENTS[@]}"; do
    echo -e "\n📁 Environment: $env"
    
    VPC_NAME="goorm-popcorn-$env-vpc"
    
    # VPC 정보
    echo "🏠 VPC 정보:"
    VPC_ID=$(aws ec2 describe-vpcs \
        --filters "Name=tag:Name,Values=$VPC_NAME" \
        --query 'Vpcs[0].VpcId' \
        --output text)
    
    echo "VPC ID: $VPC_ID"
    
    # 서브넷 상태
    echo "🔗 서브넷 상태:"
    aws ec2 describe-subnets \
        --filters "Name=vpc-id,Values=$VPC_ID" \
        --query 'Subnets[*].[SubnetId,AvailabilityZone,CidrBlock,MapPublicIpOnLaunch]' \
        --output table
    
    # NAT Gateway 상태
    echo "🚪 NAT Gateway 상태:"
    aws ec2 describe-nat-gateways \
        --filter "Name=vpc-id,Values=$VPC_ID" \
        --query 'NatGateways[*].[NatGatewayId,State,SubnetId]' \
        --output table
    
    # 인터넷 게이트웨이
    echo "🌍 인터넷 게이트웨이:"
    aws ec2 describe-internet-gateways \
        --filters "Name=attachment.vpc-id,Values=$VPC_ID" \
        --query 'InternetGateways[*].[InternetGatewayId,State]' \
        --output table
    
    # VPC 엔드포인트
    echo "🔌 VPC 엔드포인트:"
    aws ec2 describe-vpc-endpoints \
        --filters "Name=vpc-id,Values=$VPC_ID" \
        --query 'VpcEndpoints[*].[VpcEndpointId,ServiceName,State]' \
        --output table
done

echo -e "\n✅ 네트워크 점검 완료"
```

### 보안 그룹 관리
```yaml
Security Group Review Checklist:
  Database Security Groups:
    - [ ] RDS 접근은 EKS 노드에서만 허용
    - [ ] ElastiCache 접근은 애플리케이션에서만 허용
    - [ ] 불필요한 포트 차단
    - [ ] 소스 IP 범위 최소화

  EKS Security Groups:
    - [ ] 노드 간 통신 허용
    - [ ] Control Plane 통신 허용
    - [ ] ALB에서 노드로의 트래픽 허용
    - [ ] 외부 API 접근 허용 (필요시)

  ALB Security Groups:
    - [ ] HTTP/HTTPS 트래픽만 허용
    - [ ] CloudFront에서의 접근 허용 (사용시)
    - [ ] 헬스체크 트래픽 허용
    - [ ] 불필요한 포트 차단

  Kafka Security Groups:
    - [ ] 브로커 간 통신 허용 (9092, 9093)
    - [ ] 애플리케이션에서의 접근 허용
    - [ ] JMX 모니터링 포트 (필요시)
    - [ ] SSH 접근 제한 (관리용)
```

## 💰 비용 최적화

### 일일 비용 모니터링
```bash
#!/bin/bash
# cost-monitoring.sh

echo "💰 AWS 비용 모니터링"
echo "=================="

# 어제 비용
YESTERDAY=$(date -d '1 day ago' +%Y-%m-%d)
echo "📅 어제 ($YESTERDAY) 비용:"

aws ce get-cost-and-usage \
    --time-period Start=$YESTERDAY,End=$(date +%Y-%m-%d) \
    --granularity DAILY \
    --metrics BlendedCost \
    --group-by Type=DIMENSION,Key=SERVICE \
    --query 'ResultsByTime[0].Groups[?Metrics.BlendedCost.Amount>`5`].[Keys[0],Metrics.BlendedCost.Amount]' \
    --output table

# 월 누적 비용
MONTH_START=$(date +%Y-%m-01)
echo -e "\n📊 이번 달 누적 비용:"

aws ce get-cost-and-usage \
    --time-period Start=$MONTH_START,End=$(date +%Y-%m-%d) \
    --granularity MONTHLY \
    --metrics BlendedCost \
    --query 'ResultsByTime[0].Total.BlendedCost.Amount' \
    --output text | xargs echo "총 비용: $"

# 서비스별 비용 (상위 10개)
echo -e "\n🏆 서비스별 비용 (상위 10개):"
aws ce get-cost-and-usage \
    --time-period Start=$MONTH_START,End=$(date +%Y-%m-%d) \
    --granularity MONTHLY \
    --metrics BlendedCost \
    --group-by Type=DIMENSION,Key=SERVICE \
    --query 'ResultsByTime[0].Groups[:10].[Keys[0],Metrics.BlendedCost.Amount]' \
    --output table

echo -e "\n✅ 비용 모니터링 완료"
```

### 비용 최적화 체크리스트
```yaml
Compute Optimization:
  - [ ] EC2 인스턴스 사용률 분석 (>70% 목표)
  - [ ] Spot Instance 활용 검토
  - [ ] Reserved Instance 구매 계획
  - [ ] 미사용 EBS 볼륨 정리

Database Optimization:
  - [ ] RDS 인스턴스 크기 최적화
  - [ ] 백업 보존 기간 검토
  - [ ] 스냅샷 정리 자동화
  - [ ] Read Replica 필요성 검토

Storage Optimization:
  - [ ] S3 스토리지 클래스 최적화
  - [ ] 라이프사이클 정책 설정
  - [ ] 중복 데이터 제거
  - [ ] 압축 및 아카이빙

Network Optimization:
  - [ ] NAT Gateway vs VPC Endpoint 비용 비교
  - [ ] CloudFront 캐싱 최적화
  - [ ] 데이터 전송 비용 분석
  - [ ] 리전 간 트래픽 최소화
```

## 🔧 자동화 스크립트

### 백업 자동화
```bash
#!/bin/bash
# automated-backup.sh

ENVIRONMENTS=("dev" "prod")
RETENTION_DAYS=7

echo "💾 자동 백업 실행"
echo "==============="

for env in "${ENVIRONMENTS[@]}"; do
    echo -e "\n📁 Environment: $env"
    
    # RDS 스냅샷
    DB_IDENTIFIER="goorm-popcorn-$env-postgres"
    SNAPSHOT_ID="$DB_IDENTIFIER-$(date +%Y%m%d-%H%M%S)"
    
    echo "📸 RDS 스냅샷 생성: $SNAPSHOT_ID"
    aws rds create-db-snapshot \
        --db-instance-identifier $DB_IDENTIFIER \
        --db-snapshot-identifier $SNAPSHOT_ID
    
    # 오래된 스냅샷 삭제
    echo "🗑️ 오래된 스냅샷 정리"
    CUTOFF_DATE=$(date -d "$RETENTION_DAYS days ago" +%Y-%m-%d)
    
    aws rds describe-db-snapshots \
        --db-instance-identifier $DB_IDENTIFIER \
        --snapshot-type manual \
        --query "DBSnapshots[?SnapshotCreateTime<'$CUTOFF_DATE'].DBSnapshotIdentifier" \
        --output text | while read snapshot; do
        if [ -n "$snapshot" ]; then
            echo "삭제: $snapshot"
            aws rds delete-db-snapshot --db-snapshot-identifier $snapshot
        fi
    done
    
    # EBS 스냅샷 (Kafka 볼륨)
    echo "💿 EBS 스냅샷 생성"
    aws ec2 describe-instances \
        --filters "Name=tag:Environment,Values=$env" "Name=tag:Service,Values=kafka" \
        --query 'Reservations[].Instances[].BlockDeviceMappings[].Ebs.VolumeId' \
        --output text | while read volume_id; do
        if [ -n "$volume_id" ]; then
            SNAPSHOT_DESC="kafka-$env-$(date +%Y%m%d-%H%M%S)"
            aws ec2 create-snapshot \
                --volume-id $volume_id \
                --description "$SNAPSHOT_DESC"
        fi
    done
done

echo -e "\n✅ 백업 완료"
```

### 리소스 정리 자동화
```bash
#!/bin/bash
# resource-cleanup.sh

echo "🧹 리소스 정리 자동화"
echo "=================="

# 미사용 EBS 볼륨
echo "💿 미사용 EBS 볼륨 확인:"
aws ec2 describe-volumes \
    --filters "Name=status,Values=available" \
    --query 'Volumes[*].[VolumeId,Size,CreateTime]' \
    --output table

# 미사용 Elastic IP
echo -e "\n🌐 미사용 Elastic IP 확인:"
aws ec2 describe-addresses \
    --query 'Addresses[?!InstanceId].[PublicIp,AllocationId]' \
    --output table

# 오래된 AMI
echo -e "\n📀 오래된 AMI 확인 (90일 이상):"
CUTOFF_DATE=$(date -d '90 days ago' +%Y-%m-%d)
aws ec2 describe-images \
    --owners self \
    --query "Images[?CreationDate<'$CUTOFF_DATE'].[ImageId,Name,CreationDate]" \
    --output table

# 미사용 보안 그룹
echo -e "\n🔒 미사용 보안 그룹 확인:"
aws ec2 describe-security-groups \
    --query 'SecurityGroups[?GroupName!=`default`].[GroupId,GroupName]' \
    --output text | while read sg_id sg_name; do
    
    # 사용 중인지 확인
    USAGE=$(aws ec2 describe-network-interfaces \
        --filters "Name=group-id,Values=$sg_id" \
        --query 'NetworkInterfaces' \
        --output text)
    
    if [ -z "$USAGE" ]; then
        echo "미사용: $sg_id ($sg_name)"
    fi
done

echo -e "\n✅ 리소스 정리 확인 완료"
```

## 📊 모니터링 및 알림

### CloudWatch 알림 설정
```bash
#!/bin/bash
# setup-cloudwatch-alarms.sh

ENVIRONMENTS=("dev" "prod")
SNS_TOPIC_ARN="arn:aws:sns:ap-northeast-2:123456789012:infrastructure-alerts"

echo "🚨 CloudWatch 알림 설정"
echo "====================="

for env in "${ENVIRONMENTS[@]}"; do
    echo -e "\n📁 Environment: $env"
    
    # RDS CPU 알림
    aws cloudwatch put-metric-alarm \
        --alarm-name "RDS-CPU-High-$env" \
        --alarm-description "RDS CPU utilization is high" \
        --metric-name CPUUtilization \
        --namespace AWS/RDS \
        --statistic Average \
        --period 300 \
        --threshold 80 \
        --comparison-operator GreaterThanThreshold \
        --evaluation-periods 2 \
        --alarm-actions $SNS_TOPIC_ARN \
        --dimensions Name=DBInstanceIdentifier,Value=goorm-popcorn-$env-postgres
    
    # ElastiCache 메모리 알림
    aws cloudwatch put-metric-alarm \
        --alarm-name "ElastiCache-Memory-High-$env" \
        --alarm-description "ElastiCache memory utilization is high" \
        --metric-name DatabaseMemoryUsagePercentage \
        --namespace AWS/ElastiCache \
        --statistic Average \
        --period 300 \
        --threshold 85 \
        --comparison-operator GreaterThanThreshold \
        --evaluation-periods 2 \
        --alarm-actions $SNS_TOPIC_ARN \
        --dimensions Name=CacheClusterId,Value=goorm-popcorn-$env-redis
    
    # EKS 노드 CPU 알림
    aws cloudwatch put-metric-alarm \
        --alarm-name "EKS-Node-CPU-High-$env" \
        --alarm-description "EKS node CPU utilization is high" \
        --metric-name CPUUtilization \
        --namespace AWS/EC2 \
        --statistic Average \
        --period 300 \
        --threshold 80 \
        --comparison-operator GreaterThanThreshold \
        --evaluation-periods 3 \
        --alarm-actions $SNS_TOPIC_ARN
done

echo -e "\n✅ CloudWatch 알림 설정 완료"
```

## 📚 문서화 및 다이어그램

### 인프라 다이어그램 업데이트 체크리스트
```yaml
Architecture Diagrams:
  - [ ] 전체 시스템 아키텍처 다이어그램
  - [ ] 네트워크 토폴로지 다이어그램
  - [ ] 데이터 플로우 다이어그램
  - [ ] 보안 아키텍처 다이어그램

Documentation:
  - [ ] Terraform 모듈 README 업데이트
  - [ ] 환경별 설정 문서화
  - [ ] 운영 절차 문서화
  - [ ] 장애 대응 가이드 업데이트

Version Control:
  - [ ] 다이어그램 버전 관리
  - [ ] 변경 이력 추적
  - [ ] 승인 프로세스 문서화
  - [ ] 정기 검토 일정 수립
```

## 🎯 성공을 위한 팁

1. **Infrastructure as Code**: 모든 인프라를 코드로 관리하여 일관성 확보
2. **모니터링 우선**: 문제를 사전에 감지할 수 있는 모니터링 구축
3. **자동화 투자**: 반복적인 작업은 스크립트로 자동화하여 효율성 증대
4. **비용 의식**: 정기적인 비용 검토를 통한 최적화 기회 발굴
5. **보안 강화**: 최소 권한 원칙과 다층 보안 적용
6. **문서화 습관**: 모든 변경사항과 설정을 문서화하여 지식 공유
7. **백업 전략**: 정기적인 백업과 복구 테스트로 데이터 보호

Infrastructure Engineer로서 안정적이고 확장 가능한 AWS 인프라를 구축하고 운영하는 것이 핵심 목표입니다. 이 가이드를 참고하여 효과적으로 Popcorn MSA 인프라를 관리하시기 바랍니다.