import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from '@tanstack/react-router'
import {
  ChevronDown,
  ChevronUp,
  Copy,
  Database,
  File,
  History,
  LoaderCircle,
  PencilLine,
  Plus,
  RotateCcw,
  Search,
  UploadCloud,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'
import { CodeBlock } from '@/components/ui/code'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import {
  type HeaderBreadcrumbItem,
  useHeaderBreadcrumbs,
} from '@/components/layout/header-breadcrumbs'
import { ApiError } from '@/lib/api'
import { getApplication, type Application } from '@/lib/applications'
import {
  getLatestConfigContent,
  type ConfigContent,
} from '@/lib/config-content'
import {
  deleteConfigEntry,
  listConfigEntries,
  saveConfigEntry,
  type ConfigEntry,
  type ConfigEntryPayload,
} from '@/lib/config-entries'
import { listEnvironments, type Environment } from '@/lib/environments'
import {
  createConfigResource,
  listConfigResources,
  type ConfigFormat,
  type ConfigResource,
  type ConfigType,
} from '@/lib/config-resources'
import {
  getLatestConfigRelease,
  listConfigReleaseHistory,
  publishConfigRelease,
  rollbackConfigRelease,
  type ConfigRelease,
  type ConfigReleaseHistoryResponse,
} from '@/lib/config-releases'
import { cn } from '@/lib/utils'

type LoadState = 'loading' | 'ready' | 'error'
type ResourceFilter = 'ALL' | 'CONTENT' | 'ENTRIES'
type FeedbackTone = 'success' | 'error' | 'info'

type ResourceRuntime = {
  content: ConfigContent | null
  entries: ConfigEntry[]
  latestRelease: ConfigRelease | null
}

type ResourceFeedback = {
  tone: FeedbackTone
  message: string
}

type EntryDialogState = {
  resourceId: number
  entry: ConfigEntry | null
} | null

type ReleaseDialogState = {
  resource: ConfigResource
  mode: 'history' | 'rollback'
} | null

type CreateResourceDraft = {
  configName: string
  type: ConfigType
  format: ConfigFormat
  version: string
  description: string
}

type EntryDraft = {
  key: string
  value: string
  valueType: string
  description: string
}

type ReleaseSnapshotEntry = {
  key: string
  value: string
  valueType: string | null
  description: string | null
}

const defaultCreateResourceDraft: CreateResourceDraft = {
  configName: '',
  type: 'CONTENT',
  format: 'YAML',
  version: '',
  description: '',
}

const defaultEntryDraft: EntryDraft = {
  key: '',
  value: '',
  valueType: 'STRING',
  description: '',
}

const contentFormats: ConfigFormat[] = ['YAML', 'PROPERTIES', 'JSON', 'TOML', 'XML']
const entryValueTypes = ['STRING', 'BOOLEAN', 'NUMBER', 'JSON']

export function ApplicationDetailPage() {
  const { applicationId } = useParams({
    from: '/_protected/applications/$applicationId',
  })
  const numericApplicationId = Number(applicationId)

  const [application, setApplication] = useState<Application | null>(null)
  const [resources, setResources] = useState<ConfigResource[]>([])
  const [environments, setEnvironments] = useState<Environment[]>([])
  const [selectedEnvironmentId, setSelectedEnvironmentId] = useState('')
  const [resourceFilter, setResourceFilter] = useState<ResourceFilter>('ALL')
  const [searchValue, setSearchValue] = useState('')
  const [loadState, setLoadState] = useState<LoadState>('loading')
  const [errorMessage, setErrorMessage] = useState('')
  const [baseVersion, setBaseVersion] = useState(0)
  const [resourceDataVersion, setResourceDataVersion] = useState(0)
  const [resourceRuntime, setResourceRuntime] = useState<Record<number, ResourceRuntime>>({})
  const [resourceLoading, setResourceLoading] = useState(false)
  const [collapsedResourceIds, setCollapsedResourceIds] = useState<Record<number, boolean>>({})
  const [resourceFeedback, setResourceFeedback] = useState<Record<number, ResourceFeedback>>({})
  const [busyAction, setBusyAction] = useState<{ resourceId: number; action: string } | null>(null)
  const [createResourceOpen, setCreateResourceOpen] = useState(false)
  const [createResourceDraft, setCreateResourceDraft] = useState<CreateResourceDraft>(
    defaultCreateResourceDraft,
  )
  const [createResourceError, setCreateResourceError] = useState('')
  const [isCreatingResource, setIsCreatingResource] = useState(false)
  const [entryDialogState, setEntryDialogState] = useState<EntryDialogState>(null)
  const [entryDraft, setEntryDraft] = useState<EntryDraft>(defaultEntryDraft)
  const [entryDialogError, setEntryDialogError] = useState('')
  const [isSavingEntry, setIsSavingEntry] = useState(false)
  const [releaseDialogState, setReleaseDialogState] = useState<ReleaseDialogState>(null)
  const [releaseHistory, setReleaseHistory] = useState<ConfigReleaseHistoryResponse[]>([])
  const [releaseHistoryLoading, setReleaseHistoryLoading] = useState(false)
  const [releaseHistoryError, setReleaseHistoryError] = useState('')
  const [releaseComment, setReleaseComment] = useState('')
  const [isRollingBack, setIsRollingBack] = useState<number | null>(null)

  const breadcrumbItems = useMemo<HeaderBreadcrumbItem[]>(
    () => [
      { label: 'Applications', href: '/applications' },
      ...(application ? [{ label: application.name }] : []),
    ],
    [application],
  )
  useHeaderBreadcrumbs(breadcrumbItems)

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

        setApplication(nextApplication)
        setResources(nextResources)
        setEnvironments(nextEnvironments)
        setCollapsedResourceIds((current) => {
          const next = { ...current }
          for (const resource of nextResources) {
            if (!(resource.id in next)) {
              next[resource.id] = false
            }
          }
          return next
        })

        if (nextEnvironments.length > 0) {
          setSelectedEnvironmentId((current) => current || String(nextEnvironments[0].id))
        }

        setLoadState('ready')
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        setErrorMessage(
          error instanceof Error ? error.message : 'Failed to load application configurations.',
        )
        setLoadState('error')
      }
    }

    void loadBase()

    return () => abortController.abort()
  }, [numericApplicationId, baseVersion])

  useEffect(() => {
    const abortController = new AbortController()

    async function loadResourceRuntime() {
      if (!selectedEnvironmentId || resources.length === 0) {
        setResourceRuntime({})
        return
      }

      setResourceLoading(true)
      setResourceFeedback({})

      try {
        const environmentId = Number(selectedEnvironmentId)
        const entries = await Promise.all(
          resources.map(async (resource) => {
            const [content, resourceEntries, latestRelease] = await Promise.all([
              resource.type === 'CONTENT'
                ? getLatestConfigContent(resource.id, environmentId, abortController.signal).catch(
                    (error) => {
                      if (isNotFoundError(error)) {
                        return null
                      }
                      throw error
                    },
                  )
                : Promise.resolve(null),
              resource.type === 'ENTRIES'
                ? listConfigEntries(resource.id, environmentId, abortController.signal).catch(
                    (error) => {
                      if (isNotFoundError(error)) {
                        return []
                      }
                      throw error
                    },
                  )
                : Promise.resolve([]),
              getLatestConfigRelease(resource.id, environmentId, abortController.signal).catch(
                (error) => {
                  if (isNotFoundError(error)) {
                    return null
                  }
                  throw error
                },
              ),
            ])

            return [
              resource.id,
              {
                content,
                entries: resourceEntries,
                latestRelease,
              } satisfies ResourceRuntime,
            ] as const
          }),
        )

        if (abortController.signal.aborted) {
          return
        }

        setResourceRuntime(Object.fromEntries(entries))
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        setErrorMessage(
          error instanceof Error ? error.message : 'Failed to load configuration resources.',
        )
      } finally {
        if (!abortController.signal.aborted) {
          setResourceLoading(false)
        }
      }
    }

    void loadResourceRuntime()

    return () => abortController.abort()
  }, [resources, selectedEnvironmentId, resourceDataVersion])

  useEffect(() => {
    if (!releaseDialogState || !selectedEnvironmentId) {
      return
    }

    const abortController = new AbortController()
    const resourceId = releaseDialogState.resource.id

    async function loadHistory() {
      setReleaseHistoryLoading(true)
      setReleaseHistoryError('')

      try {
        const nextHistory = await listConfigReleaseHistory(
          resourceId,
          Number(selectedEnvironmentId),
          abortController.signal,
        )

        if (abortController.signal.aborted) {
          return
        }

        setReleaseHistory(nextHistory)
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        setReleaseHistoryError(
          error instanceof Error ? error.message : 'Failed to load release history.',
        )
      } finally {
        if (!abortController.signal.aborted) {
          setReleaseHistoryLoading(false)
        }
      }
    }

    void loadHistory()

    return () => abortController.abort()
  }, [releaseDialogState, selectedEnvironmentId])

  useEffect(() => {
    if (!entryDialogState) {
      setEntryDraft(defaultEntryDraft)
      setEntryDialogError('')
      return
    }

    if (entryDialogState.entry) {
      setEntryDraft({
        key: entryDialogState.entry.key,
        value: entryDialogState.entry.value,
        valueType: entryDialogState.entry.valueType ?? 'STRING',
        description: entryDialogState.entry.description ?? '',
      })
      return
    }

    setEntryDraft(defaultEntryDraft)
  }, [entryDialogState])

  const selectedEnvironment = useMemo(
    () =>
      selectedEnvironmentId
        ? environments.find((environment) => environment.id === Number(selectedEnvironmentId)) ?? null
        : null,
    [environments, selectedEnvironmentId],
  )

  const filteredResources = useMemo(() => {
    const normalizedSearch = searchValue.trim().toLowerCase()

    return resources.filter((resource) => {
      if (resourceFilter === 'CONTENT' && resource.type !== 'CONTENT') {
        return false
      }

      if (resourceFilter === 'ENTRIES' && resource.type !== 'ENTRIES') {
        return false
      }

      if (!normalizedSearch) {
        return true
      }

      return (
        resource.name.toLowerCase().includes(normalizedSearch) ||
        (resource.description ?? '').toLowerCase().includes(normalizedSearch)
      )
    })
  }, [resourceFilter, resources, searchValue])

  const retryLoad = () => {
    setBaseVersion((current) => current + 1)
    setResourceDataVersion((current) => current + 1)
  }

  const handleCreateResource = async () => {
    if (!application) {
      return
    }

    setIsCreatingResource(true)
    setCreateResourceError('')

    try {
      await createConfigResource(application.id, {
        configName: createResourceDraft.configName,
        type: createResourceDraft.type,
        format: createResourceDraft.type === 'ENTRIES' ? 'NONE' : createResourceDraft.format,
        version: createResourceDraft.version ? Number(createResourceDraft.version) : null,
        description: createResourceDraft.description,
      })

      setCreateResourceOpen(false)
      setCreateResourceDraft(defaultCreateResourceDraft)
      setBaseVersion((current) => current + 1)
      setResourceDataVersion((current) => current + 1)
    } catch (error) {
      setCreateResourceError(
        error instanceof Error ? error.message : 'Failed to create configuration resource.',
      )
    } finally {
      setIsCreatingResource(false)
    }
  }

  const handlePublish = async (resource: ConfigResource) => {
    if (!selectedEnvironmentId) {
      return
    }

    setBusyAction({ resourceId: resource.id, action: 'publish' })
    setResourceFeedback((current) => ({ ...current, [resource.id]: undefined as never }))

    try {
      const response = await publishConfigRelease(resource.id, Number(selectedEnvironmentId), '')

      setResourceFeedback((current) => ({
        ...current,
        [resource.id]:
          response.outcome === 'PUBLISHED'
            ? { tone: 'success', message: 'Published successfully.' }
            : { tone: 'info', message: 'Submitted for review.' },
      }))
      setResourceDataVersion((current) => current + 1)
    } catch (error) {
      setResourceFeedback((current) => ({
        ...current,
        [resource.id]: {
          tone: 'error',
          message: error instanceof Error ? error.message : 'Failed to publish configuration.',
        },
      }))
    } finally {
      setBusyAction(null)
    }
  }

  const handleRollback = async (resource: ConfigResource, releaseId: number) => {
    if (!selectedEnvironmentId) {
      return
    }

    setIsRollingBack(releaseId)

    try {
      await rollbackConfigRelease(resource.id, Number(selectedEnvironmentId), releaseId, '')
      setReleaseDialogState(null)
      setReleaseComment('')
      setResourceFeedback((current) => ({
        ...current,
        [resource.id]: {
          tone: 'success',
          message: `Rolled back to release #${releaseId}.`,
        },
      }))
      setResourceDataVersion((current) => current + 1)
    } catch (error) {
      setReleaseHistoryError(
        error instanceof Error ? error.message : 'Failed to rollback release.',
      )
    } finally {
      setIsRollingBack(null)
    }
  }

  const handleDeleteEntry = async (resourceId: number, entry: ConfigEntry) => {
    if (!selectedEnvironmentId) {
      return
    }

    const confirmed = window.confirm(`Delete "${entry.key}" from this environment?`)
    if (!confirmed) {
      return
    }

    setBusyAction({ resourceId, action: `delete-entry-${entry.id}` })

    try {
      await deleteConfigEntry(resourceId, Number(selectedEnvironmentId), entry.id)
      setResourceFeedback((current) => ({
        ...current,
        [resourceId]: {
          tone: 'success',
          message: `Deleted entry "${entry.key}".`,
        },
      }))
      setResourceDataVersion((current) => current + 1)
    } catch (error) {
      setResourceFeedback((current) => ({
        ...current,
        [resourceId]: {
          tone: 'error',
          message: error instanceof Error ? error.message : 'Failed to delete entry.',
        },
      }))
    } finally {
      setBusyAction(null)
    }
  }

  const handleSaveEntry = async () => {
    if (!selectedEnvironmentId || !entryDialogState) {
      return
    }

    setIsSavingEntry(true)
    setEntryDialogError('')

    try {
      const payload: ConfigEntryPayload = {
        key: entryDraft.key,
        value: entryDraft.value,
        valueType: entryDraft.valueType,
        description: entryDraft.description,
      }

      await saveConfigEntry(entryDialogState.resourceId, Number(selectedEnvironmentId), payload)

      if (
        entryDialogState.entry &&
        entryDialogState.entry.key !== entryDraft.key
      ) {
        await deleteConfigEntry(
          entryDialogState.resourceId,
          Number(selectedEnvironmentId),
          entryDialogState.entry.id,
        )
      }

      setResourceFeedback((current) => ({
        ...current,
        [entryDialogState.resourceId]: {
          tone: 'success',
          message: entryDialogState.entry ? 'Entry updated.' : 'Entry created.',
        },
      }))
      setEntryDialogState(null)
      setResourceDataVersion((current) => current + 1)
    } catch (error) {
      setEntryDialogError(error instanceof Error ? error.message : 'Failed to save entry.')
    } finally {
      setIsSavingEntry(false)
    }
  }

  const toggleCollapse = (resourceId: number) => {
    setCollapsedResourceIds((current) => ({
      ...current,
      [resourceId]: !current[resourceId],
    }))
  }

  if (loadState === 'loading') {
    return <ApplicationDetailSkeleton />
  }

  if (loadState === 'error' || !application) {
    return (
      <section className='flex min-h-0 flex-1 flex-col gap-4'>
        <div className='rounded-2xl border border-dashed border-destructive/40 bg-destructive/5 p-6'>
          <h1 className='text-xl font-semibold'>Unable to load configurations</h1>
          <p className='mt-2 text-sm text-muted-foreground'>
            {errorMessage || 'The application detail page could not be loaded.'}
          </p>
          <Button type='button' variant='outline' className='mt-4' onClick={retryLoad}>
            Retry
          </Button>
        </div>
      </section>
    )
  }

  return (
    <>
      <section className='flex min-h-0 flex-1 flex-col gap-5'>
        <div className='space-y-1'>
          <h1 className='text-[2rem] font-semibold tracking-tight'>Configurations</h1>
          <p className='text-sm text-muted-foreground'>
            Manage configuration resources for this application across different environments.
          </p>
        </div>

        <div className='flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between'>
          <div className='flex flex-1 flex-col gap-3 lg:flex-row lg:items-center'>
            <Select value={selectedEnvironmentId} onValueChange={setSelectedEnvironmentId}>
              <SelectTrigger className='h-9 w-full min-w-48 rounded-xl lg:w-[220px]'>
                <SelectValue placeholder='Environment' />
              </SelectTrigger>
              <SelectContent>
                {environments.map((environment) => (
                  <SelectItem key={environment.id} value={String(environment.id)}>
                    {environment.code}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select value={resourceFilter} onValueChange={(value) => setResourceFilter(value as ResourceFilter)}>
              <SelectTrigger className='h-9 w-full min-w-44 rounded-xl lg:w-[180px]'>
                <SelectValue placeholder='All Resources' />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value='ALL'>All Resources</SelectItem>
                <SelectItem value='CONTENT'>Text Resources</SelectItem>
                <SelectItem value='ENTRIES'>KV Resources</SelectItem>
              </SelectContent>
            </Select>

            <div className='relative w-full lg:max-w-[320px]'>
              <Search className='pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground' />
              <Input
                value={searchValue}
                onChange={(event) => setSearchValue(event.target.value)}
                className='h-9 rounded-xl pl-9 pr-12'
                placeholder='Search resources...'
              />
              <span className='pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 rounded-md border bg-muted px-1.5 py-0.5 text-[11px] text-muted-foreground'>
                ⌘ K
              </span>
            </div>
          </div>

          <Button
            type='button'
            variant='outline'
            className='h-9 rounded-xl px-4'
            onClick={() => setCreateResourceOpen(true)}
          >
            Create Resource
            <Plus className='size-4' />
          </Button>
        </div>

        <div className='space-y-3'>
          {resourceLoading ? (
            <div className='space-y-3'>
              <ResourceCardSkeleton />
              <ResourceCardSkeleton />
            </div>
          ) : filteredResources.length === 0 ? (
            <div className='rounded-2xl border border-dashed p-8 text-center text-sm text-muted-foreground'>
              No configuration resources match the current filters.
            </div>
          ) : (
            filteredResources.map((resource) => {
              const runtime = resourceRuntime[resource.id] ?? {
                content: null,
                entries: [],
                latestRelease: null,
              }
              const isCollapsed = collapsedResourceIds[resource.id] ?? false
              const feedback = resourceFeedback[resource.id]
              const latestReleaseEntries = parseReleaseSnapshotEntries(runtime.latestRelease?.snapshot)
              const latestReleaseEntriesByKey = new Map(
                latestReleaseEntries.map((entry) => [entry.key, entry]),
              )
              const cardStatus = getResourceCardStatus(resource, runtime, latestReleaseEntries)

              return (
                <Collapsible
                  key={resource.id}
                  open={!isCollapsed}
                  onOpenChange={() => toggleCollapse(resource.id)}
                >
                  <Card className='overflow-hidden rounded-2xl border border-border/60 bg-card shadow-none ring-0'>
                    <CardContent className='p-0'>
                      <div className='flex flex-col gap-3 p-3'>
                        <div className='flex flex-col gap-3 xl:flex-row xl:items-start xl:justify-between'>
                          <div className='flex min-w-0 items-start gap-3'>
                            <div
                              className={cn(
                                'flex size-10 shrink-0 items-center justify-center rounded-xl border',
                                resource.type === 'ENTRIES'
                                  ? 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/60 dark:bg-emerald-950/30 dark:text-emerald-300'
                                  : 'border-border/60 bg-muted/40 text-foreground',
                              )}
                            >
                              {resource.type === 'ENTRIES' ? (
                                <Database className='size-4.5' />
                              ) : (
                                <File className='size-4.5' />
                              )}
                            </div>

                            <div className='min-w-0 space-y-1.5'>
                              <div className='flex flex-wrap items-center gap-2'>
                                <h2 className='truncate text-lg font-semibold'>{resource.name}</h2>
                                <ResourceFormatBadge resource={resource} />
                              </div>
                              <p className='text-[13px] text-muted-foreground'>
                                {resource.description?.trim() || getFallbackDescription(resource)}
                              </p>
                              <div className='flex flex-wrap items-center gap-x-4 gap-y-1 text-[13px] text-muted-foreground'>
                                <StatusDotLabel label={cardStatus} />
                                <span>{application.owner.username}</span>
                                <span>
                                  {formatDateTime(getResourceUpdatedAt(resource, runtime))}
                                </span>
                                {selectedEnvironment ? <span>{selectedEnvironment.code}</span> : null}
                              </div>
                            </div>
                          </div>

                          <div className='flex flex-col items-start gap-2 xl:items-end'>
                            <div className='flex flex-wrap items-center gap-2'>
                              {runtime.latestRelease ? (
                                <Badge variant='outline' className='rounded-full px-2 py-0 text-[10px]'>
                                  Release #{runtime.latestRelease.version}
                                </Badge>
                              ) : null}
                            </div>

                            <div className='flex flex-wrap items-center gap-1.5'>
                              {resource.type === 'CONTENT' ? (
                                <Button
                                  type='button'
                                  variant='outline'
                                  size='xs'
                                  asChild
                                  className='rounded-lg'
                                >
                                  <Link
                                    to='/applications/$applicationId/resources/$resourceId/content'
                                    params={{
                                      applicationId: String(application.id),
                                      resourceId: String(resource.id),
                                    }}
                                  >
                                    <PencilLine className='size-3.5' />
                                    Edit
                                  </Link>
                                </Button>
                              ) : (
                                <Button
                                  type='button'
                                  variant='outline'
                                  size='xs'
                                  className='rounded-lg'
                                  onClick={() => setEntryDialogState({ resourceId: resource.id, entry: null })}
                                >
                                  <PencilLine className='size-3.5' />
                                  Edit
                                </Button>
                              )}

                              <Button
                                type='button'
                                variant='outline'
                                size='xs'
                                className='rounded-lg'
                                onClick={() => void handlePublish(resource)}
                                disabled={!selectedEnvironmentId || busyAction?.resourceId === resource.id}
                              >
                                {busyAction?.resourceId === resource.id && busyAction.action === 'publish' ? (
                                  <LoaderCircle className='size-3.5 animate-spin' />
                                ) : (
                                  <UploadCloud className='size-3.5' />
                                )}
                                Publish
                              </Button>

                              <Button
                                type='button'
                                variant='outline'
                                size='xs'
                                className='rounded-lg'
                                onClick={() => {
                                  setReleaseComment('')
                                  setReleaseDialogState({ resource, mode: 'rollback' })
                                }}
                                disabled={!selectedEnvironmentId}
                              >
                                <RotateCcw className='size-3.5' />
                                Rollback
                              </Button>

                              <Button
                                type='button'
                                variant='outline'
                                size='xs'
                                className='rounded-lg'
                                onClick={() => {
                                  setReleaseComment('')
                                  setReleaseDialogState({ resource, mode: 'history' })
                                }}
                                disabled={!selectedEnvironmentId}
                              >
                                <History className='size-3.5' />
                                History
                              </Button>

                              <CollapsibleTrigger asChild>
                                <Button type='button' variant='outline' size='icon-xs' className='rounded-lg'>
                                  {isCollapsed ? (
                                    <ChevronDown className='size-4' />
                                  ) : (
                                    <ChevronUp className='size-4' />
                                  )}
                                </Button>
                              </CollapsibleTrigger>
                            </div>
                          </div>
                        </div>

                        {feedback ? (
                          <div
                            className={cn(
                              'rounded-xl border px-3 py-2 text-sm',
                              feedback.tone === 'success'
                                ? 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/60 dark:bg-emerald-950/30 dark:text-emerald-300'
                                : feedback.tone === 'error'
                                  ? 'border-destructive/30 bg-destructive/5 text-destructive'
                                  : 'border-sky-200 bg-sky-50 text-sky-700 dark:border-sky-900/60 dark:bg-sky-950/30 dark:text-sky-300',
                            )}
                          >
                            {feedback.message}
                          </div>
                        ) : null}
                      </div>

                      <CollapsibleContent>
                          <div className='border-t px-3 pb-3 pt-2.5'>
                          {resource.type === 'CONTENT' ? (
                            <ContentResourcePreview
                              applicationId={application.id}
                              resource={resource}
                              runtime={runtime}
                            />
                          ) : (
                            <EntriesResourceTable
                              resource={resource}
                              entries={runtime.entries}
                              latestReleaseEntriesByKey={latestReleaseEntriesByKey}
                              onAddEntry={() =>
                                setEntryDialogState({ resourceId: resource.id, entry: null })
                              }
                              onEditEntry={(entry) =>
                                setEntryDialogState({ resourceId: resource.id, entry })
                              }
                              onDeleteEntry={(entry) => void handleDeleteEntry(resource.id, entry)}
                            />
                          )}
                        </div>
                      </CollapsibleContent>
                    </CardContent>
                  </Card>
                </Collapsible>
              )
            })
          )}
        </div>
      </section>

      <Dialog open={createResourceOpen} onOpenChange={setCreateResourceOpen}>
        <DialogContent className='max-w-lg rounded-2xl p-0'>
          <DialogHeader className='px-5 pt-5'>
            <DialogTitle>Create Resource</DialogTitle>
            <DialogDescription>
              Add a new text or KV configuration resource for this application.
            </DialogDescription>
          </DialogHeader>

          <div className='space-y-4 px-5 pb-5'>
            <div className='space-y-2'>
              <label className='text-sm font-medium'>Config Name</label>
              <Input
                value={createResourceDraft.configName}
                onChange={(event) =>
                  setCreateResourceDraft((current) => ({
                    ...current,
                    configName: event.target.value,
                  }))
                }
                placeholder='application.yaml'
              />
            </div>

            <div className='grid gap-4 md:grid-cols-2'>
              <div className='space-y-2'>
                <label className='text-sm font-medium'>Type</label>
                <Select
                  value={createResourceDraft.type}
                  onValueChange={(value) =>
                    setCreateResourceDraft((current) => ({
                      ...current,
                      type: value as ConfigType,
                      format: value === 'ENTRIES' ? 'NONE' : current.format === 'NONE' ? 'YAML' : current.format,
                    }))
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value='CONTENT'>Content</SelectItem>
                    <SelectItem value='ENTRIES'>KV</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {createResourceDraft.type === 'CONTENT' ? (
                <div className='space-y-2'>
                  <label className='text-sm font-medium'>Format</label>
                  <Select
                    value={createResourceDraft.format}
                    onValueChange={(value) =>
                      setCreateResourceDraft((current) => ({
                        ...current,
                        format: value as ConfigFormat,
                      }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {contentFormats.map((format) => (
                        <SelectItem key={format} value={format}>
                          {normalizeFormatLabel(format)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              ) : null}
            </div>

            <div className='space-y-2'>
              <label className='text-sm font-medium'>Description</label>
              <Textarea
                value={createResourceDraft.description}
                onChange={(event) =>
                  setCreateResourceDraft((current) => ({
                    ...current,
                    description: event.target.value,
                  }))
                }
                rows={3}
                placeholder='Describe what this resource controls.'
              />
            </div>

            {createResourceError ? (
              <p className='text-sm text-destructive'>{createResourceError}</p>
            ) : null}
          </div>

          <DialogFooter>
            <Button type='button' variant='outline' onClick={() => setCreateResourceOpen(false)}>
              Cancel
            </Button>
            <Button type='button' disabled={isCreatingResource} onClick={() => void handleCreateResource()}>
              {isCreatingResource ? <LoaderCircle className='size-4 animate-spin' /> : null}
              Create Resource
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={entryDialogState !== null}
        onOpenChange={(open) => {
          if (!open) {
            setEntryDialogState(null)
          }
        }}
      >
        <DialogContent className='max-w-2xl rounded-2xl p-0'>
          <DialogHeader className='px-5 pt-5'>
            <DialogTitle>{entryDialogState?.entry ? 'Edit KV Entry' : 'Add KV Entry'}</DialogTitle>
            <DialogDescription>
              Keep KV editing inside the application page while preserving compact release visibility.
            </DialogDescription>
          </DialogHeader>

          <div className='space-y-4 px-5 pb-5'>
            <div className='grid gap-4 md:grid-cols-[1.4fr_0.8fr]'>
              <div className='space-y-2'>
                <label className='text-sm font-medium'>Key</label>
                <Input
                  value={entryDraft.key}
                  onChange={(event) =>
                    setEntryDraft((current) => ({ ...current, key: event.target.value }))
                  }
                  placeholder='feature.signup.enabled'
                />
              </div>
              <div className='space-y-2'>
                <label className='text-sm font-medium'>Type</label>
                <Select
                  value={entryDraft.valueType}
                  onValueChange={(value) =>
                    setEntryDraft((current) => ({ ...current, valueType: value }))
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {entryValueTypes.map((valueType) => (
                      <SelectItem key={valueType} value={valueType}>
                        {valueType}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className='space-y-2'>
              <label className='text-sm font-medium'>Value</label>
              <Textarea
                value={entryDraft.value}
                onChange={(event) =>
                  setEntryDraft((current) => ({ ...current, value: event.target.value }))
                }
                rows={4}
                placeholder='true'
              />
            </div>

            <div className='space-y-2'>
              <label className='text-sm font-medium'>Comment</label>
              <Input
                value={entryDraft.description}
                onChange={(event) =>
                  setEntryDraft((current) => ({ ...current, description: event.target.value }))
                }
                placeholder='Explain what this entry controls.'
              />
            </div>

            {entryDialogError ? (
              <p className='text-sm text-destructive'>{entryDialogError}</p>
            ) : null}
          </div>

          <DialogFooter>
            <Button type='button' variant='outline' onClick={() => setEntryDialogState(null)}>
              Cancel
            </Button>
            <Button type='button' disabled={isSavingEntry} onClick={() => void handleSaveEntry()}>
              {isSavingEntry ? <LoaderCircle className='size-4 animate-spin' /> : null}
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={releaseDialogState !== null}
        onOpenChange={(open) => {
          if (!open) {
            setReleaseDialogState(null)
            setReleaseComment('')
            setReleaseHistoryError('')
          }
        }}
      >
        <DialogContent className='max-w-3xl rounded-2xl p-0'>
          <DialogHeader className='px-5 pt-5'>
            <DialogTitle>
              {releaseDialogState?.mode === 'rollback' ? 'Rollback Release' : 'Release History'}
            </DialogTitle>
            <DialogDescription>
              {releaseDialogState?.mode === 'rollback'
                ? 'Choose a historical release to restore for the selected environment.'
                : 'Review historical release transitions for this resource.'}
            </DialogDescription>
          </DialogHeader>

          <div className='space-y-4 px-5 pb-5'>
            {releaseDialogState?.mode === 'rollback' ? (
              <div className='space-y-2'>
                <label className='text-sm font-medium'>Rollback Comment</label>
                <Input
                  value={releaseComment}
                  onChange={(event) => setReleaseComment(event.target.value)}
                  placeholder='Optional rollback comment'
                />
              </div>
            ) : null}

            {releaseHistoryError ? (
              <p className='text-sm text-destructive'>{releaseHistoryError}</p>
            ) : null}

            <div className='rounded-2xl border'>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Release</TableHead>
                    <TableHead>Previous</TableHead>
                    <TableHead>Created At</TableHead>
                    <TableHead className='w-[160px] text-right'>Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {releaseHistoryLoading ? (
                    <TableRow>
                      <TableCell colSpan={4} className='py-8 text-center text-sm text-muted-foreground'>
                        Loading release history…
                      </TableCell>
                    </TableRow>
                  ) : releaseHistory.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} className='py-8 text-center text-sm text-muted-foreground'>
                        No historical releases found for this environment.
                      </TableCell>
                    </TableRow>
                  ) : (
                    releaseHistory.map((item) => (
                      <TableRow key={item.id}>
                        <TableCell className='font-medium'>#{item.releaseId}</TableCell>
                        <TableCell>{item.previousReleaseId ? `#${item.previousReleaseId}` : '—'}</TableCell>
                        <TableCell>{formatDateTime(item.createdAt)}</TableCell>
                        <TableCell className='text-right'>
                          {releaseDialogState?.mode === 'rollback' ? (
                            <Button
                              type='button'
                              size='sm'
                              variant='outline'
                              className='rounded-xl'
                              disabled={
                                !releaseDialogState || isRollingBack === item.releaseId
                              }
                              onClick={() =>
                                releaseDialogState
                                  ? void handleRollback(releaseDialogState.resource, item.releaseId)
                                  : undefined
                              }
                            >
                              {isRollingBack === item.releaseId ? (
                                <LoaderCircle className='size-3.5 animate-spin' />
                              ) : (
                                <RotateCcw className='size-3.5' />
                              )}
                              Rollback
                            </Button>
                          ) : (
                            <span className='text-sm text-muted-foreground'>Recorded</span>
                          )}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          </div>

          <DialogFooter showCloseButton />
        </DialogContent>
      </Dialog>
    </>
  )
}

type ContentResourcePreviewProps = {
  applicationId: number
  resource: ConfigResource
  runtime: ResourceRuntime
}

function ContentResourcePreview({
  applicationId,
  resource,
  runtime,
}: ContentResourcePreviewProps) {
  const preview = runtime.content?.content?.trim() || runtime.latestRelease?.snapshot?.trim() || ''

  return (
    <div className='space-y-3'>
      <div className='flex items-center justify-between gap-3'>
        <div className='text-sm text-muted-foreground'>
          Previewing the current draft for this text configuration.
        </div>
        <div className='flex items-center gap-2'>
          <Button
            type='button'
            variant='outline'
            size='sm'
            className='rounded-xl'
            onClick={() => {
              if (!preview) {
                return
              }
              void navigator.clipboard.writeText(preview)
            }}
          >
            <Copy className='size-3.5' />
            Copy
          </Button>
          <Button type='button' variant='outline' size='sm' asChild className='rounded-xl'>
            <Link
              to='/applications/$applicationId/resources/$resourceId/content'
              params={{
                applicationId: String(applicationId),
                resourceId: String(resource.id),
              }}
            >
              <PencilLine className='size-3.5' />
              Open Editor
            </Link>
          </Button>
        </div>
      </div>

      <CodeBlock language={mapResourceFormatToCodeLanguage(resource.format)}>
        {preview || '# No content saved yet'}
      </CodeBlock>
    </div>
  )
}

type EntriesResourceTableProps = {
  resource: ConfigResource
  entries: ConfigEntry[]
  latestReleaseEntriesByKey: Map<string, ReleaseSnapshotEntry>
  onAddEntry: () => void
  onEditEntry: (entry: ConfigEntry) => void
  onDeleteEntry: (entry: ConfigEntry) => void
}

function EntriesResourceTable({
  resource,
  entries,
  latestReleaseEntriesByKey,
  onAddEntry,
  onEditEntry,
  onDeleteEntry,
}: EntriesResourceTableProps) {
  return (
    <div className='space-y-3'>
      <div className='flex items-center justify-between gap-3'>
        <div className='text-[13px] text-muted-foreground'>
          Inline KV editing with release-aware row status.
        </div>
        <Button type='button' variant='outline' size='xs' className='rounded-lg' onClick={onAddEntry}>
          <Plus className='size-3.5' />
          Add Entry
        </Button>
      </div>

      <div className='overflow-hidden rounded-2xl border'>
        <Table className='table-fixed'>
          <TableHeader>
            <TableRow>
              <TableHead className='h-9 w-[120px] px-3 text-[12px]'>Release Status</TableHead>
              <TableHead className='h-9 w-[190px] px-3 text-[12px]'>Key</TableHead>
              <TableHead className='h-9 w-[110px] px-3 text-[12px]'>Type</TableHead>
              <TableHead className='h-9 px-3 text-[12px]'>Value</TableHead>
              <TableHead className='h-9 w-[200px] px-3 text-[12px]'>Comment</TableHead>
              <TableHead className='h-9 w-[160px] px-3 text-[12px]'>Modified At</TableHead>
              <TableHead className='h-9 w-[124px] px-3 text-right text-[12px]'>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {entries.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className='px-3 py-8 text-center text-sm text-muted-foreground'>
                  No KV entries yet for {resource.name}.
                </TableCell>
              </TableRow>
            ) : (
              entries.map((entry) => {
                const releaseStatus = getEntryReleaseStatus(entry, latestReleaseEntriesByKey.get(entry.key))

                return (
                  <TableRow key={entry.id} className='align-top'>
                    <TableCell className='px-3 py-1.5'>
                      <StatusPill status={releaseStatus} />
                    </TableCell>
                    <TableCell className='px-3 py-1.5'>
                      <div className='space-y-0.5'>
                        <div className='truncate font-medium'>{entry.key}</div>
                      </div>
                    </TableCell>
                    <TableCell className='px-3 py-1.5 text-[12px] text-muted-foreground'>
                      {entry.valueType ?? 'STRING'}
                    </TableCell>
                    <TableCell className='px-3 py-1.5'>
                      <code className='block max-w-full overflow-hidden text-ellipsis whitespace-nowrap rounded-md bg-muted/45 px-2 py-0.5 text-[12px]'>
                        {entry.value}
                      </code>
                    </TableCell>
                    <TableCell className='px-3 py-1.5 text-[12px] text-muted-foreground'>
                      <span className='line-clamp-2'>{entry.description || '—'}</span>
                    </TableCell>
                    <TableCell className='px-3 py-1.5 text-[12px] text-muted-foreground'>
                      {formatDateTime(entry.updatedAt)}
                    </TableCell>
                    <TableCell className='px-3 py-1.5'>
                      <div className='flex justify-end gap-1.5'>
                        <Button
                          type='button'
                          variant='outline'
                          size='xs'
                          className='rounded-md'
                          onClick={() => onEditEntry(entry)}
                        >
                          Edit
                        </Button>
                        <Button
                          type='button'
                          variant='outline'
                          size='xs'
                          className='rounded-md'
                          onClick={() => onDeleteEntry(entry)}
                        >
                          Delete
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                )
              })
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}

function ApplicationDetailSkeleton() {
  return (
    <section className='flex min-h-0 flex-1 flex-col gap-5'>
      <div className='space-y-2'>
        <Skeleton className='h-10 w-64 rounded-xl' />
        <Skeleton className='h-4 w-[520px] rounded-lg' />
      </div>

      <div className='flex gap-3'>
        <Skeleton className='h-9 w-[220px] rounded-xl' />
        <Skeleton className='h-9 w-[180px] rounded-xl' />
        <Skeleton className='h-9 w-[320px] rounded-xl' />
      </div>

      <div className='space-y-3'>
        <ResourceCardSkeleton />
        <ResourceCardSkeleton />
        <ResourceCardSkeleton />
      </div>
    </section>
  )
}

function ResourceCardSkeleton() {
  return (
    <div className='rounded-2xl border p-4'>
      <div className='flex items-start justify-between gap-4'>
        <div className='flex gap-4'>
          <Skeleton className='size-12 rounded-xl' />
          <div className='space-y-2'>
            <Skeleton className='h-6 w-56 rounded-lg' />
            <Skeleton className='h-4 w-80 rounded-lg' />
            <Skeleton className='h-4 w-72 rounded-lg' />
          </div>
        </div>
        <div className='flex gap-2'>
          <Skeleton className='h-8 w-16 rounded-xl' />
          <Skeleton className='h-8 w-16 rounded-xl' />
          <Skeleton className='h-8 w-16 rounded-xl' />
        </div>
      </div>
      <Skeleton className='mt-4 h-40 rounded-2xl' />
    </div>
  )
}

function StatusDotLabel({ label }: { label: string }) {
  const tone =
    label === 'Published'
      ? 'bg-emerald-500'
      : label === 'Modified'
        ? 'bg-amber-500'
        : 'bg-slate-400'

  return (
    <span className='inline-flex items-center gap-2'>
      <span className={cn('size-2 rounded-full', tone)} />
      {label}
    </span>
  )
}

function StatusPill({ status }: { status: string }) {
  const className =
    status === 'Published'
      ? 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/60 dark:bg-emerald-950/30 dark:text-emerald-300'
      : status === 'Modified'
        ? 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-300'
        : 'border-slate-200 bg-slate-50 text-slate-700 dark:border-slate-800 dark:bg-slate-900/40 dark:text-slate-300'

  return <span className={cn('inline-flex rounded-full border px-2 py-0 text-[10px] font-medium leading-5', className)}>{status}</span>
}

function ResourceFormatBadge({ resource }: { resource: ConfigResource }) {
  const label = resource.type === 'ENTRIES' ? 'kv' : normalizeFormatLabel(resource.format).toLowerCase()
  const className =
    resource.type === 'ENTRIES'
      ? 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/60 dark:bg-emerald-950/30 dark:text-emerald-300'
      : resource.format === 'YAML'
        ? 'border-sky-200 bg-sky-50 text-sky-700 dark:border-sky-900/60 dark:bg-sky-950/30 dark:text-sky-300'
        : resource.format === 'JSON'
          ? 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-300'
          : resource.format === 'PROPERTIES'
            ? 'border-violet-200 bg-violet-50 text-violet-700 dark:border-violet-900/60 dark:bg-violet-950/30 dark:text-violet-300'
            : resource.format === 'XML'
              ? 'border-rose-200 bg-rose-50 text-rose-700 dark:border-rose-900/60 dark:bg-rose-950/30 dark:text-rose-300'
              : 'border-slate-200 bg-slate-50 text-slate-700 dark:border-slate-800 dark:bg-slate-900/40 dark:text-slate-300'

  return (
    <Badge className={cn('rounded-full border px-2 py-0 text-[10px] uppercase', className)}>
      {label}
    </Badge>
  )
}

function parseReleaseSnapshotEntries(snapshot?: string | null) {
  if (!snapshot) {
    return [] as ReleaseSnapshotEntry[]
  }

  try {
    const parsed = JSON.parse(snapshot) as unknown
    if (!Array.isArray(parsed)) {
      return []
    }

    return parsed
      .map((candidate) => {
        if (!candidate || typeof candidate !== 'object') {
          return null
        }

        const value = candidate as Record<string, unknown>
        return {
          key: String(value.key ?? ''),
          value: String(value.value ?? ''),
          valueType: value.valueType == null ? null : String(value.valueType),
          description: value.description == null ? null : String(value.description),
        } satisfies ReleaseSnapshotEntry
      })
      .filter((candidate): candidate is ReleaseSnapshotEntry => Boolean(candidate?.key))
  } catch {
    return []
  }
}

function getResourceCardStatus(
  resource: ConfigResource,
  runtime: ResourceRuntime,
  latestReleaseEntries: ReleaseSnapshotEntry[],
) {
  if (resource.type === 'CONTENT') {
    if (!runtime.latestRelease) {
      return runtime.content?.content ? 'Draft' : 'Unpublished'
    }

    return (runtime.content?.content ?? '') === runtime.latestRelease.snapshot
      ? 'Published'
      : 'Modified'
  }

  if (!runtime.latestRelease) {
    return runtime.entries.length > 0 ? 'Draft' : 'Unpublished'
  }

  if (runtime.entries.length !== latestReleaseEntries.length) {
    return 'Modified'
  }

  const isSame = runtime.entries.every((entry) => {
    const released = latestReleaseEntries.find((candidate) => candidate.key === entry.key)
    return (
      released &&
      released.value === entry.value &&
      (released.valueType ?? 'STRING') === (entry.valueType ?? 'STRING') &&
      (released.description ?? '') === (entry.description ?? '')
    )
  })

  return isSame ? 'Published' : 'Modified'
}

function getEntryReleaseStatus(entry: ConfigEntry, releasedEntry?: ReleaseSnapshotEntry) {
  if (!releasedEntry) {
    return 'Draft'
  }

  const matches =
    releasedEntry.value === entry.value &&
    (releasedEntry.valueType ?? 'STRING') === (entry.valueType ?? 'STRING') &&
    (releasedEntry.description ?? '') === (entry.description ?? '')

  return matches ? 'Published' : 'Modified'
}

function getResourceUpdatedAt(resource: ConfigResource, runtime: ResourceRuntime) {
  if (resource.type === 'CONTENT') {
    return runtime.content?.updatedAt ?? runtime.latestRelease?.createdAt ?? resource.updatedAt
  }

  const timestamps = runtime.entries.map((entry) => new Date(entry.updatedAt).getTime())
  if (timestamps.length === 0) {
    return runtime.latestRelease?.createdAt ?? resource.updatedAt
  }

  return new Date(Math.max(...timestamps)).toISOString()
}

function getFallbackDescription(resource: ConfigResource) {
  return resource.type === 'CONTENT'
    ? 'Text configuration resource ready for side-by-side editing.'
    : 'KV configuration resource managed inline for the selected environment.'
}

function normalizeFormatLabel(format: ConfigFormat) {
  return format === 'PROPERTIES' ? 'Properties' : format
}

function mapResourceFormatToCodeLanguage(format: ConfigFormat) {
  switch (format) {
    case 'YAML':
      return 'yaml' as const
    case 'JSON':
      return 'json' as const
    case 'PROPERTIES':
      return 'properties' as const
    case 'XML':
      return 'xml' as const
    default:
      return 'plaintext' as const
  }
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '—'
  }

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function isNotFoundError(error: unknown) {
  return error instanceof ApiError && error.status === 404
}
