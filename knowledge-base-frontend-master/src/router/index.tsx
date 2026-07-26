import React, { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate, RouteObject } from 'react-router-dom';
import { MainLayout, AuthLayout } from '@/components/layout';
import { Spin } from 'antd';
import { tokenStorage } from '@/utils/token-storage';
import type { User } from '@/types';
import { ADMIN_PERMISSION_CODES, PERMISSIONS, hasAdminAccess, hasAllPermissions, hasAnyPermission } from '@/utils/permission';
import KnowledgeGraphPage from '@/pages/KnowledgeGraphPage';

// Lazy-loaded page components
const LoginPage = lazy(() => import('@/pages/LoginPage'));
const ActivateAccountPage = lazy(() => import('@/pages/ActivateAccountPage'));
const ForgotPasswordPage = lazy(() => import('@/pages/ForgotPasswordPage'));
const DashboardPage = lazy(() => import('@/pages/DashboardPage'));
const ShareViewerPage = lazy(() => import('@/pages/ShareViewerPage'));
const DocumentsPage = lazy(() => import('@/pages/DocumentsPage'));
const DraftsPage = lazy(() => import('@/pages/DraftsPage'));
const CreateDocumentPage = lazy(() => import('@/pages/CreateDocumentPage'));
const DocumentDetailPage = lazy(() => import('@/pages/DocumentDetailPage'));
const DocumentReviewWorkspacePage = lazy(() => import('@/pages/DocumentReviewWorkspacePage'));
const EditDocumentPage = lazy(() => import('@/pages/EditDocumentPage'));
const DocumentVersionsPage = lazy(() => import('@/pages/DocumentVersionsPage'));
const AutoSaveHistoryPage = lazy(() => import('@/pages/AutoSaveHistoryPage'));
const ImportDocumentPage = lazy(() => import('@/pages/ImportDocumentPage'));
const ExportDataPage = lazy(() => import('@/pages/ExportDataPage'));
const FileManagementPage = lazy(() => import('@/pages/FileManagementPage'));
const SearchPage = lazy(() => import('@/pages/SearchPage'));
const AIAssistantPage = lazy(() => import('@/pages/AIAssistantPage'));
const AIWritingPage = lazy(() => import('@/pages/AIWritingPage'));
const FavoritesPage = lazy(() => import('@/pages/FavoritesPage'));
const MyDocumentsPage = lazy(() => import('@/pages/MyDocumentsPage'));
const RecentAccessPage = lazy(() => import('@/pages/RecentAccessPage'));
const ProfilePage = lazy(() => import('@/pages/ProfilePage'));
const NotificationCenterPage = lazy(() => import('@/pages/NotificationCenterPage'));
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'));

// Admin pages
const AdminCenterPage = lazy(() => import('@/pages/admin/AdminCenterPage'));
const UsersManagementPage = lazy(() => import('@/pages/admin/UsersManagementPage'));
const PermissionsPage = lazy(() => import('@/pages/admin/PermissionsPage'));
const CategoriesPage = lazy(() => import('@/pages/admin/CategoriesPage'));
const TeamsPage = lazy(() => import('@/pages/admin/TeamsPage'));
const ReviewPage = lazy(() => import('@/pages/admin/ReviewPage'));
const StatisticsPage = lazy(() => import('@/pages/admin/StatisticsPage'));
const SettingsPage = lazy(() => import('@/pages/admin/SettingsPage'));

// Base service admin pages
const SystemConfigPage = lazy(() => import('@/pages/admin/SystemConfigPage'));
const DictionaryManagePage = lazy(() => import('@/pages/admin/DictionaryManagePage'));
const OperationLogPage = lazy(() => import('@/pages/admin/OperationLogPage'));
const NotificationTemplatePage = lazy(() => import('@/pages/admin/NotificationTemplatePage'));
const RolesPage = lazy(() => import('@/pages/admin/RolesPage'));

// Loading component
const LoadingFallback = () => (
  <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
    <Spin size="large" />
  </div>
);

// Route guard component
interface ProtectedRouteProps {
  children: React.ReactElement;
  requireAdmin?: boolean;
  requiredPermissions?: string[];
  requireAllPermissions?: boolean;
}

const getStoredUser = (): User | null => {
  const userInfo = tokenStorage.getUserInfo();
  if (!userInfo) {
    return null;
  }
  return {
    id: userInfo.userId,
    username: userInfo.username,
    nickname: userInfo.nickname,
    email: userInfo.email || '',
    phone: userInfo.phone || undefined,
    avatar: userInfo.avatar,
    role: userInfo.role,
    roles: userInfo.roles || (userInfo.role ? [userInfo.role] : []),
    permissions: userInfo.permissions || [],
    status: userInfo.status ?? 1,
  };
};

const ProtectedRoute = ({
  children,
  requireAdmin = false,
  requiredPermissions = [],
  requireAllPermissions = false,
}: ProtectedRouteProps) => {
  // 🔑 Prefer the token from the cookie, then fall back to localStorage
  const token = tokenStorage.getAccessToken() || localStorage.getItem('token');
  const user = getStoredUser();

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (requireAdmin && !hasAdminAccess(user)) {
    return <Navigate to="/" replace />;
  }

  if (requiredPermissions.length > 0) {
    const hasAccess = requireAllPermissions
      ? hasAllPermissions(user, requiredPermissions)
      : hasAnyPermission(user, requiredPermissions);

    if (!hasAccess) {
      return <Navigate to="/" replace />;
    }
  }

  return children;
};

// Route configuration
const routes: RouteObject[] = [
  {
    path: '/share/:shareId',
    element: (
      <Suspense fallback={<LoadingFallback />}>
        <ShareViewerPage />
      </Suspense>
    ),
  },
  {
    path: '/login',
    element: (
      <Suspense fallback={<LoadingFallback />}>
        <AuthLayout>
          <LoginPage />
        </AuthLayout>
      </Suspense>
    ),
  },
  {
    path: '/forgot-password',
    element: (
      <Suspense fallback={<LoadingFallback />}>
        <AuthLayout>
          <ForgotPasswordPage />
        </AuthLayout>
      </Suspense>
    ),
  },
  {
    path: '/activate',
    element: (
      <Suspense fallback={<LoadingFallback />}>
        <AuthLayout>
          <ActivateAccountPage />
        </AuthLayout>
      </Suspense>
    ),
  },
  {
    path: '/documents/:id',
    element: (
      <ProtectedRoute>
        <Suspense fallback={<LoadingFallback />}>
          <DocumentDetailPage />
        </Suspense>
      </ProtectedRoute>
    ),
  },
  {
    path: '/review/documents/:documentId',
    element: (
      <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.documentReview]}>
        <Suspense fallback={<LoadingFallback />}>
          <DocumentReviewWorkspacePage />
        </Suspense>
      </ProtectedRoute>
    ),
  },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <MainLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <DashboardPage />
          </Suspense>
        ),
      },
      {
        path: 'documents',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <DocumentsPage />
          </Suspense>
        ),
      },
      {
        path: 'drafts',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <DraftsPage />
          </Suspense>
        ),
      },
      {
        path: 'my-documents',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <MyDocumentsPage />
          </Suspense>
        ),
      },
      {
        path: 'documents/import',
        element: (
          <ProtectedRoute requiredPermissions={[PERMISSIONS.documentCreate]}>
            <Suspense fallback={<LoadingFallback />}>
              <ImportDocumentPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'documents/new',
        element: (
          <ProtectedRoute requiredPermissions={[PERMISSIONS.documentCreate]}>
            <Suspense fallback={<LoadingFallback />}>
              <CreateDocumentPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'documents/export',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <ExportDataPage />
          </Suspense>
        ),
      },
      {
        path: 'files',
        element: (
          <ProtectedRoute requiredPermissions={[PERMISSIONS.documentList]}>
            <Suspense fallback={<LoadingFallback />}>
              <FileManagementPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'documents/:id/edit',
        element: (
          <ProtectedRoute requiredPermissions={[PERMISSIONS.documentEdit]}>
            <Suspense fallback={<LoadingFallback />}>
              <EditDocumentPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'documents/:id/edit',
        element: (
          <ProtectedRoute requiredPermissions={[PERMISSIONS.documentEdit]}>
            <Suspense fallback={<LoadingFallback />}>
              <EditDocumentPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'documents/:id/versions',
        element: (
          <ProtectedRoute requiredPermissions={[PERMISSIONS.documentVersion]}>
            <Suspense fallback={<LoadingFallback />}>
              <DocumentVersionsPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'documents/:id/autosave-history',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <AutoSaveHistoryPage />
          </Suspense>
        ),
      },
      {
        path: 'search',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <SearchPage />
          </Suspense>
        ),
      },
      {
        path: 'knowledge-graph',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <KnowledgeGraphPage />
          </Suspense>
        ),
      },
      {
        path: 'ai',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <AIAssistantPage />
          </Suspense>
        ),
      },
      {
        path: 'ai-writing',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <AIWritingPage />
          </Suspense>
        ),
      },
      {
        path: 'favorites',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <FavoritesPage />
          </Suspense>
        ),
      },
      {
        path: 'recent-access',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <RecentAccessPage />
          </Suspense>
        ),
      },
      {
        path: 'notifications',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <NotificationCenterPage />
          </Suspense>
        ),
      },
      {
        path: 'profile',
        element: (
          <Suspense fallback={<LoadingFallback />}>
            <ProfilePage />
          </Suspense>
        ),
      },
      {
        path: 'admin',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={ADMIN_PERMISSION_CODES}>
            <Suspense fallback={<LoadingFallback />}>
              <AdminCenterPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/users',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.systemUser]}>
            <Suspense fallback={<LoadingFallback />}>
              <UsersManagementPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/permissions',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.systemPermission]}>
            <Suspense fallback={<LoadingFallback />}>
              <PermissionsPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/categories',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.documentCategory, PERMISSIONS.documentCategoryQuery]}>
            <Suspense fallback={<LoadingFallback />}>
              <CategoriesPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/teams',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.systemTeam]}>
            <Suspense fallback={<LoadingFallback />}>
              <TeamsPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/review',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.documentReview]}>
            <Suspense fallback={<LoadingFallback />}>
              <ReviewPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/statistics',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.systemStatistics]}>
            <Suspense fallback={<LoadingFallback />}>
              <StatisticsPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/settings',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.systemSettings]}>
            <Suspense fallback={<LoadingFallback />}>
              <SettingsPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      // Base service admin routes (standalone pages)
      {
        path: 'admin/system-config',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.systemSettings]}>
            <Suspense fallback={<LoadingFallback />}>
              <SystemConfigPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/dictionary',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.systemSettings]}>
            <Suspense fallback={<LoadingFallback />}>
              <DictionaryManagePage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/operation-logs',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.systemSettings]}>
            <Suspense fallback={<LoadingFallback />}>
              <OperationLogPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/notification-templates',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.systemSettings]}>
            <Suspense fallback={<LoadingFallback />}>
              <NotificationTemplatePage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/roles',
        element: (
          <ProtectedRoute requireAdmin requiredPermissions={[PERMISSIONS.systemRole]}>
            <Suspense fallback={<LoadingFallback />}>
              <RolesPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
    ],
  },
  {
    path: '*',
    element: (
      <Suspense fallback={<LoadingFallback />}>
        <NotFoundPage />
      </Suspense>
    ),
  },
];

export const router = createBrowserRouter(routes);

export default router;
