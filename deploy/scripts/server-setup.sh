#!/bin/bash
# ========================================
#  - 
# ========================================
# : root 
# : bash server-setup.sh

set -e

echo "=========================================="
echo "  Vue + Spring Boot "
echo "=========================================="

# =============
# 1. 
# =============
echo ""
echo "[1/6] ..."

if command -v yum &> /dev/null; then
    # CentOS/RHEL
    yum update -y
    yum install -y git java-17-openjdk java-17-openjdk-devel nginx curl wget tar
elif command -v apt &> /dev/null; then
    # Ubuntu/Debian
    apt update && apt upgrade -y
    apt install -y git openjdk-17-jdk nginx curl wget tar
else
    echo "[] "
    exit 1
fi

echo "[OK] "

# =============
# 2.  Node.js ()
# =============
echo ""
echo "[2/6]  Node.js..."

if ! command -v node &> /dev/null; then
    if command -v yum &> /dev/null; then
        curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
        yum install -y nodejs
    else
        curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
        apt install -y nodejs
    fi
fi

node -v
npm -v
echo "[OK] Node.js "

# =============
# 3. 
# =============
echo ""
echo "[3/6] ..."

mkdir -p /opt/app/{frontend/dist,backend,scripts,logs,config,backup}
chmod -R 755 /opt/app

echo "[OK] "
echo "  - /opt/app/frontend/dist/  ()"
echo "  - /opt/app/backend/        ()"
echo "  - /opt/app/scripts/        ()"
echo "  - /opt/app/logs/           ()"
echo "  - /opt/app/config/         ()"
echo "  - /opt/app/backup/         ()"

# =============
# 4.  systemd 
# =============
echo ""
echo "[4/6]  systemd ..."

cat > /etc/systemd/system/app.service << 'EOF'
[Unit]
Description=Spring Boot Application
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/app/backend
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /opt/app/backend/app.jar --spring.config.location=/opt/app/config/application-prod.yml
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable app

echo "[OK] systemd "

# =============
# 5.  Nginx
# =============
echo ""
echo "[5/6]  Nginx..."

cat > /etc/nginx/conf.d/app.conf << 'EOF'
server {
    listen 80 default_server;
    server_name _;

    # Gzip 
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    gzip_min_length 1024;

    # 
    location / {
        root /opt/app/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }

    # 
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        root /opt/app/frontend/dist;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    #  API 
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

    #  AI API 
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

# 
nginx -t
systemctl enable nginx
systemctl start nginx

echo "[OK] Nginx "

# =============
# 6. 
# =============
echo ""
echo "[6/6] ..."

# 
if command -v firewall-cmd &> /dev/null; then
    firewall-cmd --permanent --add-port=22/tcp
    firewall-cmd --permanent --add-port=80/tcp
    firewall-cmd --permanent --add-port=443/tcp
    firewall-cmd --reload
    echo "[OK] firewall-cmd "
elif command -v ufw &> /dev/null; then
    ufw allow 22/tcp
    ufw allow 80/tcp
    ufw allow 443/tcp
    ufw --force enable
    echo "[OK] ufw "
fi

echo ""
echo "=========================================="
echo " !"
echo "=========================================="
echo ""
echo ":"
echo "  1.  MySQL/Redis"
echo "  2.  application-prod.yml  /opt/app/config/"
echo "  3.  jar  /opt/app/backend/app.jar"
echo "  4.  dist  /opt/app/frontend/dist/"
echo "  5. : systemctl start app"
echo ""
echo ":"
echo "  - : http://YOUR_SERVER_IP"
echo "  - : systemctl status app"
echo "  - : journalctl -u app -f"
echo "=========================================="
