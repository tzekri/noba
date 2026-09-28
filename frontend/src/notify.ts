// Alertes du client, sans service payant :
// - page ouverte : son, vibration, notification locale (API Notification) ;
// - page fermée : Web Push (le serveur passe par le service push du navigateur : Google, Mozilla, Apple).
// Sur iPhone, le Web Push n'existe que si le site est ajouté à l'écran d'accueil (iOS 16.4+).

import { api, post } from "./api";
import { playChime } from "./realtime";

let registration: ServiceWorkerRegistration | null = null;

export async function registerServiceWorker() {
  if (!("serviceWorker" in navigator)) return;
  try {
    registration = await navigator.serviceWorker.register("/sw.js");
  } catch {
    registration = null;
  }
}

export function notificationsSupported(): boolean {
  return "Notification" in window;
}

export function notificationsGranted(): boolean {
  return notificationsSupported() && Notification.permission === "granted";
}

export function notificationsDenied(): boolean {
  return notificationsSupported() && Notification.permission === "denied";
}

export async function requestNotifications(): Promise<boolean> {
  if (!notificationsSupported()) return false;
  const result = await Notification.requestPermission();
  return result === "granted";
}

export function pushSupported(): boolean {
  return "serviceWorker" in navigator && "PushManager" in window && notificationsSupported();
}

export function isIos(): boolean {
  return /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
}

/** Site lancé depuis l'icône de l'écran d'accueil (PWA installée). */
export function isStandalone(): boolean {
  return window.matchMedia("(display-mode: standalone)").matches || (navigator as unknown as { standalone?: boolean }).standalone === true;
}

function base64UrlToBytes(value: string): Uint8Array<ArrayBuffer> {
  const base64 = (value + "=".repeat((4 - (value.length % 4)) % 4)).replace(/-/g, "+").replace(/_/g, "/");
  const binary = atob(base64);
  const bytes = new Uint8Array(new ArrayBuffer(binary.length));
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes;
}

function sameKey(a: ArrayBuffer | null | undefined, b: Uint8Array): boolean {
  if (!a) return false;
  const x = new Uint8Array(a);
  return x.length === b.length && x.every((v, i) => v === b[i]);
}

/**
 * Abonne ce navigateur aux notifications push du ticket (permission déjà accordée).
 * Idempotent : peut être rappelé à chaque ouverture de la page.
 */
export async function subscribePush(ticketToken: string): Promise<boolean> {
  if (!pushSupported() || !notificationsGranted()) return false;
  try {
    const reg = await navigator.serviceWorker.ready;
    const { publicKey } = await api<{ publicKey: string }>("/public/push/key");
    const serverKey = base64UrlToBytes(publicKey);
    let subscription = await reg.pushManager.getSubscription();
    // Clé serveur différente (serveur réinstallé) : l'ancien abonnement ne fonctionnerait plus.
    if (subscription && !sameKey(subscription.options.applicationServerKey, serverKey)) {
      await subscription.unsubscribe();
      subscription = null;
    }
    if (!subscription) {
      subscription = await reg.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: serverKey });
    }
    await post(`/public/tickets/${ticketToken}/push`, subscription.toJSON());
    return true;
  } catch {
    return false;
  }
}

/** Alerte locale (page ouverte). Même étiquette que les notifications push : pas de doublon. */
export async function alertUser(title: string, body: string, url: string, tag = "noba-ticket") {
  playChime();
  navigator.vibrate?.([300, 150, 300, 150, 600]);
  if (!notificationsGranted()) return;
  const options: NotificationOptions = { body, icon: "/icon.svg", tag, data: { url } };
  try {
    const reg = registration ?? (await navigator.serviceWorker?.getRegistration());
    if (reg) await reg.showNotification(title, options);
    else new Notification(title, options);
  } catch {
    /* notification refusée par le navigateur : son + vibration suffisent */
  }
}

/**
 * Empêche l'écran de se mettre en veille (Screen Wake Lock API) tant que la page est visible.
 * Le navigateur relâche le verrou quand on change d'onglet : il faut le redemander au retour.
 */
export function wakeLockSupported(): boolean {
  return "wakeLock" in navigator;
}

export async function requestWakeLock(): Promise<WakeLockSentinel | null> {
  if (!wakeLockSupported()) return null;
  try {
    return await navigator.wakeLock.request("screen");
  } catch {
    return null;
  }
}
