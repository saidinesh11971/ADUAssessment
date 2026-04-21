# AVB Closeout Importer

Upload an Excel workbook (`.xlsx`) and bulk-load its rows into the MySQL
`avb_closeout` table. Spring Boot backend (Java 21 + Apache POI), React + Vite
frontend, MySQL 5.7+ database.

- **Backend:** Spring Boot 3.3 on port `8080`
- **Frontend:** React 18 + Vite on port `5173` (dev)
- **Database:** MySQL, schema `adu`, table `avb_closeout`
- **Upload limit:** 25 MB per `.xlsx`

---

## Prerequisites

Pick one of the two setup paths below.

| Path | Needs |
|---|---|
| **Docker (one command)** | Docker Desktop 4+ with Compose |
| **Local dev** | Java 21, Maven 3.9+, Node 20+, npm 10+, MySQL 5.7+ |

---

## Path A — Docker Compose (recommended)

Brings up MySQL and the app (backend + built frontend) together.

```bash
docker compose up --build
```

Then open **http://localhost:8080**.

The `adu` schema and `avb_closeout` table are created automatically from
[`db/init.sql`](db/init.sql) on first run. MySQL data persists in the
`db-data` volume across restarts.

Stop and remove containers:

```bash
docker compose down          # keep data
docker compose down -v       # also wipe the DB volume
```

---

## Path B — Local development

Run the frontend dev server and backend separately so you get hot-reload on
both.

### 1. Start MySQL

Any MySQL 5.7+ instance works. Quickest option is just the DB from
compose:

```bash
docker compose up db
```

Or use an existing MySQL — create the schema and table once:

```sql
CREATE DATABASE adu;
USE adu;
SOURCE db/init.sql;
```

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

Defaults connect to `jdbc:mysql://localhost:3306/adu` as `root`/`root`.
Override via env vars (see [Configuration](#configuration)).

Backend is up when you see `Started ImporterApplication` — it listens on
**http://localhost:8080**.

### 3. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. The Vite dev server proxies nothing by
default — the frontend hits `http://localhost:8080/api/import` directly
and the backend's CORS list already whitelists `localhost:5173`.

---

## Configuration

All backend settings are read from environment variables (with the defaults
shown). See [`backend/src/main/resources/application.properties`](backend/src/main/resources/application.properties).

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:mysql://localhost:3306/adu?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | JDBC URL |
| `DB_USER` | `root` | MySQL user |
| `DB_PASSWORD` | `root` | MySQL password |
| `FRONTEND_DIR` | `file:../frontend/dist/` | Location of the built React bundle to serve |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated allow-list for `/api/**` |

The frontend reads `VITE_API_BASE` at build/dev time — leave it unset for
same-origin requests (the default).

---


## Project layout

```
evaluation/
├── backend/           Spring Boot app (Java 21, Maven)
│   └── src/main/java/com/adu/importer/
├── frontend/          React + Vite UI
│   └── src/
├── db/
│   └── init.sql       Creates the avb_closeout table
├── docker-compose.yml Brings up db + app
├── Dockerfile         Multi-stage build (frontend + backend + runtime)
└── item-export-test.xlsx  Sample workbook for testing
```

---

## Using the importer

1. Open the app in the browser.
2. Drag an `.xlsx` onto the dropzone (or click to browse). Max **25 MB**.
3. Click **Import**. The app:
   - Walks every sheet and picks up headers even when merged or not on row 1.
   - Upserts rows into `avb_closeout` (new SKUs insert, existing SKUs update).
   - Trims text values and strips commas from `Product Linq ID` before
     parsing as an integer.
4. A toast summarizes the outcome; the result card below lists processed
   sheets, skipped sheets, and per-row issues.

Required spreadsheet columns (extras are ignored):

| Spreadsheet column | DB column |
|---|---|
| `SKU` | `avb_sku` (PK) |
| `Brand` | `avb_brand` |
| `Product Linq ID` | `linq_id` |
| `CLOSEOUT_ID` | `closeout_id` |
| `CLOSEOUT_TYPE` | `closeout_type` |
| `Active Inactive Status-Default` | `avb_status` |

Try it against the included `item-export-test.xlsx`.

