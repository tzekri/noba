import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, errorMessage, post } from "../../api";
import { formatWait, Logo } from "../../components/common";
import { useBranchStream } from "../../realtime";
import type { BranchPublic, TicketView } from "../../types";
import "./client.css";

const activeTicketKey = (code: string) => `noba.ticket.${code}`;

function rememberTicket(branchCode: string, token: string) {
  try {
    localStorage.setItem(activeTicketKey(branchCode), token);
  } catch {
    /* stockage indisponible */
  }
}

/** Page ouverte en scannant le QR code de l'établissement : choix du service, prise de ticket. */
export default function TakeTicket() {
  const { code = "" } = useParams();
  const navigate = useNavigate();
  const [branch, setBranch] = useState<BranchPublic | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [existing, setExisting] = useState<TicketView | null>(null);

  const load = useCallback(() => {
    api<BranchPublic>(`/public/branches/${code}`)
      .then((b) => {
        setBranch(b);
        setError(null);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [code]);

  useEffect(load, [load]);
  const live = useBranchStream(code, load);

  // Un ticket encore actif dans cet établissement ? On propose de le reprendre plutôt que d'en prendre un second.
  useEffect(() => {
    let token: string | null = null;
    try {
      token = localStorage.getItem(activeTicketKey(code));
    } catch {
      /* stockage indisponible */
    }
    if (!token) return;
    api<TicketView>(`/public/tickets/${token}`)
      .then((t) => (["WAITING", "CALLED", "SERVING"].includes(t.status) ? setExisting(t) : null))
      .catch(() => null);
  }, [code]);

  async function take() {
    if (selected === null) return;
    setSubmitting(true);
    setError(null);
    try {
      const ticket = await post<TicketView>(`/public/branches/${code}/tickets`, { serviceId: selected });
      rememberTicket(code, ticket.token);
      navigate(`/t/${ticket.token}`);
    } catch (e) {
      setError(errorMessage(e));
      setSubmitting(false);
      load();
    }
  }

  if (!branch) {
    return (
      <div className="client">
        <div className="client-top">
          <Logo />
        </div>
        {error ? <div className="error">{error}</div> : <div className="page-loading">Chargement…</div>}
      </div>
    );
  }

  return (
    <div className="client">
      <div className="client-top">
        <Logo />
        <span className="live">
          <span className={`dot ${live ? "dot-live" : ""}`} /> {live ? "En direct" : "Connexion…"}
        </span>
      </div>

      <div className="client-branch">
        <div className="org">{branch.organizationName}</div>
        <h1>{branch.name}</h1>
        {branch.address && <div className="muted small">{branch.address}</div>}
      </div>

      {existing && (
        <div className="card" style={{ marginBottom: 16, display: "flex", alignItems: "center", gap: 12 }}>
          <div style={{ flex: 1 }}>
            <div className="small muted">Vous avez déjà un ticket</div>
            <strong style={{ fontSize: 22 }}>{existing.code}</strong> <span className="muted small">· {existing.serviceName}</span>
          </div>
          <Link className="btn btn-primary" to={`/t/${existing.token}`}>
            Suivre
          </Link>
        </div>
      )}

      {!branch.open && <div className="notice" style={{ marginBottom: 16 }}>Cet établissement ne délivre pas de tickets pour le moment.</div>}
      {error && <div className="error">{error}</div>}

      <h2 style={{ fontSize: 17 }}>Quel est le motif de votre visite ?</h2>
      <div className="service-list">
        {branch.services.map((s) => (
          <button
            key={s.id}
            className={`service-card ${selected === s.id ? "selected" : ""}`}
            disabled={!s.available}
            onClick={() => setSelected(s.id)}
            aria-pressed={selected === s.id}
          >
            <span className="service-prefix">{s.prefix}</span>
            <span className="body">
              <span className="name">{s.name}</span>
              {s.description && <span className="desc" style={{ display: "block" }}>{s.description}</span>}
              {!s.available && branch.open && <span className="desc" style={{ display: "block", color: "var(--danger)" }}>Complet pour aujourd'hui</span>}
            </span>
            <span className="meta">
              <strong>{s.waiting}</strong>
              en attente
              <span style={{ display: "block" }}>{formatWait(s.estimatedWaitMinutes)}</span>
            </span>
          </button>
        ))}
        {branch.services.length === 0 && <div className="muted">Aucun service disponible.</div>}
      </div>

      <div className="sticky-cta">
        <button className="btn btn-primary btn-lg btn-block" disabled={selected === null || submitting || !branch.open} onClick={take}>
          {submitting ? "Création du ticket…" : "Prendre mon ticket"}
        </button>
        <p className="small muted" style={{ textAlign: "center", margin: "10px 0 0" }}>
          Aucun compte ni application nécessaire. Gardez cette page ouverte pour être prévenu.
        </p>
      </div>

      {!existing && <RecoverTicket branchCode={code} onFound={(t) => { rememberTicket(code, t.token); navigate(`/t/${t.token}`); }} />}

      <div className="client-footer">Propulsé par Noba</div>
    </div>
  );
}

/** « J'ai déjà un ticket » : page de suivi perdue, autre téléphone, ticket papier délivré au guichet… */
function RecoverTicket({ branchCode, onFound }: { branchCode: string; onFound: (t: TicketView) => void }) {
  const [open, setOpen] = useState(false);
  const [ticketCode, setTicketCode] = useState("");
  const [recoveryCode, setRecoveryCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      onFound(await post<TicketView>(`/public/branches/${branchCode}/tickets/recover`, { ticketCode, recoveryCode }));
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  }

  if (!open) {
    return (
      <p style={{ textAlign: "center", margin: "18px 0 0" }}>
        <button className="link-button" onClick={() => setOpen(true)}>
          J'ai déjà un ticket
        </button>
      </p>
    );
  }

  return (
    <form className="card" style={{ marginTop: 18 }} onSubmit={submit}>
      <h2 style={{ fontSize: 17 }}>Retrouver mon ticket</h2>
      <p className="small muted" style={{ marginTop: -4 }}>
        Saisissez le numéro et le code de suivi à 4 chiffres inscrits sur votre ticket.
      </p>
      {error && <div className="error">{error}</div>}
      <div className="field-row" style={{ gridTemplateColumns: "1fr 1fr" }}>
        <div className="field">
          <label htmlFor="r-code">Numéro</label>
          <input id="r-code" value={ticketCode} onChange={(e) => setTicketCode(e.target.value.toUpperCase())} placeholder="B-042" required autoCapitalize="characters" autoComplete="off" />
        </div>
        <div className="field">
          <label htmlFor="r-pin">Code de suivi</label>
          <input
            id="r-pin"
            value={recoveryCode}
            onChange={(e) => setRecoveryCode(e.target.value.replace(/\D/g, "").slice(0, 4))}
            placeholder="1234"
            inputMode="numeric"
            pattern="\d{4}"
            required
            autoComplete="off"
          />
        </div>
      </div>
      <div style={{ display: "flex", gap: 10 }}>
        <button type="button" className="btn btn-ghost" onClick={() => setOpen(false)}>
          Annuler
        </button>
        <button className="btn btn-primary" style={{ flex: 1 }} disabled={busy || recoveryCode.length !== 4}>
          {busy ? "Recherche…" : "Retrouver"}
        </button>
      </div>
    </form>
  );
}
