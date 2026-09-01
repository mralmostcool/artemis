<script lang="ts" generics="T extends Record<string, unknown>">
  import { ArrowUpDown, ArrowUp, ArrowDown, ChevronLeft, ChevronRight } from '@lucide/svelte';
  import type { Snippet } from 'svelte';

  interface Column<T> {
    key: keyof T | string;
    label: string;
    sortable?: boolean;
    class?: string;
    cell?: Snippet<[T]>;
  }

  let {
    data,
    columns,
    rowKey = 'id',
    pageSize = 20,
    loading = false,
    emptyMessage = 'No records found',
    emptyIcon,
    onRowClick
  }: {
    data: T[];
    columns: Column<T>[];
    rowKey?: keyof T | string;
    pageSize?: number;
    loading?: boolean;
    emptyMessage?: string;
    emptyIcon?: Snippet;
    onRowClick?: (row: T) => void;
  } = $props();

  let sortKey = $state<string | null>(null);
  let sortDir = $state<'asc' | 'desc'>('asc');
  let currentPage = $state(1);

  // Sort
  const sorted = $derived.by(() => {
    if (!sortKey) return data;
    return [...data].sort((a, b) => {
      const av = (a as Record<string, unknown>)[sortKey!];
      const bv = (b as Record<string, unknown>)[sortKey!];
      const cmp = av == null ? -1 : bv == null ? 1 : av < bv ? -1 : av > bv ? 1 : 0;
      return sortDir === 'asc' ? cmp : -cmp;
    });
  });

  // Paginate
  const totalPages = $derived(Math.max(1, Math.ceil(sorted.length / pageSize)));
  const paginated = $derived(sorted.slice((currentPage - 1) * pageSize, currentPage * pageSize));

  function toggleSort(key: string) {
    if (sortKey === key) {
      sortDir = sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      sortKey = key;
      sortDir = 'asc';
    }
    currentPage = 1;
  }

  function getVal(row: T, key: string): unknown {
    return key.split('.').reduce((o: unknown, k) => (o as Record<string, unknown>)?.[k], row);
  }

  // Reset page when data changes
  $effect(() => { data; currentPage = 1; });
</script>

<div class="rounded-xl border border-border bg-card overflow-hidden">
  <div class="overflow-x-auto">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b border-border bg-muted/40">
          {#each columns as col}
            <th
              class={['text-left px-4 py-3 font-semibold text-muted-foreground text-xs uppercase tracking-wide whitespace-nowrap', col.class ?? ''].join(' ')}
            >
              {#if col.sortable}
                <button
                  onclick={() => toggleSort(col.key as string)}
                  class="flex items-center gap-1.5 hover:text-foreground transition-colors group"
                >
                  {col.label}
                  {#if sortKey === col.key}
                    {#if sortDir === 'asc'}
                      <ArrowUp class="size-3 text-primary" />
                    {:else}
                      <ArrowDown class="size-3 text-primary" />
                    {/if}
                  {:else}
                    <ArrowUpDown class="size-3 opacity-30 group-hover:opacity-60" />
                  {/if}
                </button>
              {:else}
                {col.label}
              {/if}
            </th>
          {/each}
        </tr>
      </thead>
      <tbody>
        {#if loading}
          {#each Array(pageSize > 8 ? 8 : pageSize) as _}
            <tr class="border-b border-border last:border-0">
              {#each columns as _c}
                <td class="px-4 py-3">
                  <div class="h-4 rounded bg-muted animate-pulse"></div>
                </td>
              {/each}
            </tr>
          {/each}
        {:else if paginated.length === 0}
          <tr>
            <td colspan={columns.length} class="px-4 py-14 text-center">
              {#if emptyIcon}
                {@render emptyIcon()}
              {/if}
              <p class="text-muted-foreground text-sm mt-2">{emptyMessage}</p>
            </td>
          </tr>
        {:else}
          {#each paginated as row (getVal(row, rowKey as string))}
            <tr
              class={['border-b border-border last:border-0 transition-colors', onRowClick ? 'cursor-pointer hover:bg-accent/30' : 'hover:bg-accent/10'].join(' ')}
              onclick={() => onRowClick?.(row)}
              onkeydown={(e) => e.key === 'Enter' && onRowClick?.(row)}
              role={onRowClick ? 'button' : undefined}
              tabindex={onRowClick ? 0 : undefined}
            >
              {#each columns as col}
                <td class={['px-4 py-3', col.class ?? ''].join(' ')}>
                  {#if col.cell}
                    {@render col.cell(row)}
                  {:else}
                    <span class="text-foreground">{getVal(row, col.key as string) ?? '—'}</span>
                  {/if}
                </td>
              {/each}
            </tr>
          {/each}
        {/if}
      </tbody>
    </table>
  </div>

  <!-- Pagination -->
  {#if !loading && sorted.length > pageSize}
    <div class="flex items-center justify-between px-4 py-3 border-t border-border bg-muted/20">
      <p class="text-xs text-muted-foreground">
        Showing {(currentPage - 1) * pageSize + 1}–{Math.min(currentPage * pageSize, sorted.length)} of {sorted.length}
      </p>
      <div class="flex items-center gap-1">
        <button
          onclick={() => (currentPage -= 1)}
          disabled={currentPage === 1}
          class="size-7 flex items-center justify-center rounded-md hover:bg-accent transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <ChevronLeft class="size-4" />
        </button>
        {#each Array(Math.min(7, totalPages)) as _, i}
          {@const p = totalPages <= 7 ? i + 1 : currentPage <= 4 ? i + 1 : currentPage >= totalPages - 3 ? totalPages - 6 + i : currentPage - 3 + i}
          <button
            onclick={() => (currentPage = p)}
            class={['size-7 flex items-center justify-center rounded-md text-xs font-medium transition-colors', currentPage === p ? 'bg-primary text-primary-foreground' : 'hover:bg-accent text-muted-foreground'].join(' ')}
          >{p}</button>
        {/each}
        <button
          onclick={() => (currentPage += 1)}
          disabled={currentPage === totalPages}
          class="size-7 flex items-center justify-center rounded-md hover:bg-accent transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <ChevronRight class="size-4" />
        </button>
      </div>
    </div>
  {/if}
</div>
