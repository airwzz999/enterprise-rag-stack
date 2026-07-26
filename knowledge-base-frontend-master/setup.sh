#!/bin/bash

# Knowledge base frontend project startup script

set -e

echo "🚀 Enterprise Knowledge Base frontend startup script"
echo "================================"

# Check Node.js version
echo "📋 Checking Node.js version..."
NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
    echo "❌ Node.js version is too low, 18.x or higher is required"
    echo "Current version: $(node -v)"
    exit 1
fi
echo "✅ Node.js version check passed: $(node -v)"

# Check npm version
echo "📋 Checking npm version..."
NPM_VERSION=$(npm -v | cut -d'.' -f1)
if [ "$NPM_VERSION" -lt 9 ]; then
    echo "⚠️  npm version is low, 9.x or higher is recommended"
    echo "Current version: $(npm -v)"
fi
echo "✅ npm version: $(npm -v)"

# Check whether dependencies are installed
if [ ! -d "node_modules" ]; then
    echo "📦 Installing project dependencies..."
    npm install
    echo "✅ Dependencies installed"
else
    echo "✅ Dependencies already installed"
fi

# Check for the environment variable file
if [ ! -f ".env" ]; then
    echo "⚠️  .env file not found, creating default configuration..."
    cat > .env << EOF
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=Enterprise Knowledge Base
VITE_APP_VERSION=1.0.0
EOF
    echo "✅ Environment variable file created"
fi

# Ask the user what to do
echo ""
echo "Please choose an action:"
echo "1) Start the dev server"
echo "2) Build the production version"
echo "3) Preview the production build"
echo "4) Type check"
echo "5) Lint"
echo "6) Exit"
read -p "Enter an option (1-6): " choice

case $choice in
    1)
        echo ""
        echo "🚀 Starting the dev server..."
        echo "URL: http://localhost:5173"
        echo "Press Ctrl+C to stop the server"
        echo ""
        npm run dev
        ;;
    2)
        echo ""
        echo "🔨 Building the production version..."
        npm run build
        echo "✅ Build complete, output is in the dist directory"
        ;;
    3)
        echo ""
        echo "👀 Previewing the production build..."
        if [ -d "dist" ]; then
            npm run preview
        else
            echo "❌ dist directory does not exist, please build the project first"
            exit 1
        fi
        ;;
    4)
        echo ""
        echo "🔍 Type checking..."
        npm run type-check
        ;;
    5)
        echo ""
        echo "🧹 Linting..."
        npm run lint
        ;;
    6)
        echo "👋 Goodbye!"
        exit 0
        ;;
    *)
        echo "❌ Invalid option"
        exit 1
        ;;
esac
