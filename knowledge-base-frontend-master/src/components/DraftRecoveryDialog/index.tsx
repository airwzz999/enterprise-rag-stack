import React from 'react';
import { Modal, Button } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';
import type { DraftData } from '@/utils/draft-storage';

interface DraftRecoveryDialogProps {
  open: boolean;
  draft: DraftData | null;
  onAccept: () => void;
  onDismiss: () => void;
}

export const DraftRecoveryDialog: React.FC<DraftRecoveryDialogProps> = ({
  open,
  draft,
  onAccept,
  onDismiss,
}) => {
  if (!draft) return null;

  const savedAt = new Date(draft.savedAt).toLocaleString('en-US');
  const preview = draft.content
    ? draft.content.substring(0, 100) + (draft.content.length > 100 ? '...' : '')
    : '(No content)';

  return (
    <Modal
      open={open}
      title={
        <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <ExclamationCircleOutlined style={{ color: '#f59e0b', fontSize: 18 }} />
          Unsaved Draft Found
        </span>
      }
      onCancel={onDismiss}
      footer={[
        <Button key="discard" danger onClick={onDismiss}>
          Discard Draft
        </Button>,
        <Button key="recover" type="primary" onClick={onAccept}>
          Recover Draft
        </Button>,
      ]}
      centered
      width={480}
      destroyOnClose
    >
      <div style={{ marginBottom: 16 }}>
        <p style={{ color: '#64748b', marginBottom: 12 }}>
          We found unsaved edits from your last session, saved at: {savedAt}
        </p>
        <div
          style={{
            background: '#f8fafc',
            borderRadius: 8,
            padding: '12px 16px',
            lineHeight: 1.8,
          }}
        >
          <div>
            <strong>Title: </strong>
            {draft.title || '(Untitled)'}
          </div>
          <div>
            <strong>Content preview: </strong>
            {preview}
          </div>
        </div>
      </div>
      <p style={{ color: '#94a3b8', fontSize: 12, margin: 0 }}>
        Choose "Recover Draft" to continue editing, or "Discard Draft" to clear the saved content.
      </p>
    </Modal>
  );
};
