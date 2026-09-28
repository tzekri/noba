import { Link } from "react-router-dom";
import { useAuth, homeFor } from "../auth";
import { Logo } from "../components/common";
import { Icon, type IconName } from "../components/Icon";
import "./landing.css";

export default function Landing() {
  const { user } = useAuth();
  return (
    <div className="landing">
      <div className="landing-top">
        <header className="landing-nav container">
          <Logo />
          <nav>
            <a href="#comment">Fonctionnement</a>
            <a href="#fonctions">Fonctionnalités</a>
            {user ? (
              <Link className="btn btn-primary btn-sm" to={homeFor(user.role)}>
                Mon espace
              </Link>
            ) : (
              <>
                <Link to="/login">Connexion</Link>
                <Link className="btn btn-primary btn-sm" to="/register">
                  Essai gratuit
                </Link>
              </>
            )}
          </nav>
        </header>
      </div>

      <div className="hero-wrap">
        <section className="hero container">
          <div>
            <span className="eyebrow">Gestion de files d'attente</span>
            <h1>
              Une attente <em>organisée</em>, des clients plus sereins.
            </h1>
            <p className="lead">
              Vos clients prennent un ticket en scannant un QR code et suivent la file en temps réel sur leur téléphone. Sans application,
              sans compte, sans matériel coûteux.
            </p>
            <div className="hero-cta">
              <Link className="btn btn-primary btn-lg" to="/register">
                Créer mon espace
              </Link>
              <Link className="btn btn-ghost btn-lg" to="/q/demo">
                Voir la démo
              </Link>
            </div>
            <p className="hero-demo">
              Démo : <Link to="/display/demo">écran d'affichage</Link> · <Link to="/login">interface guichet</Link> (agent@demo.noba / demo1234)
            </p>
          </div>

          <div className="hero-visual" aria-hidden="true">
            <div className="phone">
              <div className="phone-notch" />
              <div className="phone-screen">
                <div className="phone-org">Agence Centre</div>
                <div className="phone-ticket">
                  <div className="small muted" style={{ fontSize: 11 }}>
                    Votre ticket
                  </div>
                  <div className="phone-code">B-042</div>
                  <div className="small" style={{ fontWeight: 500, color: "var(--ink-2)" }}>
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
                </div>
                <div className="phone-notif">
                  <Icon name="bell" size={14} /> Alerte quand c'est votre tour
                </div>
              </div>
            </div>
            <div className="float-card">
              <span className="ico">
                <Icon name="bell" />
              </span>
              <div>
                <strong>A-017 → Guichet 2</strong>
                <div className="muted small">Appel envoyé à l'instant</div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div className="sectors container">
        <strong>Adapté à tout établissement recevant du public</strong>
        <span>Banques</span>
        <span>Cliniques</span>
        <span>Administrations</span>
        <span>Agences commerciales</span>
        <span>Services après-vente</span>
      </div>

      <section id="comment" className="section section-alt">
        <div className="container">
          <div className="lp-head">
            <div className="kicker">Fonctionnement</div>
            <h2>Quatre étapes, zéro file debout</h2>
            <p>Le parcours client tient dans un téléphone ; vos équipes gardent la main depuis un simple navigateur.</p>
          </div>
          <div className="steps-grid">
            <Step n={1} title="Scanner" text="Le client scanne le QR code affiché à l'entrée de l'établissement." />
            <Step n={2} title="Choisir" text="Il sélectionne le motif de sa visite et reçoit un ticket numérique." />
            <Step n={3} title="Patienter librement" text="Il suit sa position en direct et reçoit une alerte à l'approche de son tour." />
            <Step n={4} title="Être servi" text="L'agent appelle le suivant ; le numéro s'affiche sur l'écran et le téléphone." />
          </div>
        </div>
      </section>

      <section id="fonctions" className="section">
        <div className="container">
          <div className="lp-head">
            <div className="kicker">Fonctionnalités</div>
            <h2>Une plateforme complète, simple à déployer</h2>
            <p>Opérationnel en quelques minutes : un navigateur pour les agents, une TV pour l'affichage.</p>
          </div>
          <div className="features-grid">
            <Feature icon="smartphone" title="Suivi en temps réel" text="Position dans la file, temps d'attente estimé et alertes sur le téléphone du client." />
            <Feature icon="headset" title="Interface guichet" text="Appeler, rappeler, marquer absent, transférer un client vers un autre service." />
            <Feature icon="monitor" title="Écran d'affichage" text="Une page web plein écran sur n'importe quelle TV, avec signal sonore à chaque appel." />
            <Feature icon="building" title="Multi-établissements" text="Tous vos sites, services et guichets pilotés depuis un seul back-office." />
            <Feature icon="chart" title="Statistiques" text="Affluence horaire, temps d'attente et de traitement, performance des équipes, satisfaction." />
            <Feature icon="shield" title="Sans compte client" text="Aucune donnée personnelle demandée : un ticket, un lien sécurisé, c'est tout." />
          </div>
        </div>
      </section>

      <section className="cta container">
        <div className="cta-box">
          <h2>Modernisez votre accueil dès aujourd'hui</h2>
          <p>Créez votre espace en une minute et délivrez votre premier ticket immédiatement.</p>
          <Link className="btn btn-white btn-lg" to="/register">
            Commencer gratuitement
          </Link>
        </div>
      </section>

      <footer className="landing-footer container">
        <Logo />
        <span className="muted small">© {new Date().getFullYear()} Noba · Gestion de files d'attente</span>
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

function Feature({ icon, title, text }: { icon: IconName; title: string; text: string }) {
  return (
    <div className="card feature">
      <div className="feature-icon">
        <Icon name={icon} size={20} />
      </div>
      <h3>{title}</h3>
      <p className="muted small">{text}</p>
    </div>
  );
}
