import { useEffect, useState, type FormEvent } from 'react'
import { LoaderCircle, Plus, RefreshCcw, Shield, TriangleAlert, Users } from 'lucide-react'
import { useAuth } from '@/providers/auth-provider'
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
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { createUser, listUsers } from '@/lib/users'
import type { ConsoleUser } from '@/lib/auth'

type LoadState = 'loading' | 'ready' | 'error'
type RoleOption = 'USER' | 'SUPER_ADMIN'

export function UsersPage() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState<ConsoleUser[]>([])
  const [errorMessage, setErrorMessage] = useState('')
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [loadState, setLoadState] = useState<LoadState>('loading')
  const [requestVersion, setRequestVersion] = useState(0)
  const [selectedRole, setSelectedRole] = useState<RoleOption>('USER')

  const canManageUsers = currentUser?.permissions.includes('user:manage') ?? false

  useEffect(() => {
    const abortController = new AbortController()

    async function load() {
      setLoadState('loading')
      setErrorMessage('')

      try {
        const nextUsers = await listUsers(abortController.signal)
        setUsers(nextUsers)
        setLoadState('ready')
      } catch (error) {
        if (abortController.signal.aborted) {
          return
        }

        setErrorMessage(error instanceof Error ? error.message : 'Failed to load users.')
        setLoadState('error')
      }
    }

    if (!canManageUsers) {
      setLoadState('ready')
      return
    }

    void load()
    return () => abortController.abort()
  }, [canManageUsers, requestVersion])

  const retryLoad = () => {
    setRequestVersion((currentVersion) => currentVersion + 1)
  }

  const handleCreateUser = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setErrorMessage('')
    setIsSubmitting(true)

    const formData = new FormData(event.currentTarget)

    try {
      const nextUser = await createUser({
        username: String(formData.get('username') ?? ''),
        email: String(formData.get('email') ?? ''),
        password: String(formData.get('password') ?? ''),
        roles: selectedRole === 'SUPER_ADMIN' ? ['USER', 'SUPER_ADMIN'] : ['USER'],
      })
      setUsers((current) => current.concat(nextUser).toSorted((a, b) => a.username.localeCompare(b.username)))
      setIsCreateOpen(false)
      setSelectedRole('USER')
      event.currentTarget.reset()
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to create user.')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!canManageUsers) {
    return (
      <section className='flex min-h-0 flex-1 flex-col'>
        <Empty className='min-h-full border'>
          <EmptyHeader>
            <EmptyMedia variant='icon'>
              <Shield />
            </EmptyMedia>
            <EmptyTitle>User management is restricted</EmptyTitle>
            <EmptyDescription>
              Your account can use the console, but only users with the `user:manage`
              permission can manage accounts.
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      </section>
    )
  }

  return (
    <section className='flex min-h-0 flex-1 flex-col gap-6'>
      <div className='flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between'>
        <div>
          <h1 className='text-2xl font-bold tracking-tight'>Users</h1>
          <p className='text-muted-foreground'>
            Manage console accounts, roles, and platform-level access.
          </p>
        </div>

        <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
          <DialogTrigger asChild>
            <Button type='button'>
              <Plus className='size-4' />
              New User
            </Button>
          </DialogTrigger>
          <DialogContent className='sm:max-w-lg'>
            <DialogHeader>
              <DialogTitle>Create user</DialogTitle>
              <DialogDescription>
                Add a console user and assign the initial platform role.
              </DialogDescription>
            </DialogHeader>

            <form className='flex flex-col gap-4' onSubmit={handleCreateUser}>
              <div className='flex flex-col gap-2'>
                <Label htmlFor='user-username'>Username</Label>
                <Input id='user-username' name='username' required />
              </div>

              <div className='flex flex-col gap-2'>
                <Label htmlFor='user-email'>Email</Label>
                <Input id='user-email' name='email' type='email' required />
              </div>

              <div className='flex flex-col gap-2'>
                <Label htmlFor='user-password'>Password</Label>
                <Input id='user-password' name='password' type='password' required />
              </div>

              <div className='flex flex-col gap-2'>
                <Label htmlFor='user-role'>Role</Label>
                <Select value={selectedRole} onValueChange={(value) => setSelectedRole(value as RoleOption)}>
                  <SelectTrigger id='user-role'>{selectedRole}</SelectTrigger>
                  <SelectContent>
                    <SelectItem value='USER'>USER</SelectItem>
                    <SelectItem value='SUPER_ADMIN'>SUPER_ADMIN</SelectItem>
                  </SelectContent>
                </Select>
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
          <CardTitle>Console users</CardTitle>
          <CardDescription>
            Platform-level roles remain simple: `USER` for regular accounts and `SUPER_ADMIN`
            for global administration.
          </CardDescription>
        </CardHeader>
        <CardContent className='min-h-0 flex-1'>
          {loadState === 'error' ? (
            <Empty className='min-h-[360px] border'>
              <EmptyHeader>
                <EmptyMedia variant='icon'>
                  <TriangleAlert />
                </EmptyMedia>
                <EmptyTitle>Unable to load users</EmptyTitle>
                <EmptyDescription>{errorMessage}</EmptyDescription>
              </EmptyHeader>
              <Button type='button' variant='outline' onClick={retryLoad}>
                <RefreshCcw className='size-4' />
                Retry
              </Button>
            </Empty>
          ) : null}

          {loadState === 'ready' && users.length === 0 ? (
            <Empty className='min-h-[360px] border'>
              <EmptyHeader>
                <EmptyMedia variant='icon'>
                  <Users />
                </EmptyMedia>
                <EmptyTitle>No users yet</EmptyTitle>
                <EmptyDescription>
                  Create the next console user to expand collaboration.
                </EmptyDescription>
              </EmptyHeader>
            </Empty>
          ) : null}

          {loadState === 'loading' ? (
            <div className='flex min-h-[360px] items-center justify-center text-sm text-muted-foreground'>
              Loading users…
            </div>
          ) : null}

          {loadState === 'ready' && users.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>User</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Roles</TableHead>
                  <TableHead>Permissions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell className='font-medium'>{user.username}</TableCell>
                    <TableCell className='text-muted-foreground'>{user.email}</TableCell>
                    <TableCell>{user.roles.join(', ')}</TableCell>
                    <TableCell className='text-muted-foreground'>
                      {user.permissions.join(', ')}
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
