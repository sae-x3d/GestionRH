const fs = require('node:fs');
const path = require('node:path');

// All uploaded/demo files live under datas/, beside the db directory.
const root = path.resolve(__dirname, '..');
const sqlPath = path.join(__dirname, 'data-14-11-26.sql');
  const cvSource = path.join(root, 'cv', 'cv_test.pdf');
const photoSource = path.join(root, 'pdp', 'pdp_test.png');
if (!fs.existsSync(cvSource) || !fs.existsSync(photoSource)) {
  throw new Error('Fichiers sources manquants: datas/cv/cv_test.pdf et datas/pdp/pdp_test.png sont requis.');
}

function parseSeedPeople(sql) {
  const marker = 'INSERT INTO seed_employes VALUES';
  const start = sql.indexOf(marker);
  if (start < 0) throw new Error('Bloc seed_employes introuvable dans le script SQL.');
  const end = sql.indexOf(';', start);
  const body = sql.slice(start + marker.length, end);
  const tuples = [];
  let inQuote = false;
  let depth = 0;
  let tuple = '';
  for (const char of body) {
    if (char === "'") inQuote = !inQuote;
    if (!inQuote && char === '(') { depth = 1; tuple = ''; continue; }
    if (!inQuote && char === ')' && depth === 1) { tuples.push(tuple); depth = 0; continue; }
    if (depth) tuple += char;
  }
  return tuples.map((text) => {
    const values = [];
    let token = '';
    let quoted = false;
    for (const char of text) {
      if (char === "'") { quoted = !quoted; token += char; continue; }
      if (char === ',' && !quoted) { values.push(token.trim()); token = ''; }
      else token += char;
    }
    values.push(token.trim());
    const value = (index) => values[index]?.replace(/^'|'$/g, '').replaceAll("''", "'") ?? '';
    return {
      username: value(0), name: value(2), region: value(4), address: value(5), city: value(6),
      department: value(9), postCode: value(10), salary: value(11), contract: value(12),
      startDate: value(13), birthDate: value(14), endDate: value(15), offerCode: value(16)
    };
  }).sort((a, b) => a.username < b.username ? -1 : a.username > b.username ? 1 : 0);
}

function ascii(text) {
  return text.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[’‘]/g, "'").replace(/[–—]/g, '-').replace(/œ/g, 'oe').replace(/Œ/g, 'OE')
    .replace(/[^\x20-\x7E]/g, ' ');
}

function wrap(text, max = 94) {
  const words = ascii(text).split(/\s+/).filter(Boolean);
  const lines = [];
  let line = '';
  for (const word of words) {
    const next = line ? `${line} ${word}` : word;
    if (next.length > max && line) { lines.push(line); line = word; }
    else line = next;
  }
  if (line) lines.push(line);
  return lines;
}

function quotePdf(text) { return ascii(text).replaceAll('\\', '\\\\').replaceAll('(', '\\(').replaceAll(')', '\\)'); }

function buildPdf(lines) {
  const pages = [];
  let pageLines = [];
  for (const line of lines) {
    if (pageLines.length >= 48) { pages.push(pageLines); pageLines = []; }
    pageLines.push(line);
  }
  if (pageLines.length) pages.push(pageLines);
  const objects = [];
  objects[1] = '<< /Type /Catalog /Pages 2 0 R >>';
  const pageIds = pages.map((_, index) => 4 + index * 2);
  objects[2] = `<< /Type /Pages /Kids [${pageIds.map((id) => `${id} 0 R`).join(' ')}] /Count ${pages.length} >>`;
  objects[3] = '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>';
  pages.forEach((pageLines, pageIndex) => {
    const pageId = pageIds[pageIndex];
    const streamId = pageId + 1;
    const commands = [];
    commands.push('0.11 0.34 0.28 rg 0 744 595 98 re f');
    commands.push('1 1 1 rg BT /F2 17 Tf 44 800 Td (SIRH - CONTRAT DE TRAVAIL) Tj ET');
    commands.push('0.88 0.95 0.91 rg BT /F1 9 Tf 44 780 Td (Document contractuel de demonstration - conserver une copie signee) Tj ET');
    let y = 720;
    for (const line of pageLines) {
      const isHeading = line.startsWith('## ');
      const isSubhead = line.startsWith('# ');
      const text = line.replace(/^##? /, '');
      if (!text) { y -= 7; continue; }
      if (isHeading) {
        y -= 8;
        commands.push(`0.12 0.43 0.35 rg BT /F2 10 Tf 46 ${y} Td (${quotePdf(text.toUpperCase())}) Tj ET`);
        y -= 17;
      } else if (isSubhead) {
        commands.push(`0.16 0.20 0.18 rg BT /F2 10 Tf 46 ${y} Td (${quotePdf(text)}) Tj ET`);
        y -= 17;
      } else {
        commands.push(`0.16 0.20 0.18 rg BT /F1 9 Tf 48 ${y} Td (${quotePdf(text)}) Tj ET`);
        y -= 13;
      }
    }
    commands.push('0.45 0.53 0.48 rg BT /F1 7 Tf 45 28 Td (SIRH - Document de demonstration genere a partir des donnees du dossier salarie.) Tj ET');
    const stream = commands.join('\n');
    objects[pageId] = `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R /F2 3 0 R >> >> /Contents ${streamId} 0 R >>`;
    objects[streamId] = `<< /Length ${Buffer.byteLength(stream, 'ascii')} >>\nstream\n${stream}\nendstream`;
  });
  const count = objects.length - 1;
  let output = '%PDF-1.4\n% SIRH sample contract\n';
  const offsets = [0];
  for (let id = 1; id <= count; id++) {
    offsets[id] = Buffer.byteLength(output, 'ascii');
    output += `${id} 0 obj\n${objects[id]}\nendobj\n`;
  }
  const xref = Buffer.byteLength(output, 'ascii');
  output += `xref\n0 ${count + 1}\n0000000000 65535 f \n`;
  for (let id = 1; id <= count; id++) output += `${String(offsets[id]).padStart(10, '0')} 00000 n \n`;
  output += `trailer\n<< /Size ${count + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  // Keep a stable size so the SQL can store the exact document metadata.
  if (Buffer.byteLength(output, 'ascii') > 10000) throw new Error('Le contrat depasse la taille PDF de demonstration prevue.');
  output = output.padEnd(10000, ' ');
  return Buffer.from(output, 'ascii');
}

function contractText(person, employeeId) {
  const deptNames = { INFORMATIQUE: 'Informatique', LOGISTIQUE: 'Logistique', FINANCE: 'Finance', COMMERCIAL: 'Commercial' };
  const postNames = { MGR: 'Manager', DEV: 'Developpeur junior', AST: 'Assistant', AGL: 'Agent logistique', CPT: 'Comptable', COM: 'Commercial' };
  const departmentCodes = { INFORMATIQUE: 'IT', LOGISTIQUE: 'LG', FINANCE: 'FI', COMMERCIAL: 'CO' };
  const typeNames = { CDI: 'Contrat a duree indeterminee (CDI)', CDI_PERIODE_ESSAI: 'Contrat a duree indeterminee avec periode d essai', CDD: 'Contrat a duree determinee (CDD)', CDD_RENOUVELABLE: 'Contrat a duree determinee renouvelable' };
  const matricule = `${departmentCodes[person.department]}${person.postCode}${String(employeeId).padStart(3, '0')}`;
  const salary = `${Number(person.salary).toLocaleString('en-US')} MGA brut par mois`;
  const endClause = person.endDate
    ? `Le contrat est conclu du ${person.startDate} au ${person.endDate}, sous reserve des dispositions legales applicables au CDD.`
    : `Le contrat prend effet le ${person.startDate} et est conclu sans terme fixe, sous reserve des dispositions applicables au CDI.`;
  const probation = person.contract.includes('ESSAI')
    ? 'Une periode d essai est prevue selon la duree et les conditions indiquees dans les documents de recrutement et les dispositions applicables.'
    : 'Aucune periode d essai distincte n est ajoutee par ce document; toute stipulation particuliere doit figurer dans un avenant signe.';
  const lines = [
    '# Reference du contrat', `Numero: CTR-${person.username}`, `Matricule: ${matricule}`, `Date de generation: 2026-11-14`, '',
    '## 1. Parties',
    `Employeur: Organisation SIRH de demonstration, service des ressources humaines.`,
    `Salarie: ${person.name}, ne(e) le ${person.birthDate}, matricule ${matricule}.`,
    `Coordonnees du salarie: ${person.address}, ${person.city}, region ${person.region}, Madagascar.`, '',
    '## 2. Emploi et lieu de travail',
    `Le salarie est engage en qualite de ${postNames[person.postCode]} au departement ${deptNames[person.department]}.`,
    `Lieu de rattachement administratif: ${person.city}. L organisation des activites et les horaires sont communiques par le responsable du departement.`,
    'Les missions principales sont celles du poste et de la fiche de fonction; toute evolution durable fera l objet d un echange et, si necessaire, d un avenant.', '',
    '## 3. Nature et duree du contrat', typeNames[person.contract], endClause, probation, '',
    '## 4. Remuneration',
    `La remuneration mensuelle brute de base est fixee a ${salary}. Il s agit du salaire de base contractuel avant retenues et cotisations.`,
    'Des primes peuvent s ajouter lorsqu elles sont prevues par une regle applicable, un accord ou une decision documentee. Elles sont distinctes du salaire de base et leurs conditions sont verifiees avant chaque paie.',
    'Les retenues obligatoires et contributions sont appliquees selon les dispositions et baremes en vigueur, apres verification par les services competents.', '',
    '## 5. Temps de travail, conges et absences',
    'Le temps de travail est organise selon le planning communique au salarie, les horaires applicables au departement et les textes en vigueur.',
    'Les demandes de conge suivent le circuit de validation du manager et des ressources humaines. Une absence autorisee est enregistree dans le planning et le dossier du salarie.',
    'Les heures supplementaires, retards et absences sont enregistres et traites suivant les regles internes et les dispositions applicables.', '',
    '## 6. Obligations professionnelles',
    'Le salarie s engage a respecter les consignes de securite, les regles de confidentialite, les procedures internes et les obligations liees a sa fonction.',
    'Les informations, acces et equipements mis a disposition sont utilises pour les besoins professionnels et proteges contre tout acces non autorise.',
    'Le salarie informe son responsable et les ressources humaines de tout changement utile a la gestion de son dossier administratif.', '',
    '## 7. Documents et dossier administratif',
    'Les informations personnelles, le CV, les elements du questionnaire d embauche et les documents administratifs associes sont conserves dans le dossier individuel avec des acces limites aux personnes habilitees.',
    'Le present contrat est rattache au dossier de recrutement et au matricule indique en premiere page. Toute modification contractuelle est tracee et conservee dans l historique.', '',
    '## 8. Dispositions finales',
    'Le present document reprend les informations enregistrees dans le jeu de donnees de demonstration. Les dispositions imperatives et conventionnelles applicables prevalent en cas de contradiction.',
    'Le contrat est etabli en deux exemplaires. Chaque partie reconnait en avoir pris connaissance avant signature.', '',
    `Fait a ${person.city}, le 2026-11-14.`, '',
    'Pour l employeur (Ressources humaines):             Le salarie:', '',
    'Nom et signature: __________________________       Nom et signature: __________________________', '',
    'Document genere automatiquement pour la demonstration du SIRH. Les mentions d identite juridique de l employeur et les clauses definitives sont a renseigner et verifier avant usage reel.'
  ];
  return lines.flatMap((line) => line.startsWith('#') || !line ? [line] : wrap(line));
}

const people = parseSeedPeople(fs.readFileSync(sqlPath, 'utf8'));
if (people.length !== 24) throw new Error(`Le seed attend 24 fiches salariales, ${people.length} trouvees.`);
const extraUsers = ['demo_rh','demo_gestionnaire','demo_administration','candidat_valide','candidat_attente','candidat_non_verifie'];
for (const username of [...people.map((person) => person.username), ...extraUsers]) {
  fs.copyFileSync(photoSource, path.join(root, 'pdp', `pdp_${username}.png`));
}
for (const [index, person] of people.entries()) {
  fs.copyFileSync(cvSource, path.join(root, 'cv', `cv_${person.username}.pdf`));
  const employeeId = index + 1;
  const employeeDirectory = path.join(root, 'dossier-employe', String(employeeId));
  fs.mkdirSync(employeeDirectory, { recursive: true });
  const versionName = `00000000-0000-0000-0000-${String(employeeId).padStart(12, '0')}.pdf`;
  fs.copyFileSync(cvSource, path.join(employeeDirectory, versionName));
  const contractPath = path.join(root, 'contrats', `contrat_${person.username}.pdf`);
  const contractPdf = buildPdf(contractText(person, employeeId));
  fs.writeFileSync(contractPath, contractPdf);
  const contractVersionName = `10000000-0000-0000-0000-${String(employeeId).padStart(12, '0')}.pdf`;
  fs.copyFileSync(contractPath, path.join(employeeDirectory, contractVersionName));
}
console.log(`Documents generes: ${people.length} CV de candidature, ${people.length} CV de dossier, ${people.length} contrats PDF et ${people.length + extraUsers.length} photos de profil.`);
