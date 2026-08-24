import type { PageConfig } from '@shared/blocks';

/** Backend Spring Boot (auto-ui-api) — mặc định chạy ở :1000 theo application-local.properties. */
export const API = process.env.API_URL ?? 'http://localhost:1000';

/**
 * Envelope chung của auto-ui-api. Chú ý: GlobalExceptionHandler trả HTTP 200 kèm
 * result=false cho NotFoundException — nên không được tin `res.ok`, phải đọc `result`.
 */
interface ApiMessageDto<T> {
  result: boolean;
  code: string | null;
  message: string | null;
  data: T | null;
}

interface PublicPageDto {
  name: string;
  slug: string;
  /** page_config lưu dạng chuỗi opaque, backend trả nguyên văn — FE tự parse. */
  pageConfig: string;
  publishedAt: string | null;
}

export interface PublicPage {
  title: string;
  config: PageConfig;
  publishedAt: string | null;
}

/** Trả null khi trang không tồn tại hoặc chưa publish — caller quyết định notFound(). */
export async function fetchPublicPage(slug: string): Promise<PublicPage | null> {
  const res = await fetch(`${API}/v1/page/public/get/${encodeURIComponent(slug)}`, {
    next: { revalidate: 60, tags: [`page-${slug}`] },
  });
  if (!res.ok) return null;

  const body = (await res.json()) as ApiMessageDto<PublicPageDto>;
  if (!body.result || !body.data) return null;

  return {
    title: body.data.name,
    config: JSON.parse(body.data.pageConfig) as PageConfig,
    publishedAt: body.data.publishedAt,
  };
}
