'use client';

import { useEffect, useCallback } from 'react';

interface VerificationShortcutsConfig {
  onApprove: () => void;
  onReject: () => void;
  onSkip: () => void;
  onNext: () => void;
  onPrevious: () => void;
  disabled?: boolean;
  modalOpen?: boolean;
}

/**
 * Custom hook for keyboard shortcuts in verification flow
 * Shortcuts:
 * - A = Approve current certification
 * - R = Reject (opens dialog)
 * - S = Skip to next
 * - → = Navigate to next
 * - ← = Navigate to previous
 *
 * Shortcuts are disabled when a modal is open or when disabled=true
 */
export function useVerificationShortcuts({
  onApprove,
  onReject,
  onSkip,
  onNext,
  onPrevious,
  disabled = false,
  modalOpen = false,
}: VerificationShortcutsConfig) {
  const handleKeyPress = useCallback(
    (event: KeyboardEvent) => {
      // Don't trigger shortcuts if:
      // - Shortcuts are disabled
      // - A modal is open
      // - User is typing in an input/textarea
      // - A modifier key is pressed
      if (
        disabled ||
        modalOpen ||
        event.ctrlKey ||
        event.metaKey ||
        event.altKey ||
        event.shiftKey
      ) {
        return;
      }

      const target = event.target as HTMLElement;
      const isInputField =
        target.tagName === 'INPUT' ||
        target.tagName === 'TEXTAREA' ||
        target.isContentEditable;

      // Don't trigger if user is typing in an input field
      if (isInputField) {
        return;
      }

      // Handle shortcuts
      switch (event.key.toLowerCase()) {
        case 'a':
          event.preventDefault();
          onApprove();
          break;
        case 'r':
          event.preventDefault();
          onReject();
          break;
        case 's':
          event.preventDefault();
          onSkip();
          break;
        case 'arrowright':
          event.preventDefault();
          onNext();
          break;
        case 'arrowleft':
          event.preventDefault();
          onPrevious();
          break;
        default:
          // No action for other keys
          break;
      }
    },
    [onApprove, onReject, onSkip, onNext, onPrevious, disabled, modalOpen]
  );

  useEffect(() => {
    // Add event listener
    window.addEventListener('keydown', handleKeyPress);

    // Cleanup
    return () => {
      window.removeEventListener('keydown', handleKeyPress);
    };
  }, [handleKeyPress]);

  // Return helper info about shortcuts
  return {
    shortcuts: {
      approve: 'A',
      reject: 'R',
      skip: 'S',
      next: '→',
      previous: '←',
    },
    isEnabled: !disabled && !modalOpen,
  };
}
