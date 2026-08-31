ALTER TABLE public.profiles
ADD COLUMN display_name VARCHAR(255) NOT NULL DEFAULT '',
ADD COLUMN phone_number VARCHAR(50);
