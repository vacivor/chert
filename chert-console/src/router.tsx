/* eslint-disable react-refresh/only-export-components */
import {
  Navigate,
  Outlet,
  createRootRoute,
  createRoute,
  createRouter,
} from '@tanstack/react-router'
import { AppShell } from '@/components/layout/app-shell'
import { useAuth } from '@/providers/auth-provider'
import { ApplicationsPage } from '@/pages/applications-page'
import { ApplicationDetailPage } from '@/pages/application-detail-page'
import { ConfigContentEditorPage } from '@/pages/config-content-editor-page'
import { EnvironmentsPage } from '@/pages/environments-page'
import { LoginPage } from '@/pages/login-page'
import { NotFoundPage } from '@/pages/not-found-page'
import { UsersPage } from '@/pages/users-page'

function RootRouteComponent() {
  return <Outlet />
}

function ProtectedRouteComponent() {
  const { status } = useAuth()

  if (status === 'loading') {
    return (
      <section className='flex min-h-screen items-center justify-center'>
        <p className='text-sm text-muted-foreground'>Loading console…</p>
      </section>
    )
  }

  if (status === 'unauthenticated') {
    return <Navigate to='/login' replace />
  }

  return (
    <AppShell>
      <Outlet />
    </AppShell>
  )
}

function IndexRedirect() {
  return <Navigate to='/applications' replace />
}

const rootRoute = createRootRoute({
  component: RootRouteComponent,
  notFoundComponent: NotFoundPage,
})

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: IndexRedirect,
})

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  component: LoginPage,
})

const protectedRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: '_protected',
  component: ProtectedRouteComponent,
})

const applicationsRoute = createRoute({
  getParentRoute: () => protectedRoute,
  path: '/applications',
  component: ApplicationsPage,
})

const applicationDetailRoute = createRoute({
  getParentRoute: () => protectedRoute,
  path: '/applications/$applicationId',
  component: ApplicationDetailPage,
})

const configContentEditorRoute = createRoute({
  getParentRoute: () => protectedRoute,
  path: '/applications/$applicationId/resources/$resourceId/content',
  component: ConfigContentEditorPage,
})

const environmentsRoute = createRoute({
  getParentRoute: () => protectedRoute,
  path: '/environments',
  component: EnvironmentsPage,
})

const usersRoute = createRoute({
  getParentRoute: () => protectedRoute,
  path: '/users',
  component: UsersPage,
})

const routeTree = rootRoute.addChildren([
  indexRoute,
  loginRoute,
  protectedRoute.addChildren([
    applicationsRoute,
    applicationDetailRoute,
    configContentEditorRoute,
    environmentsRoute,
    usersRoute,
  ]),
])

export const router = createRouter({
  routeTree,
  defaultPreload: 'intent',
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
