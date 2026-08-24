import Link from 'next/link';

const EDITOR_URL = process.env.NEXT_PUBLIC_EDITOR_URL ?? 'http://localhost:5173';
const DEMO_PAGE_ID = process.env.NEXT_PUBLIC_DEMO_PAGE_ID ?? '1';

export default function Home() {
  return (
    <main style={{ maxWidth: 640, margin: '0 auto', padding: '80px 24px', lineHeight: 1.7 }}>
      <h1 style={{ marginBottom: 8 }}>auto-ui demo</h1>
      <p style={{ opacity: 0.7, marginTop: 0 }}>
        Kéo thả trong editor → Publish → trang công khai render bằng React component.
      </p>
      <ul>
        <li>
          <Link href="/p/demo">/p/demo</Link> — trang công khai
        </li>
        <li>
          <a href={`${EDITOR_URL}/?pageId=${DEMO_PAGE_ID}`}>Editor GrapesJS</a> (chạy{' '}
          <code>npm run dev</code> trong <code>editor/</code>)
        </li>
      </ul>
      <p style={{ opacity: 0.6, fontSize: 14 }}>
        Dữ liệu lấy từ auto-ui-api (Spring Boot). Backend phải chạy trước, mặc định ở cổng 1000.
      </p>
    </main>
  );
}
