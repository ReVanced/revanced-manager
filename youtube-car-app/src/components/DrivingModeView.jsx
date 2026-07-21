import React from 'react';
import { Play, Pause, SkipForward, SkipBack, Radio, Volume2, ShieldCheck, Minimize2, Mic, Zap } from 'lucide-react';

export default function DrivingModeView({ video, isPlaying, onTogglePlay, onNext, onPrevious, onCloseDrivingMode }) {
  if (!video) return null;

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      zIndex: 9999,
      background: 'radial-gradient(circle at center, #182030 0%, #07090e 100%)',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between',
      padding: '32px',
      color: '#fff'
    }}>
      {/* Top Header Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span className="badge-premium" style={{ fontSize: '0.9rem', padding: '6px 16px' }}>MODE BERKENDARA</span>
          <span className="badge-adblock">
            <ShieldCheck size={18} /> ADBLOCK ACTIVE
          </span>
        </div>

        <button onClick={onCloseDrivingMode} className="car-btn car-btn-primary" style={{ padding: '0 20px' }}>
          <Minimize2 size={24} style={{ marginRight: '8px' }} /> Keluar Mode Setir
        </button>
      </div>

      {/* Middle Center Artwork & Title */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '40px',
        maxWidth: '1000px',
        margin: '0 auto',
        width: '100%'
      }}>
        {/* Large Album Artwork */}
        <div style={{
          width: '280px',
          height: '280px',
          borderRadius: '28px',
          overflow: 'hidden',
          boxShadow: '0 20px 50px rgba(0,0,0,0.8)',
          border: '2px solid var(--border-glow)',
          flexShrink: 0
        }}>
          <img src={video.thumbnail} alt={video.title} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        </div>

        {/* Big Readable Title */}
        <div style={{ flex: 1 }}>
          <h1 style={{
            fontSize: '2.4rem',
            fontWeight: 800,
            lineHeight: 1.25,
            marginBottom: '12px',
            color: '#fff',
            textShadow: '0 4px 12px rgba(0,0,0,0.8)'
          }}>
            {video.title}
          </h1>
          <p style={{ fontSize: '1.4rem', color: 'var(--text-muted)', fontWeight: 600 }}>
            {video.uploaderName || video.uploader || 'YouTube Channel'}
          </p>

          <div style={{ marginTop: '20px', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{
              background: 'rgba(255,255,255,0.08)',
              padding: '8px 16px',
              borderRadius: '12px',
              fontSize: '1rem',
              color: 'var(--accent-green)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px'
            }}>
              <Zap size={18} /> Auto-Skip Sponsor Active
            </span>
          </div>
        </div>
      </div>

      {/* Bottom Giant Touch Buttons for Steering Wheel / Car Touchscreen */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '36px'
      }}>
        <button onClick={onPrevious} className="car-btn" style={{ width: '90px', height: '90px', borderRadius: '50%' }}>
          <SkipBack size={42} />
        </button>

        <button
          onClick={onTogglePlay}
          className="car-btn car-btn-primary"
          style={{ width: '120px', height: '120px', borderRadius: '50%', boxShadow: '0 8px 30px var(--accent-red-glow)' }}
        >
          {isPlaying ? <Pause size={56} /> : <Play size={56} style={{ marginLeft: '6px' }} />}
        </button>

        <button onClick={onNext} className="car-btn" style={{ width: '90px', height: '90px', borderRadius: '50%' }}>
          <SkipForward size={42} />
        </button>
      </div>
    </div>
  );
}
