import { useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { errorMessage, post } from "../api";
import { homeFor, useAuth } from "../auth";
import { Logo } from "../components/common";
import type { AuthResponse } from "../types";

export default function Login() {
  const { user, signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={homeFor(user.role)} replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await post<AuthResponse>("/auth/login", { email, password });
      signIn(res);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from ?? homeFor(res.user.role), { replace: true });
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card card">
        <Logo />
        <h1 style={{ fontSize: 24, marginTop: 20 }}>Espace personnel</h1>
        <p className="muted small" style={{ marginTop: -4 }}>Agents, responsables et opérateurs.</p>
        <form onSubmit={submit}>
          {error && <div className="error">{error}</div>}
          <div className="field">
            <label htmlFor="email">E-mail</label>
            <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus autoComplete="username" />
          </div>
          <div className="field">
            <label htmlFor="password">Mot de passe</label>
            <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
          </div>
          <button className="btn btn-primary btn-block btn-lg" disabled={busy}>
            {busy ? "Connexion…" : "Se connecter"}
          </button>
        </form>
        <p className="small" style={{ textAlign: "center", marginTop: 18 }}>
          Pas encore de compte ? <Link to="/register">Créer mon espace</Link>
        </p>
        <details className="demo-accounts">
          <summary>Comptes de démonstration</summary>
          <ul>
            <li>
              Responsable : <code>admin@demo.nobty.ma</code>
            </li>
            <li>
              Agent : <code>agent@demo.nobty.ma</code>
            </li>
            <li>
              Mot de passe : <code>demo1234</code>
            </li>
          </ul>
        </details>
      </div>
    </div>
  );
}
