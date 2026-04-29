import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useParams } from '@tanstack/react-router'
import {
  AppWindow,
  FileCode2,
  FileJson2,
  GitBranchPlus,
  LoaderCircle,
  Plus,
  RefreshCcw,
  Settings2,
  ShieldCheck,
  SquarePen,
  TriangleAlert,
  Trash2,
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
  createConfigResource,
  listConfigResources,
  type ConfigFormat,
  type ConfigResource,
  type ConfigType,
} from '@/lib/config-resources'
import {
  deleteConfigEntry,
  listConfigEntries,
  saveConfigEntry,
  type ConfigEntry,
} from '@/lib/config-entries'
import { listEnvironments, type Environment } from '@/lib/environments'

type LoadState = 'loading' | 'ready' | 'error'

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
        setResources(nextResources)
        setEnvironments(nextEnvironments)
        setPolicies(Object.fromEntries(policyEntries))
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
      <div className='flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between'>
        <div className='space-y-3'>
          <div className='flex items-center gap-2 text-sm text-muted-foreground'>
            <Link to='/applications' className='hover:text-foreground'>
              Applications
            </Link>
            <span>/</span>
            <span className='text-foreground'>{application.name}</span>
          </div>

          <div className='flex items-start gap-4'>
            <div className='flex size-12 items-center justify-center rounded-xl border bg-muted'>
              <AppWindow className='size-6' />
            </div>
            <div className='space-y-2'>
              <div className='flex flex-wrap items-center gap-2'>
                <h1 className='text-2xl font-bold tracking-tight'>{application.name}</h1>
                <Badge variant='secondary'>{application.appId}</Badge>
              </div>
              <p className='max-w-3xl text-sm text-muted-foreground'>
                {application.description?.trim() || 'No description provided yet.'}
              </p>
            </div>
          </div>
        </div>

        {canManage ? (
          <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
            <DialogTrigger asChild>
              <Button type='button'>
                <GitBranchPlus className='size-4' />
                New Config Resource
              </Button>
            </DialogTrigger>
            <DialogContent className='sm:max-w-lg'>
              <DialogHeader>
                <DialogTitle>Create config resource</DialogTitle>
                <DialogDescription>
                  Add a new resource inside this application. Text resources will open in a Monaco editor.
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

      <div className='grid gap-6 xl:grid-cols-[2fr_1fr]'>
        <div className='space-y-6'>
          <Card>
            <CardHeader>
              <CardTitle>Config Resources</CardTitle>
              <CardDescription>
                Manage the resources that belong to this application. Text resources open in a dedicated editor.
              </CardDescription>
            </CardHeader>
            <CardContent>
              {resources.length === 0 ? (
                <Empty className='min-h-[260px] border'>
                  <EmptyHeader>
                    <EmptyMedia variant='icon'>
                      <FileCode2 />
                    </EmptyMedia>
                    <EmptyTitle>No config resources yet</EmptyTitle>
                    <EmptyDescription>
                      Create the first resource to start editing drafts and publishing releases.
                    </EmptyDescription>
                  </EmptyHeader>
                </Empty>
              ) : (
                <div className='grid gap-4 lg:grid-cols-2'>
                  {resources.map((resource) => (
                    <Card key={resource.id} className='h-full border-dashed'>
                      <CardHeader className='gap-3'>
                        <div className='flex items-start justify-between gap-3'>
                          <div className='space-y-1'>
                            <CardTitle className='text-base'>{resource.name}</CardTitle>
                            <CardDescription>
                              {resource.description?.trim() || 'No description provided.'}
                            </CardDescription>
                          </div>
                          <Badge variant='outline'>#{resource.id}</Badge>
                        </div>
                        <div className='flex flex-wrap gap-2'>
                          <Badge variant='secondary'>{resource.type}</Badge>
                          <Badge variant='outline'>{resource.format}</Badge>
                        </div>
                      </CardHeader>
                      <CardContent className='flex flex-col gap-4'>
                        <div className='grid gap-2 text-sm text-muted-foreground'>
                          <div className='flex items-center gap-2'>
                            {resource.type === 'CONTENT' ? (
                              <FileCode2 className='size-4' />
                            ) : (
                              <FileJson2 className='size-4' />
                            )}
                            <span>Updated {formatDateTime(resource.updatedAt)}</span>
                          </div>
                        </div>

                        {resource.type === 'CONTENT' ? (
                          <Button type='button' variant='outline' asChild>
                              <Link
                              to='/applications/$applicationId/resources/$resourceId/content'
                              params={{
                                applicationId: String(application.id),
                                resourceId: String(resource.id),
                              }}
                            >
                              <SquarePen className='size-4' />
                              Open Editor
                            </Link>
                          </Button>
                        ) : (
                          <EntriesResourceEditor
                            resource={resource}
                            environments={environments}
                            canManage={canManage}
                          />
                        )}
                      </CardContent>
                    </Card>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

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
        </div>

        <div className='space-y-6'>
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
      </div>
    </section>
  )
}

type EntriesResourceEditorProps = {
  resource: ConfigResource
  environments: Environment[]
  canManage: boolean
}

function EntriesResourceEditor({
  resource,
  environments,
  canManage,
}: EntriesResourceEditorProps) {
  const [selectedEnvironmentId, setSelectedEnvironmentId] = useState<string>('')
  const [entries, setEntries] = useState<ConfigEntry[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [deletingEntryId, setDeletingEntryId] = useState<number | null>(null)

  useEffect(() => {
    if (!selectedEnvironmentId && environments.length > 0) {
      setSelectedEnvironmentId(String(environments[0].id))
    }
  }, [environments, selectedEnvironmentId])

  useEffect(() => {
    if (!selectedEnvironmentId) {
      return
    }

    const abortController = new AbortController()

    async function loadEntries() {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const nextEntries = await listConfigEntries(
          resource.id,
          Number(selectedEnvironmentId),
          abortController.signal,
        )

        if (abortController.signal.aborted) {
          return
        }

        setEntries(nextEntries)
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
  }, [resource.id, selectedEnvironmentId])

  const handleCreateEntry = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedEnvironmentId) {
      return
    }

    setIsSubmitting(true)
    setErrorMessage('')

    const formData = new FormData(event.currentTarget)

    try {
      const savedEntry = await saveConfigEntry(resource.id, Number(selectedEnvironmentId), {
        key: String(formData.get('key') ?? ''),
        value: String(formData.get('value') ?? ''),
        valueType: String(formData.get('valueType') ?? ''),
        description: String(formData.get('description') ?? ''),
      })

      setEntries((current) =>
        current
          .filter((entry) => entry.id !== savedEntry.id)
          .concat(savedEntry)
          .toSorted((left, right) => left.key.localeCompare(right.key)),
      )
      event.currentTarget.reset()
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to save entry.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDeleteEntry = async (entryId: number) => {
    if (!selectedEnvironmentId) {
      return
    }

    setDeletingEntryId(entryId)
    setErrorMessage('')

    try {
      await deleteConfigEntry(resource.id, Number(selectedEnvironmentId), entryId)
      setEntries((current) => current.filter((entry) => entry.id !== entryId))
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Failed to delete entry.',
      )
    } finally {
      setDeletingEntryId(null)
    }
  }

  return (
    <div className='space-y-4 rounded-xl border bg-muted/20 p-4'>
      <div className='flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between'>
        <div className='flex min-w-56 flex-col gap-2'>
          <Label htmlFor={`entries-environment-${resource.id}`}>Environment</Label>
          <Select value={selectedEnvironmentId} onValueChange={setSelectedEnvironmentId}>
            <SelectTrigger id={`entries-environment-${resource.id}`}>
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
        <p className='text-sm text-muted-foreground'>
          Entries resources are edited inline inside the application page.
        </p>
      </div>

      {canManage ? (
        <form className='grid gap-3 rounded-xl border bg-background p-4' onSubmit={handleCreateEntry}>
          <div className='grid gap-3 md:grid-cols-2'>
            <div className='flex flex-col gap-2'>
              <Label htmlFor={`entry-key-${resource.id}`}>Key</Label>
              <Input id={`entry-key-${resource.id}`} name='key' placeholder='db.url' required />
            </div>
            <div className='flex flex-col gap-2'>
              <Label htmlFor={`entry-value-type-${resource.id}`}>Value Type</Label>
              <Input
                id={`entry-value-type-${resource.id}`}
                name='valueType'
                placeholder='string'
              />
            </div>
          </div>

          <div className='flex flex-col gap-2'>
            <Label htmlFor={`entry-value-${resource.id}`}>Value</Label>
            <Input
              id={`entry-value-${resource.id}`}
              name='value'
              placeholder='jdbc:postgresql://localhost:5432/chert'
              required
            />
          </div>

          <div className='flex flex-col gap-2'>
            <Label htmlFor={`entry-description-${resource.id}`}>Description</Label>
            <Input
              id={`entry-description-${resource.id}`}
              name='description'
              placeholder='Primary database connection string'
            />
          </div>

          <div className='flex items-center justify-between gap-3'>
            {errorMessage ? <p className='text-sm text-destructive'>{errorMessage}</p> : <span />}
            <Button
              type='submit'
              disabled={!selectedEnvironmentId || isSubmitting}
            >
              {isSubmitting ? <LoaderCircle className='size-4 animate-spin' /> : <Plus className='size-4' />}
              Save Entry
            </Button>
          </div>
        </form>
      ) : null}

      {isLoading ? (
        <div className='rounded-lg border bg-background px-4 py-6 text-sm text-muted-foreground'>
          Loading entries…
        </div>
      ) : entries.length === 0 ? (
        <div className='rounded-lg border bg-background px-4 py-6 text-sm text-muted-foreground'>
          No entries in this environment yet.
        </div>
      ) : (
        <div className='overflow-hidden rounded-xl border bg-background'>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Key</TableHead>
                <TableHead>Value</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Description</TableHead>
                <TableHead className='w-24 text-right'>Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {entries.map((entry) => (
                <TableRow key={entry.id}>
                  <TableCell className='font-mono text-xs'>{entry.key}</TableCell>
                  <TableCell className='max-w-[280px] truncate'>{entry.value}</TableCell>
                  <TableCell>{entry.valueType || '-'}</TableCell>
                  <TableCell className='text-muted-foreground'>
                    {entry.description || '-'}
                  </TableCell>
                  <TableCell className='text-right'>
                    <Button
                      type='button'
                      variant='ghost'
                      size='icon'
                      disabled={!canManage || deletingEntryId === entry.id}
                      onClick={() => void handleDeleteEntry(entry.id)}
                    >
                      {deletingEntryId === entry.id ? (
                        <LoaderCircle className='size-4 animate-spin' />
                      ) : (
                        <Trash2 className='size-4' />
                      )}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
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
      <div className='space-y-3'>
        <Skeleton className='h-4 w-32' />
        <div className='flex items-start gap-4'>
          <Skeleton className='size-12 rounded-xl' />
          <div className='space-y-2'>
            <Skeleton className='h-8 w-56' />
            <Skeleton className='h-4 w-80' />
          </div>
        </div>
      </div>

      <div className='grid gap-6 xl:grid-cols-[2fr_1fr]'>
        <Skeleton className='h-[520px] rounded-xl' />
        <Skeleton className='h-[320px] rounded-xl' />
      </div>
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
