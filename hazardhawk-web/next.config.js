/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
    NEXT_PUBLIC_APP_URL: process.env.NEXT_PUBLIC_APP_URL || 'http://localhost:3000',
  },
  images: {
    domains: ['hazardhawk-certifications.s3.amazonaws.com', 'dob-trainingconnect.cityofnewyork.us'],
  },
}

module.exports = nextConfig
