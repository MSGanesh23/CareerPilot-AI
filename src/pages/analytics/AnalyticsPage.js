import React, { useEffect, useState } from 'react';
import {
  BarChart, Bar, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell
} from 'recharts';
import { analyticsApi } from '../../api/services';
import './AnalyticsPage.css';

const CHART_COLORS = {
  accent: '#f5c842',
  success: '#3ecf8e',
  info: '#63b3ed',
  danger: '#f56565',
  muted: '#55556a',
};

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="chart-tooltip">
      <div className="chart-tooltip-label">{label}</div>
      {payload.map((p, i) => (
        <div key={i} style={{ color: p.color, fontSize: '0.85rem' }}>
          {p.name}: <strong>{p.value}</strong>
        </div>
      ))}
    </div>
  );
}

export default function AnalyticsPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    analyticsApi.getDashboard()
      .then(res => setData(res.data.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="loading-center"><div className="spinner" /><span>Loading analytics…</span></div>;
  if (!data) return <div className="empty-state"><h3>Could not load analytics</h3></div>;

  const { applicationStats: app, interviewStats: inv, topMissingSkills, applicationTrend, scoreTrend } = data;

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1>Analytics</h1>
        <p>Your career search performance at a glance.</p>
      </div>

      {/* KPI row */}
      <div className="grid-4 mb-6">
        {[
          { label: 'Total Applications', value: app.totalApplications },
          { label: 'Offer Rate',         value: `${app.offerRate.toFixed(1)}%` },
          { label: 'Avg Match Score',    value: app.averageMatchScore ? `${Math.round(app.averageMatchScore)}%` : '—' },
          { label: 'Avg Interview Score',value: inv.averageScore ? `${inv.averageScore.toFixed(1)}/10` : '—' },
        ].map(({ label, value }) => (
          <div key={label} className="card analytics-kpi">
            <div className="analytics-kpi-value">{value}</div>
            <div className="analytics-kpi-label">{label}</div>
          </div>
        ))}
      </div>

      {/* Charts row */}
      <div className="grid-2 mb-6">
        {/* Application trend */}
        <div className="card">
          <div className="card-header"><span className="card-title">Applications Over Time</span></div>
          {applicationTrend?.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={applicationTrend} margin={{ top: 4, right: 8, bottom: 0, left: -20 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
                <XAxis dataKey="month" tick={{ fill: '#55556a', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: '#55556a', fontSize: 11 }} axisLine={false} tickLine={false} allowDecimals={false} />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="count" name="Applications" fill={CHART_COLORS.accent} radius={[4, 4, 0, 0]} opacity={0.85} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="chart-empty">No application data yet</div>
          )}
        </div>

        {/* Score trend */}
        <div className="card">
          <div className="card-header"><span className="card-title">Mock Interview Score Trend</span></div>
          {scoreTrend?.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={scoreTrend} margin={{ top: 4, right: 8, bottom: 0, left: -20 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
                <XAxis dataKey="month" tick={{ fill: '#55556a', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis domain={[0, 10]} tick={{ fill: '#55556a', fontSize: 11 }} axisLine={false} tickLine={false} />
                <Tooltip content={<CustomTooltip />} />
                <Line
                  type="monotone" dataKey="averageScore" name="Avg Score"
                  stroke={CHART_COLORS.success} strokeWidth={2}
                  dot={{ fill: CHART_COLORS.success, r: 4 }}
                  activeDot={{ r: 6 }}
                />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <div className="chart-empty">Complete some interviews to see trends</div>
          )}
        </div>
      </div>

      {/* Bottom row */}
      <div className="grid-2">
        {/* Status breakdown */}
        <div className="card">
          <div className="card-header"><span className="card-title">Status Breakdown</span></div>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart
              data={[
                { name: 'Applied',      value: app.applied,      color: CHART_COLORS.info },
                { name: 'Interviewing', value: app.interviewing, color: CHART_COLORS.accent },
                { name: 'Offer',        value: app.offers,       color: CHART_COLORS.success },
                { name: 'Rejected',     value: app.rejections,   color: CHART_COLORS.danger },
                { name: 'Withdrawn',    value: app.withdrawn,    color: CHART_COLORS.muted },
              ]}
              layout="vertical"
              margin={{ top: 0, right: 8, bottom: 0, left: 60 }}
            >
              <XAxis type="number" tick={{ fill: '#55556a', fontSize: 11 }} axisLine={false} tickLine={false} allowDecimals={false} />
              <YAxis type="category" dataKey="name" tick={{ fill: '#9090a8', fontSize: 12 }} axisLine={false} tickLine={false} width={60} />
              <Tooltip content={<CustomTooltip />} />
              <Bar dataKey="value" name="Count" radius={[0, 4, 4, 0]}>
                {[
                  { name: 'Applied', color: CHART_COLORS.info },
                  { name: 'Interviewing', color: CHART_COLORS.accent },
                  { name: 'Offer', color: CHART_COLORS.success },
                  { name: 'Rejected', color: CHART_COLORS.danger },
                  { name: 'Withdrawn', color: CHART_COLORS.muted },
                ].map((entry, index) => (
                  <Cell key={index} fill={entry.color} opacity={0.8} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Top missing skills */}
        <div className="card">
          <div className="card-header"><span className="card-title">Top Skill Gaps</span></div>
          {topMissingSkills?.length > 0 ? (
            <div className="analytics-skills">
              {topMissingSkills.slice(0, 8).map((item, i) => {
                const maxCount = topMissingSkills[0].count;
                const pct = Math.round((item.count / maxCount) * 100);
                return (
                  <div key={item.skill} className="analytics-skill-row">
                    <span className="analytics-skill-name">{item.skill}</span>
                    <div className="analytics-skill-bar-wrap">
                      <div className="analytics-skill-bar" style={{ width: `${pct}%` }} />
                    </div>
                    <span className="analytics-skill-count">{item.count}</span>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="chart-empty">No skill gap data yet</div>
          )}
        </div>
      </div>
    </div>
  );
}
