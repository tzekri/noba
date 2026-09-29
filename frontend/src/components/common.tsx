import { useEffect, useRef, type ReactNode } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import QRCode from "qrcode";
import { useAuth } from "../auth";
import type { TicketStatus } from "../types";

export function Logo({ to = "/", light = false }: { to?: string; light?: boolean }) {
  return (
    <Link to={to} className="logo" style={light ? { color: "#fff" } : undefined}>
      <img src="/icon.svg" alt="" className="logo-mark" />
      nobty
    </Link>
  );
}

/** En-tête commun aux écrans du personnel. */
export function StaffHeader({ links }: { links: { to: string; label: string; end?: boolean }[] }) {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  return (
    <header className="staff-header">
      <div className="container">
        <Logo to={links[0]?.to ?? "/"} />
        <nav className="staff-nav">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end} className={({ isActive }) => (isActive ? "active" : "")}>
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="staff-user">
          <span className="name">
            <strong>{user?.fullName}</strong>
            {user?.organizationName && <span className="muted"> · {user.organizationName}</span>}
          </span>
          <button
            className="btn btn-ghost btn-sm"
            onClick={() => {
              signOut();
              navigate("/login");
            }}
          >
            Déconnexion
          </button>
        </div>
      </div>
    </header>
  );
}

export function staffLinks(role: string | undefined) {
  if (role === "ORG_ADMIN") {
    return [
      { to: "/admin", label: "Établissements", end: true },
      { to: "/admin/staff", label: "Personnel" },
      { to: "/admin/stats", label: "Statistiques" },
      { to: "/agent", label: "Guichet" },
    ];
  }
  return [{ to: "/agent", label: "Guichet" }];
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <h2 style={{ fontSize: 20 }}>{title}</h2>
        {children}
      </div>
    </div>
  );
}

export function QrCanvas({ value, size = 200, dark = "#14201e" }: { value: string; size?: number; dark?: string }) {
  const ref = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    if (ref.current) {
      QRCode.toCanvas(ref.current, value, { width: size, margin: 1, color: { dark, light: "#ffffff" } });
    }
  }, [value, size, dark]);
  return <canvas ref={ref} width={size} height={size} style={{ borderRadius: 8 }} />;
}

const STATUS_LABELS: Record<TicketStatus, string> = {
  WAITING: "En attente",
  CALLED: "Appelé",
  SERVING: "En cours",
  DONE: "Terminé",
  NO_SHOW: "Absent",
  CANCELLED: "Annulé",
  EXPIRED: "Expiré",
};

export function StatusBadge({ status }: { status: TicketStatus }) {
  const cls =
    status === "CALLED" ? "badge-warn" : status === "SERVING" ? "badge-brand" : status === "DONE" ? "badge-ok" : status === "NO_SHOW" ? "badge-danger" : "";
  return <span className={`badge ${cls}`}>{STATUS_LABELS[status]}</span>;
}

export function formatTime(iso?: string) {
  if (!iso) return "—";
  return new Date(iso).toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
}

export function minutesSince(iso?: string, now = Date.now()) {
  if (!iso) return 0;
  return Math.max(0, Math.floor((now - new Date(iso).getTime()) / 60000));
}

export function formatWait(minutes?: number) {
  if (minutes === undefined || minutes === null) return "—";
  if (minutes <= 0) return "moins d'1 min";
  if (minutes < 60) return `~${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `~${h} h ${m.toString().padStart(2, "0")}`;
}
