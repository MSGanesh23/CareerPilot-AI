import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { analyticsApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';
import './DashboardPage.css';

function StatCard({ label, value, sub, accent }) {
  return (
    <div className={`stat-card ${accent ? 'stat-card--accent' : ''}`}>
      <div className="stat-value">{value ?? '—'}</div>
      <div className="stat-label">{label}</div>
      {sub && <div className="stat-sub">{sub}</div>}
    </div>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    analyticsApi.getDashboard()
      .then(res => setData(res.data.data))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, []);

  const app = data?.applicationStats;
  const inv = data?.interviewStats;

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  const firstName = user?.fullName?.split(' ')[0] || 'there';

  return (
    <div className="fade-in">
      {/* Header */}
      <div className="dashboard-greeting">
        <div>
          <h1>{greeting}, {firstName}</h1>
          <p className="text-secondary">Here's your career progress at a glance.</p>
        </div>
        <div className="dashboard-cta-group">
          <Link to="/jobs/new" className="btn btn-primary">+ Add Application</Link>
          <Link to="/resumes" className="btn btn-secondary">Manage Resumes</Link>
        </div>
      </div>

      {loading ? (
        <div className="loading-center"><div className="spinner" /><span>Loading dashboard…</span></div>
      ) : (
        <>
          {/* Key Stats */}
          <div className="dashboard-stats grid-4">
            <StatCard
              label="Total Applications"
              value={app?.totalApplications ?? 0}
              accent
            />
            <StatCard
              label="Offer Rate"
              value={app ? `${app.offerRate.toFixed(1)}%` : '0%'}
              sub={`${app?.offers ?? 0} offers`}
            />
            <StatCard
              label="Interview Rate"
              value={app ? `${app.interviewRate.toFixed(1)}%` : '0%'}
              sub={`${app?.interviewing ?? 0} active`}
            />
            <StatCard
              label="Avg. AI Match"
              value={app?.averageMatchScore ? `${Math.round(app.averageMatchScore)}%` : '—'}
              sub="resume score"
            />
          </div>

          {/* Status breakdown + Interview stats */}
          <div className="grid-2 mt-6">
            <div className="card">
              <div className="card-header">
                <span className="card-title">Applications by Status</span>
                <Link to="/jobs" className="btn btn-ghost btn-sm">View all →</Link>
              </div>
              <div className="status-breakdown">
                {[
                  { label: 'Applied',      value: app?.applied,      cls: 'applied' },
                  { label: 'Interviewing', value: app?.interviewing, cls: 'interviewing' },
                  { label: 'Offer',        value: app?.offers,       cls: 'offer' },
                  { label: 'Rejected',     value: app?.rejections,   cls: 'rejected' },
                  { label: 'Withdrawn',    value: app?.withdrawn,    cls: 'withdrawn' },
                ].map(({ label, value, cls }) => (
                  <div key={label} className="status-row">
                    <div className="flex items-center gap-3">
                      <span className={`status-dot status-dot--${cls}`} />
                      <span className="text-secondary" style={{ fontSize: '0.9rem' }}>{label}</span>
                    </div>
                    <span className="status-count">{value ?? 0}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="card">
              <div className="card-header">
                <span className="card-title">Mock Interviews</span>
                <Link to="/interviews" className="btn btn-ghost btn-sm">View all →</Link>
              </div>
              <div className="interview-stats">
                <div className="interview-stat-row">
                  <span className="text-secondary">Sessions completed</span>
                  <span className="text-primary">{inv?.completedSessions ?? 0}</span>
                </div>
                <div className="interview-stat-row">
                  <span className="text-secondary">In progress</span>
                  <span className="text-primary">{inv?.inProgressSessions ?? 0}</span>
                </div>
                <div className="interview-stat-row">
                  <span className="text-secondary">Questions answered</span>
                  <span className="text-primary">{inv?.totalQuestionsAnswered ?? 0}</span>
                </div>
                <div className="interview-stat-row">
                  <span className="text-secondary">Average score</span>
                  <span className={inv?.averageScore >= 7 ? 'text-success' : 'text-secondary'}>
                    {inv?.averageScore ? `${inv.averageScore.toFixed(1)} / 10` : '—'}
                  </span>
                </div>
              </div>

              <div className="mt-6">
                <Link to="/interviews" className="btn btn-secondary w-full" style={{ justifyContent: 'center' }}>
                  Start Mock Interview
                </Link>
              </div>
            </div>
          </div>

          {/* Top Missing Skills */}
          {data?.topMissingSkills?.length > 0 && (
            <div className="card mt-6">
              <div className="card-header">
                <span className="card-title">Top Skill Gaps</span>
                <span className="text-muted text-sm">Across all your applications</span>
              </div>
              <div className="skill-gap-list">
                {data.topMissingSkills.slice(0, 6).map((item, i) => {
                  const maxCount = data.topMissingSkills[0].count;
                  const pct = Math.round((item.count / maxCount) * 100);
                  return (
                    <div key={item.skill} className="skill-gap-row">
                      <div className="skill-gap-label">
                        <span className="skill-rank">#{i + 1}</span>
                        <span className="skill-name">{item.skill}</span>
                      </div>
                      <div className="skill-gap-bar-wrap">
                        <div className="skill-gap-bar" style={{ width: `${pct}%` }} />
                      </div>
                      <span className="skill-count">{item.count}×</span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
