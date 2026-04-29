import { LogOut, Shield, User } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useAuth } from '@/providers/auth-provider'

export function UserMenu() {
  const { logout, user } = useAuth()
  const initials = user?.username.slice(0, 2).toUpperCase() ?? 'CC'

  return (
    <DropdownMenu modal={false}>
      <DropdownMenuTrigger asChild>
        <Button
          variant='ghost'
          className='relative h-8 w-8 rounded-full p-0 hover:bg-accent hover:text-accent-foreground dark:hover:bg-accent/50'
          aria-label='Open profile menu'
        >
          <Avatar className='h-8 w-8'>
            <AvatarFallback>{initials}</AvatarFallback>
          </Avatar>
          <span className='sr-only'>Console profile</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className='w-56' align='end' forceMount>
        <DropdownMenuLabel className='font-normal'>
          <div className='flex flex-col gap-1.5'>
            <p className='text-sm leading-none font-medium'>{user?.username ?? 'Guest'}</p>
            <p className='text-xs leading-none text-muted-foreground'>{user?.email ?? ''}</p>
          </div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <DropdownMenuItem inset>
            <User className='size-4' />
            {user?.roles.join(', ') || 'No role'}
          </DropdownMenuItem>
          <DropdownMenuItem inset disabled>
            <Shield className='size-4' />
            {user?.permissions.length ?? 0} permissions
          </DropdownMenuItem>
          <DropdownMenuItem inset onClick={() => void logout()}>
            <LogOut className='size-4' />
            Sign Out
          </DropdownMenuItem>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
