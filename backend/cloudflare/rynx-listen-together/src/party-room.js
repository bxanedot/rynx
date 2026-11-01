// bxane
import { DurableObject } from "cloudflare:workers";
import {
  MAX_MEMBERS,
  MAX_QUEUE_LENGTH,
  MAX_UPCOMING,
  PLAY_LEAD_MS,
  addQueueTracks,
  cleanTrack,
  currentPosition,
  makeRoom,
  moveQueue,
  normalizeCode,
  safeAvatarUrl,
  validIdentity,
} from "./party-core.js";

const MAX_BODY_BYTES = 16_384;
const MAX_FRAME_BYTES = 16_384;
const DISCONNECT_GRACE_MS = 45_000;
const EMPTY_ROOM_TTL_MS = 120_000;
const MAX_ROOM_AGE_MS = 43_200_000;
const HEARTBEAT_MS = 5_000;
const MAX_FRAME_RATE = 30;
const MAX_CONTROL_RATE = 25;
const CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
const PLAYBACK_ACTIONS = new Set([
  "play", "pause", "seek", "setTrack", "setQueue", "queueAdd", "queueRemove", "queueClear",
  "queueMove", "next", "previous", "setAutoplay",
]);

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" },
  });
}

function fail(code, message, status) {
  return json({ error: code, message }, status);
}

async function readJson(request) {
  if (Number(request.headers.get("Content-Length")) > MAX_BODY_BYTES) {
    throw Object.assign(new Error("Request body is too large."), { status: 413, code: "body_too_large" });
  }
  const reader = request.body?.getReader();
  if (!reader) return {};
  const chunks = [];
  let size = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    size += value.byteLength;
    if (size > MAX_BODY_BYTES) {
      await reader.cancel();
      throw Object.assign(new Error("Request body is too large."), { status: 413, code: "body_too_large" });
    }
    chunks.push(value);
  }
  try {
    const bytes = new Uint8Array(size);
    let offset = 0;
    for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch {
    throw Object.assign(new Error("Request body must be valid JSON."), { status: 400, code: "invalid_json" });
  }
}

function safeName(value) {
  return value.trim().replace(/[\u0000-\u001f\u007f]/g, "").slice(0, 80) || "Listener";
}

function createMember(body, isHost, now) {
  return {
    memberId: crypto.randomUUID(),
    userId: body.userId.trim(),
    displayName: safeName(body.displayName),
    avatarUrl: safeAvatarUrl(body.avatarUrl),
    isHost,
    connected: false,
    joinedAtMs: now,
    lastSeenMs: now,
  };
}

function bearerToken(request) {
  const value = request.headers.get("Authorization") ?? "";
  const match = value.match(/^Bearer\s+([A-Za-z0-9_-]{40,})$/i);
  return match?.[1] ?? null;
}

function randomToken() {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/, "");
}

async function tokenHash(token) {
  const bytes = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token));
  return Array.from(new Uint8Array(bytes), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function randomCode() {
  const bytes = crypto.getRandomValues(new Uint8Array(6));
  return Array.from(bytes, (byte) => CODE_ALPHABET[byte % CODE_ALPHABET.length]).join("");
}

function publicMember(member) {
  return {
    memberId: member.memberId,
    userId: member.userId,
    displayName: member.displayName,
    avatarUrl: member.avatarUrl,
    isHost: member.isHost,
    connected: member.connected,
    joinedAtMs: member.joinedAtMs,
    lastSeenMs: member.lastSeenMs,
  };
}

function snapshot(room, now) {
  return {
    code: room.code,
    createdAtMs: room.createdAtMs,
    maxMembers: room.maxMembers,
    hostOnlyControl: room.hostOnlyControl,
    members: room.members.map(publicMember),
    playback: { ...room.playback, effectivePositionMs: currentPosition(room.playback, now) },
    queue: { ...room.queue },
    serverMs: now,
  };
}

function preview(room) {
  return {
    code: room.code,
    hostName: room.members.find((member) => member.isHost)?.displayName ?? "",
    memberCount: room.members.length,
    maxMembers: room.maxMembers,
    isFull: room.members.length >= room.maxMembers,
    members: room.members.map((member) => ({
      displayName: member.displayName,
      avatarUrl: member.avatarUrl,
      isHost: member.isHost,
    })),
  };
}

function bearerMember(privateState, tokenHashValue) {
  if (!tokenHashValue) return null;
  return Object.entries(privateState.tokenHashes).find(([, hash]) => hash === tokenHashValue)?.[0] ?? null;
}

function transferHost(room) {
  if (room.members.some((member) => member.isHost)) return;
  const next = room.members.slice().sort((a, b) => a.joinedAtMs - b.joinedAtMs)[0];
  if (next) next.isHost = true;
}

function memberSockets(ctx, memberId) {
  return ctx.getWebSockets().filter((socket) => socket.readyState === WebSocket.OPEN &&
    socket.deserializeAttachment()?.memberId === memberId);
}

function safePosition(value) {
  return Number.isSafeInteger(value) && value >= 0 && value <= 86_400_000 ? value : null;
}

function playbackIndex(room) {
  const track = room.playback.track;
  if (!track) return -1;
  const hint = Number.isInteger(room.playback.queueIndex) ? room.playback.queueIndex : room.queue.index;
  if (hint >= 0 && room.queue.items[hint]?.videoId === track.videoId) return hint;
  let nearest = -1;
  let distance = Number.POSITIVE_INFINITY;
  room.queue.items.forEach((item, index) => {
    if (item.videoId !== track.videoId) return;
    const candidate = Math.abs(index - Math.max(0, hint));
    if (candidate < distance) { nearest = index; distance = candidate; }
  });
  return nearest;
}

function commitQueue(room, items, index, member, now) {
  room.queue = { seq: room.queue.seq + 1, index, items };
  room.playback = {
    ...room.playback,
    seq: room.playback.seq + 1,
    queueSeq: room.queue.seq,
    queueLength: items.length,
    queueIndex: index,
    updatedBy: member.memberId,
    updatedAtMs: now,
  };
  room.serverMs = now;
}

function commitPlayback(room, next, now) {
  room.playback = {
    ...next,
    queueSeq: room.queue.seq,
    queueLength: room.queue.items.length,
    effectivePositionMs: currentPosition(next, now),
  };
  room.serverMs = now;
}

function errorFrame(socket, code, message, status = 400) {
  sendFrame(socket, { type: "error", error: code, message, statusCode: status });
}

function sendFrame(socket, frame) {
  if (socket.readyState !== WebSocket.OPEN) return;
  try { socket.send(JSON.stringify(frame)); } catch { /* Closed between the readiness check and send. */ }
}

export class PartyRoom extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.room = null;
    this.privateState = { devices: {}, tokenHashes: {} };
    this.ready = ctx.blockConcurrencyWhile(async () => {
      this.room = await ctx.storage.get("room") ?? null;
      this.privateState = await ctx.storage.get("private") ?? { devices: {}, tokenHashes: {} };
    });
  }

  async locked(operation) {
    await this.ready;
    return this.ctx.blockConcurrencyWhile(operation);
  }

  async persist() {
    await this.ctx.storage.transaction(async (txn) => {
      await txn.put("room", this.room);
      await txn.put("private", this.privateState);
    });
  }

  async fetch(request) {
    await this.ready;
    const url = new URL(request.url);
    if (url.pathname === "/_create" && request.method === "POST") return this.create(request);

    const upgrade = request.headers.get("Upgrade")?.toLowerCase() === "websocket";
    const route = url.pathname.match(upgrade
      ? /^\/ws\/parties\/([^/]+)$/
      : /^\/api\/parties\/([^/]+)(?:\/(join|preview|leave))?$/);
    if (!route) return fail("not_found", "That party endpoint does not exist.", 404);
    let code;
    try { code = normalizeCode(decodeURIComponent(route[1])); } catch { code = null; }
    if (!code) return fail("bad_code", "A party code is six letters or digits.", 400);
    if (upgrade) return this.connect(request, code);
    const action = route[2] ?? "snapshot";
    if (action === "join" && request.method === "POST") return this.join(request, code);
    if (action === "preview" && request.method === "GET") return this.preview(code);
    if (action === "leave" && request.method === "POST") return this.leave(request, code);
    if (action === "snapshot" && request.method === "GET") return this.getSnapshot(request, code);
    return fail("method_not_allowed", "That method is not allowed for this endpoint.", 405);
  }

  async create(request) {
    let body;
    try { body = await readJson(request); } catch (error) {
      return fail(error.code ?? "invalid_json", error.message, error.status ?? 400);
    }
    if (!validIdentity(body)) return fail("no_identity", "Sign in and choose a display name first.", 422);
    const maxMembers = body.maxMembers === undefined ? MAX_MEMBERS : body.maxMembers;
    if (!Number.isInteger(maxMembers) || maxMembers < 2 || maxMembers > MAX_MEMBERS) {
      return fail("invalid_capacity", `A party can hold 2 to ${MAX_MEMBERS} devices.`, 422);
    }
    const token = randomToken();
    const hash = await tokenHash(token);
    const now = Date.now();
    const roomCode = normalizeCode(new URL(request.url).searchParams.get("code"));
    if (!roomCode) return fail("bad_code", "A party code is six letters or digits.", 400);
    return this.locked(async () => {
      if (this.room) return fail("code_collision", "That code is already in use.", 409);
      const room = makeRoom(roomCode, now);
      room.maxMembers = maxMembers;
      room.playback.autoplayEnabled = body.autoplayEnabled === true;
      const member = createMember(body, true, now);
      room.members = [member];
      this.room = room;
      this.privateState = {
        devices: { [body.deviceId.trim()]: member.memberId },
        tokenHashes: { [member.memberId]: hash },
      };
      await this.persist();
      await this.scheduleAlarm(now);
      return json({ code: room.code, token, you: publicMember(member), party: snapshot(room, now), serverMs: now }, 201);
    });
  }

  async join(request, code) {
    let body;
    try { body = await readJson(request); } catch (error) {
      return fail(error.code ?? "invalid_json", error.message, error.status ?? 400);
    }
    if (!validIdentity(body)) return fail("no_identity", "Sign in and choose a display name first.", 422);
    const token = randomToken();
    const hash = await tokenHash(token);
    const now = Date.now();
    let change = false;
    const response = await this.locked(async () => {
      if (!this.room || this.room.code !== code) return fail("no_such_party", "That party has ended or does not exist.", 404);
      change = this.pruneDisconnected(now);
      const deviceId = body.deviceId.trim();
      let memberId = this.privateState.devices[deviceId];
      let member = memberId ? this.room.members.find((value) => value.memberId === memberId) : null;
      if (member) {
        for (const socket of memberSockets(this.ctx, member.memberId)) socket.close(4000, "device rejoined");
        member.userId = body.userId.trim();
        member.displayName = safeName(body.displayName);
        member.avatarUrl = safeAvatarUrl(body.avatarUrl);
        member.connected = false;
        member.lastSeenMs = now;
      } else {
        if (this.room.members.length >= this.room.maxMembers) {
          if (change) { await this.persist(); await this.scheduleAlarm(now); }
          return fail("party_full", "This party has reached its device limit.", 409);
        }
        member = createMember(body, false, now);
        memberId = member.memberId;
        this.room.members.push(member);
        this.privateState.devices[deviceId] = memberId;
      }
      this.privateState.tokenHashes[member.memberId] = hash;
      this.room.emptySinceMs = null;
      await this.persist();
      await this.scheduleAlarm(now);
      change = true;
      return json({ code, token, you: publicMember(member), party: snapshot(this.room, now), serverMs: now });
    });
    if (change && response.ok) this.broadcastMembers(Date.now());
    return response;
  }

  async preview(code) {
    const now = Date.now();
    let changed = false;
    const response = await this.locked(async () => {
      if (!this.room || this.room.code !== code) return fail("no_such_party", "That party has ended or does not exist.", 404);
      changed = this.pruneDisconnected(now);
      if (changed) { await this.persist(); await this.scheduleAlarm(now); }
      return json(preview(this.room));
    });
    if (changed) this.broadcastMembers(now);
    return response;
  }

  async getSnapshot(request, code) {
    const hash = await tokenHash(bearerToken(request) ?? "");
    const now = Date.now();
    return this.locked(async () => {
      if (!this.room || this.room.code !== code) return fail("no_such_party", "That party has ended or does not exist.", 404);
      const memberId = bearerMember(this.privateState, hash);
      if (!memberId || !this.room.members.some((member) => member.memberId === memberId)) {
        return fail("bad_token", "This device is no longer in the party.", 401);
      }
      return json(snapshot(this.room, now));
    });
  }

  async leave(request, code) {
    const hash = await tokenHash(bearerToken(request) ?? "");
    const now = Date.now();
    let changed = false;
    const response = await this.locked(async () => {
      if (!this.room || this.room.code !== code) return fail("no_such_party", "That party has ended or does not exist.", 404);
      const memberId = bearerMember(this.privateState, hash);
      if (!memberId) return fail("bad_token", "This device is no longer in the party.", 401);
      const member = this.room.members.find((value) => value.memberId === memberId);
      if (!member) return fail("bad_token", "This device is no longer in the party.", 401);
      for (const socket of memberSockets(this.ctx, memberId)) {
        sendFrame(socket, { type: "bye", reason: "left" });
        socket.close(1000, "left party");
      }
      this.removeMember(member);
      transferHost(this.room);
      if (this.room.members.length === 0) this.room.emptySinceMs = now;
      this.room.serverMs = now;
      await this.persist();
      await this.scheduleAlarm(now);
      changed = true;
      return json({ ok: true });
    });
    if (changed) this.broadcastMembers(now);
    return response;
  }

  async connect(request, code) {
    if (request.headers.get("Upgrade")?.toLowerCase() !== "websocket") {
      return fail("upgrade_required", "Use a WebSocket connection for this endpoint.", 426);
    }
    const origin = request.headers.get("Origin");
    if (origin && !this.allowedOrigin(origin)) return fail("origin_not_allowed", "This app origin is not allowed.", 403);
    const hash = await tokenHash(bearerToken(request) ?? "");
    const now = Date.now();
    return this.locked(async () => {
      if (!this.room || this.room.code !== code) return fail("no_such_party", "That party has ended or does not exist.", 404);
      const memberId = bearerMember(this.privateState, hash);
      const member = this.room.members.find((value) => value.memberId === memberId);
      if (!member) return fail("bad_token", "This device is no longer in the party.", 401);
      const pair = new WebSocketPair();
      const [client, server] = Object.values(pair);
      server.serializeAttachment({ memberId, frameTimes: [], controlTimes: [] });
      this.ctx.acceptWebSocket(server, [memberId]);
      member.connected = true;
      member.lastSeenMs = now;
      this.room.emptySinceMs = null;
      this.room.serverMs = now;
      await this.persist();
      await this.scheduleAlarm(now);
      sendFrame(server, {
        type: "welcome",
        you: publicMember(member),
        party: snapshot(this.room, now),
        serverMs: now,
      });
      this.broadcastMembers(now);
      return new Response(null, { status: 101, webSocket: client });
    });
  }

  allowedOrigin(origin) {
    const allowed = (this.env.ALLOWED_ORIGINS ?? "").split(",").map((value) => value.trim()).filter(Boolean);
    return allowed.includes(origin);
  }

  async webSocketMessage(socket, message) {
    await this.ready;
    const text = typeof message === "string" ? message : new TextDecoder().decode(message);
    if (new TextEncoder().encode(text).byteLength > MAX_FRAME_BYTES) {
      errorFrame(socket, "frame_too_large", "That message is too large.", 413);
      return;
    }
    const now = Date.now();
    const attachment = socket.deserializeAttachment() ?? { memberId: "", frameTimes: [], controlTimes: [] };
    attachment.frameTimes = attachment.frameTimes.filter((time) => now - time < 1_000);
    if (attachment.frameTimes.length >= MAX_FRAME_RATE) {
      socket.serializeAttachment(attachment);
      errorFrame(socket, "rate_limited", "Too many messages; slow down.", 429);
      return;
    }
    attachment.frameTimes.push(now);
    let frame;
    try { frame = JSON.parse(text); } catch {
      socket.serializeAttachment(attachment);
      errorFrame(socket, "invalid_frame", "That message is not valid JSON.");
      return;
    }
    if (!frame || typeof frame !== "object" || Array.isArray(frame)) {
      socket.serializeAttachment(attachment);
      errorFrame(socket, "invalid_frame", "That message must be a JSON object.");
      return;
    }
    if (frame.type === "control") {
      attachment.controlTimes = attachment.controlTimes.filter((time) => now - time < 1_000);
      if (attachment.controlTimes.length >= MAX_CONTROL_RATE) {
        socket.serializeAttachment(attachment);
        errorFrame(socket, "rate_limited", "Too many controls; slow down.", 429);
        return;
      }
      attachment.controlTimes.push(now);
    }
    socket.serializeAttachment(attachment);
    if (frame.type === "ping") {
      const result = await this.locked(async () => {
        const member = this.room?.members.find((value) => value.memberId === attachment.memberId);
        if (!member) return null;
        member.lastSeenMs = now;
        this.room.serverMs = now;
        await this.persist();
        await this.scheduleAlarm(now);
        return { type: "pong", clientMs: Number.isSafeInteger(frame.clientMs) ? frame.clientMs : 0, serverMs: now };
      });
      if (result) sendFrame(socket, result);
      else { errorFrame(socket, "bad_token", "This device is no longer in the party.", 401); socket.close(4401, "no membership"); }
      return;
    }
    if (frame.type === "report") {
      await this.locked(async () => {
        const member = this.room?.members.find((value) => value.memberId === attachment.memberId);
        if (!member) return;
        member.lastSeenMs = now;
        await this.persist();
        await this.scheduleAlarm(now);
      });
      return;
    }
    if (frame.type === "sync") {
      await this.locked(async () => {
        const member = this.room?.members.find((value) => value.memberId === attachment.memberId);
        if (!member) return;
        sendFrame(socket, { type: "welcome", you: publicMember(member), party: snapshot(this.room, now), serverMs: now });
      });
      return;
    }
    if (frame.type === "syncQueue") {
      await this.locked(async () => {
        if (this.room) sendFrame(socket, { type: "queue", queue: this.room.queue, serverMs: now });
      });
      return;
    }
    if (frame.type !== "control" || typeof frame.action !== "string") {
      errorFrame(socket, "unknown_message", "That message type is not supported.");
      return;
    }
    const result = await this.control(socket, attachment.memberId, frame, now);
    if (result.error) errorFrame(socket, result.error.code, result.error.message, result.error.status);
    if (result.kickedMemberId) {
      for (const other of memberSockets(this.ctx, result.kickedMemberId)) {
        sendFrame(other, { type: "bye", reason: "kicked" });
        other.close(4003, "removed by host");
      }
    }
    if (result.membersChanged) this.broadcastMembers(now);
    if (result.queueChanged) this.broadcastQueue(now);
    if (result.playbackChanged) this.broadcastState(now);
  }

  async control(socket, memberId, frame, now) {
    return this.locked(async () => {
      const room = this.room;
      const member = room?.members.find((value) => value.memberId === memberId);
      if (!room || !member) return { error: { code: "bad_token", message: "This device is no longer in the party.", status: 401 } };
      const action = frame.action;
      const hostAction = action === "setMaxMembers" || action === "kick" || action === "setHostOnlyControl";
      if (hostAction && !member.isHost) return { error: { code: "host_only", message: "Only the host can change that setting.", status: 403 } };
      if (room.hostOnlyControl && PLAYBACK_ACTIONS.has(action) && !member.isHost) {
        return { error: { code: "host_only", message: "The host has taken control of this party.", status: 403 } };
      }
      const result = this.applyControl(room, member, frame, now);
      if (result.error) return result;
      room.serverMs = now;
      await this.persist();
      await this.scheduleAlarm(now);
      return result;
    });
  }

  applyControl(room, member, frame, now) {
    const action = frame.action;
    let playbackChanged = false;
    let queueChanged = false;
    let membersChanged = false;
    let kickedMemberId = null;
    let playback = room.playback;
    const position = safePosition(frame.positionMs);
    const current = currentPosition(playback, now);
    const basePlayback = (values = {}) => ({
      ...playback,
      ...values,
      seq: playback.seq + 1,
      updatedBy: member.memberId,
      updatedAtMs: now,
    });

    if (action === "play") {
      if (frame.positionMs !== undefined && position === null) return badAction("That play position is invalid.");
      commitPlayback(room, basePlayback({
        isPlaying: true,
        positionMs: position ?? current,
        anchorMs: now + PLAY_LEAD_MS,
      }), now);
      playbackChanged = true;
    } else if (action === "pause") {
      if (frame.positionMs !== undefined && position === null) return badAction("That pause position is invalid.");
      commitPlayback(room, basePlayback({ isPlaying: false, positionMs: position ?? current, anchorMs: now }), now);
      playbackChanged = true;
    } else if (action === "seek") {
      if (position === null) return badAction("That seek position is invalid.");
      commitPlayback(room, basePlayback({ positionMs: position, anchorMs: playback.isPlaying ? now + PLAY_LEAD_MS : now }), now);
      playbackChanged = true;
    } else if (action === "setTrack") {
      let track;
      try { track = cleanTrack(frame.track); } catch (error) { return badAction(error.message, error.code); }
      if (frame.positionMs !== undefined && position === null) return badAction("That track position is invalid.");
      if (frame.isPlaying !== undefined && typeof frame.isPlaying !== "boolean") return badAction("Playing state must be true or false.");
      const nextIndex = nearestTrackIndex(room.queue.items, track.videoId, playbackIndex(room));
      commitPlayback(room, basePlayback({
        track,
        isPlaying: frame.isPlaying ?? true,
        positionMs: position ?? 0,
        anchorMs: (frame.isPlaying ?? true) ? now + PLAY_LEAD_MS : now,
        queueIndex: nextIndex,
        startedBy: member.memberId,
        startedByName: member.displayName,
      }), now);
      playbackChanged = true;
    } else if (action === "setQueue") {
      if (!Array.isArray(frame.queue)) return badAction("The queue must be a list.");
      if (frame.queue.length > MAX_QUEUE_LENGTH) return badAction("The queue is too long.", "queue_full", 409);
      let items;
      try { items = frame.queue.map(cleanTrack); } catch (error) { return badAction(error.message, error.code); }
      const suppliedIndex = Number.isInteger(frame.queueIndex) ? frame.queueIndex : -1;
      if (suppliedIndex < -1 || suppliedIndex >= items.length) return badAction("The queue index is invalid.");
      const active = playback.track ? nearestTrackIndex(items, playback.track.videoId, suppliedIndex) : -1;
      const index = active >= 0 ? active : suppliedIndex;
      if (items.length - (index >= 0 ? index + 1 : 0) > MAX_UPCOMING) {
        return badAction("The upcoming queue is full.", "queue_full", 409);
      }
      commitQueue(room, items, index, member, now);
      queueChanged = playbackChanged = true;
    } else if (action === "queueAdd") {
      if (!Array.isArray(frame.tracks) || frame.tracks.length > 100) return badAction("Tracks must be a list of at most 100 items.");
      let result;
      try { result = addQueueTracks(room.queue.items, playbackIndex(room), frame.tracks, frame.playNext === true); }
      catch (error) { return badAction(error.message, error.code); }
      if (result.addedCount === 0 && frame.tracks.length > 0) {
        const allAutoplayDuplicates = frame.tracks.every((track) => track?.fromAutoplay === true &&
          room.queue.items.slice(playbackIndex(room) + 1).some((queued) => queued.videoId === track.videoId && queued.fromAutoplay));
        if (!allAutoplayDuplicates && room.queue.items.length >= MAX_QUEUE_LENGTH) return badAction("The upcoming queue is full.", "queue_full", 409);
      }
      if (result.addedCount > 0) {
        commitQueue(room, result.items, result.index, member, now);
        queueChanged = playbackChanged = true;
      }
    } else if (action === "queueRemove") {
      if (typeof frame.videoId !== "string" || !frame.videoId) return badAction("Choose a track to remove.");
      const activeIndex = playbackIndex(room);
      const removeIndex = nearestTrackIndex(room.queue.items, frame.videoId, activeIndex >= 0 ? activeIndex + 1 : 0);
      if (removeIndex < 0) return badAction("That track is not in the queue.", "track_not_found", 404);
      const items = room.queue.items.slice();
      items.splice(removeIndex, 1);
      const index = activeIndex === removeIndex ? -1 : activeIndex > removeIndex ? activeIndex - 1 : activeIndex;
      commitQueue(room, items, index, member, now);
      queueChanged = playbackChanged = true;
    } else if (action === "queueClear") {
      commitQueue(room, [], -1, member, now);
      queueChanged = playbackChanged = true;
    } else if (action === "queueMove") {
      const activeIndex = playbackIndex(room);
      const activeTrack = activeIndex >= 0 ? room.queue.items[activeIndex] : null;
      const moved = moveQueue(room.queue.items, frame.fromIndex, frame.toIndex, frame.videoId);
      if (!moved) return badAction("That queue move is invalid.");
      const index = activeTrack ? moved.indexOf(activeTrack) : room.queue.index;
      commitQueue(room, moved, index, member, now);
      queueChanged = playbackChanged = true;
    } else if (action === "next" || action === "previous") {
      const items = room.queue.items;
      const activeIndex = playbackIndex(room);
      let index = activeIndex;
      if (index < 0) index = room.queue.index;
      const delta = action === "next" ? 1 : -1;
      let target = activeIndex < 0 ? 0 : index + delta;
      if (action === "previous" && target < 0 && index === 0) target = 0;
      if (target < 0 || target >= items.length) {
        if (action === "next" && playback.track) {
          const end = playback.track.durationMs ?? current;
          commitPlayback(room, basePlayback({ isPlaying: false, positionMs: end, anchorMs: now }), now);
          playbackChanged = true;
        } else return badAction("There is no track in that direction.", "no_track", 409);
      } else {
        const track = items[target];
        commitPlayback(room, basePlayback({
          track,
          queueIndex: target,
          positionMs: 0,
          isPlaying: playback.isPlaying,
          anchorMs: playback.isPlaying ? now + PLAY_LEAD_MS : now,
          startedBy: member.memberId,
          startedByName: member.displayName,
        }), now);
        playbackChanged = true;
      }
    } else if (action === "setMaxMembers") {
      const value = frame.maxMembers;
      if (!Number.isInteger(value) || value < 2 || value > MAX_MEMBERS || value < room.members.length) {
        return badAction(`Choose a limit from ${room.members.length} to ${MAX_MEMBERS}.`, "invalid_capacity", 422);
      }
      room.maxMembers = value;
      membersChanged = true;
    } else if (action === "kick") {
      const target = room.members.find((value) => value.memberId === frame.memberId);
      if (!target || target.memberId === member.memberId) return badAction("Choose another party member to remove.", "member_not_found", 404);
      if (target.isHost) return badAction("The host cannot remove themselves.", "host_cannot_leave", 409);
      this.removeMember(target);
      if (room.members.length === 0) room.emptySinceMs = now;
      kickedMemberId = target.memberId;
      membersChanged = true;
    } else if (action === "setAutoplay") {
      if (typeof frame.enabled !== "boolean") return badAction("Choose whether AutoPlay is on or off.");
      commitPlayback(room, basePlayback({ autoplayEnabled: frame.enabled }), now);
      playbackChanged = true;
    } else if (action === "setHostOnlyControl") {
      if (typeof frame.enabled !== "boolean") return badAction("Choose whether host-only control is on or off.");
      room.hostOnlyControl = frame.enabled;
      membersChanged = true;
    } else {
      return badAction("That control is not supported.", "unknown_control");
    }
    return { membersChanged, queueChanged, playbackChanged, kickedMemberId };
  }

  removeMember(member) {
    this.room.members = this.room.members.filter((value) => value.memberId !== member.memberId);
    delete this.privateState.tokenHashes[member.memberId];
    for (const [deviceId, id] of Object.entries(this.privateState.devices)) {
      if (id === member.memberId) delete this.privateState.devices[deviceId];
    }
    if (member.isHost) transferHost(this.room);
  }

  pruneDisconnected(now) {
    if (!this.room) return false;
    const expired = this.room.members.filter((member) => !member.connected && now - member.lastSeenMs >= DISCONNECT_GRACE_MS);
    if (!expired.length) return false;
    for (const member of expired) this.removeMember(member);
    if (!this.room.members.length && this.room.emptySinceMs === null) this.room.emptySinceMs = now;
    return true;
  }

  async scheduleAlarm(now) {
    if (!this.room) return this.ctx.storage.deleteAlarm();
    const activeSockets = this.ctx.getWebSockets().filter((socket) => socket.readyState === WebSocket.OPEN);
    const deadlines = [this.room.createdAtMs + MAX_ROOM_AGE_MS];
    if (activeSockets.length) deadlines.push(now + HEARTBEAT_MS);
    for (const member of this.room.members) {
      if (!member.connected) deadlines.push(member.lastSeenMs + DISCONNECT_GRACE_MS);
    }
    if (this.room.emptySinceMs !== null) deadlines.push(this.room.emptySinceMs + EMPTY_ROOM_TTL_MS);
    const next = Math.min(...deadlines.filter((time) => time > now));
    if (Number.isFinite(next)) await this.ctx.storage.setAlarm(next);
    else await this.ctx.storage.deleteAlarm();
  }

  async alarm() {
    const now = Date.now();
    let expired = false;
    let changed = false;
    await this.locked(async () => {
      if (!this.room) return;
      if (now - this.room.createdAtMs >= MAX_ROOM_AGE_MS ||
        (this.room.emptySinceMs !== null && now - this.room.emptySinceMs >= EMPTY_ROOM_TTL_MS)) {
        for (const socket of this.ctx.getWebSockets()) {
          sendFrame(socket, { type: "bye", reason: "expired" });
          socket.close(4004, "party expired");
        }
        this.room = null;
        this.privateState = { devices: {}, tokenHashes: {} };
        await this.ctx.storage.deleteAll();
        await this.ctx.storage.deleteAlarm();
        expired = true;
        return;
      }
      changed = this.pruneDisconnected(now);
      if (!this.room.members.length && this.room.emptySinceMs === null) this.room.emptySinceMs = now;
      await this.persist();
      await this.scheduleAlarm(now);
    });
    if (expired) return;
    if (changed) this.broadcastMembers(now);
    if (this.ctx.getWebSockets().some((socket) => socket.readyState === WebSocket.OPEN)) this.broadcastState(now);
  }

  async webSocketClose(socket) {
    await this.markDisconnected(socket);
  }

  async webSocketError(socket) {
    await this.markDisconnected(socket);
  }

  async markDisconnected(socket) {
    const memberId = socket.deserializeAttachment()?.memberId;
    if (!memberId) return;
    const now = Date.now();
    let changed = false;
    await this.locked(async () => {
      const member = this.room?.members.find((value) => value.memberId === memberId);
      if (!member || memberSockets(this.ctx, memberId).length) return;
      member.connected = false;
      member.lastSeenMs = now;
      if (this.room.members.length === 1 && this.room.members[0].memberId === memberId) {
        this.room.emptySinceMs = null;
      }
      this.room.serverMs = now;
      await this.persist();
      await this.scheduleAlarm(now);
      changed = true;
    });
    if (changed) this.broadcastMembers(now);
  }

  broadcastMembers(now) {
    if (!this.room) return;
    const frame = {
      type: "members",
      members: this.room.members.map(publicMember),
      maxMembers: this.room.maxMembers,
      hostOnlyControl: this.room.hostOnlyControl,
      serverMs: now,
    };
    for (const socket of this.ctx.getWebSockets()) sendFrame(socket, frame);
  }

  broadcastQueue(now) {
    if (!this.room) return;
    for (const socket of this.ctx.getWebSockets()) {
      sendFrame(socket, { type: "queue", queue: this.room.queue, serverMs: now });
    }
  }

  broadcastState(now) {
    if (!this.room) return;
    const playback = { ...this.room.playback, effectivePositionMs: currentPosition(this.room.playback, now) };
    for (const socket of this.ctx.getWebSockets()) sendFrame(socket, { type: "state", playback, serverMs: now });
  }
}

function nearestTrackIndex(items, videoId, hint = 0) {
  if (typeof videoId !== "string" || !videoId) return -1;
  let nearest = -1;
  let distance = Number.POSITIVE_INFINITY;
  items.forEach((track, index) => {
    if (track.videoId !== videoId) return;
    const candidate = Math.abs(index - Math.max(0, hint));
    if (candidate < distance) { nearest = index; distance = candidate; }
  });
  return nearest;
}

function badAction(message, code = "invalid_control", status = 422) {
  return { error: { code, message, status } };
}
