#!/bin/bash

# Quick fix script for 403 errors

echo "🔍 403 Forbidden error diagnostic tool"
echo "================================"

# Check whether the backend is running
echo "1️⃣ Checking the backend service..."
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/auth/login | grep -q "200\|401\|400"; then
    echo "✅ Backend service is running normally (http://localhost:8080)"
else
    echo "❌ Backend service is unreachable, make sure the backend is running on port 8080"
    echo "   Example start command: java -jar your-backend.jar"
fi

# Check the frontend configuration
echo ""
echo "2️⃣ Checking the frontend configuration..."
if [ -f "vite.config.ts" ]; then
    echo "✅ Vite config file exists"
    if grep -q "target: 'http://localhost:8080'" vite.config.ts; then
        echo "✅ Proxy config points to the correct backend address"
    else
        echo "❌ Proxy config may be misconfigured"
    fi
else
    echo "❌ Vite config file does not exist"
fi

# Check environment variables
echo ""
echo "3️⃣ Checking environment configuration..."
if [ -f ".env.development" ]; then
    echo "✅ Development environment config exists"
    echo "📋 API_BASE_URL: $(grep VITE_API_BASE_URL .env.development || echo 'Not set')"
else
    echo "⚠️  Development environment config does not exist"
fi

# Test the proxy
echo ""
echo "4️⃣ Testing API access..."
echo "📡 Testing direct access to the backend..."
DIRECT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/auth/login)
echo "   Direct access status code: $DIRECT_STATUS"

if [ "$DIRECT_STATUS" = "200" ] || [ "$DIRECT_STATUS" = "401" ]; then
    echo "   ✅ Backend API is reachable"
else
    echo "   ❌ Backend API access is failing"
fi

echo ""
echo "📋 Diagnostic summary:"
echo "================================"
echo "🔧 Suggested fix steps:"
echo ""
echo "Option A: Temporarily disable CSRF (recommended for development)"
echo "Add to the backend SecurityConfig:"
echo "  http.csrf().disable()"
echo ""
echo "Option B: Add CORS configuration"
echo "Add to the backend WebConfig:"
echo "  registry.addMapping(\"/api/**\")"
echo "    .allowedOrigins(\"http://localhost:3002\")"
echo "    .allowedMethods(\"*\")"
echo "    .allowedHeaders(\"*\")"
echo "    .allowCredentials(true);"
echo ""
echo "Option C: Use the diagnostic tool"
echo "Visit: http://localhost:3002/403-debug.html"
echo ""
echo "📚 Full documentation: 403-SOLUTION.md"
echo ""
echo "🚀 Next steps:"
echo "1. Update the backend configuration per the options above"
echo "2. Restart the backend service"
echo "3. Visit the diagnostic tool to verify"
echo "4. Test the login flow"
