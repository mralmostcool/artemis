<script lang="ts">
  import type { CertificateStatus, EnrollmentStatus } from '$lib/types';

  type StatusVariant =
    | 'active' | 'inactive'
    | CertificateStatus
    | EnrollmentStatus
    | 'DRAFT' | 'SIGNED_ON' | 'SIGNED_OFF' | 'EXTENDED'
    | 'PENDING' | 'PAID'
    | string;

  let { status, label }: { status: StatusVariant; label?: string } = $props();

  const config: Record<string, { cls: string; text: string }> = {
    // General
    active:         { cls: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400', text: 'Active' },
    inactive:       { cls: 'bg-zinc-100 text-zinc-500 dark:bg-zinc-800 dark:text-zinc-400', text: 'Inactive' },
    // Certificate
    INITIATED:      { cls: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400', text: 'Initiated' },
    L1_REVIEWED:    { cls: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400', text: 'L1 Reviewed' },
    L2_APPROVED:    { cls: 'bg-violet-100 text-violet-700 dark:bg-violet-900/30 dark:text-violet-400', text: 'L2 Approved' },
    ALLOTTED:       { cls: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400', text: 'Allotted' },
    // Contract
    DRAFT:          { cls: 'bg-zinc-100 text-zinc-600 dark:bg-zinc-800 dark:text-zinc-300', text: 'Draft' },
    SIGNED_ON:      { cls: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400', text: 'Signed On' },
    SIGNED_OFF:     { cls: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400', text: 'Signed Off' },
    EXTENDED:       { cls: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400', text: 'Extended' },
    // Enrollment
    ENROLLED:       { cls: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400', text: 'Enrolled' },
    IN_PROGRESS:    { cls: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400', text: 'In Progress' },
    COMPLETED:      { cls: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400', text: 'Completed' },
    DROPPED:        { cls: 'bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400', text: 'Dropped' },
    // Payroll
    PENDING:        { cls: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400', text: 'Pending' },
    PAID:           { cls: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400', text: 'Paid' }
  };

  const resolved = $derived(config[status] ?? { cls: 'bg-zinc-100 text-zinc-500 dark:bg-zinc-800 dark:text-zinc-400', text: status });
</script>

<span class={['inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold', resolved.cls].join(' ')}>
  {label ?? resolved.text}
</span>
