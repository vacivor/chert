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
        'sticky top-0 z-50 h-16 w-[inherit]',
        offset > 10 ? 'shadow' : 'shadow-none',
      )}
    >
      <div
        className={cn(
          'relative flex h-full items-center gap-3 p-4 sm:gap-4',
          offset > 10 &&
            'after:absolute after:inset-0 after:-z-10 after:bg-background/20 after:backdrop-blur-lg',
        )}
      >
        <SidebarTrigger variant='outline' className='max-md:scale-125' />

        <div className='ms-auto flex items-center gap-4'>
          <ThemeToggle />
          <UserMenu />
        </div>
      </div>
    </header>
  )
}
