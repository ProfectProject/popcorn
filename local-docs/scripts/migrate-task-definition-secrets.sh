#!/bin/bash

# Task Definition Secrets Migration Script
# This script migrates hardcoded values in Task Definitions to AWS Secrets Manager

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
AWS_REGION="ap-northeast-2"
AWS_ACCOUNT_ID="375896310755"
ENVIRONMENTS=("dev" "prod")

echo -e "${BLUE}🔐 태스크 정의 시크릿 마이그레이션${NC}"
echo "====================================="

# 사전 요구사항 확인
check_prerequisites() {
    echo -e "${BLUE}📋 사전 요구사항 확인 중...${NC}"
    
    # AWS CLI 확인
    if ! command -v aws &> /dev/null; then
        echo -e "${RED}❌ AWS CLI가 설치되지 않았습니다${NC}"
        echo "AWS CLI를 설치해주세요: https://aws.amazon.com/cli/"
        exit 1
    fi
    
    # AWS 자격증명 확인
    if ! aws sts get-caller-identity &> /dev/null; then
        echo -e "${RED}❌ AWS 자격증명이 설정되지 않았습니다${NC}"
        echo "다음 명령어를 실행해주세요: aws configure"
        exit 1
    fi
    
    # jq 확인
    if ! command -v jq &> /dev/null; then
        echo -e "${RED}❌ jq가 설치되지 않았습니다${NC}"
        echo "jq를 설치해주세요: https://stedolan.github.io/jq/"
        exit 1
    fi
    
    echo -e "${GREEN}✅ 모든 사전 요구사항이 충족되었습니다${NC}"
}

# 향상된 데이터베이스 자격증명 생성
create_enhanced_db_credentials() {
    local environment=$1
    echo -e "${YELLOW}${environment} 환경의 향상된 데이터베이스 자격증명 생성 중...${NC}"
    
    SECRET_NAME="goorm-popcorn-${environment}/database/credentials"
    
    # 기존 시크릿이 있는지 확인
    if aws secretsmanager describe-secret --secret-id "$SECRET_NAME" --region "$AWS_REGION" &>/dev/null; then
        echo -e "${YELLOW}⚠️  데이터베이스 자격증명이 이미 존재합니다. 업데이트 중...${NC}"
        
        # 현재 비밀번호 가져오기
        CURRENT_SECRET=$(aws secretsmanager get-secret-value --secret-id "$SECRET_NAME" --region "$AWS_REGION" --query 'SecretString' --output text)
        CURRENT_PASSWORD=$(echo "$CURRENT_SECRET" | jq -r '.password // empty')
        
        if [[ -z "$CURRENT_PASSWORD" ]]; then
            echo "${environment} 환경의 PostgreSQL 데이터베이스 비밀번호를 입력하세요:"
            read -s DB_PASSWORD
        else
            DB_PASSWORD="$CURRENT_PASSWORD"
            echo -e "${GREEN}✅ 기존 비밀번호 사용${NC}"
        fi
    else
        echo "${environment} 환경의 PostgreSQL 데이터베이스 비밀번호를 입력하세요:"
        read -s DB_PASSWORD
    fi
    
    # 데이터베이스 연결 세부정보 입력 요청
    echo "${environment} 환경의 데이터베이스 호스트를 입력하세요 (예: db-host.amazonaws.com):"
    read DB_HOST
    
    echo "${environment} 환경의 데이터베이스 포트를 입력하세요 (기본값: 5432):"
    read DB_PORT
    DB_PORT=${DB_PORT:-5432}
    
    echo "${environment} 환경의 데이터베이스 이름을 입력하세요 (기본값: goorm_popcorn_${environment}):"
    read DB_NAME
    DB_NAME=${DB_NAME:-"goorm_popcorn_${environment}"}
    
    echo "${environment} 환경의 데이터베이스 사용자명을 입력하세요 (기본값: postgres):"
    read DB_USERNAME
    DB_USERNAME=${DB_USERNAME:-"postgres"}
    
    # Create enhanced secret
    ENHANCED_SECRET_VALUE=$(cat <<EOF
{
    "password": "$DB_PASSWORD",
    "username": "$DB_USERNAME",
    "host": "$DB_HOST",
    "port": "$DB_PORT",
    "dbname": "$DB_NAME",
    "url": "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
}
EOF
)
    
    if aws secretsmanager describe-secret --secret-id "$SECRET_NAME" --region "$AWS_REGION" &>/dev/null; then
        aws secretsmanager update-secret \
            --secret-id "$SECRET_NAME" \
            --secret-string "$ENHANCED_SECRET_VALUE" \
            --region "$AWS_REGION"
        echo -e "${GREEN}✅ ${environment} 환경의 향상된 데이터베이스 자격증명이 업데이트되었습니다${NC}"
    else
        aws secretsmanager create-secret \
            --name "$SECRET_NAME" \
            --description "${environment} 환경의 PostgreSQL 데이터베이스 자격증명" \
            --secret-string "$ENHANCED_SECRET_VALUE" \
            --region "$AWS_REGION"
        echo -e "${GREEN}✅ ${environment} 환경의 향상된 데이터베이스 자격증명이 생성되었습니다${NC}"
    fi
}

# JWT 시크릿 생성
create_jwt_secret() {
    local environment=$1
    echo -e "${YELLOW}${environment} 환경의 JWT 시크릿 생성 중...${NC}"
    
    SECRET_NAME="goorm-popcorn-${environment}/jwt-secret"
    
    if aws secretsmanager describe-secret --secret-id "$SECRET_NAME" --region "$AWS_REGION" &>/dev/null; then
        echo -e "${YELLOW}⚠️  ${environment} 환경의 JWT 시크릿이 이미 존재합니다${NC}"
        return
    fi
    
    # 보안 JWT 시크릿 생성 (256-bit)
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
    
    JWT_SECRET_VALUE=$(cat <<EOF
{
    "secret_key": "$JWT_SECRET"
}
EOF
)
    
    aws secretsmanager create-secret \
        --name "$SECRET_NAME" \
        --description "${environment} 환경의 JWT 서명 시크릿" \
        --secret-string "$JWT_SECRET_VALUE" \
        --region "$AWS_REGION"
    
    echo -e "${GREEN}✅ ${environment} 환경의 JWT 시크릿이 생성되었습니다${NC}"
}

# Create Redis auth secret
create_redis_auth_secret() {
    local environment=$1
    echo -e "${YELLOW}Creating Redis auth secret for ${environment}...${NC}"
    
    SECRET_NAME="goorm-popcorn-${environment}/redis/auth"
    
    if aws secretsmanager describe-secret --secret-id "$SECRET_NAME" --region "$AWS_REGION" &>/dev/null; then
        echo -e "${YELLOW}⚠️  Redis auth secret already exists for ${environment}${NC}"
        return
    fi
    
    echo "Enter Redis AUTH token for ${environment} (or press Enter to generate):"
    read -s REDIS_TOKEN
    
    if [[ -z "$REDIS_TOKEN" ]]; then
        # Generate a secure Redis token
        REDIS_TOKEN=$(openssl rand -base64 32 | tr -d '\n')
        echo -e "${GREEN}✅ Generated Redis token${NC}"
    fi
    
    REDIS_SECRET_VALUE=$(cat <<EOF
{
    "token": "$REDIS_TOKEN"
}
EOF
)
    
    aws secretsmanager create-secret \
        --name "$SECRET_NAME" \
        --description "Redis authentication token for ${environment}" \
        --secret-string "$REDIS_SECRET_VALUE" \
        --region "$AWS_REGION"
    
    echo -e "${GREEN}✅ Redis auth secret created for ${environment}${NC}"
}

# Create internal API keys
create_internal_api_keys() {
    local environment=$1
    echo -e "${YELLOW}Creating internal API keys for ${environment}...${NC}"
    
    SECRET_NAME="goorm-popcorn-${environment}/internal-api/keys"
    
    if aws secretsmanager describe-secret --secret-id "$SECRET_NAME" --region "$AWS_REGION" &>/dev/null; then
        echo -e "${YELLOW}⚠️  Internal API keys already exist for ${environment}${NC}"
        return
    fi
    
    # Generate API keys for each service
    USER_SERVICE_KEY=$(openssl rand -hex 32)
    STORE_SERVICE_KEY=$(openssl rand -hex 32)
    ORDER_SERVICE_KEY=$(openssl rand -hex 32)
    PAYMENT_SERVICE_KEY=$(openssl rand -hex 32)
    QR_SERVICE_KEY=$(openssl rand -hex 32)
    
    API_KEYS_VALUE=$(cat <<EOF
{
    "user_service": "$USER_SERVICE_KEY",
    "store_service": "$STORE_SERVICE_KEY",
    "order_service": "$ORDER_SERVICE_KEY",
    "payment_service": "$PAYMENT_SERVICE_KEY",
    "qr_service": "$QR_SERVICE_KEY"
}
EOF
)
    
    aws secretsmanager create-secret \
        --name "$SECRET_NAME" \
        --description "Internal API keys for service-to-service communication in ${environment}" \
        --secret-string "$API_KEYS_VALUE" \
        --region "$AWS_REGION"
    
    echo -e "${GREEN}✅ Internal API keys created for ${environment}${NC}"
}

# Backup current task definitions
backup_task_definitions() {
    echo -e "${YELLOW}Creating backup of current task definitions...${NC}"
    
    BACKUP_DIR=".aws/task-definitions/backup-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$BACKUP_DIR"
    
    cp .aws/task-definitions/*.json "$BACKUP_DIR/"
    
    echo -e "${GREEN}✅ Task definitions backed up to ${BACKUP_DIR}${NC}"
}

# Update task definition with improved secrets
update_task_definition() {
    local service=$1
    local original_file=".aws/task-definitions/${service}.json"
    local improved_file=".aws/task-definitions/${service}-improved.json"
    
    if [[ -f "$improved_file" ]]; then
        echo -e "${YELLOW}Updating ${service} task definition...${NC}"
        
        # Replace original with improved version
        cp "$improved_file" "$original_file"
        
        echo -e "${GREEN}✅ ${service} task definition updated${NC}"
    else
        echo -e "${YELLOW}⚠️  No improved version found for ${service}${NC}"
    fi
}

# Validate secrets access
validate_secrets_access() {
    local environment=$1
    echo -e "${YELLOW}Validating secrets access for ${environment}...${NC}"
    
    SECRETS=(
        "goorm-popcorn-${environment}/database/credentials"
        "goorm-popcorn-${environment}/jwt-secret"
        "goorm-popcorn-${environment}/redis/auth"
        "goorm-popcorn-${environment}/internal-api/keys"
    )
    
    for secret in "${SECRETS[@]}"; do
        if aws secretsmanager describe-secret --secret-id "$secret" --region "$AWS_REGION" &>/dev/null; then
            echo -e "${GREEN}✅ ${secret}${NC}"
        else
            echo -e "${RED}❌ ${secret}${NC}"
        fi
    done
}

# Generate migration report
generate_migration_report() {
    echo -e "${BLUE}📋 Migration Report${NC}"
    echo "==================="
    
    cat <<EOF

🔐 Secrets Created:
   - Enhanced database credentials (URL, username, password)
   - JWT signing secrets (environment-specific)
   - Redis authentication tokens
   - Internal API keys for service communication

📁 Task Definitions Updated:
   - user-service: JWT secret moved to Secrets Manager
   - backend-service: JWT secret moved to Secrets Manager
   - All DB services: Enhanced database credentials

🔒 Security Improvements:
   - Removed hardcoded JWT secrets
   - Protected database connection information
   - Added Redis authentication
   - Implemented service-to-service API keys

📚 Next Steps:
   1. Test updated task definitions in development
   2. Deploy to development environment
   3. Validate application functionality
   4. Deploy to production environment
   5. Remove backup files after successful deployment

🔗 Documentation:
   - See .github/TASK_DEFINITION_SECRETS.md for detailed information
   - See .github/SECRETS_MANAGEMENT.md for ongoing management

EOF
}

# 메인 실행
main() {
    echo "이 스크립트는 태스크 정의의 하드코딩된 시크릿을 AWS Secrets Manager로 마이그레이션합니다."
    echo "JWT 시크릿, 데이터베이스 자격증명 및 기타 민감한 정보가 포함됩니다."
    echo ""
    read -p "계속하시겠습니까? (y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "마이그레이션이 취소되었습니다."
        exit 0
    fi
    
    check_prerequisites
    backup_task_definitions
    
    # 각 환경에 대한 시크릿 생성
    for env in "${ENVIRONMENTS[@]}"; do
        echo -e "${BLUE}🔧 ${env} 환경의 시크릿 설정 중...${NC}"
        create_enhanced_db_credentials "$env"
        create_jwt_secret "$env"
        create_redis_auth_secret "$env"
        create_internal_api_keys "$env"
        validate_secrets_access "$env"
        echo ""
    done
    
    # 태스크 정의 업데이트
    echo -e "${BLUE}📝 태스크 정의 업데이트 중...${NC}"
    SERVICES=("user-service" "backend-service")
    
    for service in "${SERVICES[@]}"; do
        update_task_definition "$service"
    done
    
    generate_migration_report
    
    echo -e "${GREEN}🎉 태스크 정의 시크릿 마이그레이션이 성공적으로 완료되었습니다!${NC}"
    echo -e "${YELLOW}⚠️  프로덕션에 배포하기 전에 개발 환경에서 테스트하는 것을 잊지 마세요${NC}"
}

# Run main function
main "$@"