// Service worker Noba.
// - Affiche les notifications Web Push envoyées par le serveur, même quand la page du ticket est fermée.
// - Nécessaire aussi pour les notifications locales sur Android (new Notification() y est interdit).

self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (event) => event.waitUntil(self.clients.claim()));

self.addEventListener("push", (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch {
    data = { title: "Noba", body: event.data ? event.data.text() : "" };
  }
  event.waitUntil(
    (async () => {
      // Page du ticket ouverte et visible : elle affiche déjà son alerte (son, vibration, écran orange).
      const clients = await self.clients.matchAll({ type: "window", includeUncontrolled: true });
      const pageVisible = clients.some(
        (c) => c.visibilityState === "visible" && data.url && new URL(c.url).pathname === data.url,
      );
      if (pageVisible) return;
      await self.registration.showNotification(data.title || "Noba", {
        body: data.body || "",
        icon: "/icon.svg",
        badge: "/icon.svg",
        tag: data.tag || "noba",
        renotify: true,
        requireInteraction: !!data.urgent,
        vibrate: data.urgent ? [300, 150, 300, 150, 600] : [200, 100, 200],
        data: { url: data.url || "/" },
      });
    })(),
  );
});

// Un clic sur la notification ramène l'utilisateur sur la page de son ticket.
self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url = event.notification.data && event.notification.data.url;
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clients) => {
      for (const client of clients) {
        if (url && new URL(client.url).pathname === url && "focus" in client) return client.focus();
      }
      if (url) return self.clients.openWindow(url);
    }),
  );
});
