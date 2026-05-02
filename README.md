# ChatCommerce Backend

Spring Boot backend for a laptop shopping chatbot with session-based auth, Gemini-powered chat, PostgreSQL persistence, and Redis-backed caching.

## Stack

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Docker / Docker Compose
- Google Gemini API

## Features

- Creates guest sessions with HTTP-only cookies
- Stores chat history per session
- Uses Gemini to extract preferences and generate replies
- Returns product suggestions based on user requirements
- Supports cart operations per session
- Runs locally or in Docker

## Project Structure

- `src/main/java/com/server/chatbot/controller` - API endpoints
- `src/main/java/com/server/chatbot/service` - chat, session, cart, and Gemini logic
- `src/main/resources/application.properties` - Spring config mapped from env vars
- `.env.example` - sample environment variables
- `docker-compose.yml` - local and server container run config
- `Dockerfile` - multi-stage image build

## Environment Variables

Copy `.env.example` to `.env` and fill in real values.

```env
DB_URL=jdbc:postgresql://db-host:5432/db-name
DB_USERNAME=db-user
DB_PASSWORD=db-password

GEMINI_API_KEY=api-key
GEMINI_OPENAI_URL=https://generativelanguage.googleapis.com/v1beta/openai
GEMINI_NATIVE_URL=https://generativelanguage.googleapis.com/v1beta
GEMINI_MODEL=gemini-2.5-flash

REDIS_URL=rediss://default:redis-password@redis-host:6379
REDIS_PORT=6379
REDIS_PASSWORD=redis-password
REDIS_TIMEOUT=2s
REDIS_SSL_ENABLED=true

JWT_SECRET=jwt-secret
APP_COOKIES_SECURE=true
APP_COOKIES_SAME_SITE=None
APP_CORS_ALLOWED_ORIGIN=https://frontend-domain.com
```

## Local Run

### Maven

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

### Tests

```bash
./mvnw test
```

The test suite uses H2 from `src/test/resources/application.properties`, so it does not depend on your production database.

## Docker

Build and run:

```bash
docker compose up -d --build
```

View logs:

```bash
docker compose logs -f backend
```

Stop:

```bash
docker compose down
```

## API Overview

### Session

- `POST /api/session` - creates a session and sets auth cookies
- `POST /api/refresh` - refreshes the session cookies

### Chat

- `POST /api/chat` - sends a chat message and returns chatbot reply plus products
- `POST /api/chat/history` - returns chat history for the current session

### Cart

- `GET /api/cart` - fetches the current cart
- `POST /api/cart/add` - adds a product to cart
- `DELETE /api/cart/clear` - clears the cart

### Internal Test Endpoints

- `GET /api/test-search`
- `GET /api/test-extract`

These endpoints require authentication and are mainly useful during development.

## Session and Cookie Flow

This backend uses cookie-based session auth.

1. Frontend calls `POST /api/session`
2. Backend sets `op_access` and `op_refresh` cookies
3. Frontend sends later requests with credentials enabled
4. Protected endpoints read the session from the access cookie

Frontend requests must include credentials:

```js
fetch("https://your-backend-domain/api/session", {
  method: "POST",
  credentials: "include"
})
```

For cross-site production setups such as Vercel frontend plus custom backend domain:

- `APP_COOKIES_SECURE=true`
- `APP_COOKIES_SAME_SITE=None`
- `APP_CORS_ALLOWED_ORIGIN` must match the frontend origin exactly
- backend should be served over HTTPS

## Production Notes

- Do not commit `.env`
- Rotate secrets immediately if any key is exposed
- Keep `JWT_SECRET` at least 32 characters long
- If Gemini returns `403`, check whether the API key is valid, enabled, and not revoked
- If the frontend is hosted on HTTPS, the backend must also be on HTTPS

## Common Issues

### `502 Bad Gateway` on `/api/chat`

Usually caused by Gemini configuration:

- missing `GEMINI_API_KEY`
- invalid or revoked Gemini key
- Gemini API disabled or blocked for the project

Check:

```bash
docker compose logs -f backend
```

### Cookies not being sent from frontend

Check:

- frontend uses `credentials: "include"`
- backend CORS origin matches the frontend origin
- `APP_COOKIES_SECURE=true` for HTTPS
- `APP_COOKIES_SAME_SITE=None` for cross-site requests

### Browser shows mixed content error

This happens when an HTTPS frontend calls an HTTP backend. Serve the backend over HTTPS.

## Deployment

Typical EC2 flow:

```bash
git clone <repo>
cd chatbot
nano .env
docker compose up -d --build
docker compose logs -f backend
```

If you are using Nginx in front of the container, proxy requests to:

```text
http://127.0.0.1:8080
```

## License

Add your preferred license here.
