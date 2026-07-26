import React, { useEffect, useState } from 'react';
import { DEFAULT_AVATAR } from '@/constants/default-avatar';

interface UserAvatarProps extends Omit<React.ImgHTMLAttributes<HTMLImageElement>, 'src'> {
  src?: string | null;
}

const resolveAvatarSrc = (src?: string | null): string => {
  if (!src || !src.trim()) {
    return DEFAULT_AVATAR;
  }
  return src.trim();
};

/**
 * User avatar component.
 *
 * <p>Handles both missing and broken avatar URLs, falling back to the default avatar in either case.</p>
 */
const UserAvatar: React.FC<UserAvatarProps> = ({ src, onError, alt, ...restProps }) => {
  const [currentSrc, setCurrentSrc] = useState(resolveAvatarSrc(src));

  useEffect(() => {
    setCurrentSrc(resolveAvatarSrc(src));
  }, [src]);

  return (
    <img
      {...restProps}
      src={currentSrc}
      alt={alt || 'User avatar'}
      onError={(event) => {
        if (currentSrc !== DEFAULT_AVATAR) {
          setCurrentSrc(DEFAULT_AVATAR);
        }
        onError?.(event);
      }}
    />
  );
};

export default UserAvatar;
