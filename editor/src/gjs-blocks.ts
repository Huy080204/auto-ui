import type { Editor } from 'grapesjs';
import { BLOCKS, defaultProps, type BlockType, type PropDef } from '@shared/blocks';

/**
 * Preview trong canvas — chỉ để admin nhìn cho dễ, KHÔNG phải nguồn sự thật.
 * Trang thật do component .tsx bên web/ render, nên hai bên không bao giờ khớp 100%.
 * Các thẻ con đánh dấu data-gjs-selectable=false để admin không sửa được nội dung.
 */
const CHILD = 'data-gjs-selectable="false" data-gjs-hoverable="false" data-gjs-draggable="false"';

function esc(v: string): string {
  return v.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]!);
}

function preview(type: BlockType, attrs: Record<string, unknown>): string {
  const p = (k: string): string => esc(String(attrs[k] ?? ''));

  if (type === 'hero') {
    const dark = attrs.variant === 'dark';
    return `
      <div ${CHILD} style="padding:96px 24px;text-align:center;font-family:system-ui,sans-serif;
        background:${dark ? '#0f172a' : '#f8fafc'};color:${dark ? '#f8fafc' : '#0f172a'}">
        <h1 ${CHILD} style="margin:0;font-size:48px;letter-spacing:-0.02em">${p('title')}</h1>
        <p ${CHILD} style="margin:16px auto 0;max-width:640px;font-size:18px;line-height:1.6;opacity:.75">${p('subtitle')}</p>
      </div>`;
  }

  return `
    <div ${CHILD} style="padding:64px 24px;text-align:center;font-family:system-ui,sans-serif">
      <span ${CHILD} style="display:inline-block;padding:14px 32px;border-radius:999px;
        background:#2563eb;color:#fff;font-weight:600">${p('text')}</span>
      <p ${CHILD} style="margin-top:12px;font-size:14px;opacity:.6">${p('note')}</p>
    </div>`;
}

function toTrait(name: string, def: PropDef) {
  if (def.type === 'select') {
    return {
      type: 'select',
      name,
      label: def.label,
      options: (def.options ?? []).map((o) => ({ id: o, name: o, value: o })),
    };
  }
  return { type: def.type, name, label: def.label };
}

/** Sinh component type + block từ BLOCKS — không khai báo tay từng block. */
export function registerBlocks(editor: Editor): void {
  (Object.keys(BLOCKS) as BlockType[]).forEach((type) => {
    const def = BLOCKS[type];
    const attrs = { 'data-block': type, ...defaultProps(type) };

    editor.DomComponents.addType(type, {
      isComponent: (el) => el.getAttribute?.('data-block') === type && { type },
      model: {
        defaults: {
          tagName: 'section',
          name: def.label,
          droppable: false, // không lồng block
          badgable: true,
          attributes: attrs,
          traits: Object.entries(def.props).map(([k, d]) => toTrait(k, d as PropDef)),
        },
        init(this: any) {
          this.on('change:attributes', () => this.syncPreview());
          this.syncPreview();
        },
        syncPreview(this: any) {
          this.components(preview(type, this.getAttributes()));
        },
      },
    });

    editor.BlockManager.add(type, {
      label: def.label,
      category: 'Blocks',
      content: { type, attributes: attrs },
      media: '<svg viewBox="0 0 24 24" width="28"><rect x="3" y="5" width="18" height="14" rx="2" fill="none" stroke="currentColor" stroke-width="1.6"/></svg>',
    });
  });
}
