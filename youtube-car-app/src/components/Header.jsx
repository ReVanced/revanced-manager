import React, { useState } from 'react';
import { Search, Mic, ShieldCheck, Sparkles, Car, Music, Radio, Flame, Bookmark, Clock } from 'lucide-react';

export default function Header({ onSearch, activeCategory, setActiveCategory }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [isListening, setIsListening] = useState(false);

  const categories = [
    { id: 'trending', label: 'Rekomendasi Mobil', icon: Car },
    { id: 'lofi', label: 'Lofi & Chill Drive', icon: Radio },
    { id: 'indonesia', label: 'Hits Indonesia', icon: Music },
    { id: 'podcast', label: 'Podcast & Talkshow', icon: Flame },
    { id: 'night', label: 'Night Drive Synthwave', icon: Sparkles }
  ];

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (onSearch) onSearch(searchQuery);
  };

  // Voice Search (SpeechRecognition API)
  const triggerVoiceSearch = () => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
      const recognition = new SpeechRecognition();
      recognition.lang = 'id-ID';
      recognition.onstart = () => setIsListening(true);
      recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        setSearchQuery(transcript);
        setIsListening(false);
        if (onSearch) onSearch(transcript);
      };
      recognition.onerror = () => setIsListening(false);
      recognition.onend = () => setIsListening(false);
      recognition.start();
    } else {
      alert('Pencarian suara belum didukung di browser ini. Silakan gunakan keyboard sentuh.');
    }
  };

  return (
    <header style={{
      display: 'flex',
      flexDirection: 'column',
      gap: '14px',
      padding: '16px 24px',
      background: 'var(--bg-surface)',
      borderBottom: '1px solid var(--border-color)'
    }}>
      {/* Top Row: App Logo + Search Bar + Badges */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '16px'
      }}>
        {/* Brand Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '14px',
            background: 'linear-gradient(135deg, #ff0033 0%, #99001f 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 16px var(--accent-red-glow)'
          }}>
            <Car size={26} color="#ffffff" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h1 style={{ fontSize: '1.35rem', fontWeight: 800, color: '#fff', letterSpacing: '-0.5px' }}>
                YOUTUBE <span style={{ color: 'var(--accent-red)' }}>CAR</span>
              </h1>
              <span className="badge-premium" style={{ fontSize: '0.75rem', padding: '3px 8px' }}>PRO</span>
            </div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Adblock & Background Playback for Driving
            </p>
          </div>
        </div>

        {/* Big Search Bar for Touch Screen */}
        <form onSubmit={handleSearchSubmit} style={{
          display: 'flex',
          alignItems: 'center',
          flex: 1,
          maxWidth: '520px',
          minWidth: '280px',
          gap: '8px'
        }}>
          <div style={{
            position: 'relative',
            flex: 1,
            display: 'flex',
            alignItems: 'center'
          }}>
            <Search size={22} style={{ position: 'absolute', left: '16px', color: 'var(--text-muted)' }} />
            <input
              type="text"
              placeholder="Cari lagu, podcast, atau video..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                width: '100%',
                height: '54px',
                paddingLeft: '52px',
                paddingRight: '16px',
                borderRadius: '16px',
                background: 'var(--bg-primary)',
                border: '1px solid var(--border-color)',
                color: '#fff',
                fontSize: '1rem',
                fontWeight: '500',
                outline: 'none'
              }}
            />
          </div>

          {/* Voice Search Button */}
          <button
            type="button"
            onClick={triggerVoiceSearch}
            className={`car-btn ${isListening ? 'car-btn-primary glow-active' : ''}`}
            style={{ minWidth: '54px', height: '54px', padding: 0 }}
            title="Cari via Suara (Voice Search)"
          >
            <Mic size={22} color={isListening ? '#fff' : 'var(--text-main)'} />
          </button>
        </form>

        {/* Adblock Protection Badge */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span className="badge-adblock">
            <ShieldCheck size={18} /> Iklan Diblokir 100%
          </span>
        </div>
      </div>

      {/* Quick Category Pills */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        overflowX: 'auto',
        paddingBottom: '4px'
      }}>
        {categories.map((cat) => {
          const Icon = cat.icon;
          const isActive = activeCategory === cat.id;
          return (
            <button
              key={cat.id}
              onClick={() => {
                setActiveCategory(cat.id);
                if (cat.id === 'trending') onSearch('');
                else onSearch(cat.label);
              }}
              className="car-btn"
              style={{
                minHeight: '44px',
                padding: '0 18px',
                fontSize: '0.9rem',
                borderRadius: '24px',
                background: isActive ? 'var(--accent-red)' : 'var(--bg-card)',
                color: isActive ? '#fff' : 'var(--text-main)',
                borderColor: isActive ? 'var(--accent-red)' : 'var(--border-color)',
                boxShadow: isActive ? '0 4px 14px var(--accent-red-glow)' : 'none',
                whiteSpace: 'nowrap'
              }}
            >
              <Icon size={16} style={{ marginRight: '8px' }} />
              {cat.label}
            </button>
          );
        })}
      </div>
    </header>
  );
}
