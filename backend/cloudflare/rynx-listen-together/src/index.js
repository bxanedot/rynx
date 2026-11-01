// bxane
import { normalizeCode } from "./party-core.js";
import { PartyRoom } from "./party-room.js";
import { CreateLimiter } from "./create-limiter.js";

const CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
const MAX_CREATE_ATTEMPTS = 8;
const LOCATION_HINT = "weur";

function json(data, status = 200) {
  return Response.json(data, {
    status,
    headers: { "Cache-Control": "no-store" },
  });
}

function fail(code, message, status) {
  return json({ error: code, message }, status);
}

function randomCode() {
  const bytes = crypto.getRandomValues(new Uint8Array(6));
  return Array.from(bytes, (byte) => CODE_ALPHABET[byte % CODE_ALPHABET.length]).join("");
}

async function hash(value) {
  const bytes = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(bytes), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

export { PartyRoom, CreateLimiter };

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/healthz") return json({ ok: true });
    if (request.method === "GET" && url.pathname === "/api/time") return json({ serverMs: Date.now() });

    if (request.method === "POST" && url.pathname === "/api/parties") {
      const clientIp = request.headers.get("CF-Connecting-IP") ?? "local";
      const limiterId = env.CREATE_LIMITER.idFromName(await hash(clientIp));
      const limit = await env.CREATE_LIMITER.get(limiterId, { locationHint: LOCATION_HINT })
        .fetch("https://limiter/hit", { method: "POST" });
      if (!limit.ok) return limit;
      for (let attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
        const code = randomCode();
        const id = env.PARTIES.idFromName(code);
        const room = env.PARTIES.get(id, { locationHint: LOCATION_HINT });
        const internal = new Request(`https://party/_create?code=${code}`, request.clone());
        const response = await room.fetch(internal);
        if (response.status !== 409) return response;
      }
      return fail("create_unavailable", "Could not reserve a party code. Try again.", 503);
    }

    const wsRoute = url.pathname.match(/^\/ws\/parties\/([^/]+)$/);
    const apiRoute = url.pathname.match(/^\/api\/parties\/([^/]+)(?:\/(?:join|preview|leave))?$/);
    const route = wsRoute ?? apiRoute;
    if (!route) return fail("not_found", "That endpoint does not exist.", 404);
    let code;
    try { code = normalizeCode(decodeURIComponent(route[1])); } catch { code = null; }
    if (!code) return fail("bad_code", "A party code is six letters or digits.", 400);
    return env.PARTIES.get(env.PARTIES.idFromName(code), { locationHint: LOCATION_HINT }).fetch(request);
  },
};
