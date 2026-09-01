<script lang="ts" generics="T extends Record<string, unknown>">
  import { Search, ChevronDown, X, Check, Loader2 } from '@lucide/svelte';

  interface Option {
    value: string;
    label: string;
    sub?: string;
  }

  let {
    options,
    value = $bindable<string | null>(null),
    placeholder = 'Select…',
    searchPlaceholder = 'Search…',
    loading = false,
    disabled = false,
    clearable = false,
    emptyMessage = 'No options found',
    onchange
  }: {
    options: Option[];
    value: string | null;
    placeholder?: string;
    searchPlaceholder?: string;
    loading?: boolean;
    disabled?: boolean;
    clearable?: boolean;
    emptyMessage?: string;
    onchange?: (val: string | null) => void;
  } = $props();

  let open = $state(false);
  let query = $state('');
  let triggerEl = $state<HTMLButtonElement | null>(null);

  const filtered = $derived(
    !query
      ? options
      : options.filter((o) =>
          o.label.toLowerCase().includes(query.toLowerCase()) ||
          o.sub?.toLowerCase().includes(query.toLowerCase())
        )
  );

  const selected = $derived(options.find((o) => o.value === value));

  function select(opt: Option) {
    value = opt.value;
    open = false;
    query = '';
    onchange?.(opt.value);
  }

  function clear(e: MouseEvent) {
    e.stopPropagation();
    value = null;
    onchange?.(null);
  }

  function toggle() {
    if (disabled || loading) return;
    open = !open;
    if (!open) query = '';
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Escape') { open = false; query = ''; }
  }

  // Close on outside click
  function handleOutside(e: MouseEvent) {
    if (!triggerEl?.closest('[data-searchable-select]')?.contains(e.target as Node)) {
      open = false; query = '';
    }
  }

  $effect(() => {
    if (open) document.addEventListener('click', handleOutside);
    else document.removeEventListener('click', handleOutside);
    return () => document.removeEventListener('click', handleOutside);
  });
</script>

<div class="relative" data-searchable-select>
  <!-- Trigger -->
  <button
    bind:this={triggerEl}
    type="button"
    onclick={toggle}
    onkeydown={handleKeydown}
    {disabled}
    class={['w-full flex items-center gap-2 px-3 py-2.5 rounded-lg border bg-background text-sm text-left transition-all outline-none focus:ring-2 focus:ring-ring', open ? 'border-ring ring-2 ring-ring' : 'border-input', disabled ? 'opacity-50 cursor-not-allowed' : 'hover:border-ring/50 cursor-pointer'].join(' ')}
  >
    {#if loading}
      <Loader2 class="size-4 shrink-0 text-muted-foreground animate-spin" />
    {:else}
      <Search class="size-4 shrink-0 text-muted-foreground" />
    {/if}
    <span class={['flex-1 truncate', !selected ? 'text-muted-foreground' : 'text-foreground font-medium'].join(' ')}>
      {selected?.label ?? placeholder}
    </span>
    {#if clearable && value}
      <button
        type="button"
        onclick={clear}
        class="shrink-0 size-4 rounded-full hover:bg-accent flex items-center justify-center text-muted-foreground hover:text-foreground transition-colors"
      >
        <X class="size-3" />
      </button>
    {/if}
    <ChevronDown class={['size-4 shrink-0 text-muted-foreground transition-transform duration-150', open ? 'rotate-180' : ''].join(' ')} />
  </button>

  <!-- Dropdown -->
  {#if open}
    <div class="absolute top-full left-0 right-0 z-50 mt-1 rounded-xl border border-border bg-card shadow-xl shadow-black/10 overflow-hidden">
      <!-- Search input -->
      <div class="p-2 border-b border-border">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 size-3.5 text-muted-foreground pointer-events-none" />
          <input
            type="text"
            bind:value={query}
            placeholder={searchPlaceholder}
            autofocus
            onkeydown={(e) => e.key === 'Escape' && (open = false)}
            class="w-full pl-8 pr-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all"
          />
        </div>
      </div>

      <!-- Options list -->
      <ul class="max-h-56 overflow-y-auto py-1" role="listbox">
        {#if filtered.length === 0}
          <li class="px-4 py-6 text-center text-sm text-muted-foreground">{emptyMessage}</li>
        {:else}
          {#each filtered as opt}
            <li>
              <button
                type="button"
                role="option"
                aria-selected={value === opt.value}
                onclick={() => select(opt)}
                class={['w-full flex items-center gap-3 px-3 py-2.5 text-sm text-left hover:bg-accent/50 transition-colors', value === opt.value ? 'bg-primary/5' : ''].join(' ')}
              >
                <div class={['size-4 rounded-full border-2 shrink-0 flex items-center justify-center transition-colors', value === opt.value ? 'border-primary bg-primary' : 'border-border'].join(' ')}>
                  {#if value === opt.value}
                    <Check class="size-2.5 text-primary-foreground" />
                  {/if}
                </div>
                <div class="flex-1 min-w-0">
                  <p class={['truncate font-medium', value === opt.value ? 'text-primary' : 'text-foreground'].join(' ')}>{opt.label}</p>
                  {#if opt.sub}<p class="text-xs text-muted-foreground truncate">{opt.sub}</p>{/if}
                </div>
              </button>
            </li>
          {/each}
        {/if}
      </ul>
    </div>
  {/if}
</div>
