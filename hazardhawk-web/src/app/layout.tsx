import type { Metadata } from 'next';
import '../styles/globals.css';
import { QueryProvider } from '@/lib/providers/query-provider';

export const metadata: Metadata = {
  title: 'HazardHawk - Certification Upload Portal',
  description: 'Upload and manage construction safety certifications',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  );
}
