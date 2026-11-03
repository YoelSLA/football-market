const countryCodes =
	"ad ae af ag ai al am ao aq ar as at au aw ax az ba bb bd be bf bg bh bi bj bl bm bn bo bq br bs bt bv bw by bz ca cc cd cf cg ch ci ck cl cm cn co cr cu cv cw cx cy cz de dj dk dm do dz ec ee eg eh er es et fi fj fk fm fo fr ga gb gd ge gf gg gh gi gl gm gn gp gq gr gs gt gu gw gy hk hm hn hr ht hu id ie il im in io iq ir is it je jm jo jp ke kg kh ki km kn kp kr kw ky kz la lb lc li lk lr ls lt lu lv ly ma mc md me mf mg mh mk ml mm mn mo mp mq mr ms mt mu mv mw mx my mz na nc ne nf ng ni nl no np nr nu nz om pa pe pf pg ph pk pl pm pn pr ps pt pw py qa re ro rs ru rw sa sb sc sd se sg sh si sj sk sl sm sn so sr ss st sv sx sy sz tc td tf tg th tj tk tl tm tn to tr tt tv tw tz ua ug um us uy uz va vc ve vg vi vn vu wf ws ye yt za zm zw".split(
		" ",
	);

function normalizeNationality(value: string) {
	return value
		.normalize("NFD")
		.replace(/[\u0300-\u036f]/g, "")
		.trim()
		.toLowerCase();
}

const nationalityCodes = new Map<string, string>();
for (const locale of ["en", "es"]) {
	const names = new Intl.DisplayNames([locale], { type: "region" });
	for (const code of countryCodes) {
		nationalityCodes.set(code, code);
		const name = names.of(code.toUpperCase());
		if (name) nationalityCodes.set(normalizeNationality(name), code);
	}
}

for (const [code, names] of [
	["gb-eng", ["England", "Inglaterra", "gb-eng"]],
	["gb-sct", ["Scotland", "Escocia", "gb-sct"]],
	["gb-wls", ["Wales", "Gales", "gb-wls"]],
	["gb-nir", ["Northern Ireland", "Irlanda del Norte", "gb-nir"]],
	["ci", ["Ivory Coast", "Cote d'Ivoire"]],
	["kr", ["South Korea", "Korea Republic"]],
	["cd", ["DR Congo", "Congo DR"]],
	["us", ["USA", "United States of America"]],
	["tr", ["Turkey"]],
	["xk", ["Kosovo", "xk"]],
] as const) {
	for (const name of names)
		nationalityCodes.set(normalizeNationality(name), code);
}

/** Solo resuelve nacionalidades explícitas; nunca infiere a partir del jugador o liga. */
export function getNationalityFlag(nationality: string | null): string | null {
	if (!nationality) return null;
	const code = nationalityCodes.get(normalizeNationality(nationality));
	return code ? `https://flagcdn.com/w80/${code}.png` : null;
}
