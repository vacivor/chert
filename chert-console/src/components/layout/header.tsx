import { useEffect, useState } from 'react'
import { Link } from '@tanstack/react-router'
import { ChevronRight } from 'lucide-react'
import { useHeaderBreadcrumbsContext } from '@/components/layout/header-breadcrumbs'
import { SidebarTrigger } from '@/components/ui/sidebar'
import { UserMenu } from '@/components/layout/user-menu'
import { ThemeToggle } from '@/components/layout/theme-toggle'
import { cn } from '@/lib/utils'

export function Header() {
  const [offset, setOffset] = useState(0)
  const { items } = useHeaderBreadcrumbsContext()

  useEffect(() => {
    const onScroll = () => {
      setOffset(document.body.scrollTop || document.documentElement.scrollTop)
    }

    document.addEventListener('scroll', onScroll, { passive: true })

    return () => document.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <header
      className={cn(
        'sticky top-0 z-50 h-16',
        offset > 10 ? 'shadow' : 'shadow-none',
      )}
    >
      <div
        className={cn(
          'relative flex h-full items-center justify-between gap-4 px-4 sm:px-6',
          offset > 10 &&
            'after:absolute after:inset-0 after:-z-10 after:bg-background/20 after:backdrop-blur-lg',
        )}
      >
        <div className='flex min-w-0 items-center gap-3'>
          <SidebarTrigger className='size-9 rounded-xl border' />
          {items.length > 0 ? (
            <nav className='flex min-w-0 items-center gap-1.5 text-sm'>
              {items.map((item, index) => (
                <div key={`${item.label}-${index}`} className='flex min-w-0 items-center gap-1.5'>
                  {index > 0 ? <ChevronRight className='size-4 shrink-0 text-muted-foreground' /> : null}
                  {item.href ? (
                    <Link
                      to={item.href as never}
                      className={cn(
                        'truncate',
                        index === items.length - 1
                          ? 'font-medium text-foreground'
                          : 'text-muted-foreground hover:text-foreground',
                      )}
                    >
                      {item.label}
                    </Link>
                  ) : (
                    <span
                      className={cn(
                        'truncate',
                        index === items.length - 1
                          ? 'font-medium text-foreground'
                          : 'text-muted-foreground',
                      )}
                    >
                      {item.label}
                    </span>
                  )}
                </div>
              ))}
            </nav>
          ) : (
            <div className='min-w-0'>
              <p className='truncate text-sm font-semibold'>Chert Console</p>
              <p className='truncate text-xs text-muted-foreground'>
                Configuration governance
              </p>
            </div>
          )}
        </div>

        <div className='flex items-center gap-3'>
          <ThemeToggle />
          <UserMenu />
        </div>
      </div>
    </header>
  )
}
