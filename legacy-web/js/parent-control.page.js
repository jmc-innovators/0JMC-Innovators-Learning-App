import { db, doc, getDoc, collection, query, where, orderBy, limit, getDocs, Timestamp } from "./firebase-init.js";
import { requireRole } from "./auth-guard.js";
import { renderShell } from "./nav-shell.js";
import { applyTheme, getLocalThemePref, syncThemeFromCloud } from "./theme.js";
import { toast, escapeHtml, formatMinutes, formatRelativeTime, initials, qs, qsa } from "./ui-utils.js";
import {
  generateLinkCode, claimLinkCode, subscribeMyChildren, approveChild, rejectChild,
  unlinkChild, updateControls, subscribeControls,
} from "./parent-link.js";

applyTheme(getLocalThemePref());

const SUBJECTS = ["Mathematics", "Science", "English", "ICT", "Sinhala", "Tamil", "History", "Commerce"];

let ctxUser, ctxProfile;
let children = [];          // all parentLinks rows for this parent
let childProfiles = {};     // childUid -> users/{uid} data (fetched lazily)
let selectedChildUid = null;
let unsubControls = null;

async function init() {
  const ctx = await requireRole(["parent"]);
  if (!ctx) return;
  ctxUser = ctx.user; ctxProfile = ctx.profile;
  await syncThemeFromCloud(ctxUser.uid);

  const { content } = renderShell({ user: ctxUser, profile: ctxProfile, active: "parent-control", pageTitle: "Parent Control" });
  content.innerHTML = `<div id="pcRoot"></div>`;
  const root = document.getElementById("pcRoot");
  root.innerHTML = `<div class="skel" style="height:70px; margin-bottom:16px;"></div><div class="skel" style="height:220px;"></div>`;

  subscribeMyChildren(ctxUser.uid, async (rows) => {
    children = rows;
    await Promise.all(rows.map(async r => {
      if (!childProfiles[r.childUid]) {
        try { const s = await getDoc(doc(db, "users", r.childUid)); if (s.exists()) childProfiles[r.childUid] = s.data(); }
        catch { /* not readable yet or offline */ }
      }
    }));
    if (!selectedChildUid || !rows.find(r => r.childUid === selectedChildUid)) {
      const activeFirst = rows.find(r => r.status === "active");
      selectedChildUid = activeFirst ? activeFirst.childUid : (rows[0]?.childUid || null);
    }
    renderRoot();
  });
}

function renderRoot() {
  const root = document.getElementById("pcRoot");
  const pending = children.filter(c => c.status === "pending");

  if (!children.length) {
    root.innerHTML = emptyLinkState();
    wireLinkButtons();
    return;
  }

  root.innerHTML = `
    ${pending.length ? pendingBanner(pending) : ""}
    <div class="kid-strip" id="kidStrip">
      ${children.map(kidChip).join("")}
      <div class="add-kid-chip" id="addKidChip">+ Link another child</div>
    </div>
    <div id="pcDetail"></div>
  `;

  qsa("[data-select-kid]").forEach(el => el.addEventListener("click", () => {
    selectedChildUid = el.dataset.selectKid; renderRoot();
  }));
  document.getElementById("addKidChip").addEventListener("click", openGenerateCodeModal);
  qsa("[data-approve]").forEach(b => b.addEventListener("click", async () => { await approveChild(b.dataset.approve); toast("Child approved — controls are now active.", "success"); }));
  qsa("[data-reject]").forEach(b => b.addEventListener("click", async () => { if (confirm("Reject this link request?")) { await rejectChild(b.dataset.reject); toast("Request rejected."); } }));

  const selected = children.find(c => c.childUid === selectedChildUid);
  const detail = document.getElementById("pcDetail");
  if (!selected) { detail.innerHTML = `<div class="empty-state"><p>Select a child above.</p></div>`; return; }
  if (selected.status !== "active") {
    detail.innerHTML = `<div class="g-card g-card-pad empty-state"><div class="empty-state-icon">⏳</div><p>This child hasn't been approved yet. Approve them above to see their dashboard and set controls.</p></div>`;
    return;
  }
  renderChildDetail(detail, selected);
}

function emptyLinkState() {
  return `
    <div class="g-card g-card-pad" style="text-align:center; padding:50px 24px;">
      <div style="font-size:44px; margin-bottom:12px;">👪</div>
      <h2 style="margin-bottom:8px;">Link your first child</h2>
      <p class="muted" style="max-width:420px; margin:0 auto 22px;">Generate a code, give it to your child, and they'll enter it on their own account to connect it here. Nothing is monitored until you approve the link.</p>
      <button class="btn btn-primary" id="generateCodeBtn">Generate a link code</button>
    </div>`;
}
function wireLinkButtons() {
  document.getElementById("generateCodeBtn")?.addEventListener("click", openGenerateCodeModal);
}

function pendingBanner(pending) {
  return `<div class="g-card g-card-pad mb-16" style="border-color:rgba(242,167,27,.4); background:rgba(242,167,27,.06);">
    <div class="section-title">⏳ Waiting for your approval (${pending.length})</div>
    ${pending.map(p => {
      const prof = childProfiles[p.childUid];
      return `<div class="flex-between mt-8">
        <div style="display:flex; align-items:center; gap:10px;">
          <div class="shell-avatar">${initials(prof?.fullName || "?")}</div>
          <div>
            <div style="font-weight:700; font-size:13.2px;">${escapeHtml(prof?.fullName || "New account")}</div>
            <div class="muted small">${escapeHtml(prof?.school || "")} ${prof?.grade ? "· " + escapeHtml(prof.grade) : ""}</div>
          </div>
        </div>
        <div style="display:flex; gap:8px;">
          <button class="btn btn-primary btn-sm" data-approve="${p.childUid}">Approve</button>
          <button class="btn btn-ghost btn-sm" data-reject="${p.childUid}">Reject</button>
        </div>
      </div>`;
    }).join("")}
  </div>`;
}

function kidChip(c) {
  const prof = childProfiles[c.childUid];
  const dotColor = c.status === "active" ? "var(--green)" : c.status === "pending" ? "var(--gold)" : "var(--text-3)";
  return `<div class="kid-chip ${c.childUid === selectedChildUid ? "selected" : ""}" data-select-kid="${c.childUid}">
    <div class="shell-avatar">${initials(prof?.fullName || "?")}</div>
    <span class="kid-chip-name">${escapeHtml(prof?.fullName || "Pending…")}</span>
    <span class="kid-chip-status" style="background:${dotColor}"></span>
  </div>`;
}

/* ==================== Per-child detail ==================== */
function renderChildDetail(detail, link) {
  const prof = childProfiles[link.childUid] || {};
  detail.innerHTML = `
    <div class="g-card g-card-pad mb-16 flex-between" style="flex-wrap:wrap; gap:14px;">
      <div style="display:flex; align-items:center; gap:14px;">
        <div class="shell-avatar" style="width:52px; height:52px; font-size:17px;">${initials(prof.fullName || "?")}</div>
        <div>
          <div style="font-weight:800; font-size:16px;">${escapeHtml(prof.fullName || "Student")}</div>
          <div class="muted small">${escapeHtml(prof.school || "")} ${prof.grade ? "· " + escapeHtml(prof.grade) : ""}</div>
        </div>
      </div>
      <button class="btn btn-ghost btn-sm" id="unlinkBtn">Unlink account</button>
    </div>

    <div class="pc-tabs" id="pcTabs">
      ${["overview", "controls", "monitoring", "reports", "security"].map((t, i) => `<button class="pc-tab ${i === 0 ? "active" : ""}" data-tab="${t}">${t[0].toUpperCase() + t.slice(1)}</button>`).join("")}
    </div>
    <div class="pc-panel active" data-panel="overview"><div class="skel" style="height:160px;"></div></div>
    <div class="pc-panel" data-panel="controls"><div class="skel" style="height:160px;"></div></div>
    <div class="pc-panel" data-panel="monitoring"><div class="skel" style="height:160px;"></div></div>
    <div class="pc-panel" data-panel="reports"><div class="skel" style="height:160px;"></div></div>
    <div class="pc-panel" data-panel="security"><div class="skel" style="height:160px;"></div></div>
  `;

  qsa("#pcTabs .pc-tab").forEach(b => b.addEventListener("click", () => {
    qsa("#pcTabs .pc-tab").forEach(x => x.classList.toggle("active", x === b));
    qsa(".pc-panel").forEach(p => p.classList.toggle("active", p.dataset.panel === b.dataset.tab));
  }));
  document.getElementById("unlinkBtn").addEventListener("click", async () => {
    if (confirm(`Unlink ${prof.fullName || "this child"}? All parental controls on their account will stop applying.`)) {
      await unlinkChild(link.childUid);
      selectedChildUid = null;
      toast("Account unlinked.");
    }
  });

  if (unsubControls) unsubControls();
  unsubControls = subscribeControls(link.childUid, (controls) => {
    renderOverview(link.childUid, prof, controls);
    renderControls(link.childUid, controls);
    renderMonitoring(link.childUid);
    renderReports(link.childUid);
    renderSecurity(link.childUid, controls);
  });
}

/* ---- Overview tab ---- */
async function renderOverview(childUid, prof, controls) {
  const panel = document.querySelector('.pc-panel[data-panel="overview"]');
  const [streak, weekMinutes, timeline] = await Promise.all([
    computeStreak(childUid), computeWeekMinutes(childUid), fetchTimeline(childUid, 8),
  ]);
  panel.innerHTML = `
    <div class="card-grid grid-3 mb-16">
      <div class="g-card stat-card"><div class="stat-icon" style="background:rgba(249,115,22,.16)">🔥</div><div class="stat-num">${streak}</div><div class="stat-label">Day streak</div></div>
      <div class="g-card stat-card"><div class="stat-icon" style="background:rgba(59,130,246,.16)">⏱️</div><div class="stat-num">${formatMinutes(weekMinutes)}</div><div class="stat-label">This week</div></div>
      <div class="g-card stat-card"><div class="stat-icon" style="background:${controls.emergencyLock ? "rgba(239,68,68,.18)" : "rgba(34,197,94,.16)"}">${controls.emergencyLock ? "🔒" : "✅"}</div><div class="stat-num" style="font-size:14px;">${controls.emergencyLock ? "Locked" : "Active"}</div><div class="stat-label">Account status</div></div>
    </div>
    <div class="g-card g-card-pad">
      <div class="section-title">Recent activity</div>
      ${timeline.length ? timeline.map(timelineRow).join("") : `<div class="empty-state"><div class="empty-state-icon">🕒</div><p>No activity logged yet.</p></div>`}
    </div>
  `;
}
function timelineRow(r) {
  const colorMap = { login: "var(--green)", study: "var(--blue-2)", activity: "var(--purple)" };
  return `<div class="timeline-item"><span class="timeline-dot" style="background:${colorMap[r.kind]}"></span>
    <div><div class="activity-text">${escapeHtml(r.label)}</div><div class="activity-time">${formatRelativeTime(r.at)}</div></div>
  </div>`;
}

/* ---- Controls tab ---- */
function renderControls(childUid, controls) {
  const panel = document.querySelector('.pc-panel[data-panel="controls"]');
  panel.innerHTML = `
    <div class="g-card g-card-pad mb-16">
      <div class="control-group-title">Focus</div>
      ${toggleRow("studyModeEnabled", "Study Mode", "Restrict the dashboard to approved subjects only.", controls.studyModeEnabled)}

      <div class="control-group-title">Feature access</div>
      ${toggleRow("aiEnabled", "AI Tutor", "Allow access to the AI Tutor.", controls.aiEnabled)}
      ${toggleRow("downloadsEnabled", "Downloads", "Allow downloading notes, papers and books.", controls.downloadsEnabled)}
      ${toggleRow("chatEnabled", "Chat & Messaging", "Allow messaging features.", controls.chatEnabled)}
      ${toggleRow("videosEnabled", "Video Lessons", "Allow watching video lessons.", controls.videosEnabled)}

      <div class="control-group-title">Allowed subjects</div>
      <div id="subjectChips">
        <span class="subject-chip ${isAllSubjects(controls) ? "on" : ""}" data-subj="all">All subjects</span>
        ${SUBJECTS.map(s => `<span class="subject-chip ${!isAllSubjects(controls) && controls.allowedSubjects?.includes(s) ? "on" : ""}" data-subj="${s}">${s}</span>`).join("")}
      </div>

      <div class="control-group-title">Study limits</div>
      <div class="field"><label>Daily limit — <span id="dailyLimitLabel">${controls.dailyLimitMinutes ? formatMinutes(controls.dailyLimitMinutes) : "No limit"}</span></label>
        <div class="limit-row"><input type="range" id="dailyLimitRange" min="0" max="360" step="15" value="${controls.dailyLimitMinutes || 0}"></div>
      </div>
      <div class="field"><label>Weekly limit — <span id="weeklyLimitLabel">${controls.weeklyLimitMinutes ? formatMinutes(controls.weeklyLimitMinutes) : "No limit"}</span></label>
        <div class="limit-row"><input type="range" id="weeklyLimitRange" min="0" max="2100" step="30" value="${controls.weeklyLimitMinutes || 0}"></div>
      </div>

      <div class="control-group-title">Reports & alerts</div>
      ${toggleRow("monthlyReportEnabled", "Monthly report", "A monthly summary of activity.", controls.monthlyReportEnabled)}
      ${toggleRow("emailNotifications", "Email notifications", "Alerts sent to your email.", controls.emailNotifications)}
      ${toggleRow("pushNotifications", "Push notifications", "Alerts sent to this browser.", controls.pushNotifications)}
    </div>
  `;

  qsa('[data-toggle]', panel).forEach(el => el.addEventListener("change", () => {
    updateControls(childUid, { [el.dataset.toggle]: el.checked });
  }));

  qsa(".subject-chip", panel).forEach(chip => chip.addEventListener("click", async () => {
    if (chip.dataset.subj === "all") {
      await updateControls(childUid, { allowedSubjects: ["all"] });
    } else {
      const current = isAllSubjects(controls) ? [] : (controls.allowedSubjects || []);
      const next = current.includes(chip.dataset.subj) ? current.filter(s => s !== chip.dataset.subj) : [...current, chip.dataset.subj];
      await updateControls(childUid, { allowedSubjects: next.length ? next : ["all"] });
    }
  }));

  const dailyRange = document.getElementById("dailyLimitRange");
  dailyRange.addEventListener("input", () => { document.getElementById("dailyLimitLabel").textContent = dailyRange.value == 0 ? "No limit" : formatMinutes(dailyRange.value); });
  dailyRange.addEventListener("change", () => updateControls(childUid, { dailyLimitMinutes: Number(dailyRange.value) }));

  const weeklyRange = document.getElementById("weeklyLimitRange");
  weeklyRange.addEventListener("input", () => { document.getElementById("weeklyLimitLabel").textContent = weeklyRange.value == 0 ? "No limit" : formatMinutes(weeklyRange.value); });
  weeklyRange.addEventListener("change", () => updateControls(childUid, { weeklyLimitMinutes: Number(weeklyRange.value) }));
}
function isAllSubjects(controls) { return !controls.allowedSubjects || controls.allowedSubjects.includes("all"); }
function toggleRow(key, label, desc, checked) {
  return `<div class="setting-row"><div><div class="setting-row-label">${label}</div><div class="setting-row-desc">${desc}</div></div>
    <label class="switch"><input type="checkbox" data-toggle="${key}" ${checked !== false ? "checked" : ""}><span class="track"></span></label></div>`;
}

/* ---- Monitoring tab ---- */
async function renderMonitoring(childUid) {
  const panel = document.querySelector('.pc-panel[data-panel="monitoring"]');
  const [logins, study] = await Promise.all([fetchRecent(childUid, "loginHistory", 6), fetchRecent(childUid, "studyHistory", 6)]);
  panel.innerHTML = `
    <div class="card-grid grid-2" style="align-items:start;">
      <div class="g-card g-card-pad">
        <div class="section-title">Login history</div>
        ${logins.length ? logins.map(l => `<div class="timeline-item"><span class="timeline-dot" style="background:var(--green)"></span><div><div class="activity-text">${escapeHtml(l.device || "Device")}</div><div class="activity-time">${formatRelativeTime(l.at)}</div></div></div>`).join("") : emptyMini("No sign-ins logged yet.")}
      </div>
      <div class="g-card g-card-pad">
        <div class="section-title">Study sessions</div>
        ${study.length ? study.map(s => `<div class="timeline-item"><span class="timeline-dot" style="background:var(--blue-2)"></span><div><div class="activity-text">${formatMinutes(s.minutes)} · ${escapeHtml(s.subject || "General")}</div><div class="activity-time">${formatRelativeTime(s.at)}</div></div></div>`).join("") : emptyMini("No study sessions logged yet.")}
      </div>
    </div>
    <div class="card-grid grid-2 mt-16">
      <div class="g-card g-card-pad"><div class="section-title">Downloads &amp; Quiz results</div>${emptyMini("Not tracked in this version yet — coming in a future update.")}</div>
      <div class="g-card g-card-pad"><div class="section-title">Assignments &amp; Attendance</div>${emptyMini("Not tracked in this version yet — coming in a future update.")}</div>
    </div>
  `;
}
function emptyMini(text) { return `<div class="empty-state" style="padding:20px 10px;"><p class="small">${text}</p></div>`; }

/* ---- Reports tab ---- */
async function renderReports(childUid) {
  const panel = document.querySelector('.pc-panel[data-panel="reports"]');
  const monthMinutes = await computeMonthMinutes(childUid);
  const activeDays = await computeActiveDaysThisMonth(childUid);
  panel.innerHTML = `
    <div class="g-card g-card-pad">
      <div class="section-title">This month</div>
      <div class="card-grid grid-2">
        <div class="g-card stat-card solid"><div class="stat-num">${formatMinutes(monthMinutes)}</div><div class="stat-label">Total study time</div></div>
        <div class="g-card stat-card solid"><div class="stat-num">${activeDays}</div><div class="stat-label">Active days</div></div>
      </div>
      <p class="muted small mt-16">A fuller emailed monthly report is available once you deploy the bonus report-emailing Cloud Function — see SETUP_GUIDE.md.</p>
    </div>
  `;
}

/* ---- Security tab (Emergency Lock + PIN) ---- */
function renderSecurity(childUid, controls) {
  const panel = document.querySelector('.pc-panel[data-panel="security"]');
  panel.innerHTML = `
    <div class="emergency-card mb-16">
      <div class="flex-between">
        <div>
          <div class="section-title" style="color:#fca5a5; margin-bottom:4px;">Emergency lock</div>
          <p class="muted small">Instantly blocks AI, downloads, chat and videos on this account until you turn it back off.</p>
        </div>
        <label class="switch"><input type="checkbox" id="emergencyToggle" ${controls.emergencyLock ? "checked" : ""}><span class="track"></span></label>
      </div>
    </div>
    <div class="g-card g-card-pad mb-16">
      <div class="section-title">Parent PIN</div>
      <p class="muted small">An extra quick-lock for shared devices. Firestore rules already guarantee only your signed-in Google account can change these settings — the PIN is a second, local check on top of that.</p>
      <div id="pinStatus" class="mt-16"></div>
    </div>
    <div class="g-card g-card-pad">
      <div class="section-title">Verification</div>
      <p class="muted small">Every change on this page is written under your account (${escapeHtml(ctxUser.email || "")}) and re-checked by server-side Firestore security rules — your child's app can read these settings but can never write to them.</p>
    </div>
  `;
  document.getElementById("emergencyToggle").addEventListener("change", (e) => {
    updateControls(childUid, { emergencyLock: e.target.checked });
    toast(e.target.checked ? "Emergency lock enabled." : "Emergency lock turned off.");
  });
  renderPinStatus();
}

async function renderPinStatus() {
  const box = document.getElementById("pinStatus");
  if (!box) return;
  const snap = await getDoc(doc(db, "parents", ctxUser.uid));
  const hasPin = !!(snap.exists() && snap.data().pinHash);
  box.innerHTML = hasPin
    ? `<div class="flex-between"><span class="pill pill-success">PIN set</span><div style="display:flex; gap:8px;"><button class="btn btn-ghost btn-sm" id="changePinBtn">Change</button><button class="btn btn-ghost btn-sm" id="removePinBtn">Remove</button></div></div>`
    : `<button class="btn btn-primary btn-sm" id="setPinBtn">Set a PIN</button>`;
  document.getElementById("setPinBtn")?.addEventListener("click", () => openPinModal());
  document.getElementById("changePinBtn")?.addEventListener("click", () => openPinModal());
  document.getElementById("removePinBtn")?.addEventListener("click", async () => {
    const { setDoc, serverTimestamp } = await import("./firebase-init.js");
    await setDoc(doc(db, "parents", ctxUser.uid), { pinHash: null, updatedAt: serverTimestamp() }, { merge: true });
    toast("PIN removed.");
    renderPinStatus();
  });
}

async function hashPin(pin) {
  const enc = new TextEncoder().encode(pin + ":" + ctxUser.uid);
  const buf = await crypto.subtle.digest("SHA-256", enc);
  return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, "0")).join("");
}

function openPinModal() {
  const overlay = document.createElement("div");
  overlay.className = "jmc-modal-overlay";
  overlay.innerHTML = `<div class="jmc-modal">
    <h3 style="margin-bottom:6px;">Set a 4-digit PIN</h3>
    <p class="muted small" id="pinModalHint">Enter a new PIN</p>
    <div class="pin-dots" id="pinDots">${"1111".split("").map(() => `<div class="pin-dot"></div>`).join("")}</div>
    <div class="pin-keypad" id="pinKeypad">
      ${[1,2,3,4,5,6,7,8,9,"",0,"⌫"].map(n => `<button data-key="${n}" ${n === "" ? "style=visibility:hidden" : ""}>${n}</button>`).join("")}
    </div>
    <button class="btn btn-ghost btn-block mt-16" id="pinCancelBtn">Cancel</button>
  </div>`;
  document.body.appendChild(overlay);
  requestAnimationFrame(() => overlay.classList.add("open"));

  let stage = "enter"; let firstPin = "";
  let buffer = "";
  const dots = () => qsa(".pin-dot", overlay);
  function updateDots() { dots().forEach((d, i) => d.classList.toggle("filled", i < buffer.length)); }
  function closeModal() { overlay.classList.remove("open"); setTimeout(() => overlay.remove(), 250); }

  qsa("[data-key]", overlay).forEach(btn => btn.addEventListener("click", async () => {
    const k = btn.dataset.key;
    if (k === "⌫") { buffer = buffer.slice(0, -1); updateDots(); return; }
    if (k === "" || buffer.length >= 4) return;
    buffer += k; updateDots();
    if (buffer.length === 4) {
      if (stage === "enter") {
        firstPin = buffer; buffer = "";
        stage = "confirm";
        qs("#pinModalHint", overlay).textContent = "Confirm your PIN";
        updateDots();
      } else {
        if (buffer !== firstPin) {
          toast("PINs didn't match — try again.", "error");
          stage = "enter"; firstPin = ""; buffer = "";
          qs("#pinModalHint", overlay).textContent = "Enter a new PIN";
          updateDots();
          return;
        }
        const { setDoc, serverTimestamp } = await import("./firebase-init.js");
        const hash = await hashPin(buffer);
        await setDoc(doc(db, "parents", ctxUser.uid), { pinHash: hash, updatedAt: serverTimestamp() }, { merge: true });
        toast("PIN saved.", "success");
        closeModal();
        renderPinStatus();
      }
    }
  }));
  document.getElementById("pinCancelBtn").addEventListener("click", closeModal);
  overlay.addEventListener("click", (e) => { if (e.target === overlay) closeModal(); });
}

/* ==================== Generate code modal (parent side) ==================== */
function openGenerateCodeModal() {
  const overlay = document.createElement("div");
  overlay.className = "jmc-modal-overlay";
  overlay.innerHTML = `<div class="jmc-modal">
    <h3 style="margin-bottom:6px;">Link a child's account</h3>
    <p class="muted small">Give this code to your child. They'll enter it under their own Settings → Parental controls to connect their account. It expires in 15 minutes and works once.</p>
    <div id="codeArea"><div class="skel" style="height:90px; margin:16px 0;"></div></div>
    <button class="btn btn-ghost btn-block" id="closeCodeModalBtn">Close</button>
  </div>`;
  document.body.appendChild(overlay);
  requestAnimationFrame(() => overlay.classList.add("open"));
  document.getElementById("closeCodeModalBtn").addEventListener("click", () => { overlay.classList.remove("open"); setTimeout(() => overlay.remove(), 250); });
  overlay.addEventListener("click", (e) => { if (e.target === overlay) { overlay.classList.remove("open"); setTimeout(() => overlay.remove(), 250); } });

  generateLinkCode(ctxUser.uid).then(({ code, expiresAt }) => {
    document.getElementById("codeArea").innerHTML = `
      <div class="code-display">${code}</div>
      <div class="code-timer" id="codeTimer"></div>
    `;
    const tick = () => {
      const remaining = Math.max(0, Math.round((expiresAt.toMillis() - Date.now()) / 1000));
      const el = document.getElementById("codeTimer");
      if (!el) return;
      if (remaining <= 0) { el.textContent = "Expired — generate a new code."; return; }
      el.textContent = `Expires in ${Math.floor(remaining / 60)}:${String(remaining % 60).padStart(2, "0")}`;
      setTimeout(tick, 1000);
    };
    tick();
  });
}

/* ==================== Data helpers ==================== */
async function fetchRecent(childUid, coll, max) {
  try {
    const q = query(collection(db, coll, childUid, "entries"), orderBy("at", "desc"), limit(max));
    const snap = await getDocs(q);
    const rows = []; snap.forEach(d => rows.push(d.data()));
    return rows;
  } catch { return []; }
}
async function fetchTimeline(childUid, max) {
  const [logins, study, activity] = await Promise.all([
    fetchRecent(childUid, "loginHistory", max), fetchRecent(childUid, "studyHistory", max), fetchRecent(childUid, "activityLogs", max),
  ]);
  const rows = [
    ...logins.map(l => ({ kind: "login", label: `Signed in (${l.device || "device"})`, at: l.at })),
    ...study.map(s => ({ kind: "study", label: `Studied ${formatMinutes(s.minutes)} · ${s.subject || "General"}`, at: s.at })),
    ...activity.map(a => ({ kind: "activity", label: a.detail ? `Opened ${a.detail}` : "Activity", at: a.at })),
  ];
  return rows.filter(r => r.at).sort((a, b) => (b.at?.toMillis?.() || 0) - (a.at?.toMillis?.() || 0)).slice(0, max);
}
async function computeWeekMinutes(childUid) { return sumMinutesSince(childUid, 7); }
async function computeMonthMinutes(childUid) { return sumMinutesSince(childUid, 30); }
async function sumMinutesSince(childUid, days) {
  try {
    const since = Timestamp.fromDate(new Date(Date.now() - days * 24 * 60 * 60 * 1000));
    const q = query(collection(db, "studyHistory", childUid, "entries"), where("at", ">=", since));
    const snap = await getDocs(q);
    let total = 0; snap.forEach(d => total += (d.data().minutes || 0));
    return total;
  } catch { return 0; }
}
async function computeActiveDaysThisMonth(childUid) {
  try {
    const since = Timestamp.fromDate(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000));
    const q = query(collection(db, "loginHistory", childUid, "entries"), where("at", ">=", since), limit(200));
    const snap = await getDocs(q);
    const days = new Set(); snap.forEach(d => { const at = d.data().at; if (at?.toDate) days.add(at.toDate().toDateString()); });
    return days.size;
  } catch { return 0; }
}
async function computeStreak(childUid) {
  try {
    const since = Timestamp.fromDate(new Date(Date.now() - 60 * 24 * 60 * 60 * 1000));
    const q = query(collection(db, "loginHistory", childUid, "entries"), where("at", ">=", since), orderBy("at", "desc"), limit(200));
    const snap = await getDocs(q);
    const days = new Set(); snap.forEach(d => { const at = d.data().at; if (at?.toDate) days.add(at.toDate().toDateString()); });
    if (!days.size) return 0;
    let streak = 0; const cursor = new Date();
    for (;;) { if (days.has(cursor.toDateString())) { streak++; cursor.setDate(cursor.getDate() - 1); } else break; }
    return streak;
  } catch { return 0; }
}

init();
