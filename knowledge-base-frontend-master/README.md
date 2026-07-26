# Enterprise Knowledge Base Frontend System

A modern enterprise knowledge base management system built with Vite + React 18 + TypeScript + Ant Design 5.

## Tech Stack

- **Build tool**: Vite 5
- **Framework**: React 18
- **Language**: TypeScript 5
- **UI component library**: Ant Design 5
- **Routing**: React Router 6
- **HTTP client**: Axios
- **State management**: Zustand 4
- **Charts**: @ant-design/charts
- **Markdown**: react-markdown
- **Date handling**: dayjs

## Features

### User Authentication
- User login/registration
- JWT Token authentication
- Profile management
- Avatar upload

### Document Management
- Document list view
- Document creation/editing
- Markdown editor (live preview)
- Document category management
- Tag management
- Search and filtering
- Document import (supports PDF, Word, Excel, PPT, TXT, MD)
- Document export
- Version history

### AI Assistant
- Intelligent Q&A
- Streaming responses
- Conversation history
- Context understanding
- Knowledge reference display
- Quick questions

### Search
- Full-text search
- Semantic search
- Search suggestions
- Advanced filtering
- Search history

### Knowledge Graph
- Force-directed graph visualization
- Node type differentiation
- Graph interaction (drag, zoom, click)
- Graph search
- Type filtering

### Admin Center
- User management
- Role management
- Permission management
- Category management
- Team management
- System configuration

### Data Statistics
- Document statistics
- User statistics
- Trend charts
- Category distribution

## Getting Started

### Install Dependencies

```bash
npm install
```

### Development Mode

```bash
npm run dev
```

Visit `http://localhost:5173`

### Build for Production

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

## Project Structure

```
knowledge-base-frontend/
├── public/                 # Static assets
├── src/
│   ├── components/         # Components
│   │   ├── common/        # Common components
│   │   ├── layout/        # Layout components
│   │   ├── documents/     # Document components
│   │   ├── auth/          # Auth components
│   │   ├── dashboard/     # Dashboard components
│   │   ├── ai/            # AI components
│   │   └── profile/       # Profile components
│   ├── pages/             # Pages
│   │   ├── admin/         # Admin pages
│   │   │   ├── UsersPage.tsx
│   │   │   ├── CategoriesPage.tsx
│   │   │   ├── RolesPage.tsx
│   │   │   └── index.ts
│   │   ├── LoginPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── DocumentsPage.tsx
│   │   ├── CreateDocumentPage.tsx
│   │   ├── EditDocumentPage.tsx
│   │   ├── DocumentDetailPage.tsx
│   │   ├── ImportDocumentPage.tsx
│   │   ├── SearchPage.tsx
│   │   ├── KnowledgeGraphPage.tsx
│   │   ├── AIAssistantPage.tsx
│   │   ├── ProfilePage.tsx
│   │   ├── AdminCenterPage.tsx
│   │   └── index.tsx
│   ├── services/          # API services
│   ├── stores/            # State management
│   ├── types/             # Type definitions
│   ├── utils/             # Utility functions
│   ├── hooks/             # Custom hooks
│   ├── router/            # Router configuration
│   ├── styles/            # Global styles
│   ├── App.tsx            # App entry
│   └── main.tsx           # Main file
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## Environment Variables

Create a `.env` file:

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=Enterprise Knowledge Base
VITE_APP_VERSION=1.0.0
```

## Design System

The project uses an enterprise-grade design system, including:

- **Color system**: A theme built around blue (#2563eb)
- **Typography**: System font stack
- **Spacing**: 4px base unit
- **Border radius**: 8px / 12px / 16px
- **Shadows**: Multi-layer shadow system
- **Animation**: 150-300ms transition effects

## Browser Support

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

## Development Guidelines

### TypeScript Strict Mode
The project enables TypeScript strict mode to ensure type safety.

### Component Conventions
- Use function components + Hooks
- Component filenames use PascalCase
- Props are defined using TypeScript interfaces

### Code Style
- Use ESLint for code linting
- Follow the Airbnb code style guide
- Use Prettier for code formatting

## Deployment

### Docker Deployment

```bash
docker build -t knowledge-base-frontend .
docker run -p 5173:5173 knowledge-base-frontend
```

### Nginx Configuration

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## License

MIT License
