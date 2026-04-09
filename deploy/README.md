# Vue + Spring Boot  Linux 

## 

- Linux  (CentOS/Ubuntu)
- root 
-  IP

---

## 

### 1. 

```bash
# CentOS
yum update -y
yum install -y git java-17-openjdk java-17-openjdk-devel nginx

# Ubuntu
apt update && apt upgrade -y
apt install -y git openjdk-17-jdk nginx
```

### 2.  Node.js (, )

```bash
#  Node.js
curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
yum install -y nodejs

# 
node -v
npm -v
```

### 3. 

```bash
#  Maven
cd /opt
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
tar -xzf apache-maven-3.9.6-bin.tar.gz
ln -s /opt/apache-maven-3.9.6/bin/mvn /usr/local/bin/mvn

# 
mvn -v
```

### 4. 

```bash
# 
mkdir -p /opt/app/{frontend,backend,scripts,logs,config,backup}

# 
chmod -R 755 /opt/app
```

---

## 

### 1. 

```bash
# 
firewall-cmd --permanent --add-port=22/tcp   # SSH
firewall-cmd --permanent --add-port=80/tcp    # HTTP
firewall-cmd --permanent --add-port=443/tcp   # HTTPS
firewall-cmd --reload

# 
firewall-cmd --list-ports
```

### 2. 

>  (, )

---

## 

### 1. 

```bash
#  systemd 
cat > /etc/systemd/system/app.service << 'EOF'
[Unit]
Description=Spring Boot Application
After=network.target mysql.service redis.service

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

# 
systemctl daemon-reload
systemctl enable app
```

### 2. 

```bash
#  Nginx 
cat > /etc/nginx/conf.d/app.conf << 'EOF'
# HTTP -  IP 
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
        
        # 
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
        
        # 
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }

    # 
    location /api/ai/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # AI 
        proxy_read_timeout 120s;
        proxy_buffering off;
    }
}

#  API 
server {
    listen 8081;
    server_name localhost;
    
    location / {
        return 403;
    }
}
EOF

#  Nginx 
nginx -t
systemctl enable nginx
systemctl start nginx
```

---

## 

### 1. 

```bash
#  
cat > /opt/app/config/application-prod.yml << 'EOF'
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_career_plan?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: YOUR_DB_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  data:
    redis:
      host: localhost
      port: 6379
      password: YOUR_REDIS_PASSWORD
      
  ai:
    openai:
      api-key: YOUR_OPENAI_API_KEY
      base-url: YOUR_OPENAI_BASE_URL

logging:
  file:
    path: /opt/app/logs
    name: app.log
  level:
    com.itsheng: INFO
    org.springframework: WARN

# 
jwt:
  secret: YOUR_JWT_SECRET
EOF

# 
chmod 600 /opt/app/config/application-prod.yml
```

---

## 

### 1. 

```bash
#  MySQL
yum install -y mysql-server
systemctl start mysqld
systemctl enable mysqld

# 
mysql_secure_installation

# 
mysql -u root -p
CREATE DATABASE ai_career_plan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'app_user'@'localhost' IDENTIFIED BY 'STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON ai_career_plan.* TO 'app_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2.  Redis

```bash
#  Redis
yum install -y redis
systemctl start redis
systemctl enable redis

# 
vi /etc/redis.conf
#  requirepass YOUR_REDIS_PASSWORD

systemctl restart redis
```

---

## 

### 1. 

```bash
#  Maven
cd /path/to/server
mvn clean package -DskipTests

#  target/*.jar  /opt/app/backend/app.jar
cp target/*.jar /opt/app/backend/app.jar
```

### 2. 

```bash
#  Node 
npm run build

#  dist/*  /opt/app/frontend/dist/
cp -r dist/* /opt/app/frontend/dist/
```

---

## 

```bash
# 
systemctl start app

# 
systemctl status app

# 
tail -f /opt/app/logs/app.log

# 
curl http://localhost/api/market/hot-jobs

# 
curl http://localhost
```

---

## 

### 1. 

```bash
# 
cat > /opt/app/scripts/deploy.sh << 'SCRIPT'
#!/bin/bash
set -e

echo "=========================================="
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

# 
BACKEND_JAR="/opt/app/backend/app.jar"
BACKUP_DIR="/opt/app/backup"

# 
if [ -f "$BACKEND_JAR" ]; then
    mkdir -p "$BACKUP_DIR"
    cp "$BACKEND_JAR" "$BACKUP_DIR/app_$(date +%Y%m%d_%H%M%S).jar"
    echo "[] "
fi

# 
if [ -f "/tmp/app.jar" ]; then
    systemctl stop app
    cp /tmp/app.jar "$BACKEND_JAR"
    systemctl start app
    echo "[] "
fi

# 
if [ -d "/tmp/dist" ]; then
    rm -rf /opt/app/frontend/dist/*
    cp -r /tmp/dist/* /opt/app/frontend/dist/
    nginx -s reload
    echo "[] "
fi

echo "=========================================="
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="
SCRIPT

chmod +x /opt/app/scripts/deploy.sh
```

### 2. 

```bash
# 
/opt/app/scripts/deploy.sh
```

---

## 

```bash
# 
journalctl -u app -f --no-pager

#  Nginx 
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log

# 
systemctl restart app
systemctl restart nginx

# 
systemctl stop app

# 
systemctl status app
systemctl status nginx
systemctl status mysql
systemctl status redis
```

---

## 

### 1. 

```bash
# 
certbot --nginx -d your-domain.com -d www.your-domain.com

# 
echo "0 3 * * * /usr/bin/certbot renew --quiet" | crontab -
```

### 2. 

>  SSL  Nginx 

---

## 

|  |  |  |
|---|---|---|
| 502 Bad Gateway |  | `systemctl start app` |
| 404 Not Found | Nginx  |  `root`  |
|  |  |  |
| API  |  |  |
|  |  |  |

---

## 

1. ****: 
2. ****: 
3. ****: 
4. ****: 
5. ****: 
6. ****: 

---

## 

```bash
# 
cat > /opt/app/scripts/health-check.sh << 'SCRIPT'
#!/bin/bash

# 
BACKEND_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/market/hot-jobs)
FRONTEND_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost)

echo "======  ======"
echo ": $BACKEND_HEALTH"
echo ": $FRONTEND_HEALTH"
echo "========================"

if [ "$BACKEND_HEALTH" != "200" ] || [ "$FRONTEND_HEALTH" != "200" ]; then
    echo "[] "
    # 
    systemctl restart app
    nginx -s reload
fi
SCRIPT

chmod +x /opt/app/scripts/health-check.sh

#  5 
echo "*/5 * * * * /opt/app/scripts/health-check.sh >> /opt/app/logs/health.log 2>&1" | crontab -
```

---

## 

|  |  |  |
|---|---|---|
| `/opt/app/frontend/dist/` |  | dist/ |
| `/opt/app/backend/app.jar` |  | target/*.jar |
| `/opt/app/config/` |  | application-prod.yml |
| `/opt/app/logs/` |  | app.log |
| `/opt/app/backup/` |  | .jar |
| `/opt/app/scripts/` |  | deploy.sh |

---

## 

```bash
#  1: 
systemctl status app nginx mysql redis

#  2: 
curl http://localhost
curl http://localhost/api/market/hot-jobs

#  3: 
curl http://YOUR_PUBLIC_IP
curl http://YOUR_PUBLIC_IP/api/market/hot-jobs

#  4: 
#  http://YOUR_PUBLIC_IP
```

---

## 

```bash
# 
ssh user@YOUR_SERVER_IP

# 
cd /path/to/project

# 
./deploy-local.sh
```

>  `deploy-local.sh`
