import { useEffect, useState } from 'react'
import { SidebarTrigger } from '@/components/ui/sidebar'
import { UserMenu } from '@/components/layout/user-menu'
import { ThemeToggle } from '@/components/layout/theme-toggle'
import { cn } from '@/lib/utils'

export function Header() {
  const [offset, setOffset] = useState(0)

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
          <div className='min-w-0'>
            <p className='truncate text-sm font-semibold'>Chert Console</p>
            <p className='truncate text-xs text-muted-foreground'>
              Configuration governance
            </p>
          </div>
        </div>

        <div className='flex items-center gap-3'>
          <ThemeToggle />
          <UserMenu />
        </div>
      </div>
    </header>
  )
}
