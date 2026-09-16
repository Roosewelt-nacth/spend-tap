# Spend Tracker Widget

An always-on-top desktop overlay for logging UPI/cash spending, tagging a reason, tracking it against daily/weekly budgets, and getting AI feedback on spending patterns.

## 1. Create your Supabase project

1. Go to https://supabase.com, sign up / log in, and create a new project (free tier).
2. Once it's ready, open **SQL Editor** → **New query**, paste the contents of [`schema.sql`](schema.sql), and run it.
3. Go to **Project Settings → API** and copy:
   - **Project URL**
   - **anon public** key

## 2. (Optional) Get an Anthropic API key

For the "Get AI advice" button: create a key at https://console.anthropic.com → API Keys.
Without this, everything else (logging spends, budget bars) still works — the advice button will just show an error until a key is added.

## 3. Install and run

```bash
cd spend-tracker
npm install
npm start
```

On first launch you'll see the setup screen — paste in your Supabase URL/key, Anthropic key, your 3-5 spend categories, and daily/weekly budget limits. After saving, the widget appears docked in the top-right of your screen, always on top.

## Notes on the current setup

- **No login/auth yet.** The app generates a random device ID stored locally and tags all your Supabase rows with it. This is fine for your own single-device testing, but means anyone with your anon key + device ID could read the data. Before sharing this beyond yourself, swap in Supabase Auth (email/magic link) and Row Level Security policies keyed on `auth.uid()` instead of `device_id` — `schema.sql` has a comment marking exactly where.
- **Secrets are stored locally** (via `electron-store`, in your OS user-data folder) and used only from the Electron main process — never exposed to the page's devtools.
- **AI advice** sends up to your last 40 entries (amount, category, note, payment method, timestamps) to the Anthropic API to generate feedback. Nothing is sent anywhere else.
- Pin (📌) toggles always-on-top; the app otherwise behaves like a small draggable, resizable window docked over your desktop.
