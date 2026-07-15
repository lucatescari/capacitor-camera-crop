import { defineConfig } from 'vite';

// Standard Vite app. Build output goes to dist/, which Capacitor copies into the
// native projects (see capacitor.config.json "webDir": "dist").
export default defineConfig({
  build: {
    outDir: 'dist',
  },
});
