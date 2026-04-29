import { useEffect, useState, type FormEvent } from 'react'
import { Blocks, LoaderCircle, Plus, RefreshCcw, TriangleAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
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
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useHeaderBreadcrumbs } from '@/components/layout/header-breadcrumbs'
import { createEnvironment, listEnvironments, type Environment } from '@/lib/environments'

type LoadState = 'loading' | 'ready' | 'error'

export function EnvironmentsPage() {
  useHeaderBreadcrumbs([{ label: 'Environments' }])
  const [environments, setEnvironments] = useState<Environment[]>([])
  const [errorMessage, setErrorMessage] = useState('')
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [loadState, setLoadState] = useState<LoadState>('loading')
  const [requestVersion, setRequestVersion] = useState(0)

  useEffect(() => {
    const abortController = new AbortController()

    async function load() {
      setLoadState('loading')
      setErrorMessage('')

      try {
        const nextEnvironments = await listEnvironments(abortController.signal)
        setEnvironments(nextEnvironments)
        setLoadState('ready')
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        setErrorMessage(
          error instanceof Error ? error.message : 'Failed to load environments.',
        )
        setLoadState('error')
      }
    }

    void load()

    return () => abortController.abort()
  }, [requestVersion])

  const retryLoad = () => {
    setRequestVersion((currentVersion) => currentVersion + 1)
  }

  const handleCreateEnvironment = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setErrorMessage('')
    setIsSubmitting(true)

    const formData = new FormData(event.currentTarget)

    try {
      const environment = await createEnvironment({
        code: String(formData.get('code') ?? ''),
        name: String(formData.get('name') ?? ''),
        description: String(formData.get('description') ?? ''),
      })
      setEnvironments((current) => current.concat(environment).toSorted((a, b) => a.name.localeCompare(b.name)))
      setIsCreateOpen(false)
      event.currentTarget.reset()
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to create environment.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className='flex min-h-0 flex-1 flex-col gap-6'>
      <div className='flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between'>
        <div>
          <h1 className='text-2xl font-bold tracking-tight'>Environments</h1>
          <p className='text-muted-foreground'>
            Manage environment codes shared across applications and release workflows.
          </p>
        </div>

        <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
          <DialogTrigger asChild>
            <Button type='button'>
              <Plus className='size-4' />
              New Environment
            </Button>
          </DialogTrigger>
          <DialogContent className='sm:max-w-lg'>
            <DialogHeader>
              <DialogTitle>Create environment</DialogTitle>
              <DialogDescription>
                Add a new environment code that applications can target.
              </DialogDescription>
            </DialogHeader>
            <form className='flex flex-col gap-4' onSubmit={handleCreateEnvironment}>
              <div className='flex flex-col gap-2'>
                <Label htmlFor='environment-code'>Code</Label>
                <Input id='environment-code' name='code' placeholder='prod' required />
              </div>

              <div className='flex flex-col gap-2'>
                <Label htmlFor='environment-name'>Name</Label>
                <Input id='environment-name' name='name' placeholder='Production' required />
              </div>

              <div className='flex flex-col gap-2'>
                <Label htmlFor='environment-description'>Description</Label>
                <Input id='environment-description' name='description' placeholder='Primary production environment' />
              </div>

              {errorMessage ? <p className='text-sm text-destructive'>{errorMessage}</p> : null}

              <DialogFooter>
                <Button type='submit' disabled={isSubmitting}>
                  {isSubmitting ? <LoaderCircle className='size-4 animate-spin' /> : null}
                  Create
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      <Card className='min-h-0 flex-1'>
        <CardHeader>
          <CardTitle>Environment catalog</CardTitle>
          <CardDescription>
            These environment codes are used for config drafts, releases, and publish policies.
          </CardDescription>
        </CardHeader>
        <CardContent className='min-h-0 flex-1'>
          {loadState === 'error' ? (
            <Empty className='min-h-[360px] border'>
              <EmptyHeader>
                <EmptyMedia variant='icon'>
                  <TriangleAlert />
                </EmptyMedia>
                <EmptyTitle>Unable to load environments</EmptyTitle>
                <EmptyDescription>{errorMessage}</EmptyDescription>
              </EmptyHeader>
              <Button type='button' variant='outline' onClick={retryLoad}>
                <RefreshCcw className='size-4' />
                Retry
              </Button>
            </Empty>
          ) : null}

          {loadState === 'ready' && environments.length === 0 ? (
            <Empty className='min-h-[360px] border'>
              <EmptyHeader>
                <EmptyMedia variant='icon'>
                  <Blocks />
                </EmptyMedia>
                <EmptyTitle>No environments yet</EmptyTitle>
                <EmptyDescription>
                  Create the first environment to start configuring release behavior.
                </EmptyDescription>
              </EmptyHeader>
            </Empty>
          ) : null}

          {loadState === 'loading' ? (
            <div className='flex min-h-[360px] items-center justify-center text-sm text-muted-foreground'>
              Loading environments…
            </div>
          ) : null}

          {loadState === 'ready' && environments.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Code</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Updated</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {environments.map((environment) => (
                  <TableRow key={environment.id}>
                    <TableCell className='font-medium'>{environment.name}</TableCell>
                    <TableCell className='font-mono text-xs'>{environment.code}</TableCell>
                    <TableCell className='text-muted-foreground'>
                      {environment.description || 'No description'}
                    </TableCell>
                    <TableCell className='text-muted-foreground'>
                      {formatDateTime(environment.updatedAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : null}
        </CardContent>
      </Card>
    </section>
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
