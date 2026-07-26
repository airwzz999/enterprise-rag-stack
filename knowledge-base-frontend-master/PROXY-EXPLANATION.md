# Proxy Configuration Explanation

## Problem Explanation

The URL you see, `http://localhost:3002/api/auth/auth/login`, is **completely normal**!

### Why 3002 instead of 8080?

This is how the Vite proxy works:

```
Browser                  Vite dev server              Backend server
  ↓                         ↓                          ↓
http://localhost:3002  →  proxy forward  →  http://localhost:8080
/api/auth/auth/login                 /api/auth/auth/login
```

**Key points:**
1. The browser only ever sees the frontend server (port 3002)
2. The Vite proxy transparently forwards the request to the backend (port 8080) on the server side
3. From the browser's perspective, every request is same-origin (localhost:3002)
4. This avoids CORS cross-origin issues

## Verifying the Proxy Is Working

### Method 1: Check the Network Requests

1. Open the browser developer tools (F12)
2. Switch to the Network tab
3. Trigger an API request
4. Inspect the request details:
   - Request URL: `http://localhost:3002/api/...` (normal)
   - If you see `Remote Address: [::1]:8080`, the proxy is working!

### Method 2: Use the Test Page

Visit: `http://localhost:3002/proxy-test.html`

This page will test:
- Current URL information
- Whether the proxied request works
- That a direct request to the backend fails (proving the proxy is needed)

### Method 3: Check the Vite Console

After starting the dev server, the console will show proxy logs:

```
🔄 Proxying: POST /api/auth/auth/login -> http://localhost:8080/api/auth/auth/login
✅ Proxy response: 200 /api/auth/auth/login
```

## If You Really Need Direct Access to Port 8080

### Option 1: Configure Backend CORS (not recommended for development)

The backend needs to add:

**Spring Boot:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3002")
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

Then update the frontend configuration:
```typescript
// src/services/request.ts
const request = axios.create({
  baseURL: 'http://localhost:8080/api', // access the backend directly
  withCredentials: true,
});
```

**Drawbacks:**
- You lose the benefits of the proxy
- Configuration differs between production and development
- Requires additional CORS configuration

### Option 2: Use the Proxy (recommended)

**Advantages:**
- Development environment matches production
- No CORS configuration needed
- More secure — doesn't expose the backend port
- Auth, logging, etc. can be added within the proxy

## Current Configuration Summary

✅ **Frontend port**: 3002
✅ **Backend port**: 8080
✅ **Proxy rule**: `/api/*` → `http://localhost:8080/api/*`
✅ **Backend status**: Verified running normally

## FAQ

### Q: I see port 3002 in the browser. Is that correct?
**A:** Yes! This is normal proxy behavior. The actual backend request happens on port 8080, but that's transparent to the browser.

### Q: How can I confirm the request really reached the backend?
**A:**
1. Check the Remote Address in the Network tab
2. Check the proxy logs in the Vite console
3. Check the backend server's logs

### Q: What about production?
**A:** Production environments typically use a reverse proxy such as Nginx, configured similarly to:
```nginx
location /api/ {
    proxy_pass http://backend:8080/api/;
}
```

### Q: I want to see the real port 8080 — how?
**A:** You can:
1. Access the backend API directly (will hit CORS issues)
2. Check the Remote Address in the browser developer tools
3. Check the backend server logs

## Test Steps

1. **Make sure the backend is running**
```bash
curl http://localhost:8080/api/auth/auth/login
# Should return a 200 status code
```

2. **Start the frontend dev server**
```bash
npm run dev
```

3. **Visit the test page**
```
http://localhost:3002/proxy-test.html
```

4. **Check the proxy logs**
You should see this in the Vite console:
```
🔄 Proxying: POST /api/auth/auth/login -> http://localhost:8080/api/auth/auth/login
```

5. **Check the browser Network tab**
- The Request URL should be `http://localhost:3002/api/...`
- The Remote Address should show `[::1]:8080` or another address on port 8080

## Conclusion

The current configuration is **correct**:
- The browser sees `http://localhost:3002/api/...`
- The backend actually handles `http://localhost:8080/api/...`
- This is exactly how the proxy is supposed to work

If you really want to see port 8080 in the browser, you'd need to configure backend CORS — but doing so offers no real benefit and only adds complexity.
