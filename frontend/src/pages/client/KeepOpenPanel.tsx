import { useEffect, useRef, useState } from "react";
import {
  isIos,
  isStandalone,
  notificationsDenied,
  notificationsSupported,
  pushSupported,
  requestNotifications,
  requestWakeLock,
  wakeLockSupported,
} from "../../notify";

/**
 * Encadré affiché pendant l'attente : dit clairement au client s'il sera prévenu page fermée
 * (Web Push actif) ou s'il doit garder la page ouverte, avec un bouton pour empêcher la mise en veille.
 */
export default function KeepOpenPanel({ pushOn, onEnablePush }: { pushOn: boolean; onEnablePush: () => Promise<boolean> }) {
  const [wakeOn, setWakeOn] = useState(false);
  const [asking, setAsking] = useState(false);
  const [denied, setDenied] = useState(notificationsDenied());
  const sentinel = useRef<WakeLockSentinel | null>(null);

  // Le navigateur relâche le verrou quand la page passe en arrière-plan : on le reprend au retour.
  useEffect(() => {
    if (!wakeOn) return;
    let cancelled = false;
    const acquire = async () => {
      if (document.visibilityState !== "visible") return;
      const lock = await requestWakeLock();
      if (cancelled) {
        lock?.release();
        return;
      }
      sentinel.current = lock;
      if (!lock) setWakeOn(false);
    };
    acquire();
    document.addEventListener("visibilitychange", acquire);
    return () => {
      cancelled = true;
      document.removeEventListener("visibilitychange", acquire);
      sentinel.current?.release().catch(() => undefined);
      sentinel.current = null;
    };
  }, [wakeOn]);

  async function enable() {
    setAsking(true);
    const granted = await requestNotifications();
    if (granted) await onEnablePush();
    setDenied(notificationsDenied());
    setAsking(false);
  }

  const iosNeedsInstall = isIos() && !isStandalone();
  const canPush = pushSupported() && !iosNeedsInstall;

  const wakeButton = wakeLockSupported() && (
    <button className="btn btn-ghost keep-wake" onClick={() => setWakeOn((v) => !v)} aria-pressed={wakeOn}>
      {wakeOn ? "☀️ Écran maintenu allumé — désactiver" : "☀️ Garder l'écran allumé"}
    </button>
  );

  if (pushOn) {
    return (
      <div className="keep-panel keep-ok" role="status">
        <div className="keep-title">✓ Alertes activées</div>
        <p>
          Vous serez prévenu <strong>même si vous fermez cette page</strong> ou verrouillez votre téléphone : quand il ne reste que 2
          personnes, puis quand c'est votre tour.
        </p>
        {wakeButton}
      </div>
    );
  }

  return (
    <div className="keep-panel keep-warn" role="status">
      <div className="keep-title">⚠️ Gardez cette page ouverte</div>
      <p>
        Pour l'instant, vous serez prévenu <strong>uniquement si cette page reste ouverte</strong>. Ne fermez pas l'onglet et évitez que
        l'écran se mette en veille.
      </p>

      {canPush && !denied && (
        <button className="btn btn-accent btn-lg btn-block" onClick={enable} disabled={asking}>
          🔔 {asking ? "Activation…" : "Me prévenir même page fermée"}
        </button>
      )}
      {canPush && denied && (
        <p className="small keep-note">
          Les notifications sont bloquées pour ce site. Autorisez-les dans les réglages du navigateur (icône à gauche de l'adresse), puis
          rechargez la page.
        </p>
      )}
      {iosNeedsInstall && (
        <p className="small keep-note">
          <strong>Sur iPhone :</strong> touchez <strong>Partager</strong> ⬆️ puis <strong>« Sur l'écran d'accueil »</strong>, ouvrez Noba
          depuis la nouvelle icône et activez les alertes : vous serez prévenu même application fermée.
        </p>
      )}
      {!notificationsSupported() && !iosNeedsInstall && (
        <p className="small keep-note">Votre navigateur ne permet pas les alertes : gardez cette page ouverte.</p>
      )}
      {wakeButton}
    </div>
  );
}
