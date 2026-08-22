# Person API template

Spring Boot starter backend with:

- credentials stored as BCrypt hashes in PostgreSQL table `users`
- stateless, expiring HS256 JWT bearer tokens
- role-based authorization (`USER` and `ADMIN`)
- authenticated person listing and admin-only person writes
- configurable CORS for a separate React frontend
- Flyway-managed database schema
- idempotent mock-person import from `static/persons.json`

## API

| Method | Path | Access |
|---|---|---|
| `POST` | `/api/auth/login` | Public |
| `GET` | `/api/auth/me` | Any authenticated user |
| `GET` | `/api/persons` | `USER` or `ADMIN` |
| `POST` | `/api/persons` | `ADMIN` |
| `PUT` | `/api/persons/{id}` | `ADMIN` |
| `DELETE` | `/api/persons/{id}` | `ADMIN` |

Login request:

```json
{
  "username": "admin",
  "password": "your-password"
}
```

The response contains `accessToken`, `tokenType`, `expiresIn`, `username`, and
`role`. Send the token on subsequent requests:

```http
Authorization: Bearer eyJ...
```

JWTs are stateless. Logging out means deleting the token in the React app.
There is no server-side session or revocation list in this starter.

## Run locally

Requirements: Java 17+ and Docker.

1. Start PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

2. Copy `.env.example` to a private environment file, replace every secret,
   and export the values into the shell. For example:

   ```bash
   set -a
   source .env
   set +a
   ```

   On Windows PowerShell, set the required JWT secret for the current shell
   before starting the API:

   ```powershell
   $bytes = New-Object byte[] 32
   [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
   $env:APP_JWT_SECRET = [Convert]::ToBase64String($bytes)
   ```

   The value must decode to at least 32 bytes. If it is missing or malformed,
   startup stops with a direct `APP_JWT_SECRET` configuration error.

3. On the first startup only, set
   `APP_BOOTSTRAP_ADMIN_ENABLED=true`. Start the API:

   ```bash
   ./mvnw spring-boot:run
   ```

4. After the administrator has been inserted into `users`, set
   `APP_BOOTSTRAP_ADMIN_ENABLED=false`. The initializer is idempotent and never
   replaces an existing password.

### Run without security and JWT

For local development, activate the `no-security` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=no-security
```

This profile permits every request, disables JWT encoder/decoder setup, and
removes the `/api/auth` login controller. It is intended only for local or
internal development and must not be used for an internet-facing deployment.

By default, startup also imports the bundled `src/main/resources/static/persons.json`.
Existing records are preserved and matching is case-insensitive by email. Set
`APP_BOOTSTRAP_PERSONS_ENABLED=false` when sample data should not be loaded,
especially in production.

Example:

```bash
curl -s http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"your-password"}'

curl -s http://localhost:8080/api/persons \
  -H "Authorization: Bearer $TOKEN"
```

## Authentication and authorization

`DatabaseUserDetailsService` loads the submitted username from `users`.
Spring Security verifies the stored BCrypt hash. After successful login,
`JwtService` signs a token containing the username, issuer, expiry, and `roles`
claim. The resource-server filter validates the signature, issuer, and
expiration on every protected request.

`SecurityConfig` disables server sessions and CSRF because this API uses bearer
tokens rather than cookie authentication. Method-level rules protect person
operations: both roles can read, while only `ADMIN` can modify data.

The application refuses to start unless `APP_JWT_SECRET` is set. It must be a
Base64-encoded value containing at least 32 random bytes:

```bash
openssl rand -base64 32
```

Use a different secret for every environment and rotate it if it is exposed.

## CORS and internet deployment

If React and the API are served through one public origin, for example
`https://example.com` and `https://example.com/api`, the browser treats requests
as same-origin and CORS is not involved.

If ports or subdomains differ, set every allowed React origin explicitly:

```text
APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://example.com,https://app.example.com
```

An origin contains scheme, host, and optional port, but no path or trailing
slash. The API allows `Authorization` and `Content-Type` headers. Credentials
are disabled because the JWT is sent in an authorization header, not a cookie.

For external access, terminate HTTPS at a reverse proxy and expose only ports
80/443 publicly. Proxy `/api` to `127.0.0.1:8080`; do not expose PostgreSQL or
the Spring Boot port directly. `server.forward-headers-strategy=framework`
honors standard proxy forwarding headers. Keep database credentials, the JWT
secret, and bootstrap password in environment variables or a secret manager.

## Database schema

Flyway creates:

- `users`: username, BCrypt password hash, role, enabled state, creation time
- `persons`: first name, last name, unique email

Hibernate uses `ddl-auto=validate`, so schema changes must be added as new
Flyway migrations instead of being generated automatically.

## Tests

Tests use an in-memory H2 database in PostgreSQL compatibility mode:

```bash
./mvnw test
```

They cover login, JWT-authenticated reads, anonymous rejection, role
authorization, invalid credentials, React-origin CORS preflight, and
idempotent mock-person import.
