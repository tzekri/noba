import QRCode from "qrcode";
import type { TicketView } from "./types";

export function trackingUrl(token: string) {
  return `${window.location.origin}/t/${token}`;
}

/**
 * Imprime un ticket au format 80 mm (imprimante thermique de guichet ; fonctionne aussi sur A4).
 * Le QR code permet à un proche équipé d'un smartphone de suivre la file pour le client.
 */
export async function printTicket(ticket: TicketView) {
  const qr = await QRCode.toDataURL(trackingUrl(ticket.token), { width: 360, margin: 1 });
  const w = window.open("", "_blank", "width=420,height=700");
  if (!w) return false;
  const esc = (s: string) => s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]!);
  const date = new Date(ticket.createdAt);
  const when = `${date.toLocaleDateString("fr-FR")} à ${date.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}`;
  const ahead =
    ticket.peopleAhead === undefined || ticket.peopleAhead === null
      ? ""
      : `<p class="ahead">${ticket.peopleAhead === 0 ? "Vous êtes le prochain" : `${ticket.peopleAhead} personne(s) avant vous`}</p>`;
  w.document.write(`<!doctype html><html lang="fr"><head><meta charset="utf-8"><title>Ticket ${esc(ticket.code)}</title>
<style>
  @page { size: 80mm auto; margin: 4mm; }
  body { font-family: system-ui, sans-serif; width: 72mm; margin: 0 auto; text-align: center; color: #000; }
  .org { font-size: 12px; text-transform: uppercase; letter-spacing: .08em; }
  .branch { font-size: 15px; font-weight: 700; margin-bottom: 8px; }
  .code { font-size: 56px; font-weight: 800; letter-spacing: -.02em; line-height: 1; margin: 6px 0; }
  .service { font-size: 16px; font-weight: 600; }
  .ahead { font-size: 14px; margin: 8px 0 0; }
  hr { border: 0; border-top: 1px dashed #000; margin: 12px 0; }
  .recovery { font-size: 13px; }
  .recovery b { font-size: 20px; letter-spacing: .15em; }
  img { width: 36mm; height: 36mm; }
  .small { font-size: 11px; }
</style></head><body>
  <div class="org">${esc(ticket.organizationName)}</div>
  <div class="branch">${esc(ticket.branchName)}</div>
  <div class="code">${esc(ticket.code)}</div>
  <div class="service">${esc(ticket.serviceName)}</div>
  ${ahead}
  <hr>
  <div class="recovery">Code de suivi : <b>${esc(ticket.recoveryCode ?? "")}</b></div>
  <img src="${qr}" alt="QR code de suivi">
  <div class="small">Scannez pour suivre votre tour sur un téléphone.</div>
  <hr>
  <div class="small">${when}<br>Merci de patienter, votre numéro s'affichera à l'écran.</div>
  <script>window.onload = () => setTimeout(() => { window.print(); }, 250);<\/script>
</body></html>`);
  w.document.close();
  return true;
}
