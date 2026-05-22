import type { ReactNode } from 'react';

type StatusBadgeProps = {
  status: 'checking' | 'connected' | 'error';
  children: ReactNode;
};

export function StatusBadge({ status, children }: StatusBadgeProps) {
  return <span className={`status-badge ${status}`}>{children}</span>;
}
