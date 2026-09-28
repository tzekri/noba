import { useEffect, useRef, useState } from "react";
import type { QueueEvent } from "./types";

/**
 * S'abonne au flux temps réel (SSE) d'un établissement.
 * `onEvent` reçoit chaque événement de file ; il est aussi appelé avec { type: "RESYNC" }
 * à chaque (re)connexion, pour recharger l'état après une coupure réseau.
 * EventSource se reconnecte tout seul.
 */
export function useBranchStream(code: string | undefined, onEvent: (event: QueueEvent) => void): boolean {
  const handler = useRef(onEvent);
  handler.current = onEvent;
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (!code) return;
    const source = new EventSource(`/api/public/branches/${encodeURIComponent(code)}/stream`);
    source.addEventListener("hello", () => {
      setConnected(true);
      handler.current({ type: "RESYNC" });
    });
    source.addEventListener("queue", (e) => {
      try {
        handler.current(JSON.parse((e as MessageEvent).data));
      } catch {
        /* événement illisible : ignoré */
      }
    });
    source.onerror = () => setConnected(false);
    return () => source.close();
  }, [code]);

  return connected;
}

/** Petit carillon « ding-dong » généré (aucun fichier audio). */
export function playChime() {
  try {
    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    const ctx = new AudioCtx();
    [880, 660].forEach((freq, i) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.frequency.value = freq;
      osc.type = "sine";
      const start = ctx.currentTime + i * 0.35;
      gain.gain.setValueAtTime(0.0001, start);
      gain.gain.exponentialRampToValueAtTime(0.4, start + 0.03);
      gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.6);
      osc.connect(gain).connect(ctx.destination);
      osc.start(start);
      osc.stop(start + 0.65);
    });
    setTimeout(() => ctx.close(), 1500);
  } catch {
    /* audio indisponible */
  }
}
