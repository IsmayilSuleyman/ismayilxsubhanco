// Shared Budget Tracker — fetches a public Google Sheet and renders stats.

const SHEET_ID = "15ULjX9IpOkNqVcIi35wEH8bamnPvRHh_rDYzplDn9gI";
const GID = "0";

// If you have used File → Share → Publish to web, paste the published CSV URL
// here (looks like .../spreadsheets/d/e/2PACX-.../pub?gid=0&single=true&output=csv).
// Published URLs always allow cross-origin fetch — most reliable.
const PUBLISHED_CSV_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vSVqbzTJJHPgIXlNKLg50CVSemInyOHA-yzsO2pceUVoKtmFqXSa__3j8xiGKqjo3Zb_NFePONk5a9b/pub?output=csv";

// Same-origin CSV. On GitHub Pages this file is refreshed by a GitHub Actions
// workflow (.github/workflows/refresh-sheet.yml). On Vercel it's served via
// vercel.json rewrite. Either way: same origin, no CORS.
// Relative path so it works whether the site is at user.github.io/repo/ or root.
const PROXY_URL = "./sheet.csv";

// Public CORS proxies for local testing only. Deploying to Vercel removes the
// need for any of these.
const CORS_PROXIES = [
  (u) => `https://api.allorigins.win/raw?url=${encodeURIComponent(u)}`,
  (u) => `https://api.codetabs.com/v1/proxy/?quest=${encodeURIComponent(u)}`,
  (u) => `https://corsproxy.io/?${encodeURIComponent(u)}`,
];

const SHEET_URLS = [
  PROXY_URL,
  PUBLISHED_CSV_URL,
  ...(PUBLISHED_CSV_URL ? CORS_PROXIES.map((p) => p(PUBLISHED_CSV_URL)) : []),
];

const PEOPLE = ["Sübhan", "İsmayıl"];

// Canonical category list (matches the sheet's expense categories block).
const CATEGORIES = [
  { key: "Groceries",      emoji: "🥕" },
  { key: "Dining out",     emoji: "🍝" },
  { key: "Utilities",      emoji: "⚡" },
  { key: "Transportation", emoji: "🚖" },
  { key: "Household",      emoji: "🖼" },
  { key: "Subscriptions",  emoji: "🤳" },
  { key: "Entertainment",  emoji: "🎉" },
  { key: "Healthcare",     emoji: "🏥" },
  { key: "Other",          emoji: "😀" },
];

const PERSON_COLORS = { "Sübhan": "#a78bfa", "İsmayıl": "#60a5fa" };

// ---------- Fetching ----------

async function fetchSheet() {
  const errors = [];
  for (const baseUrl of SHEET_URLS) {
    const sep = baseUrl.includes("?") ? "&" : "?";
    const url = `${baseUrl}${sep}_=${Date.now()}`;
    try {
      const r = await fetch(url, { cache: "no-store", redirect: "follow" });
      if (!r.ok) { errors.push(`${r.status} @ ${baseUrl}`); continue; }
      const text = await r.text();
      // Detect HTML sign-in page (sheet not actually public).
      if (text.trimStart().startsWith("<")) {
        errors.push(`HTML response (sheet not public) @ ${baseUrl}`);
        continue;
      }
      return text;
    } catch (e) {
      errors.push(`${e.message || e} @ ${baseUrl}`);
    }
  }
  throw new Error(errors.join(" | "));
}

// ---------- CSV ----------

function parseCSV(text) {
  const rows = [];
  let row = [];
  let cell = "";
  let inQuotes = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (inQuotes) {
      if (c === '"') {
        if (text[i + 1] === '"') { cell += '"'; i++; }
        else inQuotes = false;
      } else cell += c;
    } else {
      if (c === '"') inQuotes = true;
      else if (c === ",") { row.push(cell); cell = ""; }
      else if (c === "\n") { row.push(cell); rows.push(row); row = []; cell = ""; }
      else if (c === "\r") { /* skip */ }
      else cell += c;
    }
  }
  if (cell.length || row.length) { row.push(cell); rows.push(row); }
  if (!rows.length) return [];
  const headers = rows[0].map((h) => h.trim());
  return rows.slice(1).map((r) => {
    const o = {};
    headers.forEach((h, i) => (o[h] = (r[i] ?? "").trim()));
    return o;
  });
}

// ---------- Parsing helpers ----------

// Handles "- ₼5.00", " ₼3.00", "₼0.50", "-₼0.5", etc.
function parseAmount(s) {
  if (!s) return 0;
  const cleaned = s.replace(/\s+/g, "").replace(/₼/g, "").replace(/,/g, "");
  if (!cleaned || cleaned === "-") return 0;
  const n = parseFloat(cleaned);
  return Number.isFinite(n) ? n : 0;
}

// Strip leading emoji + whitespace from "🍝 Dining out" -> "Dining out".
function categoryKey(raw) {
  if (!raw) return "";
  return raw.replace(/^[^\p{L}\p{N}]+/u, "").trim();
}

function isExpenseRow(r) {
  return PEOPLE.includes(r["Transaction by"]) || r["Transaction by"] === "Shared";
}

function isTopUp(r) {
  return categoryKey(r["Transaction category"]).toLowerCase() === "top up";
}

// ---------- Stats ----------

function computeStats(rows) {
  rows = rows.filter(isExpenseRow);

  const perPerson = Object.fromEntries(
    PEOPLE.map((p) => [p, { balance: 0, topup: 0, spent: 0 }])
  );
  const catTotals = Object.fromEntries(CATEGORIES.map((c) => [c.key, 0]));
  const catByPerson = Object.fromEntries(
    CATEGORIES.map((c) => [c.key, Object.fromEntries(PEOPLE.map((p) => [p, 0]))])
  );
  let totalSpent = 0;

  for (const r of rows) {
    const amt = parseAmount(r["Amount (AZN)"]);
    const who = r["Transaction by"];
    const cat = categoryKey(r["Transaction category"]);

    if (isTopUp(r) && PEOPLE.includes(who)) {
      perPerson[who].topup += amt;
      perPerson[who].balance += amt;
      continue;
    }

    // Expense (amount is negative)
    if (who === "Shared") {
      const half = amt / 2;
      perPerson["Sübhan"].balance += half;
      perPerson["İsmayıl"].balance += half;
      perPerson["Sübhan"].spent += -half;
      perPerson["İsmayıl"].spent += -half;
      if (cat in catByPerson) {
        catByPerson[cat]["Sübhan"] += -half;
        catByPerson[cat]["İsmayıl"] += -half;
      }
    } else if (PEOPLE.includes(who)) {
      perPerson[who].balance += amt;
      perPerson[who].spent += -amt;
      if (cat in catByPerson) {
        catByPerson[cat][who] += -amt;
      }
    }

    if (cat in catTotals) {
      catTotals[cat] += -amt; // store positive spent
      totalSpent += -amt;
    }
  }

  const total = perPerson["Sübhan"].balance + perPerson["İsmayıl"].balance;

  const categories = CATEGORIES.map((c) => ({
    ...c,
    amount: catTotals[c.key] || 0,
  })).sort((a, b) => b.amount - a.amount);

  // Build a sorted recent list (latest first).
  const recent = rows
    .map((r) => ({
      date: r["Date"],
      time: r["Time"],
      who: r["Transaction by"],
      cat: r["Transaction category"],
      catKey: categoryKey(r["Transaction category"]),
      amount: parseAmount(r["Amount (AZN)"]),
    }))
    .sort((a, b) => (b.date + " " + b.time).localeCompare(a.date + " " + a.time))
    .slice(0, 10);

  return { total, totalSpent, perPerson, categories, catByPerson, recent, txCount: rows.length };
}

// ---------- Formatting ----------

function formatAZN(n) {
  const abs = Math.abs(n).toFixed(2);
  return (n < 0 ? "-" : "") + "₼" + abs;
}

function signClass(n) {
  if (n > 0.0049) return "pos";
  if (n < -0.0049) return "neg";
  return null;
}

function emojiFor(catKey) {
  const c = CATEGORIES.find((c) => c.key.toLowerCase() === catKey.toLowerCase());
  return c ? c.emoji : "💼";
}

function formatDate(s) {
  if (!s) return "";
  const d = new Date(s);
  if (isNaN(d)) return s;
  return d.toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

function currentMonthLabel() {
  return new Date().toLocaleDateString(undefined, { month: "long", year: "numeric" });
}

// ---------- Rendering ----------

function setText(el, text) { if (el) el.textContent = text; }

// ---------- Donut chart ----------

const SVG_NS = "http://www.w3.org/2000/svg";

function polar(r, angle) {
  return [r * Math.cos(angle), r * Math.sin(angle)];
}

function donutSlicePath(startAngle, endAngle, rOuter, rInner) {
  const [x1o, y1o] = polar(rOuter, startAngle);
  const [x2o, y2o] = polar(rOuter, endAngle);
  const [x1i, y1i] = polar(rInner, endAngle);
  const [x2i, y2i] = polar(rInner, startAngle);
  const largeArc = endAngle - startAngle > Math.PI ? 1 : 0;
  return [
    `M ${x1o} ${y1o}`,
    `A ${rOuter} ${rOuter} 0 ${largeArc} 1 ${x2o} ${y2o}`,
    `L ${x1i} ${y1i}`,
    `A ${rInner} ${rInner} 0 ${largeArc} 0 ${x2i} ${y2i}`,
    "Z",
  ].join(" ");
}

let selectedCat = null;
let currentStats = null;

function renderDonut(stats) {
  const svg = document.getElementById("cat-chart");
  if (!svg) return;
  svg.innerHTML = "";

  const rOuter = 50;
  const rInner = 30;
  const nonZero = stats.categories.filter((c) => c.amount > 0);
  const total = nonZero.reduce((s, c) => s + c.amount, 0);

  if (total <= 0) {
    const ring = document.createElementNS(SVG_NS, "circle");
    ring.setAttribute("cx", "0");
    ring.setAttribute("cy", "0");
    ring.setAttribute("r", String((rOuter + rInner) / 2));
    ring.setAttribute("fill", "none");
    ring.setAttribute("stroke", "rgba(255,255,255,0.12)");
    ring.setAttribute("stroke-width", String(rOuter - rInner));
    svg.appendChild(ring);
    return;
  }

  // Start at -90deg (12 o'clock) so the first slice begins at the top.
  let angle = -Math.PI / 2;
  // If a single non-zero slice, render as a complete ring so it's visible.
  if (nonZero.length === 1) {
    const c = nonZero[0];
    const ring = document.createElementNS(SVG_NS, "circle");
    ring.setAttribute("cx", "0");
    ring.setAttribute("cy", "0");
    ring.setAttribute("r", String((rOuter + rInner) / 2));
    ring.setAttribute("fill", "none");
    ring.setAttribute("stroke", c.color);
    ring.setAttribute("stroke-width", String(rOuter - rInner));
    ring.dataset.cat = c.key;
    ring.classList.add("slice");
    if (selectedCat === c.key) ring.classList.add("is-active");
    svg.appendChild(ring);
    return;
  }

  for (const c of nonZero) {
    const sweep = (c.amount / total) * Math.PI * 2;
    const path = document.createElementNS(SVG_NS, "path");
    path.setAttribute("d", donutSlicePath(angle, angle + sweep, rOuter, rInner));
    path.setAttribute("fill", c.color);
    path.dataset.cat = c.key;
    path.classList.add("slice");
    if (selectedCat && selectedCat !== c.key) path.classList.add("is-dim");
    if (selectedCat === c.key) path.classList.add("is-active");
    svg.appendChild(path);
    angle += sweep;
  }
}

function renderCenter(stats) {
  const center = document.getElementById("cat-chart-center");
  if (!center) return;
  const labelEl = center.querySelector(".cc-label");
  const amtEl = center.querySelector(".cc-amount");
  if (selectedCat) {
    const cat = stats.categories.find((c) => c.key === selectedCat);
    if (cat) {
      labelEl.textContent = `${cat.emoji} ${cat.key}`;
      amtEl.textContent = formatAZN(cat.amount);
      return;
    }
  }
  labelEl.textContent = "Total spent";
  amtEl.textContent = formatAZN(stats.totalSpent);
}

function renderBreakdown(stats) {
  const panel = document.getElementById("cat-breakdown");
  if (!panel) return;
  if (!selectedCat) {
    panel.hidden = true;
    panel.innerHTML = "";
    return;
  }
  const cat = stats.categories.find((c) => c.key === selectedCat);
  const split = stats.catByPerson[selectedCat] || {};
  const total = cat ? cat.amount : 0;
  const rows = PEOPLE.map((p) => {
    const amt = split[p] || 0;
    const pct = total > 0 ? Math.round((amt / total) * 100) : 0;
    const cls = amt < 0.0049 ? " zero" : "";
    return `
      <li class="bd-row${cls}">
        <span class="bd-dot" style="background:${PERSON_COLORS[p] || "#94a3b8"}"></span>
        <span class="bd-name">${p}</span>
        <span class="bd-pct">${pct}%</span>
        <span class="bd-amt">${formatAZN(amt)}</span>
      </li>
    `;
  }).join("");
  panel.innerHTML = `
    <div class="bd-head">
      <span class="bd-title">${cat ? cat.emoji + " " + cat.key : selectedCat}</span>
      <span class="bd-total">${formatAZN(total)}</span>
    </div>
    <ul class="bd-list">${rows}</ul>
    <p class="bd-note">Shared expenses are split 50/50.</p>
  `;
  panel.hidden = false;
}

function updateCatSelectionUI() {
  document.querySelectorAll("#cat-chart .slice").forEach((el) => {
    const k = el.dataset.cat;
    el.classList.toggle("is-active", selectedCat === k);
    el.classList.toggle("is-dim", !!selectedCat && selectedCat !== k);
  });
  document.querySelectorAll("#categories .cat-row").forEach((el) => {
    const k = el.dataset.cat;
    if (selectedCat === k) {
      el.setAttribute("aria-pressed", "true");
    } else {
      el.setAttribute("aria-pressed", "false");
    }
  });
}

function toggleCategory(key, stats) {
  if (!key) return;
  const cat = stats.categories.find((c) => c.key === key);
  if (!cat || cat.amount <= 0) return;
  selectedCat = selectedCat === key ? null : key;
  updateCatSelectionUI();
  renderBreakdown(stats);
  renderCenter(stats);
  renderDonut(stats);
}

function render(stats) {
  // Header
  setText(document.getElementById("month-label"), currentMonthLabel());

  // Total
  const totalEl = document.getElementById("total-amount");
  totalEl.textContent = formatAZN(stats.total);
  totalEl.classList.remove("pos", "neg");
  const totalSign = signClass(stats.total);
  if (totalSign) totalEl.classList.add(totalSign);
  setText(
    document.getElementById("total-meta"),
    `${stats.txCount} transactions · ${formatAZN(stats.totalSpent)} spent`
  );

  // Per-person
  document.querySelectorAll(".person").forEach((card) => {
    const p = card.dataset.person;
    const data = stats.perPerson[p];
    if (!data) return;
    const balEl = card.querySelector('[data-field="balance"]');
    balEl.textContent = formatAZN(data.balance);
    balEl.classList.remove("pos", "neg");
    const balSign = signClass(data.balance);
    if (balSign) balEl.classList.add(balSign);
    card.querySelector('[data-field="topup"]').textContent = formatAZN(data.topup);
    card.querySelector('[data-field="spent"]').textContent = formatAZN(-data.spent);
  });

  // Categories
  const catList = document.getElementById("categories");
  const max = Math.max(...stats.categories.map((c) => c.amount), 0);
  catList.innerHTML = "";
  for (const c of stats.categories) {
    const li = document.createElement("li");
    const isZero = c.amount === 0;
    li.className = "cat-row" + (isZero ? " zero" : "");
    li.dataset.cat = c.key;
    if (!isZero) {
      li.setAttribute("role", "button");
      li.setAttribute("tabindex", "0");
      li.setAttribute("aria-pressed", selectedCat === c.key ? "true" : "false");
    }
    const pct = max > 0 ? Math.round((c.amount / max) * 100) : 0;
    li.innerHTML = `
      <div class="cat-name"><span class="cat-emoji">${c.emoji}</span>${c.key}</div>
      <div class="cat-amount">${formatAZN(c.amount)}</div>
      <div class="cat-bar"><span style="width:${pct}%; background:${c.color}"></span></div>
    `;
    catList.appendChild(li);
  }
  setText(document.getElementById("cat-total"), formatAZN(stats.totalSpent) + " total");

  // If the previously selected category no longer has spend, clear selection.
  if (selectedCat) {
    const stillThere = stats.categories.find((c) => c.key === selectedCat && c.amount > 0);
    if (!stillThere) selectedCat = null;
  }

  renderDonut(stats);
  renderCenter(stats);
  renderBreakdown(stats);
  updateCatSelectionUI();

  // Recent transactions
  const txList = document.getElementById("tx-list");
  txList.innerHTML = "";
  for (const t of stats.recent) {
    const li = document.createElement("li");
    li.className = "tx-row";
    const chipClass = t.who === "Shared" ? "chip shared" : "chip";
    const amtClass = signClass(t.amount) || "";
    li.innerHTML = `
      <div class="tx-emoji">${emojiFor(t.catKey)}</div>
      <div class="tx-main">
        <div class="tx-cat">${t.catKey || "—"}</div>
        <div class="tx-meta">
          <span>${formatDate(t.date)}</span>
          <span class="${chipClass}">${t.who}</span>
        </div>
      </div>
      <div class="tx-amt${amtClass ? " " + amtClass : ""}">${formatAZN(t.amount)}</div>
    `;
    txList.appendChild(li);
  }
  setText(document.getElementById("tx-meta"), `${stats.recent.length} shown`);
}

// ---------- Boot ----------

async function load() {
  const btn = document.getElementById("refresh");
  const status = document.getElementById("status");
  btn.classList.add("loading");
  status.textContent = "Loading…";
  try {
    const csv = await fetchSheet();
    const rows = parseCSV(csv);
    const stats = computeStats(rows);
    currentStats = stats;
    render(stats);
    status.textContent = "Updated " + new Date().toLocaleTimeString();
  } catch (e) {
    console.error("Sheet load failed:", e);
    const proto = window.location.protocol;
    const origin = window.location.origin || "(no origin)";
    status.textContent = `Failed [${proto} ${origin}]: ${e.message || e}`;
  } finally {
    btn.classList.remove("loading");
  }
}

document.getElementById("refresh").addEventListener("click", load);

// Delegated tap-to-reveal: works for both donut slices and bar rows.
document.addEventListener("click", (e) => {
  const target = e.target.closest("[data-cat]");
  if (!target) return;
  if (!currentStats) return;
  toggleCategory(target.dataset.cat, currentStats);
});

document.addEventListener("keydown", (e) => {
  if (e.key !== "Enter" && e.key !== " ") return;
  const target = e.target.closest && e.target.closest("#categories .cat-row[data-cat]");
  if (!target || !currentStats) return;
  e.preventDefault();
  toggleCategory(target.dataset.cat, currentStats);
});

load();
