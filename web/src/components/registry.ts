import type { ComponentType } from 'react';
import { BLOCKS } from '@shared/blocks';
import Hero from './Hero';
import CTA from './CTA';

/**
 * type (trong page_config) -> React component.
 * Key phải khớp BLOCKS; nếu lệch, TypeScript báo lỗi ngay tại đây.
 */
// props tới từ page_config nên chỉ biết là Record<string, string> lúc runtime;
// mỗi component tự khai báo props thật của nó.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const registry: Record<keyof typeof BLOCKS, ComponentType<any>> = {
  hero: Hero,
  cta: CTA,
};
