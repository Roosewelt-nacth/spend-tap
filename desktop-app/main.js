const { app, BrowserWindow, ipcMain, screen } = require('electron');
const path = require('path');
const crypto = require('crypto');
const Store = require('electron-store');
const { createClient } = require('@supabase/supabase-js');

const store = new Store();

if (!store.get('deviceId')) {
  store.set('deviceId', crypto.randomUUID());
}

let mainWindow;
let supabase = null;

function getSupabase() {
  const url = store.get('supabaseUrl');
  const key = store.get('supabaseAnonKey');
  if (!url || !key) return null;
  if (!supabase) {
    supabase = createClient(url, key);
  }
  return supabase;
}

function createWindow() {
  const { width: screenW } = screen.getPrimaryDisplay().workAreaSize;

  mainWindow = new BrowserWindow({
    width: 320,
    height: 420,
    x: screenW - 340,
    y: 40,
    minWidth: 280,
    minHeight: 320,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    resizable: true,
    skipTaskbar: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  mainWindow.setAlwaysOnTop(true, 'floating');
  mainWindow.setVisibleOnAllWorkspaces(true, { visibleOnFullScreen: true });
  mainWindow.loadFile(path.join(__dirname, 'src', 'index.html'));
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) createWindow();
});

// ---- IPC: window controls ----
ipcMain.on('window-minimize', () => mainWindow.minimize());
ipcMain.on('window-close', () => mainWindow.close());
ipcMain.on('window-toggle-pin', (_e, pinned) => {
  mainWindow.setAlwaysOnTop(pinned, 'floating');
});

// ---- IPC: config ----
ipcMain.handle('config:get', () => ({
  deviceId: store.get('deviceId'),
  supabaseUrl: store.get('supabaseUrl') || '',
  supabaseAnonKey: store.get('supabaseAnonKey') || '',
  anthropicApiKey: store.get('anthropicApiKey') || '',
  onboarded: !!store.get('onboarded'),
  categories: store.get('categories') || [],
  dailyBudget: store.get('dailyBudget') || 0,
  weeklyBudget: store.get('weeklyBudget') || 0,
}));

ipcMain.handle('config:save', async (_e, cfg) => {
  if (cfg.supabaseUrl !== undefined) store.set('supabaseUrl', cfg.supabaseUrl);
  if (cfg.supabaseAnonKey !== undefined) store.set('supabaseAnonKey', cfg.supabaseAnonKey);
  if (cfg.anthropicApiKey !== undefined) store.set('anthropicApiKey', cfg.anthropicApiKey);
  if (cfg.categories !== undefined) store.set('categories', cfg.categories);
  if (cfg.dailyBudget !== undefined) store.set('dailyBudget', cfg.dailyBudget);
  if (cfg.weeklyBudget !== undefined) store.set('weeklyBudget', cfg.weeklyBudget);
  supabase = null; // force re-init with new creds

  const sb = getSupabase();
  if (sb) {
    const deviceId = store.get('deviceId');
    await sb.from('settings').upsert({
      device_id: deviceId,
      categories: store.get('categories') || [],
      daily_budget: store.get('dailyBudget') || 0,
      weekly_budget: store.get('weeklyBudget') || 0,
      updated_at: new Date().toISOString(),
    });
  }

  if (cfg.markOnboarded) store.set('onboarded', true);
  return { ok: true };
});

// ---- IPC: entries ----
ipcMain.handle('entries:add', async (_e, entry) => {
  const sb = getSupabase();
  const deviceId = store.get('deviceId');
  const row = {
    device_id: deviceId,
    amount: entry.amount,
    category: entry.category,
    note: entry.note || '',
    payment_method: entry.paymentMethod,
    is_impulse: !!entry.isImpulse,
  };
  if (!sb) {
    throw new Error('Supabase is not configured yet. Open Settings and add your Project URL and anon key.');
  }
  const { data, error } = await sb.from('entries').insert(row).select().single();
  if (error) throw new Error(error.message);
  return data;
});

ipcMain.handle('entries:list', async (_e, { since } = {}) => {
  const sb = getSupabase();
  if (!sb) return [];
  const deviceId = store.get('deviceId');
  let query = sb
    .from('entries')
    .select('*')
    .eq('device_id', deviceId)
    .order('created_at', { ascending: false });
  if (since) query = query.gte('created_at', since);
  const { data, error } = await query;
  if (error) throw new Error(error.message);
  return data;
});

// ---- IPC: AI advice ----
ipcMain.handle('advice:get', async (_e, { entries, categories, dailyBudget, weeklyBudget }) => {
  const apiKey = store.get('anthropicApiKey');
  if (!apiKey) {
    throw new Error('No Anthropic API key set. Add one in Settings to enable AI advice.');
  }

  const summaryLines = entries
    .slice(0, 40)
    .map(
      (e) =>
        `- ₹${e.amount} | ${e.category} | ${e.payment_method} | impulse:${e.is_impulse} | "${e.note || ''}" | ${e.created_at}`
    )
    .join('\n');

  const prompt = `You are a calm, non-judgmental personal finance coach. Here are a user's recent spend entries (currency INR), their categories, and budgets.

Categories: ${JSON.stringify(categories)}
Daily budget: ${dailyBudget}
Weekly budget: ${weeklyBudget}

Recent entries (most recent first):
${summaryLines || '(no entries yet)'}

In under 150 words: point out concrete patterns (which category dominates, any impulsive spend clusters, notable notes), and give 2-3 specific, practical suggestions to spend more intentionally. Be direct but encouraging, not preachy. No generic advice — ground it in their actual entries.`;

  const res = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-api-key': apiKey,
      'anthropic-version': '2023-06-01',
    },
    body: JSON.stringify({
      model: 'claude-sonnet-5',
      max_tokens: 400,
      messages: [{ role: 'user', content: prompt }],
    }),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Anthropic API error (${res.status}): ${text}`);
  }
  const json = await res.json();
  const text = json.content?.map((c) => c.text).join('') || '';
  return text.trim();
});
