# kanban-frontend

Angular 22 standalone + signals shell for the Local AI Kanban board.

## Prerequisites

- **Node.js** — `22.22.3+` (LTS, pinned in architecture Stack and `.nvmrc`) or `24.15+`.
  - With nvm-windows or `fnm`, run `nvm use` (reads `.nvmrc`) in this folder.
  - `.npmrc` has `engine-strict=true` — a mismatched Node fails `npm install` loudly rather than warning.
- **npm** — bundled with Node.

The backend (`../backend`) must be reachable at `http://localhost:8080` for live data; the dev server's proxy forwards `/api/*` there.

## Install

```bash
npm install
```

## Develop

```bash
npm start
```

Serves at `http://localhost:4200`. `proxy.conf.json` proxies `/api/*` to `http://localhost:8080` so the SPA calls the backend without CORS friction (AD-12 covers both dev and container modes).

## Build (production)

```bash
npm run build
```

Emits to `dist/kanban-frontend/browser/` — matches `../backend/Dockerfile`'s `DIST_DIR` arg so the container build picks it up with no path override.

Initial-bundle budget: 500 kB warn / 1 MB error (see `angular.json`). Current bundle is ~160 kB total (JS + CSS).

## Dark mode

Two tracks, both carried in `src/styles/tokens.css`:

- `@media (prefers-color-scheme: dark)` — respects the user's OS setting out of the box.
- `:root[data-theme="dark"]` — manual override; wins over `@media` by specificity. The `ThemeToggle` writes this attribute (lands in story 7). A manual `data-theme="light"` on a dark OS still forces light.

## Tests

None in this phase — architecture AD-11 scopes testing to the backend tree. No Karma / Jasmine / Vitest config is shipped; no `*.spec.ts` files exist.

## Layering (AD-9)

- `core/api.service.ts` — the **only** class that injects `HttpClient`.
- `core/board.store.ts` — owns board state as signals (`_cards` writable, `cards` / `todo` / `inProgress` / `done` as readonly `computed()` projections). Components talk to the store, never to `HttpClient` or `ApiService` directly.
- `features/**` — presentational components (standalone).

## File layout

```
src/
  index.html, main.ts, styles.scss
  styles/tokens.css         — design tokens; light defaults on :root, dark overrides in both tracks
  app/
    app.component.ts        — top-level host; triggers BoardStore.load() in constructor
    app.config.ts           — provideZonelessChangeDetection + provideHttpClient(withFetch())
    core/
      models/card.ts        — CardStatus / Priority / Card mirror of the server CardDto
      api.service.ts
      board.store.ts
    features/
      header/app-header.component.{ts,scss}
      board/
        board.component.{ts,scss}
        column-container.component.{ts,scss}
        card.component.{ts,scss}
        priority-chip.component.{ts,scss}
```
