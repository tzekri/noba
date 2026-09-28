import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { errorMessage, post } from "../api";
import { useAuth } from "../auth";
import { Logo } from "../components/common";
import type { AuthResponse } from "../types";

/** Inscription en libre-service d'une organisation (banque, clinique, mairie…). */
export default function Register() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ organizationName: "", fullName: "", email: "", password: "" });
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const set = (patch: Partial<typeof form>) => setForm((f) => ({ ...f, ...patch }));

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      signIn(await post<AuthResponse>("/auth/register", form));
      navigate("/admin", { replace: true });
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card card">
        <Logo />
        <h1 style={{ fontSize: 24, marginTop: 20 }}>Créez votre espace Noba</h1>
        <p className="muted small" style={{ marginTop: -4 }}>
          Un établissement, un service et un guichet sont créés automatiquement : vous pourrez délivrer votre premier ticket dans une minute.
        </p>
        <form onSubmit={submit}>
          {error && <div className="error">{error}</div>}
          <div className="field">
            <label htmlFor="org">Nom de votre organisation</label>
            <input id="org" value={form.organizationName} onChange={(e) => set({ organizationName: e.target.value })} required autoFocus placeholder="Clinique Al Amal, Mairie de…" />
          </div>
          <div className="field">
            <label htmlFor="name">Votre nom</label>
            <input id="name" value={form.fullName} onChange={(e) => set({ fullName: e.target.value })} required autoComplete="name" />
          </div>
          <div className="field">
            <label htmlFor="email">E-mail professionnel</label>
            <input id="email" type="email" value={form.email} onChange={(e) => set({ email: e.target.value })} required autoComplete="email" />
          </div>
          <div className="field">
            <label htmlFor="password">Mot de passe (8 caractères min.)</label>
            <input id="password" type="password" minLength={8} value={form.password} onChange={(e) => set({ password: e.target.value })} required autoComplete="new-password" />
          </div>
          <button className="btn btn-primary btn-block btn-lg" disabled={busy}>
            {busy ? "Création…" : "Créer mon espace"}
          </button>
        </form>
        <p className="small" style={{ textAlign: "center", marginTop: 18 }}>
          Déjà inscrit ? <Link to="/login">Se connecter</Link>
        </p>
      </div>
    </div>
  );
}
