import { db, collection, query, where, orderBy, limit, getDocs, Timestamp } from "./firebase-init.js";
import { requireCompleteProfile } from "./auth-guard.js";
import { renderShell } from "./nav-shell.js";
import { applyTheme, getLocalThemePref, syncThemeFromCloud } from "./theme.js";
import { escapeHtml, formatMinutes, formatRelativeTime, qsa } from "./ui-utils.js";
import {
  getMyControls, subscribeMyControls, isFeatureEnabled, applyStudyModeToDom,
  logLoginEvent, logActivity, getTodayMinutes,
} from "./parent-controls-enforcer.js";
import { subscribeMyChildren } from "./parent-link.js";

applyTheme(getLocalThemePref());

const ACTIVITY_ICON = {
  resource_open: { icon: "📘", bg: "rgba(59,130,246,.15)" },
  login: { icon: "🔑", bg: "rgba(34,197,94,.15)" },
  ai_chat: { icon: "🤖", bg: "rgba(139,92,246,.15)" },
  default: { icon: "✨", bg: "rgba(148,163,196,.15)" },
};

async function init() {
  const ctx = await requireCompleteProfile();
  if (!ctx) return;
  const { user, profile } = ctx;
  await syncThemeFromCloud(user.uid);

  const roleTitles = { student: "Dashboard", teacher: "Teacher Dashboard", parent: "Parent Dashboard", operator: "School Dashboard" };
  const { content } = renderShell({ user, profile, active: "dashboard", pageTitle: roleTitles[profile.role] || "Dashboard" });

  // Log this visit as a login/session event, once per browser session (not on every re-render).
  if (!sessionStorage.getItem("jmc-session-logged")) {
    logLoginEvent(user.uid);
    sessionStorage.setItem("jmc-session-logged", "1");
  }

  const controls = profile.role === "student" ? await getMyControls(user.uid) : null;
  if (controls) {
    applyStudyModeToDom(controls);
    subscribeMyControls(user.uid, applyStudyModeToDom);
  }

  if (profile.role === "student") await renderStudent(content, user, profile, controls);
  else if (profile.role === "teacher") await renderTeacher(content, user, profile);
  else if (profile.role === "parent") await renderParent(content, user, profile);
  else await renderOperator(content, user, profile);
}

/* ==================== STUDENT ==================== */
async function renderStudent(content, user, profile, controls) {
  content.innerHTML = `
    <div class="study-mode-banner">📘 <span>Study Mode is on — your parent has focused this account on approved subjects only.</span></div>
    <div class="welcome-banner g-card">
      <div>
        <h2>Welcome back, ${escapeHtml(profile.fullName?.split(" ")[0] || "there")}! 👋</h2>
        <p>${escapeHtml(profile.school || "")} · ${escapeHtml(profile.grade || "")}</p>
      </div>
    </div>

    <div class="card-grid grid-4 mb-16" id="statCards"></div>

    <div class="card-grid grid-2" style="align-items:start;">
      <div class="g-card g-card-pad">
        <div class="section-title">Subjects you've studied</div>
        <div id="subjectBreakdown"><div class="skel" style="height:80px;"></div></div>
      </div>
      <div class="g-card g-card-pad">
        <div class="section-title">Recent activity</div>
        <div id="recentActivity"><div class="skel" style="height:80px;"></div></div>
      </div>
    </div>

    <div class="section-title mt-16">Continue learning</div>
    <div class="quick-links mb-16">
      <a href="/chatbot.html" data-log="AI Tutor" class="g-card quick-link-card" id="aiTutorLink"><div class="ql-emoji">🤖</div><div class="ql-name">AI Tutor</div></a>
      <a href="/exampapers.html" data-log="Exam Papers" class="g-card quick-link-card"><div class="ql-emoji">📝</div><div class="ql-name">Exam Papers</div></a>
      <a href="/text_books.html" data-log="Text Books" class="g-card quick-link-card"><div class="ql-emoji">📚</div><div class="ql-name">Text Books</div></a>
      <a href="/educational_tools.html" data-log="Smart Tools" class="g-card quick-link-card"><div class="ql-emoji">🧰</div><div class="ql-name">Smart Tools</div></a>
      <a href="/jmc_Classroom.html" data-log="JMC Classroom" class="g-card quick-link-card"><div class="ql-emoji">🏫</div><div class="ql-name">JMC Classroom</div></a>
      <a href="/notebook.html" data-log="AI Notebook" class="g-card quick-link-card"><div class="ql-emoji">📓</div><div class="ql-name">AI Notebook</div></a>
    </div>

    <div id="achievements" class="g-card g-card-pad mb-16">
      <div class="section-title">Achievements</div>
      <div class="empty-state"><div class="empty-state-icon">🏆</div><p>Achievements unlock as you study, take quizzes and stay consistent. Check back soon!</p></div>
    </div>

    <div id="calendar" class="g-card g-card-pad">
      <div class="section-title">Calendar</div>
      <div class="empty-state"><div class="empty-state-icon">📅</div><p>No upcoming items yet. Your teacher's assignments and reminders will appear here.</p></div>
    </div>
  `;

  // Parent-control gating on the AI Tutor tile.
  if (controls && !isFeatureEnabled(controls, "ai")) {
    const tile = document.getElementById("aiTutorLink");
    tile.classList.add("hidden-disabled");
    tile.removeAttribute("href");
    tile.style.opacity = "0.45";
    tile.style.cursor = "not-allowed";
    tile.querySelector(".ql-name").textContent = "AI Tutor (paused)";
    tile.addEventListener("click", (e) => { e.preventDefault(); import("./ui-utils.js").then(m => m.toast("Your parent has paused AI Tutor access.", "info")); });
  }

  qsa("[data-log]").forEach(link => {
    link.addEventListener("click", () => logActivity(user.uid, "resource_open", link.dataset.log));
  });

  const [streakDays, weekMinutes, todayMinutes, subjectTotals, activity] = await Promise.all([
    computeStreakDays(user.uid),
    computeWeekMinutes(user.uid),
    getTodayMinutes(user.uid),
    computeSubjectTotals(user.uid),
    fetchRecentActivity(user.uid, 6),
  ]);
  const resourcesOpened = await countResourcesOpened(user.uid);
  const xp = Math.round(weekMinutes * 2 + resourcesOpened * 5);

  document.getElementById("statCards").innerHTML = [
    statCard("🔥", "Study Streak", `${streakDays} ${streakDays === 1 ? "day" : "days"}`, "rgba(249,115,22,.16)"),
    statCard("⏱️", "This Week", formatMinutes(weekMinutes), "rgba(59,130,246,.16)"),
    statCard("⭐", "XP Points", xp.toLocaleString(), "rgba(242,167,27,.16)"),
    statCard("📘", "Resources Opened", String(resourcesOpened), "rgba(20,184,166,.16)"),
  ].join("");

  renderSubjectBreakdown(document.getElementById("subjectBreakdown"), subjectTotals);
  renderActivityList(document.getElementById("recentActivity"), activity);

  if (controls && controls.dailyLimitMinutes > 0) {
    const remaining = Math.max(0, controls.dailyLimitMinutes - todayMinutes);
    if (remaining <= 15) {
      import("./ui-utils.js").then(m => m.toast(`Heads up — ${remaining}m left of today's study time.`, "info", 5000));
    }
  }
}

function statCard(emoji, label, value, bg) {
  return `<div class="g-card stat-card">
    <div class="stat-icon" style="background:${bg}">${emoji}</div>
    <div class="stat-num">${value}</div>
    <div class="stat-label">${label}</div>
  </div>`;
}

function renderSubjectBreakdown(el, totals) {
  const entries = Object.entries(totals).sort((a, b) => b[1] - a[1]).slice(0, 5);
  if (!entries.length) {
    el.innerHTML = `<div class="empty-state"><div class="empty-state-icon">📊</div><p>Start studying and your subject breakdown will show up here.</p></div>`;
    return;
  }
  const max = entries[0][1] || 1;
  const colors = ["var(--blue-2)", "var(--green)", "var(--gold)", "var(--purple)", "var(--pink)"];
  el.innerHTML = entries.map(([name, mins], i) => `
    <div class="subject-row">
      <div class="subject-name">${escapeHtml(name)}</div>
      <div class="bar" style="flex:1;"><i style="width:${Math.round((mins / max) * 100)}%; background:${colors[i % colors.length]};"></i></div>
      <div class="subject-pct">${formatMinutes(mins)}</div>
    </div>
  `).join("");
}

function renderActivityList(el, rows) {
  if (!rows.length) {
    el.innerHTML = `<div class="empty-state"><div class="empty-state-icon">🕒</div><p>Nothing logged yet — your recent activity will appear here.</p></div>`;
    return;
  }
  el.innerHTML = rows.map(r => {
    const meta = ACTIVITY_ICON[r.type] || ACTIVITY_ICON.default;
    return `<div class="activity-item">
      <div class="activity-dot" style="background:${meta.bg}">${meta.icon}</div>
      <div>
        <div class="activity-text">${escapeHtml(activityLabel(r))}</div>
        <div class="activity-time">${formatRelativeTime(r.at)}</div>
      </div>
    </div>`;
  }).join("");
}
function activityLabel(r) {
  if (r.type === "resource_open") return `Opened ${r.detail || "a resource"}`;
  if (r.type === "login") return "Signed in";
  return r.detail || "Activity";
}

/* ==================== TEACHER ==================== */
async function renderTeacher(content, user, profile) {
  content.innerHTML = `
    <div class="welcome-banner g-card">
      <div><h2>Welcome back, ${escapeHtml(profile.fullName?.split(" ")[0] || "there")}! 🧑‍🏫</h2>
      <p>${escapeHtml(profile.school || "")}</p></div>
    </div>
    <div class="card-grid grid-3 mb-16">
      ${statCard("👥", "Your Classes", "0", "rgba(59,130,246,.16)")}
      ${statCard("📨", "Messages", "0", "rgba(236,72,153,.16)")}
      ${statCard("📈", "Resources Shared", "0", "rgba(20,184,166,.16)")}
    </div>
    <div class="g-card g-card-pad mb-16" id="classes">
      <div class="section-title">My classes</div>
      <div class="empty-state"><div class="empty-state-icon">🏫</div><p>Class & gradebook management is coming in a future update. For now, use JMC Classroom and share resources directly with students.</p></div>
    </div>
    <div class="section-title">Quick links</div>
    <div class="quick-links">
      <a href="/jmc_Classroom.html" class="g-card quick-link-card"><div class="ql-emoji">🏫</div><div class="ql-name">JMC Classroom</div></a>
      <a href="/educational_tools.html" class="g-card quick-link-card"><div class="ql-emoji">🧰</div><div class="ql-name">Smart Tools</div></a>
      <a href="/exampapers.html" class="g-card quick-link-card"><div class="ql-emoji">📝</div><div class="ql-name">Exam Papers</div></a>
      <a href="/text_books.html" class="g-card quick-link-card"><div class="ql-emoji">📚</div><div class="ql-name">Text Books</div></a>
      <a href="/tell_us.html" class="g-card quick-link-card"><div class="ql-emoji">✉️</div><div class="ql-name">Message Admin</div></a>
    </div>
  `;
}

/* ==================== PARENT ==================== */
async function renderParent(content, user, profile) {
  content.innerHTML = `
    <div class="welcome-banner g-card">
      <div><h2>Welcome back, ${escapeHtml(profile.fullName?.split(" ")[0] || "there")}! 👪</h2>
      <p>Keep an eye on your child's learning, right from here.</p></div>
      <a href="/parent-control.html" class="btn btn-primary">Open Parent Control</a>
    </div>
    <div id="children" class="g-card g-card-pad">
      <div class="section-title">Your children</div>
      <div id="childrenList"><div class="skel" style="height:70px; margin-bottom:10px;"></div></div>
    </div>
  `;

  subscribeMyChildren(user.uid, async (rows) => {
    const box = document.getElementById("childrenList");
    if (!rows.length) {
      box.innerHTML = `<div class="empty-state">
        <div class="empty-state-icon">🔗</div>
        <p>You haven't linked a child's account yet.</p>
        <a href="/parent-control.html" class="btn btn-primary btn-sm mt-8">Link a child</a>
      </div>`;
      return;
    }
    box.innerHTML = rows.map(r => `
      <div class="child-mini-card">
        <div class="shell-avatar">👤</div>
        <div style="flex:1;">
          <div style="font-weight:700; font-size:13.4px;">${escapeHtml(r.childUid.slice(0, 6))}… ${statusPill(r.status)}</div>
          <div class="child-stat-mini">Linked ${formatRelativeTime(r.linkedAt)}</div>
        </div>
        <a href="/parent-control.html" class="btn btn-ghost btn-sm">Manage</a>
      </div>
    `).join("");
  });
}
function statusPill(status) {
  if (status === "active") return `<span class="pill pill-success">Active</span>`;
  if (status === "pending") return `<span class="pill pill-warn">Pending approval</span>`;
  return `<span class="pill pill-muted">${escapeHtml(status)}</span>`;
}

/* ==================== OPERATOR ==================== */
async function renderOperator(content, user, profile) {
  content.innerHTML = `
    <div class="welcome-banner g-card">
      <div><h2>Welcome back, ${escapeHtml(profile.fullName?.split(" ")[0] || "there")}! 🏫</h2>
      <p>${escapeHtml(profile.school || "")}</p></div>
    </div>
    <div class="card-grid grid-4 mb-16">
      ${statCard("👨‍🎓", "Students", "0", "rgba(59,130,246,.16)")}
      ${statCard("🧑‍🏫", "Teachers", "0", "rgba(139,92,246,.16)")}
      ${statCard("✅", "Active Today", "0", "rgba(34,197,94,.16)")}
      ${statCard("📢", "Announcements", "0", "rgba(242,167,27,.16)")}
    </div>
    <div class="g-card g-card-pad">
      <div class="section-title">School overview</div>
      <div class="empty-state"><div class="empty-state-icon">🏫</div><p>School-wide analytics and staff management are next on the roadmap — this dashboard will populate as your school's teachers and students join.</p></div>
    </div>
  `;
}

/* ==================== Data helpers (real Firestore aggregation) ==================== */
async function fetchRecentActivity(uid, max) {
  try {
    const q = query(collection(db, "activityLogs", uid, "entries"), orderBy("at", "desc"), limit(max));
    const snap = await getDocs(q);
    const rows = [];
    snap.forEach(d => rows.push(d.data()));
    return rows;
  } catch { return []; }
}

async function countResourcesOpened(uid) {
  try {
    const q = query(collection(db, "activityLogs", uid, "entries"), where("type", "==", "resource_open"));
    const snap = await getDocs(q);
    return snap.size;
  } catch { return 0; }
}

async function computeWeekMinutes(uid) {
  try {
    const since = Timestamp.fromDate(new Date(Date.now() - 7 * 24 * 60 * 60 * 1000));
    const q = query(collection(db, "studyHistory", uid, "entries"), where("at", ">=", since));
    const snap = await getDocs(q);
    let total = 0; snap.forEach(d => total += (d.data().minutes || 0));
    return total;
  } catch { return 0; }
}

async function computeSubjectTotals(uid) {
  try {
    const since = Timestamp.fromDate(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000));
    const q = query(collection(db, "studyHistory", uid, "entries"), where("at", ">=", since));
    const snap = await getDocs(q);
    const totals = {};
    snap.forEach(d => {
      const { subject = "General", minutes = 0 } = d.data();
      totals[subject] = (totals[subject] || 0) + minutes;
    });
    return totals;
  } catch { return {}; }
}

async function computeStreakDays(uid) {
  try {
    const since = Timestamp.fromDate(new Date(Date.now() - 60 * 24 * 60 * 60 * 1000));
    const q = query(collection(db, "loginHistory", uid, "entries"), where("at", ">=", since), orderBy("at", "desc"), limit(200));
    const snap = await getDocs(q);
    const days = new Set();
    snap.forEach(d => {
      const at = d.data().at;
      if (at && at.toDate) days.add(at.toDate().toDateString());
    });
    if (!days.size) return 0;
    let streak = 0;
    const cursor = new Date();
    for (;;) {
      if (days.has(cursor.toDateString())) { streak++; cursor.setDate(cursor.getDate() - 1); }
      else break;
    }
    return streak;
  } catch { return 0; }
}

init();
