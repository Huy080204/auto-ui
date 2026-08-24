/** @type {import('next').NextConfig} */
const nextConfig = {
  // shared/ nằm ngoài thư mục web/ nên cần nới root cho tracing
  outputFileTracingRoot: new URL('..', import.meta.url).pathname,
};

export default nextConfig;
