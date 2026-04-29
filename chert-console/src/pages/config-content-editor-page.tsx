import { Suspense, lazy, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from '@tanstack/react-router'
import {
  CircleAlert,
  FileCode2,
  LoaderCircle,
  RefreshCcw,
  Save,
  SearchCode,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from '@/components/ui/empty'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { useTheme } from '@/components/theme-provider'
import { getApplication, type Application } from '@/lib/applications'
import {
  getConfigContentDiff,
  getLatestConfigContent,
  saveConfigContent,
  type ConfigDiff,
} from '@/lib/config-content'
import { listConfigResources, type ConfigFormat, type ConfigResource } from '@/lib/config-resources'
import { listEnvironments, type Environment } from '@/lib/environments'

type LoadState = 'loading' | 'ready' | 'error'
const MonacoEditor = lazy(() => import('@monaco-editor/react'))

export function ConfigContentEditorPage() {
  const { applicationId, resourceId } = useParams({
    from: '/_protected/applications/$applicationId/resources/$resourceId/content',
  })
  const numericApplicationId = Number(applicationId)
  const numericResourceId = Number(resourceId)
  const { resolvedTheme } = useTheme()
  const [application, setApplication] = useState<Application | null>(null)
  const [resource, setResource] = useState<ConfigResource | null>(null)
  const [environments, setEnvironments] = useState<Environment[]>([])
  const [selectedEnvironmentId, setSelectedEnvironmentId] = useState<string>('')
  const [content, setContent] = useState('')
  const [latestSavedAt, setLatestSavedAt] = useState<string | null>(null)
  const [diff, setDiff] = useState<ConfigDiff | null>(null)
  const [loadState, setLoadState] = useState<LoadState>('loading')
  const [errorMessage, setErrorMessage] = useState('')
  const [requestVersion, setRequestVersion] = useState(0)
  const [isSaving, setIsSaving] = useState(false)
  const [saveMessage, setSaveMessage] = useState('')

  useEffect(() => {
    const abortController = new AbortController()

    async function loadBase() {
      setLoadState('loading')
      setErrorMessage('')

      try {
        const [nextApplication, nextResources, nextEnvironments] = await Promise.all([
          getApplication(numericApplicationId, abortController.signal),
          listConfigResources(numericApplicationId, abortController.signal),
          listEnvironments(abortController.signal),
        ])

        if (abortController.signal.aborted) {
          return
        }

        const nextResource =
          nextResources.find((candidate) => candidate.id === numericResourceId) ?? null

        setApplication(nextApplication)
        setResource(nextResource)
        setEnvironments(nextEnvironments)

        if (nextEnvironments.length > 0) {
          setSelectedEnvironmentId((current) =>
            current || String(nextEnvironments[0].id),
          )
        }

        setLoadState('ready')
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        setErrorMessage(
          error instanceof Error ? error.message : 'Failed to load editor context.',
        )
        setLoadState('error')
      }
    }

    void loadBase()

    return () => abortController.abort()
  }, [numericApplicationId, numericResourceId, requestVersion])

  useEffect(() => {
    const abortController = new AbortController()

    async function loadContent() {
      if (!selectedEnvironmentId) {
        return
      }

      setSaveMessage('')
      setErrorMessage('')

      try {
        const environmentId = Number(selectedEnvironmentId)
        const [nextContent, nextDiff] = await Promise.all([
          getLatestConfigContent(numericResourceId, environmentId, abortController.signal).catch(
            () => null,
          ),
          getConfigContentDiff(numericResourceId, environmentId, abortController.signal).catch(
            () => null,
          ),
        ])

        if (abortController.signal.aborted) {
          return
        }

        setContent(nextContent?.content ?? '')
        setLatestSavedAt(nextContent?.updatedAt ?? null)
        setDiff(nextDiff)
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        setErrorMessage(
          error instanceof Error ? error.message : 'Failed to load config content.',
        )
      }
    }

    void loadContent()

    return () => abortController.abort()
  }, [numericResourceId, selectedEnvironmentId])

  const monacoLanguage = useMemo(() => {
    if (!resource) {
      return 'plaintext'
    }

    return mapFormatToLanguage(resource.format)
  }, [resource])

  const handleSave = async () => {
    if (!selectedEnvironmentId) {
      return
    }

    setIsSaving(true)
    setSaveMessage('')
    setErrorMessage('')

    try {
      const saved = await saveConfigContent(
        numericResourceId,
        Number(selectedEnvironmentId),
        content,
      )

      setLatestSavedAt(saved.updatedAt)
      setSaveMessage('Draft saved.')
      setDiff((current) => ({
        oldContent: current?.oldContent ?? '',
        newContent: content,
        hasChanges: true,
      }))
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to save config.')
    } finally {
      setIsSaving(false)
    }
  }

  const retryLoad = () => {
    setRequestVersion((currentVersion) => currentVersion + 1)
  }

  if (loadState === 'loading') {
    return <ContentEditorSkeleton />
  }

  if (loadState === 'error' || !application || !resource) {
    return (
      <section className='flex min-h-0 flex-1 flex-col gap-6'>
        <Empty className='min-h-full border'>
          <EmptyHeader>
            <EmptyMedia variant='icon'>
              <CircleAlert />
            </EmptyMedia>
            <EmptyTitle>Unable to open editor</EmptyTitle>
            <EmptyDescription>{errorMessage || 'The requested resource was not found.'}</EmptyDescription>
          </EmptyHeader>
          <Button type='button' variant='outline' onClick={retryLoad}>
            <RefreshCcw className='size-4' />
            Retry
          </Button>
        </Empty>
      </section>
    )
  }

  return (
    <section className='flex min-h-0 flex-1 flex-col gap-6'>
      <div className='flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between'>
        <div className='space-y-3'>
          <div className='flex items-center gap-2 text-sm text-muted-foreground'>
            <Link to='/applications' className='hover:text-foreground'>
              Applications
            </Link>
            <span>/</span>
            <Link
              to='/applications/$applicationId'
              params={{ applicationId: String(application.id) }}
              className='hover:text-foreground'
            >
              {application.name}
            </Link>
            <span>/</span>
            <span className='text-foreground'>{resource.name}</span>
          </div>

          <div className='flex items-start gap-4'>
            <div className='flex size-12 items-center justify-center rounded-xl border bg-muted'>
              <FileCode2 className='size-6' />
            </div>
            <div className='space-y-2'>
              <div className='flex flex-wrap items-center gap-2'>
                <h1 className='text-2xl font-bold tracking-tight'>{resource.name}</h1>
                <span className='rounded-md border px-2 py-1 text-xs text-muted-foreground'>
                  {resource.format}
                </span>
              </div>
              <p className='text-sm text-muted-foreground'>
                {resource.description?.trim() || 'Text config editor for this application resource.'}
              </p>
            </div>
          </div>
        </div>

        <div className='flex flex-col gap-2 sm:flex-row sm:items-center'>
          <div className='flex min-w-48 flex-col gap-2'>
            <Label htmlFor='content-environment'>Environment</Label>
            <Select value={selectedEnvironmentId} onValueChange={setSelectedEnvironmentId}>
              <SelectTrigger id='content-environment'>
                {selectedEnvironmentId
                  ? environments.find(
                      (environment) => environment.id === Number(selectedEnvironmentId),
                    )?.name ?? 'Environment'
                  : 'Environment'}
              </SelectTrigger>
              <SelectContent>
                {environments.map((environment) => (
                  <SelectItem key={environment.id} value={String(environment.id)}>
                    {environment.name} ({environment.code})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Button
            type='button'
            className='mt-auto'
            disabled={!selectedEnvironmentId || isSaving}
            onClick={() => void handleSave()}
          >
            {isSaving ? <LoaderCircle className='size-4 animate-spin' /> : <Save className='size-4' />}
            Save Draft
          </Button>
        </div>
      </div>

      <div className='grid min-h-0 flex-1 gap-6 xl:grid-cols-[minmax(0,1fr)_320px]'>
        <Card className='min-h-0'>
          <CardHeader>
            <CardTitle>Text Editor</CardTitle>
            <CardDescription>
              Monaco is used here for raw text editing. Drafts are stored per environment.
            </CardDescription>
          </CardHeader>
          <CardContent className='min-h-0'>
            <div className='overflow-hidden rounded-xl border'>
              <Suspense
                fallback={
                  <div className='flex h-[65vh] items-center justify-center text-sm text-muted-foreground'>
                    Loading Monaco editor…
                  </div>
                }
              >
                <MonacoEditor
                  height='65vh'
                  language={monacoLanguage}
                  theme={resolvedTheme === 'dark' ? 'vs-dark' : 'light'}
                  value={content}
                  onChange={(value) => setContent(value ?? '')}
                  options={{
                    automaticLayout: true,
                    fontSize: 14,
                    minimap: { enabled: false },
                    scrollBeyondLastLine: false,
                    wordWrap: 'on',
                  }}
                />
              </Suspense>
            </div>
          </CardContent>
        </Card>

        <div className='space-y-6'>
          <Card>
            <CardHeader>
              <CardTitle>Draft Status</CardTitle>
              <CardDescription>
                Current draft state for the selected environment.
              </CardDescription>
            </CardHeader>
            <CardContent className='space-y-3 text-sm'>
              <StatusRow
                label='Environment'
                value={
                  selectedEnvironmentId
                    ? environments.find(
                        (environment) => environment.id === Number(selectedEnvironmentId),
                      )?.name ?? 'Unknown'
                    : 'Not selected'
                }
              />
              <StatusRow
                label='Last saved'
                value={latestSavedAt ? formatDateTime(latestSavedAt) : 'No draft saved yet'}
              />
              <StatusRow
                label='Diff vs latest release'
                value={diff?.hasChanges ? 'Has unpublished changes' : 'No detected changes'}
              />
              {saveMessage ? <p className='text-sm text-emerald-600'>{saveMessage}</p> : null}
              {errorMessage ? <p className='text-sm text-destructive'>{errorMessage}</p> : null}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Diff Preview</CardTitle>
              <CardDescription>
                A lightweight preview against the latest published release.
              </CardDescription>
            </CardHeader>
            <CardContent className='space-y-3'>
              {!diff ? (
                <div className='rounded-lg border bg-muted/40 px-4 py-3 text-sm text-muted-foreground'>
                  No diff data yet for this environment.
                </div>
              ) : (
                <>
                  <div className='rounded-lg border bg-muted/40 px-4 py-3 text-sm text-muted-foreground'>
                    {diff.hasChanges
                      ? 'The draft differs from the latest release.'
                      : 'The current draft matches the latest release.'}
                  </div>

                  <div className='space-y-2'>
                    <div className='flex items-center gap-2 text-sm font-medium'>
                      <SearchCode className='size-4' />
                      Latest release
                    </div>
                    <pre className='max-h-48 overflow-auto rounded-lg border bg-muted/40 p-3 text-xs text-muted-foreground'>
                      {diff.oldContent?.trim() || 'No released content yet.'}
                    </pre>
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </section>
  )
}

type StatusRowProps = {
  label: string
  value: string
}

function StatusRow({ label, value }: StatusRowProps) {
  return (
    <div className='flex items-start justify-between gap-4 rounded-lg border px-3 py-2'>
      <span className='text-muted-foreground'>{label}</span>
      <span className='text-right'>{value}</span>
    </div>
  )
}

function ContentEditorSkeleton() {
  return (
    <section className='flex min-h-0 flex-1 flex-col gap-6'>
      <div className='space-y-3'>
        <Skeleton className='h-4 w-48' />
        <div className='flex items-start gap-4'>
          <Skeleton className='size-12 rounded-xl' />
          <div className='space-y-2'>
            <Skeleton className='h-8 w-72' />
            <Skeleton className='h-4 w-80' />
          </div>
        </div>
      </div>
      <div className='grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]'>
        <Skeleton className='h-[720px] rounded-xl' />
        <Skeleton className='h-[420px] rounded-xl' />
      </div>
    </section>
  )
}

function mapFormatToLanguage(format: ConfigFormat) {
  switch (format) {
    case 'JSON':
      return 'json'
    case 'XML':
      return 'xml'
    default:
      return 'plaintext'
  }
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
