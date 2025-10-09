/**
 * Home page - redirect to QR scanner
 */

import { redirect } from 'next/navigation';

export default function HomePage() {
  redirect('/upload/scan');
}
