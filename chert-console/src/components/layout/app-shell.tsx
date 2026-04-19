import * as React from 'react'
import { AppSidebar } from '@/components/layout/app-sidebar'
import { Header } from '@/components/layout/header.tsx'
import { SidebarInset, SidebarProvider } from '@/components/ui/sidebar'
import { TooltipProvider } from '@/components/ui/tooltip'

type AppShellProps = {
  children: React.ReactNode
}

function getDefaultSidebarOpen() {
  if (typeof document === 'undefined') {
    return true
  }

  return !document.cookie.includes('sidebar_state=false')
}

export function AppShell({ children }: AppShellProps) {
  return (
    <div className='min-h-svh bg-background text-foreground'>
      <TooltipProvider delayDuration={0}>
        <SidebarProvider defaultOpen={getDefaultSidebarOpen()}>
          <AppSidebar />
          <SidebarInset className='@container/content has-data-[layout=fixed]:h-svh'>
            <Header />
            <main
              data-layout='fixed'
              className='flex grow flex-col overflow-hidden px-4 py-6 pt-0'
            >
              {children}
            </main>
          </SidebarInset>
        </SidebarProvider>
      </TooltipProvider>
    </div>
  )
}
