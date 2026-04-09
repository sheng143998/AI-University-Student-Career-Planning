#!/bin/bash
# ==========================================
# AI Career Planning -  Server Deployment Script
# ==========================================
# Usage: ./deploy-server.sh
# Requires: CentOS/Ubuntu, root privileges
# ==========================================

set -e

echo "=========================================="
echo "  AI Career Planning - Server Deployment"
echo "=========================================="

# Configuration
APP_DIR="/opt/app"
BACKEND_PORT=8081
DOMAIN=${1:-""}  # Optional domain parameter

# 1. Install dependencies
echo "[1/7] Installing dependencies..."
if command -v yum &> /dev/null; then
    yum update -y
    yum install -y git java-17-openjdk java-17-openjdk-devel nginx mysql-server redis
elif command -v apt &> /dev/null; then
    apt update && apt upgrade -y
    apt install -y git openjdk-17-jdk nginx mysql-server redis
fi

# 2. Create directory structure
echo "[2/7] Creating directory structure..."
mkdir -p $APP_DIR/{frontend/dist,backend,scripts,logs,config,backup}

# 3. Configure MySQL
echo "[3/7] Configuring MySQL..."
systemctl start mysqld || systemctl start mysql
systemctl enable mysqld || systemctl enable mysql

# Create database (run manually if needed)
# mysql -u root -e "CREATE DATABASE IF NOT EXISTS ai_career_plan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 4. Configure Redis
echo "[4/7] Configuring Redis..."
systemctl start redis
systemctl enable redis

# 5. Create systemd service for backend
echo "[5/7] Creating systemd service..."
cat > /etc/systemd/system/career-app.service << 'EOF'
[Unit]
Description=AI Career Planning Backend
After=network.target mysqld.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/app/backend
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/app/backend/app.jar --spring.profiles.active=prod
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable career-app

# 6. Configure Nginx
echo "[6/7] Configuring Nginx..."
cat > /etc/nginx/conf.d/career-app.conf << 'EOF'
server {
    listen 80 default_server;
    server_name _;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    gzip_min_length 1024;

    # Frontend static files
    location / {
        root /opt/app/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # Static assets with long cache
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        root /opt/app/frontend/dist;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # Backend API proxy
    location /api/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }

    # AI API with longer timeout
    location /api/ai/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 120s;
        proxy_buffering off;
    }
}
EOF

# Remove default nginx config if exists
rm -f /etc/nginx/sites-enabled/default 2>/dev/null || true

nginx -t
systemctl enable nginx
systemctl start nginx

# 7. Create deployment helper scripts
echo "[7/7] Creating helper scripts..."

# Frontend update script
cat > $APP_DIR/scripts/update-frontend.sh << 'SCRIPT'
#!/bin/bash
echo "Updating frontend..."
rm -rf /opt/app/frontend/dist/*
cp -r /tmp/dist/* /opt/app/frontend/dist/
nginx -s reload
echo "Frontend updated successfully!"
SCRIPT
chmod +x $APP_DIR/scripts/update-frontend.sh

# Backend update script
cat > $APP_DIR/scripts/update-backend.sh << 'SCRIPT'
#!/bin/bash
echo "Updating backend..."
# Backup old jar
if [ -f /opt/app/backend/app.jar ]; then
    cp /opt/app/backend/app.jar /opt/app/backup/app_$(date +%Y%m%d_%H%M%S).jar
fi
# Copy new jar
cp /tmp/app.jar /opt/app/backend/app.jar
# Restart service
systemctl restart career-app
echo "Backend updated successfully!"
SCRIPT
chmod +x $APP_DIR/scripts/update-backend.sh

# Health check script
cat > $APP_DIR/scripts/health-check.sh << 'SCRIPT'
#!/bin/bash
BACKEND=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/market/hot-jobs 2>/dev/null || echo "000")
FRONTEND=$(curl -s -o /dev/null -w "%{http_code}" http://localhost 2>/dev/null || echo "000")
echo "$(date): Backend=$BACKEND, Frontend=$FRONTEND"
if [ "$BACKEND" != "200" ]; then
    echo "Backend unhealthy, restarting..."
    systemctl restart career-app
fi
SCRIPT
chmod +x $APP_DIR/scripts/health-check.sh

# Add to crontab for health monitoring
(crontab -l 2>/dev/null; echo "*/5 * * * * $APP_DIR/scripts/health-check.sh >> $APP_DIR/logs/health.log 2>&1") | crontab -

# Configure firewall
echo "Configuring firewall..."
if command -v firewall-cmd &> /dev/null; then
    firewall-cmd --permanent --add-port=80/tcp
    firewall-cmd --permanent --add-port=443/tcp
    firewall-cmd --permanent --add-port=22/tcp
    firewall-cmd --reload
fi

echo "=========================================="
echo "  Deployment Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Upload backend JAR to /tmp/app.jar"
echo "   scp target/*.jar root@SERVER_IP:/tmp/app.jar"
echo ""
echo "2. Upload frontend dist to /tmp/dist/"
echo "   scp -r dist/* root@SERVER_IP:/tmp/dist/"
echo ""
echo "3. Run update scripts:"
echo "   /opt/app/scripts/update-backend.sh"
echo "   /opt/app/scripts/update-frontend.sh"
echo ""
echo "4. Configure database in /opt/app/config/application-prod.yml"
echo ""
echo "5. Access your app at: http://YOUR_SERVER_IP"
echo "=========================================="
