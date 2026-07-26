#!/bin/bash

echo "========================================="
echo "Testing the /api/auth/me endpoint"
echo "========================================="

# Color definitions
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Test the login endpoint to get a token
echo -e "\n${YELLOW}Step 1: Test the login endpoint${NC}"
echo "POST http://localhost:8080/api/auth/auth/login"
echo "Request Body: {\"username\": \"admin\", \"password\": \"123456\"}"

LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "123456"}')

echo "Response: $LOGIN_RESPONSE"

# Extract the token
TOKEN=$(echo $LOGIN_RESPONSE | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['data']['accessToken'])" 2>/dev/null)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}Login failed, could not get a token${NC}"
    exit 1
fi

echo -e "${GREEN}Login succeeded, got token: $TOKEN${NC}"

# 2. Test the get-user-info endpoint (without a token)
echo -e "\n${YELLOW}Step 2: Test /api/auth/me (without a token)${NC}"
echo "GET http://localhost:8080/api/auth/me"

ME_RESPONSE_NO_TOKEN=$(curl -s -w "\nHTTP Status: %{http_code}" -X GET http://localhost:8080/api/auth/me \
  -H "Content-Type: application/json")

echo "Response: $ME_RESPONSE_NO_TOKEN"

# 3. Test the get-user-info endpoint (with a token)
echo -e "\n${YELLOW}Step 3: Test /api/auth/me (with a token)${NC}"
echo "GET http://localhost:8080/api/auth/me"
echo "Authorization: Bearer $TOKEN"

ME_RESPONSE=$(curl -s -w "\nHTTP Status: %{http_code}" -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "Response: $ME_RESPONSE"

# 4. Test an invalid token
echo -e "\n${YELLOW}Step 4: Test /api/auth/me (with an invalid token)${NC}"
echo "GET http://localhost:8080/api/auth/me"
echo "Authorization: Bearer invalid-token-123"

ME_RESPONSE_INVALID=$(curl -s -w "\nHTTP Status: %{http_code}" -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer invalid-token-123" \
  -H "Content-Type: application/json")

echo "Response: $ME_RESPONSE_INVALID"

echo -e "\n========================================="
echo -e "${GREEN}Test complete${NC}"
echo "========================================="
