import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, errorMessage, post } from "../../api";
import { formatTime, formatWait, Logo } from "../../components/common";
import { alertUser, notificationsGranted, notificationsSupported, requestNotifications } from "../../notify";
import { useBranchStream } from "../../realtime";
import type { TicketView } from "../../types";
import "./client.css";

/** Seuil de l'alerte « bientôt votre tour ». */
const SOON_THRESHOLD = 2;

/** Suivi du ticket en temps réel, sur le téléphone du client. */
export default function TicketTrack() {
  const { token = "" } = useParams();
  const [ticket, setTicket] = useState<TicketView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notifOn, setNotifOn] = useState(notificationsGranted());
  const [dismissedCall, setDismissedCall] = useState(false);
  const [busy, setBusy] = useState(false);

  // Nombre de personnes devant au premier affichage : base de la barre de progression.
  const initialAhead = useRef<number | null>(null);
  const alerted = useRef<{ soon: boolean; calledAt?: string; recallCount: number }>({ soon: false, recallCount: 0 });

  const load = useCallback(() => {
    api<TicketView>(`/public/tickets/${token}`)
      .then((t) => {
        setTicket(t);
        setError(null);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [token]);

  useEffect(load, [load]);

  const live = useBranchStream(ticket?.branchCode, (event) => {
    // Rappel de notre ticket par l'agent : on sonne à nouveau.
    if (event.type === "RECALLED" && ticket && event.ticketCode === ticket.code) {
      setDismissedCall(false);
      alertUser(`Rappel : ticket ${ticket.code}`, `Présentez-vous au ${event.counterName}`, `/t/${token}`);
    }
    load();
  });

  // Base de progression, mémorisée pour survivre à un rechargement de la page.
  useEffect(() => {
    if (!ticket || ticket.peopleAhead === undefined || initialAhead.current !== null) return;
    const key = `noba.ahead.${token}`;
    let stored: number | null = null;
    try {
      const raw = localStorage.getItem(key);
      stored = raw === null ? null : Number(raw);
      if (stored === null) localStorage.setItem(key, String(ticket.peopleAhead));
    } catch {
      /* stockage indisponible */
    }
    initialAhead.current = Math.max(stored ?? ticket.peopleAhead, ticket.peopleAhead);
  }, [ticket, token]);

  // Alertes : bientôt votre tour, puis c'est votre tour.
  useEffect(() => {
    if (!ticket) return;
    const a = alerted.current;
    if (ticket.status === "WAITING" && ticket.peopleAhead !== undefined && ticket.peopleAhead <= SOON_THRESHOLD && !a.soon) {
      a.soon = true;
      if (ticket.peopleAhead > 0) {
        alertUser("Bientôt votre tour", `Plus que ${ticket.peopleAhead} personne(s) avant vous (ticket ${ticket.code}).`, `/t/${token}`);
      }
    }
    if (ticket.status === "CALLED" && a.calledAt !== ticket.calledAt) {
      a.calledAt = ticket.calledAt;
      setDismissedCall(false);
      alertUser(`C'est votre tour ! ${ticket.code}`, `Présentez-vous au ${ticket.counterName ?? "guichet"}.`, `/t/${token}`);
    }
  }, [ticket, token]);

  // Le titre de l'onglet reflète l'état, visible même quand on change d'onglet.
  useEffect(() => {
    if (!ticket) return;
    const title =
      ticket.status === "CALLED"
        ? `🔔 ${ticket.code} → ${ticket.counterName}`
        : ticket.status === "WAITING"
          ? ticket.peopleAhead === 0
            ? `Vous êtes le prochain · ${ticket.code}`
            : `${ticket.peopleAhead} avant vous · ${ticket.code}`
          : `${ticket.code} · Noba`;
    document.title = title;
  }, [ticket]);

  useEffect(() => {
    const previous = document.title;
    return () => {
      document.title = previous;
    };
  }, []);

  async function enableNotifications() {
    setNotifOn(await requestNotifications());
  }

  async function cancel() {
    if (!ticket || !window.confirm(`Annuler le ticket ${ticket.code} ? Vous perdrez votre place.`)) return;
    setBusy(true);
    try {
      setTicket(await post<TicketView>(`/public/tickets/${token}/cancel`));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function rate(score: number) {
    setBusy(true);
    try {
      setTicket(await post<TicketView>(`/public/tickets/${token}/rating`, { score }));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  if (!ticket) {
    return (
      <div className="client">
        <div className="client-top">
          <Logo />
        </div>
        {error ? <div className="error">{error}</div> : <div className="page-loading">Chargement de votre ticket…</div>}
      </div>
    );
  }

  const ahead = ticket.peopleAhead ?? 0;
  const base = Math.max(initialAhead.current ?? ahead, 1);
  const progress = ticket.status === "WAITING" ? Math.round(((base - ahead) / base) * 100) : 100;

  return (
    <div className="client">
      <div className="client-top">
        <Logo />
        <span className="live">
          <span className={`dot ${live ? "dot-live" : ""}`} /> {live ? "En direct" : "Reconnexion…"}
        </span>
      </div>

      <div className="client-branch" style={{ marginBottom: 14 }}>
        <div className="org">{ticket.organizationName}</div>
        <div style={{ fontWeight: 700, fontSize: 18 }}>{ticket.branchName}</div>
      </div>

      {error && <div className="error">{error}</div>}

      <div className="ticket">
        <div className="ticket-head">
          <div className="label">Votre ticket</div>
          <div className="ticket-code">{ticket.code}</div>
          <div className="ticket-service">{ticket.serviceName}</div>
        </div>
        <div className="ticket-tear" />
        <div className="ticket-body">
          {ticket.status === "WAITING" && (
            <>
              {ahead === 0 ? (
                <div style={{ textAlign: "center", fontSize: 22, fontWeight: 800, color: "var(--brand)" }}>Vous êtes le prochain !</div>
              ) : (
                <div className="ahead">
                  <span className="n">{ahead}</span>
                  <span className="t">{ahead === 1 ? "personne avant vous" : "personnes avant vous"}</span>
                </div>
              )}
              <div className="progress" aria-label={`Progression ${progress} %`}>
                <div style={{ width: `${Math.max(progress, 4)}%` }} />
              </div>
              <div className="ticket-stats">
                <div>
                  <div className="k">Attente estimée</div>
                  <div className="v">{formatWait(ticket.estimatedWaitMinutes)}</div>
                </div>
                <div>
                  <div className="k">Ticket pris à</div>
                  <div className="v">{formatTime(ticket.createdAt)}</div>
                </div>
              </div>
            </>
          )}

          {(ticket.status === "CALLED" || ticket.status === "SERVING") && (
            <div style={{ textAlign: "center" }}>
              <div style={{ fontSize: 20, fontWeight: 800 }}>{ticket.status === "CALLED" ? "C'est votre tour !" : "Vous êtes en cours de traitement"}</div>
              <div style={{ fontSize: 28, fontWeight: 800, color: "var(--brand)", marginTop: 6 }}>{ticket.counterName}</div>
            </div>
          )}

          {ticket.status === "DONE" && (
            <div style={{ textAlign: "center" }}>
              <div style={{ fontSize: 20, fontWeight: 800 }}>Merci de votre visite !</div>
              {ticket.rating ? (
                <p className="muted">Merci pour votre avis ({ticket.rating}/5).</p>
              ) : (
                <>
                  <p className="muted" style={{ marginBottom: 0 }}>Comment s'est passée votre visite ?</p>
                  <div className="stars">
                    {[1, 2, 3, 4, 5].map((n) => (
                      <button key={n} disabled={busy} onClick={() => rate(n)} aria-label={`${n} sur 5`}>
                        ★
                      </button>
                    ))}
                  </div>
                </>
              )}
            </div>
          )}

          {ticket.status === "NO_SHOW" && (
            <div style={{ textAlign: "center" }}>
              <div style={{ fontSize: 18, fontWeight: 800 }}>Vous avez été appelé mais vous étiez absent.</div>
              <p className="muted">Vous pouvez reprendre un nouveau ticket.</p>
            </div>
          )}
          {ticket.status === "CANCELLED" && <div style={{ textAlign: "center", fontWeight: 700 }}>Ticket annulé.</div>}
          {ticket.status === "EXPIRED" && <div style={{ textAlign: "center", fontWeight: 700 }}>Ce ticket a expiré (journée terminée).</div>}
        </div>
      </div>

      <div className="client-actions">
        {ticket.status === "WAITING" && notificationsSupported() && !notifOn && (
          <button className="btn btn-accent btn-lg" onClick={enableNotifications}>
            🔔 Me prévenir quand c'est mon tour
          </button>
        )}
        {ticket.status === "WAITING" && notifOn && (
          <div className="notice" style={{ textAlign: "center", background: "var(--ok-soft)", color: "var(--ok)" }}>
            Notifications activées. Gardez cette page ouverte : votre téléphone sonnera et vibrera.
          </div>
        )}
        {ticket.status === "WAITING" && (
          <button className="btn btn-ghost" onClick={cancel} disabled={busy}>
            Annuler mon ticket
          </button>
        )}
        {["NO_SHOW", "CANCELLED", "EXPIRED", "DONE"].includes(ticket.status) && (
          <Link className="btn btn-primary btn-lg" to={`/q/${ticket.branchCode}`}>
            Prendre un nouveau ticket
          </Link>
        )}
      </div>

      {ticket.status === "CALLED" && !dismissedCall && (
        <div className="called-screen" onClick={() => setDismissedCall(true)} role="alert">
          <div className="big">C'est votre tour !</div>
          <div className="code">{ticket.code}</div>
          <div>Présentez-vous au</div>
          <div className="counter">{ticket.counterName}</div>
          <p style={{ marginTop: 30, opacity: 0.7, fontSize: 14 }}>Touchez l'écran pour fermer</p>
        </div>
      )}

      <div className="client-footer">Propulsé par Noba</div>
    </div>
  );
}
