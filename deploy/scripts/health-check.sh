#!/bin/bash
# ========================================
#  -  cron 
# ========================================

LOG_FILE="/opt/app/logs/health.log"
ALERT_EMAIL=""  # 

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

check_backend() {
    # 
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 http://localhost:8081/api/market/hot-jobs)
    
    if [ "$HTTP_CODE" = "200" ]; then
        log "[OK]  (HTTP $HTTP_CODE)"
        return 0
    else
        log "[FAIL]  (HTTP $HTTP_CODE)"
        return 1
    fi
}

check_frontend() {
    # 
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 http://localhost)
    
    if [ "$HTTP_CODE" = "200" ]; then
        log "[OK]  (HTTP $HTTP_CODE)"
        return 0
    else
        log "[FAIL]  (HTTP $HTTP_CODE)"
        return 1
    fi
}

check_disk() {
    # 
    USAGE=$(df / | awk 'NR==2 {print $5}' | tr -d '%')
    
    if [ "$USAGE" -lt 90 ]; then
        log "[OK]  (${USAGE}%)"
        return 0
    else
        log "[WARN]  (${USAGE}%)"
        return 1
    fi
}

check_memory() {
    # 
    FREE_MB=$(free -m | awk 'NR==2 {print $7}')
    
    if [ "$FREE_MB" -gt 100 ]; then
        log "[OK]  (${FREE_MB}MB )"
        return 0
    else
        log "[WARN]  (${FREE_MB}MB )"
        return 1
    fi
}

auto_recover() {
    log "[] ..."
    
    # 
    if ! systemctl is-active --quiet app; then
        log "[] ..."
        systemctl start app
        sleep 5
    fi
    
    #  Nginx
    if ! systemctl is-active --quiet nginx; then
        log "[] Nginx ..."
        systemctl start nginx
    fi
    
    # 
    if ! check_backend; then
        log "[] ..."
        systemctl restart app
        sleep 5
    fi
    
    #  Nginx
    nginx -s reload
    
    log "[] "
}

# =============
#  
# =============
log "========================================="
log " "
log "========================================="

BACKEND_OK=true
FRONTEND_OK=true
DISK_OK=true
MEMORY_OK=true

check_backend || BACKEND_OK=false
check_frontend || FRONTEND_OK=false
check_disk || DISK_OK=false
check_memory || MEMORY_OK=false

# 
if [ "$BACKEND_OK" = "false" ] || [ "$FRONTEND_OK" = "false" ]; then
    auto_recover
fi

log "========================================="
