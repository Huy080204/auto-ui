import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'ITZ auto-ui',
  description: 'Demo GrapesJS + Next.js render qua page_config',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
