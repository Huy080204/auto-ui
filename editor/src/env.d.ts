/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL?: string;
  readonly VITE_WEB_URL?: string;
  readonly VITE_PAGE_ID?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
