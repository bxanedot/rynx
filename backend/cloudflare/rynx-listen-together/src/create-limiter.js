// bxane
import { DurableObject } from "cloudflare:workers";

const WINDOW_MS = 60_000;
const MAX_CREATES = 2;

export class CreateLimiter extends DurableObject {
  async fetch(request) {
    if (new URL(request.url).pathname !== "/hit" || request.method !== "POST") {
      return new Response(null, { status: 404 });
    }
    return this.ctx.blockConcurrencyWhile(async () => {
      const now = Date.now();
      const recent = (await this.ctx.storage.get("hits") ?? []).filter((time) => now - time < WINDOW_MS);
      if (recent.length >= MAX_CREATES) {
        await this.ctx.storage.put("hits", recent);
        return Response.json({ error: "rate_limited", message: "Too many parties were created. Try again in a minute." }, { status: 429 });
      }
      recent.push(now);
      await this.ctx.storage.put("hits", recent);
      return Response.json({ ok: true });
    });
  }
}
