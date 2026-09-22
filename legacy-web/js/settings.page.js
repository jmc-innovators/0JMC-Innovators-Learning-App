import {
  auth, db, doc, getDoc, setDoc, getDocs, collection, query, orderBy, limit,
  serverTimestamp, deleteUser, reauthenticateWithPopup, provider, functions, httpsCallable, signOut,
} from "./firebase-init.js";
import { requireCompleteProfile } from "./auth-guard.js";
import { renderShell } from "./nav-shell.js";
import { applyTheme, getLocalThemePref, setLocalThemePref, saveThemeToCloud, syncThemeFromCloud } from "./theme.js";
import { toast, escapeHtml, formatRelativeTime, qs, qsa, describeDevice } from "./ui-utils.js";
import { subscribeList, renderNotificationList } from "./notifications.js";
import { getMyControls } from "./parent-controls-enforcer.js";
import { subscribeMyChildren, claimLinkCode, subscribeMyLink } from "./parent-link.js";

applyTheme(getLocalThemePref());

const DEFAULT_USER_SETTINGS = {
  fontSize: "medium", reduceAnimations: false, compactMode: false, showHints: true,
  notif: { assignment: true, result: true, announcement: true, ai: true, security: true, parent: true, system: true },
  privacy: { showProfileToClass: true },
  dashboard: { showStudyModeBanner: true },
};

let ctxUser, ctxProfile, userSettings;

async function init() {
  const ctx = await requireCompleteProfile();
  if (!ctx) return;
  ctxUser = ctx.user; ctxProfile = ctx.profile;
  await syncThemeFromCloud(ctxUser.uid);

  const { content } = renderShell({ user: ctxUser, profile: ctxProfile, active: "settings", pageTitle: "Settings" });
  userSettings = await loadUserSettings(ctxUser.uid);
  applyLocalSettingEffects();

  content.innerHTML = layoutHtml(ctxProfile.role);
  wireNav();
  renderAppearance();
  renderAccount();
  renderNotifications();
  renderPrivacySecurity();
  renderParentalSection(ctxProfile.role);
  renderConnected();
  renderAccessibility();
  renderDashboardPrefs();
  renderAbout();

  if (window.location.hash === "#notifications") activateSection("notifications");
}

async function loadUserSettings(uid) {
  try {
    const snap = await getDoc(doc(db, "userSettings", uid));
    return snap.exists() ? { ...DEFAULT_USER_SETTINGS, ...snap.data() } : { ...DEFAULT_USER_SETTINGS };
  } catch { return { ...DEFAULT_USER_SETTINGS }; }
}
async function saveUserSettings(patch) {
  userSettings = { ...userSettings, ...patch };
  try {
    await setDoc(doc(db, "userSettings", ctxUser.uid), { ...userSettings, updatedAt: serverTimestamp() }, { merge: true });
  } catch (e) { console.warn("save settings failed", e); }
}
function applyLocalSettingEffects() {
  document.documentElement.setAttribute("data-font", userSettings.fontSize || "medium");
  document.documentElement.classList.toggle("reduce-motion", !!userSettings.reduceAnimations);
}

/* ---------------- Layout ---------------- */
function layoutHtml(role) {
  const navItems = [
    ["appearance", "Appearance"], ["account", "Account"], ["notifications", "Notifications"],
    ["privacy", "Privacy & Security"],
    ...(role !== "operator" ? [["parental", role === "parent" ? "Parental Control" : "Parental Controls on My Account"]] : []),
    ["connected", "Connected Accounts"], ["accessibility", "Accessibility"],
    ["dashboard-prefs", "Dashboard Preferences"], ["about", "About"],
  ];
  return `
  <div class="settings-layout">
    <nav class="settings-nav">
      ${navItems.map(([k, label], i) => `<button data-nav="${k}" class="${i === 0 ? "active" : ""}">${label}</button>`).join("")}
    </nav>
    <div>
      <section class="settings-section active" data-section="appearance"></section>
      <section class="settings-section" data-section="account"></section>
      <section class="settings-section" data-section="notifications"></section>
      <section class="settings-section" data-section="privacy"></section>
      <section class="settings-section" data-section="parental"></section>
      <section class="settings-section" data-section="connected"></section>
      <section class="settings-section" data-section="accessibility"></section>
      <section class="settings-section" data-section="dashboard-prefs"></section>
      <section class="settings-section" data-section="about"></section>
    </div>
  </div>`;
}
function activateSection(key) {
  qsa(".settings-nav button").forEach(b => b.classList.toggle("active", b.dataset.nav === key));
  qsa(".settings-section").forEach(s => s.classList.toggle("active", s.dataset.section === key));
}
function wireNav() {
  qsa(".settings-nav button").forEach(b => b.addEventListener("click", () => activateSection(b.dataset.nav)));
}
function section(key) { return document.querySelector(`.settings-section[data-section="${key}"]`); }

/* ---------------- Appearance ---------------- */
function renderAppearance() {
  const pref = getLocalThemePref();
  section("appearance").innerHTML = `
    <div class="g-card g-card-pad mb-16">
      <div class="section-title">Theme</div>
      <div class="theme-options" id="themeOptions">
        ${["light", "dark", "system"].map(t => `
          <div class="theme-opt ${pref === t ? "selected" : ""}" data-theme-opt="${t}">
            <div class="t-icon">${t === "light" ? "☀️" : t === "dark" ? "🌙" : "🖥️"}</div>
            <div class="t-name">${t[0].toUpperCase() + t.slice(1)}</div>
          </div>`).join("")}
      </div>
    </div>
    <div class="g-card g-card-pad">
      <div class="setting-row"><div><div class="setting-row-label">Compact mode</div><div class="setting-row-desc">Tighter spacing, more content per screen.</div></div>${switchHtml("compactMode", userSettings.compactMode)}</div>
      <div class="setting-row"><div><div class="setting-row-label">Show hints</div><div class="setting-row-desc">Helpful tips shown around the dashboard.</div></div>${switchHtml("showHints", userSettings.showHints)}</div>
    </div>
  `;
  qsa("[data-theme-opt]").forEach(el => el.addEventListener("click", async () => {
    const t = el.dataset.themeOpt;
    qsa("[data-theme-opt]").forEach(o => o.classList.toggle("selected", o === el));
    await saveThemeToCloud(ctxUser.uid, t);
    toast("Theme updated");
  }));
  wireSwitch("compactMode", (val) => { document.body.classList.toggle("compact-mode", val); saveUserSettings({ compactMode: val }); });
  wireSwitch("showHints", (val) => saveUserSettings({ showHints: val }));
}

/* ---------------- Account ---------------- */
function renderAccount() {
  const p = ctxProfile;
  section("account").innerHTML = `
    <div class="g-card g-card-pad mb-16">
      <div class="section-title">Your profile</div>
      <div class="field"><label>Full name</label><input value="${escapeHtml(p.fullName || "")}" disabled></div>
      <div class="grid-2col" style="display:grid; grid-template-columns:1fr 1fr; gap:14px;">
        <div class="field"><label>School</label><input value="${escapeHtml(p.school || "")}" disabled></div>
        <div class="field"><label>Grade</label><input value="${escapeHtml(p.grade || "—")}" disabled></div>
        <div class="field"><label>Country</label><input value="${escapeHtml(p.country || "")}" disabled></div>
        <div class="field"><label>Language</label><input value="${escapeHtml({ en: "English", si: "Sinhala", ta: "Tamil" }[p.language] || p.language || "")}" disabled></div>
      </div>
      <a href="/profile.html" class="btn btn-primary mt-8">Edit profile</a>
    </div>
    <div class="g-card g-card-pad">
      <div class="setting-row"><div><div class="setting-row-label">Role</div><div class="setting-row-desc">Contact support if this needs to change.</div></div><span class="pill">${escapeHtml(p.role)}</span></div>
      <div class="setting-row"><div><div class="setting-row-label">Account status</div></div><span class="pill pill-success">Active</span></div>
    </div>
  `;
}

/* ---------------- Notifications ---------------- */
function renderNotifications() {
  const cats = [
    ["assignment", "Assignments"], ["result", "Results"], ["announcement", "Announcements"],
    ["ai", "AI Tutor"], ["security", "Security alerts"], ["parent", "Parent notices"], ["system", "System"],
  ];
  section("notifications").innerHTML = `
    <div class="g-card g-card-pad mb-16">
      <div class="section-title">Notification categories</div>
      ${cats.map(([k, label]) => `
        <div class="setting-row"><div class="setting-row-label">${label}</div>${switchHtml("notif." + k, userSettings.notif?.[k] !== false)}</div>
      `).join("")}
    </div>
    <div class="g-card g-card-pad">
      <div class="section-title">Recent notifications</div>
      <div id="notifListBox"><div class="skel" style="height:60px;"></div></div>
    </div>
  `;
  cats.forEach(([k]) => wireSwitch("notif." + k, (val) => {
    const notif = { ...userSettings.notif, [k]: val };
    saveUserSettings({ notif });
  }));
  subscribeList(ctxUser.uid, (rows) => renderNotificationList(document.getElementById("notifListBox"), rows, { uid: ctxUser.uid }));
}

/* ---------------- Privacy & Security ---------------- */
function renderPrivacySecurity() {
  section("privacy").innerHTML = `
    <div class="g-card g-card-pad mb-16">
      <div class="section-title">Sessions</div>
      <div class="setting-row"><div><div class="setting-row-label">This device</div><div class="setting-row-desc">${escapeHtml(describeDevice().label)}</div></div><span class="pill pill-success">Current</span></div>
      <div id="deviceList" class="mt-8"></div>
      <button class="btn btn-ghost mt-16" id="logoutEverywhereBtn">Log out everywhere</button>
    </div>
    <div class="g-card g-card-pad mb-16">
      <div class="section-title">Change password</div>
      <p class="muted small">Your account signs in with Google, so your password is managed entirely by your Google Account.</p>
      <a href="https://myaccount.google.com/security" target="_blank" rel="noopener" class="btn btn-ghost mt-8">Manage on Google</a>
    </div>
    <div class="g-card g-card-pad mb-16">
      <div class="section-title">Your data</div>
      <div class="setting-row"><div><div class="setting-row-label">Export my data</div><div class="setting-row-desc">Download everything JMC Innovators has stored about you, as a JSON file.</div></div><button class="btn btn-ghost btn-sm" id="exportDataBtn">Export</button></div>
    </div>
    <div class="danger-zone">
      <div class="section-title" style="color:#fca5a5;">Delete account</div>
      <p class="muted small">This permanently deletes your JMC Innovators profile, settings and activity history, and your sign-in. This can't be undone.</p>
      <button class="btn btn-danger mt-8" id="deleteAccountBtn">Delete my account</button>
    </div>
  `;

  loadRecentDevices();
  document.getElementById("exportDataBtn").addEventListener("click", exportMyData);
  document.getElementById("deleteAccountBtn").addEventListener("click", confirmDeleteAccount);
  document.getElementById("logoutEverywhereBtn").addEventListener("click", logoutEverywhere);
}

async function loadRecentDevices() {
  try {
    const q = query(collection(db, "loginHistory", ctxUser.uid, "entries"), orderBy("at", "desc"), limit(5));
    const snap = await getDocs(q);
    const rows = []; snap.forEach(d => rows.push(d.data()));
    const box = document.getElementById("deviceList");
    if (!rows.length) { box.innerHTML = `<p class="muted small">No sign-in history yet.</p>`; return; }
    box.innerHTML = rows.map(r => `
      <div class="device-row"><span style="font-size:16px;">💻</span>
        <div><div style="font-size:12.8px; font-weight:700;">${escapeHtml(r.device || "Unknown device")}</div>
        <div class="muted small">${formatRelativeTime(r.at)}</div></div>
      </div>`).join("");
  } catch { /* rules or offline — silently skip */ }
}

async function exportMyData() {
  toast("Preparing your data…");
  try {
    const uid = ctxUser.uid;
    const [userDoc, settingsDoc, themeDoc, studyHist, loginHist, activity] = await Promise.all([
      getDoc(doc(db, "users", uid)),
      getDoc(doc(db, "userSettings", uid)),
      getDoc(doc(db, "themeSettings", uid)),
      getDocs(query(collection(db, "studyHistory", uid, "entries"), orderBy("at", "desc"), limit(500))),
      getDocs(query(collection(db, "loginHistory", uid, "entries"), orderBy("at", "desc"), limit(500))),
      getDocs(query(collection(db, "activityLogs", uid, "entries"), orderBy("at", "desc"), limit(500))),
    ]);
    const toArr = (snap) => { const a = []; snap.forEach(d => a.push({ id: d.id, ...d.data() })); return a; };
    const exportObj = {
      exportedAt: new Date().toISOString(),
      account: { uid, email: ctxUser.email },
      profile: userDoc.exists() ? userDoc.data() : null,
      settings: settingsDoc.exists() ? settingsDoc.data() : null,
      theme: themeDoc.exists() ? themeDoc.data() : null,
      studyHistory: toArr(studyHist), loginHistory: toArr(loginHist), activityLogs: toArr(activity),
    };
    const blob = new Blob([JSON.stringify(exportObj, null, 2)], { type: "application/json" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = `jmc-innovators-data-${uid.slice(0, 6)}.json`;
    a.click();
    URL.revokeObjectURL(a.href);
    toast("Your data was downloaded.", "success");
  } catch (e) {
    console.error(e);
    toast("Couldn't export your data right now.", "error");
  }
}

async function confirmDeleteAccount() {
  if (!confirm("Delete your JMC Innovators account permanently? This cannot be undone.")) return;
  try {
    toast("Confirm with Google to continue…");
    await reauthenticateWithPopup(auth.currentUser, provider);
    const uid = ctxUser.uid;
    await Promise.allSettled([
      import("./firebase-init.js").then(m => m.deleteDoc ? m.deleteDoc(doc(db, "users", uid)) : null),
      setDoc(doc(db, "users", uid), { deletedAt: serverTimestamp(), profileComplete: false }, { merge: true }),
    ]);
    await deleteUser(auth.currentUser);
    toast("Account deleted.", "success");
    setTimeout(() => window.location.href = "/index.html", 800);
  } catch (e) {
    console.error(e);
    toast("Couldn't delete account: " + (e.message || "please try again"), "error");
  }
}

async function logoutEverywhere() {
  try {
    const revoke = httpsCallable(functions, "revokeAllSessions");
    await revoke();
    toast("Signed out on every device.", "success");
  } catch (e) {
    console.warn("Cloud Function not deployed / failed, falling back to local sign-out:", e);
    toast("Signed out this device. (Deploy the bonus Cloud Function for true 'everywhere' sign-out — see SETUP_GUIDE.md)", "info", 6000);
  } finally {
    setTimeout(async () => { await signOut(auth); window.location.href = "/index.html"; }, 1200);
  }
}

/* ---------------- Parental section ---------------- */
async function renderParentalSection(role) {
  const el = section("parental");
  if (role === "parent") {
    el.innerHTML = `<div class="g-card g-card-pad">
      <div class="section-title">Parental Control</div>
      <p class="muted small">Manage linked children, restrictions, and monitoring from the dedicated Parent Control page.</p>
      <a href="/parent-control.html" class="btn btn-primary mt-8">Open Parent Control</a>
    </div>`;
  } else if (role === "student") {
    el.innerHTML = `<div class="g-card g-card-pad">
      <div class="section-title">Parental controls on your account</div>
      <div id="studentLinkStatus"><div class="skel" style="height:60px;"></div></div>
    </div>`;
    subscribeMyLink(ctxUser.uid, async (link) => {
      const box = document.getElementById("studentLinkStatus");
      if (!box) return;
      if (!link) {
        box.innerHTML = `
          <p class="muted small">No parent is linked yet. Ask a parent to open <b>Parent Control</b> on their account, generate a code, and give it to you.</p>
          <div class="field mt-16"><label for="linkCodeInput">Enter the 6-digit code</label>
            <input id="linkCodeInput" inputmode="numeric" maxlength="6" placeholder="482913">
          </div>
          <button class="btn btn-primary" id="claimCodeBtn">Link my account</button>`;
        document.getElementById("claimCodeBtn").addEventListener("click", async () => {
          const btn = document.getElementById("claimCodeBtn");
          btn.disabled = true; btn.textContent = "Linking…";
          const res = await claimLinkCode(ctxUser.uid, document.getElementById("linkCodeInput").value);
          if (res.ok) { toast("Linked! Waiting for your parent to approve.", "success"); }
          else { toast(res.reason, "error"); btn.disabled = false; btn.textContent = "Link my account"; }
        });
      } else if (link.status === "pending") {
        box.innerHTML = `<div class="empty-state"><div class="empty-state-icon">⏳</div><p>Waiting for your parent to approve the link.</p></div>`;
      } else if (link.status === "active") {
        const controls = await getMyControls(ctxUser.uid);
        box.innerHTML = `
          <p class="muted small">A parent has linked this account. These settings are managed by them and can't be changed here.</p>
          <div class="mt-16">
            ${settingReadout("AI Tutor", controls.aiEnabled)}
            ${settingReadout("Downloads", controls.downloadsEnabled)}
            ${settingReadout("Chat", controls.chatEnabled)}
            ${settingReadout("Videos", controls.videosEnabled)}
            ${settingReadout("Study Mode", controls.studyModeEnabled)}
          </div>`;
      } else {
        box.innerHTML = `<p class="muted small">Your last link request was not approved. Ask your parent for a new code to try again.</p>`;
      }
    });
  } else {
    el.innerHTML = `<div class="g-card g-card-pad"><p class="muted small">Parental controls apply to student and parent accounts.</p></div>`;
  }
}
function settingReadout(label, enabled) {
  return `<div class="setting-row"><div class="setting-row-label">${label}</div><span class="pill ${enabled ? "pill-success" : "pill-danger"}">${enabled ? "Allowed" : "Blocked"}</span></div>`;
}

/* ---------------- Connected accounts ---------------- */
function renderConnected() {
  const p = auth.currentUser?.providerData?.[0];
  section("connected").innerHTML = `
    <div class="g-card g-card-pad">
      <div class="section-title">Connected accounts</div>
      <div class="setting-row">
        <div style="display:flex; align-items:center; gap:12px;">
          <svg width="20" height="20" viewBox="0 0 48 48"><path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3c-1.6 4.6-6 8-11.3 8-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34.6 6 29.6 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.7-.4-3.5z"/><path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.6 15.9 18.9 13 24 13c3.1 0 5.9 1.2 8 3.1l5.7-5.7C34.6 6 29.6 4 24 4 16.3 4 9.7 8.3 6.3 14.7z"/><path fill="#4CAF50" d="M24 44c5.5 0 10.4-1.9 14.3-5.1l-6.6-5.4C29.6 35.4 26.9 36 24 36c-5.3 0-9.7-3.4-11.3-8l-6.6 5.1C9.6 39.6 16.3 44 24 44z"/><path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.3-2.2 4.3-4.1 5.6l6.6 5.4C41.8 35.6 44 30.2 44 24c0-1.3-.1-2.7-.4-3.5z"/></svg>
          <div><div class="setting-row-label">Google</div><div class="setting-row-desc">${escapeHtml(p?.email || ctxUser.email || "")}</div></div>
        </div>
        <span class="pill pill-success">Connected</span>
      </div>
    </div>
  `;
}

/* ---------------- Accessibility ---------------- */
function renderAccessibility() {
  section("accessibility").innerHTML = `
    <div class="g-card g-card-pad mb-16">
      <div class="section-title">Font size</div>
      <div class="font-size-row">
        <button id="fontDec">A-</button>
        <div class="font-size-label" id="fontLabel">${cap(userSettings.fontSize)}</div>
        <button id="fontInc">A+</button>
      </div>
    </div>
    <div class="g-card g-card-pad">
      <div class="setting-row"><div><div class="setting-row-label">Reduce animations</div><div class="setting-row-desc">Minimises motion across the app.</div></div>${switchHtml("reduceAnimations", userSettings.reduceAnimations)}</div>
      <div class="setting-row"><div><div class="setting-row-label">Keyboard navigation</div><div class="setting-row-desc">Every control here is reachable with Tab and shows a visible focus ring.</div></div><span class="pill pill-success">Supported</span></div>
    </div>
  `;
  const sizes = ["small", "medium", "large"];
  function setFont(size) {
    userSettings.fontSize = size;
    document.getElementById("fontLabel").textContent = cap(size);
    document.documentElement.setAttribute("data-font", size);
    saveUserSettings({ fontSize: size });
  }
  document.getElementById("fontDec").addEventListener("click", () => {
    const i = Math.max(0, sizes.indexOf(userSettings.fontSize) - 1); setFont(sizes[i]);
  });
  document.getElementById("fontInc").addEventListener("click", () => {
    const i = Math.min(sizes.length - 1, sizes.indexOf(userSettings.fontSize) + 1); setFont(sizes[i]);
  });
  wireSwitch("reduceAnimations", (val) => { document.documentElement.classList.toggle("reduce-motion", val); saveUserSettings({ reduceAnimations: val }); });
}
function cap(s) { return s ? s[0].toUpperCase() + s.slice(1) : "Medium"; }

/* ---------------- Dashboard preferences ---------------- */
function renderDashboardPrefs() {
  section("dashboard-prefs").innerHTML = `
    <div class="g-card g-card-pad">
      <div class="setting-row"><div><div class="setting-row-label">Show Study Mode banner</div><div class="setting-row-desc">Display the banner on your dashboard when Study Mode is active.</div></div>${switchHtml("dashboard.showStudyModeBanner", userSettings.dashboard?.showStudyModeBanner !== false)}</div>
    </div>
  `;
  wireSwitch("dashboard.showStudyModeBanner", (val) => saveUserSettings({ dashboard: { ...userSettings.dashboard, showStudyModeBanner: val } }));
}

/* ---------------- About ---------------- */
function renderAbout() {
  section("about").innerHTML = `
    <div class="g-card g-card-pad mb-16">
      <div class="section-title">App version</div>
      <p class="muted small">JMC Innovators Learning Platform — v2.0.0</p>
    </div>
    <div class="g-card g-card-pad about-links">
      <a href="/about2.html">About JMC Innovators</a>
      <a href="mailto:jmc.innovators2027@gmail.com?subject=Privacy%20Policy%20question">Privacy Policy</a>
      <a href="mailto:jmc.innovators2027@gmail.com?subject=Terms%20of%20Service%20question">Terms of Service</a>
      <a href="mailto:jmc.innovators2027@gmail.com?subject=Support%20request">Support</a>
      <a href="mailto:jmc.innovators2027@gmail.com?subject=Feedback">Send Feedback</a>
      <a href="/tell_us.html">Tell Us (contact form)</a>
    </div>
  `;
}

/* ---------------- Switch helper ---------------- */
function switchHtml(key, checked) {
  return `<label class="switch"><input type="checkbox" data-switch="${key}" ${checked ? "checked" : ""}><span class="track"></span></label>`;
}
function wireSwitch(key, onChange) {
  const el = document.querySelector(`[data-switch="${key}"]`);
  if (el) el.addEventListener("change", () => onChange(el.checked));
}

init();
