import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useParams } from '@tanstack/react-router'
import {
  ChevronRight,
  Database,
  FileCode2,
  LoaderCircle,
  PencilLine,
  Plus,
  RefreshCcw,
  Search,
  Settings2,
  ShieldCheck,
  Trash2,
  TriangleAlert,
  UsersRound,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
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
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '@/providers/auth-provider'
import {
  getApplication,
  getApplicationPublishPolicy,
  saveApplicationPublishPolicy,
  type Application,
  type ApplicationPublishPolicy,
} from '@/lib/applications'
import {
  getLatestConfigContent,
  type ConfigContent,
} from '@/lib/config-content'
import {
  createConfigResource,
  listConfigResources,
  type ConfigFormat,
  type ConfigResource,
  type ConfigType,
} from '@/lib/config-resources'
import { getLatestConfigRelease, type ConfigRelease } from '@/lib/config-releases'
import {
  deleteConfigEntry,
  listConfigEntries,
  saveConfigEntry,
  type ConfigEntry,
  type ConfigEntryPayload,
} from '@/lib/config-entries'
import { listEnvironments, type Environment } from '@/lib/environments'

type LoadState = 'loading' | 'ready' | 'error'
type ResourceFilter = 'ALL' | 'CONTENT' | 'ENTRIES'

const configTypeOptions: Array<{ label: string; value: ConfigType }> = [
  { label: 'Text Content', value: 'CONTENT' },
  { label: 'Structured Entries', value: 'ENTRIES' },
]

const configFormatOptions: Array<{ label: string; value: ConfigFormat }> = [
  { label: 'YAML', value: 'YAML' },
  { label: 'Properties', value: 'PROPERTIES' },
  { label: 'JSON', value: 'JSON' },
  { label: 'TOML', value: 'TOML' },
  { label: 'XML', value: 'XML' },
  { label: 'None', value: 'NONE' },
]

const resourceFilterOptions: Array<{ label: string; value: ResourceFilter }> = [
  { label: 'All Resources', value: 'ALL' },
  { label: 'Text Resources', value: 'CONTENT' },
  { label: 'KV Resources', value: 'ENTRIES' },
]

export function ApplicationDetailPage() {
  const { applicationId } = useParams({ from: '/_protected/applications/$applicationId' })
  const { user } = useAuth()
  const numericApplicationId = Number(applicationId)
  const [application, setApplication] = useState<Application | null>(null)
  const [resources, setResources] = useState<ConfigResource[]>([])
  const [environments, setEnvironments] = useState<Environment[]>([])
  const [policies, setPolicies] = useState<Record<number, ApplicationPublishPolicy>>({})
  const [loadState, setLoadState] = useState<LoadState>('loading')
  const [errorMessage, setErrorMessage] = useState('')
  const [requestVersion, setRequestVersion] = useState(0)
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [createErrorMessage, setCreateErrorMessage] = useState('')
  const [isCreateSubmitting, setIsCreateSubmitting] = useState(false)
  const [createType, setCreateType] = useState<ConfigType>('CONTENT')
  const [createFormat, setCreateFormat] = useState<ConfigFormat>('YAML')
  const [updatingPolicyId, setUpdatingPolicyId] = useState<number | null>(null)
  const [selectedEnvironmentId, setSelectedEnvironmentId] = useState('')
  const [resourceFilter, setResourceFilter] = useState<ResourceFilter>('ALL')
  const [searchQuery, setSearchQuery] = useState('')

  useEffect(() => {
    const abortController = new AbortController()

    async function load() {
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

        const policyEntries = await Promise.all(
          nextEnvironments.map(async (environment) => {
            const policy = await getApplicationPublishPolicy(
              numericApplicationId,
              environment.id,
            )

            return [environment.id, policy] as const
          }),
        )

        if (abortController.signal.aborted) {
          return
        }

        setApplication(nextApplication)
        setResources(nextResources.toSorted((left, right) => left.name.localeCompare(right.name)))
        setEnvironments(nextEnvironments)
        setPolicies(Object.fromEntries(policyEntries))
        setSelectedEnvironmentId((current) =>
          current || (nextEnvironments[0] ? String(nextEnvironments[0].id) : ''),
        )
        setLoadState('ready')
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        setErrorMessage(
          error instanceof Error ? error.message : 'Failed to load application details.',
        )
        setLoadState('error')
      }
    }

    void load()

    return () => abortController.abort()
  }, [numericApplicationId, requestVersion])

  const canManage = useMemo(() => {
    if (!user || !application) {
      return false
    }

    return (
      user.roles.includes('SUPER_ADMIN') ||
      user.id === application.owner.id ||
      user.id === application.maintainer.id
    )
  }, [application, user])

  const selectedEnvironment = useMemo(
    () => environments.find((environment) => environment.id === Number(selectedEnvironmentId)) ?? null,
    [environments, selectedEnvironmentId],
  )

  const filteredResources = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase()

    return resources.filter((resource) => {
      const matchesFilter =
        resourceFilter === 'ALL' ||
        (resourceFilter === 'CONTENT' && resource.type === 'CONTENT') ||
        (resourceFilter === 'ENTRIES' && resource.type === 'ENTRIES')

      if (!matchesFilter) {
        return false
      }

      if (!normalizedQuery) {
        return true
      }

      return [resource.name, resource.description ?? '', getResourceFormatLabel(resource)]
        .join(' ')
        .toLowerCase()
        .includes(normalizedQuery)
    })
  }, [resourceFilter, resources, searchQuery])

  const retryLoad = () => {
    setRequestVersion((currentVersion) => currentVersion + 1)
  }

  const handleCreateResource = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setCreateErrorMessage('')
    setIsCreateSubmitting(true)

    const formData = new FormData(event.currentTarget)

    try {
      const resource = await createConfigResource(numericApplicationId, {
        configName: String(formData.get('configName') ?? ''),
        type: createType,
        format: createFormat,
        version: null,
        description: String(formData.get('description') ?? ''),
      })

      setResources((current) =>
        current.concat(resource).toSorted((left, right) => left.name.localeCompare(right.name)),
      )
      setIsCreateOpen(false)
      setCreateType('CONTENT')
      setCreateFormat('YAML')
      event.currentTarget.reset()
    } catch (error) {
      setCreateErrorMessage(
        error instanceof Error ? error.message : 'Failed to create config resource.',
      )
    } finally {
      setIsCreateSubmitting(false)
    }
  }

  const handlePolicyChange = async (environmentId: number, checked: boolean) => {
    setUpdatingPolicyId(environmentId)

    try {
      const nextPolicy = await saveApplicationPublishPolicy(
        numericApplicationId,
        environmentId,
        checked,
      )

      setPolicies((current) => ({
        ...current,
        [environmentId]: nextPolicy,
      }))
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to update publish policy.',
      )
    } finally {
      setUpdatingPolicyId(null)
    }
  }

  if (loadState === 'loading') {
    return <ApplicationDetailSkeleton />
  }

  if (loadState === 'error' || !application) {
    return (
      <section className='flex min-h-0 flex-1 flex-col gap-6'>
        <Empty className='min-h-full border'>
          <EmptyHeader>
            <EmptyMedia variant='icon'>
              <TriangleAlert />
            </EmptyMedia>
            <EmptyTitle>Unable to load application</EmptyTitle>
            <EmptyDescription>{errorMessage}</EmptyDescription>
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
          <span className='text-foreground'>{application.name}</span>
        </div>

        <div className='space-y-2'>
          <h1 className='text-3xl font-bold tracking-tight'>Configurations</h1>
          <p className='max-w-3xl text-sm text-muted-foreground'>
            Manage configuration resources for <span className='font-medium text-foreground'>{application.name}</span>{' '}
            across environments. Text resources open in the dedicated editor, and KV resources can be edited inline.
          </p>
        </div>
      </div>

      <div className='flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between'>
        <div className='flex flex-1 flex-col gap-3 md:flex-row'>
          <Select value={selectedEnvironmentId} onValueChange={setSelectedEnvironmentId}>
            <SelectTrigger className='w-full md:w-56'>
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

          <Select value={resourceFilter} onValueChange={(value) => setResourceFilter(value as ResourceFilter)}>
            <SelectTrigger className='w-full md:w-52'>
              <SelectValue placeholder='Resources' />
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                {resourceFilterOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectGroup>
            </SelectContent>
          </Select>

          <div className='relative min-w-0 flex-1'>
            <Search className='pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground' />
            <Input
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              className='pl-9'
              placeholder='Search resources...'
            />
          </div>
        </div>

        {canManage ? (
          <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
            <DialogTrigger asChild>
              <Button type='button'>
                <Plus className='size-4' />
                Create Resource
              </Button>
            </DialogTrigger>
            <DialogContent className='sm:max-w-lg'>
              <DialogHeader>
                <DialogTitle>Create config resource</DialogTitle>
                <DialogDescription>
                  Add a new resource inside this application. Text resources open in a Monaco editor, while KV resources stay on this page.
                </DialogDescription>
              </DialogHeader>

              <form className='flex flex-col gap-4' onSubmit={handleCreateResource}>
                <div className='flex flex-col gap-2'>
                  <Label htmlFor='resource-name'>Config Name</Label>
                  <Input
                    id='resource-name'
                    name='configName'
                    placeholder='application.yaml'
                    required
                  />
                </div>

                <div className='grid gap-4 md:grid-cols-2'>
                  <div className='flex flex-col gap-2'>
                    <Label htmlFor='resource-type'>Type</Label>
                    <Select
                      value={createType}
                      onValueChange={(value) => {
                        const nextType = value as ConfigType
                        setCreateType(nextType)
                        if (nextType === 'ENTRIES') {
                          setCreateFormat('NONE')
                        } else if (createFormat === 'NONE') {
                          setCreateFormat('YAML')
                        }
                      }}
                    >
                      <SelectTrigger id='resource-type' className='w-full'>
                        <SelectValue placeholder='Type' />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectGroup>
                          {configTypeOptions.map((option) => (
                            <SelectItem key={option.value} value={option.value}>
                              {option.label}
                            </SelectItem>
                          ))}
                        </SelectGroup>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className='flex flex-col gap-2'>
                    <Label htmlFor='resource-format'>Format</Label>
                    <Select
                      value={createFormat}
                      onValueChange={(value) => setCreateFormat(value as ConfigFormat)}
                    >
                      <SelectTrigger id='resource-format' className='w-full'>
                        <SelectValue placeholder='Format' />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectGroup>
                          {configFormatOptions
                            .filter((option) =>
                              createType === 'ENTRIES'
                                ? option.value === 'NONE'
                                : option.value !== 'NONE',
                            )
                            .map((option) => (
                              <SelectItem key={option.value} value={option.value}>
                                {option.label}
                              </SelectItem>
                            ))}
                        </SelectGroup>
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div className='flex flex-col gap-2'>
                  <Label htmlFor='resource-description'>Description</Label>
                  <Input
                    id='resource-description'
                    name='description'
                    placeholder='Shared runtime configuration'
                  />
                </div>

                {createErrorMessage ? (
                  <p className='text-sm text-destructive'>{createErrorMessage}</p>
                ) : null}

                <DialogFooter>
                  <Button type='submit' disabled={isCreateSubmitting}>
                    {isCreateSubmitting ? <LoaderCircle className='size-4 animate-spin' /> : null}
                    Create Resource
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        ) : null}
      </div>

      {filteredResources.length === 0 ? (
        <Empty className='min-h-[360px] rounded-2xl border'>
          <EmptyHeader>
            <EmptyMedia variant='icon'>
              <FileCode2 />
            </EmptyMedia>
            <EmptyTitle>No matching resources</EmptyTitle>
            <EmptyDescription>
              Adjust the current filters or create a new resource for this application.
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <div className='space-y-4'>
          {filteredResources.map((resource) => (
            <Card key={resource.id} className='overflow-hidden rounded-2xl'>
              <CardContent className='space-y-5 p-6'>
                <div className='flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between'>
                  <div className='flex min-w-0 items-start gap-4'>
                    <div className='flex size-12 shrink-0 items-center justify-center rounded-2xl border bg-muted/60'>
                      {resource.type === 'CONTENT' ? (
                        <FileCode2 className='size-5' />
                      ) : (
                        <Database className='size-5' />
                      )}
                    </div>
                    <div className='min-w-0 space-y-2'>
                      <div className='flex flex-wrap items-center gap-3'>
                        <h2 className='truncate text-xl font-semibold'>{resource.name}</h2>
                        <Badge variant='secondary' className='rounded-full px-3 py-1 text-xs uppercase'>
                          {getResourceFormatLabel(resource)}
                        </Badge>
                      </div>
                      <p className='max-w-3xl text-sm text-muted-foreground'>
                        {resource.description?.trim() || 'No description provided yet.'}
                      </p>
                      <div className='flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-muted-foreground'>
                        <span>#{resource.id}</span>
                        <span>Updated {formatDateTime(resource.updatedAt)}</span>
                        {selectedEnvironment ? <span>Environment: {selectedEnvironment.code}</span> : null}
                      </div>
                    </div>
                  </div>

                  {resource.type === 'CONTENT' ? (
                    <Button type='button' variant='outline' asChild className='shrink-0'>
                      <Link
                        to='/applications/$applicationId/resources/$resourceId/content'
                        params={{
                          applicationId: String(application.id),
                          resourceId: String(resource.id),
                        }}
                      >
                        <PencilLine className='size-4' />
                        Edit
                      </Link>
                    </Button>
                  ) : null}
                </div>

                {resource.type === 'CONTENT' ? (
                  <ContentResourcePreview resource={resource} environment={selectedEnvironment} />
                ) : (
                  <EntriesResourceEditor
                    resource={resource}
                    environment={selectedEnvironment}
                    canManage={canManage}
                  />
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <div className='grid gap-6 xl:grid-cols-[1.1fr_0.9fr]'>
        <Card>
          <CardHeader>
            <CardTitle>Publish Policies</CardTitle>
            <CardDescription>
              Control whether developers need approval before publishing in each environment.
            </CardDescription>
          </CardHeader>
          <CardContent className='grid gap-4'>
            {environments.length === 0 ? (
              <div className='rounded-lg border bg-muted/40 px-4 py-3 text-sm text-muted-foreground'>
                Create environments first to configure environment-level publish rules.
              </div>
            ) : (
              environments.map((environment) => {
                const policy = policies[environment.id]
                const checked = policy?.publishRequiresApproval ?? true

                return (
                  <div
                    key={environment.id}
                    className='flex flex-col gap-3 rounded-xl border p-4 md:flex-row md:items-center md:justify-between'
                  >
                    <div className='space-y-1'>
                      <div className='flex items-center gap-2'>
                        <p className='font-medium'>{environment.name}</p>
                        <Badge variant='outline'>{environment.code}</Badge>
                      </div>
                      <p className='text-sm text-muted-foreground'>
                        {environment.description?.trim() ||
                          'No environment description configured.'}
                      </p>
                    </div>

                    <div className='flex items-center gap-3'>
                      <Label
                        htmlFor={`policy-${environment.id}`}
                        className='text-sm text-muted-foreground'
                      >
                        Developer publish requires approval
                      </Label>
                      <Switch
                        id={`policy-${environment.id}`}
                        checked={checked}
                        disabled={!canManage || updatingPolicyId === environment.id}
                        onCheckedChange={(value) =>
                          void handlePolicyChange(environment.id, value)
                        }
                      />
                    </div>
                  </div>
                )
              })
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Application Access</CardTitle>
            <CardDescription>
              Application membership controls who can edit, review, and publish configs.
            </CardDescription>
          </CardHeader>
          <CardContent className='space-y-4'>
            <MemberRow
              icon={<ShieldCheck className='size-4' />}
              label='Owner'
              username={application.owner.username}
              email={application.owner.email}
            />
            <MemberRow
              icon={<Settings2 className='size-4' />}
              label='Maintainer'
              username={application.maintainer.username}
              email={application.maintainer.email}
            />
            <div className='space-y-3 rounded-xl border p-4'>
              <div className='flex items-center gap-2 text-sm font-medium'>
                <UsersRound className='size-4' />
                Developers
              </div>
              {application.developers.length === 0 ? (
                <p className='text-sm text-muted-foreground'>No developers assigned yet.</p>
              ) : (
                <div className='grid gap-3'>
                  {application.developers.map((developer) => (
                    <div
                      key={developer.id}
                      className='rounded-lg bg-muted/40 px-3 py-2 text-sm'
                    >
                      <p className='font-medium'>{developer.username}</p>
                      <p className='text-muted-foreground'>{developer.email}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </section>
  )
}

type ContentResourcePreviewProps = {
  resource: ConfigResource
  environment: Environment | null
}

function ContentResourcePreview({ resource, environment }: ContentResourcePreviewProps) {
  const [contentState, setContentState] = useState<'idle' | 'loading' | 'ready' | 'error'>('idle')
  const [content, setContent] = useState<ConfigContent | null>(null)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    if (!environment) {
      return
    }

    const abortController = new AbortController()
    const environmentId = environment.id

    async function loadPreview() {
      setContentState('loading')
      setErrorMessage('')

      try {
        const nextContent = await getLatestConfigContent(
          resource.id,
          environmentId,
          abortController.signal,
        )

        if (abortController.signal.aborted) {
          return
        }

        setContent(nextContent)
        setContentState('ready')
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        setContent(null)
        setContentState('error')
        setErrorMessage(
          error instanceof Error ? error.message : 'Failed to load preview content.',
        )
      }
    }

    void loadPreview()

    return () => abortController.abort()
  }, [environment, resource.id])

  if (!environment) {
    return (
      <div className='rounded-2xl border bg-muted/20 px-4 py-10 text-sm text-muted-foreground'>
        Create an environment first to edit and preview content resources.
      </div>
    )
  }

  if (contentState === 'loading') {
    return (
      <div className='rounded-2xl border bg-muted/20 p-4'>
        <Skeleton className='h-32 rounded-xl' />
      </div>
    )
  }

  if (contentState === 'error') {
    return (
      <div className='rounded-2xl border border-dashed px-4 py-10 text-sm text-muted-foreground'>
        {errorMessage || 'No draft content has been saved in this environment yet.'}
      </div>
    )
  }

  const previewLines = (content?.content ?? '')
    .split('\n')
    .slice(0, 8)
    .join('\n')

  return (
    <div className='overflow-hidden rounded-2xl border bg-muted/10'>
      <div className='border-b px-4 py-3 text-sm text-muted-foreground'>
        {environment.code} draft preview
      </div>
      {previewLines.trim() ? (
        <pre className='overflow-x-auto px-4 py-4 text-sm leading-6 whitespace-pre-wrap'>
          <code>{previewLines}</code>
        </pre>
      ) : (
        <div className='px-4 py-10 text-sm text-muted-foreground'>
          No content saved for this environment yet.
        </div>
      )}
    </div>
  )
}

type EntriesResourceEditorProps = {
  resource: ConfigResource
  environment: Environment | null
  canManage: boolean
}

function EntriesResourceEditor({
  resource,
  environment,
  canManage,
}: EntriesResourceEditorProps) {
  const [entries, setEntries] = useState<ConfigEntry[]>([])
  const [latestRelease, setLatestRelease] = useState<ConfigRelease | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [editingEntry, setEditingEntry] = useState<ConfigEntry | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [deletingEntryId, setDeletingEntryId] = useState<number | null>(null)

  useEffect(() => {
    if (!environment) {
      setEntries([])
      setLatestRelease(null)
      return
    }

    const abortController = new AbortController()
    const environmentId = environment.id

    async function loadEntries() {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const [nextEntries, nextRelease] = await Promise.all([
          listConfigEntries(resource.id, environmentId, abortController.signal),
          getLatestConfigRelease(resource.id, environmentId, abortController.signal).catch(
            () => null,
          ),
        ])

        if (abortController.signal.aborted) {
          return
        }

        setEntries(nextEntries.toSorted((left, right) => left.key.localeCompare(right.key)))
        setLatestRelease(nextRelease)
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        setErrorMessage(
          error instanceof Error ? error.message : 'Failed to load entries.',
        )
      } finally {
        if (!abortController.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    void loadEntries()

    return () => abortController.abort()
  }, [environment, resource.id])

  const releaseEntryMap = useMemo(() => {
    if (!latestRelease?.snapshot) {
      return new Map<string, ReleasedEntrySnapshot>()
    }

    try {
      const parsed = JSON.parse(latestRelease.snapshot)
      if (!Array.isArray(parsed)) {
        return new Map<string, ReleasedEntrySnapshot>()
      }

      return new Map(
        parsed
          .filter((item): item is ReleasedEntrySnapshot => typeof item?.key === 'string')
          .map((item) => [item.key, item]),
      )
    } catch {
      return new Map<string, ReleasedEntrySnapshot>()
    }
  }, [latestRelease])

  const tableRows = useMemo(
    () =>
      entries.map((entry) => ({
        entry,
        status: getEntryReleaseStatus(entry, releaseEntryMap),
      })),
    [entries, releaseEntryMap],
  )

  const handleSaveEntry = async (
    event: FormEvent<HTMLFormElement>,
    mode: 'create' | 'edit',
    currentKey?: string,
  ) => {
    event.preventDefault()

    if (!environment) {
      return
    }

    setIsSubmitting(true)
    setErrorMessage('')

    const formData = new FormData(event.currentTarget)
    const payload: ConfigEntryPayload = {
      key: currentKey ?? String(formData.get('key') ?? ''),
      value: String(formData.get('value') ?? ''),
      valueType: String(formData.get('valueType') ?? ''),
      description: String(formData.get('description') ?? ''),
    }

    try {
      const savedEntry = await saveConfigEntry(resource.id, environment.id, payload)

      setEntries((current) =>
        current
          .filter((entry) => entry.id !== savedEntry.id && entry.key !== savedEntry.key)
          .concat(savedEntry)
          .toSorted((left, right) => left.key.localeCompare(right.key)),
      )

      if (mode === 'create') {
        setIsCreateOpen(false)
      } else {
        setEditingEntry(null)
      }
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to save entry.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDeleteEntry = async (entryId: number) => {
    if (!environment) {
      return
    }

    setDeletingEntryId(entryId)
    setErrorMessage('')

    try {
      await deleteConfigEntry(resource.id, environment.id, entryId)
      setEntries((current) => current.filter((entry) => entry.id !== entryId))
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to delete entry.',
      )
    } finally {
      setDeletingEntryId(null)
    }
  }

  if (!environment) {
    return (
      <div className='rounded-2xl border bg-muted/20 px-4 py-10 text-sm text-muted-foreground'>
        Create an environment first to edit KV resources.
      </div>
    )
  }

  return (
    <div className='space-y-4'>
      <div className='flex flex-col gap-3 rounded-2xl border bg-muted/10 p-4 md:flex-row md:items-center md:justify-between'>
        <div className='space-y-1'>
          <p className='text-sm font-medium'>KV entries for {environment.code}</p>
          <p className='text-sm text-muted-foreground'>
            Edit configuration items directly here. Each row can be updated or removed independently.
          </p>
        </div>
        <div className='flex items-center gap-2'>
          <Badge variant='outline'>{entries.length} items</Badge>
          {canManage ? (
            <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
              <DialogTrigger asChild>
                <Button type='button' variant='outline'>
                  <Plus className='size-4' />
                  Add Entry
                </Button>
              </DialogTrigger>
              <DialogContent className='sm:max-w-lg'>
                <DialogHeader>
                  <DialogTitle>Create KV entry</DialogTitle>
                  <DialogDescription>
                    Add a new key-value item to {resource.name} in {environment.code}.
                  </DialogDescription>
                </DialogHeader>
                <form className='space-y-4' onSubmit={(event) => void handleSaveEntry(event, 'create')}>
                  <div className='grid gap-4 md:grid-cols-2'>
                    <div className='flex flex-col gap-2'>
                      <Label htmlFor={`entry-key-create-${resource.id}`}>Key</Label>
                      <Input
                        id={`entry-key-create-${resource.id}`}
                        name='key'
                        placeholder='feature.toggle'
                        required
                      />
                    </div>
                    <div className='flex flex-col gap-2'>
                      <Label htmlFor={`entry-type-create-${resource.id}`}>Value Type</Label>
                      <Input
                        id={`entry-type-create-${resource.id}`}
                        name='valueType'
                        placeholder='string'
                      />
                    </div>
                  </div>

                  <div className='flex flex-col gap-2'>
                    <Label htmlFor={`entry-value-create-${resource.id}`}>Value</Label>
                    <Input
                      id={`entry-value-create-${resource.id}`}
                      name='value'
                      placeholder='true'
                      required
                    />
                  </div>

                  <div className='flex flex-col gap-2'>
                    <Label htmlFor={`entry-description-create-${resource.id}`}>Description</Label>
                    <Input
                      id={`entry-description-create-${resource.id}`}
                      name='description'
                      placeholder='Controls whether the feature is enabled'
                    />
                  </div>

                  {errorMessage ? <p className='text-sm text-destructive'>{errorMessage}</p> : null}

                  <DialogFooter>
                    <Button type='submit' disabled={isSubmitting}>
                      {isSubmitting ? <LoaderCircle className='size-4 animate-spin' /> : null}
                      Save Entry
                    </Button>
                  </DialogFooter>
                </form>
              </DialogContent>
            </Dialog>
          ) : null}
        </div>
      </div>

      {errorMessage ? <p className='text-sm text-destructive'>{errorMessage}</p> : null}

      {isLoading ? (
        <div className='grid gap-3'>
          <Skeleton className='h-28 rounded-2xl' />
          <Skeleton className='h-28 rounded-2xl' />
        </div>
      ) : entries.length === 0 ? (
        <div className='rounded-2xl border border-dashed px-4 py-10 text-sm text-muted-foreground'>
          No entries in this environment yet.
        </div>
      ) : (
        <div className='overflow-hidden rounded-2xl border'>
          <Table>
            <TableHeader>
              <TableRow className='hover:bg-transparent'>
                <TableHead className='w-[140px]'>Release Status</TableHead>
                <TableHead className='min-w-[180px]'>Key</TableHead>
                <TableHead className='w-[120px]'>Type</TableHead>
                <TableHead className='min-w-[280px]'>Value</TableHead>
                <TableHead className='min-w-[220px]'>Comment</TableHead>
                <TableHead className='w-[180px]'>Modified At</TableHead>
                <TableHead className='w-[160px] text-right'>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {tableRows.map(({ entry, status }) => (
                <TableRow key={entry.id} className='align-top'>
                  <TableCell className='py-4'>
                    <Badge className={getReleaseStatusBadgeClassName(status)} variant='secondary'>
                      {status}
                    </Badge>
                  </TableCell>
                  <TableCell className='py-4 font-mono text-sm font-semibold'>
                    {entry.key}
                  </TableCell>
                  <TableCell className='py-4 text-sm'>
                    {entry.valueType || '-'}
                  </TableCell>
                  <TableCell className='py-4'>
                    <div className='max-w-xl rounded-xl bg-muted/40 px-3 py-3'>
                      <p className='break-all font-mono text-sm leading-6'>{entry.value}</p>
                    </div>
                  </TableCell>
                  <TableCell className='py-4 text-sm text-muted-foreground'>
                    {entry.description?.trim() || '-'}
                  </TableCell>
                  <TableCell className='py-4 text-sm text-muted-foreground'>
                    {formatDateTime(entry.updatedAt)}
                  </TableCell>
                  <TableCell className='py-4'>
                    <div className='flex justify-end gap-2'>
                      {canManage ? (
                        <Button
                          type='button'
                          variant='outline'
                          size='sm'
                          onClick={() => setEditingEntry(entry)}
                        >
                          <PencilLine className='size-4' />
                          Edit
                        </Button>
                      ) : null}
                      {canManage ? (
                        <Button
                          type='button'
                          variant='outline'
                          size='sm'
                          disabled={deletingEntryId === entry.id}
                          onClick={() => void handleDeleteEntry(entry.id)}
                        >
                          {deletingEntryId === entry.id ? (
                            <LoaderCircle className='size-4 animate-spin' />
                          ) : (
                            <Trash2 className='size-4' />
                          )}
                          Delete
                        </Button>
                      ) : null}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <Dialog open={editingEntry !== null} onOpenChange={(open) => !open && setEditingEntry(null)}>
        <DialogContent className='sm:max-w-lg'>
          <DialogHeader>
            <DialogTitle>Edit KV entry</DialogTitle>
            <DialogDescription>
              Update the value and description for {editingEntry?.key ?? 'this item'} in {environment.code}.
            </DialogDescription>
          </DialogHeader>
          {editingEntry ? (
            <form
              className='space-y-4'
              onSubmit={(event) => void handleSaveEntry(event, 'edit', editingEntry.key)}
            >
              <div className='grid gap-4 md:grid-cols-2'>
                <div className='flex flex-col gap-2'>
                  <Label htmlFor={`entry-key-edit-${resource.id}`}>Key</Label>
                  <Input
                    id={`entry-key-edit-${resource.id}`}
                    value={editingEntry.key}
                    disabled
                    readOnly
                  />
                </div>
                <div className='flex flex-col gap-2'>
                  <Label htmlFor={`entry-type-edit-${resource.id}`}>Value Type</Label>
                  <Input
                    id={`entry-type-edit-${resource.id}`}
                    name='valueType'
                    defaultValue={editingEntry.valueType ?? ''}
                    placeholder='string'
                  />
                </div>
              </div>

              <div className='flex flex-col gap-2'>
                <Label htmlFor={`entry-value-edit-${resource.id}`}>Value</Label>
                <Input
                  id={`entry-value-edit-${resource.id}`}
                  name='value'
                  defaultValue={editingEntry.value}
                  required
                />
              </div>

              <div className='flex flex-col gap-2'>
                <Label htmlFor={`entry-description-edit-${resource.id}`}>Description</Label>
                <Input
                  id={`entry-description-edit-${resource.id}`}
                  name='description'
                  defaultValue={editingEntry.description ?? ''}
                  placeholder='Controls whether the feature is enabled'
                />
              </div>

              {errorMessage ? <p className='text-sm text-destructive'>{errorMessage}</p> : null}

              <DialogFooter>
                <Button type='submit' disabled={isSubmitting}>
                  {isSubmitting ? <LoaderCircle className='size-4 animate-spin' /> : null}
                  Save Changes
                </Button>
              </DialogFooter>
            </form>
          ) : null}
        </DialogContent>
      </Dialog>
    </div>
  )
}

type ReleasedEntrySnapshot = {
  key: string
  value?: string | null
  valueType?: string | null
  description?: string | null
}

type EntryReleaseStatus = 'Published' | 'Modified' | 'Draft'

function getEntryReleaseStatus(
  entry: ConfigEntry,
  releaseEntryMap: Map<string, ReleasedEntrySnapshot>,
): EntryReleaseStatus {
  const releasedEntry = releaseEntryMap.get(entry.key)

  if (!releasedEntry) {
    return 'Draft'
  }

  const isSame =
    (releasedEntry.value ?? '') === entry.value &&
    (releasedEntry.valueType ?? '') === (entry.valueType ?? '') &&
    (releasedEntry.description ?? '') === (entry.description ?? '')

  return isSame ? 'Published' : 'Modified'
}

function getReleaseStatusBadgeClassName(status: EntryReleaseStatus) {
  if (status === 'Published') {
    return 'bg-emerald-500/12 text-emerald-700 hover:bg-emerald-500/12 dark:text-emerald-300'
  }
  if (status === 'Modified') {
    return 'bg-amber-500/12 text-amber-700 hover:bg-amber-500/12 dark:text-amber-300'
  }

  return 'bg-slate-500/12 text-slate-700 hover:bg-slate-500/12 dark:text-slate-300'
}

type MemberRowProps = {
  icon: ReactNode
  label: string
  username: string
  email: string
}

function MemberRow({ icon, label, username, email }: MemberRowProps) {
  return (
    <div className='flex items-start gap-3 rounded-xl border p-4'>
      <div className='mt-0.5 text-muted-foreground'>{icon}</div>
      <div className='space-y-1'>
        <p className='text-sm font-medium'>{label}</p>
        <p className='text-sm'>{username}</p>
        <p className='text-sm text-muted-foreground'>{email}</p>
      </div>
    </div>
  )
}

function ApplicationDetailSkeleton() {
  return (
    <section className='flex min-h-0 flex-1 flex-col gap-6'>
      <div className='space-y-2'>
        <Skeleton className='h-4 w-40' />
        <Skeleton className='h-10 w-72' />
        <Skeleton className='h-5 w-[520px]' />
      </div>

      <div className='flex gap-3'>
        <Skeleton className='h-10 w-56 rounded-xl' />
        <Skeleton className='h-10 w-52 rounded-xl' />
        <Skeleton className='h-10 flex-1 rounded-xl' />
      </div>

      <Skeleton className='h-[320px] rounded-2xl' />
      <div className='grid gap-6 xl:grid-cols-[1.1fr_0.9fr]'>
        <Skeleton className='h-[300px] rounded-2xl' />
        <Skeleton className='h-[300px] rounded-2xl' />
      </div>
    </section>
  )
}

function getResourceFormatLabel(resource: ConfigResource) {
  if (resource.type === 'ENTRIES') {
    return 'kv'
  }

  return configFormatOptions.find((option) => option.value === resource.format)?.label ?? resource.format
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
