#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";

const files = [];

function walk(dir) {
	fs.readdirSync(dir).forEach((f) => {
		const p = path.join(dir, f);
		if (fs.statSync(p).isDirectory()) {
			walk(p);
		} else if (f.endsWith(".scss")) {
			files.push(p);
		}
	});
}

walk("src");

const bad = files.filter((f) =>
	fs.readFileSync(f, "utf8").match(/^@use\s+['"](\.\.\/)+styles\/abstracts/),
);

if (bad.length) {
	console.error("SCSS imports relativos a styles/abstracts detectados:");
	for (const f of bad) {
		console.error(` - ${f}`);
	}
	process.exit(1);
} else {
	console.log("OK: Todos los imports SCSS usan @/styles/abstracts");
}
