import { useState } from "react";
import { errorMessage, post } from "../../api";
import { formatWait, Modal, QrCanvas } from "../../components/common";
import { printTicket, trackingUrl } from "../../printTicket";
import type { ServiceBoard, TicketView } from "../../types";

/**
 * Délivrance d'un ticket par l'agent, pour un client sans smartphone :
 * choix du service → ticket à imprimer (ou à noter / annoncer oralement).
 */
export default function IssueTicketModal({ branchId, services, onClose }: { branchId: number; services: ServiceBoard[]; onClose: () => void }) {
  const [ticket, setTicket] = useState<TicketView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [autoPrint, setAutoPrint] = useState(() => {
    try {
      return localStorage.getItem("noba.agent.autoprint") === "1";
    } catch {
      return false;
    }
  });

  async function issue(serviceId: number) {
    setBusy(true);
    setError(null);
    try {
      const t = await post<TicketView>(`/agent/branches/${branchId}/tickets`, { serviceId });
      setTicket(t);
      if (autoPrint) printTicket(t);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  function toggleAutoPrint(value: boolean) {
    setAutoPrint(value);
    try {
      localStorage.setItem("noba.agent.autoprint", value ? "1" : "0");
    } catch {
      /* stockage indisponible */
    }
  }

  return (
    <Modal title={ticket ? "Ticket délivré" : "Nouveau ticket"} onClose={onClose}>
      {error && <div className="error">{error}</div>}

      {!ticket ? (
        <>
          <p className="small muted" style={{ marginTop: -4 }}>
            Pour un client sans smartphone : choisissez le motif de sa visite.
          </p>
          <div className="issue-services">
            {services.map((s) => (
              <button key={s.id} className="issue-service" disabled={busy} onClick={() => issue(s.id)}>
                <span className="service-chip">{s.prefix}</span>
                <span style={{ flex: 1, textAlign: "left" }}>
                  <strong>{s.name}</strong>
                  <span className="small muted" style={{ display: "block" }}>
                    {s.waiting} en attente · {formatWait(s.estimatedWaitMinutes)}
                  </span>
                </span>
              </button>
            ))}
            {services.length === 0 && <p className="muted">Aucun service actif.</p>}
          </div>
          <label className="check" style={{ marginTop: 14 }}>
            <input type="checkbox" checked={autoPrint} onChange={(e) => toggleAutoPrint(e.target.checked)} />
            Imprimer automatiquement
          </label>
        </>
      ) : (
        <>
          <div className="issued">
            <div className="issued-code">{ticket.code}</div>
            <div style={{ fontWeight: 600 }}>{ticket.serviceName}</div>
            <div className="small muted" style={{ marginTop: 4 }}>
              {ticket.peopleAhead === 0 ? "Prochain à passer" : `${ticket.peopleAhead} personne(s) avant · ${formatWait(ticket.estimatedWaitMinutes)}`}
            </div>
            <div className="issued-recovery">
              Code de suivi <strong>{ticket.recoveryCode}</strong>
            </div>
            <QrCanvas value={trackingUrl(ticket.token)} size={140} />
            <div className="small muted">Un proche peut scanner ce QR code pour suivre la file.</div>
          </div>
          <div className="modal-actions" style={{ flexWrap: "wrap" }}>
            <button className="btn btn-ghost" onClick={onClose}>
              Fermer
            </button>
            <button className="btn btn-ghost" onClick={() => setTicket(null)}>
              Autre ticket
            </button>
            <button className="btn btn-primary" onClick={() => printTicket(ticket)}>
              Imprimer
            </button>
          </div>
        </>
      )}
    </Modal>
  );
}
