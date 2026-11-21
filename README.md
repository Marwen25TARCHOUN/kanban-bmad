# kanban-bmad

A local AI-augmented Kanban: three columns, one AI breakdown button, Spring Boot 4.1 + Angular 22. One-week portfolio build; everything runs on your machine except one HTTPS call to Anthropic.

## Prerequisites

- **Docker Desktop** on Windows or Mac — or Docker Engine + Compose v2 plugin on Linux. Nothing else.
- Optionally, an Anthropic API key — the app works without it (the AI breakdown bar just hides; manual card management keeps working).

## Start (containerized, recommended)

**Windows** — PowerShell:

```powershell
.\start-windows.ps1
```

or just double-click `start.bat`.

**Mac / Linux**:

```bash
./start.sh
```

Either way: builds the two images, starts the stack, prints the URLs.

- Frontend: <http://localhost:4200>
- Backend health: <http://localhost:8080/actuator/health>

First run builds from source (~1–3 min). Subsequent starts reuse the images (~10 s).

### Enable AI breakdown

Copy `.env.example` to `.env` and paste your key:

```
ANTHROPIC_API_KEY=sk-ant-...
```

Then re-run the start script. Without a key the app still works — you just add cards manually.

## Stop

**Windows**: `.\stop-windows.ps1` or `stop.bat`

**Mac / Linux**: `./stop.sh`

Board data survives in the `kanban-data` Compose volume. To wipe the data:

```
.\stop-windows.ps1 -Purge       # Windows
./stop.sh --purge               # Mac/Linux
```

Both prompt for `yes` before destroying the volume.

## Development mode (hot reload)

Secondary path, for working on the code — two terminals:

```bash
# Terminal 1 — backend
cd backend && ./mvnw spring-boot:run

# Terminal 2 — frontend
cd frontend && npm start
```

Same URLs (`:8080` for the API, `:4200` for the SPA). CORS is pre-configured for the two-origin dev case; the containerized path proxies `/api` through nginx so CORS is a no-op there.

## Architecture

See [`_bmad-output/planning-artifacts/architecture/architecture-kanban-bmad-2026-11-20/`](_bmad-output/planning-artifacts/architecture/architecture-kanban-bmad-2026-11-20/):

- `ARCHITECTURE-SPINE.md` — the enforceable invariants (15 ADs, data contracts, dependency rules)
- `solution-design.md` — the narrative for reviewers (context, six calls that shaped this, non-goals, risks)

The PRD and UX docs live alongside under `_bmad-output/planning-artifacts/`.
