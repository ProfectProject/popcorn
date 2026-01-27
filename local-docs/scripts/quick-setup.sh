#!/bin/bash

# Quick Setup Script for Secrets Management Migration
# This script provides a guided setup for the secrets migration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 구름 팝콘 시크릿 관리 빠른 설정${NC}"
echo "=============================================="

# 올바른 디렉토리에 있는지 확인
if [[ ! -f ".github/scripts/migrate-task-definition-secrets.sh" ]]; then
    echo -e "${RED}❌ popcorn_msa 루트 디렉토리에서 이 스크립트를 실행해주세요${NC}"
    exit 1
fi

echo -e "${YELLOW}이 스크립트는 구름 팝콘 프로젝트의 시크릿 관리 설정을 도와드립니다.${NC}"
echo ""

# 1단계: 사전 요구사항 확인
echo -e "${BLUE}📋 1단계: 사전 요구사항 확인${NC}"
echo "================================="

# AWS CLI 확인
if command -v aws &> /dev/null; then
    echo -e "${GREEN}✅ AWS CLI가 설치되어 있습니다${NC}"
    
    # AWS 자격증명 확인
    if aws sts get-caller-identity &> /dev/null; then
        ACCOUNT_ID=$(aws sts get-caller-identity --query 'Account' --output text)
        echo -e "${GREEN}✅ AWS 자격증명이 설정되어 있습니다 (계정: ${ACCOUNT_ID})${NC}"
    else
        echo -e "${RED}❌ AWS 자격증명이 설정되지 않았습니다${NC}"
        echo "다음 명령어를 실행해주세요: aws configure"
        exit 1
    fi
else
    echo -e "${RED}❌ AWS CLI가 설치되지 않았습니다${NC}"
    echo "AWS CLI를 설치해주세요: https://aws.amazon.com/cli/"
    exit 1
fi

# GitHub CLI 확인
if command -v gh &> /dev/null; then
    echo -e "${GREEN}✅ GitHub CLI가 설치되어 있습니다${NC}"
else
    echo -e "${YELLOW}⚠️  GitHub CLI를 찾을 수 없습니다 (GitHub Secrets 설정에 선택사항)${NC}"
fi

# jq 확인
if command -v jq &> /dev/null; then
    echo -e "${GREEN}✅ jq가 설치되어 있습니다${NC}"
else
    echo -e "${RED}❌ jq가 설치되지 않았습니다${NC}"
    echo "jq를 설치해주세요: https://stedolan.github.io/jq/"
    exit 1
fi

echo ""

# 2단계: 현재 상태 분석
echo -e "${BLUE}🔍 2단계: 현재 상태 분석${NC}"
echo "==================================="

echo "태스크 정의에서 하드코딩된 시크릿을 확인 중..."

# JWT 시크릿 확인
if grep -r "JWT_SECRET_KEY.*goorm-popcorn-jwt-secret-key" .aws/task-definitions/ &>/dev/null; then
    echo -e "${RED}🔴 치명적: 하드코딩된 JWT 시크릿 발견${NC}"
    JWT_HARDCODED=true
else
    echo -e "${GREEN}✅ 하드코딩된 JWT 시크릿 없음${NC}"
    JWT_HARDCODED=false
fi

# 데이터베이스 정보 확인
if grep -r "SPRING_DATASOURCE_USERNAME.*postgres" .aws/task-definitions/ &>/dev/null; then
    echo -e "${YELLOW}🟡 데이터베이스 사용자명이 하드코딩됨${NC}"
    DB_HARDCODED=true
else
    echo -e "${GREEN}✅ 데이터베이스 자격증명이 적절히 관리됨${NC}"
    DB_HARDCODED=false
fi

# 기존 시크릿 확인
echo "기존 AWS 시크릿 확인 중..."
EXISTING_SECRETS=$(aws secretsmanager list-secrets --query 'SecretList[?contains(Name, `goorm-popcorn`)].Name' --output text 2>/dev/null || echo "")

if [[ -n "$EXISTING_SECRETS" ]]; then
    echo -e "${GREEN}✅ 기존 시크릿 발견:${NC}"
    echo "$EXISTING_SECRETS" | tr '\t' '\n' | sed 's/^/  - /'
else
    echo -e "${YELLOW}⚠️  기존 시크릿을 찾을 수 없습니다${NC}"
fi

echo ""

# 3단계: 마이그레이션 옵션
echo -e "${BLUE}🛠️  3단계: 마이그레이션 옵션${NC}"
echo "=============================="

if [[ "$JWT_HARDCODED" == true ]] || [[ "$DB_HARDCODED" == true ]]; then
    echo -e "${RED}🚨 보안 문제 감지${NC}"
    echo "태스크 정의에 즉시 주의가 필요한 하드코딩된 시크릿이 포함되어 있습니다."
    echo ""
    echo "사용 가능한 옵션:"
    echo "1. 🚀 전체 마이그레이션 (권장) - 모든 시크릿을 AWS Secrets Manager로 마이그레이션"
    echo "2. 🔧 수동 설정 - 단계별 가이드 설정"
    echo "3. 📋 분석만 - 변경 없이 보고서 생성"
    echo "4. ❌ 종료 - 수동으로 처리하겠습니다"
    echo ""
    
    read -p "옵션을 선택하세요 (1-4): " -n 1 -r
    echo
    
    case $REPLY in
        1)
            echo -e "${GREEN}🚀 전체 마이그레이션 시작...${NC}"
            MIGRATION_MODE="full"
            ;;
        2)
            echo -e "${YELLOW}🔧 수동 설정 시작...${NC}"
            MIGRATION_MODE="manual"
            ;;
        3)
            echo -e "${BLUE}📋 분석 보고서 생성...${NC}"
            MIGRATION_MODE="analysis"
            ;;
        4)
            echo -e "${BLUE}👋 종료합니다. 수동 설정 화이팅!${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}잘못된 옵션입니다. 종료합니다.${NC}"
            exit 1
            ;;
    esac
else
    echo -e "${GREEN}✅ 중요한 보안 문제가 발견되지 않았습니다${NC}"
    echo "시크릿이 적절히 관리되고 있는 것 같습니다."
    echo ""
    echo "사용 가능한 옵션:"
    echo "1. 🔍 보안 스캔 - 포괄적인 보안 스캔 실행"
    echo "2. 📊 상태 보고서 - 상세한 상태 보고서 생성"
    echo "3. 🔧 추가 보안 강화 - 추가 보안 기능 추가"
    echo "4. ❌ 종료 - 모든 것이 좋아 보입니다"
    echo ""
    
    read -p "옵션을 선택하세요 (1-4): " -n 1 -r
    echo
    
    case $REPLY in
        1)
            echo -e "${BLUE}🔍 보안 스캔 실행...${NC}"
            MIGRATION_MODE="scan"
            ;;
        2)
            echo -e "${BLUE}📊 상태 보고서 생성...${NC}"
            MIGRATION_MODE="report"
            ;;
        3)
            echo -e "${YELLOW}🔧 추가 보안 강화 시작...${NC}"
            MIGRATION_MODE="harden"
            ;;
        4)
            echo -e "${GREEN}👍 좋습니다! 시크릿 관리가 잘 되어 있네요.${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}잘못된 옵션입니다. 종료합니다.${NC}"
            exit 1
            ;;
    esac
fi

echo ""

# Step 4: Execute Based on Mode
echo -e "${BLUE}⚡ Step 4: Executing ${MIGRATION_MODE^} Mode${NC}"
echo "================================="

case $MIGRATION_MODE in
    "full")
        echo -e "${YELLOW}🚨 WARNING: This will modify your task definitions and create AWS secrets.${NC}"
        echo "Make sure you have:"
        echo "- Proper AWS permissions for Secrets Manager"
        echo "- Ability to redeploy ECS services"
        echo "- Backup of current configurations"
        echo ""
        read -p "Continue with full migration? (y/N): " -n 1 -r
        echo
        
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo -e "${GREEN}🚀 전체 마이그레이션 시작...${NC}"
            chmod +x .github/scripts/migrate-task-definition-secrets.sh
            ./.github/scripts/migrate-task-definition-secrets.sh
        else
            echo "마이그레이션이 취소되었습니다."
            exit 0
        fi
        ;;
        
    "manual")
        echo -e "${BLUE}📋 수동 설정 가이드${NC}"
        echo "===================="
        echo ""
        echo "1. AWS 시크릿 생성:"
        echo "   aws secretsmanager create-secret --name 'goorm-popcorn-dev/jwt-secret' --secret-string '{\"secret_key\":\"your-secure-key\"}'"
        echo ""
        echo "2. 태스크 정의 업데이트:"
        echo "   하드코딩된 값을 시크릿 참조로 교체"
        echo ""
        echo "3. 업데이트된 서비스 배포:"
        echo "   배포 파이프라인을 사용하여 변경사항 적용"
        echo ""
        echo "자세한 지침은 다음을 참조하세요: .github/TASK_DEFINITION_SECRETS.md"
        ;;
        
    "analysis")
        echo -e "${BLUE}📊 보안 분석 보고서${NC}"
        echo "=========================="
        echo ""
        echo "현재 상태:"
        echo "- JWT 시크릿: $([ "$JWT_HARDCODED" == true ] && echo "🔴 하드코딩됨" || echo "✅ 안전함")"
        echo "- 데이터베이스 정보: $([ "$DB_HARDCODED" == true ] && echo "🟡 부분적으로 노출됨" || echo "✅ 안전함")"
        echo "- AWS 시크릿: $([ -n "$EXISTING_SECRETS" ] && echo "✅ 설정됨" || echo "⚠️  찾을 수 없음")"
        echo ""
        echo "권장사항:"
        if [[ "$JWT_HARDCODED" == true ]]; then
            echo "- 🔴 치명적: JWT 시크릿을 즉시 AWS Secrets Manager로 이동"
        fi
        if [[ "$DB_HARDCODED" == true ]]; then
            echo "- 🟡 중간: 데이터베이스 연결 정보를 시크릿으로 이동"
        fi
        echo "- 🔍 정기적인 보안 스캔 실행: gh workflow run secret-scan.yml"
        ;;
        
    "scan")
        echo -e "${BLUE}🔍 보안 스캔 실행${NC}"
        echo "======================="
        
        if command -v gh &> /dev/null; then
            echo "GitHub Actions 보안 스캔 트리거 중..."
            gh workflow run secret-scan.yml || echo "⚠️  워크플로우를 트리거할 수 없습니다. GitHub Actions에서 수동으로 실행하세요."
        else
            echo "GitHub CLI를 사용할 수 없습니다. 보안 스캔을 수동으로 실행해주세요:"
            echo "1. 저장소의 GitHub Actions로 이동"
            echo "2. 'Secret Scan' 워크플로우 실행"
            echo "3. 결과 검토"
        fi
        ;;
        
    "report")
        echo -e "${BLUE}📊 상세 상태 보고서${NC}"
        echo "========================="
        echo ""
        echo "🔐 시크릿 관리 상태:"
        echo "- 설정 파일: $(find .github -name "*.md" | grep -i secret | wc -l)개 문서 파일"
        echo "- 마이그레이션 스크립트: $(find .github/scripts -name "*secret*" | wc -l)개 자동화 스크립트"
        echo "- 보안 워크플로우: $(find .github/workflows -name "*secret*" | wc -l)개 스캔 워크플로우"
        echo "- 태스크 정의: $(find .aws/task-definitions -name "*.json" | wc -l)개 전체 파일"
        echo ""
        echo "📋 다음 단계:"
        echo "1. .github/SECRETS_MANAGEMENT.md 문서 검토"
        echo "2. GitHub 저장소 시크릿 설정"
        echo "3. AWS Secrets Manager 설정"
        echo "4. 정기적인 보안 스캔 실행"
        ;;
        
    "harden")
        echo -e "${BLUE}🔧 추가 보안 강화${NC}"
        echo "================================"
        echo ""
        echo "사용 가능한 보안 강화 옵션:"
        echo "1. 시크릿 로테이션 설정"
        echo "2. 서비스 간 인증 추가"
        echo "3. 보안 모니터링 설정"
        echo "4. 컴플라이언스 스캔 구현"
        echo ""
        echo "상세한 보안 강화 단계는 다음을 참조하세요: .github/TASK_DEFINITION_SECRETS.md"
        ;;
esac

echo ""
echo -e "${GREEN}✅ 설정이 완료되었습니다!${NC}"
echo ""
echo "📚 추가 리소스:"
echo "- 구현 계획: .github/IMPLEMENTATION_PLAN.md"
echo "- 시크릿 관리 가이드: .github/SECRETS_MANAGEMENT.md"
echo "- 태스크 정의 가이드: .github/TASK_DEFINITION_SECRETS.md"
echo "- 보안 스캔: .github/workflows/secret-scan.yml"
echo ""
echo -e "${BLUE}🎉 보안 수준 향상에 감사드립니다!${NC}"