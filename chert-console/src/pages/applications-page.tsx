import {
  startTransition,
  useDeferredValue,
  useState,
  type ChangeEvent,
} from 'react'
import { ArrowDownAZ, ArrowUpAZ, SlidersHorizontal } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select'
import { Separator } from '@/components/ui/separator'
import { applicationIntegrations } from '@/pages/data/application-integrations'
import { cn } from '@/lib/utils'

type AppType = 'all' | 'connected' | 'notConnected'
type SortDirection = 'asc' | 'desc'

const appTypeLabels: Record<AppType, string> = {
  all: 'All Apps',
  connected: 'Connected',
  notConnected: 'Not Connected',
}

export function ApplicationsPage() {
  const [sort, setSort] = useState<SortDirection>('asc')
  const [appType, setAppType] = useState<AppType>('all')
  const [searchTerm, setSearchTerm] = useState('')
  const deferredSearchTerm = useDeferredValue(searchTerm)

  const filteredApps = applicationIntegrations
    .toSorted((a, b) =>
      sort === 'asc' ? a.name.localeCompare(b.name) : b.name.localeCompare(a.name),
    )
    .filter((app) =>
      appType === 'connected'
        ? app.connected
        : appType === 'notConnected'
          ? !app.connected
          : true,
    )
    .filter((app) =>
      app.name.toLowerCase().includes(deferredSearchTerm.trim().toLowerCase()),
    )

  const handleSearch = (event: ChangeEvent<HTMLInputElement>) => {
    const nextValue = event.target.value

    startTransition(() => {
      setSearchTerm(nextValue)
    })
  }

  return (
    <section className='@7xl/content:mx-auto @7xl/content:w-full @7xl/content:max-w-7xl flex min-h-0 flex-1 flex-col'>
      <div>
        <h1 className='text-2xl font-bold tracking-tight'>App Integrations</h1>
        <p className='text-muted-foreground'>
          Here&apos;s a list of your apps available for integration with the console.
        </p>
      </div>

      <div className='my-4 flex items-end justify-between sm:my-0 sm:items-center'>
        <div className='flex flex-col gap-4 sm:my-4 sm:flex-row'>
          <Input
            placeholder='Filter apps...'
            className='h-9 w-full sm:w-40 lg:w-62.5'
            value={searchTerm}
            onChange={handleSearch}
          />

          <Select value={appType} onValueChange={(value) => setAppType(value as AppType)}>
            <SelectTrigger className='h-9 w-full sm:w-36'>
              {appTypeLabels[appType]}
            </SelectTrigger>
            <SelectContent>
              <SelectItem value='all'>All Apps</SelectItem>
              <SelectItem value='connected'>Connected</SelectItem>
              <SelectItem value='notConnected'>Not Connected</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <Select
          value={sort}
          onValueChange={(value) => setSort(value as SortDirection)}
        >
          <SelectTrigger className='h-9 w-16' aria-label='Sort integrations'>
            <SlidersHorizontal className='size-4.5' />
          </SelectTrigger>
          <SelectContent align='end'>
            <SelectItem value='asc'>
              <div className='flex items-center gap-4'>
                <ArrowUpAZ className='size-4' />
                <span>Ascending</span>
              </div>
            </SelectItem>
            <SelectItem value='desc'>
              <div className='flex items-center gap-4'>
                <ArrowDownAZ className='size-4' />
                <span>Descending</span>
              </div>
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      <Separator className='shadow-sm' />

      <ul className='no-scrollbar faded-bottom grid min-h-0 flex-1 content-start gap-4 overflow-auto pt-4 pb-16 md:grid-cols-2 lg:grid-cols-3'>
        {filteredApps.map((app) => (
          <li
            key={app.name}
            className='rounded-lg border p-4 transition-shadow hover:shadow-md'
          >
            <div className='mb-8 flex items-center justify-between gap-3'>
              <div className='flex size-10 items-center justify-center rounded-lg bg-muted p-2 text-foreground'>
                <app.icon className='size-5' />
              </div>

              <Button
                type='button'
                variant='outline'
                size='sm'
                className={cn(
                  app.connected &&
                    'border-primary/20 bg-primary/10 text-primary hover:bg-primary/15 hover:text-primary',
                )}
              >
                {app.connected ? 'Connected' : 'Connect'}
              </Button>
            </div>

            <div>
              <h2 className='mb-1 font-semibold'>{app.name}</h2>
              <p className='line-clamp-2 text-sm text-muted-foreground'>
                {app.description}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
