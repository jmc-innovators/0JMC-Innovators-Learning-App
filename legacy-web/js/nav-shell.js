/**
 * nav-shell.js
 * ------------------------------------------------------------------
 * Renders the app sidebar + topbar shared by dashboard.html,
 * settings.html and parent-control.html, and adapts entirely to the
 * signed-in user's role.
 *
 * "Parent Control" only ever appears in NAV_CONFIG.parent — a user who
 * is not signed in, or signed in with any other role, never has it
 * rendered into the DOM at all (not just hidden with CSS).
 * ------------------------------------------------------------------
 */
import { auth, signOut } from "./firebase-init.js";
import { initials, escapeHtml } from "./ui-utils.js";
import { subscribeUnreadCount } from "./notifications.js";
import { getLocalThemePref, saveThemeToCloud } from "./theme.js";

const ICONS = {
  dashboard: '<path d="M4 13h6V4H4v9zm0 7h6v-5H4v5zm10 0h6V11h-6v9zm0-16v5h6V4h-6z" fill="currentColor"/>',
  learning: '<path d="M12 3L2 8l10 5 10-5-10-5zM4 12.8V17c0 1.8 3 3.5 8 3.5s8-1.7 8-3.5v-4.2l-8 4-8-4z" fill="currentColor"/>',
  ai: '<circle cx="12" cy="12" r="3.2" fill="currentColor"/><path d="M12 2v3.4M12 18.6V22M22 12h-3.4M5.4 12H2M18.7 5.3l-2.4 2.4M7.7 16.3l-2.4 2.4M18.7 18.7l-2.4-2.4M7.7 7.7 5.3 5.3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>',
  exam: '<path d="M6 2h9l5 5v15H6V2z" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M9 12h6M9 16h6M9 8h2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
  books: '<path d="M4 5a2 2 0 0 1 2-2h9v18H6a2 2 0 0 1-2-2V5z" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M15 3h3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-3" fill="none" stroke="currentColor" stroke-width="1.7"/>',
  tools: '<path d="M14.7 6.3a4 4 0 0 1-5.4 5.4L4 17l3 3 5.3-5.3a4 4 0 0 1 5.4-5.4l-3-3-3 3z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/>',
  classroom: '<rect x="3" y="4" width="18" height="13" rx="2" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M8 21h8M12 17v4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
  notebook: '<path d="M6 3h12v18H6z" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M9 8h6M9 12h6M9 16h4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/><path d="M6 3v18" stroke="currentColor" stroke-width="1.7"/>',
  achievements: '<circle cx="12" cy="8" r="5" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M8.5 12.5 7 21l5-2.6L17 21l-1.5-8.5" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>',
  messages: '<rect x="3" y="5" width="18" height="13" rx="2" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M4 6.5 12 13l8-6.5" fill="none" stroke="currentColor" stroke-width="1.7"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="2" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M3 10h18M8 3v4M16 3v4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
  settings: '<circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M19.4 13.5a7.6 7.6 0 0 0 0-3l2-1.5-2-3.4-2.3.9a7.6 7.6 0 0 0-2.6-1.5L14 2h-4l-.5 2.4a7.6 7.6 0 0 0-2.6 1.5l-2.3-.9-2 3.4 2 1.5a7.6 7.6 0 0 0 0 3l-2 1.5 2 3.4 2.3-.9c.8.7 1.6 1.2 2.6 1.5L10 22h4l.5-2.4a7.6 7.6 0 0 0 2.6-1.5l2.3.9 2-3.4-2-1.5z" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>',
  parentControl: '<path d="M12 2 4 5.5V11c0 5 3.4 8.9 8 10 4.6-1.1 8-5 8-10V5.5L12 2z" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M9 12.3l2.1 2.1 4-4.2" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>',
  school: '<path d="M12 3 2 8l10 5 10-5-10-5z" fill="currentColor"/><path d="M6 12.5v4.2C6 18.5 8.7 20 12 20s6-1.5 6-3.3v-4.2" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M21 8v6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>',
  people: '<circle cx="9" cy="8" r="3.2" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M2.8 20c.6-3.6 3.2-5.6 6.2-5.6s5.6 2 6.2 5.6" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><circle cx="17" cy="9" r="2.4" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M15.8 20c.3-2.2 1.4-3.9 3-4.7" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>',
  reports: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>',
};

function icon(name) {
  return `<svg viewBox="0 0 24 24" width="19" height="19" aria-hidden="true">${ICONS[name] || ""}</svg>`;
}

const NAV_CONFIG = {
  student: [
    { label: "Dashboard", href: "/dashboard.html", icon: "dashboard" },
    { label: "My Learning", href: "/dashboard.html#progress", icon: "learning" },
    { label: "AI Tutor", href: "/chatbot.html", icon: "ai" },
    { label: "Exam Papers", href: "/exampapers.html", icon: "exam" },
    { label: "Text Books", href: "/text_books.html", icon: "books" },
    { label: "Smart Tools", href: "/educational_tools.html", icon: "tools" },
    { label: "JMC Classroom", href: "/jmc_Classroom.html", icon: "classroom" },
    { label: "AI Notebook", href: "/notebook.html", icon: "notebook" },
    { label: "Achievements", href: "/dashboard.html#achievements", icon: "achievements" },
    { label: "Messages", href: "/tell_us.html", icon: "messages" },
    { label: "Calendar", href: "/dashboard.html#calendar", icon: "calendar" },
  ],
  teacher: [
    { label: "Dashboard", href: "/dashboard.html", icon: "dashboard" },
    { label: "My Classes", href: "/dashboard.html#classes", icon: "people" },
    { label: "Assignments", href: "/dashboard.html#assignments", icon: "exam" },
    { label: "JMC Classroom", href: "/jmc_Classroom.html", icon: "classroom" },
    { label: "Resources", href: "/educational_tools.html", icon: "tools" },
    { label: "Exam Papers", href: "/exampapers.html", icon: "exam" },
    { label: "Text Books", href: "/text_books.html", icon: "books" },
    { label: "Messages", href: "/tell_us.html", icon: "messages" },
    { label: "Calendar", href: "/dashboard.html#calendar", icon: "calendar" },
  ],
  parent: [
    { label: "Dashboard", href: "/dashboard.html", icon: "dashboard" },
    { label: "Parent Control", href: "/parent-control.html", icon: "parentControl", highlight: true },
    { label: "My Children", href: "/parent-control.html#children", icon: "people" },
    { label: "Reports", href: "/parent-control.html#reports", icon: "reports" },
    { label: "Messages", href: "/tell_us.html", icon: "messages" },
    { label: "Calendar", href: "/dashboard.html#calendar", icon: "calendar" },
  ],
  operator: [
    { label: "Dashboard", href: "/dashboard.html", icon: "dashboard" },
    { label: "School Overview", href: "/dashboard.html#school", icon: "school" },
    { label: "Teachers", href: "/dashboard.html#teachers", icon: "people" },
    { label: "Students", href: "/dashboard.html#students", icon: "people" },
    { label: "Reports", href: "/dashboard.html#reports", icon: "reports" },
    { label: "Announcements", href: "/tell_us.html", icon: "messages" },
    { label: "Calendar", href: "/dashboard.html#calendar", icon: "calendar" },
  ],
};

const ROLE_LABEL = { student: "Student", teacher: "Teacher", parent: "Parent", operator: "School Operator" };

/**
 * Renders the sidebar + topbar shell into #appShellRoot and returns
 * DOM handles the calling page can use (e.g. to inject a title).
 *
 * @param {Object} opts
 * @param {Object} opts.user      Firebase auth user
 * @param {Object} opts.profile   Firestore users/{uid} profile
 * @param {string} opts.active    current page key: 'dashboard' | 'settings' | 'parent-control'
 * @param {string} opts.pageTitle heading shown in the topbar
 */
export function renderShell({ user, profile, active, pageTitle }) {
  const root = document.getElementById("appShellRoot");
  if (!root) return;

  const role = profile.role;
  const items = NAV_CONFIG[role] || NAV_CONFIG.student;
  const avatarUrl = user.photoURL || "";
  const nameSafe = escapeHtml(profile.fullName || user.displayName || "there");

  root.innerHTML = `
    <button class="shell-mobile-toggle" id="shellMobileToggle" aria-label="Open menu" aria-expanded="false">
      <span></span>
    </button>
    <div class="shell-backdrop" id="shellBackdrop"></div>

    <aside class="shell-sidebar" id="shellSidebar">
      <a href="/index.html" class="shell-brand">
        <span class="shell-brand-mark">
          <svg viewBox="0 0 24 24" fill="none"><path d="M12 3L2 8l10 5 8-4.2V15h1.5V8L12 3z" fill="#f2a71b"/><path d="M6 12.2V16c0 1.8 2.7 3.5 6 3.5s6-1.7 6-3.5v-3.8l-6 3.1-6-3.1z" fill="#f2a71b" opacity="0.55"/></svg>
        </span>
        <span class="shell-brand-text"><b>JMC</b> Innovators</span>
      </a>

      <nav class="shell-nav" aria-label="Main">
        ${items.map(it => `
          <a href="${it.href}" class="shell-nav-link ${isActiveHref(it.href, active) ? "active" : ""} ${it.highlight ? "highlight" : ""}">
            ${icon(it.icon)}<span>${escapeHtml(it.label)}</span>
          </a>
        `).join("")}
        <a href="/settings.html" class="shell-nav-link ${active === "settings" ? "active" : ""}">
          ${icon("settings")}<span>Settings</span>
        </a>
      </nav>

      <div class="shell-profile-card">
        <div class="shell-avatar">${avatarUrl ? `<img src="${avatarUrl}" alt="">` : initials(nameSafe)}</div>
        <div class="shell-profile-meta">
          <div class="shell-profile-name">${nameSafe}</div>
          <div class="shell-profile-role">${ROLE_LABEL[role] || "Member"}${profile.grade && role === "student" ? " · " + escapeHtml(profile.grade) : ""}</div>
        </div>
        <button class="shell-logout-btn" id="shellLogoutBtn" title="Sign out" aria-label="Sign out">
          <svg viewBox="0 0 24 24" fill="none"><path d="M15 4H6a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h9" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M10 12h11m0 0l-4-4m4 4l-4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
      </div>
    </aside>

    <div class="shell-main">
      <header class="shell-topbar">
        <button class="shell-mobile-toggle shell-mobile-toggle--inline" id="shellMobileToggleInline" aria-label="Open menu"><span></span></button>
        <h1 class="shell-page-title">${escapeHtml(pageTitle || "")}</h1>
        <div class="shell-topbar-right">
          <button class="shell-icon-btn" id="shellThemeToggle" title="Toggle theme" aria-label="Toggle theme">
            <svg class="icon-sun" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="4.5" stroke="currentColor" stroke-width="1.7"/><path d="M12 2.5v2.4M12 19.1v2.4M4.2 4.2l1.7 1.7M18.1 18.1l1.7 1.7M2.5 12h2.4M19.1 12h2.4M4.2 19.8l1.7-1.7M18.1 5.9l1.7-1.7" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>
            <svg class="icon-moon" viewBox="0 0 24 24" fill="none"><path d="M20 14.5A8.5 8.5 0 1 1 9.5 4a7 7 0 0 0 10.5 10.5z" fill="currentColor"/></svg>
          </button>
          <a href="/settings.html#notifications" class="shell-icon-btn shell-bell" title="Notifications" aria-label="Notifications">
            <svg viewBox="0 0 24 24" fill="none"><path d="M12 3a5.5 5.5 0 0 0-5.5 5.5v2.6c0 .6-.2 1.2-.6 1.7L4.5 15h15l-1.4-2.2a2.8 2.8 0 0 1-.6-1.7V8.5A5.5 5.5 0 0 0 12 3z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M9.5 18a2.5 2.5 0 0 0 5 0" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
            <span class="shell-bell-badge" id="shellBellBadge" hidden>0</span>
          </a>
          <div class="shell-avatar shell-avatar--sm">${avatarUrl ? `<img src="${avatarUrl}" alt="">` : initials(nameSafe)}</div>
        </div>
      </header>
      <main class="shell-content" id="shellContent"></main>
    </div>
  `;

  wireShellInteractions(user.uid);
  return { content: document.getElementById("shellContent") };
}

function isActiveHref(href, active) {
  const base = href.split("#")[0].replace(/^\//, "");
  return base === `${active}.html`;
}

function wireShellInteractions(uid) {
  const sidebar = document.getElementById("shellSidebar");
  const backdrop = document.getElementById("shellBackdrop");
  const openBtns = [document.getElementById("shellMobileToggle"), document.getElementById("shellMobileToggleInline")];
  const closeSidebar = () => { sidebar.classList.remove("open"); backdrop.classList.remove("show"); };
  const openSidebar = () => { sidebar.classList.add("open"); backdrop.classList.add("show"); };
  openBtns.forEach(b => b && b.addEventListener("click", () => {
    sidebar.classList.contains("open") ? closeSidebar() : openSidebar();
  }));
  backdrop.addEventListener("click", closeSidebar);

  document.getElementById("shellLogoutBtn").addEventListener("click", async () => {
    await signOut(auth);
    window.location.href = "/index.html";
  });

  document.getElementById("shellThemeToggle").addEventListener("click", () => {
    const current = getLocalThemePref();
    const resolved = document.documentElement.getAttribute("data-theme");
    const next = (current === "system" ? resolved : current) === "dark" ? "light" : "dark";
    saveThemeToCloud(uid, next);
  });

  // Notification badge, live.
  subscribeUnreadCount(uid, (count) => {
    const badge = document.getElementById("shellBellBadge");
    if (!badge) return;
    if (count > 0) { badge.hidden = false; badge.textContent = count > 9 ? "9+" : String(count); }
    else badge.hidden = true;
  });
}
