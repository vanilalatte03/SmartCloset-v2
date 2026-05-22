import type { ErrorResponse } from '../types/api';

type ApiErrorMessageProps = {
  error: ErrorResponse;
  className?: string;
};

export function ApiErrorMessage({
  error,
  className = 'panel-error',
}: ApiErrorMessageProps) {
  return (
    <div className={className} role="status">
      <div className="error-message-main">
        <strong>{error.code}</strong>
        <span>{error.message}</span>
      </div>
      {error.details.length > 0 ? (
        <ul className="error-details">
          {error.details.map((detail) => (
            <li key={`${detail.field}:${detail.message}`}>
              {detail.field}: {detail.message}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
