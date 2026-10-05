import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// La URL del backend se externaliza por variable de entorno (VITE_PROXY_TARGET)
// para diferenciar desarrollo / pruebas / produccion. El proxy solo actua en el
// servidor de desarrollo de Vite; el bundle de produccion usa rutas relativas
// `/api` (mismo origen), por lo que NUNCA se compila `localhost` en el build.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  // El mismo proxy para `dev` y para `preview`. Sin esto, el bundle de produccion
  // (que usa rutas relativas /api) no se puede ni cargar en local: `vite preview`
  // lo sirve sin proxy, todas las llamadas al API devuelven 404 y no hay forma de
  // hacer una prueba de humo del build real antes de desplegarlo.
  const proxy = {
    '/api': {
      target: proxyTarget,
      changeOrigin: true,
    },
    // Imagenes editoriales servidas por el backend en /media/portal/**. Sin
    // este proxy las vistas previas del portal romperian en desarrollo.
    '/media': {
      target: proxyTarget,
      changeOrigin: true,
    },
  }

  return {
    plugins: [react()],
    server: { proxy },
    preview: { proxy },
  }
})
