// Notifications gratuites, sans serveur tiers : API Notification du navigateur + vibration + son.
// Elles fonctionnent tant que la page du ticket est ouverte (même en arrière-plan sur la plupart
// des navigateurs). Les notifications page fermée (Web Push/VAPID) sont prévues en v2.

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

export async function requestNotifications(): Promise<boolean> {
  if (!notificationsSupported()) return false;
  const result = await Notification.requestPermission();
  return result === "granted";
}

export async function alertUser(title: string, body: string, url: string) {
  playChime();
  navigator.vibrate?.([300, 150, 300, 150, 600]);
  if (!notificationsGranted()) return;
  const options: NotificationOptions = { body, icon: "/icon.svg", tag: "noba-ticket", data: { url } };
  try {
    const reg = registration ?? (await navigator.serviceWorker?.getRegistration());
    if (reg) await reg.showNotification(title, options);
    else new Notification(title, options);
  } catch {
    /* notification refusée par le navigateur : son + vibration suffisent */
  }
}
