import React from 'react';

interface TeamIconProps {
  icon?: string;
  variant?: 'sidebar' | 'avatar';
  size?: number;
}

/** Team icon color map */
const ICON_COLORS: Record<string, { bg: string; fg: string }> = {
  tech:     { bg: '#eff6ff', fg: '#3b82f6' },
  product:  { bg: '#f5f3ff', fg: '#8b5cf6' },
  ops:      { bg: '#ecfdf5', fg: '#10b981' },
  admin:    { bg: '#fff7ed', fg: '#f97316' },
  backend:  { bg: '#f1f5f9', fg: '#64748b' },
  frontend: { bg: '#fdf2f8', fg: '#ec4899' },
  qa:       { bg: '#f0fdfa', fg: '#14b8a6' },
};

const FALLBACK_COLOR = { bg: '#f8fafc', fg: '#94a3b8' };

/** Emoji → icon key map, for backward compatibility with legacy data */
const EMOJI_TO_KEY: Record<string, string> = {
  '🖥️': 'tech',
  '🎯': 'product',
  '📊': 'ops',
  '🏢': 'admin',
  '⚙️': 'backend',
  '🎨': 'frontend',
  '🧪': 'qa',
};

/** Converts a potentially emoji-based icon value into a standard key */
const resolveIconKey = (icon?: string): string | undefined => {
  if (!icon) return undefined;
  if (ICON_COLORS[icon]) return icon;
  return EMOJI_TO_KEY[icon];
};

/**
 * Returns the SVG icon (Feather-style stroke icons) corresponding to the team icon key.
 */
const getIconSvg = (rawIcon: string | undefined, variant: 'sidebar' | 'avatar'): React.ReactNode => {
  const icon = resolveIconKey(rawIcon);
  const size = variant === 'sidebar' ? 18 : 24;

  switch (icon) {
    // ─── Tech Center ───
    case 'tech':
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="16 18 22 12 16 6"></polyline>
          <polyline points="8 6 2 12 8 18"></polyline>
        </svg>
      );

    // ─── Product Center ───
    case 'product':
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <circle cx="12" cy="12" r="6"></circle>
          <circle cx="12" cy="12" r="2"></circle>
        </svg>
      );

    // ─── Operations Center ───
    case 'ops':
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <line x1="18" y1="20" x2="18" y2="10"></line>
          <line x1="12" y1="20" x2="12" y2="4"></line>
          <line x1="6" y1="20" x2="6" y2="14"></line>
        </svg>
      );

    // ─── Functional Center ───
    case 'admin':
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="4" y="2" width="16" height="20" rx="2" ry="2"></rect>
          <line x1="9" y1="6" x2="15" y2="6"></line>
          <line x1="9" y1="10" x2="15" y2="10"></line>
          <line x1="9" y1="14" x2="12" y2="14"></line>
        </svg>
      );

    // ─── Backend Development Team ───
    case 'backend':
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="3"></circle>
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
        </svg>
      );

    // ─── Frontend Development Team ───
    case 'frontend':
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 20h9"></path>
          <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
        </svg>
      );

    // ─── QA Team ───
    case 'qa':
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M9 3H5a2 2 0 0 0-2 2v4m6-6h10a2 2 0 0 1 2 2v4M9 3v18m0 0h10a2 2 0 0 0 2-2V9M9 21H5a2 2 0 0 1-2-2V9m0 0h18"></path>
          <line x1="9" y1="9" x2="9.01" y2="9"></line>
          <line x1="15" y1="9" x2="15.01" y2="9"></line>
          <line x1="9" y1="15" x2="9.01" y2="15"></line>
          <line x1="15" y1="15" x2="15.01" y2="15"></line>
        </svg>
      );

    // ─── Default / unmatched ───
    default:
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="9" cy="7" r="4"></circle>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
          <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
        </svg>
      );
  }
};

/**
 * Team icon component.
 *
 * <p>sidebar variant: a plain 18x18 SVG that inherits the parent's text color, matching the
 * "Knowledge Space" sidebar icon style.</p>
 * <p>avatar variant: a rounded square container with a colored background, following the
 * stat-icon design language from the original prototype.</p>
 */
const TeamIcon: React.FC<TeamIconProps> = ({ icon, variant = 'sidebar', size }) => {
  const resolvedKey = resolveIconKey(icon);
  const colors = ICON_COLORS[resolvedKey || ''] || FALLBACK_COLOR;

  if (variant === 'sidebar') {
    return (
      <span style={{ display: 'inline-flex', alignItems: 'center', color: colors.fg }}>
        {getIconSvg(icon, 'sidebar')}
      </span>
    );
  }

  // avatar variant
  const boxSize = size || 40;


  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: boxSize,
        height: boxSize,
        borderRadius: 12,
        background: `linear-gradient(135deg, ${colors.bg}, ${colors.fg}22)`,
        color: colors.fg,
      }}
    >
      <span style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
        {getIconSvg(icon, 'avatar')}
      </span>
    </span>
  );
};

export default TeamIcon;
