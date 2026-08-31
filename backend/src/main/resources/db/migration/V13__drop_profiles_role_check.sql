-- Drop obsolete profiles role check constraint to support expanded role hierarchy
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_role_check;
