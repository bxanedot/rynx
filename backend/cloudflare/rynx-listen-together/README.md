<!-- bxane -->
# Rynx Listen Together Worker

Cloudflare Worker and Durable Object backend for Rynx Listen Together. Party rooms use SQLite-backed Durable Objects so each room owns its member list, playback anchor, shared queue, and WebSocket connections. New room and create-limit objects use Cloudflare's `weur` placement hint (Western Europe), close to Switzerland.

## Local

```powershell
npm install
npm test
npm run dev
```

The local Worker serves `/healthz`, `/api/time`, `/api/parties`, `/api/parties/{code}/join`, `/api/parties/{code}/preview`, `/api/parties/{code}/leave`, and `/ws/parties/{code}`.

## Deploy

```powershell
npm run deploy
```

The production URL is `https://rynx-listen-together.bxane.workers.dev` when deployed in the `bxane.workers.dev` account.

To connect GitHub deployment, set the Cloudflare Workers Builds root directory to `backend/cloudflare/rynx-listen-together` in `bxanedot/rynx`; use `npm install` as the install command and `npm run deploy` as the deploy command.

## Limits and privacy

- A party holds up to five devices, with at most 25 upcoming tracks.
- Party codes are six characters; create requests are limited to two per source IP per minute.
- Members disconnected for 45 seconds release their slots. Empty rooms expire after two minutes and every room expires after 12 hours.
- Access tokens are random and stored only as SHA-256 hashes. Device IDs are kept in private Durable Object storage and are never sent in party snapshots.
- The Worker receives the app's opaque user ID, display name, and optional HTTPS avatar URL. It does not receive or verify Google credentials.
- Browser-origin WebSocket requests are denied unless their exact origin is listed in the `ALLOWED_ORIGINS` Worker variable. Native app requests do not send an Origin header.
- Cloudflare applies the location hint only when a Durable Object is first created; existing objects are not moved later.
