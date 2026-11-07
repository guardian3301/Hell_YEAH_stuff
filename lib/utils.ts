import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
// ponytail: 1 util, 1 caller (button.tsx) — cva уже удалён (см. button), clsx+twMerge оставлены т.к. bg-* конфликты без twMerge ломают варианты.
