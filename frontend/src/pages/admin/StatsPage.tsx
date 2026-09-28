import { useEffect, useState } from "react";
import { api, errorMessage } from "../../api";
import type { BranchAdmin, Stats } from "../../types";

const isoDay = (d: Date) => {
  const local = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
};

const fmt = (v?: number | null, unit = "") => (v === undefined || v === null ? "—" : `${v.toLocaleString("fr-FR")}${unit}`);

export default function StatsPage() {
  const [branches, setBranches] = useState<BranchAdmin[]>([]);
  const [branchId, setBranchId] = useState<number | null>(null);
  const [from, setFrom] = useState(isoDay(new Date()));
  const [to, setTo] = useState(isoDay(new Date()));
  const [stats, setStats] = useState<Stats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<BranchAdmin[]>("/admin/branches")
      .then((b) => {
        setBranches(b);
        setBranchId(b[0]?.id ?? null);
      })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  useEffect(() => {
    if (!branchId) return;
    setError(null);
    api<Stats>(`/admin/branches/${branchId}/stats?from=${from}&to=${to}`)
      .then(setStats)
      .catch((e) => setError(errorMessage(e)));
  }, [branchId, from, to]);

  const setRange = (days: number) => {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - days + 1);
    setFrom(isoDay(start));
    setTo(isoDay(end));
  };

  const maxHour = Math.max(1, ...(stats?.byHour ?? [0]));
  // On n'affiche que la plage horaire utile (première à dernière heure avec des tickets, 8h–18h par défaut).
  const activeHours = stats?.byHour.map((n, h) => (n > 0 ? h : -1)).filter((h) => h >= 0) ?? [];
  const firstHour = Math.min(8, ...activeHours);
  const lastHour = Math.max(18, ...activeHours);

  return (
    <>
      <div className="page-title">
        <h1>Statistiques</h1>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select className="select" value={branchId ?? ""} onChange={(e) => setBranchId(Number(e.target.value))}>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </select>
          <button className="btn btn-ghost btn-sm" onClick={() => setRange(1)}>
            Aujourd'hui
          </button>
          <button className="btn btn-ghost btn-sm" onClick={() => setRange(7)}>
            7 jours
          </button>
          <button className="btn btn-ghost btn-sm" onClick={() => setRange(30)}>
            30 jours
          </button>
          <input className="select" type="date" value={from} max={to} onChange={(e) => setFrom(e.target.value)} aria-label="Du" />
          <input className="select" type="date" value={to} min={from} onChange={(e) => setTo(e.target.value)} aria-label="Au" />
        </div>
      </div>
      {error && <div className="error">{error}</div>}
      {!stats ? (
        <div className="page-loading">Chargement…</div>
      ) : (
        <>
          <div className="kpis">
            <Kpi label="Tickets délivrés" value={fmt(stats.total)} />
            <Kpi label="Clients servis" value={fmt(stats.served)} sub={stats.total ? `${Math.round((stats.served / stats.total) * 100)} %` : undefined} />
            <Kpi label="Attente moyenne" value={fmt(stats.avgWaitMinutes, " min")} />
            <Kpi label="Traitement moyen" value={fmt(stats.avgServiceMinutes, " min")} />
            <Kpi label="Absents" value={fmt(stats.noShow)} sub={stats.total ? `${Math.round((stats.noShow / stats.total) * 100)} %` : undefined} />
            <Kpi label="Satisfaction" value={stats.avgRating ? `${stats.avgRating.toLocaleString("fr-FR")} / 5` : "—"} sub={`${stats.ratings} avis`} />
          </div>

          <section className="card" style={{ marginTop: 20 }}>
            <h2 style={{ fontSize: 17 }}>Affluence par heure</h2>
            <div className="hours" role="img" aria-label="Nombre de tickets par heure de la journée">
              {stats.byHour.slice(firstHour, lastHour + 1).map((n, i) => (
                <div key={i} className="hour" title={`${firstHour + i}h : ${n} ticket(s)`}>
                  <span className="hour-n">{n > 0 ? n : ""}</span>
                  <div className="hour-bar" style={{ height: `${(n / maxHour) * 100}%` }} />
                  <span className="hour-label">{firstHour + i}h</span>
                </div>
              ))}
            </div>
          </section>

          <div className="detail-grid" style={{ marginTop: 20 }}>
            <section className="card table-wrap">
              <h2 style={{ fontSize: 17 }}>Par service</h2>
              <table className="table">
                <thead>
                  <tr>
                    <th>Service</th>
                    <th>Tickets</th>
                    <th>Servis</th>
                    <th>Attente moy.</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.byService.map((s) => (
                    <tr key={s.name}>
                      <td>{s.name}</td>
                      <td>{s.total}</td>
                      <td>{s.served}</td>
                      <td>{fmt(s.avgWaitMinutes, " min")}</td>
                    </tr>
                  ))}
                  {stats.byService.length === 0 && (
                    <tr>
                      <td colSpan={4} className="muted">
                        Aucune donnée sur la période.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </section>
            <section className="card table-wrap">
              <h2 style={{ fontSize: 17 }}>Par agent</h2>
              <table className="table">
                <thead>
                  <tr>
                    <th>Agent</th>
                    <th>Servis</th>
                    <th>Traitement moy.</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.byAgent.map((a) => (
                    <tr key={a.name}>
                      <td>{a.name}</td>
                      <td>{a.served}</td>
                      <td>{fmt(a.avgServiceMinutes, " min")}</td>
                    </tr>
                  ))}
                  {stats.byAgent.length === 0 && (
                    <tr>
                      <td colSpan={3} className="muted">
                        Aucune donnée sur la période.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </section>
          </div>
        </>
      )}
    </>
  );
}

function Kpi({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="card kpi">
      <div className="kpi-label">{label}</div>
      <div className="kpi-value">{value}</div>
      {sub && <div className="small muted">{sub}</div>}
    </div>
  );
}
