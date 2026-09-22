/**
 * notifications.js — realtime notification centre.
 * Stored at notifications/{uid}/items/{id}
 * Categories: assignment | result | announcement | ai | security | parent | system
 */
import {
  db, collection, doc, onSnapshot, query, orderBy, limit,
  updateDoc, deleteDoc, addDoc, serverTimestamp
} from "./firebase-init.js";
import { escapeHtml, formatRelativeTime } from "./ui-utils.js";

const CATEGORY_META = {
  assignment: { label: "Assignment", color: "var(--blue-2)" },
  result: { label: "Results", color: "var(--green)" },
  announcement: { label: "Announcement", color: "var(--gold)" },
  ai: { label: "AI Tutor", color: "var(--purple)" },
  security: { label: "Security", color: "var(--pink)" },
  parent: { label: "Parent", color: "var(--teal)" },
  system: { label: "System", color: "var(--text-2)" },
};

function itemsRef(uid) {
  return collection(db, "notifications", uid, "items");
}

/** Live unread-count subscription, used for the sidebar bell badge. */
export function subscribeUnreadCount(uid, cb) {
  const q = query(itemsRef(uid), orderBy("createdAt", "desc"), limit(50));
  return onSnapshot(q, (snap) => {
    let unread = 0;
    snap.forEach(d => { if (!d.data().read) unread++; });
    cb(unread);
  }, (err) => console.warn("notifications subscribe failed:", err));
}

/** Live subscription for a full notification list UI (settings page notification tab). */
export function subscribeList(uid, cb, max = 30) {
  const q = query(itemsRef(uid), orderBy("createdAt", "desc"), limit(max));
  return onSnapshot(q, (snap) => {
    const rows = [];
    snap.forEach(d => rows.push({ id: d.id, ...d.data() }));
    cb(rows);
  }, (err) => console.warn("notifications list failed:", err));
}

export async function markRead(uid, id) {
  await updateDoc(doc(db, "notifications", uid, "items", id), { read: true });
}

export async function removeNotification(uid, id) {
  await deleteDoc(doc(db, "notifications", uid, "items", id));
}

/** Creates a notification for a user (e.g. a parent notifying a linked child, or the app notifying the user themself). */
export async function pushNotification(uid, { category = "system", title, body = "" }) {
  await addDoc(itemsRef(uid), {
    category, title, body, read: false, createdAt: serverTimestamp(),
  });
}

/** Renders a notification list into a container element. */
export function renderNotificationList(container, rows, { uid }) {
  if (!rows.length) {
    container.innerHTML = `<div class="empty-state">
      <div class="empty-state-icon">🔔</div>
      <p>You're all caught up — no notifications yet.</p>
    </div>`;
    return;
  }
  container.innerHTML = rows.map(n => {
    const meta = CATEGORY_META[n.category] || CATEGORY_META.system;
    return `
    <div class="notif-row ${n.read ? "" : "unread"}" data-id="${n.id}">
      <span class="notif-dot" style="background:${meta.color}"></span>
      <div class="notif-body">
        <div class="notif-top">
          <span class="notif-cat">${meta.label}</span>
          <span class="notif-time">${formatRelativeTime(n.createdAt)}</span>
        </div>
        <div class="notif-title">${escapeHtml(n.title || "")}</div>
        ${n.body ? `<div class="notif-desc">${escapeHtml(n.body)}</div>` : ""}
      </div>
      <button class="notif-del" data-del="${n.id}" aria-label="Delete notification">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
      </button>
    </div>`;
  }).join("");

  container.querySelectorAll(".notif-row").forEach(row => {
    row.addEventListener("click", (e) => {
      if (e.target.closest(".notif-del")) return;
      markRead(uid, row.dataset.id);
    });
  });
  container.querySelectorAll("[data-del]").forEach(btn => {
    btn.addEventListener("click", (e) => {
      e.stopPropagation();
      removeNotification(uid, btn.dataset.del);
    });
  });
}
