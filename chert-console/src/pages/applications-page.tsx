import { Link } from '@tanstack/react-router'
import {
  startTransition,
  useDeferredValue,
  useEffect,
  useState,
  type ChangeEvent,
  type FormEvent,
} from 'react'
import {
  AppWindow,
  ArrowDownAZ,
  ArrowUpAZ,
  CircleUserRound,
  GitPullRequestArrow,
  LoaderCircle,
  Plus,
  RefreshCcw,
  ShieldCheck,
  SlidersHorizontal,
  TriangleAlert,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from '@/components/ui/empty'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { useHeaderBreadcrumbs } from '@/components/layout/header-breadcrumbs'
import { useAuth } from '@/providers/auth-provider'
import { createApplication, listApplications, type Application } from '@/lib/applications'

type AppType = 'all' | 'described' | 'undescribed'
type SortDirection = 'asc' | 'desc'
type LoadState = 'loading' | 'ready' | 'error'

const sortLabels: Record<SortDirection, string> = {
  asc: 'Ascending',
  desc: 'Descending',
}

export function ApplicationsPage() {
  useHeaderBreadcrumbs([{ label: 'Applications' }])
  const { user } = useAuth()
  const [applications, setApplications] = useState<Application[]>([])
  const [errorMessage, setErrorMessage] = useState('')
  const [createErrorMessage, setCreateErrorMessage] = useState('')
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [isCreateSubmitting, setIsCreateSubmitting] = useState(false)
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

  const handleCreateApplication = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!user) {
      setCreateErrorMessage('You need an authenticated session to create an application.')
      return
    }

    setCreateErrorMessage('')
    setIsCreateSubmitting(true)

    const formData = new FormData(event.currentTarget)

    try {
      const application = await createApplication({
        appId: String(formData.get('appId') ?? ''),
        name: String(formData.get('name') ?? ''),
        description: String(formData.get('description') ?? ''),
        ownerUserId: user.id,
        maintainerUserId: user.id,
        developerUserIds: [],
      })

      setApplications((current) =>
        current.concat(application).toSorted((a, b) => a.name.localeCompare(b.name)),
      )
      setIsCreateOpen(false)
      event.currentTarget.reset()
    } catch (error) {
      setCreateErrorMessage(
        error instanceof Error ? error.message : 'Failed to create application.',
      )
    } finally {
      setIsCreateSubmitting(false)
    }
  }

  return (
    <section className='@7xl/content:mx-auto @7xl/content:w-full @7xl/content:max-w-7xl flex min-h-0 flex-1 flex-col'>
      <div className='flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between'>
        <div>
          <h1 className='text-2xl font-bold tracking-tight'>Applications</h1>
          <p className='text-muted-foreground'>
            Browse applications, then enter each application to manage resources and publish rules.
          </p>
        </div>

        <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
          <DialogTrigger asChild>
            <Button type='button'>
              <Plus className='size-4' />
              New Application
            </Button>
          </DialogTrigger>
          <DialogContent className='sm:max-w-lg'>
            <DialogHeader>
              <DialogTitle>Create application</DialogTitle>
              <DialogDescription>
                The current user becomes both owner and maintainer. You can add more members later.
              </DialogDescription>
            </DialogHeader>
            <form className='flex flex-col gap-4' onSubmit={handleCreateApplication}>
              <div className='flex flex-col gap-2'>
                <Label htmlFor='application-name'>Name</Label>
                <Input id='application-name' name='name' placeholder='Order Service' required />
              </div>

              <div className='flex flex-col gap-2'>
                <Label htmlFor='application-app-id'>App ID</Label>
                <Input id='application-app-id' name='appId' placeholder='order-service' required />
              </div>

              <div className='flex flex-col gap-2'>
                <Label htmlFor='application-description'>Description</Label>
                <Input
                  id='application-description'
                  name='description'
                  placeholder='Handles checkout and order placement'
                />
              </div>

              {createErrorMessage ? (
                <p className='text-sm text-destructive'>{createErrorMessage}</p>
              ) : null}

              <DialogFooter>
                <Button type='submit' disabled={isCreateSubmitting}>
                  {isCreateSubmitting ? <LoaderCircle className='size-4 animate-spin' /> : null}
                  Create
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
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
            <SelectTrigger className='h-9 w-full sm:w-40'>
              <SelectValue placeholder='Filter type' />
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                <SelectItem value='all'>All Apps</SelectItem>
                <SelectItem value='described'>Described</SelectItem>
                <SelectItem value='undescribed'>No Description</SelectItem>
              </SelectGroup>
            </SelectContent>
          </Select>
        </div>

        <Select value={sort} onValueChange={(value) => setSort(value as SortDirection)}>
          <SelectTrigger className='h-9 w-[152px]' aria-label='Sort applications'>
            <div className='flex items-center gap-2'>
              <SlidersHorizontal className='size-4.5 text-muted-foreground' />
              <span>{sortLabels[sort]}</span>
            </div>
          </SelectTrigger>
          <SelectContent align='end'>
            <SelectGroup>
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
            </SelectGroup>
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
              <li key={app.id}>
                <Card className='h-full rounded-2xl border border-border/60 ring-0 shadow-none transition-colors hover:border-border'>
                  <CardHeader className='gap-0 pb-4'>
                    <div className='flex items-start justify-between gap-4'>
                      <div className='flex min-w-0 items-start gap-3'>
                        <div className='flex size-10 shrink-0 items-center justify-center rounded-xl border border-border/50 bg-background text-foreground'>
                          <AppWindow className='size-5' />
                        </div>

                        <div className='min-w-0'>
                          <CardTitle className='truncate text-lg'>{app.name}</CardTitle>
                          <CardDescription className='pt-1 font-normal text-sm'>
                            {app.appId}
                          </CardDescription>
                        </div>
                      </div>

                      <div className='flex items-center gap-3'>
                        <Badge variant='secondary' className='rounded-full px-2.5 py-1 text-xs'>
                          #{app.id}
                        </Badge>
                        <Button type='button' variant='outline' size='sm' asChild>
                          <Link
                            to='/applications/$applicationId'
                            params={{ applicationId: String(app.id) }}
                          >
                            Open
                          </Link>
                        </Button>
                      </div>
                    </div>
                  </CardHeader>

                  <CardContent className='flex flex-1 flex-col gap-5 pt-0'>
                    <p className='min-h-10 text-sm text-muted-foreground'>
                      {app.description?.trim() || 'No description provided yet.'}
                    </p>

                    <Separator />

                    <div className='grid gap-3 text-sm'>
                      <div className='flex items-center gap-2 text-muted-foreground'>
                        <ShieldCheck className='size-4' />
                        <span>
                          Owner: <span className='text-foreground'>{app.owner.username}</span>
                        </span>
                      </div>
                      <div className='flex items-center gap-2 text-muted-foreground'>
                        <CircleUserRound className='size-4' />
                        <span>
                          Maintainer:{' '}
                          <span className='text-foreground'>{app.maintainer.username}</span>
                        </span>
                      </div>
                      <div className='flex items-center gap-2 text-muted-foreground'>
                        <GitPullRequestArrow className='size-4' />
                        <span>
                          Developers:{' '}
                          <span className='text-foreground'>{app.developers.length}</span>
                        </span>
                      </div>
                    </div>
                  </CardContent>
                </Card>
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
        <div key={index} className='rounded-2xl border p-4'>
          <div className='mb-8 flex items-center justify-between gap-3'>
            <Skeleton className='size-10 rounded-xl' />
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
