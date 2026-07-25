import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/books": "http://localhost:9000",
      "/users": "http://localhost:9000",
      "/loans": "http://localhost:9000",
    },
  },
});
