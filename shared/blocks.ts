/**
 * Nguồn sự thật duy nhất cho danh sách block.
 *
 * Dùng ở 3 nơi:
 *  - editor/ : sinh DomComponents.addType + BlockManager.add + traits
 *  - web/    : registry map type -> React component
 *  - backend : whitelist type khi publish (hiện đang hardcode trong Java)
 *
 * Thêm block mới = sửa 3 nơi: file này, file .tsx bên web/, whitelist backend.
 */

export type PropType = 'text' | 'textarea' | 'select';

export interface PropDef {
  type: PropType;
  label: string;
  default?: string;
  options?: readonly string[];
}

export interface BlockDef {
  label: string;
  props: Record<string, PropDef>;
}

export const BLOCKS = {
  hero: {
    label: 'Hero',
    props: {
      title: { type: 'text', label: 'Tiêu đề', default: 'ITZ Solution' },
      subtitle: {
        type: 'textarea',
        label: 'Mô tả',
        default: 'Dựng trang bằng cách kéo thả, render bằng React component.',
      },
      variant: { type: 'select', label: 'Kiểu', options: ['light', 'dark'], default: 'light' },
    },
  },
  cta: {
    label: 'CTA',
    props: {
      text: { type: 'text', label: 'Nội dung nút', default: 'Liên hệ' },
      href: { type: 'text', label: 'Liên kết', default: '/contact' },
      note: { type: 'text', label: 'Ghi chú', default: 'Phản hồi trong 24h' },
    },
  },
} as const satisfies Record<string, BlockDef>;

export type BlockType = keyof typeof BLOCKS;

export const BLOCK_TYPES = Object.keys(BLOCKS) as BlockType[];

/** Schema của page_config — thứ Next.js đọc để render. */
export interface PageConfig {
  blocks: Array<{ type: string; props: Record<string, string> }>;
}

/** Props mặc định của một block, dùng khi admin chưa điền gì. */
export function defaultProps(type: BlockType): Record<string, string> {
  const props = BLOCKS[type].props as Record<string, PropDef>;
  return Object.fromEntries(
    Object.entries(props)
      .filter(([, d]) => d.default !== undefined)
      .map(([k, d]) => [k, d.default as string]),
  );
}
