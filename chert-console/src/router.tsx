/* eslint-disable react-refresh/only-export-components */
import {
  Navigate,
  Outlet,
  createRootRoute,
  createRoute,
  createRouter,
} from '@tanstack/react-router'
import { AppShell } from '@/components/layout/app-shell'
import { ApplicationsPage } from '@/pages/applications-page'
import { NotFoundPage } from '@/pages/not-found-page'

function RootRouteComponent() {
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

const applicationsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/applications',
  component: ApplicationsPage,
})

const routeTree = rootRoute.addChildren([indexRoute, applicationsRoute])

export const router = createRouter({
  routeTree,
  defaultPreload: 'intent',
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
