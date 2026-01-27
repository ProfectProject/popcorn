#!/bin/bash

# Goorm Popcorn Secrets Setup Script
# This script helps set up GitHub Secrets and AWS Secrets Manager

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GITHUB_REPO="your-org/popcorn_msa"
AWS_REGION="ap-northeast-2"
AWS_ACCOUNT_ID="375896310755"

echo -e "${BLUE}🔐 Goorm Popcorn Secrets Setup${NC}"
echo "=================================="

# Check prerequisites
check_prerequisites() {
    echo -e "${BLUE}📋 Checking prerequisites...${NC}"
    
    # Check GitHub CLI
    if ! command -v gh &> /dev/null; then
        echo -e "${RED}❌ GitHub CLI (gh) is not installed${NC}"
        echo "Please install GitHub CLI: https://cli.github.com/"
        exit 1
    fi
    
    # Check AWS CLI
    if ! command -v aws &> /dev/null; then
        echo -e "${RED}❌ AWS CLI is not installed${NC}"
        echo "Please install AWS CLI: https://aws.amazon.com/cli/"
        exit 1
    fi
    
    # Check if logged in to GitHub
    if ! gh auth status &> /dev/null; then
        echo -e "${RED}❌ Not logged in to GitHub CLI${NC}"
        echo "Please run: gh auth login"
        exit 1
    fi
    
    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        echo -e "${RED}❌ AWS credentials not configured${NC}"
        echo "Please run: aws configure"
        exit 1
    fi
    
    echo -e "${GREEN}✅ All prerequisites met${NC}"
}

# Setup GitHub Secrets
setup_github_secrets() {
    echo -e "${BLUE}🔧 Setting up GitHub Secrets...${NC}"
    
    # Required secrets
    declare -A GITHUB_SECRETS=(
        ["AWS_ROLE_ARN"]="arn:aws:iam::${AWS_ACCOUNT_ID}:role/github-actions-role"
        ["PROD_APPROVERS"]="tech-lead-team,security-team"
    )
    
    # Optional secrets (will prompt for values)
    declare -A OPTIONAL_SECRETS=(
        ["DISCORD_WEBHOOK"]="Discord webhook URL"
        ["SNYK_TOKEN"]="Snyk API token"
        ["SONAR_TOKEN"]="SonarQube token"
        ["SONAR_HOST_URL"]="SonarQube host URL (e.g., https://sonarcloud.io)"
        ["CODECOV_TOKEN"]="Codecov token (optional)"
    )
    
    # Set required secrets
    for secret_name in "${!GITHUB_SECRETS[@]}"; do
        secret_value="${GITHUB_SECRETS[$secret_name]}"
        echo -e "${YELLOW}Setting ${secret_name}...${NC}"
        
        if gh secret set "$secret_name" --body "$secret_value" --repo "$GITHUB_REPO"; then
            echo -e "${GREEN}✅ ${secret_name} set successfully${NC}"
        else
            echo -e "${RED}❌ Failed to set ${secret_name}${NC}"
        fi
    done
    
    # Set optional secrets with user input
    for secret_name in "${!OPTIONAL_SECRETS[@]}"; do
        description="${OPTIONAL_SECRETS[$secret_name]}"
        echo -e "${YELLOW}Enter ${description} (${secret_name}):${NC}"
        read -r secret_value
        
        if [[ -n "$secret_value" ]]; then
            if gh secret set "$secret_name" --body "$secret_value" --repo "$GITHUB_REPO"; then
                echo -e "${GREEN}✅ ${secret_name} set successfully${NC}"
            else
                echo -e "${RED}❌ Failed to set ${secret_name}${NC}"
            fi
        else
            echo -e "${YELLOW}⏭️  Skipping ${secret_name}${NC}"
        fi
    done
}

# Setup AWS Secrets Manager
setup_aws_secrets() {
    echo -e "${BLUE}🔧 Setting up AWS Secrets Manager...${NC}"
    
    # Database credentials
    echo -e "${YELLOW}Setting up database credentials...${NC}"
    echo "Enter PostgreSQL database password:"
    read -s db_password
    
    DB_SECRET_NAME="goorm-popcorn-dev/database/credentials"
    DB_SECRET_VALUE=$(cat <<EOF
{
    "password": "$db_password",
    "username": "postgres"
}
EOF
)
    
    if aws secretsmanager create-secret \
        --name "$DB_SECRET_NAME" \
        --description "PostgreSQL database credentials" \
        --secret-string "$DB_SECRET_VALUE" \
        --region "$AWS_REGION" 2>/dev/null; then
        echo -e "${GREEN}✅ Database credentials created${NC}"
    else
        echo -e "${YELLOW}⚠️  Database credentials already exist, updating...${NC}"
        aws secretsmanager update-secret \
            --secret-id "$DB_SECRET_NAME" \
            --secret-string "$DB_SECRET_VALUE" \
            --region "$AWS_REGION"
        echo -e "${GREEN}✅ Database credentials updated${NC}"
    fi
    
    # Toss Payments credentials
    echo -e "${YELLOW}Setting up Toss Payments credentials...${NC}"
    echo "Enter Toss Payments secret key (test_sk_... or live_sk_...):"
    read -s toss_secret_key
    
    if [[ -n "$toss_secret_key" ]]; then
        TOSS_SECRET_NAME="goorm-popcorn-dev/payment/toss-secret"
        TOSS_SECRET_VALUE=$(cat <<EOF
{
    "secret_key": "$toss_secret_key"
}
EOF
)
        
        if aws secretsmanager create-secret \
            --name "$TOSS_SECRET_NAME" \
            --description "Toss Payments API credentials" \
            --secret-string "$TOSS_SECRET_VALUE" \
            --region "$AWS_REGION" 2>/dev/null; then
            echo -e "${GREEN}✅ Toss Payments credentials created${NC}"
        else
            echo -e "${YELLOW}⚠️  Toss Payments credentials already exist, updating...${NC}"
            aws secretsmanager update-secret \
                --secret-id "$TOSS_SECRET_NAME" \
                --secret-string "$TOSS_SECRET_VALUE" \
                --region "$AWS_REGION"
            echo -e "${GREEN}✅ Toss Payments credentials updated${NC}"
        fi
    else
        echo -e "${YELLOW}⏭️  Skipping Toss Payments credentials${NC}"
    fi
    
    # Payment encryption key
    echo -e "${YELLOW}Setting up payment encryption key...${NC}"
    echo "Enter payment encryption key (256-bit hex string) or press Enter to generate:"
    read -r encryption_key
    
    if [[ -z "$encryption_key" ]]; then
        # Generate a random 256-bit key
        encryption_key=$(openssl rand -hex 32)
        echo -e "${GREEN}✅ Generated encryption key: ${encryption_key}${NC}"
    fi
    
    ENCRYPTION_SECRET_NAME="goorm-popcorn-dev/payment/encryption-key"
    ENCRYPTION_SECRET_VALUE=$(cat <<EOF
{
    "encryption_key": "$encryption_key"
}
EOF
)
    
    if aws secretsmanager create-secret \
        --name "$ENCRYPTION_SECRET_NAME" \
        --description "Payment service encryption key" \
        --secret-string "$ENCRYPTION_SECRET_VALUE" \
        --region "$AWS_REGION" 2>/dev/null; then
        echo -e "${GREEN}✅ Payment encryption key created${NC}"
    else
        echo -e "${YELLOW}⚠️  Payment encryption key already exists, updating...${NC}"
        aws secretsmanager update-secret \
            --secret-id "$ENCRYPTION_SECRET_NAME" \
            --secret-string "$ENCRYPTION_SECRET_VALUE" \
            --region "$AWS_REGION"
        echo -e "${GREEN}✅ Payment encryption key updated${NC}"
    fi
    
    # JWT Secret (recommended improvement)
    echo -e "${YELLOW}Setting up JWT secret (recommended)...${NC}"
    echo "Enter JWT secret key or press Enter to generate:"
    read -r jwt_secret
    
    if [[ -z "$jwt_secret" ]]; then
        # Generate a random JWT secret
        jwt_secret=$(openssl rand -base64 64 | tr -d '\n')
        echo -e "${GREEN}✅ Generated JWT secret${NC}"
    fi
    
    JWT_SECRET_NAME="goorm-popcorn-dev/jwt-secret"
    JWT_SECRET_VALUE=$(cat <<EOF
{
    "secret_key": "$jwt_secret"
}
EOF
)
    
    if aws secretsmanager create-secret \
        --name "$JWT_SECRET_NAME" \
        --description "JWT signing secret" \
        --secret-string "$JWT_SECRET_VALUE" \
        --region "$AWS_REGION" 2>/dev/null; then
        echo -e "${GREEN}✅ JWT secret created${NC}"
    else
        echo -e "${YELLOW}⚠️  JWT secret already exists, updating...${NC}"
        aws secretsmanager update-secret \
            --secret-id "$JWT_SECRET_NAME" \
            --secret-string "$JWT_SECRET_VALUE" \
            --region "$AWS_REGION"
        echo -e "${GREEN}✅ JWT secret updated${NC}"
    fi
}

# Verify secrets
verify_secrets() {
    echo -e "${BLUE}🔍 Verifying secrets...${NC}"
    
    # Verify GitHub secrets
    echo -e "${YELLOW}GitHub Secrets:${NC}"
    if gh secret list --repo "$GITHUB_REPO" | grep -q "AWS_ROLE_ARN"; then
        echo -e "${GREEN}✅ AWS_ROLE_ARN${NC}"
    else
        echo -e "${RED}❌ AWS_ROLE_ARN${NC}"
    fi
    
    if gh secret list --repo "$GITHUB_REPO" | grep -q "DISCORD_WEBHOOK"; then
        echo -e "${GREEN}✅ DISCORD_WEBHOOK${NC}"
    else
        echo -e "${YELLOW}⚠️  DISCORD_WEBHOOK (optional)${NC}"
    fi
    
    # Verify AWS secrets
    echo -e "${YELLOW}AWS Secrets Manager:${NC}"
    if aws secretsmanager describe-secret --secret-id "goorm-popcorn-dev/database/credentials" --region "$AWS_REGION" &>/dev/null; then
        echo -e "${GREEN}✅ Database credentials${NC}"
    else
        echo -e "${RED}❌ Database credentials${NC}"
    fi
    
    if aws secretsmanager describe-secret --secret-id "goorm-popcorn-dev/payment/toss-secret" --region "$AWS_REGION" &>/dev/null; then
        echo -e "${GREEN}✅ Toss Payments credentials${NC}"
    else
        echo -e "${YELLOW}⚠️  Toss Payments credentials (optional)${NC}"
    fi
    
    if aws secretsmanager describe-secret --secret-id "goorm-popcorn-dev/payment/encryption-key" --region "$AWS_REGION" &>/dev/null; then
        echo -e "${GREEN}✅ Payment encryption key${NC}"
    else
        echo -e "${RED}❌ Payment encryption key${NC}"
    fi
}

# Generate summary
generate_summary() {
    echo -e "${BLUE}📋 Setup Summary${NC}"
    echo "=================="
    
    cat <<EOF

🔐 GitHub Secrets configured:
   - AWS_ROLE_ARN (for AWS deployment)
   - DISCORD_WEBHOOK (for notifications)
   - SNYK_TOKEN (for security scanning)
   - SONAR_TOKEN (for code analysis)
   - PROD_APPROVERS (for manual approvals)

🔒 AWS Secrets Manager configured:
   - goorm-popcorn-dev/database/credentials
   - goorm-popcorn-dev/payment/toss-secret
   - goorm-popcorn-dev/payment/encryption-key
   - goorm-popcorn-dev/jwt-secret

📚 Next Steps:
   1. Update Task Definitions to use JWT secret from AWS Secrets Manager
   2. Test workflows to ensure secrets are accessible
   3. Set up secret rotation schedule
   4. Configure monitoring for secret access

🔗 Documentation:
   - See .github/SECRETS_MANAGEMENT.md for detailed information
   - See .github/DISCORD_SETUP.md for Discord webhook setup

EOF
}

# Main execution
main() {
    echo "This script will set up GitHub Secrets and AWS Secrets Manager for the Goorm Popcorn project."
    echo "Make sure you have the necessary permissions for both GitHub and AWS."
    echo ""
    read -p "Continue? (y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Setup cancelled."
        exit 0
    fi
    
    check_prerequisites
    setup_github_secrets
    setup_aws_secrets
    verify_secrets
    generate_summary
    
    echo -e "${GREEN}🎉 Secrets setup completed successfully!${NC}"
}

# Run main function
main "$@"