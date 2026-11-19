const HR_TIME_ZONE = 'Indian/Antananarivo';

const HR_DATE_FORMATTER = new Intl.DateTimeFormat('en-CA', {
  timeZone: HR_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit'
});

export function hrToday(): string {
  return HR_DATE_FORMATTER.format(new Date());
}
