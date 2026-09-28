import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, errorMessage, post } from "../../api";
import { Modal } from "../../components/common";
import type { BranchAdmin } from "../../types";

export default function Branches() {
  const navigate = useNavigate();
  const [branches, setBranches] = useState<BranchAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    api<BranchAdmin[]>("/admin/branches")
      .then(setBranches)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  return (
    <>
      <div className="page-title">
        <h1>Établissements</h1>
        <button className="btn btn-primary" onClick={() => setCreating(true)}>
          + Nouvel établissement
        </button>
      </div>
      {error && <div className="error">{error}</div>}
      {!branches ? (
        <div className="page-loading">Chargement…</div>
      ) : (
        <div className="grid-cards">
          {branches.map((b) => (
            <Link key={b.id} to={`/admin/branches/${b.id}`} className="card branch-card">
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", gap: 8 }}>
                <h3 style={{ fontSize: 18, margin: 0 }}>{b.name}</h3>
                <span className={`badge ${b.open ? "badge-ok" : ""}`}>{b.open ? "Ouvert" : "Fermé"}</span>
              </div>
              <div className="muted small" style={{ marginTop: 4 }}>
                {b.address ?? "Adresse non renseignée"}
              </div>
              <div className="small" style={{ marginTop: 14, color: "var(--ink-2)" }}>
                Code public : <code>{b.code}</code>
              </div>
            </Link>
          ))}
        </div>
      )}
      {creating && (
        <CreateBranch
          onClose={() => setCreating(false)}
          onCreated={(b) => navigate(`/admin/branches/${b.id}`)}
        />
      )}
    </>
  );
}

function CreateBranch({ onClose, onCreated }: { onClose: () => void; onCreated: (b: BranchAdmin) => void }) {
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      onCreated(await post<BranchAdmin>("/admin/branches", { name, address, open: true }));
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  }

  return (
    <Modal title="Nouvel établissement" onClose={onClose}>
      <form onSubmit={submit}>
        {error && <div className="error">{error}</div>}
        <div className="field">
          <label htmlFor="b-name">Nom</label>
          <input id="b-name" value={name} onChange={(e) => setName(e.target.value)} required autoFocus placeholder="Agence Maârif" />
        </div>
        <div className="field">
          <label htmlFor="b-address">Adresse</label>
          <input id="b-address" value={address} onChange={(e) => setAddress(e.target.value)} placeholder="Facultatif" />
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Annuler
          </button>
          <button className="btn btn-primary" disabled={busy}>
            Créer
          </button>
        </div>
      </form>
    </Modal>
  );
}
