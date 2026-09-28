// Service worker minimal : nécessaire pour afficher des notifications sur Android
// (new Notification() y est interdit, seul registration.showNotification() fonctionne).
// Les vraies notifications push (page fermée) arriveront en v2 avec Web Push / VAPID.

self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (event) => event.waitUntil(self.clients.claim()));

// Un clic sur la notification ramène l'utilisateur sur la page de son ticket.
self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url = event.notification.data && event.notification.data.url;
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clients) => {
      for (const client of clients) {
        if (url && client.url.endsWith(url) && "focus" in client) return client.focus();
      }
      if (url) return self.clients.openWindow(url);
    }),
  );
});
