# User Menu Display Issue - Emergency Fix Guide

## 🚨 Problem Description
After the change, the avatar and user information in the top-right corner are not displaying.

## 🔍 Possible Causes

### 1. CSS Load Order Issue
- **Symptom**: The element exists but its styles aren't applied
- **Fix**: Check the CSS file load order

### 2. Empty User Data
- **Symptom**: The user object is null or undefined
- **Fix**: Make sure the user is logged in

### 3. CSS Class Name Mismatch
- **Symptom**: The HTML uses a new class name but the CSS hasn't been updated
- **Fix**: Confirm the class names match

## ⚡ Quick Fix Options

### Option 1: Force Refresh (simplest)
```bash
# 1. Stop the dev server
# Ctrl+C

# 2. Clear the cache
rm -rf node_modules/.vite
rm -rf dist

# 3. Restart the dev server
npm run dev

# 4. Hard-refresh in the browser
# Chrome: Cmd+Shift+R (Mac) or Ctrl+Shift+R (Windows)
# Firefox: Cmd+Shift+R (Mac) or Ctrl+Shift+R (Windows)
```

### Option 2: Check the User Login State
```javascript
// Run this in the browser console
const user = JSON.parse(localStorage.getItem('user'));
console.log('User data:', user);

// If it's null, you need to log in first
```

### Option 3: Temporarily Use Inline Styles
If the CSS styling issue can't be resolved quickly, you can temporarily add inline styles:

```tsx
<div
  className="user-menu"
  onClick={() => navigate('/profile')}
  style={{
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '8px 16px',
    borderRadius: '8px',
    minWidth: '180px',
    maxWidth: '220px',
    cursor: 'pointer'
  }}
>
  <div
    className="user-avatar"
    style={{
      width: '40px',
      height: '40px',
      borderRadius: '50%',
      background: 'linear-gradient(135deg, #2563eb, #8b5cf6)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      color: 'white',
      fontWeight: 600,
      fontSize: '16px',
      flexShrink: 0
    }}
  >
    {user?.username?.substring(0, 2).toUpperCase() || 'JD'}
  </div>
  <div
    className="user-info-container"
    style={{
      flex: 1,
      minWidth: 0,
      textAlign: 'left'
    }}
  >
    <div
      className="user-name"
      style={{
        fontSize: '14px',
        fontWeight: 600,
        color: '#0f172a',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        lineHeight: 1.2
      }}
    >
      {user?.username || 'John Doe'}
    </div>
    <div
      className="user-role"
      style={{
        fontSize: '12px',
        color: '#94a3b8',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        lineHeight: 1.2
      }}
    >
      Knowledge Administrator
    </div>
  </div>
</div>
```

### Option 4: Verify the CSS Is Loading
```bash
# Visit the debug page
open http://localhost:3002/user-menu-debug.html

# Or check manually
# 1. Open the browser developer tools
# 2. Check the Network tab
# 3. Confirm layout.css has loaded
# 4. Search for whether .user-info-container exists
```

## 📋 Full Checklist

### Environment Check
- [ ] The dev server is running
- [ ] The browser has been hard-refreshed (Cmd+Shift+R)
- [ ] The browser cache has been cleared
- [ ] The CSS file has loaded correctly

### Data Check
- [ ] The user is logged in
- [ ] There is a token in localStorage
- [ ] There is user data in localStorage
- [ ] user.username is not empty

### Code Check
- [ ] The user-info-container class name exists
- [ ] The CSS styles are defined
- [ ] The component renders correctly
- [ ] There are no JavaScript errors

## 🔧 Debugging Steps

### 1. Check Whether the Element Renders
```javascript
// Run this in the browser console
const userMenu = document.querySelector('.user-menu');
console.log('User menu element:', userMenu);
console.log('User menu HTML:', userMenu?.innerHTML);
```

### 2. Check Whether the Styles Are Applied
```javascript
// Run this in the browser console
const userAvatar = document.querySelector('.user-avatar');
const styles = window.getComputedStyle(userAvatar);
console.log('Avatar width:', styles.width);
console.log('Avatar height:', styles.height);
console.log('Avatar background:', styles.background);
```

### 3. Check the User Data
```javascript
// Run this in the browser console
import { useAuthStore } from '@/stores';
const authStore = useAuthStore.getState();
console.log('Auth Store state:', authStore);
console.log('User data:', authStore.user);
```

## 🚀 Most Likely Solution

**Based on the symptoms, the most likely issue is CSS caching**

1. **Stop the dev server**
2. **Clear the Vite cache**
```bash
rm -rf node_modules/.vite
```
3. **Restart the dev server**
```bash
npm run dev
```
4. **Hard-refresh the browser**
- Chrome: `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows)
- Firefox: `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows)

## 📞 Need Further Help?

1. Visit the debug tool: `http://localhost:3002/user-menu-debug.html`
2. Check the browser console for errors
3. Check the Network tab to confirm the CSS has loaded
4. Use the developer tools to inspect element styles

## ⚙️ Reverting to the Original Configuration

If needed, you can temporarily revert to the previous simple version:

```tsx
<div className="user-menu" onClick={() => navigate('/profile')}>
  <div className="user-avatar">
    {user?.username?.substring(0, 2).toUpperCase() || 'JD'}
  </div>
  {user?.username && (
    <span style={{ marginLeft: '8px', fontWeight: 600 }}>
      {user.username}
    </span>
  )}
</div>
```

This at least ensures the user information is displayed, and you can then gradually refine the styling.
