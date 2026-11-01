// bxane
export const MAX_MEMBERS = 5;
export const MAX_UPCOMING = 25;
export const MAX_QUEUE_LENGTH = 26;
export const PLAY_LEAD_MS = 350;

export function normalizeCode(value) {
  if (typeof value !== "string") return null;
  const compact = value.trim().replace(/[\s-]/g, "").toUpperCase();
  if (!/^[A-Z0-9]{6}$/.test(compact)) return null;
  return compact.replaceAll("O", "0").replace(/[IL]/g, "1");
}

export function makeRoom(code, now = Date.now()) {
  return {
    code,
    createdAtMs: now,
    emptySinceMs: null,
    maxMembers: MAX_MEMBERS,
    hostOnlyControl: false,
    members: [],
    playback: {
      seq: 0,
      track: null,
      queueSeq: 0,
      queueLength: 0,
      queueIndex: -1,
      isPlaying: false,
      positionMs: 0,
      anchorMs: now,
      effectivePositionMs: 0,
      updatedBy: null,
      startedBy: null,
      startedByName: null,
      autoplayEnabled: false,
      updatedAtMs: now,
    },
    queue: { seq: 0, index: -1, items: [] },
    serverMs: now,
  };
}

export function cleanTrack(input) {
  if (!input || typeof input !== "object" || Array.isArray(input)) {
    throw Object.assign(new Error("A track needs a video ID."), { code: "invalid_track" });
  }
  const videoId = typeof input.videoId === "string" ? input.videoId.trim() : "";
  if (!videoId || videoId.length > 128 || /[\u0000-\u001f]/.test(videoId)) {
    throw Object.assign(new Error("A track needs a valid video ID."), { code: "invalid_track" });
  }
  const thumbnailUrl = typeof input.thumbnailUrl === "string" && input.thumbnailUrl.length <= 2048 &&
    /^https:\/\//i.test(input.thumbnailUrl) ? input.thumbnailUrl : null;
  const durationMs = Number.isSafeInteger(input.durationMs) && input.durationMs > 0 && input.durationMs <= 86_400_000
    ? input.durationMs : null;
  return {
    videoId,
    title: textField(input.title, 300),
    artist: textField(input.artist, 300),
    thumbnailUrl,
    durationMs,
    fromAutoplay: input.fromAutoplay === true,
  };
}

function textField(value, maxLength) {
  return typeof value === "string" ? value.trim().slice(0, maxLength) : "";
}

export function currentPosition(playback, now) {
  if (!playback.isPlaying || now <= playback.anchorMs) return playback.positionMs;
  return playback.positionMs + Math.max(0, now - playback.anchorMs);
}

export function playAt(playback, positionMs, now, updatedBy) {
  const position = Number.isSafeInteger(positionMs) ? Math.max(0, positionMs) : 0;
  return {
    ...playback,
    seq: playback.seq + 1,
    isPlaying: true,
    positionMs: position,
    anchorMs: now + PLAY_LEAD_MS,
    effectivePositionMs: position,
    updatedBy,
    updatedAtMs: now,
  };
}

export function addQueueTracks(items, index, incoming, playNext) {
  const next = items.slice();
  const active = Number.isInteger(index) ? Math.min(index, next.length - 1) : -1;
  const waitingAutoplay = new Set(next.slice(active + 1).filter((track) => track.fromAutoplay).map((track) => track.videoId));
  const available = Math.max(0, Math.min(MAX_UPCOMING - (next.length - active - 1), MAX_QUEUE_LENGTH - next.length));
  const accepted = [];
  for (const value of incoming) {
    const track = cleanTrack(value);
    if (track.fromAutoplay && waitingAutoplay.has(track.videoId)) continue;
    if (accepted.length >= available) continue;
    accepted.push(track);
    if (track.fromAutoplay) waitingAutoplay.add(track.videoId);
  }
  const insertAt = playNext ? active + 1 : next.length;
  next.splice(insertAt, 0, ...accepted);
  return { items: next, index: active < 0 && next.length ? 0 : active, addedCount: accepted.length };
}

export function moveQueue(items, fromIndex, toIndex, videoId) {
  if (!Number.isInteger(fromIndex) || !Number.isInteger(toIndex)) return null;
  let from = fromIndex;
  if (typeof videoId === "string" && videoId) {
    let nearest = -1;
    let distance = Number.POSITIVE_INFINITY;
    for (let i = 0; i < items.length; i++) {
      if (items[i].videoId !== videoId) continue;
      const candidate = Math.abs(i - fromIndex);
      if (candidate < distance) { nearest = i; distance = candidate; }
    }
    if (nearest >= 0) from = nearest;
  }
  if (from < 0 || from >= items.length || toIndex < 0 || toIndex >= items.length) return null;
  const next = items.slice();
  const [track] = next.splice(from, 1);
  next.splice(toIndex, 0, track);
  return next;
}

export function validIdentity(input) {
  if (!input || typeof input !== "object" || Array.isArray(input)) return false;
  return typeof input.userId === "string" && input.userId.length >= 8 && input.userId.length <= 128 &&
    typeof input.deviceId === "string" && input.deviceId.length >= 8 && input.deviceId.length <= 128 &&
    typeof input.displayName === "string" && input.displayName.trim().length > 0 && input.displayName.length <= 160;
}

export function safeAvatarUrl(value) {
  return typeof value === "string" && value.length <= 2048 && /^https:\/\//i.test(value) ? value : null;
}
