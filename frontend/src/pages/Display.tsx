import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api, errorMessage } from "../api";
import { QrCanvas } from "../components/common";
import { Icon } from "../components/Icon";
import { playChime, useBranchStream } from "../realtime";
import type { DisplayView } from "../types";
import "./display.css";

/** Écran TV de la salle d'attente (navigateur en plein écran). */
export default function Display() {
  const { code = "" } = useParams();
  const [view, setView] = useState<DisplayView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [soundOn, setSoundOn] = useState(false);
  const [flash, setFlash] = useState<string | null>(null);
  const [now, setNow] = useState(new Date());

  const load = useCallback(() => {
    api<DisplayView>(`/public/branches/${code}/display`)
      .then((v) => {
        setView(v);
        setError(null);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [code]);

  useEffect(load, [load]);

  const live = useBranchStream(code, (event) => {
    if ((event.type === "CALLED" || event.type === "RECALLED") && event.ticketCode) {
      if (soundOn) playChime();
      setFlash(event.ticketCode);
      window.setTimeout(() => setFlash((f) => (f === event.ticketCode ? null : f)), 6000);
    }
    load();
  });

  useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), 10_000);
    return () => window.clearInterval(id);
  }, []);

  const ticketUrl = `${window.location.origin}/q/${code}`;
  const [latest, ...previous] = view?.called ?? [];

  return (
    <div className="display">
      <header className="display-head">
        <div>
          <div className="display-org">{view?.organizationName}</div>
          <div className="display-branch">{view?.branchName ?? "…"}</div>
        </div>
        <div className="display-clock">
          {now.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}
          <span className={`dot ${live ? "dot-live" : ""}`} style={{ marginLeft: 14 }} />
        </div>
      </header>

      {error && <div className="error" style={{ margin: 24 }}>{error}</div>}

      <main className="display-main">
        <section className="display-now">
          <div className="display-label">Ticket appelé</div>
          {latest ? (
            <div className={`display-latest ${flash === latest.code ? "flash" : ""}`}>
              <div className="code">{latest.code}</div>
              <div className="arrow">→</div>
              <div className="counter">{latest.counterName}</div>
            </div>
          ) : (
            <div className="display-latest empty">En attente du premier appel</div>
          )}

          <div className="display-label" style={{ marginTop: 36 }}>Appels précédents</div>
          <div className="display-previous">
            {previous.map((t) => (
              <div key={t.code + t.calledAt} className={`prev ${flash === t.code ? "flash" : ""}`}>
                <span className="code">{t.code}</span>
                <span className="counter">{t.counterName}</span>
              </div>
            ))}
          </div>
        </section>

        <aside className="display-side">
          <div className="display-label">En attente</div>
          <div className="display-queues">
            {view?.queues.map((q) => (
              <div key={q.prefix} className="queue">
                <span className="prefix">{q.prefix}</span>
                <span className="name">{q.name}</span>
                <span className="n">{q.waiting}</span>
              </div>
            ))}
          </div>
          <div className="display-qr">
            <QrCanvas value={ticketUrl} size={170} />
            <div>
              <strong>Prenez votre ticket</strong>
              <br />
              Scannez avec votre téléphone et suivez la file sans attendre ici.
            </div>
          </div>
        </aside>
      </main>

      {!soundOn && (
        <button className="display-sound" onClick={() => { setSoundOn(true); playChime(); }}>
          <Icon name="volume" /> Cliquer pour activer le son des appels
        </button>
      )}
    </div>
  );
}
