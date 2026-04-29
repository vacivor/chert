import * as React from 'react'
import { AppSidebar } from '@/components/layout/app-sidebar'
import { Header } from '@/components/layout/header.tsx'
import { SidebarInset, SidebarProvider } from '@/components/ui/sidebar'
import { TooltipProvider } from '@/components/ui/tooltip'

type AppShellProps = {
  children: React.ReactNode
}

export function AppShell({ children }: AppShellProps) {
  return (
    <div className='min-h-svh bg-background text-foreground'>
      <TooltipProvider delayDuration={0}>
        <SidebarProvider defaultOpen>
          <AppSidebar />
          <SidebarInset>
            <div className='@container/content flex min-h-svh flex-col'>
              <Header />
              <main
                data-layout='fixed'
                className='flex grow flex-col overflow-hidden px-4 py-6 md:px-6'
              >
                {children}
              </main>
            </div>
          </SidebarInset>
        </SidebarProvider>
      </TooltipProvider>
    </div>
  )
}
