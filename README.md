# Bowly — Android app

**Bowly** is a self-hosted calorie tracker with **virtual batch meals** (*patelnie*): cook once, log portions over several days, and share data within a household.

The app **does not work on its own** — it requires a running [Bowly backend](https://github.com/DunderTheDragon/bowlyAppBackend) (Docker + PostgreSQL). Follow the backend repository for setup instructions.

---

## Download (Android)

**[Download latest APK](https://github.com/DunderTheDragon/bowlyApp/releases/latest/download/composeApp-debug.apk)**

All releases: [github.com/DunderTheDragon/bowlyApp/releases](https://github.com/DunderTheDragon/bowlyApp/releases)

> On first install, Android may ask you to allow installation from unknown sources — this is normal for APKs outside the Play Store.

---

## Requirements

- An **Android** phone or tablet
- A computer or server running the Bowly backend on the same LAN, or reachable over the internet if you expose it publicly
- The **instance registration secret** (`REGISTRATION_SECRET` in the backend `.env`) — required when creating the first account (and any new accounts)

---

## First-time setup

### 1. Start the backend

Follow the instructions in the backend repository:

**[bowlyAppBackend — README](https://github.com/DunderTheDragon/bowlyAppBackend)**

In short: copy `.env`, set `JWT_SECRET` and `REGISTRATION_SECRET`, run `docker compose up -d --build`. The default API port is **8742**.

Verify the backend responds (browser or terminal):

```text
http://localhost:8742/api/system/status
```

### 2. Install the app

Download and install the APK from the link above.

### 3. Connect the app to your backend

When the app starts, enter the **server address** — full URL including port, for example:

| Scenario | Example address |
|----------|-----------------|
| Backend on the same machine as the Android emulator | `http://10.0.2.2:8742` |
| Backend on your home Wi‑Fi, phone on the same network | `http://<backend-host-LAN-IP>:8742` |
| Backend on the same device (uncommon with a physical phone) | `http://localhost:8742` |

Find the backend host IP with `ip addr` (Linux) or `ipconfig` (Windows). The port is `SERVER_PORT` from the backend `.env` (default **8742**).

Tap **Connect**. If the address is wrong or the backend is down, the app shows an error — fix the URL or restart the backend.

### 4. Register or log in

- **Register** — choose a username and password, plus the **backend password** (same value as `REGISTRATION_SECRET` in `.env`). This prevents random users and bots from creating accounts on a publicly reachable instance.
- **Log in** — username and password for an account that already exists.

Add household members the same way — each person enters the same backend password when registering. All users on one instance share products, recipes, and batch meals.

You can change the server address later via **Change server address** on the login screen.

---

## Features

### Calorie diary (home screen)

- Meals grouped by breakfast, lunch, dinner, and snacks
- Daily calorie and macro totals
- Progress vs. targets from your profile (BMR/TDEE)
- Add products from search or your local product list
- Log workout activities that increase your daily calorie budget

### Virtual batch meals (*patelnie*)

- Create multi-day “pots” — cook once, consume portions over time
- Multiple segments with different ingredients in one pot
- Shared across all users on the same backend instance
- Browse archived batch meals

### Products and recipes

- Product search (local cache + Open Food Facts, including Polish products)
- Barcode scanning
- Custom products and recipes with ingredient sections
- Create batch meals directly from a recipe

### Profile

- Weight, height, age, target weight, activity level
- Macro split (protein / fat / carbs)
- Light, dark, or system theme

---

## Troubleshooting

**App cannot reach the server**

- The backend must be running (`docker compose ps` on the host).
- Phone and backend host must be on the same network when using a LAN address.
- Check the port (default **8742**) and firewall rules on the backend host.
- Use a full URL with `http://` and port, e.g. `http://192.168.0.50:8742`.

**Registration fails — invalid backend password**

- In the backend password field, enter exactly the `REGISTRATION_SECRET` value from the backend `.env`.

**Switching to a different backend**

- Log out or tap **Change server address** and enter the new instance URL.

---

## Repositories

| Component | Repository |
|-----------|------------|
| API, database, Docker | [bowlyAppBackend](https://github.com/DunderTheDragon/bowlyAppBackend) |
| Android app (APK) | [bowlyApp](https://github.com/DunderTheDragon/bowlyApp) (this repo) |

---

## License

[MIT](https://github.com/DunderTheDragon/bowlyAppBackend/blob/main/LICENSE) — see the backend repository for details.
