# 

## 

###  (Spring Boot)
|  |  |  |
|---|---|---|
| Spring Boot | 4.0.4 |  |
| Java | 17 |  |
| Spring AI | 2.0.0-M3 | AI  |
| MyBatis | 4.0.1 | ORM |
| PostgreSQL + pgvector | - |  +  |
| Redis | - |  |
|  OSS | 3.17.4 |  |
| Knife4j | 4.5.0 | API  |

###  (Vue 3)
|  |  |  |
|---|---|---|
| Vue | 3.4.21 |  |
| Vite | 5.2.0 |  |
| TypeScript | 5.4.0 |  |
| TailwindCSS | 3.4.19 |  |
| Pinia | 2.1.7 |  |
| Vue Router | 4.3.0 |  |
| Axios | 1.14.0 | HTTP  |

---

## 

```
fuchuang1/
 server/     # Spring Boot 
 pojo/       # 
 common/     # 
 website/    # Vue 3 
 database/   # 
 deploy/     # 
```

---

## 

|  |  |  |
|---|---|---|
|  UI  |  |  dist/ |
| AI  |  |  jar |
|  |  |  jar |
|  |  | SQL +  |
|  |  | application.yml |

---

## 

### 1: 

```bash
#  
bash deploy/deploy-hot.sh fe

#  
bash deploy/deploy-hot.sh be

#  
bash deploy/deploy-hot.sh all
```

**:**
-  SSH 
- 
-  Nginx 

### 2: Git + 

```bash
# 
ssh root@SERVER_IP
cd /opt/app
git pull

# 
cd website && npm run build && cp -r dist/* /opt/app/frontend/dist/

# 
cd ../server && mvn package -DskipTests && systemctl restart app
```

### 3: Docker (, )

```bash
# 
docker-compose build --no-cache
docker-compose up -d
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

---

## 

|  |  |
|---|---|
|  | `systemctl start/stop/restart app` |
|  | `nginx -s reload` |
|  | `journalctl -u app -f` |
|  | `tail -f /var/log/nginx/access.log` |
|  | `curl http://localhost/api/market/hot-jobs` |

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

|  |  |  |
|---|---|---|
|  | `bash deploy-hot.sh fe` | 30 |
|  | `bash deploy-hot.sh be` | 60 |
|  | `bash deploy-hot.sh all` | 90 |
|  | `git pull + build` | 2-5 |

---

## 

```bash
#  5 
crontab -e
# :
*/5 * * * * /opt/app/scripts/health-check.sh >> /opt/app/logs/health.log 2>&1
```

---

## 

1. **:**
   - 
   - 
   - 

2. **:**
   -  CI/CD (GitHub Actions)
   - 
   - 

3. **:**
   - 
   - 
   -  OSS CDN
