"use strict";

/* ---------- Outils ---------- */

const contenu = document.getElementById("contenu");
const dialogue = document.getElementById("dialogue");
const navigation = document.getElementById("navigation");

const STATUTS = {
  DEPOSEE: ["Déposée", "info"],
  EN_COURS_EVALUATION: ["En évaluation", "attente"],
  ACCEPTEE: ["Acceptée", "ok"],
  REFUSEE: ["Refusée", "ko"],
  ANNULEE: ["Annulée", ""],
  ACTIVE: ["Active", "ok"],
  CLOTUREE: ["Clôturée", ""],
  PLANIFIE: ["Planifié", "info"],
  EN_COURS: ["En cours", "attente"],
  TERMINE: ["Terminé", "ok"],
  INTERROMPU: ["Interrompu", "ko"],
  EN_ATTENTE_DECISION: ["Attente de décision", "attente"],
  INCIDENT: ["Incident", "ko"],
  REJETEE: ["Rejetée", "ko"],
  ABANDONNEE: ["Abandonnée", ""],
  ACTIF: ["Actif", "ok"],
  SUSPENDU: ["Suspendu", "ko"],
  EN_STAGE: ["En stage", "info"],
};

const ETAPES = {
  CONTROLES: "Contrôles préalables",
  REJET: "Notification de rejet",
  TRANSMISSION: "Enregistrement et transmission",
  COMPLEMENT: "Demande de complément",
  CLOTURE: "Clôture de la candidature",
};

const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

const tampon = (s) => {
  const [libelle, classe] = STATUTS[s] || [s, ""];
  return `<span class="tampon ${classe}">${esc(libelle)}</span>`;
};

const DATE_SEULE = /^\d{4}-\d{2}-\d{2}$/;
const FORMAT_DATE = new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium", timeZone: "UTC" });
const FORMAT_DATE_LOCALE = new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" });
const FORMAT_DATE_HEURE = new Intl.DateTimeFormat("fr-FR", { dateStyle: "short", timeStyle: "short" });

/** Une date sans heure reste le même jour, quel que soit le fuseau du navigateur. */
const date = (v) => {
  if (!v) return "—";
  return DATE_SEULE.test(v) ? FORMAT_DATE.format(new Date(v)) : FORMAT_DATE_LOCALE.format(new Date(v));
};
const dateHeure = (v) => (v ? FORMAT_DATE_HEURE.format(new Date(v)) : "—");

const pluriel = (n, mot, motPluriel = mot + "s") => `${n} ${n > 1 ? motPluriel : mot}`;

function joursRestants(dateLimite) {
  const maintenant = new Date();
  const aujourdhui = Date.UTC(maintenant.getFullYear(), maintenant.getMonth(), maintenant.getDate());
  return Math.round((Date.parse(dateLimite) - aujourdhui) / 864e5);
}

function echeance(dateLimite) {
  const n = joursRestants(dateLimite);
  if (n < 0) return `<span class="echeance">Date passée</span>`;
  const texte = n === 0 ? "Dernier jour" : `J-${n}`;
  const detail = n === 0 ? "Les candidatures ferment aujourd'hui" : `Plus que ${pluriel(n, "jour")} pour candidater`;
  return `<span class="echeance ${n <= 14 ? "proche" : ""}" title="${esc(detail)}"><span aria-hidden="true">${texte}</span><span hidden>${esc(detail)}</span></span>`;
}

class SessionExpiree extends Error {}

async function api(chemin, options = {}) {
  const reponse = await fetch("/api" + chemin, {
    method: options.methode || "GET",
    headers: { "Content-Type": "application/json" },
    body: options.corps === undefined ? undefined : JSON.stringify(options.corps),
  });
  const texte = await reponse.text();
  let donnees = null;
  try { donnees = texte ? JSON.parse(texte) : null; } catch { donnees = null; }
  if (reponse.status === 401 && !options.connexion) {
    // Session expirée : retour à la page de connexion.
    etat.moi = null;
    throw new SessionExpiree();
  }
  if (!reponse.ok) {
    throw new Error(donnees?.message || `Erreur ${reponse.status}. Réessayez dans un instant.`);
  }
  return donnees;
}

/** Un dialogue ouvert passe au-dessus de tout : le message s'affiche alors à l'intérieur. */
function annoncer(message, type = "ok", duree = 6000) {
  const el = document.createElement("div");
  el.className = `annonce ${type}`;
  el.textContent = message;
  const dansDialogue = dialogue.open && dialogue.querySelector(".dialogue-corps");
  if (dansDialogue) {
    el.setAttribute("role", "alert");
    dansDialogue.prepend(el);
  } else {
    document.getElementById("annonces").append(el);
  }
  setTimeout(() => el.remove(), duree);
}

/** Exécute une action, annonce l'erreur éventuelle, puis redessine la page. */
async function agir(action) {
  try {
    await action();
  } catch (e) {
    if (!(e instanceof SessionExpiree)) annoncer(e.message, "ko", 9000);
  }
  await dessiner();
}

/** Bouton en cours d'envoi : renvoie la fonction qui rétablit son état. */
function occuper(bouton, libelle) {
  const initial = bouton.textContent;
  bouton.disabled = true;
  bouton.setAttribute("aria-busy", "true");
  bouton.textContent = libelle;
  return () => {
    bouton.disabled = false;
    bouton.removeAttribute("aria-busy");
    bouton.textContent = initial;
  };
}

/** Une partie de page qui échoue ne doit pas emporter le reste : renvoie null si son service ne répond pas. */
const tenter = (promesse) => promesse.catch((e) => {
  if (e instanceof SessionExpiree) throw e;
  return null;
});
const INDISPONIBLE = `<p class="vide">Cette partie est momentanément indisponible. Elle reviendra dès que son service répondra.</p>`;
const compteOuTiret = (liste) => (liste ? liste.length : "–");

function section(id, titre, compte, corps, actions = "", classe = "") {
  const c = compte === null ? "" : `<span class="compte">${compte}</span>`;
  const a = actions ? `<div class="section-actions">${actions}</div>` : "";
  return `<section class="section ${classe}" id="${id}" aria-labelledby="titre-${id}">
    <header><h2 class="onglet" id="titre-${id}">${esc(titre)}${c}</h2>${a}</header>
    <div class="feuille">${corps}</div>
  </section>`;
}

function tableau(colonnes, lignes, messageVide) {
  if (!lignes.length) return `<p class="vide">${esc(messageVide)}</p>`;
  const tete = colonnes.map((c) => c
    ? `<th scope="col">${esc(c)}</th>`
    : `<th scope="col"><span hidden>Actions</span></th>`).join("");
  const corps = lignes.map((l) => `<tr>${l.map((v) => `<td>${v}</td>`).join("")}</tr>`).join("");
  return `<div class="tableau" tabindex="0" role="region" aria-label="Tableau défilant"><table><thead><tr>${tete}</tr></thead><tbody>${corps}</tbody></table></div>`;
}

function filNotifications(notifications, messageVide) {
  if (!notifications.length) return `<p class="vide">${esc(messageVide)}</p>`;
  const limite = 15;
  const lignes = notifications.slice(0, limite).map((n) => `<li>
    <time datetime="${esc(n.dateEnvoi)}">${dateHeure(n.dateEnvoi)}</time>
    <div><strong>${esc(n.sujet)}</strong><p>${esc(n.message)}</p></div>
  </li>`).join("");
  const suite = notifications.length > limite ? `Les ${limite} plus récentes sont affichées. ` : "";
  return `<ul class="fil">${lignes}</ul><p class="note-pied">${suite}Envoi simulé pour la démonstration : aucun e-mail réel ne part.</p>`;
}

/** Dépôt, évaluation, puis décision : la position se lit d'un coup d'œil. */
function parcours(statut) {
  const position = { DEPOSEE: 0, EN_COURS_EVALUATION: 1 }[statut] ?? 2;
  const finale = { ACCEPTEE: "Acceptée", REFUSEE: "Refusée", ANNULEE: "Annulée" }[statut] || "Décision";
  const classeFinale = statut === "ACCEPTEE" ? "positif" : ["REFUSEE", "ANNULEE"].includes(statut) ? "negatif" : "";
  const noms = ["Dépôt", "Évaluation", finale];
  const lignes = noms.map((nom, i) => {
    let classe = "";
    if (i < position) classe = "fait";
    else if (i === position) classe = i === 2 ? classeFinale : "actuel";
    return `<li class="${classe}" ${i === position ? 'aria-current="step"' : ""}>${esc(nom)}</li>`;
  }).join("");
  return `<ol class="parcours" aria-label="Avancement de la candidature">${lignes}</ol>`;
}

function ouvrirDialogue(titre, corps, classe = "") {
  dialogue.className = classe;
  dialogue.innerHTML = `<div class="dialogue-tete"><h2>${esc(titre)}</h2>
    <button type="button" data-fermer>Fermer</button></div>
    <div class="dialogue-corps">${corps}</div>`;
  dialogue.querySelector("[data-fermer]").onclick = () => dialogue.close();
  if (!dialogue.open) dialogue.showModal();
}

/** Confirmation d'une action destructrice : le bouton nomme ce qui va se passer. */
function confirmer(titre, message, libelleAction) {
  return new Promise((resoudre) => {
    ouvrirDialogue(titre, `<p>${esc(message)}</p>
      <div class="dialogue-pied">
        <button type="button" id="confirmer-non">Conserver</button>
        <button type="button" class="danger" id="confirmer-oui">${esc(libelleAction)}</button>
      </div>`, "etroit");
    let reponse = false;
    dialogue.querySelector("#confirmer-non").onclick = () => dialogue.close();
    dialogue.querySelector("#confirmer-oui").onclick = () => { reponse = true; dialogue.close(); };
    dialogue.addEventListener("close", () => resoudre(reponse), { once: true });
    dialogue.querySelector("#confirmer-non").focus();
  });
}

/* ---------- État et accès ---------- */

const etat = { moi: null, recherche: "", profilSeul: false };
const LIBELLES_ROLES = { ETUDIANT: "Étudiant", ENTREPRISE: "Entreprise", RESPONSABLE: "Responsable" };
const LOGO = `<svg viewBox="0 0 32 26" width="32" height="26" aria-hidden="true"><path d="M1 4a3 3 0 0 1 3-3h7l3 4h14a3 3 0 0 1 3 3v14a3 3 0 0 1-3 3H4a3 3 0 0 1-3-3z" fill="none" stroke="currentColor" stroke-width="2"/><path d="m10 15 4 4 8-9" fill="none" stroke="currentColor" stroke-width="2.4"/></svg>`;

/** Écran partagé : le récit explique le parcours réel, le volet porte le formulaire. */
function ecranAcces(volet) {
  document.body.classList.add("mode-acces");
  document.getElementById("entete").hidden = true;
  document.title = "Plateforme de stages";
  if (dialogue.open) dialogue.close();
  contenu.innerHTML = `<div class="acces">
    <aside class="recit">
      <span class="marque">${LOGO}<strong>Plateforme de stages</strong></span>
      <h1>Du dépôt à la décision, un seul dossier.</h1>
      <ol class="recit-etapes">
        <li><div><strong>Vous déposez</strong><span>L'étudiant candidate à une offre, avec sa lettre.</span></div></li>
        <li><div><strong>Tout est contrôlé</strong><span>Statut, date limite, places, doublon et profil sont vérifiés.</span></div></li>
        <li><div><strong>L'entreprise décide</strong><span>Accepter, refuser ou demander un complément.</span></div></li>
        <li><div><strong>Le stage est suivi</strong><span>Dossier, comptes rendus et évaluations au même endroit.</span></div></li>
      </ol>
    </aside>
    <div class="volet">${volet}</div>
  </div>`;
}

function champMotDePasse(nom, libelle, autocomplete, extra = "") {
  return `<label class="champ" for="${nom}">${esc(libelle)}</label>
    <div class="mot-de-passe">
      <input id="${nom}" name="${nom}" type="password" autocomplete="${autocomplete}" required ${extra}>
      <button type="button" data-voir="${nom}" aria-pressed="false" aria-label="Afficher le mot de passe">Afficher</button>
    </div>`;
}

function brancherVoirMotDePasse() {
  contenu.querySelectorAll("[data-voir]").forEach((b) => {
    b.onclick = () => {
      const champ = document.getElementById(b.dataset.voir);
      const visible = champ.type === "password";
      champ.type = visible ? "text" : "password";
      b.textContent = visible ? "Masquer" : "Afficher";
      b.setAttribute("aria-pressed", String(visible));
      b.setAttribute("aria-label", visible ? "Masquer le mot de passe" : "Afficher le mot de passe");
    };
  });
}

/** Le premier champ en erreur reçoit le message et le focus. */
function verifierFormulaire(form, zoneErreur) {
  const champ = form.querySelector(":invalid");
  if (!champ) return true;
  const libelle = form.querySelector(`label[for="${champ.id}"]`)?.textContent
    || champ.closest("label")?.firstChild?.textContent || "Ce champ";
  zoneErreur.textContent = `${libelle.trim()} : ${champ.validationMessage}`;
  // L'erreur s'affiche juste sous le champ, y compris dans un long formulaire.
  (champ.closest(".deux-colonnes") || champ.closest(".mot-de-passe") || champ.closest(".champ")).after(zoneErreur);
  champ.focus();
  return false;
}

function pageConnexion(message = "") {
  ecranAcces(`<section class="formulaire-acces" aria-labelledby="titre-connexion">
    <h2 id="titre-connexion">Connexion</h2>
    <p class="aide">Étudiant : votre matricule. Entreprise : votre e-mail de contact.</p>
    <form id="f-connexion" novalidate>
      <label class="champ">Identifiant
        <input name="identifiant" autocomplete="username" spellcheck="false" autocapitalize="none" required placeholder="INE-2026-001…"></label>
      ${champMotDePasse("motDePasse", "Mot de passe", "current-password")}
      <p class="erreur" id="erreur-connexion" role="alert">${esc(message)}</p>
      <button type="submit" class="principal">Se connecter</button>
    </form>
    <p class="bascule">Pas encore de compte ? <button type="button" class="lien" id="vers-inscription">Créer un compte</button></p>
  </section>`);
  document.getElementById("vers-inscription").onclick = () => pageInscription();
  brancherVoirMotDePasse();
  const form = document.getElementById("f-connexion");
  if (matchMedia("(pointer: fine)").matches) form.identifiant.focus();
  form.onsubmit = async (ev) => {
    ev.preventDefault();
    const erreur = document.getElementById("erreur-connexion");
    erreur.textContent = "";
    if (!verifierFormulaire(form, erreur)) return;
    form.querySelector("button[type=submit]").before(erreur);
    const retablir = occuper(form.querySelector("button[type=submit]"), "Connexion…");
    try {
      etat.moi = await api("/auth/connexion", { methode: "POST", corps: Object.fromEntries(new FormData(form)), connexion: true });
      await dessiner();
    } catch (e) {
      erreur.textContent = e.message;
      form.motDePasse.value = "";
      form.motDePasse.focus();
      retablir();
    }
  };
}

const FILIERES = ["Innovation et AMOA", "Ingénierie Logicielle", "Cybersécurité", "Data Science"];
const NIVEAUX = ["INE 1", "INE 2", "INE 3"];
const options = (valeurs) => valeurs.map((v) => `<option value="${esc(v)}">${esc(v)}</option>`).join("");

const CHAMPS_INSCRIPTION = {
  ETUDIANT: `
    <label class="champ">Matricule, votre identifiant de connexion
      <input name="matricule" required maxlength="30" autocomplete="username" spellcheck="false" autocapitalize="none" placeholder="INE-2026-010…"></label>
    <label class="champ">Nom complet <input name="nom" required maxlength="120" autocomplete="name" placeholder="Salma Idrissi…"></label>
    <div class="deux-colonnes">
      <label class="champ">Filière <select name="filiere" autocomplete="off">${options(FILIERES)}</select></label>
      <label class="champ">Niveau d'études <select name="niveauEtudes" autocomplete="off">${options(NIVEAUX)}</select></label>
    </div>
    <div class="deux-colonnes">
      <label class="champ">E-mail <input name="email" type="email" required maxlength="160" autocomplete="email" spellcheck="false" placeholder="prenom@exemple.ma…"></label>
      <label class="champ">Téléphone, facultatif <input name="telephone" type="tel" maxlength="20" autocomplete="tel" placeholder="+212 6…"></label>
    </div>`,
  ENTREPRISE: `
    <label class="champ">Nom de l'entreprise <input name="nom" required maxlength="160" autocomplete="organization" placeholder="Atlas Conseil…"></label>
    <div class="deux-colonnes">
      <label class="champ">Secteur d'activité <input name="secteurActivite" required maxlength="160" autocomplete="off" placeholder="Services IT…"></label>
      <label class="champ">Responsable à contacter <input name="responsableContact" required maxlength="120" autocomplete="off" placeholder="Nom ou fonction…"></label>
    </div>
    <label class="champ">E-mail de contact, votre identifiant de connexion
      <input name="email" type="email" required maxlength="160" autocomplete="username" spellcheck="false" placeholder="rh@exemple.ma…"></label>
    <label class="champ">Téléphone, facultatif <input name="telephone" type="tel" maxlength="20" autocomplete="tel" placeholder="+212 5…"></label>`,
};

function pageInscription(role = "ETUDIANT") {
  ecranAcces(`<section class="formulaire-acces" aria-labelledby="titre-inscription">
    <h2 id="titre-inscription">Créer un compte</h2>
    <div class="choix-role" role="group" aria-label="Type de compte">
      <button type="button" data-type="ETUDIANT" aria-pressed="${role === "ETUDIANT"}">Étudiant</button>
      <button type="button" data-type="ENTREPRISE" aria-pressed="${role === "ENTREPRISE"}">Entreprise</button>
    </div>
    <form id="f-inscription" novalidate>
      ${CHAMPS_INSCRIPTION[role]}
      ${champMotDePasse("motDePasse", "Mot de passe, 8 caractères minimum", "new-password", 'minlength="8"')}
      ${champMotDePasse("confirmation", "Confirmer le mot de passe", "new-password")}
      <p class="erreur" id="erreur-inscription" role="alert"></p>
      <button type="submit" class="principal">Créer mon compte</button>
    </form>
    <p class="bascule">Déjà un compte ? <button type="button" class="lien" id="vers-connexion">Se connecter</button></p>
  </section>`);
  contenu.querySelectorAll("[data-type]").forEach((b) => {
    b.onclick = () => {
      pageInscription(b.dataset.type);
      contenu.querySelector(`[data-type="${b.dataset.type}"]`).focus();
    };
  });
  document.getElementById("vers-connexion").onclick = () => pageConnexion();
  brancherVoirMotDePasse();
  const form = document.getElementById("f-inscription");
  form.onsubmit = async (ev) => {
    ev.preventDefault();
    const erreur = document.getElementById("erreur-inscription");
    erreur.textContent = "";
    if (!verifierFormulaire(form, erreur)) return;
    const { confirmation, ...donnees } = Object.fromEntries(new FormData(form));
    if (donnees.motDePasse !== confirmation) {
      erreur.textContent = "Les deux mots de passe sont différents : saisissez-les de nouveau.";
      form.confirmation.closest(".mot-de-passe").after(erreur);
      form.confirmation.focus();
      return;
    }
    form.querySelector("button[type=submit]").before(erreur);
    const retablir = occuper(form.querySelector("button[type=submit]"), "Création du compte…");
    try {
      etat.moi = await api("/auth/inscription", { methode: "POST", corps: { ...donnees, role }, connexion: true });
      await dessiner();
      annoncer("Compte créé. Bienvenue !");
    } catch (e) {
      erreur.textContent = e.message;
      retablir();
    }
  };
}

/** Affiche l'espace du compte connecté : son rôle seul en décide. */
async function dessiner() {
  if (!etat.moi) {
    pageConnexion();
    return;
  }
  const arrivee = document.body.classList.contains("mode-acces") || !contenu.firstElementChild;
  document.body.classList.remove("mode-acces");
  document.getElementById("entete").hidden = false;
  document.getElementById("nom-utilisateur").textContent = etat.moi.nom;
  document.getElementById("role-utilisateur").textContent = LIBELLES_ROLES[etat.moi.role] || etat.moi.role;
  if (arrivee) contenu.innerHTML = `<p class="chargement">Chargement de votre espace…</p>`;
  try {
    if (etat.moi.role === "ETUDIANT") await vueEtudiant();
    else if (etat.moi.role === "ENTREPRISE") await vueEntreprise();
    else await vueResponsable();
    if (arrivee) document.getElementById(location.hash.slice(1))?.scrollIntoView();
  } catch (e) {
    if (e instanceof SessionExpiree) {
      pageConnexion("Votre session a expiré. Reconnectez-vous.");
      return;
    }
    navigation.innerHTML = "";
    contenu.innerHTML = section("erreur", "Service indisponible", null,
      `<p class="vide">${esc(e.message)} Vérifiez que les services sont démarrés, puis réessayez.</p>`,
      `<button type="button" id="reessayer">Réessayer</button>`, "alerte");
    document.getElementById("reessayer").onclick = dessiner;
  }
}

function definirNavigation(liens) {
  navigation.innerHTML = liens.map(([id, nom]) => `<a href="#${id}">${esc(nom)}</a>`).join("");
}

/* ---------- Espace étudiant ---------- */

/** Aperçu du contrôle fait par le service Offres, qui reste seul juge au moment du dépôt. */
function profilCompatible(offre, moi) {
  const filieres = (offre.filieresCibles || "").split(",").map((f) => f.trim().toLowerCase()).filter(Boolean);
  if (filieres.length && !filieres.includes(moi.filiere.toLowerCase())) return false;
  const annee = Number((moi.niveauEtudes.match(/\d+/) || [0])[0]);
  return !offre.niveauMinimum || annee >= Number(offre.niveauMinimum);
}

function ficheOffre(o, moi, deposee) {
  const compatible = profilCompatible(o, moi);
  const competences = (o.competencesRequises || "").split(",").map((c) => c.trim()).filter(Boolean);
  const action = deposee
    ? tampon(deposee.statut)
    : `<button type="button" class="principal" data-candidater="${esc(o.idOffre)}" aria-label="Candidater à l'offre ${esc(o.titre)}">Candidater</button>`;
  return `<article class="fiche">
    <div class="fiche-tete"><span class="fiche-entreprise">${esc(o.nomEntreprise)}</span>${echeance(o.dateLimite)}</div>
    <h3>${esc(o.titre)}</h3>
    <p class="fiche-texte">${esc(o.description)}</p>
    <ul class="etiquettes" aria-label="Compétences requises">${competences.map((c) => `<li>${esc(c)}</li>`).join("")}</ul>
    <div class="faits"><span>${pluriel(Number(o.dureeMois), "mois", "mois")}</span><span>${pluriel(Number(o.placesDisponibles), "place")}</span><span>Jusqu'au ${date(o.dateLimite)}</span></div>
    <div class="fiche-pied">
      <span class="profil ${compatible ? "" : "hors"}">${compatible ? "Votre profil correspond" : "Hors de votre profil"}</span>
      ${action}
    </div>
  </article>`;
}

function dessinerFiches(ouvertes, moi, deposees, matricule) {
  const conteneur = document.getElementById("liste-offres");
  const texte = etat.recherche.trim().toLowerCase();
  const visibles = ouvertes.filter((o) => {
    if (etat.profilSeul && !profilCompatible(o, moi)) return false;
    return !texte || `${o.titre} ${o.nomEntreprise} ${o.competencesRequises} ${o.description}`.toLowerCase().includes(texte);
  });
  conteneur.innerHTML = visibles.length
    ? `<div class="fiches">${visibles.map((o) => ficheOffre(o, moi, deposees.get(String(o.idOffre)))).join("")}</div>`
    : `<p class="vide">Aucune offre ne correspond à ces critères. <button type="button" class="lien" id="effacer-filtres">Effacer les filtres</button></p>`;
  document.getElementById("effacer-filtres")?.addEventListener("click", () => {
    etat.recherche = "";
    etat.profilSeul = false;
    document.getElementById("recherche-offres").value = "";
    document.getElementById("filtre-profil").setAttribute("aria-pressed", "false");
    dessinerFiches(ouvertes, moi, deposees, matricule);
  });
  conteneur.querySelectorAll("[data-candidater]").forEach((b) => {
    b.onclick = () => formulaireCandidature(matricule, ouvertes.find((o) => o.idOffre === b.dataset.candidater));
  });
}

async function vueEtudiant() {
  const matricule = etat.moi.matricule;
  const moi = await api("/etudiants/" + encodeURIComponent(matricule));
  const [offresLues, candidaturesLues, notifications, stagesLus] = await Promise.all([
    tenter(api("/offres")),
    tenter(api("/candidatures?idEtudiant=" + moi.idEtudiant)),
    tenter(api(`/notifications?destinataireType=ETUDIANT&destinataireId=${moi.idEtudiant}`)),
    tenter(api("/stages?idEtudiant=" + moi.idEtudiant)),
  ]);
  const offres = offresLues ?? [];
  const candidatures = candidaturesLues ?? [];
  const stages = stagesLus ?? [];
  const ouvertes = offres.filter((o) => o.statut === "ACTIVE" && Number(o.placesDisponibles) > 0 && joursRestants(o.dateLimite) >= 0);
  const deposees = new Map(candidatures.filter((c) => c.statut !== "ANNULEE").map((c) => [String(c.idOffre), c]));
  const enCours = candidatures.filter((c) => ["DEPOSEE", "EN_COURS_EVALUATION"].includes(c.statut)).length;
  const stageActif = stages.find((s) => ["PLANIFIE", "EN_COURS"].includes(s.statut));
  const chapo = stageActif
    ? `Votre stage « ${esc(stageActif.titreOffre)} » chez ${esc(stageActif.nomEntreprise)} débute le ${date(stageActif.dateDebut)}.`
    : enCours
      ? `${pluriel(enCours, "candidature")} en cours d'examen, et ${pluriel(ouvertes.length, "offre ouverte", "offres ouvertes")}.`
      : `${pluriel(ouvertes.length, "offre ouverte", "offres ouvertes")} : choisissez celle qui vous correspond.`;

  const lignes = candidatures.map((c) => `<article class="dossier-ligne">
    <div><h3>${esc(c.titreOffre)}</h3><div class="secondaire">${esc(c.nomEntreprise)}</div></div>
    ${parcours(c.statut)}
    <div class="dossier-fin">
      <span class="secondaire">Déposée le ${dateHeure(c.dateDepot)}</span>
      ${["DEPOSEE", "EN_COURS_EVALUATION"].includes(c.statut)
        ? `<button type="button" class="danger" data-annuler="${esc(c.idCandidature)}">Annuler</button>` : ""}
    </div>
    ${c.motif ? `<p class="motif"><strong>Motif :</strong> ${esc(c.motif)}</p>` : ""}
  </article>`).join("");

  const lignesStages = stages.map((s) => [
    `${esc(s.titreOffre)}<div class="secondaire">${esc(s.nomEntreprise)}</div>`,
    tampon(s.statut),
    `<span class="nombre">${date(s.dateDebut)} au ${date(s.dateFin)}</span>`,
    `<div class="actions"><button type="button" data-suivi="${esc(s.idStage)}">Ouvrir le suivi</button></div>`,
  ]);

  definirNavigation([["offres", "Offres"], ["candidatures", "Candidatures"], ["stages", "Stages"], ["notifications", "Notifications"]]);
  document.title = "Mon espace · Plateforme de stages";
  const recherche = esc(etat.recherche);
  contenu.innerHTML = `<div class="ouverture" id="haut">
      <div><h1>Bonjour, ${esc(moi.nom.split(" ")[0])}.</h1><p class="chapo">${chapo}</p></div>
      <div class="carte-identite">
        <header><span class="code" translate="no">${esc(moi.matricule)}</span>${tampon(moi.statut)}</header>
        <dl>
          <dt>Filière</dt><dd>${esc(moi.filiere)}</dd>
          <dt>Niveau</dt><dd>${esc(moi.niveauEtudes)}</dd>
          <dt>E-mail</dt><dd translate="no">${esc(moi.email)}</dd>
        </dl>
      </div>
    </div>` +
    section("offres", "Offres ouvertes", offresLues ? ouvertes.length : "–", `<div class="filtres">
        <label class="champ">Rechercher une offre
          <input type="search" id="recherche-offres" name="recherche" autocomplete="off" placeholder="Titre, entreprise, compétence…" value="${recherche}"></label>
        <button type="button" class="bascule-filtre" id="filtre-profil" aria-pressed="${etat.profilSeul}">Mon profil uniquement</button>
      </div><div id="liste-offres"></div>`) +
    section("candidatures", "Mes candidatures", compteOuTiret(candidaturesLues), !candidaturesLues ? INDISPONIBLE :
      lignes || `<p class="vide">Vous n'avez encore déposé aucune candidature : commencez par une offre ci-dessus.</p>`) +
    section("stages", "Mes stages", compteOuTiret(stagesLus), !stagesLus ? INDISPONIBLE :
      tableau(["Stage", "Statut", "Dates", ""], lignesStages, "Votre stage apparaîtra ici dès qu'une candidature sera acceptée.")) +
    section("notifications", "Mes notifications", compteOuTiret(notifications), !notifications ? INDISPONIBLE :
      filNotifications(notifications, "Aucune notification pour le moment."));

  if (offresLues) dessinerFiches(ouvertes, moi, deposees, matricule);
  else document.getElementById("liste-offres").innerHTML = INDISPONIBLE;
  document.getElementById("recherche-offres").oninput = (ev) => {
    etat.recherche = ev.target.value;
    dessinerFiches(ouvertes, moi, deposees, matricule);
  };
  document.getElementById("filtre-profil").onclick = (ev) => {
    etat.profilSeul = !etat.profilSeul;
    ev.currentTarget.setAttribute("aria-pressed", String(etat.profilSeul));
    dessinerFiches(ouvertes, moi, deposees, matricule);
  };
  contenu.querySelectorAll("[data-annuler]").forEach((b) => {
    b.onclick = async () => {
      if (await confirmer("Annuler cette candidature ?", "L'entreprise en sera informée. Vous pourrez candidater de nouveau tant que l'offre reste ouverte.", "Annuler la candidature")) {
        agir(async () => {
          await api("/candidatures/" + b.dataset.annuler, { methode: "DELETE" });
          annoncer("Candidature annulée.");
        });
      }
    };
  });
  brancherSuivi(stages, false);
}

function formulaireCandidature(matricule, offre) {
  ouvrirDialogue("Candidater : " + offre.titre, `<form id="f-candidature" class="formulaire">
    <label class="champ pleine">Lettre de motivation, facultative
      <textarea name="lettre" maxlength="5000" autocomplete="off" placeholder="Expliquez ce qui vous attire dans ce stage…"></textarea></label>
    <div class="dialogue-pied pleine"><button type="submit" class="principal">Déposer ma candidature</button></div>
  </form>`);
  document.getElementById("f-candidature").onsubmit = async (ev) => {
    ev.preventDefault();
    const retablir = occuper(ev.target.querySelector("button[type=submit]"), "Dépôt en cours…");
    const lettre = new FormData(ev.target).get("lettre");
    await agir(async () => {
      const resultat = await api("/processus/candidatures", {
        methode: "POST",
        corps: { matricule, idOffre: offre.idOffre, lettreMotivation: lettre },
      });
      dialogue.close();
      if (resultat.etat === "EN_ATTENTE_DECISION") {
        annoncer("Candidature enregistrée et transmise à l'entreprise.");
      } else if (resultat.etat === "REJETEE") {
        annoncer("Candidature rejetée : " + resultat.motifsRejet, "ko", 12000);
      } else {
        annoncer("Votre candidature n'a pas pu être traitée immédiatement : un responsable a été alerté.", "attente", 12000);
      }
    });
    if (dialogue.open) retablir();
  };
}

/* ---------- Espace entreprise ---------- */

async function vueEntreprise() {
  const id = etat.moi.idEntreprise;
  const [taches, offresLues, candidaturesLues, notifications] = await Promise.all([
    api("/processus/taches?idEntreprise=" + id),
    tenter(api(`/entreprises/${id}/offres`)),
    tenter(api("/candidatures?idEntreprise=" + id)),
    tenter(api(`/notifications?destinataireType=ENTREPRISE&destinataireId=${id}`)),
  ]);
  const offres = offresLues ?? [];
  const candidatures = candidaturesLues ?? [];

  const dossiers = taches.map((t) => `<article class="dossier">
    <div class="dossier-gauche">
      <h3>${esc(t.nomEtudiant)}</h3>
      <div class="faits"><span class="code" translate="no">${esc(t.matricule)}</span><span>${esc(t.filiere)}</span><span>${esc(t.niveauEtudes)}</span></div>
      <div class="faits"><span>Pour : <strong>${esc(t.titreOffre)}</strong></span><span>Reçu le ${dateHeure(t.creeeLe)}</span></div>
      ${t.lettreMotivation ? `<div class="lettre">${esc(t.lettreMotivation)}</div>` : `<p class="secondaire">Aucune lettre de motivation.</p>`}
    </div>
    <div class="dossier-droite">
      <div class="champs-decision">
        <label class="champ">Motif ou précision, transmis à l'étudiant
          <input type="text" name="motif" data-motif="${esc(t.idTache)}" maxlength="500" autocomplete="off" placeholder="Merci d'envoyer votre CV…"></label>
        <label class="champ">Début du stage, si accepté
          <input type="date" name="debut" data-debut="${esc(t.idTache)}" autocomplete="off"></label>
      </div>
      <div class="actions">
        <button type="button" class="principal" data-decision="ACCEPTEE" data-tache="${esc(t.idTache)}">Accepter</button>
        <button type="button" data-decision="COMPLEMENT" data-tache="${esc(t.idTache)}">Demander un complément</button>
        <button type="button" class="danger" data-decision="REFUSEE" data-tache="${esc(t.idTache)}">Refuser</button>
      </div>
    </div>
  </article>`).join("");

  const lignesOffres = offres.map((o) => [
    `${esc(o.titre)}<div class="secondaire">${esc(o.competencesRequises)}</div>`,
    tampon(o.statut),
    `<span class="nombre">${esc(o.placesDisponibles)}</span>`,
    `<span class="nombre">${date(o.dateLimite)}</span><div>${o.statut === "ACTIVE" ? echeance(o.dateLimite) : ""}</div>`,
    `<div class="actions"><button type="button" data-modifier="${esc(o.idOffre)}">Modifier</button>
      <button type="button" class="danger" data-supprimer="${esc(o.idOffre)}">Supprimer</button></div>`,
  ]);

  const lignesCandidatures = candidatures.map((c) => [
    `${esc(c.nomEtudiant)}<div class="secondaire code" translate="no">${esc(c.matricule)}</div>`,
    esc(c.titreOffre),
    tampon(c.statut) + (c.motif ? `<div class="secondaire">${esc(c.motif)}</div>` : ""),
    `<span class="secondaire">${dateHeure(c.dateDepot)}</span>`,
  ]);

  definirNavigation([["dossiers", "Dossiers"], ["offres", "Offres"], ["recues", "Candidatures"], ["notifications", "Notifications"]]);
  document.title = `${etat.moi.nom} · Plateforme de stages`;
  contenu.innerHTML = `<div class="ouverture" id="haut">
      <div><h1>${esc(etat.moi.nom)}</h1><p class="chapo">Traitez vos dossiers, publiez vos offres et suivez les candidatures reçues.</p></div>
      <div class="grand-chiffre ${taches.length ? "a-traiter" : ""}"><b>${taches.length}</b>
        <span>${taches.length > 1 ? "dossiers attendent votre décision" : taches.length === 1 ? "dossier attend votre décision" : "dossier en attente de décision"}</span></div>
    </div>` +
    section("dossiers", "Dossiers à traiter", taches.length, dossiers || `<p class="vide">Tout est à jour : aucun dossier n'attend votre décision.</p>`) +
    section("offres", "Mes offres", compteOuTiret(offresLues), !offresLues ? INDISPONIBLE :
      tableau(["Offre", "Statut", "Places", "Date limite", ""], lignesOffres, "Publiez une première offre pour recevoir des candidatures."),
      `<button type="button" class="principal" id="publier">Publier une offre</button>`) +
    section("recues", "Candidatures reçues", compteOuTiret(candidaturesLues), !candidaturesLues ? INDISPONIBLE :
      tableau(["Étudiant", "Offre", "Statut", "Dépôt"], lignesCandidatures, "Aucune candidature reçue pour le moment.")) +
    section("notifications", "Notifications", compteOuTiret(notifications), !notifications ? INDISPONIBLE :
      filNotifications(notifications, "Aucune notification pour le moment."));

  contenu.querySelectorAll("[data-decision]").forEach((b) => {
    b.onclick = async () => {
      const tache = b.dataset.tache;
      const champMotif = contenu.querySelector(`[data-motif="${tache}"]`);
      const motif = champMotif.value;
      const dateDebut = contenu.querySelector(`[data-debut="${tache}"]`).value;
      if (b.dataset.decision === "COMPLEMENT" && !motif.trim()) {
        annoncer("Précisez le complément demandé dans le champ « Motif ».", "attente");
        champMotif.focus();
        return;
      }
      if (b.dataset.decision === "REFUSEE" && !await confirmer("Refuser cette candidature ?", "La décision est définitive et sera transmise à l'étudiant.", "Refuser la candidature")) {
        return;
      }
      contenu.querySelectorAll("[data-decision]").forEach((x) => { x.disabled = true; });
      agir(async () => {
        const r = await api(`/processus/taches/${tache}/decision`, {
          methode: "POST", corps: { decision: b.dataset.decision, motif, dateDebut },
        });
        const messages = {
          ACCEPTEE: ["Candidature acceptée : dossier de stage créé.", "ok"],
          REFUSEE: [`Candidature refusée. ${r.motifDecision || ""}`, "ok"],
          EN_ATTENTE_DECISION: ["Demande de complément envoyée à l'étudiant.", "ok"],
          INCIDENT: ["Décision enregistrée, mais un service est indisponible : un responsable reprendra le traitement.", "attente"],
        };
        const [message, type] = messages[r.etat] || ["Décision enregistrée.", "ok"];
        annoncer(message, type);
      });
    };
  });
  document.getElementById("publier").onclick = () => formulaireOffre(id, null);
  contenu.querySelectorAll("[data-modifier]").forEach((b) => {
    b.onclick = () => formulaireOffre(id, offres.find((o) => o.idOffre === b.dataset.modifier));
  });
  contenu.querySelectorAll("[data-supprimer]").forEach((b) => {
    b.onclick = async () => {
      if (await confirmer("Supprimer cette offre ?", "La suppression est définitive. Une offre liée à des candidatures ne peut pas être supprimée : clôturez-la plutôt.", "Supprimer l'offre")) {
        agir(async () => {
          await api("/offres/" + b.dataset.supprimer, { methode: "DELETE" });
          annoncer("Offre supprimée.");
        });
      }
    };
  });
}

function formulaireOffre(idEntreprise, offre) {
  const v = offre || {};
  ouvrirDialogue(offre ? "Modifier l'offre" : "Publier une offre", `<form id="f-offre" class="formulaire">
    <label class="champ pleine">Titre <input name="titre" required maxlength="200" autocomplete="off" value="${esc(v.titre)}" placeholder="Stage développeur Java…"></label>
    <label class="champ pleine">Description <textarea name="description" required maxlength="5000" autocomplete="off" placeholder="Missions et contexte du stage…">${esc(v.description)}</textarea></label>
    <label class="champ pleine">Compétences requises, séparées par des virgules <input name="competencesRequises" required maxlength="500" autocomplete="off" value="${esc(v.competencesRequises)}" placeholder="Java, SOAP, BPMN…"></label>
    <label class="champ">Durée en mois <input name="dureeMois" type="number" inputmode="numeric" min="1" max="12" required autocomplete="off" value="${esc(v.dureeMois || 3)}"></label>
    <label class="champ">Places disponibles <input name="placesDisponibles" type="number" inputmode="numeric" min="0" required autocomplete="off" value="${esc(v.placesDisponibles ?? 1)}"></label>
    <label class="champ">Date limite <input name="dateLimite" type="date" required autocomplete="off" value="${esc(v.dateLimite)}"></label>
    <label class="champ">Année minimale, facultative <input name="niveauMinimum" type="number" inputmode="numeric" min="1" max="10" autocomplete="off" value="${esc(v.niveauMinimum)}"></label>
    <label class="champ pleine">Filières ciblées, séparées par des virgules ; vide pour toutes
      <input name="filieresCibles" maxlength="500" autocomplete="off" value="${esc(v.filieresCibles)}" placeholder="Innovation et AMOA, Data Science…"></label>
    ${offre ? `<label class="champ">Statut <select name="statut" autocomplete="off">
      <option value="ACTIVE" ${v.statut === "ACTIVE" ? "selected" : ""}>Active</option>
      <option value="CLOTUREE" ${v.statut === "CLOTUREE" ? "selected" : ""}>Clôturée</option></select></label>` : ""}
    <p class="erreur pleine" id="erreur-offre" role="alert"></p>
    <div class="dialogue-pied pleine"><button type="submit" class="principal">${offre ? "Enregistrer l'offre" : "Publier l'offre"}</button></div>
  </form>`);
  const form = document.getElementById("f-offre");
  form.noValidate = true;
  form.onsubmit = async (ev) => {
    ev.preventDefault();
    const erreur = document.getElementById("erreur-offre");
    erreur.textContent = "";
    if (!verifierFormulaire(form, erreur)) return;
    form.querySelector(".dialogue-pied").before(erreur);
    const retablir = occuper(form.querySelector("button[type=submit]"), "Enregistrement…");
    const corps = Object.fromEntries(new FormData(form));
    corps.idEntreprise = idEntreprise;
    await agir(async () => {
      await api(offre ? "/offres/" + offre.idOffre : "/offres", { methode: offre ? "PUT" : "POST", corps });
      dialogue.close();
      annoncer(offre ? "Offre modifiée." : "Offre publiée.");
    });
    if (dialogue.open) retablir();
  };
}

/* ---------- Espace responsable ---------- */

function groupeServices(titre, services) {
  return `<div class="groupe-services"><h3>${esc(titre)}</h3><ul class="services">
    ${services.map(([nom, ok]) => `<li class="${ok ? "" : "panne"}"><span class="voyant" aria-hidden="true"></span><span>${esc(nom)}</span><span>${ok ? "Disponible" : "Indisponible"}</span></li>`).join("")}
  </ul></div>`;
}

async function vueResponsable() {
  const [sante, incidents, candidaturesLues, stagesLus, processus] = await Promise.all([
    api("/sante"), api("/processus/incidents"), tenter(api("/candidatures")), tenter(api("/stages")), api("/processus"),
  ]);
  const candidatures = candidaturesLues ?? [];
  const stages = stagesLus ?? [];

  const entrees = Object.entries(sante);
  const nomCourt = ([nom, ok]) => [nom.replace(/^(SOAP|REST) /, ""), ok];
  const enPanne = entrees.filter(([, ok]) => !ok).length;
  const compter = (...statuts) => (candidaturesLues ? candidatures.filter((c) => statuts.includes(c.statut)).length : "–");

  const lignesIncidents = incidents.map((i) => [
    `<span class="secondaire nombre">${dateHeure(i.creeeLe)}</span>`,
    esc(ETAPES[i.etape] || i.etape),
    `${esc(i.message)}<div class="secondaire"><span class="code" translate="no">${esc(i.matricule)}</span>${i.titreOffre ? " · " + esc(i.titreOffre) : ""}</div>`,
    `<div class="actions"><button type="button" class="principal" data-resoudre="RELANCER" data-tache="${esc(i.idTache)}">Relancer</button>
      <button type="button" class="danger" data-resoudre="ABANDONNER" data-tache="${esc(i.idTache)}">Abandonner</button></div>`,
  ]);

  const lignesCandidatures = candidatures.map((c) => [
    `${esc(c.nomEtudiant)}<div class="secondaire code" translate="no">${esc(c.matricule)}</div>`,
    `${esc(c.titreOffre)}<div class="secondaire">${esc(c.nomEntreprise)}</div>`,
    tampon(c.statut) + (c.motif ? `<div class="secondaire">${esc(c.motif)}</div>` : ""),
    `<span class="secondaire nombre">${dateHeure(c.dateDepot)}</span>`,
  ]);

  const lignesStages = stages.map((s) => [
    `${esc(s.nomEtudiant)}<div class="secondaire code" translate="no">${esc(s.matricule)}</div>`,
    `${esc(s.titreOffre)}<div class="secondaire">${esc(s.nomEntreprise)}</div>`,
    tampon(s.statut),
    `<span class="nombre">${date(s.dateDebut)} au ${date(s.dateFin)}</span>`,
    `<div class="actions"><button type="button" data-suivi="${esc(s.idStage)}">Ouvrir le suivi</button></div>`,
  ]);

  const lignesProcessus = processus.slice(0, 30).map((p) => [
    `${esc(p.nomEtudiant || p.matricule)}<div class="secondaire">${esc(p.titreOffre || "Offre " + p.idOffre)}</div>`,
    tampon(p.etat) + (p.motifsRejet ? `<div class="secondaire">${esc(p.motifsRejet)}</div>` : ""),
    `<span class="secondaire nombre">${dateHeure(p.debut)}</span>`,
    `<div class="actions"><button type="button" data-processus="${esc(p.idProcessus)}">Voir le parcours</button></div>`,
  ]);

  const chapo = incidents.length
    ? `${pluriel(incidents.length, "incident")} à traiter en priorité.`
    : enPanne
      ? `${pluriel(enPanne, "service indisponible", "services indisponibles")}, aucun incident ouvert pour l'instant.`
      : "Tous les services répondent et aucun incident n'est ouvert.";

  definirNavigation([["services", "Services"], ["incidents", "Incidents"], ["candidatures", "Candidatures"], ["stages", "Stages"], ["historique", "Historique"]]);
  document.title = "Supervision · Plateforme de stages";
  contenu.innerHTML = `<div class="ouverture seule" id="haut">
      <div><h1>Supervision</h1><p class="chapo">${chapo}</p></div>
    </div>
    <div class="compteurs" aria-label="Candidatures par statut">
      <div class="compteur"><b>${compter("DEPOSEE")}</b><span>Déposées</span></div>
      <div class="compteur"><b>${compter("EN_COURS_EVALUATION")}</b><span>En évaluation</span></div>
      <div class="compteur"><b>${compter("ACCEPTEE")}</b><span>Acceptées</span></div>
      <div class="compteur"><b>${compter("REFUSEE")}</b><span>Refusées</span></div>
      <div class="compteur"><b>${compter("ANNULEE")}</b><span>Annulées</span></div>
    </div>` +
    section("incidents", "Incidents à traiter", incidents.length,
      tableau(["Date", "Étape", "Cause", ""], lignesIncidents, "Aucun incident en cours."), "", incidents.length ? "alerte" : "") +
    section("services", "Disponibilité des services", null, `<div class="groupes-services">
        ${groupeServices("Services SOAP", entrees.filter(([n]) => n.startsWith("SOAP")).map(nomCourt))}
        ${groupeServices("Services REST", entrees.filter(([n]) => n.startsWith("REST")).map(nomCourt))}
      </div>`,
      `<button type="button" id="rappels">Envoyer les rappels de date limite</button>`) +
    section("candidatures", "Candidatures", compteOuTiret(candidaturesLues), !candidaturesLues ? INDISPONIBLE :
      tableau(["Étudiant", "Offre", "Statut", "Dépôt"], lignesCandidatures, "Aucune candidature pour le moment.")) +
    section("stages", "Stages", compteOuTiret(stagesLus), !stagesLus ? INDISPONIBLE :
      tableau(["Étudiant", "Stage", "Statut", "Dates", ""], lignesStages, "Aucun stage pour le moment.")) +
    section("historique", "Historique des processus", processus.length,
      tableau(["Candidature", "État", "Début", ""], lignesProcessus, "Aucun processus exécuté pour le moment."));

  document.getElementById("rappels").onclick = (ev) => {
    occuper(ev.currentTarget, "Envoi des rappels…");
    agir(async () => {
      const r = await api("/notifications/rappels?joursAvant=7", { methode: "POST" });
      annoncer(`${pluriel(r.rappelsEnvoyes, "rappel envoyé", "rappels envoyés")} pour les offres qui expirent sous 7 jours.`);
    });
  };
  contenu.querySelectorAll("[data-resoudre]").forEach((b) => {
    b.onclick = async () => {
      if (b.dataset.resoudre === "ABANDONNER" && !await confirmer("Abandonner ce traitement ?", "La candidature sera annulée et l'étudiant sera prévenu.", "Abandonner le traitement")) return;
      contenu.querySelectorAll("[data-resoudre]").forEach((x) => { x.disabled = true; });
      agir(async () => {
        const r = await api(`/processus/incidents/${b.dataset.tache}/resolution`, {
          methode: "POST", corps: { action: b.dataset.resoudre },
        });
        if (r.etat === "INCIDENT") annoncer(`Toujours en échec. ${r.erreurMessage}. Rétablissez le service, puis relancez.`, "ko", 9000);
        else annoncer("Traitement repris. État : " + (STATUTS[r.etat]?.[0] || r.etat));
      });
    };
  });
  contenu.querySelectorAll("[data-processus]").forEach((b) => {
    b.onclick = () => detailProcessus(b.dataset.processus).catch((e) => annoncer(e.message, "ko"));
  });
  brancherSuivi(stages, true);
}

async function detailProcessus(id) {
  const d = await api("/processus/" + id);
  const etapes = d.activites.map((a) => `<li><span>${esc(a.nom)}</span>
    <span class="secondaire nombre">${a.fin ? dateHeure(a.debut) : "En cours"}</span></li>`).join("");
  ouvrirDialogue(`Parcours de ${d.nomEtudiant || d.matricule}`, `<div>${tampon(d.etat)}</div>
    <div class="diagramme" tabindex="0" role="region" aria-label="Diagramme défilant"><img src="/api/processus/${esc(id)}/diagramme" width="1512" height="460" alt="Diagramme BPMN du processus, étapes parcourues surlignées"></div>
    <ul class="etapes">${etapes}</ul>`, "large");
}

/* ---------- Suivi d'un stage ---------- */

function brancherSuivi(stages, gestion) {
  contenu.querySelectorAll("[data-suivi]").forEach((b) => {
    b.onclick = () => {
      const stage = stages.find((s) => String(s.idStage) === b.dataset.suivi);
      suiviStage(stage, gestion).catch((e) => annoncer(e.message, "ko"));
    };
  });
}

const SUITES_STAGE = { PLANIFIE: ["EN_COURS", "INTERROMPU"], EN_COURS: ["TERMINE", "INTERROMPU"] };

async function suiviStage(stage, gestion) {
  const [comptes, evaluations] = await Promise.all([
    api(`/stages/${stage.idStage}/comptes-rendus`), api(`/stages/${stage.idStage}/evaluations`),
  ]);
  const suites = gestion ? (SUITES_STAGE[stage.statut] || []) : [];
  const liste = (lignes, vide) => (lignes.length ? `<ul class="etapes">${lignes.join("")}</ul>` : `<p class="secondaire">${vide}</p>`);

  ouvrirDialogue(stage.titreOffre, `
    <div class="faits"><span>${tampon(stage.statut)}</span><span>${esc(stage.nomEtudiant)}</span>
      <span class="nombre">${date(stage.dateDebut)} au ${date(stage.dateFin)}</span></div>
    ${suites.length ? `<div class="actions">${suites.map((s) =>
      `<button type="button" data-statut="${s}">Passer à : ${esc(STATUTS[s][0])}</button>`).join("")}</div>` : ""}
    <h3>Comptes rendus</h3>
    ${liste(comptes.map((c) => `<li><span><strong>${esc(c.periode)}</strong> : ${esc(c.contenu)}</span>
      <span class="secondaire">${esc(c.auteur)}</span></li>`), "Aucun compte rendu pour le moment.")}
    ${stage.statut === "EN_COURS" ? `<form id="f-cr" class="formulaire">
      <label class="champ">Période <input name="periode" required maxlength="60" autocomplete="off" placeholder="Mars 2027…"></label>
      <label class="champ">Auteur <input name="auteur" required maxlength="120" autocomplete="off" placeholder="Nom de l'auteur…"></label>
      <label class="champ pleine">Contenu <textarea name="contenu" required maxlength="10000" autocomplete="off" placeholder="Travaux réalisés et prochaines étapes…"></textarea></label>
      <div class="dialogue-pied pleine"><button type="submit">Ajouter le compte rendu</button></div>
    </form>` : ""}
    <h3>Évaluations</h3>
    ${liste(evaluations.map((e) => `<li><span><strong class="nombre">${esc(e.note)} / 20</strong> ${esc(e.commentaire)}</span>
      <span class="secondaire">${esc(e.evaluateur)}</span></li>`), "Aucune évaluation pour le moment.")}
    ${gestion && ["EN_COURS", "TERMINE"].includes(stage.statut) ? `<form id="f-eval" class="formulaire">
      <label class="champ">Note sur 20 <input name="note" type="number" inputmode="decimal" min="0" max="20" step="0.25" required autocomplete="off"></label>
      <label class="champ">Évaluateur <input name="evaluateur" required maxlength="120" autocomplete="off" placeholder="Nom de l'évaluateur…"></label>
      <label class="champ pleine">Commentaire <input name="commentaire" maxlength="5000" autocomplete="off" placeholder="Appréciation générale…"></label>
      <div class="dialogue-pied pleine"><button type="submit">Ajouter l'évaluation</button></div>
    </form>` : ""}`);

  const envoyer = (id, chemin, message, libelle) => {
    const form = document.getElementById(id);
    if (form) {
      form.onsubmit = async (ev) => {
        ev.preventDefault();
        const retablir = occuper(form.querySelector("button[type=submit]"), libelle);
        await agir(async () => {
          await api(`/stages/${stage.idStage}/${chemin}`, { methode: "POST", corps: Object.fromEntries(new FormData(form)) });
          dialogue.close();
          annoncer(message);
        });
        if (dialogue.open) retablir();
      };
    }
  };
  envoyer("f-cr", "comptes-rendus", "Compte rendu ajouté.", "Ajout en cours…");
  envoyer("f-eval", "evaluations", "Évaluation ajoutée.", "Ajout en cours…");
  dialogue.querySelectorAll("[data-statut]").forEach((b) => {
    b.onclick = () => {
      occuper(b, "Mise à jour…");
      agir(async () => {
        await api("/stages/" + stage.idStage, { methode: "PATCH", corps: { statut: b.dataset.statut } });
        dialogue.close();
        annoncer("Statut du stage mis à jour.");
      });
    };
  });
}

/* ---------- Démarrage ---------- */

document.getElementById("deconnexion").onclick = async () => {
  try {
    await api("/auth/deconnexion", { methode: "POST" });
  } catch {
    /* Session déjà fermée : on revient simplement à la connexion. */
  }
  etat.moi = null;
  etat.recherche = "";
  etat.profilSeul = false;
  history.replaceState(null, "", location.pathname);
  pageConnexion();
};

(async () => {
  try {
    etat.moi = await api("/auth/moi");
  } catch {
    etat.moi = null;
  }
  await dessiner();
})();
