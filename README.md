# Spend Tracker

Tracks UPI/cash spending against self-set category budgets, with a note on why each spend happened, and gives feedback/AI advice on spending habits. Three surfaces, one shared Supabase backend:

- **[desktop-app/](desktop-app)** — Electron always-on-top overlay widget (Windows/macOS/Linux)
- **[mobile-app/](mobile-app)** — Expo/React Native app for iOS + Android, each with a native home-screen widget
- **[supabase/](supabase)** — shared backend: Postgres schema (`desktop-app/schema.sql`) and the `get-advice` Edge Function that proxies Claude API calls so the API key never ships inside a client app

## Status

- Desktop app: built, dependencies verified, launches successfully.
- Mobile app: full source written and dependency-checked; **not yet built/run** — needs Android Studio (Windows-buildable) and a Mac + Xcode (iOS-only) to actually compile and test. See [mobile-app/README-mobile.md](mobile-app/README-mobile.md) for exact steps.
- No real user authentication yet — everything is scoped by a locally generated device ID, fine for solo testing across your own devices only.

## Setup

1. Create a Supabase project, run `desktop-app/schema.sql` in its SQL editor
2. Deploy `supabase/functions/get-advice` and set the `ANTHROPIC_API_KEY` secret (see [mobile-app/README-mobile.md](mobile-app/README-mobile.md) section 1)
3. Desktop: `cd desktop-app && npm install && npm start`
4. Mobile: `cd mobile-app && npm install && npx expo start`

## Pushing to GitHub

```bash
git init
git add .
git commit -m "Initial commit: spend tracker (desktop + mobile scaffolding)"
git branch -M main
git remote add origin <your-repo-url>
git push -u origin main
```
