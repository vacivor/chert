export function NotFoundPage() {
  return (
    <section className='flex min-h-[calc(100vh-8rem)] items-center justify-center rounded-3xl border border-dashed border-border bg-card/20 px-6 text-center'>
      <div className='flex flex-col gap-2'>
        <p className='text-sm font-medium'>Page not found</p>
        <p className='text-sm text-muted-foreground'>
          This route has not been scaffolded yet.
        </p>
      </div>
    </section>
  )
}
