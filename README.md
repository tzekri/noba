# Noba — gestion de files d'attente (SaaS)

Le client scanne un QR code à l'entrée d'un établissement, choisit un service, reçoit un ticket sur son
téléphone et suit sa position en temps réel. Il n'a besoin ni d'application ni de compte. L'agent appelle
le suivant d'un clic, et le numéro s'affiche sur l'écran TV et sur le téléphone du client.

- **Backend** : Spring Boot 3.3 / Java 21, JPA, Spring Security + JWT (HS256), PostgreSQL (H2 en local).
- **Frontend** : React 18 + TypeScript + Vite, PWA légère (manifest + service worker).
- **Temps réel** : Server-Sent Events, un flux par établissement (`/api/public/branches/{code}/stream`).

## Démarrer en local (sans PostgreSQL)

```bash
# Backend (port 8090), base H2 en mémoire + données de démo
cd backend
./mvnw -o -DskipTests package
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local

# Frontend (port 5174), /api est relayé vers le backend
cd frontend
npm install
npm run dev
```

Avec Docker : `docker compose up --build` (PostgreSQL + backend), puis `npm run dev` pour le frontend.

Pour tester sur un vrai téléphone : même Wi-Fi que le PC, puis ouvrez `http://<ip-du-pc>:5174/q/demo`.

## Déployer sur Render

Le `Dockerfile` à la racine construit une **image unique** : le frontend React est compilé puis embarqué
dans le jar Spring Boot, qui sert tout sur le même domaine (pas de CORS, flux SSE direct).
Le fichier `render.yaml` décrit l'infrastructure.

1. Pousser le dépôt sur GitHub ou GitLab.
2. Render → **New → Blueprint** → choisir le dépôt. Render crée :
   - `noba-db` : PostgreSQL ;
   - `noba` : Web Service Docker, relié à la base.
3. Renseigner les deux valeurs demandées : `SUPER_ADMIN_EMAIL` et `SUPER_ADMIN_PASSWORD`.
4. Attendre la fin du build (quelques minutes), puis ouvrir `https://<service>.onrender.com`.

Variables utiles :

| Variable | Rôle |
|---|---|
| `JWT_SECRET` | Générée par Render |
| `SEED_DEMO` | `true` crée « Banque Démo » (`/q/demo`). **Mettre `false` pour de vrais clients** : les comptes de démo ont des mots de passe publics |
| `APP_TIMEZONE` | Fuseau de la « journée » des tickets (`Africa/Casablanca`) |

Offre gratuite Render : le service s'endort après 15 min sans visite (environ 1 min au réveil) et la base
gratuite expire au bout de 30 jours.

## Écrans

| URL | Pour qui | Rôle |
|---|---|---|
| `/` | prospects | Page d'accueil |
| `/register` | nouvelle organisation | Inscription en libre-service (crée un établissement, un service et un guichet) |
| `/q/{code}` | client | Prise de ticket (cible du QR code) |
| `/t/{jeton}` | client | Suivi du ticket en temps réel, alertes, annulation, avis |
| `/display/{code}` | salle d'attente | Écran TV : appels en cours, files, QR code |
| `/agent` | agent / responsable | Guichet : ouvrir, appeler le suivant, rappeler, absent, transférer |
| `/admin` | responsable | Établissements, services, guichets, QR code / affiche, personnel, statistiques |
| `/super` | opérateur Noba | Organisations clientes (suspendre / réactiver) |

## Comptes de démonstration (profil local)

| Rôle | E-mail | Mot de passe |
|---|---|---|
| Responsable | `admin@demo.noba` | `demo1234` |
| Agent | `agent@demo.noba`, `agent2@demo.noba` | `demo1234` |
| Super-admin | `super@noba.io` | `super1234` |

Code public de l'établissement de démo : `demo`, soit `/q/demo` et `/display/demo`.

## Modèle

`Organization` (client SaaS) → `Branch` (établissement, code public) → `QueueService` (une file, avec un
préfixe de tickets) et `Counter` (guichet, qui traite un ou plusieurs services). `StaffUser` a le rôle
SUPER_ADMIN, ORG_ADMIN ou AGENT. `Ticket` porte un jeton secret pour le suivi sans compte.

Quelques règles clés :

- **Numérotation** : repart à 1 chaque jour et par service (`DailySequence`, verrou pessimiste, sans doublon).
- **Appel du suivant** : le plus ancien ticket en attente parmi les services du guichet (verrou
  pessimiste, donc deux guichets ne peuvent pas appeler le même ticket). Le ticket en cours est clôturé
  automatiquement.
- **Estimation** : (personnes devant + 0,5) × durée moyenne ÷ guichets ouverts sur ce service. La durée
  moyenne est glissante sur les 20 derniers tickets du jour. À défaut, c'est la valeur configurée.
- **Transfert** : le ticket garde son heure de prise, donc sa place.
- **Multi-tenant** : chaque accès du personnel est vérifié par `CurrentUser.checkBranch`.
- **Fin de journée** : les tickets restés actifs passent en EXPIRED à 00:01 (fuseau `APP_TIMEZONE`).

## Notifications (gratuites)

Elles reposent sur l'API Notification du navigateur (via le service worker, nécessaire sur Android), la
vibration, un carillon généré en WebAudio et le titre de l'onglet. Elles fonctionnent **tant que la page
du ticket reste ouverte**, même en arrière-plan. Le navigateur exige HTTPS pour les notifications, sauf
sur `localhost`.

## Feuille de route

- **v2** : Web Push (VAPID) pour être prévenu page fermée, limite anti-abus sur la prise de tickets
  (par IP ou par appareil), migrations Flyway, tests automatisés, relais Redis pour le temps réel à
  plusieurs instances.
- **v3** : réservation à distance et prise de ticket géolocalisée, horaires d'ouverture automatiques,
  multilingue FR / AR / EN, abonnements et facturation SaaS, annonce vocale sur l'écran TV.
