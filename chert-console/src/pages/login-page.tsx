import { useState, type FormEvent } from 'react'
import { Navigate, useRouter } from '@tanstack/react-router'
import { LoaderCircle, LogIn } from 'lucide-react'
import { useAuth } from '@/providers/auth-provider'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export function LoginPage() {
  const router = useRouter()
  const { errorMessage: sessionErrorMessage, login, status } = useAuth()
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (status === 'authenticated') {
    return <Navigate to='/applications' replace />
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setErrorMessage('')
    setIsSubmitting(true)

    const formData = new FormData(event.currentTarget)
    const username = String(formData.get('username') ?? '')
    const password = String(formData.get('password') ?? '')

    try {
      await login({ username, password })
      await router.navigate({ to: '/applications', replace: true })
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Failed to sign in.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className='flex min-h-screen items-center justify-center px-4 py-10'>
      <Card className='w-full max-w-md'>
        <CardHeader className='gap-2'>
          <div className='flex size-10 items-center justify-center rounded-xl border bg-muted'>
            <LogIn className='size-4' />
          </div>
          <div className='space-y-1'>
            <CardTitle>Sign in to Chert Console</CardTitle>
            <CardDescription>
              Use your console account to manage applications, environments, and releases.
            </CardDescription>
          </div>
        </CardHeader>
        <CardContent>
          <form className='flex flex-col gap-4' onSubmit={handleSubmit}>
            <div className='flex flex-col gap-2'>
              <Label htmlFor='username'>Username</Label>
              <Input id='username' name='username' autoComplete='username' required />
            </div>

            <div className='flex flex-col gap-2'>
              <Label htmlFor='password'>Password</Label>
              <Input
                id='password'
                name='password'
                type='password'
                autoComplete='current-password'
                required
              />
            </div>

            {sessionErrorMessage || errorMessage ? (
              <p className='text-sm text-destructive'>
                {errorMessage || sessionErrorMessage}
              </p>
            ) : null}

            <Button type='submit' disabled={isSubmitting}>
              {isSubmitting ? <LoaderCircle className='size-4 animate-spin' /> : null}
              Sign In
            </Button>
          </form>
        </CardContent>
      </Card>
    </section>
  )
}
