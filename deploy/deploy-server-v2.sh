#!/bin/bash
# ==========================================
# AI Career Planning - Server Deployment Script (v2)
# ==========================================
#  adapted for Alibaba Cloud RDS PostgreSQL
# Usage: ./deploy-server-v2.sh
# ==========================================

set -e

echo "=========================================="
echo "  AI Career Planning - Server Deployment"
echo "=========================================="

# Configuration
APP_DIR="/opt/app"

# 1. Install dependencies
echo "[1/5] Installing dependencies..."
if command -v yum &> /dev/null; then
    yum update -y
    yum install -y git java-17-openjdk java-17-openjdk-devel nginx redis
elif command -v apt &> /dev/null; then
    apt update && apt upgrade -y
    apt install -y git openjdk-17-jdk nginx redis-server
fi

# 2. Create directory structure
echo "[2/5] Creating directory structure..."
mkdir -p $APP_DIR/{frontend/dist,backend,scripts,logs,config,backup}

# 3. Configure Redis (local cache)
echo "[3/5] Configuring Redis..."
systemctl start redis || systemctl start redis-server
systemctl enable redis || systemctl enable redis-server

# 4. Create systemd service for backend
echo "[4/5] Creating systemd service..."
cat > /etc/systemd/system/career-app.service << 'EOF'
[Unit]
Description=AI Career Planning Backend
After=network.target redis.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/app/backend
# Load environment variables from file
EnvironmentFile=/opt/app/config/env
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

# 5. Configure Nginx
echo "[5/5] Configuring Nginx..."
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

# Create helper scripts
echo "Creating helper scripts..."

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
if [ -f /opt/app/backend/app.jar ]; then
    cp /opt/app/backend/app.jar /opt/app/backup/app_$(date +%Y%m%d_%H%M%S).jar
fi
cp /tmp/app.jar /opt/app/backend/app.jar
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

# Add to crontab
(crontab -l 2>/dev/null; echo "*/5 * * * * $APP_DIR/scripts/health-check.sh >> $APP_DIR/logs/health.log 2>&1") | crontab -

# Configure firewall
echo "Configuring firewall..."
if command -v firewall-cmd &> /dev/null; then
    firewall-cmd --permanent --add-port=80/tcp
    firewall-cmd --permanent --add-port=443/tcp
    firewall-cmd --permanent --add-port=22/tcp
    firewall-cmd --reload
fi

# Create environment file template
cat > $APP_DIR/config/env << 'EOF'
# PostgreSQL (Alibaba Cloud RDS)
fuchuang_datasource_host=pgm-bp1m35r42v5tn93aso.pg.rds.aliyuncs.com
fuchuang_datasource_port=5432
fuchuang_datasource_database=ai_career_plan
fuchuang_datasource_username=ai_career_plan
fuchuang_datasource_password=YOUR_PASSWORD_HERE
fuchuang_datasource_schema=ai_career_plan

# Redis
FUCHUANG_REDIS_HOST=localhost
FUCHUANG_REDIS_PORT=6379
FUCHUANG_REDIS_DATABASE=0

# Alibaba Cloud OSS
FUCHUANG_ALIOSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
FUCHUANG_ALIOSS_ACCESS_KEY_ID=YOUR_ACCESS_KEY
FUCHUANG_ALIOSS_ACCESS_KEY_SECRET=YOUR_ACCESS_SECRET
FUCHUANG_ALIOSS_BUCKET_NAME=YOUR_BUCKET_NAME

# AI (DashScope)
OPENAI_API_KEY=YOUR_DASHSCOPE_API_KEY
EOF

chmod 600 $APP_DIR/config/env

echo "=========================================="
echo "  Deployment Complete!"
echo "=========================================="
echo ""
echo "IMPORTANT: Configure environment variables:"
echo "  vi /opt/app/config/env"
echo ""
echo "Then deploy your app:"
echo "  1. Upload backend JAR to /tmp/app.jar"
echo "  2. Upload frontend dist to /tmp/dist/"
echo "  3. Run: /opt/app/scripts/update-backend.sh"
echo "  4. Run: /opt/app/scripts/update-frontend.sh"
echo ""
echo "Access: http://YOUR_SERVER_IP"
echo "=========================================="
