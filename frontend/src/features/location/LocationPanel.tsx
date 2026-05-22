import type { UserLocationResponse } from '../../types/api';

type LocationPanelProps = {
  location: UserLocationResponse | null;
  loading: boolean;
};

export function LocationPanel({ location, loading }: LocationPanelProps) {
  return (
    <article className="panel">
      <h2>Location</h2>
      {loading ? (
        <p className="muted">Loading current user location.</p>
      ) : location ? (
        <dl className="metric-list">
          <div>
            <dt>Name</dt>
            <dd>{location.name}</dd>
          </div>
          <div>
            <dt>Code</dt>
            <dd>{location.code}</dd>
          </div>
          <div>
            <dt>KMA grid</dt>
            <dd>
              nx={location.nx}, ny={location.ny}
            </dd>
          </div>
        </dl>
      ) : (
        <p className="muted">No location data loaded.</p>
      )}
    </article>
  );
}
