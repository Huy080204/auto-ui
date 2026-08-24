type Props = {
  title?: string;
  subtitle?: string;
  variant?: string;
};

/** Block `hero` — developer sở hữu CSS, admin chỉ điền props. */
export default function Hero({ title, subtitle, variant = 'light' }: Props) {
  const dark = variant === 'dark';

  return (
    <section
      style={{
        padding: '96px 24px',
        textAlign: 'center',
        background: dark ? '#0f172a' : '#f8fafc',
        color: dark ? '#f8fafc' : '#0f172a',
      }}
    >
      <h1 style={{ margin: 0, fontSize: 'clamp(32px, 6vw, 56px)', letterSpacing: '-0.02em' }}>
        {title ?? 'Tiêu đề'}
      </h1>
      {subtitle && (
        <p
          style={{
            margin: '16px auto 0',
            maxWidth: 640,
            fontSize: 18,
            lineHeight: 1.6,
            opacity: 0.75,
          }}
        >
          {subtitle}
        </p>
      )}
    </section>
  );
}
