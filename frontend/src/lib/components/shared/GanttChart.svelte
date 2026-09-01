<script lang="ts">
  interface Bar {
    id: string;
    label: string;
    sublabel?: string;
    start: string | Date;
    end: string | Date;
    color?: string; // tailwind bg class e.g. 'bg-primary'
    tooltip?: string;
  }

  let {
    bars,
    windowDays = 60,
    startOffset = 10,
    rowHeight = 48,
    title = 'Timeline'
  }: {
    bars: Bar[];
    windowDays?: number;
    startOffset?: number;
    rowHeight?: number;
    title?: string;
  } = $props();

  const windowStart = $derived.by(() => {
    const d = new Date();
    d.setDate(d.getDate() - startOffset);
    return d;
  });

  const windowEnd = $derived.by(() => {
    const d = new Date(windowStart);
    d.setDate(d.getDate() + windowDays);
    return d;
  });

  const totalMs = $derived(windowEnd.getTime() - windowStart.getTime());

  function pct(date: string | Date): number {
    const ms = new Date(date).getTime() - windowStart.getTime();
    return Math.max(0, Math.min(100, (ms / totalMs) * 100));
  }

  function widthPct(start: string | Date, end: string | Date): number {
    return Math.max(0.5, pct(end) - pct(start));
  }

  const todayPct = $derived(pct(new Date()));

  // Generate date markers (5 evenly spaced)
  const markers = $derived.by(() => {
    return Array.from({ length: 6 }, (_, i) => {
      const d = new Date(windowStart);
      d.setDate(d.getDate() + Math.round((windowDays / 5) * i));
      return { label: d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }), pct: (i / 5) * 100 };
    });
  });
</script>

<div class="rounded-xl border border-border bg-card overflow-hidden">
  <!-- Date ruler -->
  <div class="relative h-9 border-b border-border bg-muted/30 overflow-hidden select-none">
    {#each markers as m}
      <span
        class="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 text-[10px] text-muted-foreground whitespace-nowrap"
        style="left:{m.pct}%"
      >{m.label}</span>
    {/each}
  </div>

  <!-- Rows -->
  {#if bars.length === 0}
    <div class="flex items-center justify-center py-12 text-muted-foreground text-sm">No data in this window</div>
  {:else}
    <div class="divide-y divide-border overflow-x-hidden">
      {#each bars as bar}
        <div class="flex" style="height:{rowHeight}px">
          <!-- Label -->
          <div class="w-40 shrink-0 border-r border-border flex flex-col justify-center px-4 py-2 bg-muted/10">
            <p class="text-xs font-semibold text-foreground truncate leading-tight">{bar.label}</p>
            {#if bar.sublabel}<p class="text-[10px] text-muted-foreground truncate">{bar.sublabel}</p>{/if}
          </div>

          <!-- Track -->
          <div class="relative flex-1 overflow-hidden">
            <!-- Today line -->
            <div
              class="absolute top-0 bottom-0 w-px bg-primary/50 z-10 pointer-events-none"
              style="left:{todayPct}%"
            ></div>

            <!-- Bar -->
            <div
              class={[
                'absolute top-3 bottom-3 rounded-md flex items-center px-2.5 overflow-hidden transition-opacity hover:opacity-90',
                bar.color ?? 'bg-primary/70'
              ].join(' ')}
              style="left:{pct(bar.start)}%;width:{widthPct(bar.start, bar.end)}%"
              title={bar.tooltip ?? `${bar.label}: ${new Date(bar.start).toLocaleDateString()} – ${new Date(bar.end).toLocaleDateString()}`}
            >
              <span class="text-[10px] font-semibold text-white/90 truncate whitespace-nowrap">
                {new Date(bar.start).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                –
                {new Date(bar.end).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
              </span>
            </div>
          </div>
        </div>
      {/each}
    </div>
  {/if}

  <!-- Legend -->
  <div class="border-t border-border px-4 py-2 flex items-center gap-4 bg-muted/10">
    <div class="flex items-center gap-1.5">
      <div class="w-px h-3 bg-primary/50"></div>
      <span class="text-[10px] text-muted-foreground">Today</span>
    </div>
    <span class="text-[10px] text-muted-foreground">{windowDays}-day window</span>
  </div>
</div>
