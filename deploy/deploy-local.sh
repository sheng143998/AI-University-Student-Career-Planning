#!/bin/bash
# ========================================
#  - Windows Git Bash / WSL
# ========================================

set -e

# =============
# 
# =============
SERVER_USER="root"                    # SSH 
SERVER_IP="YOUR_SERVER_IP"             #  IP
SERVER_DIR="/opt/app"                  # 
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"  # 

echo "=========================================="
echo "  Vue + Spring Boot "
echo "=========================================="
echo ": $SERVER_USER@$SERVER_IP"
echo ": $PROJECT_DIR"
echo "=========================================="

# =============
# 1. 
# =============
echo ""
echo "[1/5] ..."
cd "$PROJECT_DIR/server"
mvn clean package -DskipTests
echo "[] : $PROJECT_DIR/server/target/*.jar"

# =============
# 2. 
# =============
echo ""
echo "[2/5] ..."
cd "$PROJECT_DIR/website"
npm install
npm run build
echo "[] : $PROJECT_DIR/website/dist/"

# =============
# 3. 
# =============
echo ""
echo "[3/5] ..."

# 
scp "$PROJECT_DIR/server/target/"*.jar $SERVER_USER@$SERVER_IP:/tmp/app.jar
echo "[OK]  -> /tmp/app.jar"

# 
tar -czf /tmp/dist.tar.gz -C "$PROJECT_DIR/website/dist" .
scp /tmp/dist.tar.gz $SERVER_USER@$SERVER_IP:/tmp/dist.tar.gz
echo "[OK]  -> /tmp/dist.tar.gz"

# 
if [ -f "$PROJECT_DIR/server/src/main/resources/application-prod.yml" ]; then
    scp "$PROJECT_DIR/server/src/main/resources/application-prod.yml" $SERVER_USER@$SERVER_IP:/tmp/application-prod.yml
    echo "[OK]  -> /tmp/application-prod.yml"
fi

# =============
# 4. 
# =============
echo ""
echo "[4/5] ..."
ssh $SERVER_USER@$SERVER_IP << 'EOF'
set -e

echo "  ..."

# 
mkdir -p /opt/app/{frontend/dist,backend,scripts,logs,config,backup}

# 
if [ -f "/opt/app/backend/app.jar" ]; then
    cp /opt/app/backend/app.jar /opt/app/backup/app_$(date +%Y%m%d_%H%M%S).jar
    echo "[] "
fi

# 
systemctl stop app || true

# 
cp /tmp/app.jar /opt/app/backend/app.jar
echo "[OK] "

# 
rm -rf /opt/app/frontend/dist/*
tar -xzf /tmp/dist.tar.gz -C /opt/app/frontend/dist/
echo "[OK] "

# 
if [ -f "/tmp/application-prod.yml" ]; then
    cp /tmp/application-prod.yml /opt/app/config/
    chmod 600 /opt/app/config/application-prod.yml
    echo "[OK] "
fi

# 
systemctl start app
echo "[OK] "

#  Nginx
nginx -s reload
echo "[OK] Nginx "

# 
sleep 3
if systemctl is-active --quiet app; then
    echo "[OK]  "
else
    echo "[] "
    journalctl -u app --no-pager -n 20
    exit 1
fi

# 
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost)
if [ "$HTTP_CODE" = "200" ]; then
    echo "[OK]  (HTTP $HTTP_CODE)"
else
    echo "[]  (HTTP $HTTP_CODE)"
fi

echo "=========================================="
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="
EOF

# =============
# 5. 
# =============
echo ""
echo "[5/5] ..."
echo ""
echo "=========================================="
echo " !"
echo "=========================================="
echo ": http://$SERVER_IP"
echo ": http://$SERVER_IP/api/market/hot-jobs"
echo ""
echo ":"
echo "  ssh $SERVER_USER@$SERVER_IP"
echo "  journalctl -u app -f"
echo "=========================================="
