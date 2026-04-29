import * as React from 'react'
import { Header } from '@/components/layout/header.tsx'
import { TooltipProvider } from '@/components/ui/tooltip'

type AppShellProps = {
  children: React.ReactNode
}

export function AppShell({ children }: AppShellProps) {
  return (
    <div className='min-h-svh bg-background text-foreground'>
      <TooltipProvider delayDuration={0}>
        <div className='@container/content mx-auto flex min-h-svh w-full max-w-7xl flex-col'>
          <Header />
          <main
            data-layout='fixed'
            className='flex grow flex-col overflow-hidden px-4 py-6 md:px-6'
          >
            {children}
          </main>
        </div>
      </TooltipProvider>
    </div>
  )
}
