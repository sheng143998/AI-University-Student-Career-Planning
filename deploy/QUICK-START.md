# 

## 

```bash
# 1. 
bash deploy/scripts/server-setup.sh

# 2.  MySQL/Redis

# 3. 
# Windows Git Bash:
bash deploy/deploy-local.sh
```

---

## 

|  |  |
|---|---|
|  | `systemctl start/stop/restart app` |
|  | `systemctl start/stop/restart nginx` |
|  | `journalctl -u app -f` |
|  | `tail -f /var/log/nginx/access.log` |
|  | `curl http://localhost/api/market/hot-jobs` |
|  | `curl http://YOUR_IP` |

---

## 

|  |  |
|---|---|
| 502 Bad Gateway | `systemctl start app` |
| 404 Not Found |  Nginx root  |
|  |  |
|  |  |

---

## 

|  |  |
|---|---|
| `/opt/app/frontend/dist/` |  |
| `/opt/app/backend/app.jar` |  |
| `/opt/app/config/` |  |
| `/opt/app/logs/` |  |
| `/opt/app/backup/` |  |

---

## 

```bash
#  5 
crontab -e
# :
*/5 * * * * /opt/app/scripts/health-check.sh
```

---

## 

```bash
# 
ssh root@YOUR_IP

# 
cd /opt/app

# 
bash scripts/deploy.sh
```
