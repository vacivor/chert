import { Settings, User } from 'lucide-react'
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

export function UserMenu() {
  return (
    <DropdownMenu modal={false}>
      <DropdownMenuTrigger asChild>
        <Button
          variant='ghost'
          className='relative h-8 w-8 rounded-full p-0 hover:bg-accent hover:text-accent-foreground dark:hover:bg-accent/50'
          aria-label='Open profile menu'
        >
          <Avatar className='h-8 w-8'>
            <AvatarFallback>CC</AvatarFallback>
          </Avatar>
          <span className='sr-only'>Console profile</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className='w-56' align='end' forceMount>
        <DropdownMenuLabel className='font-normal'>
          <div className='flex flex-col gap-1.5'>
            <p className='text-sm leading-none font-medium'>Chert Console</p>
            <p className='text-xs leading-none text-muted-foreground'>
              Boilerplate workspace
            </p>
          </div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <DropdownMenuItem inset>
            <User className='size-4' />
            Profile
          </DropdownMenuItem>
          <DropdownMenuItem inset>
            <Settings className='size-4' />
            Settings
          </DropdownMenuItem>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
