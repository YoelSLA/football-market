import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { inflateSync } from "node:zlib";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const output = path.join(root, "src/features/players/assets/teams");
const snapshotPath = path.join(output, "teams.json");
const registryPath = path.join(output, "registry.json");
const userAgent =
	"FootballMarketAssets/1.0 (educational local assets; https://commons.wikimedia.org/)";
const rejectedFiles = new Map([
	[
		"File:Logo Liverpool FC (2024).png",
		"Wordmark L.F.C.; no es el escudo del club (revisión del recurso)",
	],
]);
const unsuitableLogo = (title) =>
	rejectedFiles.get(title) ||
	(/text[ _]logo|wordmark/i.test(title) ? "Logo tipográfico, no escudo" : null);
const leagues = {
	"Premier League": { folder: "premier-league", country: "Q145" },
	Bundesliga: { folder: "bundesliga", country: "Q183" },
	"Primera Division": { folder: "la-liga", country: "Q29" },
	"Primera División": { folder: "la-liga", country: "Q29" },
	"Serie A": { folder: "serie-a", country: "Q38" },
	"Ligue 1": { folder: "ligue-1", country: "Q142" },
};

const normalize = (value) =>
	value.normalize("NFKC").trim().replace(/\s+/g, " ").toLowerCase();
// Solo elimina designaciones jurídicas/deportivas; no usa coincidencias parciales.
const identityName = (value) =>
	normalize(value)
		.replace(/\b(?:f\.?c\.?|a\.?f\.?c\.?|c\.?f\.?)\b/g, "")
		.replace(/[.]/g, "")
		.replace(/\s+/g, " ")
		.trim();
const slug = (value) =>
	value
		.normalize("NFD")
		.replace(/[\u0300-\u036f]/g, "")
		.toLowerCase()
		.replace(/[^a-z0-9]+/g, "-")
		.replace(/^-|-$/g, "");
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
let lastRequest = 0;

async function request(url, binary = false) {
	for (let attempt = 0; attempt < 3; attempt += 1) {
		await delay(Math.max(0, 1100 - (Date.now() - lastRequest)));
		lastRequest = Date.now();
		try {
			const response = await fetch(url, {
				headers: { "User-Agent": userAgent },
				signal: AbortSignal.timeout(25000),
			});
			if ([429, 503].includes(response.status)) {
				const retry = Number(response.headers.get("retry-after"));
				await delay(
					Math.max(
						5000 * (attempt + 1),
						Number.isFinite(retry) ? retry * 1000 : 0,
					),
				);
				continue;
			}
			if (!response.ok) throw new Error(`HTTP ${response.status}`);
			if (binary) {
				if (Number(response.headers.get("content-length")) > 2_000_000)
					throw new Error("Archivo demasiado grande");
				const chunks = [];
				let size = 0;
				for await (const chunk of response.body) {
					size += chunk.length;
					if (size > 2_000_000) throw new Error("Archivo demasiado grande");
					chunks.push(chunk);
				}
				return Buffer.concat(chunks);
			}
			const data = await response.json();
			if (!data || typeof data !== "object" || data.error)
				throw new Error("Respuesta API inválida");
			return data;
		} catch (error) {
			if (attempt === 2) throw error;
			await delay(3000 * (attempt + 1));
		}
	}
	throw new Error("Límite del proveedor; reintentar más tarde");
}

function api(host, parameters) {
	return `https://${host}/w/api.php?${new URLSearchParams({ format: "json", maxlag: "5", ...parameters })}`;
}

async function loadJson(file, fallback) {
	try {
		return JSON.parse(await readFile(file, "utf8"));
	} catch (error) {
		if (error.code === "ENOENT") return fallback;
		throw error;
	}
}

async function saveJson(file, value) {
	await writeFile(file, `${JSON.stringify(value, null, 2)}\n`);
}

function validateImage(bytes, extension) {
	if (extension === "svg") {
		// XML de biblioteca estándar: no ejecuta código, scripts ni recursos externos.
		execFileSync(
			"python3",
			[
				"-c",
				`
import sys, re, xml.etree.ElementTree as ET
data = sys.stdin.buffer.read()
if re.search(rb'<!DOCTYPE|<!ENTITY|<\\?xml-stylesheet', data, re.I): raise ValueError('XML externo')
root = ET.fromstring(data)
if root.tag != '{http://www.w3.org/2000/svg}svg': raise ValueError('No es SVG')
if not root.get('viewBox') and not (root.get('width') and root.get('height')): raise ValueError('Sin dimensiones')
for node in root.iter():
 if node.tag.split('}')[-1].lower() in ('script', 'foreignobject', 'image', 'iframe', 'animate', 'set'): raise ValueError('SVG activo o externo')
 for key, value in node.attrib.items():
  key = key.split('}')[-1].lower()
  if key.startswith('on') or (key == 'href' and not value.startswith('#')): raise ValueError('Referencia externa')
  if re.search(r'url\\(\\s*[^#]|@import|javascript:', value, re.I): raise ValueError('CSS externo')
 if node.tag.split('}')[-1] == 'style' and re.search(r'url\\(\\s*[^#]|@import', node.text or '', re.I): raise ValueError('CSS externo')
`,
			],
			{ input: bytes, stdio: ["pipe", "pipe", "pipe"] },
		);
	} else {
		if (
			bytes.length < 33 ||
			!bytes.subarray(0, 8).equals(Buffer.from("89504e470d0a1a0a", "hex")) ||
			bytes.toString("ascii", 12, 16) !== "IHDR"
		)
			throw new Error("PNG inválido");
		if (
			Math.min(bytes.readUInt32BE(16), bytes.readUInt32BE(20)) < 128 ||
			![4, 6].includes(bytes[25])
		)
			throw new Error("PNG sin alfa o demasiado pequeño");
		if (Math.max(bytes.readUInt32BE(16), bytes.readUInt32BE(20)) > 4096)
			throw new Error("PNG demasiado grande");
		const compressed = [];
		let ended = false;
		for (let offset = 8; offset < bytes.length; ) {
			if (offset + 12 > bytes.length) throw new Error("PNG truncado");
			const size = bytes.readUInt32BE(offset);
			const end = offset + 12 + size;
			if (end > bytes.length) throw new Error("PNG truncado");
			const type = bytes.toString("ascii", offset + 4, offset + 8);
			let crc = 0xffffffff;
			for (const byte of bytes.subarray(offset + 4, end - 4)) {
				crc ^= byte;
				for (let bit = 0; bit < 8; bit += 1)
					crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0);
			}
			if ((crc ^ 0xffffffff) >>> 0 !== bytes.readUInt32BE(end - 4))
				throw new Error("CRC PNG inválido");
			if (type === "IDAT") compressed.push(bytes.subarray(offset + 8, end - 4));
			if (type === "IEND") {
				if (size !== 0 || end !== bytes.length)
					throw new Error("Final PNG inválido");
				ended = true;
			}
			offset = end;
		}
		if (!ended || !compressed.length) throw new Error("PNG incompleto");
		inflateSync(Buffer.concat(compressed), { maxOutputLength: 32_000_000 });
	}
}

async function resolveTeam(team) {
	const league = leagues[team.leagueName];
	if (!league) throw new Error("Liga no reconocida");
	const search = await request(
		api("www.wikidata.org", {
			action: "wbsearchentities",
			search: team.name,
			language: "en",
			limit: "5",
		}),
	);
	const ids = (search.search ?? [])
		.map((item) => item.id)
		.filter((id) => /^Q\d+$/.test(id));
	if (!ids.length) throw new Error("Sin entidad candidata");
	const result = await request(
		api("www.wikidata.org", {
			action: "wbgetentities",
			ids: ids.join("|"),
			props: "labels|aliases|descriptions|claims",
			languages: "en|es|de|fr|it",
		}),
	);
	const candidates = Object.values(result.entities ?? {}).filter((entity) => {
		const names = [
			...Object.values(entity.labels ?? {}),
			...Object.values(entity.aliases ?? {}).flat(),
		].map((item) => item.value);
		const descriptions = Object.values(entity.descriptions ?? {})
			.map((item) => item.value)
			.join(" ");
		const countries = (entity.claims?.P17 ?? []).map(
			(claim) => claim.mainsnak?.datavalue?.value?.id,
		);
		const countryMatches =
			countries.includes(league.country) ||
			(team.name === "AS Monaco FC" && countries.includes("Q235"));
		return (
			countryMatches &&
			/football|soccer|fußball|fútbol|calcio/i.test(descriptions) &&
			!/women|reserve|femenin|féminin/i.test(descriptions) &&
			names.some((name) => identityName(name) === identityName(team.name))
		);
	});
	if (candidates.length !== 1)
		throw new Error(
			`Identidad no confirmada (${candidates.length} coincidencias)`,
		);
	const entity = candidates[0];
	const logos = (entity.claims?.P154 ?? [])
		.filter(
			(claim) => claim.rank !== "deprecated" && !claim.qualifiers?.P582?.length,
		)
		.map((claim) => claim.mainsnak?.datavalue?.value)
		.filter((value) => typeof value === "string");
	const unique = [...new Set(logos)];
	if (
		unique.length !== 1 ||
		/(?:old|histor|former|previous|\b19\d{2}\b)/i.test(unique[0])
	)
		throw new Error("Logo actual no identificado inequívocamente en P154");
	const title = `File:${unique[0]}`;
	if (unsuitableLogo(title)) throw new Error(unsuitableLogo(title));
	const commons = await request(
		api("commons.wikimedia.org", {
			action: "query",
			titles: title,
			prop: "imageinfo",
			iiprop: "url|extmetadata|mime|size",
		}),
	);
	const info = Object.values(commons.query?.pages ?? {})[0]?.imageinfo?.[0];
	if (!info) throw new Error("Archivo no disponible en Commons");
	const metadata = info.extmetadata ?? {};
	const value = (key) => metadata[key]?.value ?? "";
	const license = value("LicenseShortName");
	// Política conservadora: evita incorporar CC BY/SA sin UI de atribución/licencia.
	if (
		!/^(Public domain|CC0(?: 1\.0)?)$/i.test(license) ||
		value("AttributionRequired") !== "false"
	)
		throw new Error(
			`Licencia no autorizada automáticamente: ${license || "desconocida"}`,
		);
	if (/non.free|fair.use|permission|personality/i.test(value("Restrictions")))
		throw new Error("Restricciones adicionales sin permiso verificado");
	if (!value("Credit") || !info.descriptionurl)
		throw new Error("Procedencia incompleta");
	const extension =
		info.mime === "image/svg+xml"
			? "svg"
			: info.mime === "image/png"
				? "png"
				: null;
	if (!extension) throw new Error("Formato no admitido");
	const source = new URL(info.url);
	if (
		source.protocol !== "https:" ||
		source.hostname !== "upload.wikimedia.org"
	)
		throw new Error("Origen de descarga no autorizado");
	source.search = "";
	const bytes = await request(source.href, true);
	validateImage(bytes, extension);
	const file = `${league.folder}/${slug(team.name)}-${team.id}.${extension}`;
	await mkdir(path.join(output, league.folder), { recursive: true });
	// Nunca sobrescribe recursos previos, incluso ante cambios de identidad/licencia.
	await writeFile(path.join(output, file), bytes, { flag: "wx" });
	return {
		status: "downloaded",
		file,
		entityId: entity.id,
		commonsTitle: title,
		aliases: [
			...new Set(
				[
					...Object.values(entity.labels ?? {}),
					...Object.values(entity.aliases ?? {}).flat(),
				].map((item) => item.value),
			),
		],
		sourceUrl: info.descriptionurl,
		downloadUrl: source.href,
		author: value("Artist") || "No identificado",
		credit: value("Credit"),
		license,
		licenseUrl: value("LicenseUrl") || info.descriptionurl,
		rights: value("UsageTerms"),
		restrictions: value("Restrictions"),
		trademarkNotice:
			"La licencia de copyright no concede derechos de marca ni implica respaldo del club.",
		sha256: createHash("sha256").update(bytes).digest("hex"),
		metadata,
	};
}

await mkdir(output, { recursive: true });
let snapshot = await loadJson(snapshotPath, null);
if (process.argv.includes("--refresh-teams") || !snapshot) {
	const sql =
		'SELECT COALESCE(json_agg(row_to_json(x)), \'[]\'::json) FROM (SELECT t.id,t.name,l.id AS "leagueId",l.name AS "leagueName",t.current FROM teams t JOIN leagues l ON l.id=t.league_id ORDER BY t.id) x';
	const teams = JSON.parse(
		execFileSync(
			"docker",
			[
				"exec",
				process.env.FOOTBALL_DB_CONTAINER || "football-market-postgres",
				"psql",
				"-U",
				"postgres",
				"-d",
				"football_market_dev",
				"-At",
				"-c",
				sql,
			],
			{ encoding: "utf8" },
		),
	);
	if (
		!Array.isArray(teams) ||
		!teams.length ||
		teams.some(
			(team) =>
				!Number.isSafeInteger(team.id) ||
				typeof team.name !== "string" ||
				typeof team.leagueName !== "string",
		) ||
		new Set(teams.map((team) => team.id)).size !== teams.length
	)
		throw new Error("Inventario de equipos inválido");
	snapshot = {
		source:
			"teams JOIN leagues; PostgreSQL de desarrollo; consulta de solo lectura",
		consultedAt: new Date().toISOString(),
		teams,
	};
	await saveJson(snapshotPath, snapshot);
}
const previous = await loadJson(registryPath, { entries: [] });
const entries = [];
for (const team of snapshot.teams) {
	const existing = previous.entries.find(
		(entry) =>
			entry.id === team.id &&
			entry.name === team.name &&
			entry.leagueName === team.leagueName,
	);
	// Recupera el registro del wordmark descartado durante la revisión inicial;
	// conserva procedencia y archivo, pero nunca lo publica como escudo.
	if (
		existing?.status === "pending" &&
		team.name === "Liverpool FC" &&
		!existing.file
	) {
		const file = "premier-league/liverpool-fc-6.png";
		const bytes = await readFile(path.join(output, file)).catch(() => null);
		if (bytes) {
			const title = "File:Logo Liverpool FC (2024).png";
			const response = await request(
				api("commons.wikimedia.org", {
					action: "query",
					titles: title,
					prop: "imageinfo",
					iiprop: "url|extmetadata",
				}),
			);
			const info = Object.values(response.query?.pages ?? {})[0]
				?.imageinfo?.[0];
			if (info) {
				const metadata = info.extmetadata ?? {};
				Object.assign(existing, {
					file,
					commonsTitle: title,
					sourceUrl: info.descriptionurl,
					downloadUrl: info.url,
					author: metadata.Artist?.value || "No identificado",
					credit: metadata.Credit?.value || "",
					license: metadata.LicenseShortName?.value || "No identificada",
					licenseUrl: metadata.LicenseUrl?.value || info.descriptionurl,
					restrictions: metadata.Restrictions?.value || "",
					metadata,
					sha256: createHash("sha256").update(bytes).digest("hex"),
					reason: rejectedFiles.get(title),
				});
			}
		}
	}
	if (
		existing?.status === "downloaded" &&
		unsuitableLogo(existing.commonsTitle)
	) {
		entries.push({
			...existing,
			status: "pending",
			reason: unsuitableLogo(existing.commonsTitle),
		});
		continue;
	}
	if (
		existing?.status === "pending" &&
		process.argv.includes("--keep-pending")
	) {
		entries.push(existing);
		continue;
	}
	if (existing?.status === "downloaded") {
		try {
			const bytes = await readFile(path.join(output, existing.file));
			validateImage(bytes, path.extname(existing.file).slice(1));
			if (createHash("sha256").update(bytes).digest("hex") !== existing.sha256)
				throw new Error("Hash cambiado");
			if (!existing.aliases) {
				try {
					const response = await request(
						api("www.wikidata.org", {
							action: "wbgetentities",
							ids: existing.entityId,
							props: "labels|aliases",
							languages: "en|es|de|fr|it",
						}),
					);
					const entity = response.entities?.[existing.entityId];
					existing.aliases = [
						...new Set(
							[
								...Object.values(entity?.labels ?? {}),
								...Object.values(entity?.aliases ?? {}).flat(),
							].map((item) => item.value),
						),
					];
				} catch (error) {
					console.warn(`${team.name}: aliases pendientes — ${error.message}`);
				}
			}
			entries.push(existing);
			console.log(`${team.name}: conservado`);
			await saveJson(registryPath, {
				source: snapshot.source,
				entries: [
					...entries,
					...previous.entries.filter(
						(old) => !entries.some((item) => item.id === old.id),
					),
				],
			});
			continue;
		} catch (error) {
			entries.push({
				...existing,
				status: "pending",
				reason: `Archivo previo no reutilizado: ${error.message}`,
				consultedAt: new Date().toISOString(),
			});
			continue;
		}
	}
	let entry;
	try {
		entry = {
			...team,
			...(await resolveTeam(team)),
			consultedAt: new Date().toISOString(),
		};
	} catch (error) {
		entry = {
			...team,
			status: "pending",
			reason: error.message,
			consultedAt: new Date().toISOString(),
		};
	}
	entries.push(entry);
	console.log(
		`${team.name}: ${entry.status}${entry.reason ? ` — ${entry.reason}` : ""}`,
	);
	// Persistencia incremental: una interrupción no pierde las descargas anteriores.
	await saveJson(registryPath, {
		source: snapshot.source,
		entries: [
			...entries,
			...previous.entries.filter(
				(old) => !entries.some((item) => item.id === old.id),
			),
		],
	});
}
await saveJson(registryPath, { source: snapshot.source, entries });
const approved = entries.filter((entry) => entry.status === "downloaded");
const imports = approved.map(
	(entry, index) =>
		`import logo${index} from ${JSON.stringify(`./${entry.file}`)};`,
);
const mapping = approved.map(
	(entry, index) =>
		`\t{ id: ${entry.id}, name: ${JSON.stringify(entry.name)}, aliases: ${JSON.stringify(entry.aliases || [])}, url: logo${index} },`,
);
await writeFile(
	path.join(output, "index.ts"),
	`// Generado por scripts/import-team-logos.mjs. No editar manualmente.\n${imports.join("\n")}\n\nexport const teamLogoEntries: ReadonlyArray<{ id: number; name: string; aliases: readonly string[]; url: string }> = [\n${mapping.join("\n")}\n];\n`,
);
// Archivos de evidencia: los descartados no se publican, pero se conservan con su procedencia.
const reviewed = [];
for (const entry of entries) {
	if (entry.status !== "downloaded" && entry.file)
		reviewed.push({ ...entry, published: false });
}
if (reviewed.length)
	await writeFile(
		path.join(output, "review.json"),
		`${JSON.stringify({ note: "Recursos revisados y no publicados. Procedencia conservada; ver TEAM-LOGOS.md.", entries: reviewed }, null, 2)}\n`,
	);
console.log(
	`Equipos: ${entries.length}; descargados: ${entries.filter((entry) => entry.status === "downloaded").length}; pendientes: ${entries.filter((entry) => entry.status === "pending").length}`,
);
