import { Link, useRouterState } from '@tanstack/react-router'
import { cn } from '@/lib/utils'
import { useAuth } from '@/providers/auth-provider'

type NavigationItem = {
  label: string
  to: '/applications' | '/environments' | '/users'
  permission?: string
}

const navigationItems: NavigationItem[] = [
  {
    label: 'Applications',
    to: '/applications',
  },
  {
    label: 'Environments',
    to: '/environments',
  },
  {
    label: 'Users',
    to: '/users',
    permission: 'user:manage',
  },
]

export function TopNavigation() {
  const pathname = useRouterState({
    select: (state) => state.location.pathname,
  })
  const { user } = useAuth()

  return (
    <nav className='flex items-center gap-1 overflow-x-auto'>
      {navigationItems
        .filter((item) => !item.permission || user?.permissions.includes(item.permission))
        .map((item) => (
          <Link
            key={item.to}
            to={item.to}
            preload='intent'
            className={cn(
              'rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-foreground',
              pathname === item.to && 'bg-accent text-foreground',
            )}
          >
            {item.label}
          </Link>
        ))}
    </nav>
  )
}
