#!/bin/bash
# ========================================
#  - 
# ========================================
# :
#   bash deploy-hot.sh fe     #  
#   bash deploy-hot.sh be     #  
#   bash deploy-hot.sh all    #  

set -e

# =============
# 
# =============
SERVER_USER="root"
SERVER_IP="YOUR_SERVER_IP"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# 
DEPLOY_MODE="${1:-all}"

echo "=========================================="
echo "  - $DEPLOY_MODE"
echo "=========================================="

# =============
#  
# =============
deploy_frontend() {
    echo ""
    echo "[FRONTEND] ..."
    
    # 
    cd "$PROJECT_DIR/website"
    npm run build
    
    # 
    tar -czf /tmp/dist.tar.gz -C dist .
    scp /tmp/dist.tar.gz $SERVER_USER@$SERVER_IP:/tmp/
    
    # 
    ssh $SERVER_USER@$SERVER_IP << 'EOF'
rm -rf /opt/app/frontend/dist/*
tar -xzf /tmp/dist.tar.gz -C /opt/app/frontend/dist/
nginx -s reload
echo "[OK] "
EOF
    
    echo "[OK]  - "
}

# =============
#  
# =============
deploy_backend() {
    echo ""
    echo "[BACKEND] ..."
    
    # 
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests -pl server -am
    
    # 
    scp server/target/*.jar $SERVER_USER@$SERVER_IP:/tmp/app.jar
    
    # 
    ssh $SERVER_USER@$SERVER_IP << 'EOF'
# 
if [ -f /opt/app/backend/app.jar ]; then
    cp /opt/app/backend/app.jar /opt/app/backup/app_$(date +%Y%m%d_%H%M%S).jar
fi

# 
systemctl stop app
cp /tmp/app.jar /opt/app/backend/app.jar
systemctl start app

# 
sleep 3
if systemctl is-active --quiet app; then
    echo "[OK] "
else
    echo "[FAIL] "
    journalctl -u app --no-pager -n 20
    exit 1
fi
EOF
    
    echo "[OK]  - "
}

# =============
# 
# =============
case "$DEPLOY_MODE" in
    "fe"|"frontend")
        deploy_frontend
        ;;
    "be"|"backend")
        deploy_backend
        ;;
    "all"|"full")
        deploy_backend
        deploy_frontend
        ;;
    *)
        echo ": $0 [fe|be|all]"
        echo "  fe  - "
        echo "  be  - "
        echo "  all - "
        exit 1
        ;;
esac

echo ""
echo "=========================================="
echo " !"
echo "=========================================="
echo ": http://$SERVER_IP"
echo "=========================================="
