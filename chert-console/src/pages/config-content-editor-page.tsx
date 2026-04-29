import { Suspense, lazy, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from '@tanstack/react-router'
import {
  ChevronRight,
  CircleAlert,
  Clock3,
  File,
  LoaderCircle,
  RefreshCcw,
  Save,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from '@/components/ui/empty'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
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

const MonacoDiffEditor = lazy(() =>
  import('@monaco-editor/react').then((module) => ({ default: module.DiffEditor })),
)

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
          setSelectedEnvironmentId((current) => current || String(nextEnvironments[0].id))
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

  const selectedEnvironment = useMemo(
    () =>
      selectedEnvironmentId
        ? environments.find((environment) => environment.id === Number(selectedEnvironmentId)) ?? null
        : null,
    [environments, selectedEnvironmentId],
  )

  const diffSummary = useMemo(() => summarizeDiff(diff?.oldContent ?? '', content), [content, diff])

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
      setDiff({
        oldContent: diff?.oldContent ?? '',
        newContent: content,
        hasChanges: (diff?.oldContent ?? '') !== content,
      })
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
      <div className='space-y-4'>
        <div className='flex items-center gap-2 text-sm text-muted-foreground'>
          <Link to='/applications' className='hover:text-foreground'>
            Applications
          </Link>
          <ChevronRight className='size-4' />
          <Link
            to='/applications/$applicationId'
            params={{ applicationId: String(application.id) }}
            className='hover:text-foreground'
          >
            {application.name}
          </Link>
          <ChevronRight className='size-4' />
          <span className='text-foreground'>{resource.name}</span>
        </div>

        <div className='space-y-2'>
          <h1 className='text-3xl font-bold tracking-tight'>Edit Configuration</h1>
          <p className='text-sm text-muted-foreground'>
            Review changes and edit the text configuration using side-by-side diff.
          </p>
        </div>
      </div>

      <Card className='overflow-hidden rounded-2xl'>
        <CardContent className='space-y-5 p-6'>
          <div className='flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between'>
            <div className='flex min-w-0 items-start gap-4'>
              <div className='flex size-14 shrink-0 items-center justify-center rounded-2xl border bg-muted/50'>
                <File className='size-6' />
              </div>
              <div className='min-w-0 space-y-2'>
                <div className='flex flex-wrap items-center gap-3'>
                  <h2 className='truncate text-2xl font-semibold'>{resource.name}</h2>
                  <Badge variant='secondary' className='rounded-full px-3 py-1 text-xs uppercase'>
                    {resource.format}
                  </Badge>
                </div>
                <p className='text-sm text-muted-foreground'>
                  {resource.description?.trim() || 'Main text configuration for this application resource.'}
                </p>
                <div className='flex flex-wrap items-center gap-2 pt-1'>
                  <div className='min-w-44'>
                    <Select value={selectedEnvironmentId} onValueChange={setSelectedEnvironmentId}>
                      <SelectTrigger id='content-environment' className='w-full'>
                        <SelectValue placeholder='Environment' />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectGroup>
                          {environments.map((environment) => (
                            <SelectItem key={environment.id} value={String(environment.id)}>
                              {environment.code} · {environment.name}
                            </SelectItem>
                          ))}
                        </SelectGroup>
                      </SelectContent>
                    </Select>
                  </div>
                  <Badge variant='outline'>Format: {resource.format}</Badge>
                  <Badge variant='outline'>
                    Status: {diffSummary.total > 0 ? 'Draft' : 'Synced'}
                  </Badge>
                </div>
                <div className='flex flex-wrap items-center gap-x-5 gap-y-2 pt-1 text-sm text-muted-foreground'>
                  <span className='flex items-center gap-2'>
                    <Clock3 className='size-4' />
                    Last modified: {latestSavedAt ? formatDateTime(latestSavedAt) : 'No draft saved yet'}
                  </span>
                  {selectedEnvironment ? <span>Environment: {selectedEnvironment.code}</span> : null}
                </div>
              </div>
            </div>

            <div className='flex flex-wrap items-center gap-2 xl:justify-end'>
              <Button
                type='button'
                variant='outline'
                asChild
              >
                <Link to='/applications/$applicationId' params={{ applicationId: String(application.id) }}>
                  Cancel
                </Link>
              </Button>
              <Button
                type='button'
                disabled={!selectedEnvironmentId || isSaving}
                onClick={() => void handleSave()}
              >
                {isSaving ? <LoaderCircle className='size-4 animate-spin' /> : <Save className='size-4' />}
                Save Draft
              </Button>
            </div>
          </div>

          {(saveMessage || errorMessage) ? (
            <div className='flex flex-col gap-2 text-sm'>
              {saveMessage ? <p className='text-emerald-600'>{saveMessage}</p> : null}
              {errorMessage ? <p className='text-destructive'>{errorMessage}</p> : null}
            </div>
          ) : null}
        </CardContent>
      </Card>

      <Card className='min-h-0 overflow-hidden rounded-2xl'>
        <CardHeader className='border-b'>
          <div className='flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between'>
            <div className='space-y-1'>
              <CardTitle>Diff Editor</CardTitle>
              <CardDescription>
                Compare the latest release with the current draft and edit directly on the modified side.
              </CardDescription>
            </div>
            <div className='flex flex-wrap items-center gap-2 text-sm'>
              <Badge variant='outline'>Original: latest release</Badge>
              <Badge variant='outline'>Modified: draft</Badge>
              <Badge variant='secondary' className='rounded-full px-3 py-1'>
                {diffSummary.total} changes
              </Badge>
            </div>
          </div>
        </CardHeader>
        <CardContent className='min-h-0 p-0'>
          <Suspense
            fallback={
              <div className='flex h-[68vh] items-center justify-center text-sm text-muted-foreground'>
                Loading Monaco diff editor…
              </div>
            }
          >
            <MonacoDiffEditor
              height='68vh'
              language={monacoLanguage}
              theme={resolvedTheme === 'dark' ? 'vs-dark' : 'light'}
              original={diff?.oldContent ?? ''}
              modified={content}
              onMount={(editor) => {
                const modifiedEditor = editor.getModifiedEditor()
                const model = modifiedEditor.getModel()

                model?.onDidChangeContent(() => {
                  setContent(model.getValue())
                })
              }}
              options={{
                automaticLayout: true,
                fontSize: 14,
                minimap: { enabled: false },
                renderSideBySide: true,
                originalEditable: false,
                scrollBeyondLastLine: false,
                wordWrap: 'on',
              }}
            />
          </Suspense>
        </CardContent>
      </Card>

      <Card className='rounded-2xl'>
        <CardHeader>
          <CardTitle>Change Summary</CardTitle>
          <CardDescription>
            Lightweight summary of the current draft compared with the latest release.
          </CardDescription>
        </CardHeader>
        <CardContent className='flex flex-wrap items-center gap-3'>
          <SummaryBadge tone='amber' label={`${diffSummary.modified} modified`} />
          <SummaryBadge tone='emerald' label={`${diffSummary.added} added`} />
          <SummaryBadge tone='rose' label={`${diffSummary.deleted} deleted`} />
        </CardContent>
      </Card>
    </section>
  )
}

function ContentEditorSkeleton() {
  return (
    <section className='flex min-h-0 flex-1 flex-col gap-6'>
      <div className='space-y-3'>
        <Skeleton className='h-4 w-48' />
        <Skeleton className='h-10 w-64' />
        <Skeleton className='h-5 w-[520px]' />
      </div>
      <Skeleton className='h-[220px] rounded-2xl' />
      <Skeleton className='h-[760px] rounded-2xl' />
      <Skeleton className='h-[140px] rounded-2xl' />
    </section>
  )
}

function mapFormatToLanguage(format: ConfigFormat) {
  switch (format) {
    case 'JSON':
      return 'json'
    case 'XML':
      return 'xml'
    case 'YAML':
      return 'yaml'
    case 'PROPERTIES':
      return 'ini'
    case 'TOML':
      return 'toml'
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

function summarizeDiff(oldContent: string, newContent: string) {
  const oldLines = oldContent.split('\n')
  const newLines = newContent.split('\n')
  const maxLength = Math.max(oldLines.length, newLines.length)

  let added = 0
  let deleted = 0
  let modified = 0

  for (let index = 0; index < maxLength; index += 1) {
    const oldLine = oldLines[index]
    const newLine = newLines[index]

    if (oldLine === undefined && newLine !== undefined) {
      added += 1
      continue
    }

    if (oldLine !== undefined && newLine === undefined) {
      deleted += 1
      continue
    }

    if ((oldLine ?? '') !== (newLine ?? '')) {
      modified += 1
    }
  }

  return {
    added,
    deleted,
    modified,
    total: added + deleted + modified,
  }
}

type SummaryBadgeProps = {
  label: string
  tone: 'amber' | 'emerald' | 'rose'
}

function SummaryBadge({ label, tone }: SummaryBadgeProps) {
  const className =
    tone === 'amber'
      ? 'border-amber-200 bg-amber-500/10 text-amber-700 dark:border-amber-900 dark:text-amber-300'
      : tone === 'emerald'
        ? 'border-emerald-200 bg-emerald-500/10 text-emerald-700 dark:border-emerald-900 dark:text-emerald-300'
        : 'border-rose-200 bg-rose-500/10 text-rose-700 dark:border-rose-900 dark:text-rose-300'

  return <span className={`rounded-full border px-3 py-1 text-sm ${className}`}>{label}</span>
}
