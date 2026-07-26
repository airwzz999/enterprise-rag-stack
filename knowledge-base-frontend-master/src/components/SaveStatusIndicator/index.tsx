import React from 'react';
import { LoadingOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import type { SaveStatus } from '@/hooks/useAutoSave';

interface SaveStatusIndicatorProps {
  status: SaveStatus;
  lastSavedAt: Date | null;
}

const statusConfig: Record<SaveStatus, { label: string; color: string }> = {
  idle: { label: '', color: 'transparent' },
  saved: { label: 'Saved', color: '#10b981' },
  saving: { label: 'Saving...', color: '#f59e0b' },
  unsaved: { label: 'Unsaved changes', color: '#94a3b8' },
  error: { label: 'Save failed', color: '#ef4444' },
};

const formatTime = (date: Date): string => {
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  if (diffSec < 5) return 'just now';
  if (diffSec < 60) return `${diffSec}s ago`;
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
};

export const SaveStatusIndicator: React.FC<SaveStatusIndicatorProps> = ({
  status,
  lastSavedAt,
}) => {
  if (status === 'idle') return null;

  const config = statusConfig[status];

  const icon =
    status === 'saving' ? (
      <LoadingOutlined style={{ fontSize: 12 }} />
    ) : status === 'saved' ? (
      <CheckCircleOutlined style={{ fontSize: 12 }} />
    ) : status === 'error' ? (
      <ExclamationCircleOutlined style={{ fontSize: 12 }} />
    ) : (
      <span
        style={{
          display: 'inline-block',
          width: 6,
          height: 6,
          borderRadius: '50%',
          backgroundColor: config.color,
        }}
      />
    );

  return (
    <div
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 4,
        fontSize: 12,
        color: config.color,
        whiteSpace: 'nowrap',
        lineHeight: '32px',
      }}
    >
      {icon}
      <span>{config.label}</span>
      {lastSavedAt && status === 'saved' && (
        <span style={{ color: '#94a3b8' }}>{formatTime(lastSavedAt)}</span>
      )}
    </div>
  );
};
