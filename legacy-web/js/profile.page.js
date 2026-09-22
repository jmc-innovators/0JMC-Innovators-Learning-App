import { auth, db, doc, setDoc, getDoc, serverTimestamp } from "./firebase-init.js";
import { getCurrentUser, fetchProfile } from "./auth-guard.js";
import { toast, sanitizeText, qs, qsa } from "./ui-utils.js";
import { applyTheme, getLocalThemePref, syncThemeFromCloud } from "./theme.js";
import { logLoginEvent } from "./parent-controls-enforcer.js";

applyTheme(getLocalThemePref());

const COUNTRIES = [
  "Sri Lanka", "India", "United Kingdom", "United States", "Canada", "Australia",
  "United Arab Emirates", "Qatar", "Saudi Arabia", "Singapore", "Malaysia",
  "New Zealand", "South Africa", "Germany", "Other",
];

let currentUser = null;
let currentRole = "";
let editingExisting = false;

async function init() {
  const user = await getCurrentUser();
  if (!user) { window.location.href = "/index.html"; return; }
  currentUser = user;
  await syncThemeFromCloud(user.uid);

  // Populate countries
  const countrySel = qs("#country");
  countrySel.innerHTML = COUNTRIES.map(c => `<option ${c === "Sri Lanka" ? "selected" : ""}>${c}</option>`).join("");

  // Pre-fill if a profile already exists (editing, not first-time).
  const existing = await fetchProfile(user.uid);
  if (existing) {
    editingExisting = true;
    if (existing.role) selectRole(existing.role);
    if (existing.fullName) qs("#fullName").value = existing.fullName;
    if (existing.school) qs("#school").value = existing.school;
    if (existing.grade) qs("#grade").value = existing.grade;
    if (existing.country) countrySel.value = existing.country;
    if (existing.language) qs("#language").value = existing.language;
    qs("#step1Title").textContent = "Your role";
    // Role is locked after first save — it underpins dashboard access and,
    // for students, parental controls. Firestore rules enforce this too;
    // this just gives an honest UI instead of a confusing failed save.
    qsa(".role-card").forEach(c => {
      if (c.dataset.role !== existing.role) { c.style.opacity = "0.4"; c.style.pointerEvents = "none"; }
    });
    const roleNote = document.createElement("p");
    roleNote.className = "muted small";
    roleNote.style.marginTop = "10px";
    roleNote.textContent = "Your role is locked after setup. Contact support if this needs to change.";
    qs("#roleGrid").after(roleNote);
    goToStep(2);
  } else if (user.displayName) {
    qs("#fullName").value = user.displayName;
  }

  qs("#profileLoading").style.display = "none";
  qs("#profileForm").style.display = "block";
}

function selectRole(role) {
  currentRole = role;
  qsa(".role-card").forEach(c => c.classList.toggle("selected", c.dataset.role === role));
  qs("#toStep2Btn").disabled = false;
  const gradeField = qs("#gradeField");
  const gradeInput = qs("#grade");
  if (role === "student") {
    gradeField.style.display = "";
    gradeInput.required = true;
  } else {
    gradeField.style.display = "none";
    gradeInput.required = false;
  }
}

qsa(".role-card").forEach(card => {
  card.addEventListener("click", () => selectRole(card.dataset.role));
});

function goToStep(n) {
  qsa(".profile-step").forEach(s => s.classList.toggle("active", s.dataset.step === String(n)));
  qsa(".profile-step-dot").forEach(d => d.classList.toggle("active", d.dataset.dot === String(n)));
}

qs("#toStep2Btn").addEventListener("click", () => { if (currentRole) goToStep(2); });
qs("#backToStep1Btn").addEventListener("click", () => goToStep(1));

qs("#profileForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  if (!currentRole) { goToStep(1); return; }

  const fullName = sanitizeText(qs("#fullName").value, 80);
  const school = sanitizeText(qs("#school").value, 120);
  const grade = currentRole === "student" ? qs("#grade").value : "";
  const country = qs("#country").value;
  const language = qs("#language").value;

  if (!fullName || !school || (currentRole === "student" && !grade)) {
    toast("Please fill in every field.", "error");
    return;
  }

  const btn = qs("#submitProfileBtn");
  btn.disabled = true; btn.textContent = "Saving…";

  try {
    const payload = {
      uid: currentUser.uid,
      email: currentUser.email || "",
      fullName, school, grade, country, language,
      role: currentRole,
      profileComplete: true,
      updatedAt: serverTimestamp(),
    };
    if (!editingExisting) payload.createdAt = serverTimestamp();

    await setDoc(doc(db, "users", currentUser.uid), payload, { merge: true });

    // Role-specific shard, per the collection structure (students/teachers/parents/operators).
    const shardCollection = { student: "students", teacher: "teachers", parent: "parents", operator: "operators" }[currentRole];
    await setDoc(doc(db, shardCollection, currentUser.uid), {
      uid: currentUser.uid, fullName, school, updatedAt: serverTimestamp(),
      ...(currentRole === "student" ? { grade } : {}),
    }, { merge: true });

    await logLoginEvent(currentUser.uid);

    toast(editingExisting ? "Profile updated!" : "You're all set!", "success");
    setTimeout(() => { window.location.href = "/dashboard.html"; }, 500);
  } catch (err) {
    console.error(err);
    toast("Couldn't save your profile: " + (err.message || "please try again"), "error");
    btn.disabled = false; btn.textContent = "Save & continue";
  }
});

init();
