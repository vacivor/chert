import {
  startTransition,
  useEffect,
  useDeferredValue,
  useState,
  type ChangeEvent,
} from 'react'
import {
  AppWindow,
  ArrowDownAZ,
  ArrowUpAZ,
  RefreshCcw,
  SlidersHorizontal,
  TriangleAlert,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from '@/components/ui/empty'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { listApplications, type Application } from '@/lib/applications'

type AppType = 'all' | 'described' | 'undescribed'
type SortDirection = 'asc' | 'desc'
type LoadState = 'loading' | 'ready' | 'error'

const appTypeLabels: Record<AppType, string> = {
  all: 'All Apps',
  described: 'Described',
  undescribed: 'No Description',
}

export function ApplicationsPage() {
  const [applications, setApplications] = useState<Application[]>([])
  const [errorMessage, setErrorMessage] = useState('')
  const [loadState, setLoadState] = useState<LoadState>('loading')
  const [requestVersion, setRequestVersion] = useState(0)
  const [sort, setSort] = useState<SortDirection>('asc')
  const [appType, setAppType] = useState<AppType>('all')
  const [searchTerm, setSearchTerm] = useState('')
  const deferredSearchTerm = useDeferredValue(searchTerm)

  const filteredApps = applications
    .filter((app) =>
      appType === 'described'
        ? Boolean(app.description?.trim())
        : appType === 'undescribed'
          ? !app.description?.trim()
          : true,
    )
    .filter((app) =>
      [app.name, app.appId, app.description ?? '']
        .join(' ')
        .toLowerCase()
        .includes(deferredSearchTerm.trim().toLowerCase()),
    )
    .toSorted((a, b) =>
      sort === 'asc' ? a.name.localeCompare(b.name) : b.name.localeCompare(a.name),
    )

  useEffect(() => {
    const abortController = new AbortController()

    async function loadApplications() {
      setLoadState('loading')
      setErrorMessage('')

      try {
        const nextApplications = await listApplications({
          signal: abortController.signal,
        })

        setApplications(nextApplications)
        setLoadState('ready')
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        setErrorMessage(
          error instanceof Error ? error.message : 'Failed to load applications.',
        )
        setLoadState('error')
      }
    }

    void loadApplications()

    return () => {
      abortController.abort()
    }
  }, [requestVersion])

  const handleSearch = (event: ChangeEvent<HTMLInputElement>) => {
    const nextValue = event.target.value

    startTransition(() => {
      setSearchTerm(nextValue)
    })
  }

  const retryLoad = () => {
    setRequestVersion((currentVersion) => currentVersion + 1)
  }

  return (
    <section className='@7xl/content:mx-auto @7xl/content:w-full @7xl/content:max-w-7xl flex min-h-0 flex-1 flex-col'>
      <div>
        <h1 className='text-2xl font-bold tracking-tight'>Applications</h1>
        <p className='text-muted-foreground'>
          Manage the applications registered in the Chert console.
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
              <SelectItem value='described'>Described</SelectItem>
              <SelectItem value='undescribed'>No Description</SelectItem>
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

      <div className='min-h-0 flex-1 pt-4'>
        {loadState === 'loading' ? <ApplicationsSkeleton /> : null}

        {loadState === 'error' ? (
          <Empty className='min-h-full border'>
            <EmptyHeader>
              <EmptyMedia variant='icon'>
                <TriangleAlert />
              </EmptyMedia>
              <EmptyTitle>Unable to load applications</EmptyTitle>
              <EmptyDescription>{errorMessage}</EmptyDescription>
            </EmptyHeader>
            <Button type='button' variant='outline' onClick={retryLoad}>
              <RefreshCcw className='size-4' />
              Retry
            </Button>
          </Empty>
        ) : null}

        {loadState === 'ready' && filteredApps.length === 0 ? (
          <Empty className='min-h-full border'>
            <EmptyHeader>
              <EmptyMedia variant='icon'>
                <AppWindow />
              </EmptyMedia>
              <EmptyTitle>No applications found</EmptyTitle>
              <EmptyDescription>
                Try adjusting the search term or filter to find a different application.
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        ) : null}

        {loadState === 'ready' && filteredApps.length > 0 ? (
          <ul className='no-scrollbar faded-bottom grid h-full content-start gap-4 overflow-auto pb-16 md:grid-cols-2 lg:grid-cols-3'>
            {filteredApps.map((app) => (
              <li
                key={app.id}
                className='rounded-lg border p-4 transition-shadow hover:shadow-md'
              >
                <div className='mb-8 flex items-center justify-between gap-3'>
                  <div className='flex size-10 items-center justify-center rounded-lg bg-muted p-2 text-foreground'>
                    <AppWindow className='size-5' />
                  </div>

                  <Badge variant='secondary' className='font-mono text-[11px]'>
                    {app.appId}
                  </Badge>
                </div>

                <div>
                  <h2 className='mb-1 font-semibold'>{app.name}</h2>
                  <p className='line-clamp-2 min-h-10 text-sm text-muted-foreground'>
                    {app.description?.trim() || 'No description provided yet.'}
                  </p>
                </div>

                <div className='mt-4 flex items-center justify-between gap-3 text-xs text-muted-foreground'>
                  <span>ID #{app.id}</span>
                  <span>Updated {formatDateTime(app.updatedAt)}</span>
                </div>
              </li>
            ))}
          </ul>
        ) : null}
      </div>
    </section>
  )
}

function ApplicationsSkeleton() {
  return (
    <div className='grid h-full content-start gap-4 md:grid-cols-2 lg:grid-cols-3'>
      {Array.from({ length: 6 }, (_, index) => (
        <div key={index} className='rounded-lg border p-4'>
          <div className='mb-8 flex items-center justify-between gap-3'>
            <Skeleton className='size-10 rounded-lg' />
            <Skeleton className='h-5 w-20 rounded-full' />
          </div>
          <Skeleton className='mb-2 h-5 w-2/3' />
          <Skeleton className='mb-2 h-4 w-full' />
          <Skeleton className='h-4 w-5/6' />
          <div className='mt-4 flex items-center justify-between gap-3'>
            <Skeleton className='h-4 w-12' />
            <Skeleton className='h-4 w-24' />
          </div>
        </div>
      ))}
    </div>
  )
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
