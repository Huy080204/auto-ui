import { notFound } from 'next/navigation';
import { registry } from '@/components/registry';
import { fetchPublicPage } from '@/lib/api';

export const revalidate = 60;

type Props = { params: Promise<{ slug: string }> };

export async function generateMetadata({ params }: Props) {
  const { slug } = await params;
  const page = await fetchPublicPage(slug);
  return { title: page?.title ?? 'Không tìm thấy trang' };
}

export default async function PublicPage({ params }: Props) {
  const { slug } = await params;
  const page = await fetchPublicPage(slug);
  if (!page) notFound();

  return (
    <>
      {page.config.blocks.map((b, i) => {
        const C = registry[b.type as keyof typeof registry];
        if (!C) return null; // block lạ → bỏ qua, không crash
        return <C key={i} {...b.props} />;
      })}
    </>
  );
}
