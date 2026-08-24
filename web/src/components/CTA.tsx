type Props = {
  text?: string;
  href?: string;
  note?: string;
};

/** Block `cta` */
export default function CTA({ text = 'Liên hệ', href = '/contact', note }: Props) {
  return (
    <section style={{ padding: '64px 24px', textAlign: 'center' }}>
      <a
        href={href}
        style={{
          display: 'inline-block',
          padding: '14px 32px',
          borderRadius: 999,
          background: '#2563eb',
          color: '#fff',
          fontWeight: 600,
          textDecoration: 'none',
        }}
      >
        {text}
      </a>
      {note && <p style={{ marginTop: 12, fontSize: 14, opacity: 0.6 }}>{note}</p>}
    </section>
  );
}
