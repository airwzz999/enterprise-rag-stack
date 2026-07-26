# Frontend/Backend Login Functionality Test Guide

## 📋 Feature Description

### Token Storage Strategy
- **Cookie storage**: Primary storage method, stores the JWT Token
- **LocalStorage storage**: Backup storage method, stores user information

### Authentication Flow
1. User logs in → backend returns a JWT Token
2. Frontend saves the Token to a Cookie
3. Frontend reads the Token from the Cookie
4. Token is added to the Authorization Header
5. Backend reads the Token from the Header and validates it

---

## 🔧 Backend Configuration

### Completed Configuration

#### 1. Spring Security Configuration (No Session)
```java
// SecurityConfig.java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless, no session used
```

#### 2. JWT Authentication Filter
```java
// JwtAuthenticationFilter.java
- Reads the Token from the Authorization Header
- Parses and validates the Token
- Sets user information into the ThreadLocal context
- Cleans up the context after the request completes
```

#### 3. Endpoint Paths
```
Login endpoint: /api/auth/auth/login
User info: /api/auth/auth/me
```

---

## 🎯 Frontend Configuration

### Completed Configuration

#### 1. Cookie Management (`utils/cookie.ts`)
```typescript
// Save Token to Cookie
cookieManager.set('access_token', token, {
  expires: 7,           // expires in 7 days
  path: '/',             // available on all paths
  sameSite: 'lax',      // prevents CSRF
});
```

#### 2. Token Storage (`utils/token-storage.ts`)
```typescript
// Save Token after successful login
saveToken(loginResponse: LoginResponse) {
  // Save to Cookie (primary)
  cookieManager.set('access_token', accessToken);

  // Save to LocalStorage (backup)
  localStorage.setItem('access_token', accessToken);
}

// Get Token (prefer Cookie)
getAccessToken(): string | null {
  return cookieManager.get('access_token') ||
         localStorage.getItem('access_token');
}
```

#### 3. Axios Interceptor (`services/request.ts`)
```typescript
// Request interceptor: read the Token from the Cookie and add it to the Header
request.interceptors.request.use((config) => {
  const authHeader = tokenStorage.getAuthorizationHeader();
  if (authHeader) {
    config.headers.Authorization = authHeader;
  }
  return config;
});
```

---

## ✅ Test Steps

### Step 1: Test the Login Endpoint

```bash
curl -X POST http://localhost:8080/api/auth/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "123456"}'
```

**Expected response:**
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": {
      "userId": 1000000000000000001,
      "username": "admin",
      "nickname": "System Administrator"
    }
  },
  "timestamp": 1715905600000
}
```

### Step 2: Manually Save the Token to a Cookie

Run this in the browser console:
```javascript
// Save the Token to a Cookie (expires in 7 days)
document.cookie = "access_token=<your_access_token>; path=/; max-age=604800; SameSite=Lax";

// Verify the Cookie was saved
console.log(document.cookie);
```

### Step 3: Test the Get User Info Endpoint

```bash
curl -X GET http://localhost:8080/api/auth/auth/me \
  -H "Authorization: Bearer <your_access_token>"
```

**Expected response:**
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 1000000000000000001,
    "username": "admin",
    "realName": "System Administrator",
    "email": "admin@company.com",
    "avatar": "https://api.dicebear.com/7.x/avataaars/svg?seed=admin"
  },
  "timestamp": 1715905600000
}
```

### Step 4: Use the Test Page

Visit: `http://localhost:3002/test-login.html`

The test page includes a full functional test:
1. ✅ Log in and save the Token to a Cookie
2. ✅ Read the Token from the Cookie
3. ✅ Add the Token to the Authorization Header
4. ✅ Call the /me endpoint to get user info

---

## 🐛 Debugging Methods

### 1. Check Whether the Cookie Was Saved Successfully

Run this in the browser console:
```javascript
// View all cookies
console.log(document.cookie);

// View a specific cookie
const getCookie = (name) => {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  return parts.length === 2 ? parts.pop().split(';').shift() : null;
};
console.log('access_token:', getCookie('access_token'));
```

### 2. Check Whether the Token Was Added to the Header

In the browser developer tools:
1. Open the Network tab
2. Find the `/api/auth/auth/me` request
3. Inspect the Request Headers
4. Confirm `Authorization: Bearer xxx` is present

### 3. Check the Backend Logs

```bash
# View the user auth service log
tail -f kb-user-auth/logs/kb-user-auth.log

# View the gateway log
tail -f kb-gateway/logs/kb-gateway.log
```

---

## 📝 Verification Checklist

- [ ] The backend generates a real JWT Token (not a mock token)
- [ ] The frontend saves the Token to a Cookie after successful login
- [ ] The frontend reads the Token from the Cookie
- [ ] The frontend adds the Token to the Authorization Header
- [ ] The backend reads the Token from the Header
- [ ] The backend validates the Token and parses the user information
- [ ] The backend sets the user information into the context
- [ ] The /me endpoint gets the user information from the context
- [ ] The correct user information is returned

---

## 🔑 Key Points

1. **No session used**: the backend is configured with `SessionCreationPolicy.STATELESS`
2. **Token stored in a Cookie**: for persistence across requests
3. **Token read from the Cookie**: the frontend reads it and adds it to the Header
4. **Backend reads from the Header**: it does not rely on the Cookie being sent automatically
5. **Authorization Header**: uses the `Bearer <token>` format

---

## 📞 Troubleshooting

### Issue: The Cookie isn't being saved
**Cause**: Possibly a CORS or Cookie configuration issue
**Fix**: Check the CORS configuration and Cookie attributes

### Issue: There's no Token in the Header
**Cause**: Cookie read failure or interceptor issue
**Fix**: Check the debug logs in the browser console

### Issue: Backend returns a 401 error
**Cause**: The Token is invalid or has expired
**Fix**: Log in again to obtain a new Token

### Issue: The backend can't read the Token from the Header
**Cause**: Incorrect header name or format
**Fix**: Confirm the `Authorization: Bearer <token>` format is used
