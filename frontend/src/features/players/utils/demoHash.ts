/**
 * Hash determinista de enteros usado por la fuente de datos de demostración.
 *
 * No depende de Math.random(), del reloj ni de la configuración regional: el mismo par
 * (seed, salt) produce siempre el mismo entero positivo.
 */
export function demoHash(seed: number, salt: number): number {
	const value = Math.trunc(seed) * 374761393 + Math.trunc(salt) * 668265263;
	let mixed = Math.imul(value ^ (value >>> 13), 1274126177);

	mixed = Math.imul(mixed ^ (mixed >>> 16), 2246822507);
	mixed = mixed ^ (mixed >>> 13);

	return Math.abs(mixed);
}
