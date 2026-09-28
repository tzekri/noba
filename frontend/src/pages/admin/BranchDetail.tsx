import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import QRCode from "qrcode";
import { api, errorMessage, post, put } from "../../api";
import { Modal, QrCanvas } from "../../components/common";
import type { BranchAdmin, CounterAdmin, ServiceAdmin } from "../../types";

/** Configuration d'un établissement : infos, QR code, services (files) et guichets. */
export default function BranchDetail() {
  const { id } = useParams();
  const [branch, setBranch] = useState<BranchAdmin | null>(null);
  const [services, setServices] = useState<ServiceAdmin[]>([]);
  const [counters, setCounters] = useState<CounterAdmin[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [editingBranch, setEditingBranch] = useState(false);
  const [editService, setEditService] = useState<ServiceAdmin | "new" | null>(null);
  const [editCounter, setEditCounter] = useState<CounterAdmin | "new" | null>(null);

  const load = useCallback(() => {
    Promise.all([
      api<BranchAdmin[]>("/admin/branches"),
      api<ServiceAdmin[]>(`/admin/branches/${id}/services`),
      api<CounterAdmin[]>(`/admin/branches/${id}/counters`),
    ])
      .then(([bs, ss, cs]) => {
        setBranch(bs.find((b) => String(b.id) === id) ?? null);
        setServices(ss);
        setCounters(cs);
      })
      .catch((e) => setError(errorMessage(e)));
  }, [id]);

  useEffect(load, [load]);

  if (!branch) return error ? <div className="error">{error}</div> : <div className="page-loading">Chargement…</div>;

  const ticketUrl = `${window.location.origin}/q/${branch.code}`;
  const displayUrl = `${window.location.origin}/display/${branch.code}`;

  async function toggleOpen() {
    if (!branch) return;
    try {
      setBranch(await put<BranchAdmin>(`/admin/branches/${branch.id}`, { ...branch, open: !branch.open }));
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <>
      <div className="small" style={{ marginBottom: 8 }}>
        <Link to="/admin">← Établissements</Link>
      </div>
      <div className="page-title">
        <div>
          <h1>{branch.name}</h1>
          <div className="muted small">{branch.address ?? "Adresse non renseignée"}</div>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <button className={`btn ${branch.open ? "btn-ghost" : "btn-primary"}`} onClick={toggleOpen}>
            {branch.open ? "Suspendre la prise de tickets" : "Ouvrir la prise de tickets"}
          </button>
          <button className="btn btn-ghost" onClick={() => setEditingBranch(true)}>
            Modifier
          </button>
        </div>
      </div>
      {error && <div className="error">{error}</div>}

      <div className="detail-grid">
        <div>
          <section className="card">
            <div className="section-head">
              <h2>Services (files d'attente)</h2>
              <button className="btn btn-primary btn-sm" onClick={() => setEditService("new")}>
                + Ajouter
              </button>
            </div>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Préfixe</th>
                    <th>Nom</th>
                    <th>Durée moy.</th>
                    <th>Limite/jour</th>
                    <th>État</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {services.map((s) => (
                    <tr key={s.id}>
                      <td>
                        <span className="service-chip">{s.prefix}</span>
                      </td>
                      <td>
                        <strong>{s.name}</strong>
                        {s.description && <div className="small muted">{s.description}</div>}
                      </td>
                      <td>{s.defaultServiceMinutes} min</td>
                      <td>{s.dailyLimit ?? "—"}</td>
                      <td>
                        <span className={`badge ${s.active ? "badge-ok" : ""}`}>{s.active ? "Actif" : "Inactif"}</span>
                        {s.active && !counters.some((c) => c.active && c.serviceIds.includes(s.id)) && (
                          <span className="badge badge-danger" style={{ marginLeft: 6 }} title="Les tickets de ce service ne peuvent pas être appelés">
                            Aucun guichet
                          </span>
                        )}
                      </td>
                      <td style={{ textAlign: "right" }}>
                        <button className="btn btn-ghost btn-sm" onClick={() => setEditService(s)}>
                          Modifier
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="card" style={{ marginTop: 20 }}>
            <div className="section-head">
              <h2>Guichets</h2>
              <button className="btn btn-primary btn-sm" onClick={() => setEditCounter("new")}>
                + Ajouter
              </button>
            </div>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Nom</th>
                    <th>Services traités</th>
                    <th>Occupé par</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {counters.map((c) => (
                    <tr key={c.id} style={c.active ? undefined : { opacity: 0.5 }}>
                      <td>
                        <strong>{c.name}</strong>
                        {!c.active && <span className="badge" style={{ marginLeft: 8 }}>Désactivé</span>}
                      </td>
                      <td>
                        {c.serviceIds.map((sid) => {
                          const s = services.find((x) => x.id === sid);
                          return s ? (
                            <span key={sid} className="badge badge-brand" style={{ marginRight: 4 }}>
                              {s.prefix} · {s.name}
                            </span>
                          ) : null;
                        })}
                      </td>
                      <td>{c.agentName ?? <span className="muted">—</span>}</td>
                      <td style={{ textAlign: "right" }}>
                        <button className="btn btn-ghost btn-sm" onClick={() => setEditCounter(c)}>
                          Modifier
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <aside>
          <section className="card qr-card">
            <h2>QR code à afficher</h2>
            <p className="small muted">Imprimez-le à l'entrée : les clients le scannent pour prendre un ticket.</p>
            <QrCanvas value={ticketUrl} size={220} />
            <a className="small" href={ticketUrl} target="_blank" rel="noreferrer" style={{ wordBreak: "break-all" }}>
              {ticketUrl}
            </a>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", justifyContent: "center" }}>
              <button className="btn btn-primary btn-sm" onClick={() => printPoster(branch, ticketUrl)}>
                Imprimer l'affiche
              </button>
              <button className="btn btn-ghost btn-sm" onClick={() => downloadQr(branch, ticketUrl)}>
                Télécharger PNG
              </button>
            </div>
          </section>
          <section className="card" style={{ marginTop: 16 }}>
            <h2>Écran d'affichage</h2>
            <p className="small muted">Ouvrez ce lien en plein écran (F11) sur la TV de la salle d'attente.</p>
            <a className="btn btn-ghost btn-sm" href={displayUrl} target="_blank" rel="noreferrer">
              Ouvrir l'écran TV ↗
            </a>
          </section>
        </aside>
      </div>

      {editingBranch && (
        <BranchForm
          branch={branch}
          onClose={() => setEditingBranch(false)}
          onSaved={(b) => {
            setBranch(b);
            setEditingBranch(false);
          }}
        />
      )}
      {editService && (
        <ServiceForm
          branchId={branch.id}
          service={editService === "new" ? null : editService}
          nextOrder={services.length + 1}
          onClose={() => setEditService(null)}
          onSaved={() => {
            setEditService(null);
            load();
          }}
        />
      )}
      {editCounter && (
        <CounterForm
          branchId={branch.id}
          counter={editCounter === "new" ? null : editCounter}
          services={services}
          defaultName={`Guichet ${counters.length + 1}`}
          onClose={() => setEditCounter(null)}
          onSaved={() => {
            setEditCounter(null);
            load();
          }}
        />
      )}
    </>
  );
}

async function downloadQr(branch: BranchAdmin, url: string) {
  const dataUrl = await QRCode.toDataURL(url, { width: 1024, margin: 2 });
  const a = document.createElement("a");
  a.href = dataUrl;
  a.download = `noba-qr-${branch.code}.png`;
  a.click();
}

/** Affiche A4 prête à imprimer, générée dans une nouvelle fenêtre. */
async function printPoster(branch: BranchAdmin, url: string) {
  const dataUrl = await QRCode.toDataURL(url, { width: 800, margin: 1 });
  const w = window.open("", "_blank");
  if (!w) return;
  const esc = (s: string) => s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]!);
  w.document.write(`<!doctype html><html lang="fr"><head><meta charset="utf-8"><title>Affiche ${esc(branch.name)}</title>
<style>
  body{font-family:system-ui,sans-serif;text-align:center;margin:0;padding:48px 32px;color:#14201e}
  h1{font-size:44px;margin:0 0 8px} h2{font-size:24px;font-weight:500;margin:0 0 36px;color:#4a5a57}
  img{width:380px;height:380px} ol{display:inline-block;text-align:left;font-size:22px;line-height:1.8;margin-top:28px}
  .brand{margin-top:40px;color:#0f766e;font-weight:800;font-size:20px}
</style></head><body>
  <h1>Prenez votre ticket ici</h1>
  <h2>${esc(branch.name)}</h2>
  <img src="${dataUrl}" alt="QR code">
  <div><ol><li>Scannez le QR code avec votre téléphone</li><li>Choisissez votre service</li><li>Suivez votre tour en direct, installez-vous !</li></ol></div>
  <div class="brand">noba</div>
  <script>window.onload=()=>setTimeout(()=>window.print(),300)</script>
</body></html>`);
  w.document.close();
}

function BranchForm({ branch, onClose, onSaved }: { branch: BranchAdmin; onClose: () => void; onSaved: (b: BranchAdmin) => void }) {
  const [name, setName] = useState(branch.name);
  const [address, setAddress] = useState(branch.address ?? "");
  const [error, setError] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    try {
      onSaved(await put<BranchAdmin>(`/admin/branches/${branch.id}`, { name, address, open: branch.open }));
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <Modal title="Modifier l'établissement" onClose={onClose}>
      <form onSubmit={submit}>
        {error && <div className="error">{error}</div>}
        <div className="field">
          <label htmlFor="bf-name">Nom</label>
          <input id="bf-name" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>
        <div className="field">
          <label htmlFor="bf-address">Adresse</label>
          <input id="bf-address" value={address} onChange={(e) => setAddress(e.target.value)} />
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            Annuler
          </button>
          <button className="btn btn-primary">Enregistrer</button>
        </div>
      </form>
    </Modal>
  );
}

function ServiceForm(props: { branchId: number; service: ServiceAdmin | null; nextOrder: number; onClose: () => void; onSaved: () => void }) {
  const s = props.service;
  const [form, setForm] = useState({
    name: s?.name ?? "",
    description: s?.description ?? "",
    prefix: s?.prefix ?? "",
    defaultServiceMinutes: s?.defaultServiceMinutes ?? 5,
    dailyLimit: s?.dailyLimit?.toString() ?? "",
    active: s?.active ?? true,
    sortOrder: s?.sortOrder ?? props.nextOrder,
    attachToAllCounters: true,
  });
  const [error, setError] = useState<string | null>(null);
  const set = (patch: Partial<typeof form>) => setForm((f) => ({ ...f, ...patch }));

  async function submit(e: FormEvent) {
    e.preventDefault();
    const body = { ...form, dailyLimit: form.dailyLimit ? Number(form.dailyLimit) : null };
    try {
      if (s) await put(`/admin/services/${s.id}`, body);
      else await post(`/admin/branches/${props.branchId}/services`, body);
      props.onSaved();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <Modal title={s ? "Modifier le service" : "Nouveau service"} onClose={props.onClose}>
      <form onSubmit={submit}>
        {error && <div className="error">{error}</div>}
        <div className="field-row">
          <div className="field">
            <label htmlFor="s-name">Nom</label>
            <input id="s-name" value={form.name} onChange={(e) => set({ name: e.target.value })} required autoFocus placeholder="Retrait" />
          </div>
          <div className="field">
            <label htmlFor="s-prefix">Préfixe des tickets</label>
            <input id="s-prefix" value={form.prefix} onChange={(e) => set({ prefix: e.target.value.toUpperCase().replace(/[^A-Z]/g, "").slice(0, 3) })} required placeholder="A" />
          </div>
        </div>
        <div className="field">
          <label htmlFor="s-desc">Description (visible par le client)</label>
          <input id="s-desc" value={form.description} onChange={(e) => set({ description: e.target.value })} />
        </div>
        <div className="field-row">
          <div className="field">
            <label htmlFor="s-min">Durée moyenne (min)</label>
            <input id="s-min" type="number" min={1} max={240} value={form.defaultServiceMinutes} onChange={(e) => set({ defaultServiceMinutes: Number(e.target.value) })} required />
          </div>
          <div className="field">
            <label htmlFor="s-limit">Tickets max / jour</label>
            <input id="s-limit" type="number" min={1} value={form.dailyLimit} onChange={(e) => set({ dailyLimit: e.target.value })} placeholder="Illimité" />
          </div>
        </div>
        <div className="field-row">
          <div className="field">
            <label htmlFor="s-order">Ordre d'affichage</label>
            <input id="s-order" type="number" value={form.sortOrder} onChange={(e) => set({ sortOrder: Number(e.target.value) })} />
          </div>
          <label className="check" style={{ marginTop: 20 }}>
            <input type="checkbox" checked={form.active} onChange={(e) => set({ active: e.target.checked })} />
            Service actif
          </label>
        </div>
        {!s && (
          <label className="check" style={{ marginBottom: 14 }}>
            <input type="checkbox" checked={form.attachToAllCounters} onChange={(e) => set({ attachToAllCounters: e.target.checked })} />
            Traiter ce service à tous les guichets
          </label>
        )}
        <p className="small muted">La durée moyenne sert à estimer l'attente tant que l'historique du jour est insuffisant ; ensuite Noba utilise les durées réelles.</p>
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

function CounterForm(props: {
  branchId: number;
  counter: CounterAdmin | null;
  services: ServiceAdmin[];
  defaultName: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const c = props.counter;
  const [name, setName] = useState(c?.name ?? props.defaultName);
  const [active, setActive] = useState(c?.active ?? true);
  const [serviceIds, setServiceIds] = useState<number[]>(c?.serviceIds ?? props.services.filter((s) => s.active).map((s) => s.id));
  const [error, setError] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    try {
      const body = { name, active, serviceIds };
      if (c) await put(`/admin/counters/${c.id}`, body);
      else await post(`/admin/branches/${props.branchId}/counters`, body);
      props.onSaved();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <Modal title={c ? "Modifier le guichet" : "Nouveau guichet"} onClose={props.onClose}>
      <form onSubmit={submit}>
        {error && <div className="error">{error}</div>}
        <div className="field">
          <label htmlFor="c-name">Nom affiché aux clients</label>
          <input id="c-name" value={name} onChange={(e) => setName(e.target.value)} required autoFocus />
        </div>
        <div className="field">
          <label>Services traités à ce guichet</label>
          {props.services.map((s) => (
            <label key={s.id} className="check">
              <input
                type="checkbox"
                checked={serviceIds.includes(s.id)}
                onChange={(e) => setServiceIds((ids) => (e.target.checked ? [...ids, s.id] : ids.filter((x) => x !== s.id)))}
              />
              {s.prefix} · {s.name}
              {!s.active && <span className="muted small">(inactif)</span>}
            </label>
          ))}
        </div>
        <label className="check" style={{ marginBottom: 14 }}>
          <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
          Guichet actif
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
