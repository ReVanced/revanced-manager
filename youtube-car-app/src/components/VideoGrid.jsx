import React from 'react';
import { Play, Eye, Clock, ShieldCheck } from 'lucide-react';

export default function VideoGrid({ videos, onSelectVideo, currentVideoId }) {
  if (!videos || videos.length === 0) {
    return (
      <div style={{
        padding: '60px 20px',
        textAlign: 'center',
        color: 'var(--text-muted)'
      }}>
        <h3>Tidak ada video ditemukan. Silakan cari dengan kata kunci lain.</h3>
      </div>
    );
  }

  const formatViews = (views) => {
    if (!views) return 'Populer';
    if (views >= 1000000) return `${(views / 1000000).toFixed(1)}M ditonton`;
    if (views >= 1000) return `${(views / 1000).toFixed(0)}rb ditonton`;
    return `${views} ditonton`;
  };

  const formatDuration = (seconds) => {
    if (!seconds || seconds === 0) return 'LIVE';
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
      gap: '20px',
      padding: '24px'
    }}>
      {videos.map((vid) => {
        const isPlayingThis = currentVideoId === vid.id;
        return (
          <div
            key={vid.id}
            onClick={() => onSelectVideo(vid)}
            className={`video-card-car ${isPlayingThis ? 'glow-active' : ''}`}
            style={{
              borderColor: isPlayingThis ? 'var(--accent-red)' : 'var(--border-color)'
            }}
          >
            {/* Thumbnail Box */}
            <div className="video-card-thumbnail">
              <img src={vid.thumbnail} alt={vid.title} loading="lazy" />
              <div className="duration-badge">
                {formatDuration(vid.duration)}
              </div>
              
              {/* Play Overlay */}
              <div style={{
                position: 'absolute',
                top: 0, left: 0, right: 0, bottom: 0,
                background: 'rgba(0,0,0,0.3)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                opacity: isPlayingThis ? 1 : 0.8
              }}>
                <div className="car-btn car-btn-primary" style={{ width: '56px', height: '56px', borderRadius: '50%', padding: 0 }}>
                  <Play size={24} style={{ marginLeft: '3px' }} />
                </div>
              </div>
            </div>

            {/* Video Details */}
            <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <h3 style={{
                fontSize: '1rem',
                fontWeight: '700',
                color: '#fff',
                lineHeight: '1.35',
                display: '-webkit-box',
                WebkitLineClamp: 2,
                WebkitBoxOrient: 'vertical',
                overflow: 'hidden'
              }}>
                {vid.title}
              </h3>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                <span>{vid.uploaderName}</span>
                <span>{formatViews(vid.views)}</span>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
