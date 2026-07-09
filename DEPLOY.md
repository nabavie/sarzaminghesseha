# Deploying to production (faradia.ir)

The stack is three containers managed by `docker-compose.yml`:

| Service | Role |
|---------|------|
| `traefik` | Reverse proxy on ports 80/443; gets and auto-renews free Let's Encrypt TLS certificates |
| `app` | Spring Boot app built from the local `Dockerfile`, runs with the `prod` profile |
| `db` | MySQL 8.4 on an internal-only network (no port exposed to the internet) |

## Prerequisites

- A Linux server with Docker + the compose plugin (`docker compose version`)
- DNS: an **A record for `faradia.ir`** pointing at the server's public IP
  (Let's Encrypt validates over HTTP, so DNS must resolve before first start)
- Ports 80 and 443 open in the server firewall

## Steps

1. Copy the project to the server (e.g. `git clone` or `scp`/`rsync` — the
   `target/` and `uploads/` folders are not needed; the image builds from source).

2. Create the secrets file:

   ```bash
   cp .env.example .env
   nano .env        # fill in real passwords; e.g. generate with: openssl rand -base64 24
   ```

   Seed account passwords must satisfy the app's policy (min 8 chars, at least
   one letter and one digit).

3. Build and start:

   ```bash
   docker compose up -d --build
   ```

   First start takes a few minutes (Maven build + cert issuance). Check with
   `docker compose logs -f traefik app`.

4. Open https://faradia.ir — the demo content (admin/testadmin accounts, the
   `qessegoo` storyteller, 7 categories, 8 approved folk tales with narration)
   is seeded automatically on the first boot of the fresh database, with the
   passwords you set in `.env`.

## Bringing over data created locally (optional)

The seeders recreate all the built-in demo content, so nothing needs migrating
for a plain demo. Only if you added *extra* users/tales/comments on your local
machine and want them on the server:

```bash
# on your machine: dump the local DB
mysqldump -u root -p myapp_db > dump.sql

# copy dump.sql and your local uploads/ folder to the server, then:
docker compose up -d db
docker compose exec -T db sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" qesseha' < dump.sql
docker compose cp uploads/. app:/app/uploads/    # after `docker compose up -d app`
```

(Local passwords come with the dump — the seeders skip accounts/tales that
already exist, so `.env` seed passwords won't apply to imported rows.)

## Day-2 operations

- **Redeploy after a code change:** `docker compose up -d --build app`
- **Logs:** `docker compose logs -f app`
- **Backup:** dump the DB and copy the uploads volume:
  ```bash
  docker compose exec db sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" qesseha' > backup-$(date +%F).sql
  docker run --rm -v myapp_uploads:/u -v "$PWD":/out alpine tar czf /out/uploads-$(date +%F).tgz -C /u .
  ```
- Certificates renew automatically; the `letsencrypt` volume must persist
  (don't `docker compose down -v`).

## Production security posture (what's already handled)

- TLS everywhere; HTTP redirects to HTTPS; HSTS header set at the edge
- MySQL has no public port; app talks to it over an internal-only network
- All secrets (DB, seed accounts, media-token HMAC) come from `.env`, not the image
- Session cookie is `Secure` + `HttpOnly` + `SameSite=Lax` in the prod profile
- App container runs as a non-root user
- `server.forward-headers-strategy=native` + Traefik's `X-Forwarded-For` means
  login rate-limiting sees real client IPs (Tomcat only trusts the header from
  the private-network proxy, so clients can't spoof it)
- SQL logging and Thymeleaf template reloading are off in the prod profile
