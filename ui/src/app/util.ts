/** Pulls a readable message out of an HTTP error from the backend. */
export function errMsg(e: any): string {
  if (e?.status === 0) { return 'Cannot reach the server. Is the Spring Boot backend running on port 8080?'; }
  return e?.error?.error || e?.error?.message || 'Something went wrong. Please try again.';
}
