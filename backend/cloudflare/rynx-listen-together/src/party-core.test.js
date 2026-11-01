// bxane
import test from "node:test";
import assert from "node:assert/strict";
import {
  addQueueTracks,
  cleanTrack,
  currentPosition,
  makeRoom,
  moveQueue,
  normalizeCode,
  playAt,
} from "./party-core.js";

test("normalizes six-character invite codes and rejects malformed codes", () => {
  assert.equal(normalizeCode("oIl-23a"), "01123A");
  assert.equal(normalizeCode("short"), null);
});

test("new rooms begin empty and use the shared party snapshot shape", () => {
  const room = makeRoom("AB23CD", 1_000);
  assert.equal(room.maxMembers, 5);
  assert.equal(room.members.length, 0);
  assert.equal(room.queue.seq, 0);
  assert.equal(room.playback.seq, 0);
  assert.equal(room.serverMs, 1_000);
});

test("queue additions stop at 25 upcoming tracks", () => {
  const items = [{ videoId: "current", fromAutoplay: false }];
  const tracks = Array.from({ length: 30 }, (_, i) => ({ videoId: `next-${i}`, fromAutoplay: false }));
  const result = addQueueTracks(items, 0, tracks, false);
  assert.equal(result.items.length, 26);
  assert.equal(result.addedCount, 25);
});

test("autoplay additions do not duplicate already waiting autoplay tracks", () => {
  const items = [
    { videoId: "current", fromAutoplay: false },
    { videoId: "next", fromAutoplay: true },
  ];
  const result = addQueueTracks(items, 0, [
    { videoId: "next", fromAutoplay: true },
    { videoId: "new", fromAutoplay: true },
  ], false);
  assert.deepEqual(result.items.map((track) => track.videoId), ["current", "next", "new"]);
});

test("queue move resolves the requested track after indices shift", () => {
  const items = ["a", "b", "c", "d"].map((videoId) => ({ videoId }));
  const moved = moveQueue(items, 1, 3, "c");
  assert.deepEqual(moved.map((track) => track.videoId), ["a", "b", "d", "c"]);
});

test("playback anchors a scheduled start and holds until that instant", () => {
  const playback = playAt(makeRoom("AB23CD", 1_000).playback, 42_000, 1_000, "member");
  assert.equal(playback.anchorMs, 1_350);
  assert.equal(currentPosition(playback, 1_349), 42_000);
  assert.equal(currentPosition(playback, 1_850), 42_500);
});

test("tracks require a bounded non-empty video ID", () => {
  assert.equal(cleanTrack({ videoId: "v123", title: "Song" }).videoId, "v123");
  assert.throws(() => cleanTrack({ videoId: "" }), { code: "invalid_track" });
});
