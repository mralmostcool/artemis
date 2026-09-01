<script lang="ts">
  import { AlertTriangle, Trash2, X, Loader2 } from '@lucide/svelte';
  import type { Snippet } from 'svelte';

  let {
    open = $bindable(false),
    title = 'Are you sure?',
    message,
    confirmLabel = 'Confirm',
    cancelLabel = 'Cancel',
    variant = 'danger',
    loading = false,
    onConfirm,
    onCancel,
    children
  }: {
    open: boolean;
    title?: string;
    message?: string;
    confirmLabel?: string;
    cancelLabel?: string;
    variant?: 'danger' | 'warning' | 'primary';
    loading?: boolean;
    onConfirm: () => void | Promise<void>;
    onCancel?: () => void;
    children?: Snippet;
  } = $props();

  const variantMap = {
    danger: {
      icon: Trash2,
      iconBg: 'bg-destructive/10',
      iconColor: 'text-destructive',
      btn: 'bg-destructive hover:bg-destructive/90 text-destructive-foreground shadow-sm shadow-destructive/30'
    },
    warning: {
      icon: AlertTriangle,
      iconBg: 'bg-amber-500/10',
      iconColor: 'text-amber-500',
      btn: 'bg-amber-500 hover:bg-amber-600 text-white shadow-sm shadow-amber-500/30'
    },
    primary: {
      icon: AlertTriangle,
      iconBg: 'bg-primary/10',
      iconColor: 'text-primary',
      btn: 'bg-primary hover:bg-primary/90 text-primary-foreground shadow-sm shadow-primary/30'
    }
  };

  const v = $derived(variantMap[variant]);

  function cancel() {
    open = false;
    onCancel?.();
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Escape') cancel();
    if (e.key === 'Enter' && !loading) onConfirm();
  }
</script>

{#if open}
  <!-- Backdrop -->
  <div
    class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-150"
    role="dialog"
    aria-modal="true"
    aria-labelledby="confirm-title"
    onkeydown={handleKeydown}
  >
    <!-- Modal -->
    <div class="bg-card border border-border rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden">
      <!-- Close -->
      <div class="flex items-center justify-end p-3 pb-0">
        <button
          onclick={cancel}
          class="size-7 flex items-center justify-center rounded-lg hover:bg-accent transition-colors text-muted-foreground"
        >
          <X class="size-4" />
        </button>
      </div>

      <div class="px-6 pb-6 pt-2 space-y-4">
        <!-- Icon + title -->
        <div class="flex flex-col items-center text-center gap-3">
          <div class={['size-14 rounded-2xl flex items-center justify-center', v.iconBg].join(' ')}>
            <v.icon class={['size-7', v.iconColor].join(' ')} />
          </div>
          <div>
            <h2 id="confirm-title" class="text-lg font-bold text-foreground">{title}</h2>
            {#if message}
              <p class="text-sm text-muted-foreground mt-1.5">{message}</p>
            {/if}
          </div>
        </div>

        <!-- Optional custom content -->
        {#if children}
          <div class="text-sm text-muted-foreground">
            {@render children()}
          </div>
        {/if}

        <!-- Actions -->
        <div class="flex gap-3 pt-1">
          <button
            onclick={cancel}
            disabled={loading}
            class="flex-1 py-2.5 rounded-xl border border-border bg-secondary/50 text-secondary-foreground font-medium text-sm hover:bg-secondary transition-all disabled:opacity-50"
          >
            {cancelLabel}
          </button>
          <button
            onclick={onConfirm}
            disabled={loading}
            class={['flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl font-semibold text-sm transition-all active:scale-[0.98] disabled:opacity-60 disabled:cursor-not-allowed', v.btn].join(' ')}
          >
            {#if loading}
              <Loader2 class="size-4 animate-spin" />
            {/if}
            {loading ? 'Please wait…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  </div>
{/if}
