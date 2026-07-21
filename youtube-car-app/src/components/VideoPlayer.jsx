import React, { useEffect, useRef, useState } from 'react';
import { Play, Pause, SkipForward, SkipBack, Maximize, ShieldCheck, Zap, Volume2, VolumeX, Gauge, Radio, Minimize2, Tv } from 'lucide-react';
import { getSponsorSegments } from '../services/youtubeApi';
import { setupMediaSession, updatePlaybackState } from '../services/mediaSession';

export default function VideoPlayer({ video, onNext, onPrevious, isDrivingMode, setIsDrivingMode }) {
  const videoRef = useRef(null);
  const iframeRef = useRef(null);
  const [isPlaying, setIsPlaying] = useState(true);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [isMuted, setIsMuted] = useState(false);
  const [audioOnly, setAudioOnly] = useState(false);
  const [playbackSpeed, setPlaybackSpeed] = useState(1);
  const [sponsorNotice, setSponsorNotice] = useState(null);
  const [sponsorSegments, setSponsorSegments] = useState([]);
  const [useIframeFallback, setUseIframeFallback] = useState(false);

  // Load SponsorBlock segments
  useEffect(() => {
    if (video?.id) {
      getSponsorSegments(video.id).then(segments => {
        setSponsorSegments(segments);
      });
      // Set default fallback if Piped stream isn't direct
      if (video.isEmbedFallback) {
        setUseIframeFallback(true);
      } else {
        setUseIframeFallback(false);
      }
    }
  }, [video]);

  // Setup MediaSession for Car Bluetooth Controls
  useEffect(() => {
    if (video) {
      setupMediaSession({
        title: video.title,
        artist: video.uploaderName || video.uploader || 'YouTube Channel',
        artworkUrl: video.thumbnail,
        onPlay: handlePlay,
        onPause: handlePause,
        onNext: onNext,
        onPrevious: onPrevious,
        onSeek: (seconds) => {
          if (videoRef.current) {
            videoRef.current.currentTime += seconds;
          }
        }
      });
    }
  }, [video, onNext, onPrevious]);

  // Handle Play/Pause
  const handlePlay = () => {
    if (videoRef.current) {
      videoRef.current.play();
      setIsPlaying(true);
      updatePlaybackState('playing');
    }
  };

  const handlePause = () => {
    if (videoRef.current) {
      videoRef.current.pause();
      setIsPlaying(false);
      updatePlaybackState('paused');
    }
  };

  const togglePlayPause = () => {
    if (isPlaying) {
      handlePause();
    } else {
      handlePlay();
    }
  };

  // Check and auto-skip SponsorBlock segments
  const handleTimeUpdate = () => {
    if (!videoRef.current) return;
    const curr = videoRef.current.currentTime;
    setCurrentTime(curr);
    setDuration(videoRef.current.duration || 0);

    // Auto skip sponsor segment if current time falls in segment
    for (const seg of sponsorSegments) {
      if (curr >= seg.start && curr < seg.end) {
        videoRef.current.currentTime = seg.end;
        setSponsorNotice(`Otomatis Melompati Sponsor (${seg.category})`);
        setTimeout(() => setSponsorNotice(null), 3500);
        break;
      }
    }
  };

  // Picture in Picture
  const togglePiP = async () => {
    try {
      if (document.pictureInPictureElement) {
        await document.exitPictureInPicture();
      } else if (videoRef.current && document.pictureInPictureEnabled) {
        await videoRef.current.requestPictureInPicture();
      }
    } catch (e) {
      console.warn('PiP error:', e);
    }
  };

  // Change speed
  const changeSpeed = (speed) => {
    setPlaybackSpeed(speed);
    if (videoRef.current) {
      videoRef.current.playbackRate = speed;
    }
  };

  const formatTime = (secs) => {
    if (!secs || isNaN(secs)) return '00:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  if (!video) return null;

  return (
    <div className={`video-player-container ${isDrivingMode ? 'driving-mode-active' : ''}`} style={{ width: '100%' }}>
      {/* Toast Notification for SponsorBlock & Adblock */}
      {sponsorNotice && (
        <div style={{
          position: 'fixed',
          top: '20px',
          right: '20px',
          zIndex: 999,
          background: 'rgba(0, 230, 118, 0.95)',
          color: '#000',
          padding: '12px 20px',
          borderRadius: '16px',
          fontWeight: '700',
          boxShadow: '0 8px 24px rgba(0,230,118,0.4)',
          display: 'flex',
          alignItems: 'center',
          gap: '10px'
        }}>
          <Zap size={20} />
          {sponsorNotice}
        </div>
      )}

      {/* Main Player Box */}
      <div style={{
        position: 'relative',
        width: '100%',
        background: '#000',
        borderRadius: isDrivingMode ? '0px' : '24px',
        overflow: 'hidden',
        boxShadow: '0 12px 40px rgba(0,0,0,0.8)',
        border: isDrivingMode ? 'none' : '1px solid var(--border-color)'
      }}>
        {/* Direct HTML5 Stream / YouTube Fallback Embed */}
        {useIframeFallback ? (
          <div style={{ position: 'relative', width: '100%', aspectRatio: '16/9' }}>
            <iframe
              ref={iframeRef}
              src={`https://www.youtube-nocookie.com/embed/${video.id}?autoplay=1&enablejsapi=1&rel=0&modestbranding=1`}
              title={video.title}
              style={{ width: '100%', height: '100%', border: 'none' }}
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
            />
          </div>
        ) : (
          <div style={{ position: 'relative', width: '100%', aspectRatio: '16/9', background: '#000' }}>
            {audioOnly ? (
              <div style={{
                width: '100%',
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'linear-gradient(135deg, #10141e 0%, #1e2638 100%)',
                color: '#fff',
                padding: '20px',
                textAlign: 'center'
              }}>
                <Radio size={64} style={{ color: 'var(--accent-red)', marginBottom: '16px' }} className="glow-active" />
                <h3 style={{ fontSize: '1.4rem', fontWeight: 700 }}>Mode Audio Sahaja (Hemat Data Mobil)</h3>
                <p style={{ color: 'var(--text-muted)', marginTop: '6px' }}>{video.title}</p>
              </div>
            ) : (
              <video
                ref={videoRef}
                src={video.hlsUrl || `https://www.youtube.com/watch?v=${video.id}`}
                poster={video.thumbnail}
                style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                autoPlay
                playsInline
                onTimeUpdate={handleTimeUpdate}
                onEnded={onNext}
              />
            )}
          </div>
        )}

        {/* Video Info Header Bar */}
        <div style={{
          padding: '16px 20px',
          background: 'var(--bg-surface)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderTop: '1px solid var(--border-color)',
          flexWrap: 'wrap',
          gap: '12px'
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px' }}>
              <span className="badge-premium">PREMIUM</span>
              <span className="badge-adblock">
                <ShieldCheck size={14} /> ADBLOCK ACTIVE
              </span>
            </div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-main)' }}>
              {video.title}
            </h2>
            <p style={{ fontSize: '0.95rem', color: 'var(--text-muted)' }}>
              {video.uploaderName || video.uploader || 'YouTube Channel'}
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            {/* Driving Mode Button */}
            <button
              onClick={() => setIsDrivingMode(!isDrivingMode)}
              className={`car-btn ${isDrivingMode ? 'car-btn-primary' : ''}`}
              title="Mode Berkendara"
            >
              {isDrivingMode ? <Minimize2 size={20} /> : <Tv size={20} />}
              <span style={{ marginLeft: '8px' }}>{isDrivingMode ? 'Keluar Mode Setir' : 'Mode Setir (Driving)'}</span>
            </button>

            {/* PiP Button */}
            <button onClick={togglePiP} className="car-btn" title="Picture in Picture">
              <Maximize size={20} />
            </button>

            {/* Audio Only Toggle */}
            <button
              onClick={() => setAudioOnly(!audioOnly)}
              className={`car-btn ${audioOnly ? 'car-btn-primary' : ''}`}
              title="Mode Hemat Data / Audio"
            >
              <Radio size={20} />
            </button>
          </div>
        </div>

        {/* Car Extra Large Controls Panel (Ideal for Head Unit Screen) */}
        <div style={{
          padding: isDrivingMode ? '24px' : '16px 20px',
          background: 'var(--bg-card)',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px'
        }}>
          {/* Progress Bar with timestamp */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)', fontWeight: 600 }}>
              {formatTime(currentTime)}
            </span>
            <input
              type="range"
              min="0"
              max={duration || 100}
              value={currentTime}
              onChange={(e) => {
                const val = parseFloat(e.target.value);
                setCurrentTime(val);
                if (videoRef.current) videoRef.current.currentTime = val;
              }}
              style={{
                flex: 1,
                accentColor: 'var(--accent-red)',
                height: '10px',
                borderRadius: '5px',
                cursor: 'pointer'
              }}
            />
            <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)', fontWeight: 600 }}>
              {formatTime(duration)}
            </span>
          </div>

          {/* Main Car Touch Controls (Big Play, Next, Prev, Skip 10s) */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: isDrivingMode ? '28px' : '16px',
            flexWrap: 'wrap'
          }}>
            {/* Previous */}
            <button onClick={onPrevious} className="car-btn" style={{ minWidth: isDrivingMode ? '72px' : '64px' }}>
              <SkipBack size={isDrivingMode ? 32 : 24} />
            </button>

            {/* Skip -10s */}
            <button
              onClick={() => { if (videoRef.current) videoRef.current.currentTime -= 10; }}
              className="car-btn"
            >
              -10s
            </button>

            {/* Play/Pause Giant Button */}
            <button
              onClick={togglePlayPause}
              className="car-btn car-btn-primary car-btn-giant"
              style={{
                width: isDrivingMode ? '96px' : '80px',
                height: isDrivingMode ? '96px' : '80px'
              }}
            >
              {isPlaying ? <Pause size={isDrivingMode ? 44 : 36} /> : <Play size={isDrivingMode ? 44 : 36} style={{ marginLeft: '4px' }} />}
            </button>

            {/* Skip +10s */}
            <button
              onClick={() => { if (videoRef.current) videoRef.current.currentTime += 10; }}
              className="car-btn"
            >
              +10s
            </button>

            {/* Next */}
            <button onClick={onNext} className="car-btn" style={{ minWidth: isDrivingMode ? '72px' : '64px' }}>
              <SkipForward size={isDrivingMode ? 32 : 24} />
            </button>
          </div>

          {/* Speed Selector */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
            <Gauge size={18} style={{ color: 'var(--text-muted)', marginRight: '6px' }} />
            {[0.75, 1, 1.25, 1.5, 2].map(speed => (
              <button
                key={speed}
                onClick={() => changeSpeed(speed)}
                className="car-btn"
                style={{
                  minHeight: '40px',
                  padding: '0 14px',
                  fontSize: '0.85rem',
                  background: playbackSpeed === speed ? 'var(--accent-red)' : 'var(--bg-surface)',
                  color: playbackSpeed === speed ? '#fff' : 'var(--text-muted)',
                  borderColor: playbackSpeed === speed ? 'var(--accent-red)' : 'var(--border-color)'
                }}
              >
                {speed}x
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
