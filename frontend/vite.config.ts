import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Le proxy évite tout souci de CORS en dev : /api -> backend Spring Boot.
const API_TARGET = "http://localhost:8090";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    // Accessible depuis un téléphone sur le même Wi-Fi (http://<ip-du-pc>:5174)
    host: true,
    proxy: {
      "/api": {
        target: API_TARGET,
        changeOrigin: true,
      },
    },
  },
});
