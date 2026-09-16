let categories = [];
let pinned = true;

const el = (id) => document.getElementById(id);

// ---- titlebar ----
el('minBtn').onclick = () => window.api.minimize();
el('closeBtn').onclick = () => window.api.close();
el('pinBtn').onclick = () => {
  pinned = !pinned;
  window.api.togglePin(pinned);
  el('pinBtn').style.opacity = pinned ? '1' : '0.4';
};
el('settingsBtn').onclick = () => showSetup(true);

function showSetup(fromWidget) {
  el('setupView').classList.remove('hidden');
  el('widgetView').classList.add('hidden');
  el('backToWidgetBtn').classList.toggle('hidden', !fromWidget);
}

function showWidget() {
  el('setupView').classList.add('hidden');
  el('widgetView').classList.remove('hidden');
}

el('backToWidgetBtn').onclick = () => showWidget();

// ---- category editor ----
function renderCategoryList() {
  const list = el('categoryList');
  list.innerHTML = '';
  categories.forEach((cat, idx) => {
    const chip = document.createElement('span');
    chip.className = 'category-chip';
    chip.innerHTML = `${cat} <button data-idx="${idx}">✕</button>`;
    chip.querySelector('button').onclick = () => {
      categories.splice(idx, 1);
      renderCategoryList();
    };
    list.appendChild(chip);
  });
}

el('addCategoryBtn').onclick = () => {
  const input = el('newCategory');
  const val = input.value.trim();
  if (!val) return;
  if (categories.length >= 5) {
    showError('setupError', 'Max 5 categories.');
    return;
  }
  categories.push(val);
  input.value = '';
  renderCategoryList();
};

function showError(id, msg) {
  const e = el(id);
  e.textContent = msg;
  e.classList.remove('hidden');
  setTimeout(() => e.classList.add('hidden'), 4000);
}

// ---- setup save ----
el('saveSetupBtn').onclick = async () => {
  const supabaseUrl = el('supabaseUrl').value.trim();
  const supabaseKey = el('supabaseKey').value.trim();
  const anthropicKey = el('anthropicKey').value.trim();
  const dailyBudget = parseFloat(el('dailyBudget').value) || 0;
  const weeklyBudget = parseFloat(el('weeklyBudget').value) || 0;

  if (!supabaseUrl || !supabaseKey) {
    showError('setupError', 'Supabase URL and anon key are required.');
    return;
  }
  if (categories.length < 3) {
    showError('setupError', 'Add at least 3 categories.');
    return;
  }

  try {
    await window.api.saveConfig({
      supabaseUrl,
      supabaseAnonKey: supabaseKey,
      anthropicApiKey: anthropicKey,
      categories,
      dailyBudget,
      weeklyBudget,
      markOnboarded: true,
    });
    populateCategorySelect();
    showWidget();
    await refreshAll();
  } catch (err) {
    showError('setupError', err.message);
  }
};

function populateCategorySelect() {
  const sel = el('category');
  sel.innerHTML = '';
  categories.forEach((c) => {
    const opt = document.createElement('option');
    opt.value = c;
    opt.textContent = c;
    sel.appendChild(opt);
  });
}

// ---- entry form ----
el('entryForm').onsubmit = async (e) => {
  e.preventDefault();
  const amount = parseFloat(el('amount').value);
  const category = el('category').value;
  const note = el('note').value.trim();
  const paymentMethod = document.querySelector('input[name="payMethod"]:checked').value;
  const isImpulse = el('isImpulse').checked;

  if (!amount || amount <= 0) {
    showError('formError', 'Enter a valid amount.');
    return;
  }

  try {
    await window.api.addEntry({ amount, category, note, paymentMethod, isImpulse });
    el('amount').value = '';
    el('note').value = '';
    el('isImpulse').checked = false;
    await refreshAll();
  } catch (err) {
    showError('formError', err.message);
  }
};

// ---- advice ----
el('adviceBtn').onclick = async () => {
  const btn = el('adviceBtn');
  const box = el('adviceText');
  btn.disabled = true;
  btn.textContent = 'Thinking...';
  box.classList.remove('hidden');
  box.textContent = '';
  try {
    const entries = await window.api.listEntries({});
    const config = await window.api.getConfig();
    const text = await window.api.getAdvice({
      entries,
      categories: config.categories,
      dailyBudget: config.dailyBudget,
      weeklyBudget: config.weeklyBudget,
    });
    box.textContent = text;
  } catch (err) {
    box.textContent = `⚠ ${err.message}`;
  } finally {
    btn.disabled = false;
    btn.textContent = 'Get AI advice';
  }
};

// ---- dashboard rendering ----
function startOfDay() {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
}
function startOfWeek() {
  const d = startOfDay();
  const day = d.getDay(); // 0 = Sunday
  d.setDate(d.getDate() - day);
  return d;
}

function setBar(fillEl, spent, budget) {
  const pct = budget > 0 ? Math.min(100, (spent / budget) * 100) : spent > 0 ? 100 : 0;
  fillEl.style.width = `${pct}%`;
  let color = 'var(--accent)';
  if (pct >= 100) color = 'var(--red)';
  else if (pct >= 70) color = 'var(--amber)';
  fillEl.style.background = color;
}

async function refreshAll() {
  const config = await window.api.getConfig();
  const entries = await window.api.listEntries({});

  const todayStart = startOfDay();
  const weekStart = startOfWeek();

  const todaySpend = entries
    .filter((e) => new Date(e.created_at) >= todayStart)
    .reduce((s, e) => s + Number(e.amount), 0);
  const weekSpend = entries
    .filter((e) => new Date(e.created_at) >= weekStart)
    .reduce((s, e) => s + Number(e.amount), 0);

  el('dailySpendLabel').textContent = `₹${todaySpend.toFixed(0)} / ₹${config.dailyBudget}`;
  el('weeklySpendLabel').textContent = `₹${weekSpend.toFixed(0)} / ₹${config.weeklyBudget}`;
  setBar(el('dailyBar'), todaySpend, config.dailyBudget);
  setBar(el('weeklyBar'), weekSpend, config.weeklyBudget);

  const list = el('entryList');
  list.innerHTML = '';
  entries.slice(0, 15).forEach((entry) => {
    const item = document.createElement('div');
    item.className = 'entry-item' + (entry.is_impulse ? ' impulse' : '');
    const time = new Date(entry.created_at).toLocaleString([], {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
    item.innerHTML = `
      <div>
        <div class="amount">₹${Number(entry.amount).toFixed(0)} · ${entry.category}</div>
        <div class="meta">${entry.payment_method.toUpperCase()} · ${time}${entry.note ? ' · ' + escapeHtml(entry.note) : ''}</div>
      </div>`;
    list.appendChild(item);
  });
}

function escapeHtml(s) {
  const d = document.createElement('div');
  d.textContent = s;
  return d.innerHTML;
}

// ---- boot ----
(async function init() {
  const config = await window.api.getConfig();
  categories = config.categories && config.categories.length ? config.categories : [];

  el('supabaseUrl').value = config.supabaseUrl || '';
  el('supabaseKey').value = config.supabaseAnonKey || '';
  el('anthropicKey').value = config.anthropicApiKey || '';
  el('dailyBudget').value = config.dailyBudget || '';
  el('weeklyBudget').value = config.weeklyBudget || '';
  renderCategoryList();

  if (config.onboarded && config.supabaseUrl && config.supabaseAnonKey && categories.length) {
    populateCategorySelect();
    showWidget();
    try {
      await refreshAll();
    } catch (err) {
      showError('formError', err.message);
    }
  } else {
    showSetup(false);
  }
})();
