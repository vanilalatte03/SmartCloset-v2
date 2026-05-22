export function ClosetPanel() {
  return (
    <article className="panel">
      <h2>Closet</h2>
      <dl className="metric-list">
        <div>
          <dt>Active items</dt>
          <dd className="muted">Not loaded</dd>
        </div>
        <div>
          <dt>Panel state</dt>
          <dd>Ready</dd>
        </div>
      </dl>
    </article>
  );
}
