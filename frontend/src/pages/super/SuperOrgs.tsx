import { useEffect, useState } from "react";
import { api, errorMessage, patch } from "../../api";
import { StaffHeader } from "../../components/common";
import type { OrganizationAdmin } from "../../types";

/** Console de l'opérateur Nobty : organisations clientes de la plateforme. */
export default function SuperOrgs() {
  const [orgs, setOrgs] = useState<OrganizationAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<OrganizationAdmin[]>("/super/organizations")
      .then(setOrgs)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  async function toggle(org: OrganizationAdmin) {
    const verb = org.active ? "Suspendre" : "Réactiver";
    if (!window.confirm(`${verb} « ${org.name} » ?`)) return;
    try {
      const updated = await patch<OrganizationAdmin>(`/super/organizations/${org.id}`, { active: !org.active });
      setOrgs((list) => list?.map((o) => (o.id === org.id ? updated : o)) ?? null);
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <>
      <StaffHeader links={[{ to: "/super", label: "Organisations" }]} />
      <main className="staff-main">
        <div className="container">
          <div className="page-title">
            <h1>Organisations clientes</h1>
            {orgs && <span className="muted">{orgs.length} au total</span>}
          </div>
          {error && <div className="error">{error}</div>}
          {!orgs ? (
            <div className="page-loading">Chargement…</div>
          ) : (
            <div className="card table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Organisation</th>
                    <th>Inscrite le</th>
                    <th>Établissements</th>
                    <th>Personnel</th>
                    <th>État</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {orgs.map((o) => (
                    <tr key={o.id}>
                      <td>
                        <strong>{o.name}</strong>
                        <div className="small muted">{o.slug}</div>
                      </td>
                      <td>{new Date(o.createdAt).toLocaleDateString("fr-FR")}</td>
                      <td>{o.branches}</td>
                      <td>{o.staff}</td>
                      <td>
                        <span className={`badge ${o.active ? "badge-ok" : "badge-danger"}`}>{o.active ? "Active" : "Suspendue"}</span>
                      </td>
                      <td style={{ textAlign: "right" }}>
                        <button className={`btn btn-sm ${o.active ? "btn-danger" : "btn-ghost"}`} onClick={() => toggle(o)}>
                          {o.active ? "Suspendre" : "Réactiver"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </>
  );
}
