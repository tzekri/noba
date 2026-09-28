import { useCallback, useEffect, useState } from "react";
import { api, errorMessage, post } from "../../api";
import { useAuth } from "../../auth";
import { formatTime, formatWait, minutesSince, StaffHeader, staffLinks, StatusBadge } from "../../components/common";
import { Icon } from "../../components/Icon";
import { useBranchStream } from "../../realtime";
import type { AgentTicket, BoardView, BranchSummary } from "../../types";
import "./agent.css";

const COUNTER_KEY = "noba.agent.counter";

/** Poste de l'agent : ouvrir son guichet, appeler le suivant, traiter le ticket en cours. */
export default function AgentDesk() {
  const { user } = useAuth();
  const [branches, setBranches] = useState<BranchSummary[]>([]);
  const [branchId, setBranchId] = useState<number | null>(null);
  const [board, setBoard] = useState<BoardView | null>(null);
  const [counterId, setCounterId] = useState<number | null>(() => {
    try {
      const raw = localStorage.getItem(COUNTER_KEY);
      return raw ? Number(raw) : null;
    } catch {
      return null;
    }
  });
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [transferOpen, setTransferOpen] = useState(false);
  const [, setTick] = useState(0);

  useEffect(() => {
    api<BranchSummary[]>("/agent/branches")
      .then((list) => {
        setBranches(list);
        setBranchId((current) => current ?? list[0]?.id ?? null);
      })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  const load = useCallback(() => {
    if (!branchId) return;
    api<BoardView>(`/agent/branches/${branchId}/board`)
      .then((b) => {
        setBoard(b);
        // Si j'occupe déjà un guichet, il est sélectionné d'office.
        const mine = b.counters.find((c) => c.agentId === user?.id);
        if (mine) setCounterId(mine.id);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [branchId, user?.id]);

  useEffect(load, [load]);
  const live = useBranchStream(board?.branch.code, load);

  // Rafraîchit les durées affichées (« depuis 3 min »).
  useEffect(() => {
    const id = window.setInterval(() => setTick((t) => t + 1), 30_000);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    try {
      if (counterId) localStorage.setItem(COUNTER_KEY, String(counterId));
    } catch {
      /* stockage indisponible */
    }
  }, [counterId]);

  async function run<T>(action: () => Promise<T>, success?: (result: T) => string | null) {
    setBusy(true);
    setError(null);
    setInfo(null);
    try {
      const result = await action();
      if (success) setInfo(success(result));
      load();
    } catch (e) {
      setError(errorMessage(e));
      load();
    } finally {
      setBusy(false);
    }
  }

  const counter = board?.counters.find((c) => c.id === counterId) ?? null;
  const iHoldIt = counter?.agentId === user?.id;
  const current = iHoldIt ? counter?.current : undefined;
  const myServices = board?.services.filter((s) => counter?.serviceIds.includes(s.id)) ?? [];
  const waitingForMe = myServices.reduce((sum, s) => sum + s.waiting, 0);
  // Tickets en attente dans des services qu'aucun guichet ouvert ne traite : personne ne pourra les appeler.
  const openServiceIds = new Set(board?.counters.filter((c) => c.agentId).flatMap((c) => c.serviceIds) ?? []);
  const orphanServices = board?.services.filter((s) => s.waiting > 0 && !counter?.serviceIds.includes(s.id) && !openServiceIds.has(s.id)) ?? [];

  const callNext = () =>
    run(
      () => post<AgentTicket | null>(`/agent/counters/${counterId}/next`),
      (t) => (t ? null : "Personne n'attend pour les services de ce guichet."),
    );
  const ticketAction = (path: string) => current && run(() => post(`/agent/tickets/${current.id}/${path}`));

  // Raccourci clavier : Espace / Entrée = appeler le suivant.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLSelectElement || e.target instanceof HTMLTextAreaElement) return;
      if ((e.key === " " || e.key === "Enter") && iHoldIt && !busy) {
        e.preventDefault();
        callNext();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  return (
    <>
      <StaffHeader links={staffLinks(user?.role)} />
      <main className="staff-main">
        <div className="container">
          <div className="page-title">
            <div>
              <h1>Guichet</h1>
              {board && (
                <span className="live muted small" style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
                  <span className={`dot ${live ? "dot-live" : ""}`} /> {board.branch.name} · {live ? "temps réel" : "reconnexion…"}
                </span>
              )}
            </div>
            <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
              {branches.length > 1 && (
                <select className="select" value={branchId ?? ""} onChange={(e) => { setBranchId(Number(e.target.value)); setCounterId(null); }}>
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name}
                    </option>
                  ))}
                </select>
              )}
              {board && (
                <select className="select" value={counterId ?? ""} onChange={(e) => setCounterId(Number(e.target.value) || null)}>
                  <option value="">Choisir un guichet…</option>
                  {board.counters.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                      {c.agentName ? ` — ${c.agentId === user?.id ? "vous" : c.agentName}` : " — libre"}
                    </option>
                  ))}
                </select>
              )}
            </div>
          </div>

          {error && <div className="error">{error}</div>}
          {info && <div className="notice" style={{ marginBottom: 14 }}>{info}</div>}

          {!board ? (
            <div className="page-loading">Chargement…</div>
          ) : branches.length === 0 ? (
            <div className="card">Aucun établissement accessible.</div>
          ) : (
            <div className="desk">
              <section className="desk-main">
                {!counter ? (
                  <div className="card desk-empty">Sélectionnez votre guichet pour commencer.</div>
                ) : !iHoldIt ? (
                  <div className="card desk-empty">
                    <h2>{counter.name}</h2>
                    {counter.agentName ? (
                      <p className="muted">Ce guichet est actuellement occupé par {counter.agentName}.</p>
                    ) : (
                      <p className="muted">
                        Guichet fermé. Services traités : {myServices.map((s) => s.name).join(", ") || "aucun"}.
                      </p>
                    )}
                    <button className="btn btn-primary btn-lg" disabled={busy || !!counter.agentName} onClick={() => run(() => post(`/agent/counters/${counter.id}/open`))}>
                      Ouvrir ce guichet
                    </button>
                  </div>
                ) : (
                  <>
                    <div className="card desk-current">
                      <div className="desk-current-head">
                        <span className="muted small">{counter.name} · Ticket en cours</span>
                        <button className="btn btn-ghost btn-sm" disabled={busy} onClick={() => run(() => post(`/agent/counters/${counter.id}/close`))}>
                          Fermer le guichet
                        </button>
                      </div>
                      {current ? (
                        <>
                          <div className="desk-code">{current.code}</div>
                          <div className="desk-meta">
                            <StatusBadge status={current.status} />
                            <span>{current.serviceName}</span>
                            <span className="muted">
                              appelé à {formatTime(current.calledAt)} · attente {minutesSince(current.queuedAt, new Date(current.calledAt ?? Date.now()).getTime())} min
                            </span>
                            {current.recallCount > 0 && <span className="badge badge-warn">rappelé ×{current.recallCount}</span>}
                          </div>
                          <div className="desk-actions">
                            {current.status === "CALLED" && (
                              <>
                                <button className="btn btn-ghost" disabled={busy} onClick={() => ticketAction("recall")}>
                                  <Icon name="bell" /> Rappeler
                                </button>
                                <button className="btn btn-ghost" disabled={busy} onClick={() => ticketAction("start")}>
                                  <Icon name="play" /> Client présent
                                </button>
                                <button className="btn btn-danger" disabled={busy} onClick={() => ticketAction("no-show")}>
                                  <Icon name="userX" /> Absent
                                </button>
                              </>
                            )}
                            <button className="btn btn-ghost" disabled={busy} onClick={() => setTransferOpen((o) => !o)}>
                              <Icon name="swap" /> Transférer
                            </button>
                            <button className="btn btn-ghost" disabled={busy} onClick={() => ticketAction("complete")}>
                              <Icon name="check" /> Terminer
                            </button>
                          </div>
                          {transferOpen && (
                            <div className="desk-transfer">
                              <span className="small muted">Transférer vers :</span>
                              {board.services
                                .filter((s) => s.id !== current.serviceId)
                                .map((s) => (
                                  <button
                                    key={s.id}
                                    className="btn btn-ghost btn-sm"
                                    disabled={busy}
                                    onClick={() => {
                                      setTransferOpen(false);
                                      run(() => post(`/agent/tickets/${current.id}/transfer`, { serviceId: s.id }), () => `${current.code} transféré vers ${s.name}.`);
                                    }}
                                  >
                                    {s.name}
                                  </button>
                                ))}
                            </div>
                          )}
                        </>
                      ) : (
                        <div className="desk-idle">Aucun ticket en cours</div>
                      )}
                    </div>

                    <button className="btn btn-primary desk-next" disabled={busy || waitingForMe === 0} onClick={callNext}>
                      {current ? "Terminer et appeler le suivant" : "Appeler le suivant"}
                      <span className="desk-next-sub">
                        {waitingForMe === 0 ? "personne en attente pour ce guichet" : `${waitingForMe} en attente · Espace`}
                      </span>
                    </button>
                    {orphanServices.length > 0 && (
                      <div className="notice" style={{ marginTop: 12, display: "flex", gap: 10, alignItems: "flex-start" }}>
                        <Icon name="alert" />
                        <span>
                          {orphanServices.map((s) => `${s.name} (${s.waiting})`).join(", ")} : des clients attendent, mais aucun guichet ouvert ne
                          traite ce service. Un responsable doit l'affecter à un guichet (Établissements → Guichets).
                        </span>
                      </div>
                    )}
                  </>
                )}

                <div className="card" style={{ marginTop: 20 }}>
                  <h3 style={{ fontSize: 16 }}>File d'attente ({board.waiting.length})</h3>
                  {board.waiting.length === 0 ? (
                    <p className="muted small">Personne n'attend.</p>
                  ) : (
                    <div className="table-wrap">
                      <table className="table">
                        <thead>
                          <tr>
                            <th>Ticket</th>
                            <th>Service</th>
                            <th>Pris à</th>
                            <th>Attente</th>
                          </tr>
                        </thead>
                        <tbody>
                          {board.waiting.map((t) => (
                            <tr key={t.id} style={counter && !counter.serviceIds.includes(t.serviceId) ? { opacity: 0.45 } : undefined}>
                              <td>
                                <strong>{t.code}</strong>
                              </td>
                              <td>{t.serviceName}</td>
                              <td>{formatTime(t.queuedAt)}</td>
                              <td>{minutesSince(t.queuedAt)} min</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </section>

              <aside className="desk-side">
                <div className="card">
                  <h3 style={{ fontSize: 16 }}>Services</h3>
                  {board.services.map((s) => (
                    <div key={s.id} className="desk-service">
                      <span className="service-chip">{s.prefix}</span>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 600 }}>{s.name}</div>
                        <div className="small muted">
                          {formatWait(s.estimatedWaitMinutes)} · {s.avgServiceMinutes} min/client
                        </div>
                      </div>
                      <strong style={{ fontSize: 22 }}>{s.waiting}</strong>
                    </div>
                  ))}
                </div>
                <div className="card" style={{ marginTop: 16 }}>
                  <h3 style={{ fontSize: 16 }}>Guichets</h3>
                  {board.counters.map((c) => (
                    <div key={c.id} className="desk-service">
                      <span className={`dot ${c.agentName ? "dot-live" : ""}`} />
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 600 }}>{c.name}</div>
                        <div className="small muted">{c.agentName ?? "Fermé"}</div>
                      </div>
                      {c.current && <strong>{c.current.code}</strong>}
                    </div>
                  ))}
                </div>
                {board.recent.length > 0 && (
                  <div className="card" style={{ marginTop: 16 }}>
                    <h3 style={{ fontSize: 16 }}>Derniers traités</h3>
                    {board.recent.map((t) => (
                      <div key={t.code + t.calledAt} className="desk-service small">
                        <strong style={{ width: 60 }}>{t.code}</strong>
                        <span style={{ flex: 1 }} className="muted">
                          {t.counterName}
                        </span>
                        <StatusBadge status={t.status} />
                      </div>
                    ))}
                  </div>
                )}
              </aside>
            </div>
          )}
        </div>
      </main>
    </>
  );
}
