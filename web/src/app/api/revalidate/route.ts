import { revalidateTag } from 'next/cache';
import { NextResponse } from 'next/server';

const HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
};

export function OPTIONS() {
  return new NextResponse(null, { status: 204, headers: HEADERS });
}

/**
 * Editor gọi sau khi publish để trang công khai cập nhật ngay, không phải đợi hết
 * 60s revalidate. Bản production nên để Spring bắn webhook này (PLAN.md mục 12)
 * và thêm secret — demo thì mở, không có gì để lộ.
 */
export async function POST(req: Request) {
  const slug = new URL(req.url).searchParams.get('slug');
  if (!slug) return NextResponse.json({ message: 'Thiếu slug' }, { status: 400, headers: HEADERS });

  revalidateTag(`page-${slug}`);
  return NextResponse.json({ revalidated: slug }, { headers: HEADERS });
}
