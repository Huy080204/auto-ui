import type { Component, Editor } from 'grapesjs';
import { BLOCKS, type BlockType, type PageConfig } from '@shared/blocks';

/**
 * Duyệt một tầng gốc của wrapper → { blocks: [{ type, props }] }.
 * Chỉ lấy đúng key khai báo trong BLOCKS, bỏ id/class/data-block.
 * Mọi block đều phẳng nên không cần đệ quy (PLAN mục 7).
 */
export function extractConfig(editor: Editor): PageConfig {
  const blocks = editor
    .getWrapper()!
    .components()
    .map((c: Component) => {
      const type = c.get('type') as string;
      const def = BLOCKS[type as BlockType];
      if (!def) return null; // block lạ → loại

      const attrs = c.getAttributes() as Record<string, unknown>;
      const props = Object.fromEntries(
        Object.keys(def.props)
          .filter((k) => attrs[k] !== undefined)
          .map((k) => [k, String(attrs[k])]),
      );
      return { type, props };
    })
    .filter((b): b is { type: string; props: Record<string, string> } => b !== null);

  return { blocks };
}
