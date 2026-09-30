import { Link } from "react-router-dom";
import { useAuth, homeFor } from "../auth";
import { Logo } from "../components/common";
import "./landing.css";

export default function Landing() {
  const { user } = useAuth();
  return (
    <div className="landing">
      <header className="landing-nav container">
        <Logo />
        <nav>
          <a href="#comment">Comment ça marche</a>
          <a href="#fonctions">Fonctionnalités</a>
          {user ? (
            <Link className="btn btn-primary btn-sm" to={homeFor(user.role)}>
              Mon espace
            </Link>
          ) : (
            <>
              <Link to="/login">Connexion</Link>
              <Link className="btn btn-primary btn-sm" to="/register">
                Essayer gratuitement
              </Link>
            </>
          )}
        </nav>
      </header>

      <section className="hero container">
        <div className="hero-text">
          <span className="badge badge-brand">Gestion de files d'attente</span>
          <h1>
            Vos clients attendent <em>où ils veulent</em>, pas debout dans la salle.
          </h1>
          <p className="lead">
            Un QR code à l'entrée, un ticket sur le téléphone, et la file qui avance en direct. Sans application à installer, sans compte client.
          </p>
          <div className="hero-cta">
            <Link className="btn btn-primary btn-lg" to="/register">
              Créer mon espace
            </Link>
            <Link className="btn btn-ghost btn-lg" to="/q/demo">
              Prendre un ticket de démo
            </Link>
          </div>
          <p className="small muted">
            Démo : <Link to="/display/demo">écran TV</Link> · <Link to="/login">guichet agent</Link> (agent@demo.nobty.ma / demo1234)
          </p>
        </div>

        <div className="hero-phone" aria-hidden="true">
          <div className="phone">
            <div className="phone-notch" />
            <div className="phone-screen">
              <div className="small muted" style={{ textTransform: "uppercase", letterSpacing: ".08em", fontWeight: 700, fontSize: 10 }}>
                Votre ticket
              </div>
              <div className="phone-code">B-042</div>
              <div className="small" style={{ fontWeight: 600, color: "var(--ink-2)" }}>
                Ouverture de compte
              </div>
              <div className="phone-sep" />
              <div className="phone-ahead">
                <strong>3</strong> personnes avant vous
              </div>
              <div className="progress">
                <div style={{ width: "70%" }} />
              </div>
              <div className="small muted">Attente estimée ~12 min</div>
              <div className="phone-notif">🔔 On vous prévient quand c'est votre tour</div>
            </div>
          </div>
        </div>
      </section>

      <section id="comment" className="steps container">
        <h2>Comment ça marche</h2>
        <div className="steps-grid">
          <Step n={1} title="Scanner" text="Le client scanne le QR code affiché à l'entrée de votre établissement." />
          <Step n={2} title="Choisir" text="Il choisit le motif de sa visite et reçoit un ticket numérique instantanément." />
          <Step n={3} title="Patienter librement" text="Il suit sa position en direct : café, voiture, courses… Son téléphone sonne quand c'est son tour." />
          <Step n={4} title="Être servi" text="L'agent appelle le suivant d'un clic, le numéro s'affiche sur l'écran TV et le téléphone." />
        </div>
      </section>

      <section id="fonctions" className="features container">
        <h2>Tout ce qu'il faut, rien de superflu</h2>
        <div className="features-grid">
          <Feature icon="📱" title="Suivi temps réel" text="Position, temps d'attente estimé et alertes sur le téléphone du client." />
          <Feature icon="🧑‍💼" title="Interface guichet" text="Appeler, rappeler, marquer absent, transférer vers un autre service." />
          <Feature icon="📺" title="Écran d'affichage" text="Une simple page web en plein écran sur n'importe quelle TV, avec carillon." />
          <Feature icon="🏢" title="Multi-établissements" text="Gérez toutes vos agences, services et guichets depuis un seul back-office." />
          <Feature icon="📊" title="Statistiques" text="Affluence par heure, temps d'attente et de traitement, performance, satisfaction." />
          <Feature icon="⏱️" title="Estimation intelligente" text="Le temps d'attente s'ajuste sur les durées réelles de la journée et les guichets ouverts." />
        </div>
      </section>

      <section className="cta container">
        <div className="cta-box">
          <h2>Prêt à supprimer la file d'attente ?</h2>
          <p>Créez votre espace en une minute et délivrez votre premier ticket aujourd'hui.</p>
          <Link className="btn btn-accent btn-lg" to="/register">
            Commencer gratuitement
          </Link>
        </div>
      </section>

      <footer className="landing-footer container">
        <Logo />
        <span className="muted small">© {new Date().getFullYear()} Nobty — Gestion de files d'attente</span>
      </footer>
    </div>
  );
}

function Step({ n, title, text }: { n: number; title: string; text: string }) {
  return (
    <div className="step">
      <span className="step-n">{n}</span>
      <h3>{title}</h3>
      <p className="muted">{text}</p>
    </div>
  );
}

function Feature({ icon, title, text }: { icon: string; title: string; text: string }) {
  return (
    <div className="card feature">
      <div className="feature-icon">{icon}</div>
      <h3>{title}</h3>
      <p className="muted small">{text}</p>
    </div>
  );
}
