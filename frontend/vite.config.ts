import { fileURLToPath, URL } from "node:url";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

/**
 * Configuración principal de Vite para el frontend.
 *
 * Define los plugins utilizados durante desarrollo/build y las reglas
 * de resolución de módulos propias del proyecto.
 */
export default defineConfig({
  /**
   * Habilita el soporte de React dentro de Vite.
   *
   * Permite que Vite procese correctamente archivos JSX/TSX y aplica
   * las transformaciones necesarias para trabajar con React durante
   * desarrollo y build.
   */
  plugins: [react()],

  /**
   * Configuración utilizada por Vite para resolver imports.
   */
  resolve: {
    /**
     * Define aliases para evitar imports relativos largos.
     *
     * El alias "@" representa el directorio `src`.
     *
     * Ejemplo:
     *
     *   import { http } from "@/shared/http";
     *
     * equivale a importar desde:
     *
     *   src/shared/http
     *
     * El mismo alias se declara en `tsconfig.json` mediante `paths`
     * para que tanto Vite como TypeScript interpreten `@` de la
     * misma manera.
     */
    alias: {
      /**
       * `import.meta.url` contiene la URL del archivo vite.config.ts.
       *
       * `new URL("./src", import.meta.url)` construye una URL absoluta
       * apuntando al directorio `src`.
       *
       * `fileURLToPath` convierte esa URL `file://` en una ruta válida
       * del sistema de archivos que Vite puede utilizar como alias.
       *
       * Este enfoque funciona con ES Modules y evita depender de
       * `__dirname`.
       */
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});