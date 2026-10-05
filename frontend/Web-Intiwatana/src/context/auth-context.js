import { createContext } from "react";

/**
 * El contexto vive solo, separado del provider y del hook.
 *
 * Si el objeto se exportara desde el mismo archivo que el provider, ESLint
 * marcaría react-refresh/only-export-components, porque ese archivo mezclaría
 * componentes y valores que no son componentes.
 */
export const AuthContext = createContext(null);
