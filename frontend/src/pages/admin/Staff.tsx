import { useCallback, useEffect, useState, type FormEvent } from "react";
import { api, errorMessage, post, put } from "../../api";
import { useAuth } from "../../auth";
import { Modal } from "../../components/common";
import type { BranchAdmin, Role, StaffMember } from "../../types";

const ROLE_LABELS: Record<Role, string> = { SUPER_ADMIN: "Super-admin", ORG_ADMIN: "Responsable", AGENT: "Agent" };

export default function Staff() {
  const { user } = useAuth();
  const [staff, setStaff] = useState<StaffMember[] | null>(null);
  const [branches, setBranches] = useState<BranchAdmin[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<StaffMember | "new" | null>(null);

  const load = useCallback(() => {
    Promise.all([api<StaffMember[]>("/admin/staff"), api<BranchAdmin[]>("/admin/branches")])
      .then(([s, b]) => {
        setStaff(s);
        setBranches(b);
      })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  useEffect(load, [load]);

  return (
    <>
      <div className="page-title">
        <h1>Personnel</h1>
        <button className="btn btn-primary" onClick={() => setEditing("new")}>
          + Ajouter un membre
        </button>
      </div>
      {error && <div className="error">{error}</div>}
      {!staff ? (
        <div className="page-loading">Chargement…</div>
      ) : (
        <div className="card table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Nom</th>
                <th>E-mail</th>
                <th>Rôle</th>
                <th>Établissement</th>
                <th>État</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {staff.map((m) => (
                <tr key={m.id}>
                  <td>
                    <strong>{m.fullName}</strong>
                    {m.id === user?.id && <span className="muted small"> (vous)</span>}
                  </td>
                  <td>{m.email}</td>
                  <td>
                    <span className={`badge ${m.role === "ORG_ADMIN" ? "badge-brand" : ""}`}>{ROLE_LABELS[m.role]}</span>
                  </td>
                  <td>{m.branchName ?? <span className="muted">Tous</span>}</td>
                  <td>
                    <span className={`badge ${m.active ? "badge-ok" : ""}`}>{m.active ? "Actif" : "Désactivé"}</span>
                  </td>
                  <td style={{ textAlign: "right" }}>
                    <button className="btn btn-ghost btn-sm" onClick={() => setEditing(m)}>
                      Modifier
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {editing && (
        <StaffForm
          member={editing === "new" ? null : editing}
          branches={branches}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            load();
          }}
        />
      )}
    </>
  );
}

function StaffForm(props: { member: StaffMember | null; branches: BranchAdmin[]; onClose: () => void; onSaved: () => void }) {
  const m = props.member;
  const [form, setForm] = useState({
    fullName: m?.fullName ?? "",
    email: m?.email ?? "",
    password: "",
    role: (m?.role ?? "AGENT") as Role,
    branchId: m?.branchId ?? props.branches[0]?.id ?? null,
    active: m?.active ?? true,
  });
  const [error, setError] = useState<string | null>(null);
  const set = (patch: Partial<typeof form>) => setForm((f) => ({ ...f, ...patch }));

  async function submit(e: FormEvent) {
    e.preventDefault();
    const body = { ...form, password: form.password || null, branchId: form.role === "AGENT" ? form.branchId : null };
    try {
      if (m) await put(`/admin/staff/${m.id}`, body);
      else await post("/admin/staff", body);
      props.onSaved();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <Modal title={m ? "Modifier le membre" : "Nouveau membre"} onClose={props.onClose}>
      <form onSubmit={submit}>
        {error && <div className="error">{error}</div>}
        <div className="field">
          <label htmlFor="m-name">Nom complet</label>
          <input id="m-name" value={form.fullName} onChange={(e) => set({ fullName: e.target.value })} required autoFocus />
        </div>
        <div className="field">
          <label htmlFor="m-email">E-mail de connexion</label>
          <input id="m-email" type="email" value={form.email} onChange={(e) => set({ email: e.target.value })} required disabled={!!m} />
        </div>
        <div className="field">
          <label htmlFor="m-pwd">{m ? "Nouveau mot de passe (laisser vide pour ne pas changer)" : "Mot de passe"}</label>
          <input id="m-pwd" type="password" minLength={8} value={form.password} onChange={(e) => set({ password: e.target.value })} required={!m} autoComplete="new-password" />
        </div>
        <div className="field-row">
          <div className="field">
            <label htmlFor="m-role">Rôle</label>
            <select id="m-role" value={form.role} onChange={(e) => set({ role: e.target.value as Role })}>
              <option value="AGENT">Agent de guichet</option>
              <option value="ORG_ADMIN">Responsable</option>
            </select>
          </div>
          {form.role === "AGENT" && (
            <div className="field">
              <label htmlFor="m-branch">Établissement</label>
              <select id="m-branch" value={form.branchId ?? ""} onChange={(e) => set({ branchId: Number(e.target.value) })} required>
                {props.branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>
        <label className="check" style={{ marginBottom: 14 }}>
          <input type="checkbox" checked={form.active} onChange={(e) => set({ active: e.target.checked })} />
          Compte actif
        </label>
        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={props.onClose}>
            Annuler
          </button>
          <button className="btn btn-primary">Enregistrer</button>
        </div>
      </form>
    </Modal>
  );
}
