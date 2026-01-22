/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: false, // 🚨 결제창 중복 실행 방지
  output: 'standalone', // Docker 컨테이너용 standalone 빌드
  experimental: {
    // 최적화된 이미지 빌드
    outputFileTracingRoot: require('path').join(__dirname, '../'),
  },
  async redirects() {
    return [
      {
        source: '/payments',
        has: [{ type: 'query', key: 'token' }],
        destination: '/auto-payment?token=:token',
        permanent: false
      }
    ];
  }
};

module.exports = nextConfig;
