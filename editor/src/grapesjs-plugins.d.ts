// grapesjs-tabs không ship .d.ts (các plugin còn lại đều có).
declare module 'grapesjs-tabs' {
  import type { Plugin } from 'grapesjs';
  const plugin: Plugin<Record<string, unknown>>;
  export default plugin;
}
